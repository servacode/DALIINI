from __future__ import annotations

from io import BytesIO
from typing import IO
from uuid import UUID, uuid4

from django.core.exceptions import ValidationError
from PIL import Image, UnidentifiedImageError

from storage.backends import PrivateS3Storage, PublicS3Storage

MAX_UPLOAD_BYTES = 10 * 1024 * 1024
MAX_IMAGE_PIXELS = 25_000_000
MAX_DIMENSION = 10_000
JPEG_QUALITY = 88


def _read_upload(upload: IO[bytes]) -> bytes:
    if getattr(upload, "size", 0) > MAX_UPLOAD_BYTES:
        raise ValidationError("Image exceeds the upload byte limit.")
    data = upload.read(MAX_UPLOAD_BYTES + 1)
    if len(data) > MAX_UPLOAD_BYTES:
        raise ValidationError("Image exceeds the upload byte limit.")
    if not data:
        raise ValidationError("Image is empty.")
    return data


def safe_reencode_image(upload: IO[bytes]) -> tuple[bytes, int, int]:
    raw = _read_upload(upload)
    try:
        with Image.open(BytesIO(raw)) as image:
            width, height = image.size
            if width <= 0 or height <= 0:
                raise ValidationError("Image dimensions are invalid.")
            if width > MAX_DIMENSION or height > MAX_DIMENSION:
                raise ValidationError("Image dimensions exceed the allowed limit.")
            if width * height > MAX_IMAGE_PIXELS:
                raise ValidationError("Image pixel count exceeds the allowed limit.")
            image.load()
            safe = image.convert("RGB")
            output = BytesIO()
            safe.save(output, format="JPEG", quality=JPEG_QUALITY, optimize=True)
            return output.getvalue(), width, height
    except (UnidentifiedImageError, OSError) as exc:
        raise ValidationError("Uploaded file is not a valid image.") from exc


def save_public_image(
    *, facility_id: UUID, upload: IO[bytes]
) -> tuple[PublicS3Storage, str, int, int]:
    data, width, height = safe_reencode_image(upload)
    key = f"facilities/{facility_id}/public/{uuid4().hex}.jpg"
    storage = PublicS3Storage()
    from django.core.files.base import ContentFile

    saved = storage.save(key, ContentFile(data, name="image.jpg"))
    return storage, saved, width, height


def save_private_evidence(
    *, facility_id: UUID, requirement_id: int, upload: IO[bytes]
) -> tuple[PrivateS3Storage, str, int, int]:
    data, width, height = safe_reencode_image(upload)
    key = (
        f"facilities/{facility_id}/evidence/{requirement_id}/"
        f"{uuid4().hex}.jpg"
    )
    storage = PrivateS3Storage()
    from django.core.files.base import ContentFile

    saved = storage.save(key, ContentFile(data, name="evidence.jpg"))
    return storage, saved, width, height
