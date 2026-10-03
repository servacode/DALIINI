"""What needs an operator's attention: the tasks queue, smart alerts and launch readiness.

Everything here is read-only and computed on request from bounded queries: counts, a few
oldest rows and small aggregates. Nothing is cached, so a decision taken a second ago is
reflected on the next refresh.
"""

from __future__ import annotations

import uuid
from datetime import datetime, timedelta
from typing import Any

from django.contrib.postgres.aggregates import ArrayAgg
from django.db import connection
from django.db.models import Case, Count, F, IntegerField, Min, Value, When
from django.db.models.fields.json import KT
from django.db.models.functions import Greatest
from django.utils import timezone

from analytics.models import ProductAnalyticsEvent
from content_services.models import EmergencyNumber
from directory.models import Category, CategoryProvince
from facilities.models import Facility, FacilityApplication, FacilityReport
from locations.models import Province
from pharmacy_duty.coverage import (
    DEFAULT_WINDOW_DAYS,
    duty_gaps,
    duty_provinces,
    local_today,
    window,
)
from platform_settings.maintenance import get_maintenance_state
from platform_settings.operations import (
    READINESS_MIN_FACILITIES_KEY,
    REVIEW_SLA_HOURS_KEY,
    get_int_setting,
)

from .quality import STALE_AFTER_DAYS

TASK_LIMIT = 10
REPORTED_FACILITY_THRESHOLD = 3
REPORTED_FACILITY_CRITICAL = 5
DUTY_GAP_CRITICAL_DAYS = 2
ZERO_RESULT_WINDOW_DAYS = 7
ZERO_RESULT_MIN_COUNT = 3
ZERO_RESULT_LIMIT = 5

SEVERITY_ORDER = {"critical": 0, "warning": 1, "info": 2}
ALERT_KINDS = [
    ("DUTY_GAP", "Duty gap"),
    ("STALE_FACILITY", "Stale facilities"),
    ("REPORTED_FACILITY", "Repeatedly reported facility"),
    ("ZERO_RESULT_SEARCH", "Searches with no results"),
    ("REVIEW_OVERDUE", "Reviews past the SLA"),
    ("MAINTENANCE_ON", "Maintenance mode is on"),
]
ALERT_SEVERITIES = [("info", "Info"), ("warning", "Warning"), ("critical", "Critical")]
LINK_ENTITY_TYPES = [
    ("FACILITY", "Facility"),
    ("PROVINCE", "Province"),
    ("APPLICATION", "Application"),
    ("FACILITY_LIST", "Facility list"),
    ("APPLICATION_LIST", "Application list"),
    ("REPORT_LIST", "Report list"),
    ("DUTY_ROSTER", "Duty roster"),
    ("SETTINGS", "Settings"),
]
READINESS_CODES = [
    ("PROVINCE_ACTIVE", "Province is active"),
    ("CATEGORY_PUBLIC", "At least one category is public"),
    ("MIN_ACTIVE_FACILITIES", "Enough active facilities"),
    ("DUTY_COVERAGE", "Duty roster covered for 14 days"),
    ("EMERGENCY_NUMBERS", "Emergency numbers present"),
]


def sla_hours() -> int:
    return get_int_setting(REVIEW_SLA_HOURS_KEY)


def _age_hours(since: datetime | None, now: datetime) -> float:
    if since is None:
        return 0.0
    return round(max(0.0, (now - since).total_seconds()) / 3600.0, 1)


def _iso(value: Any) -> Any:
    return value.isoformat() if value else None


# --------------------------------------------------------------------------------------
# Tasks center
# --------------------------------------------------------------------------------------


