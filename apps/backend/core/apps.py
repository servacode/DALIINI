from django.apps import AppConfig


class CoreConfig(AppConfig):
    default_auto_field = "django.db.models.BigAutoField"
    name = "core"

    def ready(self):
        # Importing the module registers the drf-spectacular authentication and
        # serializer extensions; without it the schema carries no security scheme.
        from . import openapi  # noqa: F401
