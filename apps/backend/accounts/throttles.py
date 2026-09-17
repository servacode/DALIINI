from rest_framework.throttling import SimpleRateThrottle

from .phone import normalize_syrian_phone


class PhoneAndIpThrottle(SimpleRateThrottle):
    phone_field = "phone"

    def get_cache_key(self, request, view):
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


class OtpVerifyThrottle(SimpleRateThrottle):
    scope = "otp_verify"

    def get_cache_key(self, request, view):
        ident = self.get_ident(request)
        challenge = str(request.data.get("challengeId", "missing"))
        return self.cache_format % {"scope": self.scope, "ident": f"{ident}:{challenge}"}