def _application_bucket(kind: str, now: datetime, sla: timedelta) -> dict[str, Any]:
    queue = FacilityApplication.objects.filter(
        status=FacilityApplication.Status.SUBMITTED, kind=kind
    )
    oldest = queue.select_related("facility__province").order_by(
        F("submitted_at").asc(nulls_first=True), "id"
    )[:TASK_LIMIT]
    items = []
    for application in oldest:
        since = application.submitted_at or application.created_at
        items.append(
            {
                "id": str(application.pk),
                "facilityId": str(application.facility_id),
                "facilityNameAr": application.facility.name_ar,
                "provinceNameAr": application.facility.province.name_ar,
                "submittedAt": _iso(since),
                "ageHours": _age_hours(since, now),
                "overdue": since < now - sla,
            }
        )
    return {
        "count": queue.count(),
        "overdueCount": queue.filter(submitted_at__lt=now - sla).count(),
        "oldest": items,
    }


def _report_bucket(now: datetime, sla: timedelta) -> dict[str, Any]:
    open_reports = FacilityReport.objects.filter(status=FacilityReport.Status.OPEN)
    groups: list[dict[str, Any]] = list(
        open_reports.values("facility_id")
        .annotate(
            open_count=Count("id"),
            oldest_at=Min("created_at"),
            reasons=ArrayAgg("reason", distinct=True),
        )
        .annotate(
            repeated=Case(
                When(open_count__gte=2, then=Value(0)),
                default=Value(1),
                output_field=IntegerField(),
            ),
        )
        .order_by("repeated", "oldest_at", "facility_id")[:TASK_LIMIT]
    )
    names = dict(
        Facility.objects.filter(pk__in=[row["facility_id"] for row in groups]).values_list(
            "pk", "name_ar"
        )
    )
    items = [
        {
            "facilityId": str(row["facility_id"]),
            "facilityNameAr": names.get(row["facility_id"], ""),
            "openCount": row["open_count"],
            "oldestAt": _iso(row["oldest_at"]),
            "ageHours": _age_hours(row["oldest_at"], now),
            "overdue": row["oldest_at"] < now - sla,
            "reasons": sorted(row["reasons"]),
        }
        for row in groups
    ]
    return {
        "count": open_reports.count(),
        "facilityCount": open_reports.values("facility_id").distinct().count(),
        "overdueCount": open_reports.filter(created_at__lt=now - sla).count(),
        "oldest": items,
    }


def _reverification_bucket(now: datetime, sla: timedelta) -> dict[str, Any]:
    # A facility enters REVERIFICATION_REQUIRED by a save that bumps `updated_at`, and stays
    # there without further edits until its owner resubmits, so `updated_at` dates the wait.
    waiting = Facility.objects.filter(status=Facility.Status.REVERIFICATION_REQUIRED)
    oldest = waiting.select_related("province").order_by("updated_at", "id")[:TASK_LIMIT]
    return {
        "count": waiting.count(),
        "overdueCount": waiting.filter(updated_at__lt=now - sla).count(),
        "oldest": [
            {
                "facilityId": str(facility.pk),
                "facilityNameAr": facility.name_ar,
                "provinceNameAr": facility.province.name_ar,
                "since": _iso(facility.updated_at),
                "ageHours": _age_hours(facility.updated_at, now),
                "overdue": facility.updated_at < now - sla,
            }
            for facility in oldest
        ],
    }


def tasks_center(now: datetime | None = None) -> dict[str, Any]:
    now = now or timezone.now()
    hours = sla_hours()
    sla = timedelta(hours=hours)
    return {
        "slaHours": hours,
        "generatedAt": now.isoformat(),
        "applications": {
            "initial": _application_bucket(FacilityApplication.Kind.INITIAL, now, sla),
            "reverification": _application_bucket(
                FacilityApplication.Kind.REVERIFICATION, now, sla
            ),
            # Edits to live facilities: the facility stays published while these wait.
            "change": _application_bucket(FacilityApplication.Kind.CHANGE, now, sla),
            # Somebody asking to own a facility nobody owns.
            "claim": _application_bucket(FacilityApplication.Kind.CLAIM, now, sla),
        },
        "reports": _report_bucket(now, sla),
        "reverificationRequired": _reverification_bucket(now, sla),
    }


# --------------------------------------------------------------------------------------
# Smart alerts
# --------------------------------------------------------------------------------------


