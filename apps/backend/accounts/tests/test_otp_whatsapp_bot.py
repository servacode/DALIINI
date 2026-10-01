from __future__ import annotations

import json
from typing import Any

import pytest
from django.test import override_settings

from accounts.providers.base import InvalidRecipient, OtpMessage, TransientOtpError
from accounts.providers.factory import get_otp_sender
from accounts.providers.whatsapp_bot import WhatsAppBotOtpSender

"""
The bot sender, proven without a bot.

The service on the other end holds a real WhatsApp session, so no test may reach it: every case
here drives the sender through an opener of its own and checks the request we post and the
verdict we draw from each answer the bot can give. That the bot's own pacing and session rules
hold is proven separately, in services/whatsapp-bot/test.

The verdicts matter more here than for the official route. This account can be banned without
warning, and the difference between "this number cannot receive WhatsApp" and "try again" is
what decides whether a person is turned away or asked to wait.
"""

CONFIGURED = {
    "OTP_PROVIDER": "whatsapp_bot",
    "WHATSAPP_BOT_URL": "http://whatsapp-bot:8085",
    "WHATSAPP_BOT_TOKEN": "bot-secret",
}

MESSAGE = OtpMessage(phone="+963933000000", code="482913")


class Recorder:
    """An opener that answers whatever the test says and keeps what it was asked."""

    def __init__(
        self, status: int = 200, body: object = None, raises: Exception | None = None
    ) -> None:
        self.status = status
        self.body = body if body is not None else {"sent": True}
        self.raises = raises
        self.calls: list[tuple[str, dict[str, Any], dict[str, str]]] = []

    def __call__(self, url: str, body: bytes, headers: dict[str, str]) -> tuple[int, bytes]:
        if self.raises:
            raise self.raises
        self.calls.append((url, json.loads(body.decode("utf-8")), headers))
        return self.status, json.dumps(self.body).encode("utf-8")


@override_settings(**CONFIGURED)
def test_it_posts_the_number_and_the_code_to_the_bot() -> None:
    opener = Recorder()
    WhatsAppBotOtpSender(opener=opener).send(MESSAGE)

    assert len(opener.calls) == 1
    url, body, headers = opener.calls[0]

    assert url == "http://whatsapp-bot:8085/send"
    # The shared secret, because the bot sends WhatsApp messages to any number it is given.
    assert headers["Authorization"] == "Bearer bot-secret"
    assert headers["Content-Type"] == "application/json"
    assert body == {"phone": "+963933000000", "code": "482913"}


@override_settings(**CONFIGURED | {"WHATSAPP_BOT_URL": "http://whatsapp-bot:8085/"})
def test_a_trailing_slash_in_the_setting_does_not_become_a_double_slash() -> None:
    opener = Recorder()
    WhatsAppBotOtpSender(opener=opener).send(MESSAGE)
    assert opener.calls[0][0] == "http://whatsapp-bot:8085/send"


@override_settings(**CONFIGURED)
@pytest.mark.parametrize("status", [200, 201, 202])
def test_any_accepted_answer_is_success(status: int) -> None:
    WhatsAppBotOtpSender(opener=Recorder(status=status)).send(MESSAGE)


@override_settings(**CONFIGURED)
@pytest.mark.parametrize("reason", ["not_on_whatsapp", "invalid_number"])
def test_a_number_that_can_never_receive_is_not_retried(reason: str) -> None:
    # Asking again changes nothing, and each attempt is one more message from an account that
    # can be banned for exactly this pattern.
    opener = Recorder(status=422, body={"reason": reason})
    with pytest.raises(InvalidRecipient):
        WhatsAppBotOtpSender(opener=opener).send(MESSAGE)


