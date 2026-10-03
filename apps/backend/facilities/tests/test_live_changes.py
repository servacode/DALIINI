"""An owner's edit to a live facility is reviewed while the facility stays in the directory."""

from typing import Any

import pytest
from django.contrib.gis.geos import Point
from django.utils import timezone
from rest_framework.test import APIClient

from accounts.models import User
from audit.models import AuditEvent
from directory.models import CategoryProvince
from facilities.models import Facility, FacilityApplication, FacilityMembership
from locations.models import City, Province
from notifications.models import Notification

PUBLISHED_NAME = "صيدلية اختبار"


@pytest.fixture
def live(facility: Facility, user: User) -> Facility:
    CategoryProvince.objects.create(
        province=facility.province,
        category=facility.category,
        public_enabled=True,
        owner_registration_enabled=True,
    )
    FacilityMembership.objects.create(
        facility=facility, user=user, role=FacilityMembership.Role.OWNER
    )
    facility.location = Point(39.01, 35.95, srid=4326)
    facility.activated_at = facility.last_verified_at = timezone.now()
    facility.save()
    return facility


@pytest.fixture
def owner(user: User) -> APIClient:
    client = APIClient()
    client.force_authenticate(user=user)
    return client


def _owner_url(facility: Facility, suffix: str = "") -> str:
    return f"/api/v1/owner/facilities/{facility.pk}/{suffix}"


def _public(facility: Facility) -> dict[str, Any]:
    response = APIClient().get(f"/api/v1/public/facilities/{facility.pk}/")
    assert response.status_code == 200, response.content
    return dict(response.json())


def _change(facility: Facility) -> FacilityApplication:
    return FacilityApplication.objects.get(
        facility=facility, kind=FacilityApplication.Kind.CHANGE, status="SUBMITTED"
    )


@pytest.mark.django_db
def test_the_name_waits_the_phone_does_not_and_the_listing_stays_up(
    live: Facility, owner: APIClient
) -> None:
    response = owner.patch(
        _owner_url(live), {"nameAr": "صيدلية الفرات", "phone": "0221234567"}, format="json"
    )

    assert response.status_code == 200, response.content
    body = response.json()
    # The owner sees what they saved, marked as waiting.
    assert body["nameAr"] == "صيدلية الفرات"
    assert body["status"] == "ACTIVE"
    assert body["pendingChange"]["proposedFields"] == ["nameAr"]
    # The public still sees the approved name, and already the new phone.
    public = _public(live)
    assert public["nameAr"] == PUBLISHED_NAME
    assert public["phone"] == "0221234567"


@pytest.mark.django_db
def test_later_edits_join_the_waiting_change_and_undoing_them_withdraws_it(
    live: Facility, owner: APIClient
) -> None:
    owner.patch(_owner_url(live), {"nameAr": "صيدلية الفرات"}, format="json")
    owner.put(_owner_url(live, "location/"), {"latitude": 35.96, "longitude": 39.02}, format="json")
    change = _change(live)

    assert change.revision == 2
    assert sorted(change.proposed_changes) == ["location", "nameAr"]

    owner.patch(_owner_url(live), {"nameAr": PUBLISHED_NAME}, format="json")
    owner.put(_owner_url(live, "location/"), {"latitude": 35.95, "longitude": 39.01}, format="json")

    assert not FacilityApplication.objects.filter(kind=FacilityApplication.Kind.CHANGE).exists()
    assert owner.get(_owner_url(live)).json()["pendingChange"] is None
    assert AuditEvent.objects.filter(action="facility.owner_change.withdrawn").exists()


