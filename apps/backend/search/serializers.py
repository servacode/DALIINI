from collections.abc import Callable
from typing import Any

from business_hours.serializers import serialize_hours
from business_hours.services import (
    availability_from_flags,
    get_facility_availability,
    is_on_duty_today,
    is_open_now,
)
from directory.tags import ORDER as TAG_ORDER
from directory.tags import named
from facilities.slugs import facility_slug
from storage.backends import PublicS3Storage

_UNSET = object()


def _flag(facility: Any, annotation: str, fallback: Callable[[Any], object]) -> bool:
    """Prefer what the query already worked out; compute only when it did not.

    Views that page a list annotate these, so the common path costs nothing. The fallback
    keeps a single serialised facility correct wherever the annotation was not asked for.
    """
    value = getattr(facility, annotation, _UNSET)
    return bool(value) if value is not _UNSET else bool(fallback(facility))


def _iso(value: Any) -> str | None:
    return value.isoformat() if value else None


_FLAGS = ("_availability_closed", "_availability_duty", "_availability_scheduled")


def _availability_payload(facility: Any) -> dict[str, Any]:
    # A list row carries the three flags from its own query; a lone facility asks the engine.
    flagged = all(getattr(facility, name, _UNSET) is not _UNSET for name in _FLAGS)
    result = availability_from_flags(facility) if flagged else get_facility_availability(facility)
    return {
        "state": result.state.value,
        "nextOpenAt": result.next_open_at.isoformat() if result.next_open_at else None,
        # Independent of each other and of `state`, which collapses both into one value and
        # lets duty win. A pharmacy can be open and on duty, shut and on duty, or open and not
        # on duty, and the three have to be tellable apart on a list row.
        "isOpenNow": _open_now(facility),
        "isOnDutyToday": _flag(facility, "_availability_duty_today", is_on_duty_today),
    }


def _open_now(facility: Any) -> bool:
    closed = getattr(facility, "_availability_closed", _UNSET)
    scheduled = getattr(facility, "_availability_scheduled", _UNSET)
    if closed is not _UNSET and scheduled is not _UNSET:
        return bool(scheduled) and not bool(closed)
    return bool(is_open_now(facility))


def _first_image_url(facility: Any) -> str | None:
    """The one picture a list row shows, or nothing at all.

    `images` is prefetched in the ordering the owner chose, so this reads the list already in
    memory rather than asking the database once per row.
    """
    images = list(facility.images.all())
    if not images:
        return None
    return PublicS3Storage().url(images[0].storage_key)


def info_confirmed_at(facility: Any) -> Any:
    """The later of an operator's approval and the owner's own confirmation of the hours."""
    moments = [
        value
        for value in (facility.last_verified_at, getattr(facility, "hours_confirmed_at", None))
        if value is not None
    ]
    return max(moments) if moments else None


def compact_facility(facility: Any) -> dict[str, Any]:
    distance = getattr(facility, "distance_meters", None)
    return {
        "id": str(facility.id),
        "slug": facility_slug(facility.name_ar),
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
        # How to reach it and how to get there, on the row itself. These three were detail-only,
        # which made calling a pharmacy from a list of pharmacies a page load away; they are the
        # same public values the detail endpoint has always served, and they are already loaded
        # on the row, so no list query grows for them.
        # Where it is, in words. A row that says only "Raqqa" is a row somebody has to open to
        # know whether it is the pharmacy on their street.
        "addressAr": facility.address_ar or None,
        "neighborhood": (
            {"id": str(facility.neighborhood_id), "nameAr": facility.neighborhood.name_ar}
            if facility.neighborhood_id
            else None
        ),
        "phone": facility.phone or None,
        "whatsapp": facility.whatsapp or None,
        "location": (
            {"latitude": facility.location.y, "longitude": facility.location.x}
            if facility.location
            else None
        ),
        # False for anonymous callers and for anyone who has not saved it (INT-097).
        "isFavorite": bool(getattr(facility, "is_favorite", False)),
        # The owner's own first photograph, or null. A list row shows the brand mark when it
        # is null; nothing stands in for a picture the facility never uploaded.
        "imageUrl": _first_image_url(facility),
        "lastVerifiedAt": _iso(facility.last_verified_at),
        "infoConfirmedAt": _iso(info_confirmed_at(facility)),
        "updatedAt": _iso(facility.updated_at),
    }


def facility_detail(facility: Any) -> dict[str, Any]:
    storage = PublicS3Storage()
    payload = compact_facility(facility)
    payload.update(
        {
            "descriptionAr": facility.description_ar or None,
            "descriptionEn": facility.description_en or None,
            "addressEn": facility.address_en or None,
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
            # Integer ids, as the rows are keyed (`NamedIntRef`). They were sent as text under
            # a contract that called them UUIDs, so a client that trusted the contract could
            # not open a facility that had one. A retired (inactive) item is not shown.
            "specialties": [
                named(link.specialty)
                for link in facility.specialty_links.filter(specialty__active=True)
                .select_related("specialty")
                .order_by(*(f"specialty__{column}" for column in TAG_ORDER))
            ],
            "services": [
                named(link.service_tag)
                for link in facility.service_links.filter(service_tag__active=True)
                .select_related("service_tag")
                .order_by(*(f"service_tag__{column}" for column in TAG_ORDER))
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
