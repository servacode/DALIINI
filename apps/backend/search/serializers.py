from business_hours.services import get_facility_availability


def _availability_payload(facility):
    result = get_facility_availability(facility)
    return {
        "state": result.state.value,
        "nextOpenAt": result.next_open_at.isoformat() if result.next_open_at else None,
    }


def compact_facility(facility):
    distance = getattr(facility, "distance", None)
    return {
        "id": str(facility.id),
        "nameAr": facility.name_ar,
        "nameEn": facility.name_en or None,
        "category": {
            "id": str(facility.category_id),
            "nameAr": facility.category.name_ar,
            "nameEn": facility.category.name_en or None,
        },
        "city": (
            {"id": str(facility.city_id), "nameAr": facility.city.name_ar}
            if facility.city_id
            else None
        ),
        "distanceMeters": round(distance.m, 1) if distance is not None else None,
        "ratingAverage": (
            round(float(facility.rating_average), 2)
            if getattr(facility, "rating_average", None) is not None
            else None
        ),
        "ratingCount": getattr(facility, "rating_count", 0),
        "availability": _availability_payload(facility),
    }


def facility_detail(facility):
    payload = compact_facility(facility)
    payload.update(
        {
            "descriptionAr": facility.description_ar or None,
            "descriptionEn": facility.description_en or None,
            "phone": facility.phone or None,
            "addressAr": facility.address_ar or None,
            "addressEn": facility.address_en or None,
            "neighborhood": (
                {
                    "id": str(facility.neighborhood_id),
                    "nameAr": facility.neighborhood.name_ar,
                }
                if facility.neighborhood_id
                else None
            ),
            "location": (
                {
                    "latitude": facility.location.y,
                    "longitude": facility.location.x,
                }
                if facility.location
                else None
            ),
            "images": [
                {
                    "id": str(image.id),
                    "url": f"/api/v1/public/media/images/{image.id}/",
                }
                for image in facility.images.order_by("sort_order", "created_at")
            ],
            "specialties": [
                {
                    "id": str(link.specialty_id),
                    "nameAr": link.specialty.name_ar,
                }
                for link in facility.specialty_links.select_related("specialty")
            ],
            "services": [
                {
                    "id": str(link.service_tag_id),
                    "nameAr": link.service_tag.name_ar,
                }
                for link in facility.service_links.select_related("service_tag")
            ],
            "hours": [
                {
                    "weekday": row.weekday,
                    "opensAt": row.opens_at.isoformat(),
                    "closesAt": row.closes_at.isoformat(),
                }
                for row in facility.business_hours.order_by(
                    "weekday",
                    "opens_at",
                    "sort_order",
                )
            ],
        }
    )
    return payload
