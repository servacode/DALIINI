"""The second draft of the pages replaces only the untouched first draft (DECISION-084)."""

import importlib
from typing import Any

import pytest
from django.apps import apps
from rest_framework.test import APIClient

from content_services.models import LegalDocument

second = importlib.import_module("content_services.migrations.0007_legal_drafts_v2")
first = importlib.import_module("content_services.migrations.0003_seed_legal_documents")
LIST = "/api/v1/public/legal/"


def _active(key: str) -> LegalDocument:
    return LegalDocument.objects.get(key=key, active=True)


@pytest.mark.django_db
def test_the_pages_describe_the_app_as_it_is_now() -> None:
    privacy = APIClient().get(f"{LIST}PRIVACY/").json()

    assert privacy["version"] == 2
    # The route's ends go to the platform's own engine, and a trip reads the location only
    # under its visible notice.
    assert "محرك المسارات" in privacy["bodyAr"]
    assert "الشاشة مقفلة" in privacy["bodyAr"]
    assert "١٨٠ يوماً" in privacy["bodyAr"]
    assert "OpenStreetMap" in APIClient().get(f"{LIST}TERMS/").json()["bodyAr"]
    for key in second.PAGES:
        assert _active(key).body_ar == second.PAGES[key]
        assert not LegalDocument.objects.get(key=key, version=1).active
    # The about page was not rewritten.
    assert _active("ABOUT").version == 1


def _back_to_first_draft(key: str, body: str | None = None) -> None:
    LegalDocument.objects.filter(key=key, version__gt=1).delete()
    LegalDocument.objects.filter(key=key, version=1).update(
        active=True, body_ar=body if body is not None else getattr(first, key)
    )


@pytest.mark.django_db
def test_an_operator_s_own_words_are_never_replaced() -> None:
    _back_to_first_draft("TERMS", body="شروط كتبها المشغّل بنفسه.")
    _back_to_first_draft("PRIVACY")

    second.publish(apps, None)

    assert _active("TERMS").body_ar == "شروط كتبها المشغّل بنفسه."
    assert _active("TERMS").version == 1
    assert _active("PRIVACY").body_ar == second.PRIVACY
    assert _active("PRIVACY").version == 2


@pytest.mark.django_db
def test_reversing_brings_the_first_draft_back() -> None:
    second.unpublish(apps, None)

    for key in second.PAGES:
        row: Any = _active(key)
        assert (row.version, row.body_ar) == (1, getattr(first, key))
