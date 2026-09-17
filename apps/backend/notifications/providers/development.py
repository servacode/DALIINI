from .base import PushMessage


class DevelopmentPushProvider:
    def send(self, message: PushMessage) -> None:
        return None
