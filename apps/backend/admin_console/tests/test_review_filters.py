"""The review queue's date and evidence filters, and what the ads list returns for editing."""

from datetime import datetime, timedelta
from typing import Any

import pytest
from django.utils import timezone

from content_services.models import Advertisement
from directory.models import VerificationRequirement
from facilities.models import Facility, FacilityApplication, VerificationEvidence

REVIEWS = "/api/v1/admin/applications/"
ADS = "/api/v1/admin/ads/"


def _damascus(day: str, hour: int) -> datetime:
    return timezone.make_aware(datetime.fromisoformat(f"{day}T{hour:02d}:00:00"))


def _sibling(facility: Facility, name: str) -> Facility:
    """Another facility in the same place: one submitted application per facility and kind."""
    return Facility.objects.create(
        category=facility.category,
        province=facility.province,
        name_ar=name,
        status=Facility.Status.DRAFT,
    )


def _submitted(facility: Facility, at: datetime) -> FacilityApplication:
    return FacilityApplication.objects.create(
        facility=facility,
        kind=FacilityApplication.Kind.INITIAL,
        status=FacilityApplication.Status.SUBMITTED,
        submitted_at=at,
    )


def _ids(response: Any) -> list[str]:
    assert response.status_code == 200, response.content
    return [item["id"] for item in response.json()["items"]]


@pytest.mark.django_db
def test_the_queue_filters_by_the_day_it_was_submitted_on_damascus_days(
    admin_api: Any, facility: Facility
) -> None:
    early = _submitted(facility, _damascus("2026-09-20", 23))
    middle = _submitted(_sibling(facility, "صيدلية الوسط"), _damascus("2026-09-21", 1))
    late = _submitted(_sibling(facility, "صيدلية المتأخرة"), _damascus("2026-09-23", 12))
    client = admin_api("admin.reviews.read")

    assert _ids(client.get(REVIEWS, {"from": "2026-09-21"})) == [str(late.id), str(middle.id)]
    assert _ids(client.get(REVIEWS, {"to": "2026-09-21"})) == [str(middle.id), str(early.id)]
    assert _ids(client.get(REVIEWS, {"from": "2026-09-21", "to": "2026-09-21"})) == [
        str(middle.id)
    ]
    assert client.get(REVIEWS, {"from": "yesterday"}).status_code == 400


@pytest.mark.django_db
def test_the_queue_says_which_applications_miss_a_required_document(
    admin_api: Any, facility: Facility
) -> None:
    licence = VerificationRequirement.objects.create(
        category=facility.category, label_ar="ترخيص المزاولة", min_files=1, max_files=2
    )
    VerificationRequirement.objects.create(
        category=facility.category, label_ar="صورة اختيارية", required=False
    )
    VerificationRequirement.objects.create(
        category=facility.category, label_ar="وثيقة ملغاة", active=False
    )
    other = _sibling(facility, "صيدلية بلا ترخيص")
    VerificationEvidence.objects.create(
        facility=facility, requirement=licence, storage_key="evidence/licence-1.jpg"
    )
    complete = _submitted(facility, timezone.now() - timedelta(hours=2))
    missing = _submitted(other, timezone.now() - timedelta(hours=1))
    client = admin_api("admin.reviews.read")

    rows = {item["id"]: item for item in client.get(REVIEWS).json()["items"]}
    assert rows[str(complete.id)]["evidenceComplete"] is True
    assert rows[str(missing.id)]["evidenceComplete"] is False
    assert _ids(client.get(REVIEWS, {"evidence": "complete"})) == [str(complete.id)]
    assert _ids(client.get(REVIEWS, {"evidence": "incomplete"})) == [str(missing.id)]
    assert client.get(REVIEWS, {"evidence": "some"}).status_code == 400

    # A requirement added after submission shows on the application already in the queue.
    VerificationRequirement.objects.create(category=facility.category, label_ar="سجل تجاري")
    detail = client.get(f"{REVIEWS}{complete.id}/").json()
    assert detail["evidenceComplete"] is False


@pytest.mark.django_db
def test_an_ad_opens_for_editing_with_its_target_and_its_image(
    admin_api: Any, facility: Facility, settings: Any
) -> None:
    settings.S3_PUBLIC_MEDIA_BASE_URL = "https://media.example.test"
    Advertisement.objects.create(
        image_key="ads/slide one.jpg",
        title_ar="إعلان المحافظة",
        target_scope=Advertisement.TargetScope.PROVINCE,
        province=facility.province,
    )

    [item] = admin_api("admin.ads.read").get(ADS).json()["items"]

    assert item["targetScope"] == "PROVINCE"
    assert item["provinceId"] == str(facility.province_id)
    assert item["categoryId"] is None
    assert item["imageUrl"] == "https://media.example.test/ads/slide%20one.jpg"
