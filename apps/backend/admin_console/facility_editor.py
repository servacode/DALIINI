"""Operators create and correct facilities directly.

Until now a facility could only come into the directory through an owner's application, so a
pharmacy whose owner had not registered could not be listed at all, and a wrong phone number
could only be fixed by asking the owner. An operator now adds a facility with no owner (it can
be claimed later) and edits any facility's details.

The rules are the owner's rules: the same fields, the same city and neighbourhood checks, the
same specialty and service validation (`facilities.services`). What differs is what an edit
does to the facility. An owner's change to a live facility sends it back for re-verification;
an operator's change is the verification, so the status stays where it was. Every change is
audited with both snapshots, and the facility's owners are told that the directory changed
their listing.
"""

from __future__ import annotations

from typing import Any
from uuid import UUID

from django.contrib.gis.geos import Point
from django.core.exceptions import ValidationError
from django.db import transaction
from django.utils import timezone
from rest_framework import serializers

from accounts.models import User
from accounts.phone import normalize_syrian_phone
from audit.services import record_audit
from core.openapi import CoordinatesSerializer
from directory.models import Category
from facilities.models import Facility, FacilityMembership
from facilities.services import (
    application_snapshot,
    replace_service_tags,
    replace_specialties,
    resolve_city,
    resolve_neighborhood,
)
from locations.models import Province
from notifications.models import Notification
from notifications.services import notify

TEXT_FIELDS = {
    "nameAr": "name_ar",
    "nameEn": "name_en",
    "descriptionAr": "description_ar",
    "descriptionEn": "description_en",
    "phone": "phone",
    "whatsapp": "whatsapp",
    "addressAr": "address_ar",
    "addressEn": "address_en",
}
CREATE_STATUSES = (Facility.Status.ACTIVE, Facility.Status.DRAFT)


class AdminFacilityWriteSerializer(serializers.Serializer[Any]):
    """Every detail an operator may set. On create, `categoryId`, `provinceId` and `nameAr`
    are required; on update every field is optional and only the ones sent change."""

    categoryId = serializers.UUIDField(required=False)
    provinceId = serializers.UUIDField(required=False)
    cityId = serializers.UUIDField(required=False, allow_null=True)
    neighborhoodId = serializers.UUIDField(required=False, allow_null=True)
    nameAr = serializers.CharField(max_length=160, required=False)
    nameEn = serializers.CharField(max_length=160, required=False, allow_blank=True)
    descriptionAr = serializers.CharField(required=False, allow_blank=True)
    descriptionEn = serializers.CharField(required=False, allow_blank=True)
    phone = serializers.CharField(max_length=16, required=False, allow_blank=True)
    whatsapp = serializers.CharField(
        max_length=20,
        required=False,
        allow_blank=True,
        help_text="Optional Syrian mobile (09XXXXXXXX or +9639XXXXXXXX); blank clears it.",
    )
    addressAr = serializers.CharField(max_length=255, required=False, allow_blank=True)
    addressEn = serializers.CharField(max_length=255, required=False, allow_blank=True)
    location = CoordinatesSerializer(
        required=False, allow_null=True, help_text="Null removes the pin."
    )
    specialtyIds = serializers.ListField(
        child=serializers.IntegerField(min_value=1),
        required=False,
        help_text="Replaces the facility's specialties; an empty list clears them.",
    )
    serviceTagIds = serializers.ListField(
        child=serializers.IntegerField(min_value=1),
        required=False,
        help_text="Replaces the facility's services; an empty list clears them.",
    )

    def validate_nameAr(self, value: str) -> str:  # noqa: N802 - the wire name
        if not value.strip():
            raise serializers.ValidationError("The Arabic name is required.")
        return value

    def validate_whatsapp(self, value: str) -> str:
        if not value.strip():
            return ""
        try:
            return normalize_syrian_phone(value)
        except ValueError as exc:
            raise serializers.ValidationError("Enter a valid Syrian mobile number.") from exc


class AdminFacilityCreateSerializer(AdminFacilityWriteSerializer):
    categoryId = serializers.UUIDField()
    provinceId = serializers.UUIDField()
    nameAr = serializers.CharField(max_length=160)
    status = serializers.ChoiceField(
        choices=[(value, value) for value in CREATE_STATUSES],
        required=False,
        help_text="ACTIVE (the default) publishes it at once; DRAFT keeps it hidden.",
    )


