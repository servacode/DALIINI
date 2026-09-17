from .base import *  # noqa: F403

DEBUG = False
SECRET_KEY = "test-only-key"
PASSWORD_HASHERS = ["django.contrib.auth.hashers.MD5PasswordHasher"]
# DATABASES is inherited from base: PostgreSQL + PostGIS via DATABASE_URL.
# The domain owns spatial fields, a PostGIS extension migration, btree_gist and an
# exclusion constraint, so SQLite cannot run this suite at all: it fails on
# `geo_db_type` before a single test executes. Point DATABASE_URL at a PostGIS
# instance to run the tests.
CHANNEL_LAYERS = {"default": {"BACKEND": "channels.layers.InMemoryChannelLayer"}}
CELERY_TASK_ALWAYS_EAGER = True
CELERY_TASK_EAGER_PROPAGATES = True
STORAGES = {
    "default": {"BACKEND": "django.core.files.storage.InMemoryStorage"},
    "staticfiles": {"BACKEND": "django.contrib.staticfiles.storage.StaticFilesStorage"},
}
