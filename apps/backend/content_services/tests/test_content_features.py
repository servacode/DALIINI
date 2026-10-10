"""Content pages (on the versioned legal documents), FAQ, emergency numbers and contact."""

from __future__ import annotations

from typing import Any

import pytest
from rest_framework.test import APIClient

from audit.models import AuditEvent
from content_services.models import ContactMessage, EmergencyNumber, LegalDocument
from facilities.models import Facility

PAGES = "/api/v1/admin/content/pages/"


# ------------------------------------------------------------------------------ pages


@pytest.mark.django_db
def test_builtin_legal_pages_are_content_pages() -> None:
    response = APIClient().get("/api/v1/content/pages/privacy/")
    assert response.status_code == 200
    body = response.json()
    # Version 3: the second draft (0007, DECISION-084), approved without its draft note (0008).
    assert (body["slug"], body["kind"], body["version"]) == ("privacy", "LEGAL", 3)
    assert response["Cache-Control"] == "public, max-age=300"
    assert APIClient().get("/api/v1/content/pages/faq/").json()["kind"] == "FAQ"
    assert APIClient().get("/api/v1/content/pages/nothing-here/").status_code == 404


@pytest.mark.django_db
def test_page_lifecycle_versions_publish_and_delete(admin_api: Any) -> None:
    editor = admin_api("admin.content.read", "admin.content.manage")
    created = editor.post(
        PAGES,
        {"slug": "How-To-Report", "kind": "PAGE", "titleAr": "كيف تبلّغ", "bodyAr": "الخطوة 1"},
        format="json",
    )
    assert created.status_code == 201
    assert created.json()["slug"] == "how-to-report"
    assert created.json()["published"] is False
    public = "/api/v1/content/pages/how-to-report/"
    assert APIClient().get(public).status_code == 404

    # A draft that never went live is edited in place.
    draft = editor.put(f"{PAGES}how-to-report/", {"bodyAr": "الخطوة 1 و2"}, format="json").json()
    assert draft["version"] == 1
    published = editor.put(f"{PAGES}how-to-report/", {"published": True}, format="json").json()
    assert published["published"] is True and published["publishedVersion"] == 1
    assert APIClient().get(public).json()["bodyAr"] == "الخطوة 1 و2"

    # Editing a live page writes a new version and leaves the live one alone until published.
    edited = editor.put(f"{PAGES}how-to-report/", {"titleAr": "طريقة التبليغ"}, format="json")
    assert edited.json()["version"] == 2
    assert edited.json()["hasUnpublishedChanges"] is True
    assert APIClient().get(public).json()["titleAr"] == "كيف تبلّغ"
    editor.put(f"{PAGES}how-to-report/", {"published": True}, format="json")
    live = APIClient().get(public).json()
    assert (live["titleAr"], live["version"]) == ("طريقة التبليغ", 2)
    assert LegalDocument.objects.filter(key="HOW-TO-REPORT").count() == 2

    editor.put(f"{PAGES}how-to-report/", {"published": False}, format="json")
    assert APIClient().get(public).status_code == 404
    # Pages operators add never leak into the legacy list, whose keys are a fixed enum.
    legacy = {item["key"] for item in APIClient().get("/api/v1/public/legal/").json()["items"]}
    assert "HOW-TO-REPORT" not in legacy

    duplicate = editor.post(
        PAGES, {"slug": "how-to-report", "titleAr": "x", "bodyAr": "y"}, format="json"
    )
    assert (duplicate.status_code, duplicate.json()["code"]) == (409, "CONTENT_PAGE_EXISTS")
    builtin = editor.delete(f"{PAGES}privacy/")
    assert (builtin.status_code, builtin.json()["code"]) == (409, "CONTENT_PAGE_BUILT_IN")
    assert editor.delete(f"{PAGES}how-to-report/").status_code == 204
    assert not LegalDocument.objects.filter(key="HOW-TO-REPORT").exists()
    actions = set(AuditEvent.objects.values_list("action", flat=True))
    assert {"content_page.created", "content_page.updated", "content_page.deleted"} <= actions


@pytest.mark.django_db
def test_page_permissions_and_listing(admin_api: Any) -> None:
    reader = admin_api("admin.content.read")
    slugs = {item["slug"] for item in reader.get(PAGES).json()["items"]}
    assert {"about", "privacy", "terms", "instructions", "faq"} <= slugs
    assert reader.get(f"{PAGES}terms/").json()["builtIn"] is True
    assert (
        reader.post(PAGES, {"slug": "x", "titleAr": "x", "bodyAr": "y"}, format="json").status_code
        == 403
    )
    assert reader.put(f"{PAGES}terms/", {"published": False}, format="json").status_code == 403
    assert admin_api("admin.ads.read").get(PAGES).status_code == 403


# -------------------------------------------------------------------------------- faq


@pytest.mark.django_db
def test_faq_public_list_shows_published_entries_in_order(admin_api: Any) -> None:
    editor = admin_api("admin.content.read", "admin.content.manage")
    url = "/api/v1/admin/content/faq/"
    second = editor.post(
        url,
        {"questionAr": "س2", "answerAr": "ج2", "sortOrder": 20, "published": True},
        format="json",
    ).json()
    editor.post(
        url,
        {"questionAr": "س1", "answerAr": "ج1", "sortOrder": 10, "published": True},
        format="json",
    )
    hidden = editor.post(url, {"questionAr": "مخفي", "answerAr": "لا"}, format="json").json()
    assert hidden["published"] is False

    response = APIClient().get("/api/v1/content/faq/")
    assert response["Cache-Control"] == "public, max-age=300"
    assert [item["questionAr"] for item in response.json()["items"]] == ["س1", "س2"]

    assert (
        editor.put(f"{url}{second['id']}/", {"published": False}, format="json").status_code == 200
    )
    assert [
        item["questionAr"] for item in APIClient().get("/api/v1/content/faq/").json()["items"]
    ] == ["س1"]
    assert editor.delete(f"{url}{hidden['id']}/").status_code == 204
    assert len(editor.get(url).json()["items"]) == 2


