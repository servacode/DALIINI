from rest_framework.authentication import BaseAuthentication, get_authorization_header
from rest_framework.exceptions import AuthenticationFailed

from .models import User
from .tokens import decode_access_token


class BearerAccessTokenAuthentication(BaseAuthentication):
    keyword = b"bearer"

    def authenticate(self, request):
        parts = get_authorization_header(request).split()
        if not parts:
            return None
        if len(parts) != 2 or parts[0].lower() != self.keyword:
            raise AuthenticationFailed("Invalid authorization header.")
        try:
            raw = parts[1].decode("ascii")
        except UnicodeDecodeError as exc:
            raise AuthenticationFailed("Invalid authorization header.") from exc
        claims = decode_access_token(raw)
        if not claims:
            raise AuthenticationFailed("Invalid or expired access token.")
        user = User.objects.filter(pk=claims["sub"], is_active=True).first()
        if user is None:
            raise AuthenticationFailed("Account is unavailable.")
        return user, claims
