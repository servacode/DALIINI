# mypy: disable-error-code="no-untyped-call"
"""The smart admin console: tasks, alerts, quality, search, templates, timeline, bulk
report decisions, broadcasts, readiness, analytics periods, staff stats, CSV exports and
advertisement images."""

from __future__ import annotations

from datetime import time, timedelta
from io import BytesIO
from types import SimpleNamespace
from typing import Any

import pytest
from django.contrib.gis.geos import Point
from django.core.files.uploadedfile import SimpleUploadedFile
from django.db import connection
from django.test.utils import CaptureQueriesContext
from django.utils import timezone
from PIL import Image
from rest_framework.test import APIClient

from accounts.models import User
from admin_console.services import decide_application
from analytics.models import ProductAnalyticsEvent
from audit.models import AuditEvent
from business_hours.models import BusinessHour
from directory.models import CategoryProvince
from facilities.models import (
    Facility,
    FacilityApplication,
    FacilityMembership,
    FacilityReport,
    RejectionTemplate,
)
from notifications.models import Broadcast, Notification
from pharmacy_duty.coverage import day_bounds, local_today
from pharmacy_duty.models import DutyShift
from platform_settings.maintenance import invalidate_maintenance_cache
from platform_settings.models import PlatformSetting


def _report(facility: Facility, *, hours_ago: float = 0, **extra: Any) -> FacilityReport:
    report = FacilityReport.objects.create(facility=facility, reason="WRONG_INFO", **extra)
    if hours_ago:
        FacilityReport.objects.filter(pk=report.pk).update(
            created_at=timezone.now() - timedelta(hours=hours_ago)
        )
        report.refresh_from_db()
    return report


def _sibling(facility: Facility, name: str, **extra: Any) -> Facility:
    return Facility.objects.create(
        category=facility.category, province=facility.province, name_ar=name, **extra
    )


# ------------------------------------------------------------------------------ tasks


@pytest.mark.django_db
def test_tasks_center_counts_ages_and_overdue(admin_api: Any, facility: Facility) -> None:
    now = timezone.now()
    FacilityApplication.objects.create(
        facility=facility,
        kind="INITIAL",
        status="SUBMITTED",
        submitted_at=now - timedelta(hours=60),
    )
    waiting = _sibling(facility, "بانتظار التحقق", status="REVERIFICATION_REQUIRED")
    FacilityApplication.objects.create(
        facility=waiting,
        kind="REVERIFICATION",
        status="SUBMITTED",
        submitted_at=now - timedelta(hours=2),
    )
    _report(facility, hours_ago=1)
    _report(facility, hours_ago=2)
    _report(waiting, hours_ago=100)
    client = admin_api("admin.dashboard.read")

    body = client.get("/api/v1/admin/tasks/").json()

    assert body["slaHours"] == 48
    initial = body["applications"]["initial"]
    assert (initial["count"], initial["overdueCount"]) == (1, 1)
    assert initial["oldest"][0]["overdue"] is True
    assert initial["oldest"][0]["ageHours"] >= 59
    reverification = body["applications"]["reverification"]
    assert (reverification["count"], reverification["overdueCount"]) == (1, 0)
    reports = body["reports"]
    assert (reports["count"], reports["facilityCount"], reports["overdueCount"]) == (3, 2, 1)
    # Two open reports outrank one older report.
    assert [row["facilityId"] for row in reports["oldest"]] == [str(facility.pk), str(waiting.pk)]
    assert reports["oldest"][0]["openCount"] == 2
    assert reports["oldest"][0]["reasons"] == ["WRONG_INFO"]
    assert body["reverificationRequired"]["count"] == 1

    PlatformSetting.objects.filter(key="review.slaHours").update(value=1)
    body = client.get("/api/v1/admin/tasks/").json()
    assert body["slaHours"] == 1
    assert body["applications"]["reverification"]["oldest"][0]["overdue"] is True


@pytest.mark.django_db
def test_tasks_and_alerts_need_dashboard_permission(admin_api: Any) -> None:
    client = admin_api("admin.reviews.read")
    assert client.get("/api/v1/admin/tasks/").status_code == 403
    assert client.get("/api/v1/admin/alerts/").status_code == 403


