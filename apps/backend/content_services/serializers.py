from typing import Any

from content_services.models import Advertisement, LegalDocument
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


def legal_summary(document: LegalDocument) -> dict[str, object]:
    """A published page as the list shows it: enough to decide whether to fetch its words."""
    return {
        "key": document.key,
        "titleAr": document.title_ar,
        "version": document.version,
        "publishedAt": document.published_at.isoformat() if document.published_at else None,
    }


def legal_document(document: LegalDocument) -> dict[str, object]:
    return {**legal_summary(document), "bodyAr": document.body_ar}
