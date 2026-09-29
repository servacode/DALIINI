"""The owner saying "our opening hours are still right", and the weekly reminder to say it.

Confirming is cheap on purpose: one tap, no form. It moves `hours_confirmed_at`, which feeds
the public `infoConfirmedAt` (the later of that and the last operator approval) and keeps
the facility out of the admin STALE list. `last_verified_at` is untouched: it still means an
operator approved the facility's details.
"""

from __future__ import annotations

from datetime import datetime, time, timedelta
from typing import Any

from django.db.models import BooleanField, F, Q, Value
from django.db.models.functions import Coalesce
from django.shortcuts import get_object_or_404
from django.utils import timezone
from drf_spectacular.utils import extend_schema
from rest_framework.permissions import IsAuthenticated
from rest_framework.request import Request
from rest_framework.response import Response
from rest_framework.views import APIView

from audit.services import record_audit
from business_hours.services import DAMASCUS
from core.exceptions import ConflictError
from core.openapi import CONFLICT_409, NOT_FOUND_404, protected
from notifications.models import Notification
from notifications.services import notify

from .models import Facility, FacilityMembership
from .permissions import require_facility_member
from .schemas import OwnerHoursConfirmedSerializer

CONFIRMATION_MAX_AGE_DAYS = 7
REMINDER_TYPE = "facility.hours.confirm_request"
REMINDER_TITLE_AR = "هل أوقات دوامك ما زالت صحيحة؟"


def _supports_hours(facility: Facility) -> bool:
    capabilities = getattr(facility.category, "capabilities", None)
    return capabilities is None or bool(capabilities.supports_hours)


class OwnerFacilityConfirmHoursView(APIView):
    permission_classes = [IsAuthenticated]

    @extend_schema(
        operation_id="ownerFacilityHoursConfirm",
        tags=["Owner"],
        summary="Confirm that the facility's opening hours are still right",
        description=(
            "Any owner or manager may confirm. Sets `hoursConfirmedAt`, which also moves the "
            "public `infoConfirmedAt`; `lastVerifiedAt` keeps meaning an operator approval. "
            "Replacing the hours confirms them too. 409 HOURS_NOT_SUPPORTED when the "
            "category has no opening hours."
        ),
        request=None,
        responses={
            200: OwnerHoursConfirmedSerializer,
            **protected(),
            404: NOT_FOUND_404,
            409: CONFLICT_409,
        },
    )
    def post(self, request: Request, facility_id: str) -> Response:
        facility = get_object_or_404(
            Facility.objects.filter(memberships__user=request.user.pk)
            .select_related("category__capabilities")
            .distinct(),
            pk=facility_id,
        )
        require_facility_member(request.user, facility)
        if not _supports_hours(facility):
            raise ConflictError("HOURS_NOT_SUPPORTED", message="هذا التصنيف لا يدعم أوقات الدوام.")
        before = facility.hours_confirmed_at
        now = timezone.now()
        Facility.objects.filter(pk=facility.pk).update(hours_confirmed_at=now)
        record_audit(
            actor=request.user,
            action="facility.hours.confirmed",
            target=facility,
            before_snapshot={"hoursConfirmedAt": before.isoformat() if before else None},
            after_snapshot={"hoursConfirmedAt": now.isoformat()},
            request_id=getattr(request, "request_id", ""),
        )
        latest = max(value for value in (now, facility.last_verified_at) if value is not None)
        return Response(
            {
                "facilityId": str(facility.pk),
                "hoursConfirmedAt": now.isoformat(),
                "infoConfirmedAt": latest.isoformat(),
            }
        )


def week_start(now: datetime | None = None) -> datetime:
    """Monday 00:00 in Damascus of the current ISO week, as an aware datetime."""
    local = (now or timezone.now()).astimezone(DAMASCUS)
    monday = local.date() - timedelta(days=local.weekday())
    return datetime.combine(monday, time(0, 0), tzinfo=DAMASCUS)


def remind_owners_to_confirm_hours(now: datetime | None = None) -> int:
    """Ask the owners of every ACTIVE facility with stale hours to confirm them.

    Stale means never confirmed, or confirmed more than a week ago. At most one reminder per
    facility per ISO week (Damascus): the facility is claimed with a conditional UPDATE before
    anyone is notified, so a rerun, or two workers at once, send nothing twice. Returns how
    many facilities were reminded.
    """
    now = now or timezone.now()
    this_week = week_start(now)
    stale_before = now - timedelta(days=CONFIRMATION_MAX_AGE_DAYS)
    due = (Q(hours_confirmed_at__isnull=True) | Q(hours_confirmed_at__lt=stale_before)) & (
        Q(hours_reminder_sent_at__isnull=True) | Q(hours_reminder_sent_at__lt=this_week)
    )
    candidates = (
        Facility.objects.filter(status=Facility.Status.ACTIVE)
        .annotate(
            supports_hours=Coalesce(
                F("category__capabilities__supports_hours"),
                Value(True),
                output_field=BooleanField(),
            )
        )
        .filter(due, supports_hours=True, memberships__role=FacilityMembership.Role.OWNER)
        .distinct()
        .values_list("pk", "name_ar")
    )
    reminded = 0
    for facility_id, name_ar in list(candidates):
        claimed = Facility.objects.filter(due, pk=facility_id).update(hours_reminder_sent_at=now)
        if not claimed:
            continue
        reminded += 1
        owners = FacilityMembership.objects.filter(
            facility_id=facility_id, role=FacilityMembership.Role.OWNER, user__is_active=True
        ).select_related("user")
        for membership in owners:
            _remind(membership.user, facility_id, name_ar)
    return reminded


def _remind(user: Any, facility_id: Any, name_ar: str) -> None:
    notify(
        user=user,
        type=REMINDER_TYPE,
        title_ar=REMINDER_TITLE_AR,
        body_ar=f"أكّد أوقات دوام {name_ar} أو عدّلها حتى يبقى الدليل دقيقاً."[:400],
        destination=Notification.Destination.OWNER_FACILITIES,
        payload={"facilityId": str(facility_id)},
    )
