"""Moving a facility to another owner from the console (DECISION-119)."""

from typing import Any

import pytest

from accounts.models import User
from audit.models import AuditEvent
from facilities.models import Facility, FacilityMembership
from notifications.models import Notification

MANAGE = ("admin.facilities.read", "admin.facilities.manage")


def _url(facility: Facility) -> str:
    return f"/api/v1/admin/facilities/{facility.pk}/owner/"


def _account(phone: str, name: str) -> User:
    return User.objects.create_user(phone=phone, password="Daliini2026x", name=name)


def _roles(facility: Facility) -> dict[str, str]:
    return {m.user.name: m.role for m in FacilityMembership.objects.filter(facility=facility)}


@pytest.fixture
def owned(facility: Facility) -> Facility:
    seller = _account("+963933000801", "البائع")
    FacilityMembership.objects.create(
        facility=facility, user=seller, role=FacilityMembership.Role.OWNER
    )
    _account("+963933000802", "المشتري")
    return facility


@pytest.mark.django_db
def test_the_facility_moves_and_both_sides_are_told(admin_api: Any, owned: Facility) -> None:
    response = admin_api(*MANAGE).post(_url(owned), {"phone": "0933000802"}, format="json")

    assert response.status_code == 200, response.content
    assert _roles(owned) == {"المشتري": "OWNER"}
    assert Notification.objects.filter(type="facility.owner.received").count() == 1
    assert Notification.objects.filter(type="facility.owner.moved").count() == 1
    event = AuditEvent.objects.get(action="facility.owner.transferred")
    assert len(event.before_snapshot["owners"]) == 1


@pytest.mark.django_db
def test_the_previous_owner_can_stay_as_a_manager(admin_api: Any, owned: Facility) -> None:
    admin_api(*MANAGE).post(
        _url(owned), {"phone": "0933000802", "keepPreviousAsManager": True}, format="json"
    )

    assert _roles(owned) == {"المشتري": "OWNER", "البائع": "MANAGER"}


@pytest.mark.django_db
def test_a_number_without_an_account_is_refused(admin_api: Any, owned: Facility) -> None:
    response = admin_api(*MANAGE).post(_url(owned), {"phone": "0933000899"}, format="json")

    assert response.status_code == 400
    assert response.json()["code"] == "OWNER_NOT_FOUND"
    assert _roles(owned) == {"البائع": "OWNER"}


@pytest.mark.django_db
def test_the_owner_already_is_refused(admin_api: Any, owned: Facility) -> None:
    response = admin_api(*MANAGE).post(_url(owned), {"phone": "0933000801"}, format="json")

    assert response.status_code == 409
    assert response.json()["code"] == "ALREADY_OWNER"


@pytest.mark.django_db
def test_a_closed_facility_does_not_move(admin_api: Any, owned: Facility) -> None:
    Facility.objects.filter(pk=owned.pk).update(status=Facility.Status.CLOSED)

    response = admin_api(*MANAGE).post(_url(owned), {"phone": "0933000802"}, format="json")

    assert response.status_code == 409


@pytest.mark.django_db
def test_moving_needs_the_manage_permission(admin_api: Any, owned: Facility) -> None:
    response = admin_api("admin.facilities.read", "admin.facilities.edit").post(
        _url(owned), {"phone": "0933000802"}, format="json"
    )

    assert response.status_code == 403
    assert _roles(owned) == {"البائع": "OWNER"}


@pytest.mark.django_db
def test_once_moved_the_seller_can_delete_their_account(admin_api: Any, owned: Facility) -> None:
    """The refusal that pointed to a transfer now has one to point to."""
    admin_api(*MANAGE).post(_url(owned), {"phone": "0933000802"}, format="json")
    from accounts.services import request_account_deletion

    seller = User.objects.get(name="البائع")
    request_account_deletion(user=seller, channel="APP")

    assert not User.objects.filter(pk=seller.pk, is_active=True).exists()
