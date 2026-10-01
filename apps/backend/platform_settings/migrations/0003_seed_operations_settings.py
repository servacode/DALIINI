from django.db import migrations

DEFAULTS = {
    "review.slaHours": ("INTEGER", 48),
    "readiness.minActiveFacilities": ("INTEGER", 5),
}


def seed(apps, schema_editor):
    PlatformSetting = apps.get_model("platform_settings", "PlatformSetting")
    for key, (value_type, value) in DEFAULTS.items():
        PlatformSetting.objects.get_or_create(
            key=key, defaults={"value_type": value_type, "value": value}
        )


class Migration(migrations.Migration):
    dependencies = [("platform_settings", "0002_seed_maintenance_settings")]
    operations = [migrations.RunPython(seed, migrations.RunPython.noop)]
