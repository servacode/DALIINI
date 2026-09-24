"""The picture a person puts on their own account.

It is stored the same way a facility's public photographs are, and for the same reasons: the
bytes are decoded and re-encoded rather than trusted, which strips whatever metadata the camera
wrote — a photograph carries the place it was taken, and a profile picture is the last thing
that should publish where someone lives.
"""

from __future__ import annotations

from uuid import uuid4

from django.core.files.base import ContentFile

from facilities.media import safe_reencode_image
from storage.backends import PublicS3Storage


def save_profile_image(*, user_id, upload) -> tuple[PublicS3Storage, str]:
    """Re-encode the upload and store it under a key nobody can guess.

    The key carries a random name rather than the user's, so the URL of a profile picture does
    not disclose which account it belongs to.
    """
    data, _width, _height = safe_reencode_image(upload)
    key = f"accounts/{user_id}/avatar/{uuid4().hex}.jpg"
    storage = PublicS3Storage()
    saved = storage.save(key, ContentFile(data, name="avatar.jpg"))
    return storage, saved


def profile_image_url(key: str) -> str | None:
    """The public address of a stored picture, or nothing when there is none."""
    if not key:
        return None
    return PublicS3Storage().url(key)


def delete_profile_image(key: str) -> None:
    """Remove a stored picture, and say nothing if it was already gone."""
    if not key:
        return
    try:
        PublicS3Storage().delete(key)
    except Exception:  # noqa: BLE001 - a missing object must not fail the request
        pass
