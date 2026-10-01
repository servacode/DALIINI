from __future__ import annotations

from dataclasses import dataclass
from typing import Protocol


@dataclass(frozen=True)
class OtpMessage:
    """A code on its way to one person.

    `phone` is E.164 as the account stores it (`+9639XXXXXXXX`); `code` is the six digits the
    caller generated and will compare against later. Nothing here knows how the code was made or
    how it will be checked — a sender only delivers.
    """

    phone: str
    code: str


class InvalidRecipient(Exception):
    """The provider says this number cannot receive the message at all.

    Not a transport failure: the number has no WhatsApp account, or is malformed, or the
    provider refuses it. Retrying delivers nothing, so the caller tells the person rather than
    queueing another attempt.
    """


class TransientOtpError(Exception):
    """A temporary failure — a timeout, a 5xx, a rate limit. The send may be retried."""


class OtpSender(Protocol):
    def send(self, message: OtpMessage) -> None:
        """Deliver the code.

        Raises `InvalidRecipient` when the number can never receive it, `TransientOtpError`
        when the attempt failed but another might work. Returning means the provider accepted
        it for delivery — never that it arrived, which no provider can promise.
        """
        ...
