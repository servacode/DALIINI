from __future__ import annotations

import json
import logging
import urllib.error
import urllib.request
from collections.abc import Callable

from django.conf import settings

from .base import InvalidRecipient, OtpMessage, TransientOtpError

logger = logging.getLogger(__name__)

TIMEOUT_SECONDS = 15

# What the bot answers with when the number has no WhatsApp account at all. Anything else it
# refuses is treated as an attempt that might work again.
PERMANENT_REASONS = frozenset({"not_on_whatsapp", "invalid_number"})


# POST (url, body, headers) -> (status, body). Raises OSError when nothing came back.
Opener = Callable[[str, bytes, dict[str, str]], tuple[int, bytes]]


def _urlopen(url: str, body: bytes, headers: dict[str, str]) -> tuple[int, bytes]:
    request = urllib.request.Request(url, data=body, headers=headers, method="POST")
    try:
        with urllib.request.urlopen(request, timeout=TIMEOUT_SECONDS) as response:
            return int(response.status), response.read()
    except urllib.error.HTTPError as error:
        return int(error.code), error.read()


class WhatsAppBotOtpSender:
    """Sends the code through our own WhatsApp bot service.

    The bot is a separate process holding a WhatsApp session, reached over HTTP on a private
    network. This class knows nothing about how it holds that session; it posts a number and a
    code and reads a verdict.

    **What this arrangement is.** The bot drives an ordinary WhatsApp account rather than the
    Business Cloud API. That is outside WhatsApp's terms, and the account can be banned without
    warning or appeal — at which point registration stops for everyone until a new number is
    paired. Two things here exist because of that:

      - *Nothing else depends on this class.* It implements the same `OtpSender` protocol as
        the Cloud API sender beside it, so moving to the official route is one setting, not a
        rewrite.
      - *A disconnected bot fails loudly.* It raises rather than reporting success, so a person
        is told the code could not be sent instead of waiting for one that will never arrive.

    **What is deliberately never logged.** Not the code and not the number — a failure records
    the bot's own reason and the HTTP status, so a log file is not a list of who registered.
    """

    def __init__(self, opener: Opener | None = None) -> None:
        self._open = opener or _urlopen
        self.base_url = str(getattr(settings, "WHATSAPP_BOT_URL", "") or "").rstrip("/")
        self.token = str(getattr(settings, "WHATSAPP_BOT_TOKEN", "") or "")

    def send(self, message: OtpMessage) -> None:
        self._post({"phone": message.phone, "code": message.code})

    def send_welcome(self, phone: str) -> None:
        """The platform's first word to a new account (DECISION-101).

        Only the kind is sent: the words live in the bot, which delivers nothing else, so a
        leaked secret cannot turn it into something that sends arbitrary text.
        """
        self._post({"phone": phone, "kind": "welcome"})

    def _post(self, payload: dict[str, str]) -> None:
        if not (self.base_url and self.token):
            raise TransientOtpError("WhatsApp bot is not configured")

        body = json.dumps(payload).encode("utf-8")
        headers = {
            # A shared secret, because the bot sends WhatsApp messages to anyone who asks it to.
            "Authorization": f"Bearer {self.token}",
            "Content-Type": "application/json",
        }

        try:
            status, raw = self._open(f"{self.base_url}/send", body, headers)
        except OSError as exc:
            raise TransientOtpError("WhatsApp bot is unreachable") from exc

        if status < 300:
            return

        reason = _reason(raw)
        logger.warning("otp.whatsapp_bot_rejected", extra={"status": status, "reason": reason})
        if reason in PERMANENT_REASONS:
            raise InvalidRecipient(f"the number cannot receive WhatsApp ({reason})")
        raise TransientOtpError(f"WhatsApp bot returned {status}")


def _reason(raw: bytes) -> str | None:
    """The bot's own word for what went wrong, when it gave one."""
    try:
        body = json.loads(raw.decode("utf-8"))
    except (ValueError, UnicodeDecodeError):
        return None
    value = body.get("reason") if isinstance(body, dict) else None
    return value if isinstance(value, str) else None
