# mypy: disable-error-code="no-untyped-call"
"""Additive Admin, public and owner fields (names, trust, reports, insights, filters)."""

from datetime import timedelta
from types import SimpleNamespace
from typing import Any

import pytest
from django.contrib.gis.geos import Point
from django.core.cache import cache
from django.utils import timezone
from rest_framework.test import APIClient

from accounts.models import AdminRole, User
from admin_console.review import normalize_name
from admin_console.services import decide_application
from analytics.models import ProductAnalyticsEvent
from audit.models import AuditEvent
from directory.models import CategoryProvince
from facilities.models import (
    Facility,
    FacilityApplication,
    FacilityMembership,
    FacilityReport,
)
from facilities.services import submit_facility
from locations.models import City
from pharmacy_duty.models import DutyShift


@pytest.fixture
def owned(facility: Facility, user: User) -> Facility:
    CategoryProvince.objects.create(
        category=facility.category,
        province=facility.province,
        public_enabled=True,
        owner_registration_enabled=True,
    )
    FacilityMembership.objects.create(facility=facility, user=user, role="OWNER")
    facility.location = Point(39.01, 35.95, srid=4326)
    facility.phone = "+963933333333"
    facility.save()
    return facility


def _owner_client(user: User) -> APIClient:
    client = APIClient()
    client.force_authenticate(user=user)
    return client


@pytest.mark.django_db
def test_admin_facility_list_and_detail_carry_names_and_owner(
    admin_api: Any, owned: Facility, user: User
) -> None:
    client = admin_api("admin.facilities.read")
    item = client.get("/api/v1/admin/facilities/").json()["items"][0]
    assert item["categoryNameAr"] == owned.category.name_ar
    assert item["provinceNameAr"] == owned.province.name_ar
    assert item["ownerName"] == user.name
    assert item["ownerPhone"] == user.phone
    detail = client.get(f"/api/v1/admin/facilities/{owned.pk}/").json()
    assert detail["ownerPhone"] == user.phone


@pytest.mark.django_db
def test_review_detail_has_previous_duplicates_images_and_location(
    admin_api: Any, owned: Facility, user: User
) -> None:
    operator = admin_api("admin.reviews.read", "admin.facilities.read")
    first = submit_facility(actor=user, facility=owned)
    decide_application(
        request=SimpleNamespace(user=operator.user, request_id=""),
        application_id=first.pk,
        approve=True,
    )
    owned.refresh_from_db()
    assert owned.last_verified_at is not None
    owned.status = Facility.Status.REVERIFICATION_REQUIRED
    owned.name_ar = "صيدلية جديدة"
    owned.save()
    second = submit_facility(actor=user, facility=owned)
    # A lookalike 50 m away and a namesake far away with the same phone.
    Facility.objects.create(
        category=owned.category,
        province=owned.province,
        name_ar="صيدليّة  جديدة",
        location=Point(39.0105, 35.95, srid=4326),
        status="ACTIVE",
    )
    Facility.objects.create(
        category=owned.category,
        province=owned.province,
        name_ar="أخرى",
        phone=owned.phone,
        status="ACTIVE",
    )
    Facility.objects.create(
        category=owned.category,
        province=owned.province,
        name_ar="مختلفة",
        location=Point(39.0106, 35.95, srid=4326),
        status="ACTIVE",
    )

    body = operator.get(f"/api/v1/admin/applications/{second.pk}/").json()

    assert body["kind"] == "REVERIFICATION"
    assert body["previous"]["nameAr"] == "صيدلية اختبار"
    assert body["previous"]["approvedAt"]
    assert body["snapshot"]["nameAr"] == "صيدلية جديدة"
    assert body["snapshot"]["phone"] == owned.phone
    assert body["location"] == {"latitude": 35.95, "longitude": 39.01}
    assert body["publicImages"] == []
    reasons = sorted(tuple(item["reasons"]) for item in body["duplicates"])
    assert reasons == [("SAME_NAME_NEARBY",), ("SAME_PHONE",)]
    assert body["ownerName"] == user.name


def test_normalize_name_ignores_diacritics_and_spacing() -> None:
    assert normalize_name(" صيدليّة  الأمل ") == normalize_name("صيدلية الامل")


