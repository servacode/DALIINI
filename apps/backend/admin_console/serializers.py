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
        # The name beside the id. The console shows accounts to a person, and a UUID tells
        # them nothing; without it every row would have to fetch the province list to
        # translate one field.
        "provinceName": user.province.name_ar if user.province_id else None,
        "phoneVerifiedAt": _iso(user.phone_verified_at),
        "lastLoginAt": _iso(user.last_login),
        "createdAt": _iso(user.created_at),
        "updatedAt": _iso(user.updated_at),
    }


def user_facilities_payload(user: Any) -> list[dict[str, Any]]:
    """The places this account owns or helps run.

    An operator about to block an account needs to know what hangs off it. Three active
    pharmacies behind a name is the difference between a block and a phone call, and the
    console had no way to learn it: the facility list shows its owner, and nothing showed
    the reverse.
    """
    links = (
        FacilityMembership.objects.filter(user=user)
        .select_related("facility")
        .order_by("facility__name_ar")
    )
    return [
        {
            "id": str(link.facility_id),
            "nameAr": link.facility.name_ar,
            "role": link.role,
            "status": link.facility.status,
        }
        for link in links
    ]


def user_sessions_payload(user: Any) -> list[dict[str, Any]]:
    """The devices currently signed in, newest first.

    Secrets never leave the server: neither the refresh digest nor the previous one is in
    this payload, and nothing here can be used to sign in. Revoked and expired sessions are
    left out — the question this answers is «who is signed in now».
    """
    from django.utils import timezone

    from sessions.models import UserSession

    rows = UserSession.objects.filter(
        user=user, revoked_at__isnull=True, expires_at__gt=timezone.now()
    ).order_by("-created_at")
    return [
        {
            "id": str(row.id),
            "platform": row.platform,
            "deviceName": row.device_name,
            "createdAt": _iso(row.created_at),
            "lastSeenAt": _iso(row.last_seen_at),
        }
        for row in rows
    ]


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
    return queryset.select_related(
        "facility__category", "facility__province", "applicant"
    ).prefetch_related(
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
        # A claim comes from somebody who is not a member yet; nobody else's does.
        "applicantName": application.applicant.name if application.applicant_id else None,
        "applicantPhone": application.applicant.phone if application.applicant_id else None,
    }