@override_settings(**CONFIGURED)
@pytest.mark.parametrize(
    ("status", "reason"),
    [
        (503, "disconnected"),
        (503, "logged_out"),
        (429, "hourly_cap"),
        (429, "too_soon_for_this_number"),
        (502, "send_failed"),
        # A code we malformed ourselves: a bug on our side, never a verdict on the number.
        (400, "invalid_code"),
        (401, "unauthorised"),
        (500, None),
    ],
)
def test_everything_else_is_transient(status: int, reason: str | None) -> None:
    opener = Recorder(status=status, body={"reason": reason} if reason else {})
    with pytest.raises(TransientOtpError):
        WhatsAppBotOtpSender(opener=opener).send(MESSAGE)


@override_settings(**CONFIGURED)
def test_a_logged_out_session_fails_loudly_rather_than_claiming_success() -> None:
    # The whole point of the bot answering 503: a person is told the code could not be sent,
    # instead of waiting for one that nothing will ever deliver.
    opener = Recorder(status=503, body={"reason": "logged_out"})
    with pytest.raises(TransientOtpError):
        WhatsAppBotOtpSender(opener=opener).send(MESSAGE)


@override_settings(**CONFIGURED)
def test_a_bot_that_is_not_running_is_transient_rather_than_a_crash() -> None:
    opener = Recorder(raises=OSError("connection refused"))
    with pytest.raises(TransientOtpError):
        WhatsAppBotOtpSender(opener=opener).send(MESSAGE)


@override_settings(**CONFIGURED)
def test_an_unreadable_body_does_not_become_a_permanent_refusal() -> None:
    # Without a readable reason there is no evidence the number is at fault, so the attempt is
    # treated as one that might work again rather than as a number to give up on.
    class Garbage(Recorder):
        def __call__(self, url: str, body: bytes, headers: dict[str, str]) -> tuple[int, bytes]:
            return 502, b"<html>bad gateway</html>"

    with pytest.raises(TransientOtpError):
        WhatsAppBotOtpSender(opener=Garbage()).send(MESSAGE)


@override_settings(**CONFIGURED | {"WHATSAPP_BOT_TOKEN": ""})
def test_an_unconfigured_sender_refuses_before_reaching_the_network() -> None:
    opener = Recorder()
    with pytest.raises(TransientOtpError):
        WhatsAppBotOtpSender(opener=opener).send(MESSAGE)
    assert opener.calls == []


@override_settings(**CONFIGURED | {"WHATSAPP_BOT_URL": ""})
def test_a_sender_with_no_address_refuses_too() -> None:
    opener = Recorder()
    with pytest.raises(TransientOtpError):
        WhatsAppBotOtpSender(opener=opener).send(MESSAGE)
    assert opener.calls == []


@override_settings(**CONFIGURED)
def test_neither_the_code_nor_the_number_reaches_the_logs(caplog: pytest.LogCaptureFixture) -> None:
    opener = Recorder(status=422, body={"reason": "not_on_whatsapp"})
    with caplog.at_level("WARNING"):
        with pytest.raises(InvalidRecipient):
            WhatsAppBotOtpSender(opener=opener).send(MESSAGE)

    # The message and every field attached to it, because either can carry a code.
    written = "\n".join(
        record.getMessage() + " " + " ".join(f"{k}={v}" for k, v in vars(record).items())
        for record in caplog.records
    )
    assert "482913" not in written
    assert "963933000000" not in written
    # What it does record is the bot's own verdict, which is what a person debugging needs.
    assert "not_on_whatsapp" in written
    assert "422" in written


@override_settings(**CONFIGURED)
def test_the_token_is_never_written_to_the_logs(caplog: pytest.LogCaptureFixture) -> None:
    opener = Recorder(status=401, body={"reason": "unauthorised"})
    with caplog.at_level("WARNING"):
        with pytest.raises(TransientOtpError):
            WhatsAppBotOtpSender(opener=opener).send(MESSAGE)
    assert "bot-secret" not in caplog.text


def test_the_factory_picks_the_bot_when_the_deployment_names_it() -> None:
    with override_settings(**CONFIGURED):
        assert isinstance(get_otp_sender(), WhatsAppBotOtpSender)