@pytest.mark.django_db
def test_audit_date_filters(admin_api: Any, facility: Facility, user: User) -> None:
    client = admin_api("admin.audit.read")
    old = AuditEvent.objects.create(actor=user, action="old", target_type="X", target_id="1")
    AuditEvent.objects.filter(pk=old.pk).update(created_at=timezone.now() - timedelta(days=10))
    AuditEvent.objects.create(actor=user, action="new", target_type="X", target_id="2")
    since = (timezone.now() - timedelta(days=1)).date().isoformat()
    actions = {
        e["action"] for e in client.get(f"/api/v1/admin/audit/?from={since}").json()["items"]
    }
    assert actions == {"new"}
    until = (timezone.now() - timedelta(days=5)).date().isoformat()
    actions = {e["action"] for e in client.get(f"/api/v1/admin/audit/?to={until}").json()["items"]}
    assert actions == {"old"}
    assert client.get("/api/v1/admin/audit/?from=yesterday").status_code == 400


@pytest.mark.django_db
def test_users_role_filter(admin_api: Any, user: User) -> None:
    client = admin_api("admin.users.read")
    role = AdminRole.objects.get(code="fixture-1")
    by_code = client.get(f"/api/v1/admin/users/?role={role.code}").json()["items"]
    assert [u["id"] for u in by_code] == [str(client.user.pk)]
    by_id = client.get(f"/api/v1/admin/users/?role={role.pk}").json()["items"]
    assert [u["id"] for u in by_id] == [str(client.user.pk)]
    none = client.get("/api/v1/admin/users/?role=none").json()["items"]
    assert [u["id"] for u in none] == [str(user.pk)]


@pytest.mark.django_db
def test_dashboard_and_analytics_kpis(admin_api: Any, owned: Facility, user: User) -> None:
    client = admin_api("admin.dashboard.read", "admin.analytics.read")
    now = timezone.now()
    DutyShift.objects.create(
        facility=owned, starts_at=now - timedelta(hours=1), ends_at=now + timedelta(hours=1)
    )
    FacilityReport.objects.create(facility=owned, reason="WRONG_HOURS")
    for name in ("search_submitted", "search_submitted", "search_zero_results", "facility_view"):
        ProductAnalyticsEvent.objects.create(name=name, properties={}, occurred_at=now)
    application = FacilityApplication.objects.create(
        facility=owned,
        kind="INITIAL",
        status="APPROVED",
        submitted_at=now - timedelta(hours=10),
        reviewed_at=now,
    )
    assert application.pk

    dashboard = client.get("/api/v1/admin/dashboard/").json()
    assert dashboard["dutyActiveNow"] == 1
    assert dashboard["newUsers7d"] >= 2
    assert dashboard["openReports"] == 1
    assert isinstance(dashboard["systemWarnings"], list)

    analytics = client.get("/api/v1/admin/analytics/").json()
    assert analytics["searches"] == 2
    assert analytics["zeroResultSearches"] == 1
    assert analytics["facilityViews"] == 1
    assert analytics["directionsRequests"] == 0
    assert analytics["approvalMedianHours"] == pytest.approx(10, abs=0.1)


@pytest.mark.django_db
def test_report_a_problem_end_to_end(admin_api: Any, owned: Facility) -> None:
    url = f"/api/v1/facilities/{owned.pk}/reports/"
    created = APIClient().post(url, {"reason": "NOT_ON_DUTY", "note": "مغلقة"}, format="json")
    assert created.status_code == 201
    assert created.json()["status"] == "OPEN"
    assert APIClient().post(url, {"reason": "NOPE"}, format="json").status_code == 400
    assert (
        APIClient().post(url, {"reason": "OTHER", "note": "x" * 501}, format="json").status_code
        == 400
    )

    reader = admin_api("admin.reports.read")
    listed = reader.get("/api/v1/admin/reports/?status=OPEN").json()["items"]
    assert [item["reason"] for item in listed] == ["NOT_ON_DUTY"]
    report_id = listed[0]["id"]
    assert reader.post(f"/api/v1/admin/reports/{report_id}/resolve/").status_code == 403

    manager = admin_api("admin.reports.read", "admin.reports.manage")
    resolved = manager.post(f"/api/v1/admin/reports/{report_id}/resolve/", {"note": "fixed"})
    assert resolved.status_code == 200
    assert resolved.json()["status"] == "RESOLVED"
    assert resolved.json()["resolvedById"] == str(manager.user.pk)
    assert manager.post(f"/api/v1/admin/reports/{report_id}/dismiss/").status_code == 400
    assert AuditEvent.objects.filter(action="facility_report.resolved").exists()

    second = APIClient().post(url, {"reason": "OTHER"}, format="json").json()["id"]
    dismissed = manager.post(f"/api/v1/admin/reports/{second}/dismiss/")
    assert dismissed.json()["status"] == "DISMISSED"


