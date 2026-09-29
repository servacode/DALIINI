"""Shared OpenAPI contract infrastructure.

Everything here describes what the runtime already does. Nothing in this module changes
behaviour: it exists so drf-spectacular can emit a contract that generated clients can
consume without anyone hand-authoring transport DTOs.

Operation id convention, chosen here and applied through every `@extend_schema`:

    <area><Resource><Action>   in lowerCamelCase

    publicFacilitiesList · publicFacilityRetrieve · ownerFacilitySubmit ·
    adminReviewApprove · authRegisterStart

The area prefix is the API family, not the Python class name, so renaming a view never
changes the public contract.
"""

from typing import TYPE_CHECKING, Any

from drf_spectacular.extensions import OpenApiAuthenticationExtension
from drf_spectacular.utils import OpenApiExample, OpenApiResponse, extend_schema_serializer
from rest_framework import serializers

if TYPE_CHECKING:
    from drf_spectacular.openapi import AutoSchema


# drf-spectacular's extension base registers subclasses through an unannotated
# __init_subclass__, which strict mode reports at every subclass definition.
class BearerAccessTokenScheme(OpenApiAuthenticationExtension):  # type: ignore[no-untyped-call]
    """Describe `accounts.authentication.BearerAccessTokenAuthentication`.

    The wire format is the short-lived JOSE/JWT access token only. The opaque rotating
    refresh secret is never an Authorization header and is deliberately not described
    as one; it travels in the refresh request body.
    """

    target_class = "accounts.authentication.BearerAccessTokenAuthentication"
    name = "bearerAccessToken"

    def get_security_definition(self, auto_schema: "AutoSchema") -> dict[str, Any]:
        return {
            "type": "http",
            "scheme": "bearer",
            "bearerFormat": "JWT",
            "description": (
                "Short-lived access token issued by the auth endpoints. Send it as "
                "`Authorization: Bearer <accessToken>`. The rotating refresh secret is "
                "not accepted here."
            ),
        }


# --------------------------------------------------------------------------------------
# Error contract
#
# `core.exceptions.exception_handler` is installed as the DRF `EXCEPTION_HANDLER`, so every
# failure leaves the API in the one envelope `08-API-CONTRACT.md` specifies: validation,
# authentication, permission, not found, domain conflict, throttling and unexpected server
# errors alike. There is no second shape left to document. The schemas below describe that
# envelope; the earlier `{"error": {...}}` and `{"detail": ...}` bodies no longer exist.
# --------------------------------------------------------------------------------------


@extend_schema_serializer(
    component_name="ApiError",
    examples=[
        OpenApiExample(
            "validation",
            value={
                "code": "VALIDATION_ERROR",
                "message": "تعذر حفظ البيانات.",
                "details": {"phone": ["Enter a valid Syrian mobile number."]},
                "requestId": "6f1c2b90-4f1a-4a8e-9a2a-0d5f2f6f9c11",
            },
            response_only=True,
        ),
        OpenApiExample(
            "domain conflict",
            value={
                "code": "DUTY_OVERLAP_OR_INVALID",
                "message": "الوردية تتعارض مع وردية أخرى أو بياناتها غير صالحة.",
                "details": {},
                "requestId": "6f1c2b90-4f1a-4a8e-9a2a-0d5f2f6f9c11",
            },
            response_only=True,
        ),
    ],
)
class ApiErrorSerializer(serializers.Serializer[Any]):
    """The single error envelope returned by every failing request."""

    code = serializers.CharField(
        help_text=(
            "Stable machine-readable code. Clients branch on this and never on `message`. "
            "Transport codes are VALIDATION_ERROR, AUTHENTICATION_REQUIRED, "
            "AUTHENTICATION_FAILED, PERMISSION_DENIED, NOT_FOUND, METHOD_NOT_ALLOWED, "
            "NOT_ACCEPTABLE, UNSUPPORTED_MEDIA_TYPE, THROTTLED and INTERNAL_ERROR; domain "
            "codes such as DUTY_OVERLAP_OR_INVALID are documented on the operations that "
            "raise them."
        )
    )
    message = serializers.CharField(
        help_text="Human-readable Arabic message, safe to display. Never parsed by clients."
    )
    details = serializers.DictField(
        child=serializers.ListField(child=serializers.CharField()),
        help_text=(
            "Field-scoped messages keyed by request field path. Nested paths are joined "
            "with dots and list indices are bracketed, for example `contacts[1].phone`. A "
            "message with no field of its own appears under `nonFieldErrors`. Empty when "
            "the error is not field-scoped."
        ),
    )
    requestId = serializers.CharField(
        allow_blank=True,
        help_text=(
            "The request correlation id, identical to the `X-Request-ID` response header "
            "and to the id recorded in the server logs. Quote it in a support report."
        ),
    )


VALIDATION_400 = OpenApiResponse(
    response=ApiErrorSerializer,
    description="Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.",
)
DOMAIN_400 = OpenApiResponse(
    response=ApiErrorSerializer,
    description="A domain rule rejected the request; `code` names the rule.",
)
UNAUTHENTICATED_401 = OpenApiResponse(
    response=ApiErrorSerializer,
    description="No valid access token was supplied.",
)
FORBIDDEN_403 = OpenApiResponse(
    response=ApiErrorSerializer,
    description="Authenticated, but the caller lacks the required permission or membership.",
)
NOT_FOUND_404 = OpenApiResponse(
    response=ApiErrorSerializer,
    description="The addressed resource does not exist or is not visible to the caller.",
)
CONFLICT_409 = OpenApiResponse(
    response=ApiErrorSerializer,
    description="The request conflicts with the current state or with a domain rule.",
)
THROTTLED_429 = OpenApiResponse(
    response=ApiErrorSerializer,
    description="Rate limit exceeded for this endpoint; see the `Retry-After` header.",
)
SERVER_ERROR_500 = OpenApiResponse(
    response=ApiErrorSerializer,
    description=(
        "An unexpected failure. `code` is INTERNAL_ERROR and no internal detail is "
        "disclosed; the incident is identified by `requestId` in the server logs."
    ),
)


def protected(*extra: tuple[int, OpenApiResponse]) -> dict[int, OpenApiResponse]:
    """Standard error set for an endpoint that requires authentication."""
    return {401: UNAUTHENTICATED_401, 403: FORBIDDEN_403, **dict(extra)}


# --------------------------------------------------------------------------------------
# Reusable value objects
# --------------------------------------------------------------------------------------


class CoordinatesSerializer(serializers.Serializer[Any]):
    """WGS84 / SRID 4326 decimal degrees, as stored by PostGIS."""

    latitude = serializers.FloatField(min_value=-90, max_value=90)
    longitude = serializers.FloatField(min_value=-180, max_value=180)


class NamedRefSerializer(serializers.Serializer[Any]):
    """An id plus its Arabic display name, the shape used for inline references."""

    id = serializers.UUIDField()
    nameAr = serializers.CharField()


class BilingualRefSerializer(serializers.Serializer[Any]):
    id = serializers.UUIDField()
    nameAr = serializers.CharField()
    nameEn = serializers.CharField(allow_null=True)
