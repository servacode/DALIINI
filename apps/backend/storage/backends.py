from django.conf import settings
from storages.backends.s3 import S3Storage


class _DirectoryS3Storage(S3Storage):
    endpoint_url = settings.S3_ENDPOINT_URL
    region_name = settings.S3_REGION
    access_key = settings.S3_ACCESS_KEY_ID
    secret_key = settings.S3_SECRET_ACCESS_KEY
    default_acl = None
    querystring_auth = True


class PrivateS3Storage(_DirectoryS3Storage):
    bucket_name = settings.S3_PRIVATE_BUCKET
    file_overwrite = False


class PublicS3Storage(_DirectoryS3Storage):
    bucket_name = settings.S3_PUBLIC_BUCKET
    file_overwrite = False
