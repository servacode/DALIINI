from rest_framework.request import Request
from rest_framework.throttling import SimpleRateThrottle
from rest_framework.views import APIView

from .phone import normalize_syrian_phone


class PhoneAndIpThrottle(SimpleRateThrottle):
    phone_field = "phone"

    def get_cache_key(self, request: Request, view: APIView) -> str:
        ident = self.get_ident(request)
        raw_phone = str(request.data.get(self.phone_field, ""))
        try:
            phone = normalize_syrian_phone(raw_phone)
        except ValueError:
            phone = "invalid"
        return self.cache_format % {"scope": self.scope, "ident": f"{ident}:{phone}"}


class OtpStartThrottle(PhoneAndIpThrottle):
    scope = "otp_start"


class LoginThrottle(PhoneAndIpThrottle):
    scope = "login"


class RecoveryThrottle(PhoneAndIpThrottle):
    scope = "recovery"


class PhoneOnlyThrottle(SimpleRateThrottle):
    """Count requests per destination number, whatever address they come from.

    The per-address limits above stop one caller hammering one number. They do nothing about
    many addresses taking turns at the same number, and every code costs a message on the
    sending account — which for the paired WhatsApp account is also what gets a number banned.
    A number that is not a valid Syrian mobile is not counted here: validation refuses it
    anyway, and one shared "invalid" bucket would let a stranger's typos lock out everyone
    else's.
    """

    phone_field = "phone"

    def get_cache_key(self, request: Request, view: APIView) -> str | None:
        try:
            phone = normalize_syrian_phone(str(request.data.get(self.phone_field, "")))
        except ValueError:
            return None
        return self.cache_format % {"scope": self.scope, "ident": phone}


class OtpPhoneHourThrottle(PhoneOnlyThrottle):
    scope = "otp_phone_hour"


class OtpPhoneDayThrottle(PhoneOnlyThrottle):
    scope = "otp_phone_day"


#: Every view that sends a code carries these, in addition to its own per-address limit.
OTP_SEND_THROTTLES = [OtpPhoneHourThrottle, OtpPhoneDayThrottle]


class OtpVerifyThrottle(SimpleRateThrottle):
    scope = "otp_verify"

    def get_cache_key(self, request: Request, view: APIView) -> str:
        ident = self.get_ident(request)
        challenge = str(request.data.get("challengeId", "missing"))
        return self.cache_format % {"scope": self.scope, "ident": f"{ident}:{challenge}"}
