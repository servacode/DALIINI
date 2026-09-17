from __future__ import annotations

from dataclasses import dataclass
from typing import Protocol


@dataclass(frozen=True)
class PushMessage:
    token: str
    title: str
    body: str
    data: dict[str, str]


class PushProvider(Protocol):
    def send(self, message: PushMessage) -> None: ...