@pytest.mark.django_db
def test_reports_on_hidden_facilities_are_404_and_throttled(
    facility: Facility, owned: Facility, monkeypatch: Any
) -> None:
    from core.throttles import FacilityReportThrottle

    hidden = Facility.objects.create(
        category=facility.category, province=facility.province, name_ar="مسودة"
    )
    url = f"/api/v1/facilities/{hidden.pk}/reports/"
    assert APIClient().post(url, {"reason": "OTHER"}, format="json").status_code == 404

    cache.clear()
    monkeypatch.setattr(FacilityReportThrottle, "THROTTLE_RATES", {"facility_report": "1/hour"})
    url = f"/api/v1/facilities/{owned.pk}/reports/"
    assert APIClient().post(url, {"reason": "OTHER"}, format="json").status_code == 201
    assert APIClient().post(url, {"reason": "OTHER"}, format="json").status_code == 429


@pytest.mark.django_db
def test_province_cities_admin(admin_api: Any, facility: Facility) -> None:
    city = City.objects.create(province=facility.province, code="c1", name_ar="مدينة")
    reader = admin_api("admin.provinces.read")
    items = reader.get(f"/api/v1/admin/provinces/{facility.province_id}/cities/").json()["items"]
    assert [item["id"] for item in items] == [str(city.pk)]
    manager = admin_api("admin.provinces.manage")
    url = f"/api/v1/admin/provinces/{facility.province_id}/cities/{city.pk}/"
    assert manager.put(url, {"active": False}, format="json").json()["active"] is False
    city.refresh_from_db()
    assert city.active is False


@pytest.mark.django_db
def test_category_group_icon_round_trips(admin_api: Any, facility: Facility) -> None:
    client = admin_api("admin.taxonomy.manage", "admin.taxonomy.read")
    group = facility.category.group
    response = client.put(
        f"/api/v1/admin/category-groups/{group.pk}/",
        {"iconKey": "health", "nameAr": "صحة", "sortOrder": 3},
        format="json",
    )
    assert response.status_code == 200
    assert response.json()["iconKey"] == "health"
    assert response.json()["sortOrder"] == 3


@pytest.mark.django_db
def test_public_trust_fields_and_whatsapp(owned: Facility, user: User) -> None:
    owner = _owner_client(user)
    url = f"/api/v1/owner/facilities/{owned.pk}/"
    assert owner.patch(url, {"whatsapp": "12345"}, format="json").status_code == 400
    saved = owner.patch(url, {"whatsapp": "0933 123 456"}, format="json")
    assert saved.status_code == 200
    assert saved.json()["whatsapp"] == "+963933123456"
    # The edit put an active facility into reverification; make it public again.
    Facility.objects.filter(pk=owned.pk).update(status="ACTIVE", last_verified_at=timezone.now())

    detail = APIClient().get(f"/api/v1/public/facilities/{owned.pk}/").json()
    assert detail["whatsapp"] == "+963933123456"
    assert detail["lastVerifiedAt"] is not None
    assert detail["updatedAt"] is not None
    listing = (
        APIClient()
        .get(
            f"/api/v1/public/facilities/?provinceId={owned.province_id}"
            f"&categoryId={owned.category_id}"
        )
        .json()["items"]
    )
    assert listing[0]["lastVerifiedAt"] is not None
    assert "updatedAt" in listing[0]


@pytest.mark.django_db
def test_owner_insights_counts_only_this_facility(owned: Facility, user: User) -> None:
    now = timezone.now()
    other = Facility.objects.create(category=owned.category, province=owned.province, name_ar="غير")
    for name, facility_id, when in (
        ("facility_view", owned.pk, now),
        ("facility_view", owned.pk, now),
        ("phone_tap", owned.pk, now),
        ("directions_start", owned.pk, now - timedelta(days=40)),
        ("facility_view", other.pk, now),
    ):
        ProductAnalyticsEvent.objects.create(
            name=name, properties={"facilityId": str(facility_id)}, occurred_at=when
        )
    body = _owner_client(user).get(f"/api/v1/owner/facilities/{owned.pk}/insights/").json()
    assert (body["views"], body["calls"], body["directions"]) == (2, 1, 0)
    assert body["windowDays"] == 30

    stranger = User.objects.create_user(phone="+963900000066", password="x" * 12, name="S")
    denied = _owner_client(stranger).get(f"/api/v1/owner/facilities/{owned.pk}/insights/")
    assert denied.status_code == 404


@pytest.mark.django_db
def test_int_010_anonymous_owner_list_is_401() -> None:
    response = APIClient().get("/api/v1/owner/facilities/")
    assert response.status_code == 401
    assert response.json()["code"] == "AUTHENTICATION_REQUIRED"
