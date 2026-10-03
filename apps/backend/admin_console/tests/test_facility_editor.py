"""Operators add facilities themselves and correct any facility's details."""

from typing import Any

import pytest

from audit.models import AuditEvent
from directory.models import Category, CategoryCapabilities, ServiceTag, Specialty
from facilities.models import Facility, FacilityMembership
from locations.models import City, Province
from notifications.models import Notification

LIST = "/api/v1/admin/facilities/"


def _detail(facility_id: Any) -> str:
    return f"{LIST}{facility_id}/"


@pytest.fixture
def editor(admin_api: Any) -> Any:
    return admin_api("admin.facilities.read", "admin.facilities.edit")


@pytest.mark.django_db
def test_an_operator_lists_a_pharmacy_nobody_registered(
    editor: Any, facility: Facility
) -> None:
    city = City.objects.create(province=facility.province, code="raqqa-city", name_ar="الرقة")
    response = editor.post(
        LIST,
        {
            "categoryId": str(facility.category_id),
            "provinceId": str(facility.province_id),
            "cityId": str(city.pk),
            "nameAr": "  صيدلية الفرات  ",
            "phone": "0221234567",
            "whatsapp": "0991234567",
            "location": {"latitude": 35.95, "longitude": 39.01},
        },
        format="json",
    )

    assert response.status_code == 201, response.content
    body = response.json()
    created = Facility.objects.get(pk=body["id"])
    assert created.status == Facility.Status.ACTIVE
    assert created.activated_at is not None and created.last_verified_at is not None
    assert body["nameAr"] == "صيدلية الفرات"
    assert body["whatsapp"] == "+963991234567"
    assert body["location"] == {"latitude": 35.95, "longitude": 39.01}
    assert body["ownerCount"] == 0
    assert AuditEvent.objects.filter(action="facility.admin.created", target_id=body["id"]).exists()


@pytest.mark.django_db
def test_a_draft_stays_hidden_and_inputs_are_checked(editor: Any, facility: Facility) -> None:
    base = {"categoryId": str(facility.category_id), "provinceId": str(facility.province_id)}

    draft = editor.post(LIST, {**base, "nameAr": "مسودة", "status": "DRAFT"}, format="json")
    blank = editor.post(LIST, {**base, "nameAr": "   "}, format="json")
    closed = editor.post(LIST, {**base, "nameAr": "س", "status": "CLOSED"}, format="json")
    elsewhere = City.objects.create(
        province=Province.objects.create(code="other", name_ar="أخرى", active=True),
        code="other-city",
        name_ar="أخرى",
    )
    wrong_city = editor.post(
        LIST, {**base, "nameAr": "س", "cityId": str(elsewhere.pk)}, format="json"
    )

    assert draft.status_code == 201
    assert Facility.objects.get(pk=draft.json()["id"]).activated_at is None
    assert blank.status_code == 400
    assert closed.status_code == 400
    assert wrong_city.status_code == 400
    assert "cityId" in wrong_city.json()["details"]


@pytest.mark.django_db
def test_a_correction_keeps_the_status_and_tells_the_owners(
    editor: Any, facility: Facility, user: Any
) -> None:
    FacilityMembership.objects.create(
        facility=facility, user=user, role=FacilityMembership.Role.OWNER
    )

    response = editor.patch(
        _detail(facility.pk), {"phone": "0227654321", "addressAr": "شارع تل أبيض"}, format="json"
    )

    assert response.status_code == 200, response.content
    facility.refresh_from_db()
    # An owner's change would have sent it back for re-verification; this one does not.
    assert facility.status == Facility.Status.ACTIVE
    assert facility.phone == "0227654321"
    event = AuditEvent.objects.get(action="facility.admin.updated", target_id=str(facility.pk))
    assert event.metadata["fields"] == ["addressAr", "phone"]
    assert event.before_snapshot["phone"] is None
    note = Notification.objects.get(user=user)
    assert note.type == "facility.admin.updated"
    assert note.destination == Notification.Destination.OWNER_FACILITIES


