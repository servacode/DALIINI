from django.db import migrations

DEFAULTS = {
    "maintenance.enabled": ("BOOLEAN", False),
    "maintenance.messageAr": ("STRING", "المنصة قيد الصيانة حالياً. يرجى المحاولة لاحقاً."),
    "maintenance.retryAfterSeconds": ("INTEGER", 600),
}


def seed(apps, schema_editor):
    PlatformSetting = apps.get_model("platform_settings", "PlatformSetting")
    for key, (value_type, value) in DEFAULTS.items():
        PlatformSetting.objects.get_or_create(
            key=key, defaults={"value_type": value_type, "value": value}
        )


class Migration(migrations.Migration):
    dependencies = [("platform_settings", "0001_initial")]
    operations = [migrations.RunPython(seed, migrations.RunPython.noop)]
