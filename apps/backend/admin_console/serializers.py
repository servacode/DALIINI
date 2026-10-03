from typing import Any

from django.db.models import Prefetch, QuerySet

from facilities.models import Facility, FacilityApplication, FacilityMembership

from .review import evidence_complete


def _iso(value: Any) -> Any:
    return value.isoformat() if value else None


def user_payload(user: Any) -> Any:
    return {
        "id": str(user.id),
        "name": user.name,
        "phone": user.phone,
        "active": user.is_active,
        "provinceId": str(user.province_id) if user.province_id else None,
        "createdAt": _iso(user.created_at),
        "updatedAt": _iso(user.updated_at),
    }


def _owner_prefetch(prefix: str = "") -> Any:
    return Prefetch(
        f"{prefix}memberships",
        queryset=FacilityMembership.objects.filter(role=FacilityMembership.Role.OWNER)
        .select_related("user")
        .order_by("created_at"),
        to_attr="owner_links",
    )


def with_facility_names(queryset: QuerySet[Facility]) -> QuerySet[Facility]:
    """Everything `facility_payload` reads, fetched in a fixed number of queries."""
    return queryset.select_related("category", "province").prefetch_related(_owner_prefetch())


def with_application_names(
    queryset: QuerySet[FacilityApplication],
) -> QuerySet[FacilityApplication]:
    return queryset.select_related("facility__category", "facility__province").prefetch_related(
        _owner_prefetch("facility__")
    )


def _owner(facility: Any) -> Any:
    links = getattr(facility, "owner_links", None)
    if links is None:
        links = list(
            facility.memberships.filter(role=FacilityMembership.Role.OWNER)
            .select_related("user")
            .order_by("created_at")[:1]
        )
    return links[0].user if links else None


def _names(facility: Any) -> Any:
    owner = _owner(facility)
    return {
        "categoryNameAr": facility.category.name_ar,
        "provinceNameAr": facility.province.name_ar,
        "ownerName": owner.name if owner else None,
        "ownerPhone": owner.phone if owner else None,
    }


def location_payload(facility: Any) -> Any:
    if not facility.location:
        return None
    return {"latitude": facility.location.y, "longitude": facility.location.x}


def facility_payload(facility: Any) -> Any:
    return {
        "id": str(facility.id),
        "nameAr": facility.name_ar,
        "nameEn": facility.name_en or None,
        "categoryId": str(facility.category_id),
        "provinceId": str(facility.province_id),
        "cityId": str(facility.city_id) if facility.city_id else None,
        "status": facility.status,
        "location": location_payload(facility),
        "updatedAt": _iso(facility.updated_at),
        **_names(facility),
    }


def facility_detail_payload(facility: Any) -> Any:
    """One facility as the console reads and edits it: the row, its quality, every detail."""
    from .quality import quality_payload

    return {
        **facility_payload(facility),
        **quality_payload(facility),
        "neighborhoodId": str(facility.neighborhood_id) if facility.neighborhood_id else None,
        "descriptionAr": facility.description_ar or None,
        "descriptionEn": facility.description_en or None,
        "phone": facility.phone or None,
        "whatsapp": facility.whatsapp or None,
        "addressAr": facility.address_ar or None,
        "addressEn": facility.address_en or None,
        "specialtyIds": sorted(facility.specialty_links.values_list("specialty_id", flat=True)),
        "serviceTagIds": sorted(
            facility.service_links.values_list("service_tag_id", flat=True)
        ),
        "ownerCount": facility.memberships.filter(role=FacilityMembership.Role.OWNER).count(),
        "activatedAt": _iso(facility.activated_at),
        "lastVerifiedAt": _iso(facility.last_verified_at),
        "createdAt": _iso(facility.created_at),
    }


def application_payload(application: Any) -> Any:
    facility = application.facility
    return {
        "id": str(application.id),
        "facilityId": str(application.facility_id),
        "facilityNameAr": facility.name_ar,
        "kind": application.kind,
        "status": application.status,
        "provinceId": str(facility.province_id),
        "categoryId": str(facility.category_id),
        "submittedAt": _iso(application.submitted_at),
        "reviewedAt": _iso(application.reviewed_at),
        "rejectionReason": application.rejection_reason or None,
        "evidenceComplete": evidence_complete(application),
        **_names(facility),
    }
