"""How a stored notification reaches the app."""

import uuid

from .models import Notification

# Destinations whose message is about one facility: its public page, or one of the owner's own.
FACILITY_DESTINATIONS = {
    Notification.Destination.FACILITY,
    Notification.Destination.OWNER_FACILITIES,
}


def _facility_id(notification: Notification) -> str | None:
    if notification.destination not in FACILITY_DESTINATIONS:
        return None
    try:
        return str(uuid.UUID(str(notification.payload.get("facilityId"))))
    except (TypeError, ValueError):
        return None


def notification_payload(notification: Notification) -> dict[str, object]:
    return {
        "id": str(notification.id),
        "type": notification.type,
        "titleAr": notification.title_ar,
        "bodyAr": notification.body_ar,
        "destination": notification.destination,
        "facilityId": _facility_id(notification),
        "isRead": notification.read_at is not None,
        "createdAt": notification.created_at.isoformat(),
    }
