"""What an operator may learn about an account, and what they may do to it (DECISION-104).

The console never holds anyone's password. Everything here follows from that: the operator
can start a recovery, open an account without one, and sign a lost phone out — and at no
point does a credential belonging to someone else pass through their hands.
"""

from datetime import timedelta
from typing import Any
from unittest import mock

import pytest
from django.utils import timezone

from accounts.models import OTPChallenge, User
from audit.models import AuditEvent
from facilities.models import Facility, FacilityMembership
from locations.models import Province
from sessions.models import UserSession

READ = "admin.users.read"
MANAGE = ("admin.users.read", "admin.users.manage")


@pytest.fixture
def province(db: None) -> Province:
    return Province.objects.create(code="raqqa-test", name_ar="الرقة", active=True)


def _person(province: Province, phone: str = "+963900888001") -> User:
    return User.objects.create_user(
        phone=phone, password="StrongPass123!", name="صاحب حساب", province=province
    )


# ----------------------------------------------------------------- what is shown


@pytest.mark.django_db
def test_the_list_carries_the_province_name_not_only_its_id(
    admin_api: Any, province: Province
) -> None:
    """A UUID tells an operator nothing; the console must not fetch a second list to read it."""
    _person(province)
    client = admin_api(READ)

    row = next(
        item
        for item in client.get("/api/v1/admin/users/").json()["items"]
        if item["phone"] == "+963900888001"
    )

    assert row["provinceId"] == str(province.pk)
    assert row["provinceName"] == "الرقة"


@pytest.mark.django_db
def test_the_detail_shows_what_hangs_off_the_account(admin_api: Any, facility: Facility) -> None:
    """Blocking an owner of live pharmacies is a different decision from blocking a visitor."""
    person = _person(facility.province)
    FacilityMembership.objects.create(
        user=person, facility=facility, role=FacilityMembership.Role.OWNER
    )
    UserSession.objects.create(
        user=person,
        refresh_digest="digest-one",
        platform="ANDROID",
        device_name="هاتف المالك",
        expires_at=timezone.now() + timedelta(days=30),
    )
    client = admin_api(READ)

    body = client.get(f"/api/v1/admin/users/{person.pk}/").json()

    assert [f["nameAr"] for f in body["facilities"]] == [facility.name_ar]
    assert body["facilities"][0]["role"] == FacilityMembership.Role.OWNER
    assert body["facilities"][0]["status"] == Facility.Status.ACTIVE
    assert [s["deviceName"] for s in body["sessions"]] == ["هاتف المالك"]
    assert body["phoneVerifiedAt"] is None
    assert "lastLoginAt" in body


@pytest.mark.django_db
def test_a_session_payload_carries_no_secret(admin_api: Any, province: Province) -> None:
    person = _person(province)
    UserSession.objects.create(
        user=person,
        refresh_digest="a-secret-digest",
        previous_refresh_digest="an-older-secret",
        platform="ANDROID",
        expires_at=timezone.now() + timedelta(days=30),
    )
    client = admin_api(READ)

    session = client.get(f"/api/v1/admin/users/{person.pk}/").json()["sessions"][0]

    assert set(session) == {"id", "platform", "deviceName", "createdAt", "lastSeenAt"}
    assert "secret" not in str(session)


@pytest.mark.django_db
def test_a_revoked_session_is_not_listed_as_signed_in(
    admin_api: Any, province: Province
) -> None:
    person = _person(province)
    UserSession.objects.create(
        user=person,
        refresh_digest="gone",
        expires_at=timezone.now() + timedelta(days=30),
        revoked_at=timezone.now(),
    )
    client = admin_api(READ)

    assert client.get(f"/api/v1/admin/users/{person.pk}/").json()["sessions"] == []


# ----------------------------------------------------------------- recovery


