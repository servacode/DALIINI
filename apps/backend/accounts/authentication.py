from typing import TYPE_CHECKING, Any

from django.core.exceptions import ValidationError as DjangoValidationError
from django.db.models import QuerySet
from django.utils import timezone
from rest_framework.authentication import BaseAuthentication, get_authorization_header
from rest_framework.exceptions import AuthenticationFailed
from rest_framework.request import Request

from accounts.models import User
from sessions.models import UserSession

from .tokens import decode_access_token

if TYPE_CHECKING:

    class AuthenticatedRequest(Request):
        """The request a view behind IsAuthenticated receives: its user is an account.

        For the type checker only. At runtime the name is DRF's own Request.
        """

        user: User

else:
    AuthenticatedRequest = Request


def live_sessions_for(claims: dict[str, Any]) -> QuerySet[UserSession]:
    """The session an access token was issued for, if it may still act.

    Shared by the REST authentication and the WebSocket consumer, so that revoking a
    session, or detecting a refresh replay, stops both at once.
    """
    return UserSession.objects.select_related("user").filter(
        pk=claims["sid"],
        user_id=claims["sub"],
        user__is_active=True,
        revoked_at__isnull=True,
        compromised_at__isnull=True,
        expires_at__gt=timezone.now(),
    )


class BearerAccessTokenAuthentication(BaseAuthentication):
    keyword = b"bearer"

    def authenticate(self, request: Request) -> tuple[User, dict[str, Any]] | None:
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
        try:
            session = live_sessions_for(claims).first()
        except (TypeError, ValueError, DjangoValidationError) as exc:
            raise AuthenticationFailed("Invalid or expired access token.") from exc
        if session is None:
            raise AuthenticationFailed("Session is no longer active.")
        # Kept for the console's second-step check (accounts/mfa.py), which needs to know
        # whether this particular session passed it.
        request.user_session = session  # type: ignore[attr-defined]
        return session.user, claims