@pytest.mark.django_db
def test_sending_nothing_new_changes_nothing(editor: Any, facility: Facility, user: Any) -> None:
    FacilityMembership.objects.create(
        facility=facility, user=user, role=FacilityMembership.Role.OWNER
    )

    response = editor.patch(_detail(facility.pk), {"nameAr": facility.name_ar}, format="json")

    assert response.status_code == 200
    assert not AuditEvent.objects.filter(action="facility.admin.updated").exists()
    assert not Notification.objects.filter(user=user).exists()


@pytest.mark.django_db
def test_moving_province_or_category_drops_what_no_longer_applies(
    editor: Any, facility: Facility
) -> None:
    city = City.objects.create(province=facility.province, code="c1", name_ar="المدينة")
    facility.city = city
    facility.save()
    specialty = Specialty.objects.create(category=facility.category, name_ar="تركيب")
    service = ServiceTag.objects.create(category=facility.category, name_ar="قياس ضغط")
    editor.patch(
        _detail(facility.pk),
        {"specialtyIds": [specialty.pk], "serviceTagIds": [service.pk]},
        format="json",
    )
    other_province = Province.objects.create(code="hasakah", name_ar="الحسكة", active=True)
    other_category = Category.objects.create(
        group=facility.category.group, code="clinic", slug="clinic", name_ar="عيادة"
    )
    CategoryCapabilities.objects.create(category=other_category)

    moved = editor.patch(
        _detail(facility.pk),
        {"provinceId": str(other_province.pk), "categoryId": str(other_category.pk)},
        format="json",
    ).json()

    assert moved["provinceId"] == str(other_province.pk)
    assert moved["cityId"] is None
    assert moved["specialtyIds"] == [] and moved["serviceTagIds"] == []


@pytest.mark.django_db
def test_reading_is_not_enough_to_edit(admin_api: Any, facility: Facility) -> None:
    reader = admin_api("admin.facilities.read")
    base = {"categoryId": str(facility.category_id), "provinceId": str(facility.province_id)}

    assert reader.get(_detail(facility.pk)).status_code == 200
    assert reader.post(LIST, {**base, "nameAr": "س"}, format="json").status_code == 403
    assert reader.patch(_detail(facility.pk), {"nameAr": "س"}, format="json").status_code == 403
    assert admin_api("admin.facilities.edit", "admin.facilities.read").patch(
        f"{LIST}00000000-0000-0000-0000-000000000000/", {"nameAr": "س"}, format="json"
    ).status_code == 404


@pytest.mark.django_db
def test_the_list_filters_by_city_and_refuses_a_malformed_id(
    editor: Any, facility: Facility
) -> None:
    city = City.objects.create(province=facility.province, code="c2", name_ar="المدينة")
    in_city = Facility.objects.create(
        category=facility.category, province=facility.province, city=city, name_ar="في المدينة"
    )

    rows = editor.get(LIST, {"city": str(city.pk)}).json()["items"]

    assert [row["id"] for row in rows] == [str(in_city.pk)]
    assert editor.get(LIST, {"city": "not-an-id"}).status_code == 400


@pytest.mark.django_db
def test_the_timeline_names_the_change_and_keeps_removed_shifts(
    admin_api: Any, facility: Facility, user: Any
) -> None:
    from datetime import timedelta

    from django.utils import timezone
    from rest_framework.test import APIClient

    FacilityMembership.objects.create(
        facility=facility, user=user, role=FacilityMembership.Role.OWNER
    )
    owner = APIClient()
    owner.force_authenticate(user=user)
    start = timezone.now() + timedelta(days=1)
    shift = owner.post(
        f"/api/v1/owner/facilities/{facility.pk}/duty/",
        {"startsAt": start.isoformat(), "endsAt": (start + timedelta(hours=8)).isoformat()},
        format="json",
    ).json()
    owner.delete(f"/api/v1/owner/facilities/{facility.pk}/duty/{shift['id']}/")
    admin_api("admin.facilities.read", "admin.facilities.edit").patch(
        _detail(facility.pk), {"phone": "0221234567"}, format="json"
    )

    timeline = admin_api("admin.facilities.read").get(f"{_detail(facility.pk)}timeline/").json()
    titles = [event["titleAr"] for event in timeline["items"]]

    assert "أُلغيت وردية مناوبة" in titles
    assert "أُضيفت وردية مناوبة" in titles
    assert "عدّلت إدارة الدليل بيانات المنشأة" in titles
