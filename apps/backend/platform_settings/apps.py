from django.apps import AppConfig


class PlatformSettingsConfig(AppConfig):
    default_auto_field = "django.db.models.BigAutoField"
    name = "platform_settings"

    def ready(self) -> None:
        from . import signals  # noqa: F401
