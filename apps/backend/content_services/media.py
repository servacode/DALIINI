"""Advertisement images: checked, re-encoded and stored in the public media bucket.

Stored exactly like a facility's public photographs (`facilities.media`): the bytes are
decoded and re-encoded to JPEG rather than trusted, which also strips any metadata, and the
key is random so it discloses nothing. The limits are tighter than for photographs because
an ad is a banner loaded on every home screen.
"""

from __future__ import annotations

from io import BytesIO
from typing import Any
from uuid import uuid4

from django.core.exceptions import ValidationError
from django.core.files.base import ContentFile
from PIL import Image, UnidentifiedImageError

from facilities.media import safe_reencode_image
from storage.backends import PublicS3Storage

MAX_AD_IMAGE_BYTES = 2 * 1024 * 1024
ALLOWED_FORMATS = frozenset({"JPEG", "PNG", "WEBP"})
MIN_SIDE = 100
MAX_SIDE = 4096


def save_ad_image(upload: Any) -> tuple[PublicS3Storage, str, int, int]:
    """Validate an uploaded ad image and store it. Returns (storage, key, width, height)."""
    if getattr(upload, "size", 0) > MAX_AD_IMAGE_BYTES:
        raise ValidationError("The image is larger than 2 MB.")
    raw = upload.read(MAX_AD_IMAGE_BYTES + 1)
    if len(raw) > MAX_AD_IMAGE_BYTES:
        raise ValidationError("The image is larger than 2 MB.")
    if not raw:
        raise ValidationError("The image is empty.")
    try:
        with Image.open(BytesIO(raw)) as image:
            image_format = image.format
            width, height = image.size
    except (UnidentifiedImageError, OSError) as exc:
        raise ValidationError("The file is not a valid image.") from exc
    if image_format not in ALLOWED_FORMATS:
        raise ValidationError("Only JPEG, PNG and WebP images are accepted.")
    if not (MIN_SIDE <= width <= MAX_SIDE and MIN_SIDE <= height <= MAX_SIDE):
        raise ValidationError(f"Each side must be between {MIN_SIDE} and {MAX_SIDE} pixels.")
    data, width, height = safe_reencode_image(ContentFile(raw, name="ad"))
    key = f"ads/{uuid4().hex}.jpg"
    storage = PublicS3Storage()
    saved = storage.save(key, ContentFile(data, name="ad.jpg"))
    return storage, saved, width, height
