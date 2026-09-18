"""INT-038: every failure leaves the API in one envelope.

These run against the real URL conf and the real exception handler, so they fail if the
handler is unregistered, if a view starts returning a hand-built error body again, or if
an internal failure starts leaking its text.
"""

from typing import Any

import pytest
from django.urls import path
from rest_framework.exceptions import APIException, Throttled
from rest_framework.response import Response
from rest_framework.test import APIClient, APIRequestFactory
from rest_framework.views import APIView

from core.exceptions import ConflictError, DomainError, exception_handler

ENVELOPE_KEYS = {"code", "message", "details", "requestId"}


def _handle(exc: Exception, request: Any = None) -> Response:
    request = request or APIRequestFactory().get("/")
    request.request_id = "req-under-test"
    return exception_handler(exc, {"request": request})


def test_envelope_shape_is_exactly_the_contract() -> None:
    response = _handle(DomainError("DUTY_NOT_SUPPORTED", message="غير مدعوم."))

    assert response.status_code == 400
    assert set(response.data) == ENVELOPE_KEYS
    assert response.data["code"] == "DUTY_NOT_SUPPORTED"
    assert response.data["message"] == "غير مدعوم."
    assert response.data["details"] == {}
    assert response.data["requestId"] == "req-under-test"


def test_conflict_error_keeps_its_status() -> None:
    assert _handle(ConflictError("LAST_OWNER_PROTECTED")).status_code == 409


def test_validation_detail_is_flattened_to_a_map_of_lists() -> None:
    from rest_framework.exceptions import ValidationError

    response = _handle(
        ValidationError({"contacts": [{}, {"phone": ["INVALID_PHONE"]}], "name": "Required."})
    )

    assert response.status_code == 400
    assert response.data["code"] == "VALIDATION_ERROR"
    assert response.data["details"]["contacts[1].phone"] == ["INVALID_PHONE"]
    assert response.data["details"]["name"] == ["Required."]


def test_non_field_validation_error_has_a_stable_key() -> None:
    from rest_framework.exceptions import ValidationError

    response = _handle(ValidationError("Opening and closing time cannot be equal."))

    assert response.data["details"] == {
        "nonFieldErrors": ["Opening and closing time cannot be equal."]
    }


@pytest.mark.parametrize(
    ("exc", "status", "code"),
    [
        ("NotAuthenticated", 401, "AUTHENTICATION_REQUIRED"),
        ("AuthenticationFailed", 401, "AUTHENTICATION_FAILED"),
        ("PermissionDenied", 403, "PERMISSION_DENIED"),
        ("NotFound", 404, "NOT_FOUND"),
        ("MethodNotAllowed", 405, "METHOD_NOT_ALLOWED"),
        ("NotAcceptable", 406, "NOT_ACCEPTABLE"),
        ("UnsupportedMediaType", 415, "UNSUPPORTED_MEDIA_TYPE"),
    ],
)
def test_every_transport_failure_maps_to_its_code(exc: Any, status: Any, code: Any) -> None:
    from rest_framework import exceptions as drf

    cls = getattr(drf, exc)
    arguments = {"MethodNotAllowed": ("GET",), "UnsupportedMediaType": ("text/csv",)}
    instance = cls(*arguments.get(exc, ()))

    response = _handle(instance)

    assert (response.status_code, response.data["code"]) == (status, code)
    assert set(response.data) == ENVELOPE_KEYS


def test_throttling_reports_retry_after() -> None:
    response = _handle(Throttled(wait=42))

    assert response.status_code == 429
    assert response.data["code"] == "THROTTLED"
    assert response["Retry-After"] == "42"


def test_unexpected_failure_discloses_nothing() -> None:
    secret = "postgres://admin:hunter2@db.internal:5432/directory"

    response = _handle(RuntimeError(f"connection to {secret} failed"))

    assert response.status_code == 500
    assert response.data["code"] == "INTERNAL_ERROR"
    assert response.data["details"] == {}
    body = str(response.data)
    assert secret not in body
    assert "hunter2" not in body
    assert "RuntimeError" not in body
    assert "Traceback" not in body


def test_server_side_api_exception_is_not_echoed() -> None:
    class StorageUnreachable(APIException):
        status_code = 503
        default_detail = "boto3 endpoint https://minio.internal:9000 refused the connection"

    response = _handle(StorageUnreachable())

    assert response.status_code == 500
    assert response.data["code"] == "INTERNAL_ERROR"
    assert "minio.internal" not in str(response.data)


# --------------------------------------------------------------------------------------
# Through the real stack
# --------------------------------------------------------------------------------------


class _Boom(APIView):
    permission_classes = []
    authentication_classes = []

    def get(self, request: Any) -> Response:
        raise ValueError("internal detail that must not travel")


class _Fine(APIView):
    permission_classes = []
    authentication_classes = []

    def get(self, request: Any) -> Response:
        return Response({"ok": True})


urlpatterns = [
    path("boom/", _Boom.as_view()),
    path("fine/", _Fine.as_view()),
]


@pytest.mark.urls("core.tests.test_error_contract")
def test_request_id_matches_the_response_header() -> None:
    client = APIClient(raise_request_exception=False)

    response = client.get("/boom/", HTTP_X_REQUEST_ID="client-supplied-id")

    assert response.status_code == 500
    assert response.json()["requestId"] == "client-supplied-id"
    assert response["X-Request-ID"] == "client-supplied-id"
    assert "internal detail" not in response.content.decode()


@pytest.mark.urls("core.tests.test_error_contract")
def test_request_id_is_generated_when_the_client_sends_none() -> None:
    client = APIClient(raise_request_exception=False)

    response = client.get("/boom/")

    assert response.json()["requestId"] == response["X-Request-ID"]
    assert response.json()["requestId"]


@pytest.mark.django_db
def test_unauthenticated_owner_call_uses_the_envelope() -> None:
    response = APIClient().get("/api/v1/owner/facilities/")

    assert response.status_code == 401
    assert set(response.json()) == ENVELOPE_KEYS
    assert response.json()["code"] == "AUTHENTICATION_REQUIRED"


@pytest.mark.django_db
def test_missing_resource_uses_the_envelope() -> None:
    response = APIClient().get(
        "/api/v1/public/facilities/00000000-0000-0000-0000-000000000000/"
    )

    assert response.status_code == 404
    assert response.json()["code"] == "NOT_FOUND"
    assert set(response.json()) == ENVELOPE_KEYS
