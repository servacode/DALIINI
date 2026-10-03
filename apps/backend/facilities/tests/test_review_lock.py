"""A facility under review is frozen, and the decision reaches the owner's phone.

The operator decides on the snapshot taken at submission. Edits made after it used to be
accepted, so an approval could publish a name, a number or a photograph nobody had looked at.
And the decision itself reached only the inbox, never a device.
"""

from types import SimpleNamespace
from typing import Any

import pytest
from django.contrib.gis.geos import Point
from rest_framework.test import APIClient

from accounts.models import User
from admin_console.services import decide_application
from core.exceptions import ConflictError
from directory.models import CategoryProvince
from facilities.models import Facility, FacilityApplication, FacilityMembership
from facilities.services import submit_facility, update_facility_core, update_facility_location
from notifications.models import Notification


@pytest.fixture
def operator(db: None) -> Any:
    admin = User.objects.create_user(phone="+963900000078", password="x" * 12, name="Op")
    return SimpleNamespace(user=admin, request_id="req-lock")


@pytest.fixture
def submitted(facility: Facility, user: User) -> FacilityApplication:
    CategoryProvince.objects.create(
        category=facility.category,
        province=facility.province,
        public_enabled=True,
        owner_registration_enabled=True,
    )
    FacilityMembership.objects.create(facility=facility, user=user, role="OWNER")
    facility.status = Facility.Status.DRAFT
    facility.location = Point(39.01, 35.95, srid=4326)
    facility.save()
    return submit_facility(actor=user, facility=facility)


@pytest.mark.django_db
def test_core_fields_cannot_change_during_review(
    submitted: FacilityApplication, user: User
) -> None:
    facility = submitted.facility
    with pytest.raises(ConflictError) as raised:
        update_facility_core(actor=user, facility=facility, data={"nameAr": "اسم آخر"})

    assert raised.value.code == "FACILITY_LOCKED_DURING_REVIEW"
    facility.refresh_from_db()
    assert facility.name_ar == "صيدلية اختبار"


@pytest.mark.django_db
def test_the_location_cannot_change_during_review(
    submitted: FacilityApplication, user: User
) -> None:
    with pytest.raises(ConflictError):
        update_facility_location(
            actor=user, facility=submitted.facility, latitude=35.9, longitude=39.0
        )


@pytest.mark.django_db
def test_the_api_answers_409_with_the_rule(submitted: FacilityApplication, user: User) -> None:
    client = APIClient()
    client.force_authenticate(user=user)

    response = client.patch(
        f"/api/v1/owner/facilities/{submitted.facility_id}/",
        {"nameAr": "اسم آخر"},
        format="json",
    )

    assert response.status_code == 409
    assert response.json()["code"] == "FACILITY_LOCKED_DURING_REVIEW"


@pytest.mark.django_db
def test_editing_resumes_after_a_rejection(
    submitted: FacilityApplication, user: User, operator: Any
) -> None:
    decide_application(
        request=operator, application_id=submitted.pk, approve=False, reason="الاسم غير واضح"
    )
    facility = Facility.objects.get(pk=submitted.facility_id)

    updated = update_facility_core(actor=user, facility=facility, data={"nameAr": "اسم أوضح"})

    assert updated.name_ar == "اسم أوضح"


@pytest.mark.django_db
@pytest.mark.parametrize("approve", [True, False])
def test_the_decision_is_pushed_to_every_member(
    monkeypatch: pytest.MonkeyPatch,
    submitted: FacilityApplication,
    user: User,
    operator: Any,
    approve: bool,
) -> None:
    pushed: list[Notification] = []
    monkeypatch.setattr("notifications.services.enqueue_push", pushed.append)

    decide_application(
        request=operator,
        application_id=submitted.pk,
        approve=approve,
        reason="" if approve else "الصور غير واضحة",
    )

    expected = "facility.application.approved" if approve else "facility.application.rejected"
    assert [item.type for item in pushed] == [expected]
    assert pushed[0].user_id == user.pk
    assert Notification.objects.filter(user=user, type=expected).count() == 1
