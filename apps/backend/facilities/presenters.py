from business_hours.serializers import serialize_hours
from directory.presenters import category_capabilities


def facility_summary(facility):
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


def _required_action(facility, application):
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


def facility_detail(facility):
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
        "specialtyIds": [
            str(item.specialty_id) for item in facility.specialty_links.all()
        ],
        "serviceTagIds": [
            str(item.service_tag_id) for item in facility.service_links.all()
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