def _alert(
    kind: str,
    severity: str,
    title_ar: str,
    detail_ar: str,
    count: int,
    entity_type: str | None = None,
    entity_id: str | None = None,
    query: str | None = None,
) -> dict[str, Any]:
    return {
        "kind": kind,
        "severity": severity,
        "titleAr": title_ar,
        "detailAr": detail_ar,
        "count": count,
        "link": (
            {"entityType": entity_type, "entityId": entity_id, "query": query}
            if entity_type
            else None
        ),
    }


def _duty_gap_alerts(now: datetime) -> list[dict[str, Any]]:
    today = local_today(now)
    provinces = {str(pk): name for pk, name in duty_provinces().values_list("pk", "name_ar")}
    gaps = duty_gaps(window(today, DEFAULT_WINDOW_DAYS), province_ids=provinces)
    alerts = []
    for province_id, days in gaps.items():
        if not days:
            continue
        soon = (days[0] - today).days < DUTY_GAP_CRITICAL_DAYS
        listed = "، ".join(f"{day.day}/{day.month}" for day in days[:7])
        more = f" و{len(days) - 7} أيام أخرى" if len(days) > 7 else ""
        alerts.append(
            _alert(
                "DUTY_GAP",
                "critical" if soon else "warning",
                f"أيام بلا صيدلية مناوبة في {provinces[province_id]}",
                f"لا توجد وردية مناوبة لأي صيدلية فعّالة في: {listed}{more}.",
                len(days),
                "DUTY_ROSTER",
                province_id,
                f"provinceId={province_id}",
            )
        )
    return alerts


def _stale_alert(now: datetime) -> list[dict[str, Any]]:
    stale = (
        Facility.objects.filter(status=Facility.Status.ACTIVE)
        .annotate(fresh_at=Greatest("updated_at", "hours_confirmed_at", "last_verified_at"))
        .filter(fresh_at__lt=now - timedelta(days=STALE_AFTER_DAYS))
        .count()
    )
    if not stale:
        return []
    return [
        _alert(
            "STALE_FACILITY",
            "warning",
            "منشآت لم تُحدَّث منذ مدة",
            f"{stale} منشأة فعّالة لم تتغير بياناتها ولم يؤكد أصحابها أوقات الدوام منذ "
            f"{STALE_AFTER_DAYS} يوماً.",
            stale,
            "FACILITY_LIST",
            None,
            "status=ACTIVE&issue=STALE",
        )
    ]


def _reported_alerts() -> list[dict[str, Any]]:
    rows = (
        FacilityReport.objects.filter(status=FacilityReport.Status.OPEN)
        .values("facility_id", "facility__name_ar")
        .annotate(open_count=Count("id"))
        .filter(open_count__gte=REPORTED_FACILITY_THRESHOLD)
        .order_by("-open_count", "facility_id")[:TASK_LIMIT]
    )
    return [
        _alert(
            "REPORTED_FACILITY",
            "critical" if row["open_count"] >= REPORTED_FACILITY_CRITICAL else "warning",
            f"بلاغات متكررة عن {row['facility__name_ar']}",
            f"{row['open_count']} بلاغات مفتوحة عن هذه المنشأة.",
            row["open_count"],
            "FACILITY",
            str(row["facility_id"]),
            f"facility={row['facility_id']}&status=OPEN",
        )
        for row in rows
    ]


def _uuid_or_none(value: Any) -> str | None:
    try:
        return str(uuid.UUID(str(value)))
    except (TypeError, ValueError, AttributeError):
        return None


