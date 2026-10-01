from __future__ import annotations

from collections import Counter
from typing import TYPE_CHECKING, Any
from uuid import UUID

from django.contrib.gis.geos import Point
from django.core.exceptions import ValidationError
from django.db import transaction
from django.utils import timezone

from audit.services import record_audit
from directory.models import CategoryProvince, ServiceTag, Specialty
from locations.models import City, Neighborhood

from .models import (
    Facility,
    FacilityApplication,
    FacilityMembership,
    FacilityServiceTag,
    FacilitySpecialty,
    VerificationEvidence,
)

if TYPE_CHECKING:
    from accounts.models import User


def _snapshot(facility: Facility) -> dict[str, Any]:
    return {
        "status": facility.status,
        "nameAr": facility.name_ar,
        "categoryId": str(facility.category_id),
        "provinceId": str(facility.province_id),
        "cityId": str(facility.city_id) if facility.city_id else None,
        "neighborhoodId": (
            str(facility.neighborhood_id) if facility.neighborhood_id else None
        ),
        "hasLocation": facility.location is not None,
    }


def application_snapshot(facility: Facility) -> dict[str, Any]:
    """What the operator reviews: the audit snapshot plus the public-facing content."""
    point = facility.location
    return {
        **_snapshot(facility),
        "nameEn": facility.name_en or None,
        "descriptionAr": facility.description_ar or None,
        "descriptionEn": facility.description_en or None,
        "phone": facility.phone or None,
        "whatsapp": facility.whatsapp or None,
        "addressAr": facility.address_ar or None,
        "addressEn": facility.address_en or None,
        "location": {"latitude": point.y, "longitude": point.x} if point else None,
        "specialtyIds": sorted(facility.specialty_links.values_list("specialty_id", flat=True)),
        "serviceTagIds": sorted(
            facility.service_links.values_list("service_tag_id", flat=True)
        ),
        "imageIds": [
            str(value)
            for value in facility.images.order_by("sort_order", "created_at").values_list(
                "id", flat=True
            )
        ],
    }


def _owner_switch(*, province_id: UUID, category_id: UUID) -> CategoryProvince | None:
    return CategoryProvince.objects.select_related(
        "category__group", "category__capabilities", "province"
    ).filter(
        province_id=province_id,
        category_id=category_id,
        province__active=True,
        category__active=True,
        category__group__active=True,
        owner_registration_enabled=True,
        category__capabilities__supports_owner_onboarding=True,
    ).first()


def validate_owner_registration(*, province_id: UUID, category_id: UUID) -> CategoryProvince:
    switch = _owner_switch(province_id=province_id, category_id=category_id)
    if switch is None:
        raise ValidationError("Owner onboarding is not enabled for this selection.")
    return switch


@transaction.atomic
def create_facility_draft(
    *, actor: User, data: dict[str, Any], request_id: str = ""
) -> Facility:
    switch = validate_owner_registration(
        province_id=data["provinceId"], category_id=data["categoryId"]
    )
    facility = Facility.objects.create(
        province=switch.province,
        category=switch.category,
        name_ar=data["nameAr"].strip(),
        name_en=data.get("nameEn", "").strip(),
        status=Facility.Status.DRAFT,
    )
    FacilityMembership.objects.create(
        facility=facility,
        user=actor,
        role=FacilityMembership.Role.OWNER,
    )
    FacilityApplication.objects.create(
        facility=facility,
        kind=FacilityApplication.Kind.INITIAL,
        status=FacilityApplication.Status.DRAFT,
    )
    record_audit(
        actor=actor,
        action="facility.owner_draft.created",
        target=facility,
        after_snapshot=_snapshot(facility),
        request_id=request_id,
    )
    return facility


def _resolve_city(*, facility: Facility, city_id: UUID | None) -> City | None:
    if city_id is None:
        return None
    try:
        return City.objects.get(pk=city_id, province=facility.province, active=True)
    except City.DoesNotExist as exc:
        raise ValidationError({"cityId": "City is not valid for the facility province."}) from exc


