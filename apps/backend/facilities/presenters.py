from typing import Any

from business_hours.serializers import serialize_hours
from directory.presenters import category_capabilities
from directory.tags import active_in_order

from .models import Facility, FacilityApplication


def facility_summary(facility: Facility) -> dict[str, Any]:
    latest = facility.applications.order_by("-updated_at").first()
    return {
        "id": str(facility.pk),
        "nameAr": facility.name_ar,
        "category": {
            "id": str(facility.category_id),
            "nameAr": facility.category.name_ar,
        },
        "province": {
            "id": str(facility.province_id),
            "nameAr": facility.province.name_ar,
        },
        "status": facility.status,
        "lastUpdate": facility.updated_at.isoformat(),
        "requiredAction": _required_action(facility, latest),
        # The category's capabilities, so an owner client shows exactly the controls this
        # facility supports without looking them up elsewhere (INT-056).
        "capabilities": category_capabilities(facility.category),
    }


def _required_action(facility: Facility, application: FacilityApplication | None) -> str | None:
    if application and application.status == application.Status.REJECTED:
        return "REVIEW_REJECTION"
    if facility.status == facility.Status.DRAFT:
        return "COMPLETE_AND_SUBMIT"
    if facility.status == facility.Status.REVERIFICATION_REQUIRED:
        return "REVERIFY_AND_SUBMIT"
    if facility.status == facility.Status.SUBMITTED:
        return "WAIT_FOR_REVIEW"
    if facility.status == facility.Status.SUSPENDED:
        return "CONTACT_SUPPORT"
    return None


def facility_detail(facility: Facility) -> dict[str, Any]:
    point = facility.location
    latest = facility.applications.order_by("-updated_at").first()
    return {
        **facility_summary(facility),
        "nameEn": facility.name_en or None,
        "descriptionAr": facility.description_ar or None,
        "descriptionEn": facility.description_en or None,
        "phone": facility.phone or None,
        "whatsapp": facility.whatsapp or None,
        "addressAr": facility.address_ar or None,
        "addressEn": facility.address_en or None,
        "cityId": str(facility.city_id) if facility.city_id else None,
        "neighborhoodId": (
            str(facility.neighborhood_id) if facility.neighborhood_id else None
        ),
        "location": (
            {"latitude": point.y, "longitude": point.x} if point else None
        ),
        # Integer ids, as the choices in the owner configuration carry them. A retired item
        # is left out: it is no longer offered, so an owner could neither see nor send it.
        "specialtyIds": [
            row.pk
            for row in active_in_order(link.specialty for link in facility.specialty_links.all())
        ],
        "serviceTagIds": [
            row.pk
            for row in active_in_order(
                link.service_tag for link in facility.service_links.all()
            )
        ],
        "evidence": [
            {
                "id": str(item.pk),
                "requirementId": item.requirement_id,
                "createdAt": item.created_at.isoformat(),
            }
            for item in facility.evidence.all()
        ],
        "hours": serialize_hours(facility.business_hours.order_by("weekday", "sort_order")),
        "hoursConfirmedAt": (
            facility.hours_confirmed_at.isoformat() if facility.hours_confirmed_at else None
        ),
        "application": (
            {
                "id": str(latest.pk),
                "kind": latest.kind,
                "status": latest.status,
                "rejectionReason": latest.rejection_reason or None,
                "submittedAt": (
                    latest.submitted_at.isoformat() if latest.submitted_at else None
                ),
            }
            if latest
            else None
        ),
    }