def _zero_result_alerts(now: datetime) -> list[dict[str, Any]]:
    """Where searches find nothing, by province and category.

    The analytics registry deliberately records no search text (only `queryLength`, the
    province and the category), so the terms themselves cannot be reported. Grouping by the
    context shows where the directory is thin, which is the actionable part.
    """
    rows = list(
        ProductAnalyticsEvent.objects.filter(
            name="search_zero_results",
            occurred_at__gte=now - timedelta(days=ZERO_RESULT_WINDOW_DAYS),
        )
        .values(province=KT("properties__provinceId"), category=KT("properties__categoryId"))
        .annotate(total=Count("id"))
        .filter(total__gte=ZERO_RESULT_MIN_COUNT)
        .order_by("-total", "province", "category")[:ZERO_RESULT_LIMIT]
    )
    province_ids = {_uuid_or_none(row["province"]) for row in rows} - {None}
    category_ids = {_uuid_or_none(row["category"]) for row in rows} - {None}
    provinces = {
        str(pk): name
        for pk, name in Province.objects.filter(pk__in=province_ids).values_list("pk", "name_ar")
    }
    categories = {
        str(pk): name
        for pk, name in Category.objects.filter(pk__in=category_ids).values_list("pk", "name_ar")
    }
    alerts = []
    for row in rows:
        province_id = _uuid_or_none(row["province"])
        category_id = _uuid_or_none(row["category"])
        where = provinces.get(province_id or "", "محافظة غير محددة")
        what = categories.get(category_id or "", "كل التصنيفات")
        alerts.append(
            _alert(
                "ZERO_RESULT_SEARCH",
                "info",
                f"بحث بلا نتائج: {what} في {where}",
                f"{row['total']} عملية بحث لم تجد أي نتيجة خلال آخر {ZERO_RESULT_WINDOW_DAYS} "
                "أيام. نص البحث لا يُسجَّل، لذا التجميع حسب المحافظة والتصنيف.",
                row["total"],
                "PROVINCE" if province_id else None,
                province_id,
                f"categoryId={category_id}" if category_id else None,
            )
        )
    return alerts


def _review_overdue_alert(now: datetime) -> list[dict[str, Any]]:
    hours = sla_hours()
    queue = FacilityApplication.objects.filter(status=FacilityApplication.Status.SUBMITTED)
    overdue = queue.filter(submitted_at__lt=now - timedelta(hours=hours)).count()
    if not overdue:
        return []
    doubled = queue.filter(submitted_at__lt=now - timedelta(hours=2 * hours)).exists()
    return [
        _alert(
            "REVIEW_OVERDUE",
            "critical" if doubled else "warning",
            "طلبات تجاوزت مهلة المراجعة",
            f"{overdue} طلب بانتظار المراجعة منذ أكثر من {hours} ساعة.",
            overdue,
            "APPLICATION_LIST",
            None,
            "status=SUBMITTED",
        )
    ]


def _maintenance_alert() -> list[dict[str, Any]]:
    if not get_maintenance_state().enabled:
        return []
    return [
        _alert(
            "MAINTENANCE_ON",
            "warning",
            "وضع الصيانة مفعّل",
            "الواجهات العامة وتطبيقات المستخدمين ترد بـ 503 حتى يُطفأ وضع الصيانة.",
            1,
            "SETTINGS",
            None,
            "key=maintenance.enabled",
        )
    ]


def smart_alerts(now: datetime | None = None) -> list[dict[str, Any]]:
    now = now or timezone.now()
    alerts = [
        *_maintenance_alert(),
        *_duty_gap_alerts(now),
        *_review_overdue_alert(now),
        *_reported_alerts(),
        *_stale_alert(now),
        *_zero_result_alerts(now),
    ]
    kinds = [code for code, _ in ALERT_KINDS]
    alerts.sort(key=lambda item: (SEVERITY_ORDER[item["severity"]], kinds.index(item["kind"])))
    return alerts


# --------------------------------------------------------------------------------------
# Launch readiness
# --------------------------------------------------------------------------------------


