"""Owners invite people by phone number, and the answer never says who is registered."""

from datetime import timedelta
from typing import Any

import pytest
from django.utils import timezone
from rest_framework.test import APIClient

from accounts.models import User
from audit.models import AuditEvent
from facilities.invitations import announce_waiting
from facilities.models import Facility, FacilityInvitation, FacilityMembership
from notifications.models import Notification


def _client(user: User) -> APIClient:
    client = APIClient()
    client.force_authenticate(user=user)
    return client


@pytest.fixture
def owner(facility: Facility, user: User) -> APIClient:
    FacilityMembership.objects.create(
        facility=facility, user=user, role=FacilityMembership.Role.OWNER
    )
    return _client(user)


@pytest.fixture
def colleague() -> User:
    return User.objects.create_user(phone="+963944000111", password="x" * 12, name="سامر")


def _invite(owner: APIClient, facility: Facility, phone: str, role: str = "MANAGER") -> Any:
    return owner.post(
        f"/api/v1/owner/facilities/{facility.pk}/invitations/",
        {"phone": phone, "role": role},
        format="json",
    )


def _shape(body: dict[str, Any]) -> list[str]:
    return sorted(body)


@pytest.mark.django_db
def test_the_answer_is_the_same_whether_or_not_the_number_has_an_account(
    owner: APIClient, facility: Facility, colleague: User
) -> None:
    registered = _invite(owner, facility, "0944000111")
    unknown = _invite(owner, facility, "0944000222")

    assert registered.status_code == unknown.status_code == 201
    assert _shape(registered.json()) == _shape(unknown.json())
    assert registered.json()["status"] == unknown.json()["status"] == "PENDING"
    assert registered.json()["phone"] == "+963944000111"
    # Only the person with an account is told, and only in their own inbox.
    assert Notification.objects.filter(user=colleague, type="facility.invitation.received").exists()
    assert Notification.objects.count() == 1


@pytest.mark.django_db
def test_the_invited_number_accepts_and_becomes_a_manager(
    owner: APIClient, facility: Facility, colleague: User, user: User
) -> None:
    invitation = _invite(owner, facility, "0944000111").json()
    mine = _client(colleague).get("/api/v1/account/invitations/").json()["items"]

    assert [item["id"] for item in mine] == [invitation["id"]]
    assert mine[0]["facility"]["nameAr"] == facility.name_ar

    accepted = _client(colleague).post(f"/api/v1/account/invitations/{invitation['id']}/accept/")

    assert accepted.status_code == 200
    assert accepted.json() == {"facilityId": str(facility.pk), "role": "MANAGER"}
    assert FacilityMembership.objects.filter(
        facility=facility, user=colleague, role="MANAGER"
    ).exists()
    assert Notification.objects.filter(user=user, type="facility.invitation.accepted").exists()
    assert AuditEvent.objects.filter(action="facility.invitation.accepted").exists()
    # Accepted once is closed.
    again = _client(colleague).post(f"/api/v1/account/invitations/{invitation['id']}/accept/")
    assert again.status_code == 409


@pytest.mark.django_db
def test_nobody_else_can_answer_an_invitation(
    owner: APIClient, facility: Facility, colleague: User
) -> None:
    invitation = _invite(owner, facility, "0944000111").json()
    stranger = User.objects.create_user(phone="+963944000333", password="x" * 12, name="غريب")

    for action in ("accept", "decline"):
        response = _client(stranger).post(
            f"/api/v1/account/invitations/{invitation['id']}/{action}/"
        )
        assert response.status_code == 404
    assert _client(stranger).get("/api/v1/account/invitations/").json()["items"] == []
    assert not FacilityMembership.objects.filter(user=stranger).exists()


@pytest.mark.django_db
def test_an_expired_invitation_cannot_be_accepted_and_reads_as_expired(
    owner: APIClient, facility: Facility, colleague: User
) -> None:
    invitation = _invite(owner, facility, "0944000111").json()
    FacilityInvitation.objects.filter(pk=invitation["id"]).update(
        expires_at=timezone.now() - timedelta(minutes=1)
    )

    response = _client(colleague).post(f"/api/v1/account/invitations/{invitation['id']}/accept/")
    listed = owner.get(f"/api/v1/owner/facilities/{facility.pk}/invitations/").json()["items"]

    assert response.status_code == 409
    assert response.json()["code"] == "INVITATION_EXPIRED"
    assert listed[0]["status"] == "EXPIRED"
    assert _client(colleague).get("/api/v1/account/invitations/").json()["items"] == []


@pytest.mark.django_db
def test_inviting_again_renews_and_revoking_closes(
    owner: APIClient, facility: Facility, colleague: User
) -> None:
    first = _invite(owner, facility, "0944000111").json()
    second = _invite(owner, facility, "+963 944 000 111", role="OWNER").json()

    assert second["id"] == first["id"]
    assert second["role"] == "OWNER"
    assert FacilityInvitation.objects.count() == 1

    revoked = owner.delete(f"/api/v1/owner/facilities/{facility.pk}/invitations/{first['id']}/")
    response = _client(colleague).post(f"/api/v1/account/invitations/{first['id']}/accept/")

    assert revoked.status_code == 204
    assert response.status_code == 409


@pytest.mark.django_db
def test_an_owner_invitation_raises_a_manager_and_never_lowers_anyone(
    owner: APIClient, facility: Facility, colleague: User
) -> None:
    FacilityMembership.objects.create(facility=facility, user=colleague, role="MANAGER")
    # A member's number is refused: the owner already sees it in the member list.
    assert _invite(owner, facility, "0944000111").status_code == 409

    FacilityMembership.objects.filter(user=colleague).delete()
    invitation = _invite(owner, facility, "0944000111", role="OWNER").json()
    FacilityMembership.objects.create(facility=facility, user=colleague, role="MANAGER")
    _client(colleague).post(f"/api/v1/account/invitations/{invitation['id']}/accept/")

    assert FacilityMembership.objects.get(user=colleague).role == "OWNER"


@pytest.mark.django_db
def test_only_owners_invite_and_numbers_are_checked(
    owner: APIClient, facility: Facility, colleague: User
) -> None:
    FacilityMembership.objects.create(facility=facility, user=colleague, role="MANAGER")
    manager = _client(colleague)

    assert _invite(manager, facility, "0944000999").status_code == 403
    assert _invite(owner, facility, "0221234567").status_code == 400
    assert manager.get(f"/api/v1/owner/facilities/{facility.pk}/invitations/").status_code == 403


@pytest.mark.django_db
def test_a_new_account_hears_of_invitations_sent_before_it_existed(
    owner: APIClient, facility: Facility
) -> None:
    _invite(owner, facility, "0944000555")
    newcomer = User.objects.create_user(phone="+963944000555", password="x" * 12, name="جديد")

    announce_waiting(newcomer)

    assert Notification.objects.filter(user=newcomer, type="facility.invitation.received").exists()
