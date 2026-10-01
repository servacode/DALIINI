from __future__ import annotations

import json
import logging
import urllib.error
import urllib.request
from collections.abc import Callable
from typing import Any

from django.conf import settings

from .base import InvalidRecipient, OtpMessage, TransientOtpError

logger = logging.getLogger(__name__)

# Meta versions the Graph API by date. Pinned, so a new version cannot change the shape of a
# request underneath us; raised deliberately, after reading what changed.
GRAPH_VERSION = "v21.0"
SEND_URL = "https://graph.facebook.com/{version}/{phone_number_id}/messages"
TIMEOUT_SECONDS = 10

# Meta's codes for a recipient that can never receive this message, as opposed to an attempt
# that failed and might succeed next time. From the Cloud API's error reference.
PERMANENT = frozenset(
    {
        131026,  # Undeliverable — the number is not a WhatsApp account.
        131047,  # Outside the re-engagement window; only a template may be sent.
        131051,  # Unsupported message type for this recipient.
        132000,  # The parameters do not match the approved template.
        132001,  # No such template on this account.
        132005,  # The template is not approved in this language.
        133010,  # The sending number is not registered to the business account.
    }
)

# POST (url, body, headers) -> (status, body). Raises OSError when nothing came back.
Opener = Callable[[str, bytes, dict[str, str]], tuple[int, bytes]]


def _urlopen(url: str, body: bytes, headers: dict[str, str]) -> tuple[int, bytes]:
    request = urllib.request.Request(url, data=body, headers=headers, method="POST")
    try:
        with urllib.request.urlopen(request, timeout=TIMEOUT_SECONDS) as response:
            return int(response.status), response.read()
    except urllib.error.HTTPError as error:
        return int(error.code), error.read()


class WhatsAppOtpSender:
    """Sends the code as a WhatsApp authentication template, through Meta's Cloud API.

    **Why a template rather than a plain message.** WhatsApp lets a business open a conversation
    only with a template Meta has approved in advance; free text is allowed solely inside the
    24 hours after the person writes first. Somebody registering has never written to us, so a
    free-text message would never arrive. Meta has a category for exactly this case —
    "Authentication" — whose templates carry the code and a copy button, and it is the only
    supported way to send one.

    **What this file does not decide.** The template's wording is Meta's; we fill one blank. Its
    name and language are configuration, because the approved template belongs to the business
    account rather than to this code.

    **What is deliberately never logged.** Not the code, and not the number. A failure records
    the provider's own error code and the HTTP status, so a log file never becomes a list of who
    registered, when, and with what.
    """

    def __init__(self, opener: Opener | None = None) -> None:
        self._open = opener or _urlopen
        self.phone_number_id = str(getattr(settings, "WHATSAPP_PHONE_NUMBER_ID", "") or "")
        self.access_token = str(getattr(settings, "WHATSAPP_ACCESS_TOKEN", "") or "")
        self.template = str(getattr(settings, "WHATSAPP_TEMPLATE_NAME", "") or "")
        self.language = str(getattr(settings, "WHATSAPP_TEMPLATE_LANGUAGE", "") or "ar")

    def send(self, message: OtpMessage) -> None:
        if not (self.phone_number_id and self.access_token and self.template):
            raise TransientOtpError("WhatsApp sender is not configured")

        url = SEND_URL.format(version=GRAPH_VERSION, phone_number_id=self.phone_number_id)
        body = json.dumps(self._payload(message)).encode("utf-8")
        headers = {
            "Authorization": f"Bearer {self.access_token}",
            "Content-Type": "application/json",
        }

        try:
            status, raw = self._open(url, body, headers)
        except OSError as exc:
            raise TransientOtpError("WhatsApp request failed") from exc

        if status < 300:
            return

        code = _error_code(raw)
        logger.warning("otp.whatsapp_rejected", extra={"status": status, "provider_code": code})
        if code in PERMANENT:
            raise InvalidRecipient(f"WhatsApp refused the recipient ({code})")
        raise TransientOtpError(f"WhatsApp returned {status}")

    def _payload(self, message: OtpMessage) -> dict[str, Any]:
        """The authentication template, with the code in it.

        Meta's authentication templates carry the code twice — once in the body and once as the
        copy button's payload — and both must be sent or the template is refused for a parameter
        mismatch.
        """
        return {
            "messaging_product": "whatsapp",
            "recipient_type": "individual",
            # The API wants digits with no leading plus.
            "to": message.phone.lstrip("+"),
            "type": "template",
            "template": {
                "name": self.template,
                "language": {"code": self.language},
                "components": [
                    {"type": "body", "parameters": [{"type": "text", "text": message.code}]},
                    {
                        "type": "button",
                        "sub_type": "url",
                        "index": "0",
                        "parameters": [{"type": "text", "text": message.code}],
                    },
                ],
            },
        }


def _error_code(raw: bytes) -> int | None:
    """Meta's own numeric code, when the body carries one."""
    try:
        body = json.loads(raw.decode("utf-8"))
    except (ValueError, UnicodeDecodeError):
        return None
    error = body.get("error") if isinstance(body, dict) else None
    if not isinstance(error, dict):
        return None
    value = error.get("code")
    return value if isinstance(value, int) else None
