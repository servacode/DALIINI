"""Moving a facility to another owner, from the console (DECISION-119).

Nothing anywhere could change who owns a facility. A pharmacy sold to a new owner could only be
closed, and its only owner could never delete their account — two refusals told people to
«transfer the ownership» with nowhere to do it. The console now does it, on the word of both
sides, which the operator checks before pressing the button.

The new owner must already have an account: the platform holds no one's number without their
consent. The previous owners leave the facility, or stay as managers when the operator says so.
Both sides are told, and the change is audited with who owned it before.
"""

from __future__ import annotations

from typing import Any
from uuid import UUID

from django.db import transaction
from django.shortcuts import get_object_or_404
from drf_spectacular.utils import extend_schema
from rest_framework import serializers
from rest_framework.response import Response

from accounts.authentication import AuthenticatedRequest
from accounts.models import User
from accounts.phone import normalize_syrian_phone
from audit.services import record_audit
from core.exceptions import ConflictError, DomainError
from core.openapi import CONFLICT_409, DOMAIN_400, NOT_FOUND_404, protected
from facilities.models import Facility, FacilityMembership
from notifications.models import Notification
from notifications.services import notify

from .views import AdminView, _request_id


class AdminFacilityOwnerRequestSerializer(serializers.Serializer[Any]):
    phone = serializers.CharField(max_length=20, help_text="The new owner's account number.")
    keepPreviousAsManager = serializers.BooleanField(
        default=False, help_text="The previous owners stay, as managers, instead of leaving."
    )


class AdminFacilityOwnerSerializer(serializers.Serializer[Any]):
    facilityId = serializers.UUIDField()
    ownerId = serializers.UUIDField()
    ownerName = serializers.CharField()
    ownerPhone = serializers.CharField()


class AdminFacilityOwnerView(AdminView):
    required_permission = "admin.facilities.manage"

    @extend_schema(
        operation_id="adminFacilityOwnerTransfer",
        tags=["Admin Facilities"],
        summary="Move a facility to another owner",
        request=AdminFacilityOwnerRequestSerializer,
        responses={
            200: AdminFacilityOwnerSerializer,
            400: DOMAIN_400,
            **protected(),
            404: NOT_FOUND_404,
            409: CONFLICT_409,
        },
    )
    def post(self, request: AuthenticatedRequest, facility_id: UUID) -> Response:
        data = AdminFacilityOwnerRequestSerializer(data=request.data)
        data.is_valid(raise_exception=True)
        try:
            phone = normalize_syrian_phone(data.validated_data["phone"])
        except ValueError as exc:
            raise DomainError(
                "INVALID_PHONE", message="الرقم غير صالح. اكتب رقم جوال سوري مثل 0933123456."
            ) from exc
        keep = bool(data.validated_data["keepPreviousAsManager"])

        with transaction.atomic():
            facility = get_object_or_404(Facility.objects.select_for_update(), pk=facility_id)
            if facility.status == Facility.Status.CLOSED:
                raise ConflictError(
                    "FACILITY_CLOSED", message="المنشأة مغلقة نهائياً: لا تُنقل ملكيتها."
                )
            owner = User.objects.filter(phone=phone, is_active=True).first()
            if owner is None:
                raise DomainError(
                    "OWNER_NOT_FOUND",
                    message="لا يوجد حساب فعّال بهذا الرقم. يسجّل صاحبه في التطبيق أولاً.",
                )
            memberships = FacilityMembership.objects.filter(facility=facility)
            previous = list(
                memberships.filter(role=FacilityMembership.Role.OWNER)
                .exclude(user=owner)
                .select_related("user")
            )
            already = memberships.filter(user=owner, role=FacilityMembership.Role.OWNER).exists()
            if already and not previous:
                raise ConflictError("ALREADY_OWNER", message="هذا الحساب هو مالكها بالفعل.")
            for membership in previous:
                if keep:
                    membership.role = FacilityMembership.Role.MANAGER
                    membership.save(update_fields=["role"])
                else:
                    membership.delete()
            FacilityMembership.objects.update_or_create(
                facility=facility, user=owner, defaults={"role": FacilityMembership.Role.OWNER}
            )
            record_audit(
                actor=request.user,
                action="facility.owner.transferred",
                target=facility,
                before_snapshot={"owners": [str(m.user_id) for m in previous]},
                after_snapshot={"owner": str(owner.pk), "previousKeptAsManagers": keep},
                request_id=_request_id(request),
            )

        notify(
            user=owner,
            type="facility.owner.received",
            title_ar="أصبحت مالك منشأة",
            body_ar=f"نُقلت إليك ملكية {facility.name_ar}. تجدها في «منشآتي».",
            destination=Notification.Destination.OWNER_FACILITIES,
            payload={"facilityId": str(facility.pk)},
        )
        for membership in previous:
            notify(
                user=membership.user,
                type="facility.owner.moved",
                title_ar="نُقلت ملكية منشأة",
                body_ar=(
                    f"نُقلت ملكية {facility.name_ar} إلى حساب آخر"
                    + ("، وبقيت مديراً فيها." if keep else ".")
                ),
                destination=Notification.Destination.OWNER_FACILITIES,
                payload={"facilityId": str(facility.pk)},
            )
        return Response(
            {
                "facilityId": str(facility.pk),
                "ownerId": str(owner.pk),
                "ownerName": owner.name,
                "ownerPhone": owner.phone,
            }
        )
