def _iso(value):
    return value.isoformat() if value else None


def user_payload(user):
    return {
        "id": str(user.id),
        "name": user.name,
        "phone": user.phone,
        "active": user.is_active,
        "provinceId": str(user.province_id) if user.province_id else None,
        "createdAt": _iso(user.created_at),
        "updatedAt": _iso(user.updated_at),
    }


def facility_payload(facility):
    return {
        "id": str(facility.id),
        "nameAr": facility.name_ar,
        "nameEn": facility.name_en or None,
        "categoryId": str(facility.category_id),
        "provinceId": str(facility.province_id),
        "cityId": str(facility.city_id) if facility.city_id else None,
        "status": facility.status,
        "location": (
            {"latitude": facility.location.y, "longitude": facility.location.x}
            if facility.location
            else None
        ),
        "updatedAt": _iso(facility.updated_at),
    }


def application_payload(application):
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
    }
