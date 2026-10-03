from __future__ import annotations

from typing import Any
from uuid import UUID

from channels.generic.websocket import AsyncJsonWebsocketConsumer

from locations.models import Province

from .groups import admin_group, province_group, user_group


class DirectoryConsumer(AsyncJsonWebsocketConsumer):  # type: ignore[misc]
    async def connect(self) -> None:
        self.joined_groups: set[str] = set()
        self.authenticated_user_id: str | None = None
        self.authenticated_claims: dict[str, Any] | None = None
        await self.accept()

    async def disconnect(self, close_code: int) -> None:
        for group in self.joined_groups:
            await self.channel_layer.group_discard(group, self.channel_name)

    async def receive_json(self, content: Any, **kwargs: Any) -> None:
        if not isinstance(content, dict):
            await self.send_json({"type": "error", "code": "UNKNOWN_ACTION"})
            return
        action = content.get("action")
        if action == "authenticate":
            await self._authenticate(content)
            return
        if action == "subscribeProvince":
            await self._subscribe_province(content)
            return
        if action == "subscribeUser":
            await self._subscribe_user()
            return
        if action == "subscribeAdmin":
            await self._subscribe_admin(content)
            return
        await self.send_json({"type": "error", "code": "UNKNOWN_ACTION"})

    async def _authenticate(self, content: dict[str, Any]) -> None:
        token = content.get("accessToken")
        if not isinstance(token, str) or not token:
            await self.send_json({"type": "auth", "ok": False})
            return
        resolved = await self._resolve_access_token(token)
        if resolved is None:
            await self.send_json({"type": "auth", "ok": False})
            return
        user_id, claims = resolved
        self.authenticated_user_id = str(user_id)
        self.authenticated_claims = claims
        await self.send_json({"type": "auth", "ok": True})

    async def _subscribe_province(self, content: dict[str, Any]) -> None:
        province_id = _as_uuid(content.get("provinceId"))
        if province_id is None or not await self._province_is_active(province_id):
            await self.send_json({"type": "error", "code": "INVALID_PROVINCE"})
            return
        group = province_group(province_id)
        await self.channel_layer.group_add(group, self.channel_name)
        self.joined_groups.add(group)
        await self.send_json({"type": "subscribed", "scope": "province"})

    async def _subscribe_user(self) -> None:
        if self.authenticated_user_id is None:
            await self.send_json({"type": "error", "code": "AUTH_REQUIRED"})
            return
        group = user_group(self.authenticated_user_id)
        await self.channel_layer.group_add(group, self.channel_name)
        self.joined_groups.add(group)
        await self.send_json({"type": "subscribed", "scope": "user"})

    async def _subscribe_admin(self, content: dict[str, Any]) -> None:
        if self.authenticated_user_id is None:
            await self.send_json({"type": "error", "code": "AUTH_REQUIRED"})
            return
        channel = content.get("channel") or "system"
        if channel not in {"system", "review_queue"}:
            await self.send_json({"type": "error", "code": "INVALID_ADMIN_SCOPE"})
            return
        allowed = await self._has_admin_permission("realtime.admin")
        if not allowed:
            await self.send_json({"type": "error", "code": "FORBIDDEN"})
            return
        group = admin_group(channel)
        await self.channel_layer.group_add(group, self.channel_name)
        self.joined_groups.add(group)
        await self.send_json({"type": "subscribed", "scope": "admin"})

    async def directory_event(self, event: dict[str, Any]) -> None:
        payload = event["payload"]
        private = (payload.get("scope") or {}).get("type") in {"user", "admin"}
        if private and not await self._session_still_live():
            # The session this socket authenticated with was revoked or found compromised
            # after the socket joined its private groups. Stop, rather than keep serving it.
            await self.close(code=4401)
            return
        await self.send_json({"type": "event", **payload})

    async def _session_still_live(self) -> bool:
        from accounts.authentication import live_sessions_for

        if self.authenticated_claims is None:
            return False
        return await live_sessions_for(self.authenticated_claims).aexists()

    async def _has_admin_permission(self, permission_code: str) -> bool:
        from accounts.models import User

        if self.authenticated_user_id is None:
            return False
        user = await User.objects.filter(pk=self.authenticated_user_id).afirst()
        if user is None:
            return False
        if not await self._permission_sync(user, permission_code):
            return False
        return await self._second_step_passed(user)

    async def _second_step_passed(self, user: Any) -> bool:
        """The console's events need the same second step as the console (DECISION-065)."""
        from asgiref.sync import sync_to_async

        from accounts.authentication import live_sessions_for
        from accounts.mfa import console_block

        if self.authenticated_claims is None:
            return False
        session = await live_sessions_for(self.authenticated_claims).afirst()
        block = await sync_to_async(console_block)(user, session)
        return block is None

    @staticmethod
    async def _permission_sync(user: Any, permission_code: str) -> bool:
        from asgiref.sync import sync_to_async

        from accounts.rbac import user_has_admin_permission

        return await sync_to_async(user_has_admin_permission)(
            user,
            permission_code,
        )

    @staticmethod
    async def _province_is_active(province_id: UUID) -> bool:
        return await Province.objects.filter(pk=province_id, active=True).aexists()

    @staticmethod
    async def _resolve_access_token(token: str) -> tuple[Any, dict[str, Any]] | None:
        from django.core.exceptions import ValidationError as DjangoValidationError

        from accounts.authentication import live_sessions_for
        from accounts.tokens import decode_access_token

        claims = decode_access_token(token)
        if claims is None:
            return None
        try:
            session = await live_sessions_for(claims).afirst()
        except (TypeError, ValueError, DjangoValidationError):
            return None
        return (session.user_id, claims) if session else None


def _as_uuid(value: object) -> UUID | None:
    """The province id a client sent, or None when it is not one.

    Anything that is not a UUID used to reach the database and fail there, which closed the
    socket with a server error. Parsing it also settles its spelling: an id sent in capitals
    joins the same group the publisher sends to.
    """
    if not isinstance(value, str):
        return None
    try:
        return UUID(value)
    except ValueError:
        return None
