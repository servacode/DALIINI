from typing import Any

from content_services.models import Advertisement
from storage.public_media import public_media_url


def public_ad(ad: Advertisement) -> dict[str, Any]:
    return {
        "id": str(ad.id),
        # The image's public address. The relative path served before pointed at a route
        # that does not exist (INT-065).
        "imageUrl": public_media_url(ad.image_key),
        "titleAr": ad.title_ar or None,
        "titleEn": ad.title_en or None,
        "subtitleAr": ad.subtitle_ar or None,
        "subtitleEn": ad.subtitle_en or None,
        "action": {"type": ad.action_type, "payload": ad.action_payload},
        "slideDurationMs": ad.slide_duration_ms,
    }
