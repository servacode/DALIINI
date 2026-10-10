"""An operator sets a facility's week and its photos from the console (DECISION-115).

The directory opens empty and the first facilities are the operator's: without these, one the
platform added itself could never be «مفتوح الآن» and always showed a placeholder.
"""

from io import BytesIO
from typing import Any

import pytest
from django.core.files.uploadedfile import SimpleUploadedFile
from PIL import Image

from audit.models import AuditEvent
from business_hours.models import BusinessHour
from facilities.models import Facility, FacilityImage


def _url(facility: Facility, tail: str) -> str:
    return f"/api/v1/admin/facilities/{facility.pk}/{tail}"


class _Storage:
    saved: dict[str, bytes] = {}

    def save(self, key: str, content: Any) -> str:
        self.saved[key] = content.read()
        return key

    def url(self, key: str) -> str:
        return f"https://media.example/{key}"

    def delete(self, key: str) -> None:
        self.saved.pop(key, None)


def _photo() -> SimpleUploadedFile:
    buffer = BytesIO()
    Image.new("RGB", (800, 450), (20, 120, 80)).save(buffer, format="PNG")
    return SimpleUploadedFile("front.png", buffer.getvalue())


WEEK = [
    {"weekday": day, "opensAt": "09:00", "closesAt": "21:00", "sequence": 0} for day in range(6)
]


@pytest.mark.django_db
def test_an_editor_sets_the_week_and_reads_it_back(admin_api: Any, facility: Facility) -> None:
    editor = admin_api("admin.facilities.read", "admin.facilities.edit")

    saved = editor.put(_url(facility, "hours/"), WEEK, format="json")
    read = editor.get(_url(facility, "hours/"))

    assert saved.status_code == 200, saved.content
    assert BusinessHour.objects.filter(facility=facility).count() == 6
    assert len(read.json()["items"]) == 6


@pytest.mark.django_db
def test_an_overlapping_day_is_refused_in_words(admin_api: Any, facility: Facility) -> None:
    editor = admin_api("admin.facilities.read", "admin.facilities.edit")
    overlap = [
        {"weekday": 0, "opensAt": "09:00", "closesAt": "14:00", "sequence": 0},
        {"weekday": 0, "opensAt": "13:00", "closesAt": "18:00", "sequence": 1},
    ]

    response = editor.put(_url(facility, "hours/"), overlap, format="json")

    assert response.status_code == 400
    assert response.json()["code"] == "INVALID_HOURS"


@pytest.mark.django_db
def test_reading_is_not_editing(admin_api: Any, facility: Facility) -> None:
    reader = admin_api("admin.facilities.read")

    assert reader.get(_url(facility, "hours/")).status_code == 200
    assert reader.put(_url(facility, "hours/"), WEEK, format="json").status_code == 403
    assert (
        reader.post(_url(facility, "images/"), {"file": _photo()}, format="multipart").status_code
        == 403
    )


@pytest.mark.django_db
def test_a_photo_is_added_listed_and_removed(
    admin_api: Any, facility: Facility, monkeypatch: pytest.MonkeyPatch
) -> None:
    monkeypatch.setattr("facilities.media.PublicS3Storage", _Storage)
    monkeypatch.setattr("admin_console.views_facility_media.PublicS3Storage", _Storage)
    editor = admin_api("admin.facilities.read", "admin.facilities.edit")

    created = editor.post(_url(facility, "images/"), {"file": _photo()}, format="multipart")
    assert created.status_code == 201, created.content
    image_id = created.json()["id"]
    assert created.json()["url"].startswith("https://media.example/")
    assert [item["id"] for item in editor.get(_url(facility, "images/")).json()["items"]] == [
        image_id
    ]

    removed = editor.delete(_url(facility, f"images/{image_id}/"))
    assert removed.status_code == 204
    assert not FacilityImage.objects.filter(pk=image_id).exists()
    actions = set(AuditEvent.objects.values_list("action", flat=True))
    assert {"facility.public_image.created", "facility.public_image.deleted"} <= actions
