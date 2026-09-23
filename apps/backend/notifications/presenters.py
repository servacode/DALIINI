"""How a stored notification reaches the app."""

from .models import Notification


def notification_payload(notification: Notification) -> dict[str, object]:
    facility_id = notification.payload.get("facilityId")
    return {
        "id": str(notification.id),
        "type": notification.type,
        "titleAr": notification.title_ar,
        "bodyAr": notification.body_ar,
        "destination": notification.destination,
        "facilityId": (
            str(facility_id)
            if notification.destination == Notification.Destination.FACILITY and facility_id
            else None
        ),
        "isRead": notification.read_at is not None,
        "createdAt": notification.created_at.isoformat(),
    }