# ---------------------------------------------------------------- emergency numbers


@pytest.mark.django_db
def test_emergency_numbers_national_then_provincial(admin_api: Any, facility: Facility) -> None:
    national = APIClient().get("/api/v1/emergency-numbers/").json()["items"]
    assert [(item["phone"], item["kind"], item["scope"]) for item in national] == [
        ("110", "AMBULANCE", "NATIONAL"),
        ("113", "FIRE", "NATIONAL"),
        ("112", "POLICE", "NATIONAL"),
    ]
    assert "adminNote" not in national[0]
    seeded = EmergencyNumber.objects.get(phone="110")
    assert "يتحقق" in seeded.admin_note

    editor = admin_api("admin.content.read", "admin.content.manage")
    created = editor.post(
        "/api/v1/admin/emergency-numbers/",
        {
            "provinceId": str(facility.province_id),
            "labelAr": "مشفى الرقة الوطني",
            "phone": "0222123456",
            "kind": "HOSPITAL",
        },
        format="json",
    )
    assert created.status_code == 201
    assert created.json()["provinceNameAr"] == facility.province.name_ar
    listed = (
        APIClient()
        .get(f"/api/v1/emergency-numbers/?provinceId={facility.province_id}")
        .json()["items"]
    )
    assert [item["scope"] for item in listed] == ["NATIONAL"] * 3 + ["PROVINCE"]
    assert listed[-1]["provinceId"] == str(facility.province_id)

    number_id = created.json()["id"]
    editor.put(f"/api/v1/admin/emergency-numbers/{number_id}/", {"active": False}, format="json")
    listed = (
        APIClient()
        .get(f"/api/v1/emergency-numbers/?provinceId={facility.province_id}")
        .json()["items"]
    )
    assert len(listed) == 3
    assert APIClient().get("/api/v1/emergency-numbers/?provinceId=nope").status_code == 400
    bad = editor.post(
        "/api/v1/admin/emergency-numbers/",
        {"labelAr": "x", "phone": "call me", "kind": "OTHER"},
        format="json",
    )
    assert bad.status_code == 400
    assert (
        admin_api("admin.content.read")
        .post(
            "/api/v1/admin/emergency-numbers/",
            {"labelAr": "x", "phone": "110", "kind": "OTHER"},
            format="json",
        )
        .status_code
        == 403
    )


# ------------------------------------------------------------------------- contact


@pytest.mark.django_db
def test_contact_form_validates_and_throttles_by_remote_address(admin_api: Any) -> None:
    client = APIClient()
    url = "/api/v1/contact/"
    ok = client.post(
        url,
        {
            "name": "سارة",
            "phone": "0933 123 456",
            "message": "المعلومات قديمة",
            "kind": "CORRECTION",
        },
        format="json",
        REMOTE_ADDR="10.0.0.1",
    )
    assert ok.status_code == 201
    message = ContactMessage.objects.get(pk=ok.json()["id"])
    assert (message.phone, message.kind, message.user_id) == ("+963933123456", "CORRECTION", None)
    assert (
        client.post(
            url,
            {"name": "x", "phone": "123", "message": "y"},
            format="json",
            REMOTE_ADDR="10.0.0.1",
        ).status_code
        == 400
    )
    assert (
        client.post(
            url, {"name": "x", "message": "y" * 1001}, format="json", REMOTE_ADDR="10.0.0.1"
        ).status_code
        == 400
    )

    # Rejected requests count too: this is the fourth from the address, and a forged
    # X-Forwarded-For does not make it someone else.
    throttled = client.post(
        url,
        {"name": "x", "message": "y"},
        format="json",
        REMOTE_ADDR="10.0.0.1",
        HTTP_X_FORWARDED_FOR="203.0.113.9",
    )
    assert throttled.status_code == 429
    assert throttled.json()["code"] == "THROTTLED"
    assert (
        client.post(
            url, {"name": "x", "message": "y"}, format="json", REMOTE_ADDR="10.0.0.2"
        ).status_code
        == 201
    )

    reader = admin_api("admin.content.read")
    inbox = reader.get("/api/v1/admin/contact-messages/?status=open").json()
    assert set(inbox) == {"items", "nextCursor", "hasMore"}
    assert len(inbox["items"]) == 2
    handle = f"/api/v1/admin/contact-messages/{message.pk}/handle/"
    assert reader.post(handle, {}, format="json").status_code == 403
    manager = admin_api("admin.content.manage")
    handled = manager.post(handle, {"note": "اتصلنا بها"}, format="json").json()
    assert handled["handled"] is True
    assert manager.post(handle, {}, format="json").json()["handledAt"] == handled["handledAt"]
    assert AuditEvent.objects.filter(action="contact_message.handled").count() == 1
    assert len(reader.get("/api/v1/admin/contact-messages/?status=open").json()["items"]) == 1
    assert reader.get("/api/v1/admin/contact-messages/?kind=correction").json()["items"][0][
        "id"
    ] == str(message.pk)
