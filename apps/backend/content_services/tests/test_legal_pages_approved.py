"""The approved privacy and terms pages carry no draft note, and approving can be undone."""

import importlib

import pytest
from django.apps import apps

from content_services.models import LegalDocument

approved = importlib.import_module("content_services.migrations.0008_legal_pages_approved")


@pytest.mark.django_db
def test_the_approved_pages_say_nothing_of_a_draft() -> None:
    for key in approved.KEYS:
        page = LegalDocument.objects.get(key=key, active=True)
        assert approved.DRAFT_NOTE not in page.body_ar
        assert "مسودة" not in page.body_ar


@pytest.mark.django_db
def test_undoing_the_approval_brings_the_draft_back() -> None:
    approved.unapprove(apps, None)

    for key in approved.KEYS:
        assert approved.DRAFT_NOTE in LegalDocument.objects.get(key=key, active=True).body_ar


@pytest.mark.django_db
def test_an_approved_page_is_not_approved_twice() -> None:
    before = LegalDocument.objects.count()

    approved.approve(apps, None)

    assert LegalDocument.objects.count() == before
