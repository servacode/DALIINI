from __future__ import annotations

from channels.generic.websocket import AsyncJsonWebsocketConsumer
from django.contrib.auth import get_user_model
from django.db.models import Q

from locations.models import Province

from .groups import admin_group, province_group, user_group

User = get_user_model()


class DirectoryConsumer(AsyncJsonWebsocketConsumer):
    async def connect(self):
        self.joined_groups = set()
        self.authenticated_user_id = None
        await self.accept()

    async def disconnect(self, close_code):
        for group in self.joined_groups:
            await self.channel_layer.group_discard(group, self.channel_name)

    async def receive_json(self, content, **kwargs):
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

    async def _authenticate(self, content):
        token = content.get("accessToken")
        if not isinstance(token, str) or not token:
            await self.send_json({"type": "auth", "ok": False})
            return
        user_id = await self._resolve_access_token(token)
        if user_id is None:
            await self.send_json({"type": "auth", "ok": False})
            return
        self.authenticated_user_id = str(user_id)
        await self.send_json({"type": "auth", "ok": True})

    async def _subscribe_province(self, content):
        province_id = content.get("provinceId")
        if not province_id or not await self._province_is_active(province_id):
            await self.send_json({"type": "error", "code": "INVALID_PROVINCE"})
            return
        group = province_group(province_id)
        await self.channel_layer.group_add(group, self.channel_name)
        self.joined_groups.add(group)
        await self.send_json({"type": "subscribed", "scope": "province"})

    async def _subscribe_user(self):
        if self.authenticated_user_id is None:
            await self.send_json({"type": "error", "code": "AUTH_REQUIRED"})
            return
        group = user_group(self.authenticated_user_id)
        await self.channel_layer.group_add(group, self.channel_name)
        self.joined_groups.add(group)
        await self.send_json({"type": "subscribed", "scope": "user"})

    async def _subscribe_admin(self, content):
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

    async def directory_event(self, event):
        await self.send_json({"type": "event", **event["payload"]})

    async def _has_admin_permission(self, permission_code):
        from accounts.models import User

        user = await User.objects.filter(pk=self.authenticated_user_id).afirst()
        if user is None:
            return False
        return await self._permission_sync(user, permission_code)

    @staticmethod
    async def _permission_sync(user, permission_code):
        from asgiref.sync import sync_to_async
        from accounts.rbac import user_has_admin_permission

        return await sync_to_async(user_has_admin_permission)(
            user,
            permission_code,
        )

    @staticmethod
    async def _province_is_active(province_id):
        return await Province.objects.filter(pk=province_id, active=True).aexists()

    @staticmethod
    async def _resolve_access_token(token):
        from accounts.tokens import decode_access_token

        claims = decode_access_token(token)
        if claims is None:
            return None
        try:
            user = await User.objects.filter(
                Q(pk=claims["sub"]),
                is_active=True,
            ).afirst()
        except (TypeError, ValueError):
            return None
        return user.pk if user else None
