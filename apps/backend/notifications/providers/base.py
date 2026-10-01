from __future__ import annotations

from dataclasses import dataclass
from typing import Protocol


@dataclass(frozen=True)
class PushMessage:
    token: str
    title: str
    body: str
    data: dict[str, str]


class InvalidPushToken(Exception):
    """The provider says the token is no longer registered to any app instance."""


class TransientPushError(Exception):
    """A temporary transport failure (timeout, 5xx, quota); the send may be retried."""


class PushProvider(Protocol):
    def send(self, message: PushMessage) -> None:
        """Deliver the message, or raise InvalidPushToken when the provider rejects the token."""
        ...
