"""Every console list is read in cursor pages, and walking them yields each row exactly once.

The lists used to stop at 200 or 250 rows, so an operator past the first few months of
reports, accounts or audit entries could not reach the rest at all.
"""

from datetime import timedelta
from typing import Any

import pytest
from django.utils import timezone

from accounts.models import User
from audit.models import AuditEvent
from facilities.models import Facility, FacilityApplication, FacilityReport

APPLICATIONS = "/api/v1/admin/applications/"
FACILITIES = "/api/v1/admin/facilities/"
USERS = "/api/v1/admin/users/"
REPORTS = "/api/v1/admin/reports/"
AUDIT = "/api/v1/admin/audit/"


def _walk(client: Any, url: str, **params: Any) -> list[dict[str, Any]]:
    """Every row of a list, following `nextCursor` two rows at a time."""
    rows: list[dict[str, Any]] = []
    cursor = None
    for _ in range(100):
        query = {**params, "limit": 2, **({"cursor": cursor} if cursor else {})}
        response = client.get(url, query)
        assert response.status_code == 200, response.content
        body = response.json()
        assert len(body["items"]) <= 2
        assert body["hasMore"] is (body["nextCursor"] is not None)
        rows.extend(body["items"])
        cursor = body["nextCursor"]
        if cursor is None:
            return rows
    raise AssertionError("the pages never ended")


def _ids(rows: list[dict[str, Any]]) -> list[str]:
    return [row["id"] for row in rows]


def _siblings(facility: Facility, count: int) -> list[Facility]:
    return [
        Facility.objects.create(
            category=facility.category,
            province=facility.province,
            name_ar=f"صيدلية {n}",
            status=Facility.Status.ACTIVE,
        )
        for n in range(count)
    ]


@pytest.mark.django_db
def test_facilities_are_walked_once_each_in_both_orders(
    admin_api: Any, facility: Facility
) -> None:
    client = admin_api("admin.facilities.read")
    every = {str(f.pk) for f in [facility, *_siblings(facility, 6)]}

    by_change = _walk(client, FACILITIES)
    # Quality scores tie a lot: seven facilities, a handful of distinct scores.
    by_quality = _walk(client, FACILITIES, ordering="qualityScore")

    assert sorted(_ids(by_change)) == sorted(every)
    assert len(_ids(by_change)) == len(every)
    assert sorted(_ids(by_quality)) == sorted(every)
    scores = [row["qualityScore"] for row in by_quality]
    assert scores == sorted(scores)


@pytest.mark.django_db
def test_applications_include_drafts_and_follow_the_newest_submission(
    admin_api: Any, facility: Facility
) -> None:
    client = admin_api("admin.reviews.read")
    now = timezone.now()
    places = [facility, *_siblings(facility, 4)]
    submitted = [
        FacilityApplication.objects.create(
            facility=place,
            kind=FacilityApplication.Kind.INITIAL,
            status=FacilityApplication.Status.SUBMITTED,
            submitted_at=now - timedelta(hours=n),
        )
        for n, place in enumerate(places[:4])
    ]
    # Never submitted: no submission time, which a cursor cannot sort on by itself.
    draft = FacilityApplication.objects.create(
        facility=places[4],
        kind=FacilityApplication.Kind.INITIAL,
        status=FacilityApplication.Status.DRAFT,
    )

    rows = _walk(client, APPLICATIONS)

    # The draft was started after every submission here, so it leads.
    assert _ids(rows) == [str(a.pk) for a in [draft, *submitted]]


@pytest.mark.django_db
def test_users_reports_and_audit_are_walked_once_each(
    admin_api: Any, facility: Facility
) -> None:
    client = admin_api("admin.users.read", "admin.reports.read", "admin.audit.read")
    for n in range(5):
        User.objects.create_user(phone=f"+9639881000{n:02d}", password="x" * 12, name=f"U{n}")
    reports = [
        FacilityReport.objects.create(facility=facility, reason="WRONG_HOURS") for _ in range(5)
    ]
    events = [
        AuditEvent.objects.create(action="test.event", target_type="Facility", target_id=str(n))
        for n in range(5)
    ]

    users = _walk(client, USERS)
    report_rows = _walk(client, REPORTS)
    audit_rows = _walk(client, AUDIT, action="test.event")

    assert len(users) == len(set(_ids(users))) == User.objects.count()
    assert _ids(report_rows) == [str(r.pk) for r in reversed(reports)]
    assert set(_ids(audit_rows)) == {str(e.pk) for e in events}
    assert len(audit_rows) == len(events)


@pytest.mark.django_db
def test_a_page_is_fifty_rows_unless_asked_and_never_more_than_two_hundred(
    admin_api: Any, facility: Facility
) -> None:
    client = admin_api("admin.audit.read")
    AuditEvent.objects.bulk_create(
        AuditEvent(action="bulk.event", target_type="Facility", target_id=str(n))
        for n in range(205)
    )

    default = client.get(AUDIT, {"action": "bulk.event"}).json()
    asked = client.get(AUDIT, {"action": "bulk.event", "limit": 500}).json()

    assert len(default["items"]) == 50 and default["hasMore"] is True
    assert len(asked["items"]) == 200 and asked["hasMore"] is True


@pytest.mark.django_db
def test_a_mangled_cursor_is_a_validation_error(admin_api: Any) -> None:
    response = admin_api("admin.users.read").get(USERS, {"cursor": "not-a-cursor"})

    assert response.status_code == 400
    assert "cursor" in response.json()["details"]


@pytest.mark.django_db
def test_users_can_be_walked_by_name_in_either_direction_and_a_bad_order_is_refused(
    admin_api: Any,
) -> None:
    client = admin_api("admin.users.read")
    for name in ("ياسر", "أحمد", "باسل", "أحمد", "تامر"):
        User.objects.create_user(
            phone=f"+96398820{User.objects.count():04d}", password="x" * 12, name=name
        )

    ascending = _walk(client, USERS, ordering="name")
    descending = _walk(client, USERS, ordering="-name")

    names = [row["name"] for row in ascending]
    assert names == sorted(names)
    assert len(ascending) == len(set(_ids(ascending))) == User.objects.count()
    assert _ids(descending) == list(reversed(_ids(ascending)))
    assert client.get(USERS, {"ordering": "phone"}).status_code == 400
