"""INT-036: the Admin API speaks camelCase, including the endpoints fed by `values()`.

Two layers. The structural test fixes the rule for every current and future response
serializer in this app, so a new `values()` endpoint cannot reintroduce column names. The
connected tests prove the rule holds in an actual HTTP response, which is what the earlier
source-text checks could not do.
"""

from __future__ import annotations

import inspect
from collections.abc import Iterator
from typing import Any

import pytest
from rest_framework import serializers
from rest_framework.test import APIClient

from accounts.models import AdminPermission, AdminRole, UserAdminRole
from admin_console import schemas
from content_services.models import Advertisement
from directory.models import VerificationRequirement
from facilities.models import Facility
from locations.models import Province

# `AdminCapabilities*` still declares `supports_*` on both the request and the response.
# It is not a list endpoint, so it is outside this batch; it is registered as INT-039 in
# artifacts/evidence/RECEIPT-AUDIT-2026-09-17.md rather than silently exempted.
KNOWN_SNAKE_CASE = {
    "AdminCapabilitiesRequestSerializer",
    "AdminCapabilitiesSerializer",
}


def _response_serializers() -> Iterator[tuple[str, type[serializers.Serializer[Any]]]]:
    for name, obj in vars(schemas).items():
        if not inspect.isclass(obj) or not issubclass(obj, serializers.Serializer):
            continue
        if obj is serializers.Serializer or name.endswith("RequestSerializer"):
            continue
        if name in KNOWN_SNAKE_CASE:
            continue
        yield name, obj


@pytest.mark.parametrize(("name", "serializer"), list(_response_serializers()))
def test_no_response_field_carries_a_column_name(
    name: str, serializer: type[serializers.Serializer[Any]]
) -> None:
    fields: dict[str, Any] = dict(serializer().fields)
    offenders = [name for name in fields if "_" in name]

    assert offenders == [], f"{name} exposes snake_case on the wire: {offenders}"


@pytest.fixture
def admin_client(db: Any, user: Any) -> APIClient:
    role = AdminRole.objects.create(code="qa", name="QA")
    for code in (
        "admin.taxonomy.read",
        "admin.provinces.read",
        "admin.provinces.manage",
        "admin.verification.read",
        "admin.ads.read",
        "admin.settings.read",
        "admin.audit.read",
        "admin.dashboard.read",
    ):
        permission, _ = AdminPermission.objects.get_or_create(code=code)
        role.permissions.add(permission)
    UserAdminRole.objects.create(user=user, role=role, active=True)
    client = APIClient()
    client.force_authenticate(user=user)
    return client


def _keys(items: Any) -> set[str]:
    return {key for item in items for key in item}


@pytest.mark.django_db
def test_category_list_is_camel_case(admin_client: APIClient, facility: Facility) -> None:
    body = admin_client.get("/api/v1/admin/categories/").json()

    keys = _keys(body["items"])
    assert keys == {
        "id",
        "groupId",
        "code",
        "slug",
        "nameAr",
        "nameEn",
        "iconKey",
        "specialization",
        "active",
        "sortOrder",
    }


@pytest.mark.django_db
def test_category_group_list_is_camel_case(admin_client: APIClient, facility: Facility) -> None:
    body = admin_client.get("/api/v1/admin/category-groups/").json()

    expected = {"id", "code", "nameAr", "nameEn", "iconKey", "active", "sortOrder"}
    assert _keys(body["items"]) == expected


@pytest.mark.django_db
def test_province_list_is_camel_case(admin_client: APIClient, facility: Facility) -> None:
    body = admin_client.get("/api/v1/admin/provinces/").json()

    assert _keys(body["items"]) == {"id", "code", "nameAr", "nameEn", "active", "sortOrder"}


@pytest.mark.django_db
def test_verification_requirement_list_is_camel_case(
    admin_client: APIClient, facility: Facility
) -> None:
    VerificationRequirement.objects.create(
        category=facility.category,
        label_ar="السجل التجاري",
    )

    body = admin_client.get("/api/v1/admin/verification-requirements/").json()

    assert _keys(body["items"]) == {
        "id",
        "categoryId",
        "labelAr",
        "labelEn",
        "required",
        "active",
        "minFiles",
        "maxFiles",
        "sortOrder",
    }


@pytest.mark.django_db
def test_advertisement_list_is_camel_case(admin_client: APIClient, facility: Facility) -> None:
    Advertisement.objects.create(image_key="ads/one.jpg", title_ar="إعلان")

    body = admin_client.get("/api/v1/admin/ads/").json()

    assert _keys(body["items"]) == {
        "id",
        "titleAr",
        "targetScope",
        "enabled",
        "startsAt",
        "endsAt",
        "sortOrder",
        "slideDurationMs",
    }


@pytest.mark.django_db
def test_audit_list_is_camel_case(admin_client: APIClient, facility: Facility) -> None:
    Province.objects.filter(pk=facility.province_id).update(active=True)
    admin_client.put(
        f"/api/v1/admin/provinces/{facility.province_id}/",
        {"sortOrder": 3},
        format="json",
    )

    body = admin_client.get("/api/v1/admin/audit/").json()

    assert body["items"], "the province update should have recorded an audit event"
    assert _keys(body["items"]) == {
        "id",
        "actorId",
        "action",
        "targetType",
        "targetId",
        "requestId",
        "metadata",
        "createdAt",
    }


@pytest.mark.django_db
def test_no_admin_response_body_contains_a_column_name(
    admin_client: APIClient, facility: Facility
) -> None:
    for route in (
        "/api/v1/admin/categories/",
        "/api/v1/admin/category-groups/",
        "/api/v1/admin/provinces/",
        "/api/v1/admin/verification-requirements/",
        "/api/v1/admin/ads/",
        "/api/v1/admin/settings/",
        "/api/v1/admin/audit/",
        "/api/v1/admin/dashboard/",
    ):
        body = admin_client.get(route).json()
        assert _snake_keys(body) == set(), f"{route} leaks column names"


# `metadata`, `snapshot` and a setting's `value` carry recorded or operator-authored
# payloads. They are opaque to the API contract, so their inner keys are not the API's
# naming to police.
OPAQUE = {"metadata", "snapshot", "value"}


def _snake_keys(node: Any) -> set[str]:
    found: set[str] = set()
    if isinstance(node, dict):
        for key, value in node.items():
            if "_" in key:
                found.add(key)
            if key not in OPAQUE:
                found |= _snake_keys(value)
    elif isinstance(node, list):
        for item in node:
            found |= _snake_keys(item)
    return found