# ----------------------------------------------------------------------------- alerts


def _alerts(client: Any, kind: str, entity_id: Any = None) -> list[dict[str, Any]]:
    items = client.get("/api/v1/admin/alerts/").json()["items"]
    return [
        item
        for item in items
        if item["kind"] == kind
        and (entity_id is None or (item["link"] or {}).get("entityId") == str(entity_id))
    ]


@pytest.mark.django_db
def test_duty_gap_alert_is_critical_until_the_next_two_days_are_covered(
    admin_api: Any, facility: Facility
) -> None:
    client = admin_api("admin.dashboard.read")
    province = facility.province_id
    # No duty category is public in the province yet: no roster, so no gap. (The launch
    # baseline's own Raqqa roster is empty and alerts on its own.)
    assert _alerts(client, "DUTY_GAP", province) == []
    CategoryProvince.objects.create(
        category=facility.category, province=facility.province, public_enabled=True
    )

    (gap,) = _alerts(client, "DUTY_GAP", province)
    assert gap["severity"] == "critical"
    assert gap["count"] == 14
    assert gap["link"] == {
        "entityType": "DUTY_ROSTER",
        "entityId": str(facility.province_id),
        "query": f"provinceId={facility.province_id}",
    }

    today = local_today()
    start, _ = day_bounds(today)
    _, end = day_bounds(today + timedelta(days=1))
    DutyShift.objects.create(facility=facility, starts_at=start, ends_at=end)
    (gap,) = _alerts(client, "DUTY_GAP", province)
    assert gap["severity"] == "warning"
    assert gap["count"] == 12

    # A suspended pharmacy's shifts do not cover anything.
    Facility.objects.filter(pk=facility.pk).update(status="SUSPENDED")
    assert _alerts(client, "DUTY_GAP", province)[0]["count"] == 14


@pytest.mark.django_db
def test_reported_overdue_stale_maintenance_and_zero_result_alerts(
    admin_api: Any, facility: Facility
) -> None:
    client = admin_api("admin.dashboard.read")
    for _ in range(3):
        _report(facility)
    FacilityApplication.objects.create(
        facility=facility,
        kind="INITIAL",
        status="SUBMITTED",
        submitted_at=timezone.now() - timedelta(hours=60),
    )
    stale = _sibling(facility, "قديمة", status="ACTIVE")
    Facility.objects.filter(pk=stale.pk).update(updated_at=timezone.now() - timedelta(days=100))
    for _ in range(3):
        ProductAnalyticsEvent.objects.create(
            name="search_zero_results",
            properties={
                "provinceId": str(facility.province_id),
                "categoryId": str(facility.category_id),
                "queryLength": 4,
            },
            occurred_at=timezone.now(),
        )
    PlatformSetting.objects.update_or_create(
        key="maintenance.enabled", defaults={"value_type": "BOOLEAN", "value": True}
    )
    invalidate_maintenance_cache()

    items = client.get("/api/v1/admin/alerts/").json()["items"]
    kinds = [item["kind"] for item in items]

    (reported,) = [item for item in items if item["kind"] == "REPORTED_FACILITY"]
    assert reported["count"] == 3
    assert reported["severity"] == "warning"
    assert reported["link"]["entityId"] == str(facility.pk)
    (overdue,) = [item for item in items if item["kind"] == "REVIEW_OVERDUE"]
    assert (overdue["count"], overdue["severity"]) == (1, "warning")
    (stale_alert,) = [item for item in items if item["kind"] == "STALE_FACILITY"]
    assert stale_alert["count"] == 1
    assert stale_alert["link"]["query"] == "status=ACTIVE&issue=STALE"
    (zero,) = [item for item in items if item["kind"] == "ZERO_RESULT_SEARCH"]
    assert zero["count"] == 3
    assert facility.category.name_ar in zero["titleAr"]
    assert "MAINTENANCE_ON" in kinds
    severities = [item["severity"] for item in items]
    assert severities == sorted(
        severities, key=lambda value: {"critical": 0, "warning": 1, "info": 2}[value]
    )