def _resolve_neighborhood(
    *, city: City | None, neighborhood_id: UUID | None
) -> Neighborhood | None:
    if neighborhood_id is None:
        return None
    if city is None:
        raise ValidationError({"neighborhoodId": "City is required."})
    try:
        return Neighborhood.objects.get(pk=neighborhood_id, city=city, active=True)
    except Neighborhood.DoesNotExist as exc:
        raise ValidationError(
            {"neighborhoodId": "Neighborhood is not valid for the city."}
        ) from exc


def _replace_specialties(facility: Facility, specialty_ids: list[int] | None) -> None:
    if specialty_ids is None:
        return
    specialties = list(Specialty.objects.filter(pk__in=set(specialty_ids), active=True))
    if len(specialties) != len(set(specialty_ids)):
        raise ValidationError({"specialtyIds": "One or more specialties are invalid."})
    for specialty in specialties:
        if specialty.category_id and specialty.category_id != facility.category_id:
            raise ValidationError({"specialtyIds": "Specialty is outside facility category."})
        if (
            specialty.specialization
            and specialty.specialization != facility.category.specialization
        ):
            raise ValidationError({"specialtyIds": "Specialty is outside facility specialization."})
    FacilitySpecialty.objects.filter(facility=facility).delete()
    FacilitySpecialty.objects.bulk_create(
        [FacilitySpecialty(facility=facility, specialty=item) for item in specialties]
    )


def _replace_service_tags(facility: Facility, tag_ids: list[int] | None) -> None:
    if tag_ids is None:
        return
    tags = list(
        ServiceTag.objects.filter(
            pk__in=set(tag_ids), category=facility.category, active=True
        )
    )
    if len(tags) != len(set(tag_ids)):
        raise ValidationError({"serviceTagIds": "One or more service tags are invalid."})
    FacilityServiceTag.objects.filter(facility=facility).delete()
    FacilityServiceTag.objects.bulk_create(
        [FacilityServiceTag(facility=facility, service_tag=item) for item in tags]
    )


@transaction.atomic
def update_facility_core(
    *, actor: User, facility: Facility, data: dict[str, Any], request_id: str = ""
) -> Facility:
    locked = Facility.objects.select_for_update().select_related("category").get(pk=facility.pk)
    before = _snapshot(locked)
    city_id = data.get("cityId", locked.city_id)
    city = _resolve_city(facility=locked, city_id=city_id)
    neighborhood_id = data.get("neighborhoodId", locked.neighborhood_id)
    if "cityId" in data and city is None and "neighborhoodId" not in data:
        neighborhood_id = None
    neighborhood = _resolve_neighborhood(
        city=city,
        neighborhood_id=neighborhood_id,
    )
    field_map = {
        "nameAr": "name_ar",
        "nameEn": "name_en",
        "descriptionAr": "description_ar",
        "descriptionEn": "description_en",
        "phone": "phone",
        "whatsapp": "whatsapp",
        "addressAr": "address_ar",
        "addressEn": "address_en",
    }
    for input_name, model_name in field_map.items():
        if input_name in data:
            setattr(locked, model_name, data[input_name].strip())
    if "cityId" in data:
        locked.city = city
        if city is None and "neighborhoodId" not in data:
            locked.neighborhood = None
    if "neighborhoodId" in data:
        locked.neighborhood = neighborhood
    if locked.status == Facility.Status.ACTIVE:
        locked.status = Facility.Status.REVERIFICATION_REQUIRED
    locked.full_clean(exclude=["location"])
    locked.save()
    _replace_specialties(locked, data.get("specialtyIds"))
    _replace_service_tags(locked, data.get("serviceTagIds"))
    record_audit(
        actor=actor,
        action="facility.owner_core.updated",
        target=locked,
        before_snapshot=before,
        after_snapshot=_snapshot(locked),
        request_id=request_id,
    )
    return locked


