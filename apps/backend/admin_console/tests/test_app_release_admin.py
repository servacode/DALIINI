from __future__ import annotations

from typing import Any

import pytest
from rest_framework.test import APIClient

from accounts.models import AdminPermission, AdminRole, User, UserAdminRole
from audit.models import AuditEvent
from content_services.models import AppRelease

"""
The one admin screen that can stop every phone in the field.

So the tests are mostly about who may touch it and what it refuses: reading is one permission,
writing another, and a minimum nobody can install is refused outright.
"""

pytestmark = pytest.mark.django_db

URL = "/api/v1/admin/app-release/"


def client_with(*permissions: str) -> APIClient:
    user = User.objects.create_user(phone="+963900111222", password="OperatorPass123!")
    role = AdminRole.objects.create(code=f"role-{len(permissions)}", name="Role")
    for code in permissions:
        role.permissions.add(AdminPermission.objects.get_or_create(code=code)[0])
    UserAdminRole.objects.create(user=user, role=role, active=True)
    client = APIClient()
    client.force_authenticate(user=user)
    return client


def test_a_platform_nobody_has_set_reads_as_zeros() -> None:
    response = client_with("admin.settings.read").get(URL)

    assert response.status_code == 200
    assert response.data["minimumVersionCode"] == 0
    assert response.data["latestVersionCode"] == 0
    assert response.data["updatedAt"] is None


def test_setting_it_stores_the_row_and_answers_the_new_state() -> None:
    client = client_with("admin.settings.read", "admin.settings.manage")

    response = client.put(
        URL,
        {
            "minimumVersionCode": 14,
            "latestVersionCode": 20,
            "storeUrl": "https://play.example.test/app",
            "noticeAr": "حدّث التطبيق للمتابعة.",
        },
        format="json",
    )

    assert response.status_code == 200
    assert response.data["minimumVersionCode"] == 14
    assert response.data["updatedAt"] is not None
    stored = AppRelease.objects.get(platform=AppRelease.Platform.ANDROID)
    assert stored.latest_version_code == 20


def test_a_second_write_updates_the_same_row_rather_than_adding_one() -> None:
    client = client_with("admin.settings.read", "admin.settings.manage")
    body: dict[str, Any] = {
        "minimumVersionCode": 1,
        "latestVersionCode": 2,
        "storeUrl": "",
        "noticeAr": "",
    }

    client.put(URL, body, format="json")
    client.put(URL, {**body, "minimumVersionCode": 2}, format="json")

    assert AppRelease.objects.filter(platform=AppRelease.Platform.ANDROID).count() == 1


def test_a_minimum_above_the_latest_is_refused() -> None:
    # Nobody can install a build that does not exist, so the pair is a promise, not two numbers.
    client = client_with("admin.settings.read", "admin.settings.manage")

    response = client.put(
        URL,
        {"minimumVersionCode": 30, "latestVersionCode": 4, "storeUrl": "", "noticeAr": ""},
        format="json",
    )

    assert response.status_code == 400
    assert not AppRelease.objects.exists()


def test_reading_needs_its_permission() -> None:
    assert client_with("admin.ads.read").get(URL).status_code == 403


def test_writing_needs_more_than_reading() -> None:
    response = client_with("admin.settings.read").put(
        URL,
        {"minimumVersionCode": 1, "latestVersionCode": 1, "storeUrl": "", "noticeAr": ""},
        format="json",
    )
    assert response.status_code == 403
    assert not AppRelease.objects.exists()


def test_a_stranger_is_refused() -> None:
    assert APIClient().get(URL).status_code in (401, 403)


def test_the_change_is_audited_with_what_it_was_and_what_it_became() -> None:
    client = client_with("admin.settings.read", "admin.settings.manage")
    client.put(
        URL,
        {"minimumVersionCode": 3, "latestVersionCode": 9, "storeUrl": "", "noticeAr": ""},
        format="json",
    )
    client.put(
        URL,
        {"minimumVersionCode": 8, "latestVersionCode": 9, "storeUrl": "", "noticeAr": ""},
        format="json",
    )

    events = AuditEvent.objects.filter(action="app_release.updated").order_by("created_at")
    assert events.count() == 2
    # The first had nothing before it; the second records the state it replaced, which is what
    # a person asking "who locked everyone out, and from what" needs.
    assert events[0].before_snapshot in (None, {})
    assert events[1].before_snapshot["minimumVersionCode"] == 3
    assert events[1].after_snapshot["minimumVersionCode"] == 8


def test_each_platform_is_its_own_row() -> None:
    client = client_with("admin.settings.read", "admin.settings.manage")
    body = {"minimumVersionCode": 5, "latestVersionCode": 5, "storeUrl": "", "noticeAr": ""}

    client.put(URL, body, format="json")
    client.put(
        f"{URL}?platform=IOS",
        {**body, "minimumVersionCode": 7, "latestVersionCode": 7},
        format="json",
    )

    assert client.get(URL).data["minimumVersionCode"] == 5
    assert client.get(f"{URL}?platform=IOS").data["minimumVersionCode"] == 7


def test_an_unknown_platform_is_refused() -> None:
    assert client_with("admin.settings.read").get(f"{URL}?platform=SYMBIAN").status_code == 400
