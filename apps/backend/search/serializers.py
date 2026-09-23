from business_hours.serializers import serialize_hours
from business_hours.services import get_facility_availability
from storage.backends import PublicS3Storage


def _availability_payload(facility):
    result = get_facility_availability(facility)
    return {
        "state": result.state.value,
        "nextOpenAt": result.next_open_at.isoformat() if result.next_open_at else None,
    }


def compact_facility(facility):
    distance = getattr(facility, "distance_meters", None)
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
        "distanceMeters": round(distance, 1) if distance is not None else None,
        "ratingAverage": (
            round(float(facility.rating_average), 2)
            if getattr(facility, "rating_average", None) is not None
            else None
        ),
        "ratingCount": getattr(facility, "rating_count", 0),
        "availability": _availability_payload(facility),
        # False for anonymous callers and for anyone who has not saved it (INT-097).
        "isFavorite": bool(getattr(facility, "is_favorite", False)),
    }


def facility_detail(facility):
    storage = PublicS3Storage()
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
            # The storage URL, as the owner endpoint serves it. The previous relative path
            # pointed at a route that does not exist, so every public image was a broken
            # link (INT-060).
            "images": [
                {
                    "id": str(image.id),
                    "url": storage.url(image.storage_key),
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
            # The shape the contract declares: with the id and the `sequence` that orders a
            # day's spans. Both were missing, so a generated client could not read any
            # facility that had opening hours (INT-059).
            "hours": serialize_hours(
                facility.business_hours.order_by("weekday", "sort_order", "opens_at")
            ),
        }
    )
    return payload
