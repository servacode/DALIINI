from __future__ import annotations

from .base import OtpMessage


class DevelopmentOtpSender:
    """Delivers nothing, on purpose.

    Local work and the automated tests set the stored digest directly rather than reading a
    code out of a log. So this neither sends, nor logs, nor returns the code: a development
    build that printed it would be one copied line away from a production build that did.
    """

    def send(self, message: OtpMessage) -> None:
        return None
