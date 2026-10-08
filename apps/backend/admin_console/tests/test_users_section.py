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
    client = admin_api(READ)

    body = client.get(f"/api/v1/admin/users/{person.pk}/").json()

    assert [f["nameAr"] for f in body["facilities"]] == [facility.name_ar]
    assert body["facilities"][0]["role"] == FacilityMembership.Role.OWNER
    assert body["facilities"][0]["status"] == Facility.Status.ACTIVE
    # Nothing about devices: signing one out is the person's own, in their app (§8), and
    # the console asks for no power over them. Nor whether the number was proved: the app
    # proves it before the account exists, so it would read the same on every card.
    assert "sessions" not in body
    assert "phoneVerifiedAt" not in body
    assert "lastLoginAt" in body


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


# ----------------------------------------------------------------- the card's own facts


@pytest.mark.django_db
def test_a_row_carries_what_a_card_shows(admin_api: Any, facility: Facility) -> None:
    person = _person(facility.province)
    FacilityMembership.objects.create(
        user=person, facility=facility, role=FacilityMembership.Role.OWNER
    )
    UserSession.objects.create(
        user=person,
        refresh_digest="live",
        last_seen_at=timezone.now(),
        expires_at=timezone.now() + timedelta(days=30),
    )
    client = admin_api(READ)

    row = next(
        item
        for item in client.get("/api/v1/admin/users/").json()["items"]
        if item["id"] == str(person.pk)
    )

    assert row["facilityCount"] == 1
    # The card names the place rather than counting it, and never opens to find out.
    assert [f["nameAr"] for f in row["facilities"]] == [facility.name_ar]
    assert row["facilities"][0]["status"] == Facility.Status.ACTIVE
    assert "sessionCount" not in row
    assert row["lastSeenAt"] is not None
    assert row["recentlyActive"] is True


@pytest.mark.django_db
def test_an_account_last_seen_yesterday_is_not_called_active(
    admin_api: Any, province: Province
) -> None:
    """«Recently active» is a claim about the last half hour, and it has to be false sometimes."""
    person = _person(province)
    UserSession.objects.create(
        user=person,
        refresh_digest="stale",
        last_seen_at=timezone.now() - timedelta(days=1),
        expires_at=timezone.now() + timedelta(days=30),
    )
    client = admin_api(READ)

    row = next(
        item
        for item in client.get("/api/v1/admin/users/").json()["items"]
        if item["id"] == str(person.pk)
    )

    assert row["recentlyActive"] is False


@pytest.mark.django_db
def test_the_list_asks_the_same_number_of_questions_however_many_rows(
    admin_api: Any, facility: Facility, django_assert_num_queries: Any
) -> None:
    """Three counts per card would be three hundred queries on a page of a hundred."""
    client = admin_api(READ)

    made = 0

    def load(people: int) -> int:
        nonlocal made
        for _ in range(people):
            made += 1
            person = _person(facility.province, phone=f"+96390077{made:04d}")
            FacilityMembership.objects.create(
                user=person, facility=facility, role=FacilityMembership.Role.MANAGER
            )
            UserSession.objects.create(
                user=person,
                refresh_digest=f"session-{person.pk}",
                expires_at=timezone.now() + timedelta(days=30),
            )
        from django.db import connection
        from django.test.utils import CaptureQueriesContext

        with CaptureQueriesContext(connection) as captured:
            assert client.get("/api/v1/admin/users/").status_code == 200
        return len(captured)

    one = load(1)
    many = load(9)

    assert many == one, f"{one} queries for 1 account, {many} for 10"
