"""The account's own inbox.

The inbox is the record of what the platform told an account. A push may never arrive — the
permission is the user's to give — so everything that matters has to be readable here, in
order, with its own words, and belonging to exactly one account.
"""

import pytest
from rest_framework.test import APIClient

from accounts.models import User
from notifications.models import Notification
from notifications.services import create_notification

INBOX = "/api/v1/account/notifications/"
UNREAD = "/api/v1/account/notifications/unread-count/"
READ_ALL = "/api/v1/account/notifications/read-all/"


@pytest.fixture
def signed_in(db: None, user: User) -> APIClient:
    client = APIClient()
    client.force_authenticate(user=user)
    return client


@pytest.mark.django_db
def test_a_message_carries_its_own_words(signed_in: APIClient, user: User) -> None:
    create_notification(
        user=user,
        type="facility.application.approved",
        title_ar="تمت الموافقة على منشأتك",
        body_ar="صيدلية الاختبار صارت ظاهرة في الدليل.",
        destination=Notification.Destination.FACILITY,
        payload={"facilityId": "3f2b1c62-0000-4000-8000-000000000001"},
    )

    body = signed_in.get(INBOX).json()

    assert body["unreadCount"] == 1
    item = body["items"][0]
    assert item["titleAr"] == "تمت الموافقة على منشأتك"
    assert item["bodyAr"].startswith("صيدلية")
    assert item["destination"] == "FACILITY"
    assert item["facilityId"] == "3f2b1c62-0000-4000-8000-000000000001"
    assert item["isRead"] is False


@pytest.mark.django_db
def test_the_newest_message_is_first(signed_in: APIClient, user: User) -> None:
    for index in range(3):
        create_notification(user=user, type="t", title_ar=f"رسالة {index}", payload={})

    titles = [item["titleAr"] for item in signed_in.get(INBOX).json()["items"]]

    assert titles == ["رسالة 2", "رسالة 1", "رسالة 0"]


@pytest.mark.django_db
def test_reading_one_message_lowers_the_unread_count(signed_in: APIClient, user: User) -> None:
    first = create_notification(user=user, type="t", title_ar="أ", payload={})
    create_notification(user=user, type="t", title_ar="ب", payload={})

    response = signed_in.post(f"{INBOX}{first.id}/read/")

    assert response.json()["unreadCount"] == 1
    assert signed_in.get(UNREAD).json()["unreadCount"] == 1
    first.refresh_from_db()
    assert first.read_at is not None


@pytest.mark.django_db
def test_reading_the_same_message_twice_keeps_the_first_time(
    signed_in: APIClient, user: User
) -> None:
    message = create_notification(user=user, type="t", title_ar="أ", payload={})

    signed_in.post(f"{INBOX}{message.id}/read/")
    message.refresh_from_db()
    first_time = message.read_at
    signed_in.post(f"{INBOX}{message.id}/read/")
    message.refresh_from_db()

    assert message.read_at == first_time


@pytest.mark.django_db
def test_marking_all_read_empties_the_count(signed_in: APIClient, user: User) -> None:
    for index in range(4):
        create_notification(user=user, type="t", title_ar=str(index), payload={})

    assert signed_in.post(READ_ALL).json()["unreadCount"] == 0
    assert signed_in.get(UNREAD).json()["unreadCount"] == 0


@pytest.mark.django_db
def test_one_account_cannot_read_or_see_anothers_messages(
    signed_in: APIClient, user: User, db: None
) -> None:
    from accounts.models import User

    mine = create_notification(user=user, type="t", title_ar="لي", payload={})
    other = User.objects.create_user(
        phone="+963900000003", password="StrongPass123!", name="Other"
    )
    other_client = APIClient()
    other_client.force_authenticate(user=other)

    assert other_client.get(INBOX).json()["items"] == []
    assert other_client.post(f"{INBOX}{mine.id}/read/").status_code == 404
    mine.refresh_from_db()
    assert mine.read_at is None


@pytest.mark.django_db
def test_a_destination_outside_the_closed_set_is_refused(user: User) -> None:
    with pytest.raises(ValueError):
        create_notification(
            user=user, type="t", title_ar="أ", payload={}, destination="EXTERNAL_URL"
        )


@pytest.mark.django_db
def test_the_inbox_needs_an_account() -> None:
    anonymous = APIClient()

    assert anonymous.get(INBOX).status_code in (401, 403)
    assert anonymous.get(UNREAD).status_code in (401, 403)
    assert anonymous.post(READ_ALL).status_code in (401, 403)
