"""Public media has a permanent address; private evidence has none (INT-061, INT-065)."""

import pytest
from django.test import override_settings
from rest_framework.test import APIClient

from content_services.models import Advertisement
from content_services.serializers import public_ad
from directory.models import CategoryProvince
from facilities.models import Facility, FacilityImage
from storage.backends import PrivateS3Storage, PublicS3Storage
from storage.public_media import public_media_url

CDN = "https://media.example.test/public"


@override_settings(S3_PUBLIC_MEDIA_BASE_URL=CDN + "/")
def test_the_public_address_is_stable_and_unsigned() -> None:
    first = public_media_url("facilities/f/public/a b.jpg")

    assert first == f"{CDN}/facilities/f/public/a%20b.jpg"
    assert first == public_media_url("/facilities/f/public/a b.jpg")
    assert "X-Amz" not in first
    assert "Expires" not in first


@override_settings(S3_PUBLIC_MEDIA_BASE_URL=CDN)
def test_the_public_storage_hands_out_the_public_address_only() -> None:
    assert PublicS3Storage().url("facilities/f/public/a.jpg") == f"{CDN}/facilities/f/public/a.jpg"


@override_settings(S3_PUBLIC_MEDIA_BASE_URL=CDN)
def test_evidence_is_never_given_a_public_address() -> None:
    private = PrivateS3Storage().url("facilities/f/evidence/r/e.jpg")

    # Private objects stay behind signatures in their own bucket; nothing maps them to the
    # public base, so a leaked key cannot be turned into a readable address.
    assert not private.startswith(CDN)
    assert "directory-private" in private or "X-Amz" in private


@pytest.mark.django_db
@override_settings(S3_PUBLIC_MEDIA_BASE_URL=CDN)
def test_facility_photos_are_served_at_their_public_address(facility: Facility) -> None:
    CategoryProvince.objects.create(
        province=facility.province,
        category=facility.category,
        public_enabled=True,
        owner_registration_enabled=False,
    )
    FacilityImage.objects.create(
        facility=facility, storage_key="facilities/f/public/a.jpg", width=10, height=10
    )

    images = APIClient().get(f"/api/v1/public/facilities/{facility.pk}/").json()["images"]

    assert images[0]["url"] == f"{CDN}/facilities/f/public/a.jpg"


@pytest.mark.django_db
@override_settings(S3_PUBLIC_MEDIA_BASE_URL=CDN)
def test_advertisement_images_are_served_at_their_public_address() -> None:
    ad = Advertisement.objects.create(image_key="ads/summer.jpg")

    assert public_ad(ad)["imageUrl"] == f"{CDN}/ads/summer.jpg"
