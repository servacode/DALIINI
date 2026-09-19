"""Stable addresses for public media.

Two kinds of object live in object storage, in separate buckets with separate policies:

- PUBLIC_MEDIA — facility photos and advertisement images, meant to be shown to anyone. They
  are read directly from `S3_PUBLIC_MEDIA_BASE_URL`, which in production is a CDN in front of
  the public bucket. The address never expires, so a photo a client cached keeps working.
- PRIVATE_EVIDENCE — verification uploads. They are never addressable: an authorised operator
  receives their bytes through the backend, which checks permission and writes an audit entry.

Nothing here ever produces an address for the private bucket.
"""

from urllib.parse import quote

from django.conf import settings


def public_media_url(key: str) -> str:
    """The permanent public address of an object in the public media bucket."""
    base = str(settings.S3_PUBLIC_MEDIA_BASE_URL).rstrip("/")
    return f"{base}/{quote(key.lstrip('/'), safe='/')}"
