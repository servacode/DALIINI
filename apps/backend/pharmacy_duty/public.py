"""The public duty roster by date: which pharmacies are on duty on a given day.

Same visibility as the public duty-now listing (`search.selectors.public_facilities`: an
ACTIVE facility, in an active province and category, publicly enabled there) narrowed to
categories that support duty. A facility is on a day's roster when one of its shifts
overlaps that Damascus calendar day, so a night shift appears on both days it touches, as
it does in the `dutyToday` filter. Rows are the public list's compact facility.
"""

from __future__ import annotations

import uuid
from typing import Any

from django.db.models import Exists, OuterRef
from django.utils.dateparse import parse_date
from drf_spectacular.utils import OpenApiParameter, extend_schema
from rest_framework import serializers
from rest_framework.exceptions import ValidationError
from rest_framework.request import Request
from rest_framework.response import Response
from rest_framework.views import APIView

from business_hours.query import with_availability_flags
from core.openapi import VALIDATION_400
from core.throttles import SearchThrottle, WebServerThrottle
from search.schemas import CompactFacilitySerializer
from search.selectors import public_facilities, with_rating_summary
from search.serializers import compact_facility

from .coverage import day_bounds, local_today, window
from .models import DutyShift

MAX_DAYS = 7
DAY_LIMIT = 200
CACHE_CONTROL = "public, max-age=60"


class PublicDutyShiftSerializer(serializers.Serializer[Any]):
    facilityId = serializers.UUIDField()
    startsAt = serializers.DateTimeField()
    endsAt = serializers.DateTimeField()


class PublicDutyDaySerializer(serializers.Serializer[Any]):
    date = serializers.DateField(help_text="A Damascus calendar day.")
    items = CompactFacilitySerializer(
        many=True, help_text=f"Pharmacies on duty that day, by Arabic name, at most {DAY_LIMIT}."
    )
    shifts = PublicDutyShiftSerializer(
        many=True, help_text="Their shifts overlapping that day, by start time."
    )


class PublicDutyRosterSerializer(serializers.Serializer[Any]):
    provinceId = serializers.UUIDField()
    days = PublicDutyDaySerializer(many=True)


def _param(request: Request, name: str) -> str:
    return str(request.query_params.get(name) or "").strip()


class PublicDutyByDateView(APIView):
    # Anonymous by design, so the answer is the same for everyone and can be cached.
    authentication_classes: list[Any] = []
    permission_classes: list[Any] = []
    throttle_classes = [SearchThrottle, WebServerThrottle]

    @extend_schema(
        operation_id="publicDutyByDateList",
        tags=["Public Discovery"],
        summary="Pharmacies on duty on a given day (or up to 7 days)",
        description=(
            "Days are Damascus calendar days starting at `date` (default today). A pharmacy "
            "is listed on every day one of its duty shifts overlaps. Same visibility as the "
            "public duty-now listing. Cacheable for one minute."
        ),
        parameters=[
            OpenApiParameter("provinceId", str, OpenApiParameter.QUERY, required=True),
            OpenApiParameter(
                "date", str, OpenApiParameter.QUERY, required=False, description="YYYY-MM-DD"
            ),
            OpenApiParameter(
                "days", int, OpenApiParameter.QUERY, required=False, description="1 to 7."
            ),
            OpenApiParameter("categoryId", str, OpenApiParameter.QUERY, required=False),
            OpenApiParameter("cityId", str, OpenApiParameter.QUERY, required=False),
        ],
        responses={200: PublicDutyRosterSerializer, 400: VALIDATION_400},
    )
    def get(self, request: Request) -> Response:
        filters: dict[str, Any] = {}
        for name, column in (
            ("provinceId", "province_id"),
            ("categoryId", "category_id"),
            ("cityId", "city_id"),
        ):
            value = _param(request, name)
            if not value:
                if name == "provinceId":
                    raise ValidationError({name: ["Required."]})
                continue
            try:
                filters[column] = uuid.UUID(value)
            except ValueError as exc:
                raise ValidationError({name: ["Must be a valid UUID."]}) from exc
        raw_date = _param(request, "date")
        start = parse_date(raw_date) if raw_date else local_today()
        if start is None:
            raise ValidationError({"date": ["Expected YYYY-MM-DD."]})
        raw_days = _param(request, "days") or "1"
        if not raw_days.isdigit() or not 1 <= int(raw_days) <= MAX_DAYS:
            raise ValidationError({"days": [f"Between 1 and {MAX_DAYS}."]})
        visible = public_facilities()
        base = with_availability_flags(
            with_rating_summary(
                visible.filter(category__capabilities__supports_duty=True, **filters)
            )
        )
        days = []
        for day in window(start, int(raw_days)):
            day_start, day_end = day_bounds(day)
            overlapping = DutyShift.objects.filter(starts_at__lt=day_end, ends_at__gt=day_start)
            rows = list(
                base.filter(Exists(overlapping.filter(facility_id=OuterRef("pk")))).order_by(
                    "name_ar", "id"
                )[:DAY_LIMIT]
            )
            shifts = overlapping.filter(facility_id__in=[row.pk for row in rows]).order_by(
                "starts_at", "id"
            )
            days.append(
                {
                    "date": day.isoformat(),
                    "items": [compact_facility(row) for row in rows],
                    "shifts": [
                        {
                            "facilityId": str(shift.facility_id),
                            "startsAt": shift.starts_at.isoformat(),
                            "endsAt": shift.ends_at.isoformat(),
                        }
                        for shift in shifts
                    ],
                }
            )
        response = Response({"provinceId": str(filters["province_id"]), "days": days})
        response["Cache-Control"] = CACHE_CONTROL
        return response
