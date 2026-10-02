from __future__ import annotations

import pytest
from django.urls import reverse
from rest_framework.test import APIClient

from content_services.models import AppRelease

"""
What the app is told about its own version.

The case that matters most is the empty one: a backend nobody has configured must not lock
every phone out of a platform that works perfectly.
"""

pytestmark = pytest.mark.django_db


def url() -> str:
    return reverse("public-app-release")


def test_an_unconfigured_backend_blocks_nobody() -> None:
    response = APIClient().get(url())

    assert response.status_code == 200
    assert response.data["minimumVersionCode"] == 0
    assert response.data["latestVersionCode"] == 0
    assert response.data["storeUrl"] == ""
    assert response.data["noticeAr"] == ""


def test_it_answers_what_the_operator_set() -> None:
    AppRelease.objects.create(
        platform=AppRelease.Platform.ANDROID,
        minimum_version_code=12,
        latest_version_code=19,
        store_url="https://play.example.test/app",
        notice_ar="حدّث التطبيق للمتابعة.",
    )

    response = APIClient().get(url())

    assert response.status_code == 200
    assert response.data["platform"] == "ANDROID"
    assert response.data["minimumVersionCode"] == 12
    assert response.data["latestVersionCode"] == 19
    assert response.data["storeUrl"] == "https://play.example.test/app"
    assert response.data["noticeAr"] == "حدّث التطبيق للمتابعة."


def test_each_platform_is_answered_from_its_own_row() -> None:
    AppRelease.objects.create(platform=AppRelease.Platform.ANDROID, minimum_version_code=5,
                              latest_version_code=5)
    AppRelease.objects.create(platform=AppRelease.Platform.IOS, minimum_version_code=9,
                              latest_version_code=9)

    android = APIClient().get(url(), {"platform": "android"})
    ios = APIClient().get(url(), {"platform": "IOS"})

    # The query is read case-insensitively; a phone should not fail on capitalisation.
    assert android.data["minimumVersionCode"] == 5
    assert ios.data["minimumVersionCode"] == 9


def test_an_unknown_platform_is_refused_rather_than_answered_for_android() -> None:
    response = APIClient().get(url(), {"platform": "WINDOWS_PHONE"})
    assert response.status_code == 400


def test_the_answer_may_be_cached() -> None:
    assert "max-age" in APIClient().get(url())["Cache-Control"]


def test_a_minimum_above_the_latest_is_refused_by_the_database() -> None:
    # The pair is a promise: anything the app may be forced up to must be something it can get.
    from django.db.utils import IntegrityError

    with pytest.raises(IntegrityError):
        AppRelease.objects.create(
            platform=AppRelease.Platform.ANDROID,
            minimum_version_code=20,
            latest_version_code=3,
        )


def test_no_session_is_needed_to_ask() -> None:
    # The check runs before anybody has signed in; requiring a token would defeat it.
    response = APIClient().get(url())
    assert response.status_code == 200