@pytest.mark.django_db
def test_recovery_sends_a_code_and_tells_the_operator_nothing_more(
    admin_api: Any, province: Province
) -> None:
    """The operator starts it; only the person's own phone can finish it."""
    person = _person(province)
    client = admin_api(*MANAGE)

    with mock.patch("accounts.otp.get_otp_sender") as sender:
        response = client.post(f"/api/v1/admin/users/{person.pk}/recovery/")

    assert response.status_code == 200
    assert response.json() == {"sent": True, "phone": person.phone}
    # Nothing an operator could use to complete it.
    assert "challengeId" not in response.json()
    assert "code" not in response.json()
    assert sender.return_value.send.call_count == 1
    assert OTPChallenge.objects.filter(
        phone=person.phone, purpose=OTPChallenge.Purpose.RECOVERY
    ).exists()
    assert AuditEvent.objects.filter(action="user.recovery_sent").exists()


@pytest.mark.django_db
def test_an_operator_who_may_only_read_cannot_send_a_recovery_code(
    admin_api: Any, province: Province
) -> None:
    person = _person(province)
    client = admin_api(READ)

    assert client.post(f"/api/v1/admin/users/{person.pk}/recovery/").status_code == 403
    assert not OTPChallenge.objects.exists()


# ----------------------------------------------------------------- opening an account


@pytest.mark.django_db
def test_an_account_opened_from_the_console_has_no_usable_password(
    admin_api: Any, province: Province
) -> None:
    client = admin_api(*MANAGE)

    response = client.post(
        "/api/v1/admin/users/",
        {"name": "مشغّل جديد", "phone": "0933111222", "provinceId": str(province.pk)},
        format="json",
    )

    assert response.status_code == 201
    # Normalised on the way in: one number, one spelling, one account.
    assert response.json()["phone"] == "+963933111222"
    opened = User.objects.get(phone="+963933111222")
    assert opened.has_usable_password() is False
    assert opened.province_id == province.pk
    assert AuditEvent.objects.filter(action="user.created").exists()


@pytest.mark.django_db
def test_a_number_that_already_has_an_account_is_refused(
    admin_api: Any, province: Province
) -> None:
    _person(province, phone="+963933111333")
    client = admin_api(*MANAGE)

    response = client.post(
        "/api/v1/admin/users/",
        {"name": "مكرّر", "phone": "0933111333", "provinceId": str(province.pk)},
        format="json",
    )

    assert response.status_code == 400
    assert User.objects.filter(phone="+963933111333").count() == 1


@pytest.mark.django_db
def test_a_number_that_is_not_a_syrian_mobile_is_refused(
    admin_api: Any, province: Province
) -> None:
    client = admin_api(*MANAGE)

    response = client.post(
        "/api/v1/admin/users/",
        {"name": "رقم خطأ", "phone": "12345", "provinceId": str(province.pk)},
        format="json",
    )

    assert response.status_code == 400
    assert "phone" in response.json().get("details", response.json())


@pytest.mark.django_db
def test_opening_an_account_needs_more_than_read(admin_api: Any, province: Province) -> None:
    client = admin_api(READ)

    response = client.post(
        "/api/v1/admin/users/",
        {"name": "ممنوع", "phone": "0933111444", "provinceId": str(province.pk)},
        format="json",
    )

    assert response.status_code == 403
    assert not User.objects.filter(phone="+963933111444").exists()


# ----------------------------------------------------------------- a lost phone


@pytest.mark.django_db
def test_revoking_sessions_signs_the_devices_out_and_leaves_the_account_active(
    admin_api: Any, province: Province
) -> None:
    """A stolen phone is not a reason to lock an owner out of their own facilities."""
    person = _person(province)
    for digest in ("one", "two"):
        UserSession.objects.create(
            user=person,
            refresh_digest=digest,
            expires_at=timezone.now() + timedelta(days=30),
        )
    client = admin_api(*MANAGE)

    body = client.post(f"/api/v1/admin/users/{person.pk}/sessions/revoke/").json()

    person.refresh_from_db()
    assert person.is_active is True
    assert body["active"] is True
    assert body["sessions"] == []
    assert UserSession.objects.filter(user=person, revoked_at__isnull=True).count() == 0
    assert AuditEvent.objects.filter(action="user.sessions_revoked").exists()
