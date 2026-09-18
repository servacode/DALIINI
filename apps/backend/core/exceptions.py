"""One error envelope for the whole API.

`08-API-CONTRACT.md` specifies a single shape carrying `code`, `message`, `details` and
`requestId`, and states that clients branch on `code`. Before this module the runtime
emitted three unrelated shapes — DRF's `{"detail": ...}`, DRF's field map, and a
hand-rolled `{"error": {"code": ...}}` — which is INT-038.

Nothing here leaks. Stack traces, SQL, secrets, token material and raw exception text from
unexpected failures never reach the client; an unexpected failure is logged in full on the
server and answered with a bare `INTERNAL_ERROR`.

`requestId` is the value `core.middleware.RequestIdMiddleware` already assigns to the
request and echoes in the `X-Request-ID` response header, so a client-reported id matches
the server logs exactly.
"""

import logging
from typing import Any

from django.core.exceptions import PermissionDenied as DjangoPermissionDenied
from django.core.exceptions import ValidationError as DjangoValidationError
from django.http import Http404
from rest_framework import exceptions as drf
from rest_framework.response import Response
from rest_framework.views import set_rollback

logger = logging.getLogger("django.request")

# Machine-readable codes. Clients branch on these, never on the message text.
VALIDATION_ERROR = "VALIDATION_ERROR"
AUTHENTICATION_REQUIRED = "AUTHENTICATION_REQUIRED"
AUTHENTICATION_FAILED = "AUTHENTICATION_FAILED"
PERMISSION_DENIED = "PERMISSION_DENIED"
NOT_FOUND = "NOT_FOUND"
METHOD_NOT_ALLOWED = "METHOD_NOT_ALLOWED"
NOT_ACCEPTABLE = "NOT_ACCEPTABLE"
UNSUPPORTED_MEDIA_TYPE = "UNSUPPORTED_MEDIA_TYPE"
THROTTLED = "THROTTLED"
INTERNAL_ERROR = "INTERNAL_ERROR"

_MESSAGES = {
    VALIDATION_ERROR: "تعذر حفظ البيانات.",
    AUTHENTICATION_REQUIRED: "يلزم تسجيل الدخول.",
    AUTHENTICATION_FAILED: "تعذر التحقق من بيانات الدخول.",
    PERMISSION_DENIED: "لا تملك صلاحية تنفيذ هذا الإجراء.",
    NOT_FOUND: "العنصر المطلوب غير موجود.",
    METHOD_NOT_ALLOWED: "طريقة الطلب غير مدعومة لهذا المسار.",
    NOT_ACCEPTABLE: "صيغة الاستجابة المطلوبة غير مدعومة.",
    UNSUPPORTED_MEDIA_TYPE: "نوع محتوى الطلب غير مدعوم.",
    THROTTLED: "تم تجاوز الحد المسموح من المحاولات. حاول لاحقاً.",
    INTERNAL_ERROR: "حدث خطأ غير متوقع. تم تسجيل الحادثة.",
}

_BY_EXCEPTION = {
    drf.NotAuthenticated: (AUTHENTICATION_REQUIRED, 401),
    drf.AuthenticationFailed: (AUTHENTICATION_FAILED, 401),
    drf.PermissionDenied: (PERMISSION_DENIED, 403),
    drf.NotFound: (NOT_FOUND, 404),
    drf.MethodNotAllowed: (METHOD_NOT_ALLOWED, 405),
    drf.NotAcceptable: (NOT_ACCEPTABLE, 406),
    drf.UnsupportedMediaType: (UNSUPPORTED_MEDIA_TYPE, 415),
    drf.Throttled: (THROTTLED, 429),
}


class DomainError(drf.APIException):
    """A business rule rejected the request.

    Raised instead of returning a bare Response so the rejection travels through the same
    envelope as every other error, and so the surrounding transaction is rolled back.
    """

    status_code = 400
    default_code = "DOMAIN_ERROR"
    default_message = "تعذر تنفيذ الطلب وفق قواعد العمل."

    def __init__(
        self,
        code: str,
        *,
        message: str | None = None,
        details: Any = None,
        status_code: int | None = None,
    ) -> None:
        self.code = code
        self.message = message or self.default_message
        self.details = details
        if status_code is not None:
            self.status_code = status_code
        super().__init__(detail=self.message, code=code)


