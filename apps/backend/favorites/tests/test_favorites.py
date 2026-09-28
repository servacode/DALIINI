"""Saving a facility.

What matters here is that saving is idempotent in both directions, that a saved facility
that stops being public stops being served, and that one account never sees another's list.
"""

from typing import Any

import pytest
from rest_framework.test import APIClient

from directory.models import Category, CategoryCapabilities, CategoryGroup, CategoryProvince
from facilities.models import Facility
from favorites.models import Favorite
from locations.models import Province

FAVORITES = "/api/v1/account/favorites/"


@pytest.fixture
def public_facility(db):
    province = Province.objects.create(code="fav-province", name_ar="محافظة", active=True)
    group = CategoryGroup.objects.create(code="fav-group", name_ar="مجموعة")
    category = Category.objects.create(
        group=group,
        code="fav-category",
        slug="fav-category",
        name_ar="صيدليات",
        specialization=Category.Specialization.PHARMACY,
        active=True,
    )
    CategoryCapabilities.objects.create(category=category)
    CategoryProvince.objects.create(category=category, province=province, public_enabled=True)
    return Facility.objects.create(
        category=category,
        province=province,
        name_ar="صيدلية محفوظة",
        status=Facility.Status.ACTIVE,
    )


@pytest.fixture
def signed_in(db, user):
    client = APIClient()
    client.force_authenticate(user=user)
    return client


@pytest.mark.django_db
def test_saving_is_idempotent(signed_in, public_facility, user):
    first = signed_in.post(FAVORITES, {"facilityId": str(public_facility.id)}, format="json")
    second = signed_in.post(FAVORITES, {"facilityId": str(public_facility.id)}, format="json")

    assert first.status_code == 200
    assert second.status_code == 200
    assert second.json() == {"facilityId": str(public_facility.id), "isFavorite": True}
    assert Favorite.objects.filter(user=user, facility=public_facility).count() == 1


@pytest.mark.django_db
def test_removing_what_was_never_saved_is_not_an_error(signed_in, public_facility):
    response = signed_in.delete(f"{FAVORITES}{public_facility.id}/")

    assert response.status_code == 200
    assert response.json()["isFavorite"] is False


@pytest.mark.django_db
def test_the_list_returns_the_saved_facility_and_when_it_was_saved(signed_in, public_facility):
    signed_in.post(FAVORITES, {"facilityId": str(public_facility.id)}, format="json")

    body = signed_in.get(FAVORITES).json()

    assert [item["id"] for item in body["items"]] == [str(public_facility.id)]
    assert body["items"][0]["nameAr"] == "صيدلية محفوظة"
    assert body["items"][0]["favoritedAt"]
    assert body["hasMore"] is False


@pytest.mark.django_db
def test_a_facility_that_stops_being_public_leaves_the_list(signed_in, public_facility):
    signed_in.post(FAVORITES, {"facilityId": str(public_facility.id)}, format="json")
    public_facility.status = Facility.Status.SUSPENDED
    public_facility.save(update_fields=["status"])

    assert signed_in.get(FAVORITES).json()["items"] == []


@pytest.mark.django_db
def test_a_facility_that_is_not_public_cannot_be_saved(signed_in, public_facility):
    public_facility.status = Facility.Status.DRAFT
    public_facility.save(update_fields=["status"])

    response = signed_in.post(FAVORITES, {"facilityId": str(public_facility.id)}, format="json")

    assert response.status_code == 404


@pytest.mark.django_db
def test_the_public_facility_page_reports_the_callers_own_state(signed_in, public_facility):
    detail = f"/api/v1/public/facilities/{public_facility.id}/"

    assert APIClient().get(detail).json()["isFavorite"] is False
    assert signed_in.get(detail).json()["isFavorite"] is False

    signed_in.post(FAVORITES, {"facilityId": str(public_facility.id)}, format="json")

    assert signed_in.get(detail).json()["isFavorite"] is True
    # Another visitor's page is unchanged by what this account saved.
    assert APIClient().get(detail).json()["isFavorite"] is False


@pytest.mark.django_db
def test_an_account_never_sees_another_accounts_list(signed_in, public_facility, db):
    from accounts.models import User

    signed_in.post(FAVORITES, {"facilityId": str(public_facility.id)}, format="json")
    other = User.objects.create_user(
        phone="+963900000002", password="StrongPass123!", name="Other"
    )
    other_client = APIClient()
    other_client.force_authenticate(user=other)

    assert other_client.get(FAVORITES).json()["items"] == []


@pytest.mark.django_db
def test_saving_requires_an_account(public_facility):
    anonymous = APIClient()

    assert anonymous.get(FAVORITES).status_code in (401, 403)
    assert anonymous.post(
        FAVORITES, {"facilityId": str(public_facility.id)}, format="json"
    ).status_code in (401, 403)


def test_favorite_writes_are_throttled_but_reads_are_not(
    signed_in: Any, public_facility: Any, monkeypatch: Any
) -> None:
    from core.throttles import FavoritesWriteThrottle

    monkeypatch.setattr(FavoritesWriteThrottle, "THROTTLE_RATES", {"favorites_write": "2/hour"})
    body = {"facilityId": str(public_facility.id)}
    assert signed_in.post(FAVORITES, body, format="json").status_code < 300
    assert signed_in.delete(f"{FAVORITES}{public_facility.id}/").status_code < 300
    assert signed_in.post(FAVORITES, body, format="json").status_code == 429
    assert signed_in.get(FAVORITES).status_code == 200