# ---------------------------------------------------------------------------- quality


@pytest.mark.django_db
def test_quality_score_issues_ordering_and_filter(admin_api: Any, facility: Facility) -> None:
    client = admin_api("admin.facilities.read")
    good = _sibling(
        facility,
        "مكتملة",
        status="ACTIVE",
        phone="+963933333333",
        location=Point(39.0, 35.9, srid=4326),
        last_verified_at=timezone.now(),
    )
    BusinessHour.objects.create(facility=good, weekday=0, opens_at=time(9), closes_at=time(17))

    detail = client.get(f"/api/v1/admin/facilities/{facility.pk}/").json()
    assert detail["qualityScore"] == 25
    assert detail["qualityIssues"] == [
        "NO_PHOTOS",
        "NO_HOURS",
        "NO_LOCATION",
        "NO_PHONE",
        "NOT_VERIFIED_RECENTLY",
    ]
    good_detail = client.get(f"/api/v1/admin/facilities/{good.pk}/").json()
    assert (good_detail["qualityScore"], good_detail["qualityIssues"]) == (90, ["NO_PHOTOS"])

    _report(good)
    listing = client.get("/api/v1/admin/facilities/?ordering=-qualityScore").json()["items"]
    assert [row["id"] for row in listing] == [str(good.pk), str(facility.pk)]
    assert listing[0]["qualityScore"] == 75
    assert "OPEN_REPORTS" in listing[0]["qualityIssues"]
    ascending = client.get("/api/v1/admin/facilities/?ordering=qualityScore").json()["items"]
    assert ascending[0]["id"] == str(facility.pk)

    only_phone = client.get("/api/v1/admin/facilities/?issue=NO_PHONE").json()["items"]
    assert [row["id"] for row in only_phone] == [str(facility.pk)]
    assert client.get("/api/v1/admin/facilities/?issue=NOPE").status_code == 400
    assert client.get("/api/v1/admin/facilities/?ordering=name").status_code == 400

    Facility.objects.filter(pk=good.pk).update(updated_at=timezone.now() - timedelta(days=120))
    Facility.objects.filter(pk=good.pk).update(last_verified_at=timezone.now() - timedelta(days=95))
    stale = client.get("/api/v1/admin/facilities/?issue=STALE").json()["items"]
    assert [row["id"] for row in stale] == [str(good.pk)]


@pytest.mark.django_db
def test_quality_list_query_count_does_not_grow_with_rows(
    admin_api: Any, facility: Facility
) -> None:
    client = admin_api("admin.facilities.read")

    def count() -> int:
        with CaptureQueriesContext(connection) as queries:
            assert client.get("/api/v1/admin/facilities/").status_code == 200
        return len(queries)

    few = count()
    for index in range(6):
        _sibling(facility, f"منشأة {index}", status="ACTIVE")
    assert count() == few


# ----------------------------------------------------------------------------- search


@pytest.mark.django_db
def test_global_search_respects_permissions_and_masks_phones(
    admin_api: Any, facility: Facility, user: User
) -> None:
    FacilityApplication.objects.create(facility=facility, kind="INITIAL", status="SUBMITTED")

    facilities_only = admin_api("admin.facilities.read")
    body = facilities_only.get("/api/v1/admin/search/?q=اختبار").json()
    assert [group["type"] for group in body["groups"]] == ["FACILITY", "USER"]
    assert body["groups"][0]["items"][0]["id"] == str(facility.pk)

    by_phone = facilities_only.get("/api/v1/admin/search/?q=0900000001").json()
    users = next(group for group in by_phone["groups"] if group["type"] == "USER")["items"]
    assert [item["id"] for item in users] == [str(user.pk)]
    assert users[0]["subtitle"].endswith("0001")
    assert user.phone not in users[0]["subtitle"]

    user_reader = admin_api("admin.users.read")
    body = user_reader.get("/api/v1/admin/search/?q=Test User").json()
    assert [group["type"] for group in body["groups"]] == ["USER"]
    assert body["groups"][0]["items"][0]["subtitle"] == user.phone

    reviewer = admin_api("admin.reviews.read")
    body = reviewer.get("/api/v1/admin/search/?q=اختبار").json()
    assert [group["type"] for group in body["groups"]] == ["APPLICATION"]
    assert body["groups"][0]["items"][0]["titleAr"] == facility.name_ar

    assert (
        admin_api("admin.dashboard.read").get("/api/v1/admin/search/?q=ab").json()["groups"] == []
    )
    assert reviewer.get("/api/v1/admin/search/?q=a").status_code == 400
    outsider = APIClient()
    outsider.force_authenticate(user=user)
    assert outsider.get("/api/v1/admin/search/?q=اختبار").status_code == 403


