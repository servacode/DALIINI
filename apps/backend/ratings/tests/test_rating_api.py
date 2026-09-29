from typing import Any

import pytest
from django.db.models import Avg, Count
from rest_framework.test import APIClient

from accounts.models import User
from directory.models import CategoryProvince
from facilities.models import Facility
from ratings.models import Rating


@pytest.fixture
def public_facility(facility: Facility) -> Facility:
    CategoryProvince.objects.create(
        category=facility.category, province=facility.province, public_enabled=True
    )
    return facility


@pytest.fixture
def client(user: User) -> APIClient:
    api = APIClient()
    api.force_authenticate(user=user)
    return api


def _url(facility: Facility) -> str:
    return f"/api/v1/facilities/{facility.pk}/rating/"


@pytest.mark.django_db
def test_create_then_update_keeps_one_row_and_the_summary(
    client: APIClient, public_facility: Facility, user: User
) -> None:
    assert client.put(_url(public_facility), {"stars": 4}, format="json").json()["stars"] == 4
    assert client.put(_url(public_facility), {"stars": 2}, format="json").json()["stars"] == 2
    assert Rating.objects.filter(user=user).count() == 1
    summary = Rating.objects.filter(facility=public_facility).aggregate(
        avg=Avg("stars"), n=Count("id")
    )
    assert summary == {"avg": 2, "n": 1}


@pytest.mark.django_db
def test_invalid_stars_and_anonymous_are_refused(
    client: APIClient, public_facility: Facility
) -> None:
    assert client.put(_url(public_facility), {"stars": 6}, format="json").status_code == 400
    assert APIClient().put(_url(public_facility), {"stars": 3}, format="json").status_code == 401


@pytest.mark.django_db
def test_delete_removes_and_is_idempotent(
    client: APIClient, public_facility: Facility, user: User
) -> None:
    client.put(_url(public_facility), {"stars": 5}, format="json")
    assert client.delete(_url(public_facility)).status_code == 204
    assert client.delete(_url(public_facility)).status_code == 204
    assert not Rating.objects.filter(user=user).exists()


@pytest.mark.django_db
def test_hidden_facilities_are_404_for_write_and_delete(
    client: APIClient, facility: Facility
) -> None:
    # No public province switch: the facility is not publicly visible.
    assert client.put(_url(facility), {"stars": 3}, format="json").status_code == 404
    assert client.delete(_url(facility)).status_code == 404


@pytest.mark.django_db
def test_category_without_ratings_is_refused(client: APIClient, public_facility: Facility) -> None:
    caps = public_facility.category.capabilities
    caps.supports_ratings = False
    caps.save()
    assert client.put(_url(public_facility), {"stars": 3}, format="json").status_code == 400


@pytest.mark.django_db
def test_rating_writes_are_throttled(
    client: APIClient, public_facility: Facility, monkeypatch: Any
) -> None:
    from core.throttles import RatingsWriteThrottle

    monkeypatch.setattr(RatingsWriteThrottle, "THROTTLE_RATES", {"ratings_write": "2/hour"})
    for stars in (1, 2):
        assert client.put(_url(public_facility), {"stars": stars}, format="json").status_code == 200
    throttled = client.put(_url(public_facility), {"stars": 3}, format="json")
    assert throttled.status_code == 429
    assert throttled.json()["code"] == "THROTTLED"
    assert "Retry-After" in throttled
