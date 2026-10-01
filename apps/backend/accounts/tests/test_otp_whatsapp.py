from __future__ import annotations

import json

import pytest
from django.core.exceptions import ImproperlyConfigured
from django.test import override_settings

from accounts.providers.base import InvalidRecipient, OtpMessage, TransientOtpError
from accounts.providers.development import DevelopmentOtpSender
from accounts.providers.factory import get_otp_sender
from accounts.providers.whatsapp import WhatsAppOtpSender

"""
The WhatsApp sender, proven without a network.

Every case here drives the sender through an opener of its own, so what is tested is the
request we build and the verdicts we draw from an answer — never Meta's availability. The one
thing a test cannot prove is that a message arrives; that is the staging run's job.
"""

CONFIGURED = {
    "OTP_PROVIDER": "whatsapp",
    "WHATSAPP_PHONE_NUMBER_ID": "123456",
    "WHATSAPP_ACCESS_TOKEN": "token-abc",
    "WHATSAPP_TEMPLATE_NAME": "daliini_otp",
    "WHATSAPP_TEMPLATE_LANGUAGE": "ar",
}

MESSAGE = OtpMessage(phone="+963933000000", code="482913")


class Recorder:
    """An opener that answers whatever the test says and keeps what it was asked."""

    def __init__(self, status: int = 200, body: object = None, raises: Exception | None = None):
        self.status = status
        self.body = body if body is not None else {"messages": [{"id": "wamid.x"}]}
        self.raises = raises
        self.calls: list[tuple[str, dict, dict[str, str]]] = []

    def __call__(self, url: str, body: bytes, headers: dict[str, str]):
        if self.raises:
            raise self.raises
        self.calls.append((url, json.loads(body.decode("utf-8")), headers))
        return self.status, json.dumps(self.body).encode("utf-8")


@override_settings(**CONFIGURED)
def test_it_sends_the_approved_template_with_the_code_in_both_places():
    opener = Recorder()
    WhatsAppOtpSender(opener=opener).send(MESSAGE)

    assert len(opener.calls) == 1
    url, body, headers = opener.calls[0]

    assert url == "https://graph.facebook.com/v21.0/123456/messages"
    assert headers["Authorization"] == "Bearer token-abc"
    assert body["messaging_product"] == "whatsapp"
    # The API wants digits with no leading plus.
    assert body["to"] == "963933000000"
    assert body["type"] == "template"
    assert body["template"]["name"] == "daliini_otp"
    assert body["template"]["language"] == {"code": "ar"}

    components = body["template"]["components"]
    # Meta's authentication templates carry the code in the body and again as the copy
    # button's payload; sending only one is a parameter mismatch and the template is refused.
    assert components[0]["parameters"][0]["text"] == "482913"
    assert components[1]["sub_type"] == "url"
    assert components[1]["parameters"][0]["text"] == "482913"


@override_settings(**CONFIGURED)
@pytest.mark.parametrize("code", [131026, 131047, 132001, 132005, 133010])
def test_a_number_that_can_never_receive_is_not_retried(code: int):
    opener = Recorder(status=400, body={"error": {"code": code, "message": "nope"}})
    with pytest.raises(InvalidRecipient):
        WhatsAppOtpSender(opener=opener).send(MESSAGE)


@override_settings(**CONFIGURED)
@pytest.mark.parametrize("status", [429, 500, 502, 503])
def test_a_failure_that_might_pass_next_time_is_transient(status: int):
    opener = Recorder(status=status, body={"error": {"code": 1, "message": "later"}})
    with pytest.raises(TransientOtpError):
        WhatsAppOtpSender(opener=opener).send(MESSAGE)


@override_settings(**CONFIGURED)
def test_a_dead_connection_is_transient_rather_than_a_crash():
    opener = Recorder(raises=OSError("connection refused"))
    with pytest.raises(TransientOtpError):
        WhatsAppOtpSender(opener=opener).send(MESSAGE)


@override_settings(**CONFIGURED)
def test_an_unreadable_body_does_not_become_a_permanent_refusal():
    # Without a readable code there is no evidence the number is at fault, so the attempt is
    # treated as one that might work again rather than as a number to give up on.
    class Garbage(Recorder):
        def __call__(self, url, body, headers):
            return 400, b"<html>gateway</html>"

    with pytest.raises(TransientOtpError):
        WhatsAppOtpSender(opener=Garbage()).send(MESSAGE)


@override_settings(OTP_PROVIDER="whatsapp", WHATSAPP_PHONE_NUMBER_ID="", WHATSAPP_ACCESS_TOKEN="")
def test_an_unconfigured_sender_refuses_before_reaching_the_network():
    opener = Recorder()
    with pytest.raises(TransientOtpError):
        WhatsAppOtpSender(opener=opener).send(MESSAGE)
    assert opener.calls == []


@override_settings(**CONFIGURED)
def test_the_code_never_reaches_the_logs(caplog):
    opener = Recorder(status=400, body={"error": {"code": 131026}})
    with caplog.at_level("WARNING"):
        with pytest.raises(InvalidRecipient):
            WhatsAppOtpSender(opener=opener).send(MESSAGE)

    # The message and every field attached to it, because a code can hide in either.
    written = "\n".join(
        record.getMessage() + " " + " ".join(f"{k}={v}" for k, v in vars(record).items())
        for record in caplog.records
    )
    assert "482913" not in written
    assert "963933000000" not in written
    # What it does record is the provider's own verdict, which is what a person debugging needs.
    assert "131026" in written


def test_the_factory_picks_the_sender_the_deployment_names():
    with override_settings(OTP_PROVIDER="development"):
        assert isinstance(get_otp_sender(), DevelopmentOtpSender)
    with override_settings(**CONFIGURED):
        assert isinstance(get_otp_sender(), WhatsAppOtpSender)


def test_an_unknown_provider_fails_loudly_rather_than_delivering_nothing():
    with override_settings(OTP_PROVIDER="carrier-pigeon"):
        with pytest.raises(ImproperlyConfigured):
            get_otp_sender()


def test_the_development_sender_delivers_and_reveals_nothing(caplog):
    with caplog.at_level("DEBUG"):
        DevelopmentOtpSender().send(MESSAGE)
    assert "482913" not in caplog.text