@pytest.mark.django_db
def test_an_approval_publishes_exactly_the_revision_the_operator_saw(
    live: Facility, owner: APIClient, admin_api: Any, user: User
) -> None:
    operator = admin_api("admin.reviews.read", "admin.reviews.decide")
    owner.patch(_owner_url(live), {"nameAr": "صيدلية الفرات"}, format="json")
    change = _change(live)
    review = operator.get(f"/api/v1/admin/applications/{change.pk}/").json()
    assert review["kind"] == "CHANGE"
    assert review["proposedFields"] == ["nameAr"]
    assert review["previous"]["nameAr"] == PUBLISHED_NAME
    assert review["snapshot"]["nameAr"] == "صيدلية الفرات"
    # Only the proposal differs between the two sides.
    differing = sorted(
        key for key in review["snapshot"] if review["snapshot"][key] != review["previous"].get(key)
    )
    assert differing == ["nameAr"]

    # The owner revises it after the operator opened it.
    owner.patch(_owner_url(live), {"nameAr": "صيدلية الفرات الجديدة"}, format="json")
    stale = operator.post(
        f"/api/v1/admin/applications/{change.pk}/approve/",
        {"revision": review["revision"]},
        format="json",
    )
    assert stale.status_code == 409
    assert stale.json()["code"] == "APPLICATION_CHANGED"
    assert _public(live)["nameAr"] == PUBLISHED_NAME

    fresh = operator.get(f"/api/v1/admin/applications/{change.pk}/").json()
    approved = operator.post(
        f"/api/v1/admin/applications/{change.pk}/approve/",
        {"revision": fresh["revision"]},
        format="json",
    )
    assert approved.status_code == 200, approved.content
    live.refresh_from_db()
    assert live.status == Facility.Status.ACTIVE
    assert _public(live)["nameAr"] == "صيدلية الفرات الجديدة"
    assert owner.get(_owner_url(live)).json()["pendingChange"] is None
    note = Notification.objects.filter(user=user).latest("created_at")
    assert note.title_ar == "اعتُمد تعديلك"


@pytest.mark.django_db
def test_a_rejection_leaves_the_facility_as_it_was(
    live: Facility, owner: APIClient, admin_api: Any, user: User
) -> None:
    operator = admin_api("admin.reviews.read", "admin.reviews.decide")
    owner.put(_owner_url(live, "location/"), {"latitude": 36.5, "longitude": 40.7}, format="json")
    change = _change(live)

    rejected = operator.post(
        f"/api/v1/admin/applications/{change.pk}/reject/",
        {"reason": "الموقع خارج المدينة"},
        format="json",
    )

    assert rejected.status_code == 200, rejected.content
    live.refresh_from_db()
    assert live.status == Facility.Status.ACTIVE
    assert live.location is not None
    assert (live.location.y, live.location.x) == (35.95, 39.01)
    detail = owner.get(_owner_url(live)).json()
    assert detail["location"] == {"latitude": 35.95, "longitude": 39.01}
    assert detail["application"]["status"] == "REJECTED"
    assert detail["application"]["rejectionReason"] == "الموقع خارج المدينة"
    note = Notification.objects.filter(user=user).latest("created_at")
    assert "الموقع خارج المدينة" in note.body_ar


@pytest.mark.django_db
def test_a_city_from_another_province_is_refused_when_saved(
    live: Facility, owner: APIClient
) -> None:
    elsewhere = City.objects.create(
        province=Province.objects.create(code="other", name_ar="أخرى", active=True),
        code="other-city",
        name_ar="أخرى",
    )

    response = owner.patch(_owner_url(live), {"cityId": str(elsewhere.pk)}, format="json")

    assert response.status_code == 400
    assert "cityId" in response.json()["details"]
    assert not FacilityApplication.objects.filter(kind=FacilityApplication.Kind.CHANGE).exists()


@pytest.mark.django_db
def test_submitting_a_live_facility_never_takes_it_down(
    live: Facility, owner: APIClient
) -> None:
    nothing = owner.post(_owner_url(live, "submit/"))
    owner.patch(_owner_url(live), {"addressAr": "شارع تل أبيض"}, format="json")
    waiting = owner.post(_owner_url(live, "submit/"))

    assert nothing.status_code == 400
    assert waiting.status_code == 200
    assert waiting.json()["applicationId"] == str(_change(live).pk)
    live.refresh_from_db()
    assert live.status == Facility.Status.ACTIVE


@pytest.mark.django_db
def test_the_task_board_counts_waiting_changes(
    live: Facility, owner: APIClient, admin_api: Any
) -> None:
    owner.patch(_owner_url(live), {"nameAr": "صيدلية الفرات"}, format="json")

    tasks = admin_api("admin.dashboard.read").get("/api/v1/admin/tasks/").json()

    assert tasks["applications"]["change"]["count"] == 1
    assert tasks["applications"]["reverification"]["count"] == 0