def province_readiness(province: Province, now: datetime | None = None) -> dict[str, Any]:
    now = now or timezone.now()
    minimum = get_int_setting(READINESS_MIN_FACILITIES_KEY)
    public_categories = CategoryProvince.objects.filter(
        province=province, public_enabled=True, category__active=True
    ).count()
    active = Facility.objects.filter(province=province, status=Facility.Status.ACTIVE).count()
    offers_duty = duty_provinces().filter(pk=province.pk).exists()
    days = window(local_today(now), DEFAULT_WINDOW_DAYS)
    gaps = duty_gaps(days, province_ids=[province.pk]).get(str(province.pk), [])
    if not offers_duty:
        duty_ok, duty_detail = True, "لا يوجد تصنيف مناوبة متاح للعامة في هذه المحافظة؛ لا ينطبق."
    elif gaps:
        duty_ok = False
        duty_detail = f"{len(gaps)} يوماً بلا مناوبة خلال الأيام الـ{DEFAULT_WINDOW_DAYS} القادمة."
    else:
        duty_ok, duty_detail = True, f"كل الأيام الـ{DEFAULT_WINDOW_DAYS} القادمة مغطاة بمناوبة."
    numbers = (
        EmergencyNumber.objects.filter(active=True).filter(province__isnull=True).count()
        + EmergencyNumber.objects.filter(active=True, province=province).count()
    )
    items = [
        {
            "code": "PROVINCE_ACTIVE",
            "ok": province.active,
            "detailAr": "المحافظة مفعّلة." if province.active else "المحافظة غير مفعّلة بعد.",
        },
        {
            "code": "CATEGORY_PUBLIC",
            "ok": public_categories >= 1,
            "detailAr": f"{public_categories} تصنيف متاح للعامة.",
        },
        {
            "code": "MIN_ACTIVE_FACILITIES",
            "ok": active >= minimum,
            "detailAr": f"{active} منشأة فعّالة، والمطلوب {minimum} على الأقل.",
        },
        {"code": "DUTY_COVERAGE", "ok": duty_ok, "detailAr": duty_detail},
        {
            "code": "EMERGENCY_NUMBERS",
            "ok": numbers >= 1,
            "detailAr": f"{numbers} رقم طوارئ فعّال (وطني أو خاص بالمحافظة).",
        },
    ]
    return {
        "provinceId": str(province.pk),
        "provinceNameAr": province.name_ar,
        "ready": all(item["ok"] for item in items),
        "minActiveFacilities": minimum,
        "items": items,
    }


# --------------------------------------------------------------------------------------
# Staff performance
# --------------------------------------------------------------------------------------


def staff_performance(start: datetime, end: datetime) -> list[dict[str, Any]]:
    """Per reviewer: decisions on applications in [start, end), and report decisions."""
    with connection.cursor() as cursor:
        cursor.execute(
            "SELECT reviewed_by_id, COUNT(*), "
            "COUNT(*) FILTER (WHERE status = %s), COUNT(*) FILTER (WHERE status = %s), "
            "percentile_cont(0.5) WITHIN GROUP (ORDER BY "
            "EXTRACT(EPOCH FROM (reviewed_at - submitted_at)) / 3600.0) "
            "FILTER (WHERE submitted_at IS NOT NULL) "
            "FROM facilities_facilityapplication "
            "WHERE reviewed_by_id IS NOT NULL AND reviewed_at >= %s AND reviewed_at < %s "
            "AND status IN (%s, %s) GROUP BY reviewed_by_id",
            [
                FacilityApplication.Status.APPROVED,
                FacilityApplication.Status.REJECTED,
                start,
                end,
                FacilityApplication.Status.APPROVED,
                FacilityApplication.Status.REJECTED,
            ],
        )
        decided = {str(row[0]): row[1:] for row in cursor.fetchall()}
    reports = {
        str(row["resolved_by_id"]): row["total"]
        for row in FacilityReport.objects.filter(
            resolved_by__isnull=False, resolved_at__gte=start, resolved_at__lt=end
        )
        .values("resolved_by_id")
        .annotate(total=Count("id"))
    }
    from accounts.models import User

    ids = set(decided) | set(reports)
    names = {
        str(pk): name for pk, name in User.objects.filter(pk__in=ids).values_list("pk", "name")
    }
    items = []
    for user_id in ids:
        total, approvals, rejections, median = decided.get(user_id, (0, 0, 0, None))
        items.append(
            {
                "userId": user_id,
                "name": names.get(user_id, ""),
                "decisions": total,
                "approvals": approvals,
                "rejections": rejections,
                "medianDecisionHours": round(float(median), 2) if median is not None else None,
                "reportDecisions": reports.get(user_id, 0),
            }
        )
    items.sort(key=lambda item: (-item["decisions"], -item["reportDecisions"], item["name"]))
    return items