def _category(category_id: UUID) -> Category:
    try:
        return Category.objects.select_related("capabilities").get(pk=category_id, active=True)
    except Category.DoesNotExist as exc:
        raise ValidationError({"categoryId": "Not an active category."}) from exc


def _province(province_id: UUID) -> Province:
    try:
        return Province.objects.get(pk=province_id, active=True)
    except Province.DoesNotExist as exc:
        raise ValidationError({"provinceId": "Not an active province."}) from exc


def _apply(facility: Facility, data: dict[str, Any], *, created: bool) -> None:
    """Set everything in `data` except the specialty and service links, which need a row."""
    if "provinceId" in data and data["provinceId"] != facility.province_id:
        facility.province = _province(data["provinceId"])
        if "cityId" not in data:
            # A city belongs to one province; the old one cannot follow the facility.
            facility.city = None
            facility.neighborhood = None
    if "categoryId" in data and (created or data["categoryId"] != facility.category_id):
        facility.category = _category(data["categoryId"])
    for wire, column in TEXT_FIELDS.items():
        if wire in data:
            setattr(facility, column, data[wire].strip())
    if "cityId" in data:
        facility.city = resolve_city(facility=facility, city_id=data["cityId"])
        if "neighborhoodId" not in data:
            facility.neighborhood = None
    if "neighborhoodId" in data:
        city = facility.city if facility.city_id else None
        facility.neighborhood = resolve_neighborhood(
            city=city, neighborhood_id=data["neighborhoodId"]
        )
    if "location" in data:
        point = data["location"]
        facility.location = (
            Point(float(point["longitude"]), float(point["latitude"]), srid=4326)
            if point
            else None
        )


def _links(facility: Facility, data: dict[str, Any], *, category_changed: bool) -> None:
    specialties = data.get("specialtyIds")
    services = data.get("serviceTagIds")
    if category_changed:
        # The old category's specialties and services mean nothing in the new one.
        specialties = [] if specialties is None else specialties
        services = [] if services is None else services
    replace_specialties(facility, specialties)
    replace_service_tags(facility, services)


@transaction.atomic
def create_facility(*, actor: User, data: dict[str, Any], request_id: str = "") -> Facility:
    status = data.get("status") or Facility.Status.ACTIVE
    facility = Facility(
        category=_category(data["categoryId"]),
        province=_province(data["provinceId"]),
        status=status,
    )
    _apply(facility, {k: v for k, v in data.items() if k not in {"categoryId", "provinceId"}},
           created=True)
    if status == Facility.Status.ACTIVE:
        # Listed by the directory itself: published and verified in the same act.
        facility.activated_at = facility.last_verified_at = timezone.now()
    facility.full_clean()
    facility.save()
    _links(facility, data, category_changed=False)
    record_audit(
        actor=actor,
        action="facility.admin.created",
        target=facility,
        after_snapshot=application_snapshot(facility),
        request_id=request_id,
    )
    return facility


@transaction.atomic
def update_facility(
    *, actor: User, facility_id: UUID, data: dict[str, Any], request_id: str = ""
) -> Facility:
    facility = Facility.objects.select_for_update().select_related("category").get(
        pk=facility_id
    )
    before = application_snapshot(facility)
    category_before = facility.category_id
    _apply(facility, data, created=False)
    facility.full_clean()
    facility.save()
    _links(facility, data, category_changed=facility.category_id != category_before)
    after = application_snapshot(facility)
    changed = sorted(key for key in after if after[key] != before.get(key))
    if changed:
        record_audit(
            actor=actor,
            action="facility.admin.updated",
            target=facility,
            before_snapshot=before,
            after_snapshot=after,
            metadata={"fields": changed},
            request_id=request_id,
        )
        _tell_owners(facility)
    return facility


def _tell_owners(facility: Facility) -> None:
    owners = FacilityMembership.objects.filter(
        facility=facility, role=FacilityMembership.Role.OWNER, user__is_active=True
    ).select_related("user")
    for membership in owners:
        notify(
            user=membership.user,
            type="facility.admin.updated",
            title_ar="تحديث من إدارة الدليل",
            body_ar=f"عدّلت إدارة الدليل بيانات {facility.name_ar}. راجعها من صفحة منشأتك."[:400],
            destination=Notification.Destination.OWNER_FACILITIES,
            payload={"facilityId": str(facility.pk)},
        )