# ---------------------------------------------------------------- rejection templates


@pytest.mark.django_db
def test_rejection_templates_are_seeded_and_managed(admin_api: Any) -> None:
    reader = admin_api("admin.reviews.read")
    seeded = reader.get("/api/v1/admin/rejection-templates/").json()["items"]
    assert len(seeded) == 6
    assert {item["titleAr"] for item in seeded} >= {"الترخيص غير واضح", "منشأة مكررة"}
    assert (
        reader.post(
            "/api/v1/admin/rejection-templates/", {"titleAr": "x", "bodyAr": "y"}, format="json"
        ).status_code
        == 403
    )

    decider = admin_api("admin.reviews.read", "admin.reviews.decide")
    created = decider.post(
        "/api/v1/admin/rejection-templates/",
        {"titleAr": "صور ناقصة", "bodyAr": "يرجى رفع صور واضحة.", "sortOrder": 70},
        format="json",
    )
    assert created.status_code == 201
    template_id = created.json()["id"]
    updated = decider.put(
        f"/api/v1/admin/rejection-templates/{template_id}/", {"active": False}, format="json"
    )
    assert updated.status_code == 200
    assert updated.json()["active"] is False
    assert updated.json()["titleAr"] == "صور ناقصة"
    active = decider.get("/api/v1/admin/rejection-templates/?active=true").json()["items"]
    assert template_id not in {item["id"] for item in active}
    assert decider.delete(f"/api/v1/admin/rejection-templates/{template_id}/").status_code == 204
    assert not RejectionTemplate.objects.filter(pk=template_id).exists()
    actions = set(AuditEvent.objects.filter(target_id=template_id).values_list("action", flat=True))
    assert actions == {
        "rejection_template.created",
        "rejection_template.updated",
        "rejection_template.deleted",
    }


# ---------------------------------------------------------------------------- timeline


@pytest.mark.django_db
def test_facility_timeline_merges_applications_reports_and_audit(
    admin_api: Any, facility: Facility, user: User
) -> None:
    operator = admin_api("admin.facilities.read", "admin.reports.manage")
    application = FacilityApplication.objects.create(
        facility=facility,
        kind="INITIAL",
        status="SUBMITTED",
        submitted_at=timezone.now() - timedelta(hours=3),
    )
    decide_application(
        request=SimpleNamespace(user=operator.user, request_id="req-approve"),
        application_id=application.pk,
        approve=True,
    )
    report = _report(facility)
    operator.post(f"/api/v1/admin/reports/{report.pk}/resolve/", {}, format="json")

    body = operator.get(f"/api/v1/admin/facilities/{facility.pk}/timeline/").json()
    kinds = [item["kind"] for item in body["items"]]

    assert {"APPLICATION_SUBMITTED", "APPLICATION_APPROVED", "REPORT_CREATED"} <= set(kinds)
    assert "REPORT_RESOLVED" in kinds
    approved = next(item for item in body["items"] if item["kind"] == "APPLICATION_APPROVED")
    assert approved["actorName"] == operator.user.name
    assert approved["requestId"] == "req-approve"
    stamps = [item["at"] for item in body["items"]]
    assert stamps == sorted(stamps, reverse=True)
    # Decisions are shown once, from the rows themselves, not again as raw audit entries.
    assert "facility_application.approved" not in {item["action"] for item in body["items"]}
    assert (
        admin_api("admin.reviews.read")
        .get(f"/api/v1/admin/facilities/{facility.pk}/timeline/")
        .status_code
        == 403
    )


