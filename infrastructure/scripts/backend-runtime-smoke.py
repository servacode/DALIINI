"""Connected P2 smoke. Run only with real development/staging dependencies configured."""
from __future__ import annotations

import os

os.environ.setdefault("DJANGO_SETTINGS_MODULE", "directory_backend.settings.development")

import django

django.setup()

from django.conf import settings
from django.db import connection
import redis
from storages.backends.s3 import S3Storage


def main() -> None:
    with connection.cursor() as cursor:
        cursor.execute("SELECT PostGIS_Version()")
        version = cursor.fetchone()[0]
        if not version:
            raise RuntimeError("PostGIS version unavailable")

    client = redis.Redis.from_url(settings.REDIS_URL)
    if client.ping() is not True:
        raise RuntimeError("Redis ping failed")

    storage = S3Storage(
        endpoint_url=settings.S3_ENDPOINT_URL,
        region_name=settings.S3_REGION,
        access_key=settings.S3_ACCESS_KEY_ID,
        secret_key=settings.S3_SECRET_ACCESS_KEY,
        bucket_name=settings.S3_PRIVATE_BUCKET,
    )
    # Accessing bucket metadata is enough to prove credentials/endpoint connectivity.
    storage.connection.meta.client.head_bucket(Bucket=settings.S3_PRIVATE_BUCKET)
    print("P2 connected runtime smoke passed")


if __name__ == "__main__":
    main()
