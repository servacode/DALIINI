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

from drf_spectacular.extensions import OpenApiAuthenticationExtension, OpenApiSerializerExtension
from drf_spectacular.plumbing import build_array_type, build_basic_type, build_object_type
from drf_spectacular.types import OpenApiTypes
from drf_spectacular.utils import OpenApiExample, OpenApiResponse, extend_schema_serializer
from rest_framework import serializers


class BearerAccessTokenScheme(OpenApiAuthenticationExtension):
    """Describe `accounts.authentication.BearerAccessTokenAuthentication`.

    The wire format is the short-lived JOSE/JWT access token only. The opaque rotating
    refresh secret is never an Authorization header and is deliberately not described
    as one; it travels in the refresh request body.
    """

    target_class = "accounts.authentication.BearerAccessTokenAuthentication"
    name = "bearerAccessToken"

    def get_security_definition(self, auto_schema):
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
# The project has no custom DRF EXCEPTION_HANDLER, so two shapes reach clients and both
# are described below exactly as they are produced. `08-API-CONTRACT.md` specifies a
# richer single envelope with `code`, `message`, `details` and `requestId`; the runtime
# does not emit that today. Documenting the specified envelope instead of the real one
# would make the contract lie, so the divergence is recorded as a defect rather than
# papered over here.
# --------------------------------------------------------------------------------------


class DomainErrorBodySerializer(serializers.Serializer):
    code = serializers.CharField(
        help_text="Stable machine-readable domain code, for example DUTY_OVERLAP_OR_INVALID."
    )
    details = serializers.DictField(
        required=False,
        help_text="Optional field-scoped detail, present on validation-style domain errors.",
    )


@extend_schema_serializer(
    component_name="DomainError",
    examples=[
        OpenApiExample(
            "duty overlap",
            value={"error": {"code": "DUTY_OVERLAP_OR_INVALID"}},
            response_only=True,
        )
    ],
)
class DomainErrorSerializer(serializers.Serializer):
    """Domain rule rejection: `{"error": {"code": ..., "details": ...}}`."""

    error = DomainErrorBodySerializer()


@extend_schema_serializer(
    component_name="DetailError",
    examples=[
        OpenApiExample(
            "authentication required",
            value={"detail": "Authentication credentials were not provided."},
            response_only=True,
        )
    ],
)
class DetailErrorSerializer(serializers.Serializer):
    """DRF's default single-message body, used for auth, permission, 404 and throttling."""

    detail = serializers.CharField()


@extend_schema_serializer(component_name="FieldValidationError")
class FieldValidationErrorSerializer(serializers.Serializer):
    """DRF's default validation body: a map of field name to messages.

    The shape is `{"<field>": ["<message>", ...]}` with arbitrary keys, so it is emitted
    through `FieldValidationErrorExtension` below rather than as declared fields.
    """


class FieldValidationErrorExtension(OpenApiSerializerExtension):
    target_class = FieldValidationErrorSerializer

    def map_serializer(self, auto_schema, direction):
        schema = build_object_type(
            description=(
                "Field-scoped validation errors. Keys are request field names; values are "
                "the messages for that field. A non-field error is reported under "
                "`non_field_errors`."
            )
        )
        schema["additionalProperties"] = build_array_type(build_basic_type(OpenApiTypes.STR))
        return schema


VALIDATION_400 = OpenApiResponse(
    response=FieldValidationErrorSerializer,
    description="Request validation failed.",
)
DOMAIN_400 = OpenApiResponse(
    response=DomainErrorSerializer,
    description="The request was rejected by a domain rule.",
)
UNAUTHENTICATED_401 = OpenApiResponse(
    response=DetailErrorSerializer,
    description="No valid access token was supplied.",
)
FORBIDDEN_403 = OpenApiResponse(
    response=DetailErrorSerializer,
    description="Authenticated, but the caller lacks the required permission or membership.",
)
NOT_FOUND_404 = OpenApiResponse(
    response=DetailErrorSerializer,
    description="The addressed resource does not exist or is not visible to the caller.",
)
CONFLICT_409 = OpenApiResponse(
    response=DomainErrorSerializer,
    description="The request conflicts with the current state or with a domain rule.",
)
THROTTLED_429 = OpenApiResponse(
    response=DetailErrorSerializer,
    description="Rate limit exceeded for this endpoint.",
)


def protected(*extra):
    """Standard error set for an endpoint that requires authentication."""
    return {401: UNAUTHENTICATED_401, 403: FORBIDDEN_403, **dict(extra)}


# --------------------------------------------------------------------------------------
# Reusable value objects
# --------------------------------------------------------------------------------------


class CoordinatesSerializer(serializers.Serializer):
    """WGS84 / SRID 4326 decimal degrees, as stored by PostGIS."""

    latitude = serializers.FloatField(min_value=-90, max_value=90)
    longitude = serializers.FloatField(min_value=-180, max_value=180)


class NamedRefSerializer(serializers.Serializer):
    """An id plus its Arabic display name, the shape used for inline references."""

    id = serializers.UUIDField()
    nameAr = serializers.CharField()


class BilingualRefSerializer(serializers.Serializer):
    id = serializers.UUIDField()
    nameAr = serializers.CharField()
    nameEn = serializers.CharField(allow_null=True)