# ------------------------------------------------------------------------ bulk reports


@pytest.mark.django_db
def test_bulk_report_decisions_report_each_id(admin_api: Any, facility: Facility) -> None:
    first, second = _report(facility), _report(facility)
    closed = _report(facility, status="DISMISSED")
    missing = "00000000-0000-0000-0000-000000000000"
    client = admin_api("admin.reports.manage")

    response = client.post(
        "/api/v1/admin/reports/bulk/",
        {
            "ids": [str(first.pk), str(second.pk), str(closed.pk), missing],
            "action": "resolve",
            "note": "تم التصحيح",
        },
        format="json",
    )

    assert response.status_code == 200
    body = response.json()
    assert body["decided"] == 2
    outcomes = {row["id"]: (row["outcome"], row["status"]) for row in body["results"]}
    assert outcomes == {
        str(first.pk): ("DECIDED", "RESOLVED"),
        str(second.pk): ("DECIDED", "RESOLVED"),
        str(closed.pk): ("NOT_OPEN", "DISMISSED"),
        missing: ("NOT_FOUND", None),
    }
    audited = AuditEvent.objects.filter(action="facility_report.resolved")
    assert audited.count() == 2
    assert audited.earliest("created_at").metadata["note"] == "تم التصحيح"
    assert (
        client.post(
            "/api/v1/admin/reports/bulk/", {"ids": [], "action": "resolve"}, format="json"
        ).status_code
        == 400
    )
    assert (
        admin_api("admin.reports.read")
        .post(
            "/api/v1/admin/reports/bulk/",
            {"ids": [str(first.pk)], "action": "dismiss"},
            format="json",
        )
        .status_code
        == 403
    )


# -------------------------------------------------------------------------- broadcast


@pytest.mark.django_db
def test_broadcast_reaches_the_audience_is_audited_and_rate_limited(
    admin_api: Any,
    facility: Facility,
    user: User,
    django_capture_on_commit_callbacks: Any,
) -> None:
    FacilityMembership.objects.create(facility=facility, user=user, role="OWNER")
    resident = User.objects.create_user(
        phone="+963900000002", name="Resident", province=facility.province
    )
    User.objects.create_user(phone="+963900000003", name="Elsewhere")
    client = admin_api("admin.notifications.send")
    url = "/api/v1/admin/notifications/broadcast/"

    with django_capture_on_commit_callbacks() as callbacks:
        owners = client.post(
            url, {"titleAr": "تنبيه", "bodyAr": "للمالكين", "audience": "OWNERS"}, format="json"
        )
    assert owners.status_code == 201
    assert owners.json()["recipientCount"] == 1
    assert len(callbacks) == 1  # the push fan-out, queued after commit
    assert Notification.objects.filter(user=user, type="platform.broadcast").count() == 1

    local = client.post(
        url,
        {
            "titleAr": "تنبيه",
            "bodyAr": "لأهل المحافظة",
            "audience": "ALL",
            "provinceId": str(facility.province_id),
        },
        format="json",
    )
    assert local.json()["recipientCount"] == 1
    assert Notification.objects.filter(user=resident, type="platform.broadcast").exists()
    everyone = client.post(
        url, {"titleAr": "ت", "bodyAr": "للجميع", "audience": "ALL"}, format="json"
    )
    assert everyone.json()["recipientCount"] == User.objects.filter(is_active=True).count()
    assert AuditEvent.objects.filter(action="notification.broadcast.sent").count() == 3

    for _ in range(2):
        assert (
            client.post(
                url, {"titleAr": "ت", "bodyAr": "ب", "audience": "OWNERS"}, format="json"
            ).status_code
            == 201
        )
    limited = client.post(url, {"titleAr": "ت", "bodyAr": "ب", "audience": "OWNERS"}, format="json")
    assert limited.status_code == 429
    assert limited.json()["code"] == "THROTTLED"

    history = client.get("/api/v1/admin/notifications/broadcasts/?limit=2").json()
    assert set(history) == {"items", "nextCursor", "hasMore"}
    assert history["hasMore"] is True
    assert history["items"][0]["actorName"] == client.user.name
    assert Broadcast.objects.count() == 5
    assert (
        admin_api("admin.settings.read")
        .post(url, {"titleAr": "ت", "bodyAr": "ب", "audience": "ALL"}, format="json")
        .status_code
        == 403
    )


