
def public_ad(ad):
    return {
        "id": str(ad.id),
        "imageUrl": f"/api/v1/public/media/ads/{ad.id}/",
        "titleAr": ad.title_ar or None,
        "titleEn": ad.title_en or None,
        "subtitleAr": ad.subtitle_ar or None,
        "subtitleEn": ad.subtitle_en or None,
        "action": {"type": ad.action_type, "payload": ad.action_payload},
        "slideDurationMs": ad.slide_duration_ms,
    }