class ConflictError(DomainError):
    """A domain rejection that reflects current state rather than a malformed request."""

    status_code = 409


def _normalise_details(detail: Any) -> dict[str, list[str]] | None:
    """Flatten any DRF or Django detail into a stable `{path: [message, ...]}` map.

    DRF hands back nested structures for nested and `many=True` serializers. Clients need
    one predictable shape, so a nested path is joined with dots and a list index is
    bracketed: `contacts[1].phone`. A message with no field of its own lands under
    `nonFieldErrors`.
    """
    if detail is None:
        return None
    return _flatten(detail, "")


def _flatten(detail: Any, prefix: str) -> dict[str, list[str]]:
    out: dict[str, list[str]] = {}
    if isinstance(detail, dict):
        for key, value in detail.items():
            path = f"{prefix}.{key}" if prefix else str(key)
            out.update(_flatten(value, path))
    elif isinstance(detail, list | tuple):
        if all(not isinstance(item, dict | list | tuple) for item in detail):
            out[prefix or "nonFieldErrors"] = [str(item) for item in detail]
        else:
            for index, item in enumerate(detail):
                out.update(_flatten(item, f"{prefix}[{index}]"))
    else:
        out[prefix or "nonFieldErrors"] = [str(detail)]
    return out


def _envelope(
    code: str,
    message: str,
    details: dict[str, list[str]] | None,
    request: Any,
) -> dict[str, Any]:
    body = {
        "code": code,
        "message": message,
        "details": details if details is not None else {},
        "requestId": getattr(request, "request_id", "") if request is not None else "",
    }
    return body


def exception_handler(exc: Exception, context: dict[str, Any]) -> Response:
    """Render every failure as the single envelope the contract declares."""
    request = context.get("request") if context else None

    if isinstance(exc, Http404):
        exc = drf.NotFound()
    elif isinstance(exc, DjangoPermissionDenied):
        exc = drf.PermissionDenied()
    elif isinstance(exc, DjangoValidationError):
        detail = getattr(exc, "message_dict", None) or {"nonFieldErrors": exc.messages}
        exc = drf.ValidationError(detail)

    if isinstance(exc, DomainError):
        set_rollback()
        return Response(
            _envelope(exc.code, exc.message, _normalise_details(exc.details), request),
            status=exc.status_code,
        )

    if isinstance(exc, drf.ValidationError):
        set_rollback()
        return Response(
            _envelope(
                VALIDATION_ERROR,
                _MESSAGES[VALIDATION_ERROR],
                _normalise_details(exc.detail),
                request,
            ),
            status=400,
        )

    for exception_type, (code, status) in _BY_EXCEPTION.items():
        if isinstance(exc, exception_type):
            set_rollback()
            response = Response(
                _envelope(code, _MESSAGES[code], None, request), status=status
            )
            # `wait` is set by Throttled.__init__ but absent from the DRF stubs.
            wait = getattr(exc, "wait", None)
            if wait is not None:
                response["Retry-After"] = str(int(wait))
            return response

    if isinstance(exc, drf.APIException) and exc.status_code < 500:
        # Any other DRF exception, including custom ones declared elsewhere. Its detail is
        # written to be shown to a caller, so it is passed through. A 5xx APIException is
        # deliberately excluded: its detail describes a server fault and falls through to
        # the bare envelope below.
        set_rollback()
        code = str(getattr(exc, "default_code", "API_ERROR")).upper()
        return Response(
            _envelope(code, str(exc.detail), _normalise_details(exc.detail), request),
            status=exc.status_code,
        )

    # Unexpected failure. Everything DRF itself knows how to render is an APIException,
    # an Http404 or a Django PermissionDenied, and all three are handled above, so anything
    # reaching here is a genuine bug. The traceback goes to the server log under
    # `django.request`, correlated by request id; the caller gets a bare envelope with no
    # exception text, no stack frame and no SQL.
    logger.exception(
        "Unhandled exception while serving the API",
        extra={"request_id": getattr(request, "request_id", "")},
    )
    return Response(
        _envelope(INTERNAL_ERROR, _MESSAGES[INTERNAL_ERROR], None, request),
        status=500,
    )