# -------------------------------------------------------------------------- readiness


@pytest.mark.django_db
def test_province_readiness_checklist(admin_api: Any, facility: Facility) -> None:
    client = admin_api("admin.provinces.read")
    url = f"/api/v1/admin/provinces/{facility.province_id}/readiness/"

    body = client.get(url).json()
    items = {item["code"]: item["ok"] for item in body["items"]}
    assert items == {
        "PROVINCE_ACTIVE": True,
        "CATEGORY_PUBLIC": False,
        "MIN_ACTIVE_FACILITIES": False,
        "DUTY_COVERAGE": True,  # no duty category offered yet: not applicable
        "EMERGENCY_NUMBERS": True,  # the seeded national numbers
    }
    assert body["ready"] is False
    assert body["minActiveFacilities"] == 5

    CategoryProvince.objects.create(
        category=facility.category, province=facility.province, public_enabled=True
    )
    PlatformSetting.objects.filter(key="readiness.minActiveFacilities").update(value=1)
    body = client.get(url).json()
    items = {item["code"]: item["ok"] for item in body["items"]}
    assert items["CATEGORY_PUBLIC"] and items["MIN_ACTIVE_FACILITIES"]
    assert items["DUTY_COVERAGE"] is False
    today = local_today()
    start, _ = day_bounds(today)
    _, end = day_bounds(today + timedelta(days=13))
    DutyShift.objects.create(facility=facility, starts_at=start, ends_at=end)
    assert client.get(url).json()["ready"] is True
    assert admin_api("admin.dashboard.read").get(url).status_code == 403


# -------------------------------------------------------------------------- analytics


@pytest.mark.django_db
def test_analytics_period_and_previous_comparison(admin_api: Any) -> None:
    now = timezone.now()
    for days_ago in (1, 2, 12):
        ProductAnalyticsEvent.objects.create(
            name="search_submitted", properties={}, occurred_at=now - timedelta(days=days_ago)
        )
    client = admin_api("admin.analytics.read")
    since = (now - timedelta(days=10)).isoformat()
    body = client.get("/api/v1/admin/analytics/", {"from": since, "to": now.isoformat()}).json()
    assert body["searches"] == 2
    assert body["previous"]["searches"] == 1
    assert body["previous"]["to"] == body["from"]
    default = client.get("/api/v1/admin/analytics/").json()
    assert default["searches"] == 3
    assert client.get("/api/v1/admin/analytics/", {"from": "nonsense"}).status_code == 400
    assert (
        client.get("/api/v1/admin/analytics/", {"from": now.isoformat(), "to": since}).status_code
        == 400
    )


@pytest.mark.django_db
def test_staff_performance_per_reviewer(admin_api: Any, facility: Facility) -> None:
    reviewer = admin_api("admin.analytics.read")
    now = timezone.now()
    for hours, approve in ((4, True), (8, False)):
        target = _sibling(facility, f"طلب {hours}")
        application = FacilityApplication.objects.create(
            facility=target,
            kind="INITIAL",
            status="SUBMITTED",
            submitted_at=now - timedelta(hours=hours),
        )
        decide_application(
            request=SimpleNamespace(user=reviewer.user, request_id=""),
            application_id=application.pk,
            approve=approve,
            reason="" if approve else "ناقص",
        )
    report = _report(facility)
    FacilityReport.objects.filter(pk=report.pk).update(
        status="RESOLVED", resolved_by=reviewer.user, resolved_at=now
    )

    body = reviewer.get("/api/v1/admin/analytics/staff/").json()
    (row,) = body["items"]
    assert row["userId"] == str(reviewer.user.pk)
    assert (row["decisions"], row["approvals"], row["rejections"]) == (2, 1, 1)
    assert row["medianDecisionHours"] == pytest.approx(6, abs=0.1)
    assert row["reportDecisions"] == 1


# ---------------------------------------------------------------------------- exports


