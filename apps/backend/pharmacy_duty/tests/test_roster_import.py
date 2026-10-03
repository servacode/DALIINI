"""A duty roster from a spreadsheet, and rotations that generate whole months."""

import io
from datetime import datetime, timedelta
from typing import Any

import pytest
from django.core.files.uploadedfile import SimpleUploadedFile
from django.utils import timezone

from accounts.models import User
from business_hours.services import DAMASCUS
from facilities.models import Facility, FacilityMembership
from notifications.models import Notification
from pharmacy_duty.models import DutyShift

IMPORT = "/api/v1/admin/duty/import/"
ROTATIONS = "/api/v1/admin/duty/rotations/"


def _csv(text: str, name: str = "roster.csv") -> SimpleUploadedFile:
    return SimpleUploadedFile(name, text.encode("utf-8-sig"), content_type="text/csv")


def _pharmacy(facility: Facility, name: str, phone: str = "") -> Facility:
    return Facility.objects.create(
        category=facility.category,
        province=facility.province,
        name_ar=name,
        phone=phone,
        status=Facility.Status.ACTIVE,
    )


@pytest.fixture
def operator(admin_api: Any) -> Any:
    return admin_api("admin.duty.read", "admin.duty.manage")


def _post(client: Any, facility: Facility, upload: SimpleUploadedFile, apply: bool) -> Any:
    return client.post(
        IMPORT,
        {"file": upload, "provinceId": str(facility.province_id), "apply": apply},
        format="multipart",
    )


@pytest.mark.django_db
def test_a_roster_with_arabic_headers_is_previewed_then_applied_once(
    operator: Any, facility: Facility
) -> None:
    _pharmacy(facility, "صيدلية الأمل", phone="0221111111")
    roster = (
        "الصيدلية,التاريخ,من,إلى\n"
        "صيدليه اختبار,2026-11-01,20:00,08:00\n"
        "صيدلية الامل,2026-11-02,20:00,08:00\n"
    )

    preview = _post(operator, facility, _csv(roster), apply=False).json()

    assert preview["applied"] is False
    assert preview["errorCount"] == 0
    assert [row["outcome"] for row in preview["rows"]] == ["CREATED", "CREATED"]
    assert not DutyShift.objects.exists()
    first = preview["rows"][0]
    # A night shift ends the next morning, Damascus time.
    assert datetime.fromisoformat(first["startsAt"]) == datetime(2026, 11, 1, 20, tzinfo=DAMASCUS)
    assert datetime.fromisoformat(first["endsAt"]) == datetime(2026, 11, 2, 8, tzinfo=DAMASCUS)

    applied = _post(operator, facility, _csv(roster), apply=True).json()
    again = _post(operator, facility, _csv(roster), apply=True).json()

    assert applied["applied"] is True and applied["created"] == 2
    assert DutyShift.objects.filter(source="IMPORT").count() == 2
    assert again["created"] == 0 and again["unchanged"] == 2


@pytest.mark.django_db
def test_every_problem_is_named_by_its_line_and_nothing_is_written(
    operator: Any, facility: Facility
) -> None:
    _pharmacy(facility, "صيدلية النور")
    _pharmacy(facility, "صيدلية النور")
    start = timezone.make_aware(datetime(2026, 11, 5, 19), DAMASCUS)
    DutyShift.objects.create(
        facility=facility, starts_at=start, ends_at=start + timedelta(hours=10)
    )
    roster = (
        "pharmacy,date,from,to\n"
        "صيدلية لا توجد,2026-11-03,20:00,08:00\n"
        "صيدلية النور,2026-11-03,20:00,08:00\n"
        "صيدلية اختبار,2026-13-40,20:00,08:00\n"
        "صيدلية اختبار,2026-11-05,20:00,08:00\n"
        "صيدلية اختبار,2026-11-07,20:00,08:00\n"
    )

    result = _post(operator, facility, _csv(roster), apply=True).json()

    assert result["applied"] is False
    problems = {row["line"]: row["problems"] for row in result["rows"]}
    assert "لا صيدلية باسم" in problems[2][0]
    assert "أكثر من صيدلية" in problems[3][0]
    assert "التاريخ" in problems[4][0]
    assert problems[5], "overlaps the shift already stored"
    assert problems[6] == []
    assert DutyShift.objects.count() == 1


@pytest.mark.django_db
def test_a_workbook_is_read_like_a_csv(operator: Any, facility: Facility) -> None:
    from openpyxl import Workbook

    book = Workbook()
    sheet = book.active
    sheet.append(["facilityId", "startsAt", "endsAt"])
    sheet.append([str(facility.pk), "2026-11-10T20:00:00+03:00", "2026-11-11T08:00:00+03:00"])
    buffer = io.BytesIO()
    book.save(buffer)
    upload = SimpleUploadedFile(
        "roster.xlsx",
        buffer.getvalue(),
        content_type="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    )

    result = _post(operator, facility, upload, apply=True).json()

    assert result["applied"] is True and result["created"] == 1


@pytest.mark.django_db
def test_a_file_that_is_not_a_roster_is_refused(operator: Any, facility: Facility) -> None:
    response = _post(operator, facility, _csv("a,b\n1,2\n"), apply=False)

    assert response.status_code == 400
    assert "file" in response.json()["details"]


@pytest.mark.django_db
def test_a_rotation_cycles_through_its_pharmacies_and_tells_each_owner_once(
    operator: Any, facility: Facility, user: User
) -> None:
    second = _pharmacy(facility, "صيدلية الفرات")
    third = _pharmacy(facility, "صيدلية الرشيد")
    FacilityMembership.objects.create(facility=second, user=user, role="OWNER")
    created = operator.post(
        ROTATIONS,
        {
            "name": "مناوبة الرقة الليلية",
            "provinceId": str(facility.province_id),
            "facilityIds": [str(facility.pk), str(second.pk), str(third.pk)],
            "startsAt": "21:00",
            "endsAt": "08:00",
            "perDay": 1,
            "anchorDate": "2026-12-01",
        },
        format="json",
    )
    assert created.status_code == 201, created.content
    url = f"{ROTATIONS}{created.json()['id']}/generate/"

    preview = operator.post(
        url, {"fromDate": "2026-12-01", "toDate": "2026-12-06"}, format="json"
    ).json()
    names = [row["facilityNameAr"] for row in preview["rows"]]
    applied = operator.post(
        url, {"fromDate": "2026-12-01", "toDate": "2026-12-06", "apply": True}, format="json"
    ).json()

    assert names == [
        "صيدلية اختبار",
        "صيدلية الفرات",
        "صيدلية الرشيد",
        "صيدلية اختبار",
        "صيدلية الفرات",
        "صيدلية الرشيد",
    ]
    assert applied["applied"] is True and applied["created"] == 6
    notes = Notification.objects.filter(user=user, type="duty.shift.admin_changed")
    assert notes.count() == 1
    assert "2 وردية" in notes.get().body_ar


@pytest.mark.django_db
def test_reading_duty_is_not_enough_to_import_or_save_rotations(
    admin_api: Any, facility: Facility
) -> None:
    reader = admin_api("admin.duty.read")

    assert reader.get(ROTATIONS).status_code == 200
    assert _post(reader, facility, _csv("pharmacy,date,from,to\n"), apply=False).status_code == 403
    assert reader.post(ROTATIONS, {}, format="json").status_code == 403