@transaction.atomic
def update_facility_location(
    *,
    actor: User,
    facility: Facility,
    latitude: float,
    longitude: float,
    request_id: str = "",
) -> Facility:
    locked = Facility.objects.select_for_update().get(pk=facility.pk)
    before = _snapshot(locked)
    locked.location = Point(float(longitude), float(latitude), srid=4326)
    if locked.status == Facility.Status.ACTIVE:
        locked.status = Facility.Status.REVERIFICATION_REQUIRED
    locked.save(update_fields=["location", "status", "updated_at"])
    record_audit(
        actor=actor,
        action="facility.owner_location.updated",
        target=locked,
        before_snapshot=before,
        after_snapshot=_snapshot(locked),
        request_id=request_id,
    )
    return locked


def _required_evidence_complete(facility: Facility) -> bool:
    requirements = list(
        facility.category.verification_requirements.filter(active=True, required=True)
    )
    counts = Counter(
        VerificationEvidence.objects.filter(facility=facility).values_list(
            "requirement_id", flat=True
        )
    )
    return all(counts[item.id] >= item.min_files for item in requirements)


@transaction.atomic
def submit_facility(
    *, actor: User, facility: Facility, request_id: str = ""
) -> FacilityApplication:
    locked = (
        # Lock the facility row only. `category__capabilities` is a reverse one-to-one,
        # so select_related emits a LEFT OUTER JOIN and PostgreSQL refuses FOR UPDATE on
        # the nullable side of an outer join. `of=("self",)` keeps the intended lock on
        # facilities_facility while still prefetching the reference rows.
        Facility.objects.select_for_update(of=("self",))
        .select_related("category__group", "category__capabilities", "province")
        .get(pk=facility.pk)
    )
    if locked.status in (Facility.Status.SUSPENDED, Facility.Status.CLOSED):
        # A suspension or closure is an operator decision; resubmitting must not undo it.
        raise ValidationError(
            {"status": "A suspended or closed facility cannot be submitted for review."}
        )
    validate_owner_registration(
        province_id=locked.province_id,
        category_id=locked.category_id,
    )
    if locked.location is None:
        raise ValidationError({"location": "Facility location is required."})
    if not locked.name_ar.strip():
        raise ValidationError({"nameAr": "Facility Arabic name is required."})
    if not _required_evidence_complete(locked):
        raise ValidationError({"evidence": "Current verification evidence is incomplete."})
    existing = FacilityApplication.objects.select_for_update().filter(
        facility=locked,
        status=FacilityApplication.Status.SUBMITTED,
    ).exists()
    if existing:
        raise ValidationError("A submitted application is already pending review.")
    kind = (
        FacilityApplication.Kind.INITIAL
        if locked.activated_at is None
        else FacilityApplication.Kind.REVERIFICATION
    )
    application = (
        FacilityApplication.objects.filter(
            facility=locked,
            kind=kind,
            status__in=[FacilityApplication.Status.DRAFT, FacilityApplication.Status.REJECTED],
        )
        .order_by("-updated_at")
        .first()
    )
    if application is None:
        application = FacilityApplication.objects.create(facility=locked, kind=kind)
    application.status = FacilityApplication.Status.SUBMITTED
    application.submitted_at = timezone.now()
    application.rejection_reason = ""
    application.snapshot = application_snapshot(locked)
    application.save(
        update_fields=[
            "status",
            "submitted_at",
            "rejection_reason",
            "snapshot",
            "updated_at",
        ]
    )
    locked.status = Facility.Status.SUBMITTED
    locked.save(update_fields=["status", "updated_at"])
    record_audit(
        actor=actor,
        action="facility.owner_submitted",
        target=application,
        after_snapshot={"facility": _snapshot(locked)},
        request_id=request_id,
    )
    return application