def _csv(response: Any) -> str:
    return b"".join(response.streaming_content).decode("utf-8")


@pytest.mark.django_db
def test_csv_exports_have_bom_arabic_filters_and_permissions(
    admin_api: Any, facility: Facility
) -> None:
    client = admin_api("admin.facilities.read", "admin.reports.read", "admin.audit.read")
    response = client.get("/api/v1/admin/exports/facilities.csv", HTTP_ACCEPT="text/csv")
    assert response.status_code == 200
    assert response["Content-Type"].startswith("text/csv")
    assert "attachment" in response["Content-Disposition"]
    text = _csv(response)
    assert text.startswith("﻿id,nameAr")
    assert facility.name_ar in text

    _report(facility, note="=HYPERLINK(1)")
    reports = _csv(client.get("/api/v1/admin/exports/reports.csv?status=OPEN"))
    assert "'=HYPERLINK(1)" in reports
    assert _csv(client.get("/api/v1/admin/exports/reports.csv?status=DISMISSED")).count("\n") == 1

    AuditEvent.objects.create(action="probe.action", target_type="X", target_id="1")
    audit = _csv(client.get("/api/v1/admin/exports/audit.csv?action=probe"))
    assert "probe.action" in audit
    assert client.get("/api/v1/admin/exports/audit.csv?from=bad").status_code == 400
    denied = admin_api("admin.dashboard.read").get(
        "/api/v1/admin/exports/facilities.csv", HTTP_ACCEPT="text/csv"
    )
    assert denied.status_code == 403
    assert denied.json()["code"] == "PERMISSION_DENIED"


# ------------------------------------------------------------------------- ad images


class _Storage:
    saved: dict[str, bytes] = {}

    def save(self, key: str, content: Any) -> str:
        self.saved[key] = content.read()
        return key

    def url(self, key: str) -> str:
        return f"https://media.example/{key}"


def _image(fmt: str, size: tuple[int, int]) -> SimpleUploadedFile:
    buffer = BytesIO()
    Image.new("RGB", size, (200, 30, 30)).save(buffer, format=fmt)
    return SimpleUploadedFile(f"ad.{fmt.lower()}", buffer.getvalue())


@pytest.mark.django_db
def test_ad_image_upload_validates_and_stores_publicly(
    admin_api: Any, monkeypatch: pytest.MonkeyPatch
) -> None:
    monkeypatch.setattr("content_services.media.PublicS3Storage", _Storage)
    client = admin_api("admin.ads.manage")
    url = "/api/v1/admin/ads/images/"

    created = client.post(url, {"file": _image("PNG", (600, 300))}, format="multipart")
    assert created.status_code == 201
    body = created.json()
    assert body["imageKey"].startswith("ads/") and body["imageKey"].endswith(".jpg")
    assert body["url"] == f"https://media.example/{body['imageKey']}"
    assert (body["width"], body["height"]) == (600, 300)
    assert _Storage.saved[body["imageKey"]][:2] == b"\xff\xd8"  # re-encoded as JPEG
    assert AuditEvent.objects.filter(action="advertisement.image.uploaded").exists()

    for upload in (
        _image("PNG", (50, 50)),
        _image("GIF", (300, 300)),
        SimpleUploadedFile("big.png", b"\x89PNG" + b"0" * (2 * 1024 * 1024)),
        SimpleUploadedFile("text.png", b"not an image"),
    ):
        response = client.post(url, {"file": upload}, format="multipart")
        assert response.status_code == 400, upload.name
    assert (
        admin_api("admin.ads.read")
        .post(url, {"file": _image("PNG", (600, 300))}, format="multipart")
        .status_code
        == 403
    )


@pytest.mark.django_db
def test_new_permissions_are_granted_alongside_existing_ones() -> None:
    from accounts.models import AdminPermission

    codes = set(AdminPermission.objects.values_list("code", flat=True))
    assert {
        "admin.notifications.send",
        "admin.content.read",
        "admin.content.manage",
        "admin.duty.read",
        "admin.duty.manage",
    } <= codes
    assert PlatformSetting.objects.get(key="review.slaHours").value == 48
