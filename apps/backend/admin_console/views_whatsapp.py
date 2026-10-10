"""Linking the WhatsApp bot from the console (DECISION-116).

The bot sends the codes people sign up and recover with, from a WhatsApp account it holds as a
linked device. Linking it meant reading a QR code off the bot's container log on the server:
work for whoever runs the server, not for the platform's owner. These routes let the console
show the bot's state, draw its QR code to scan from the phone that will send, and start a fresh
pairing — to recover from an unlinked account, or to move the bot to another number.

The bot is never on the internet; the backend asks it on the private network with the shared
secret, and only an operator who may change the platform's settings sees the code. The code is
drawn here, as the two-step sign-in's is, so it never leaves as text.
"""

from __future__ import annotations

import json
import logging
import urllib.error
import urllib.request
from collections.abc import Callable
from typing import Any

from django.conf import settings
from drf_spectacular.utils import extend_schema
from rest_framework import serializers
from rest_framework.response import Response

from accounts.authentication import AuthenticatedRequest
from audit.services import record_audit
from core.exceptions import DomainError
from core.openapi import DOMAIN_400, protected

from .views import AdminView, _request_id

logger = logging.getLogger(__name__)

MANAGE = "admin.settings.manage"
TIMEOUT_SECONDS = 8

# (method, url, headers) -> (status, body). Raises OSError when nothing came back.
Caller = Callable[[str, str, dict[str, str]], tuple[int, bytes]]


def _call(method: str, url: str, headers: dict[str, str]) -> tuple[int, bytes]:
    data = b"{}" if method == "POST" else None
    request = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(request, timeout=TIMEOUT_SECONDS) as response:
            return int(response.status), response.read()
    except urllib.error.HTTPError as error:
        return int(error.code), error.read()


class AdminWhatsAppStateSerializer(serializers.Serializer[Any]):
    provider = serializers.CharField(help_text="The code provider in use, e.g. whatsapp_bot.")
    configured = serializers.BooleanField(help_text="The bot's address and secret are set.")
    reachable = serializers.BooleanField(help_text="The bot answered.")
    connected = serializers.BooleanField()
    loggedOut = serializers.BooleanField(help_text="WhatsApp dropped the link: pair again.")
    linkedNumber = serializers.CharField(allow_null=True)
    qrSvgDataUri = serializers.CharField(
        allow_null=True, help_text="The code to scan, drawn as SVG; null once linked."
    )


class WhatsAppBotView(AdminView):
    """What both routes share: asking the bot, and reading its answer as the console's state."""

    required_permission = MANAGE
    caller: Caller = staticmethod(_call)

    def _bot(self) -> tuple[str, str]:
        url = str(getattr(settings, "WHATSAPP_BOT_URL", "") or "").rstrip("/")
        token = str(getattr(settings, "WHATSAPP_BOT_TOKEN", "") or "")
        return url, token

    def _ask(self, method: str, path: str) -> tuple[int, dict[str, Any]] | None:
        url, token = self._bot()
        if not (url and token):
            return None
        try:
            status, raw = self.caller(method, f"{url}{path}", {"Authorization": f"Bearer {token}"})
        except OSError:
            logger.warning("whatsapp.bot_unreachable", extra={"path": path})
            return None
        try:
            body = json.loads(raw or b"{}")
        except ValueError:
            body = {}
        return status, body if isinstance(body, dict) else {}

    def _state(self) -> dict[str, Any]:
        url, token = self._bot()
        state: dict[str, Any] = {
            "provider": str(getattr(settings, "OTP_PROVIDER", "development")).lower(),
            "configured": bool(url and token),
            "reachable": False,
            "connected": False,
            "loggedOut": False,
            "linkedNumber": None,
            "qrSvgDataUri": None,
        }
        answer = self._ask("GET", "/pairing")
        if answer is None or answer[0] >= 300:
            return state
        body = answer[1]
        number = str(body.get("linkedNumber") or "")
        qr = body.get("qr")
        state.update(
            reachable=True,
            connected=body.get("connected") is True,
            loggedOut=body.get("loggedOut") is True,
            linkedNumber=f"+{number}" if number.isdigit() else None,
            qrSvgDataUri=_draw(qr) if isinstance(qr, str) and qr else None,
        )
        return state


class AdminWhatsAppView(WhatsAppBotView):
    """The bot's link, for the console's «ربط واتساب» card."""

    @extend_schema(
        operation_id="adminWhatsAppRetrieve",
        tags=["Admin Settings"],
        summary="The WhatsApp bot's link: state, number, or the QR code to scan",
        responses={200: AdminWhatsAppStateSerializer, **protected()},
    )
    def get(self, request: AuthenticatedRequest) -> Response:
        return Response(self._state())


class AdminWhatsAppRelinkView(WhatsAppBotView):
    @extend_schema(
        operation_id="adminWhatsAppRelink",
        tags=["Admin Settings"],
        summary="Forget the linked WhatsApp account and start a fresh pairing",
        description=(
            "The bot logs its account out (it leaves the phone's linked devices), forgets it "
            "and offers a new QR code. Codes cannot be sent until it is scanned."
        ),
        request=None,
        responses={202: AdminWhatsAppStateSerializer, 400: DOMAIN_400, **protected()},
    )
    def post(self, request: AuthenticatedRequest) -> Response:
        before = self._state()
        answer = self._ask("POST", "/pairing/relink")
        if answer is None or answer[0] >= 300:
            raise DomainError(
                "WHATSAPP_BOT_UNAVAILABLE",
                message="تعذّر الوصول إلى بوت واتساب. تحقق من تشغيله ثم أعد المحاولة.",
            )
        record_audit(
            actor=request.user,
            action="whatsapp.relinked",
            target=None,
            before_snapshot={"linkedNumber": before["linkedNumber"]},
            after_snapshot={"linkedNumber": None},
            request_id=_request_id(request),
        )
        return Response(self._state(), status=202)


def _draw(qr: str) -> str:
    import segno

    return str(segno.make(qr, error="m").svg_data_uri(scale=5, border=2))
