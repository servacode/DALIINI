"""The platform's own published pages.

A page is served only when it has been published, one version at a time, and the list carries
just enough for a client to tell whether what it cached is still current.

The launch baseline already publishes the first version of five pages, so these tests work
with the one key it deliberately leaves unpublished: contact, which has no configured channel
to publish yet.
"""

import pytest
from django.db import IntegrityError
from django.utils import timezone
from rest_framework.test import APIClient

from content_services.models import LegalDocument

LIST = "/api/v1/public/legal/"


def _publish(key: str, *, title: str, body: str, version: int = 1) -> LegalDocument:
    return LegalDocument.objects.create(
        key=key,
        title_ar=title,
        body_ar=body,
        version=version,
        active=True,
        published_at=timezone.now(),
    )


@pytest.mark.django_db
def test_the_launch_baseline_publishes_the_pages_the_app_shows():
    keys = {item["key"] for item in APIClient().get(LIST).json()["items"]}

    assert {"ABOUT", "PRIVACY", "TERMS", "INSTRUCTIONS", "FAQ"} <= keys
    # No support channel is configured anywhere, so no contact page is invented.
    assert "CONTACT" not in keys


@pytest.mark.django_db
def test_a_draft_is_not_listed_and_not_served():
    LegalDocument.objects.create(
        key=LegalDocument.Key.CONTACT,
        title_ar="تواصل معنا",
        body_ar="مسودة",
        version=1,
        active=False,
    )
    client = APIClient()

    assert "CONTACT" not in {item["key"] for item in client.get(LIST).json()["items"]}
    assert client.get(f"{LIST}CONTACT/").status_code == 404


@pytest.mark.django_db
def test_the_list_carries_titles_and_versions_but_not_the_words():
    item = next(
        row for row in APIClient().get(LIST).json()["items"] if row["key"] == "ABOUT"
    )

    assert item["titleAr"] == "من نحن"
    assert item["version"] == 1
    assert item["publishedAt"]
    assert "bodyAr" not in item


@pytest.mark.django_db
def test_a_published_page_is_served_in_full():
    body = APIClient().get(f"{LIST}PRIVACY/").json()

    assert body["key"] == "PRIVACY"
    assert "الموقع" in body["bodyAr"]
    # The words say what the app actually does: the location is foreground-only.
    assert "الخلفية" in body["bodyAr"]


@pytest.mark.django_db
def test_an_unknown_page_is_not_found():
    assert APIClient().get(f"{LIST}NOTHING/").status_code == 404


@pytest.mark.django_db
def test_one_version_of_a_page_is_active_at_a_time():
    _publish(LegalDocument.Key.CONTACT, title="تواصل معنا", body="النسخة الأولى", version=1)

    with pytest.raises(IntegrityError):
        _publish(LegalDocument.Key.CONTACT, title="تواصل معنا", body="الثانية", version=2)


@pytest.mark.django_db
def test_a_new_version_replaces_the_one_before_it():
    first = _publish(
        LegalDocument.Key.CONTACT, title="تواصل معنا", body="النسخة الأولى", version=1
    )
    first.active = False
    first.save(update_fields=["active"])
    _publish(LegalDocument.Key.CONTACT, title="تواصل معنا", body="النسخة الثانية", version=2)

    body = APIClient().get(f"{LIST}CONTACT/").json()

    assert body["version"] == 2
    assert body["bodyAr"] == "النسخة الثانية"


@pytest.mark.django_db
def test_the_pages_are_public():
    assert APIClient().get(LIST).status_code == 200
    assert APIClient().get(f"{LIST}ABOUT/").status_code == 200
