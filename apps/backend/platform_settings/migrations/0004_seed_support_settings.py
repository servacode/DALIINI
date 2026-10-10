from django.conf import settings
from django.db import migrations

# Seeded from the environment, so a server that already had SUPPORT_WHATSAPP keeps showing it.
KEYS = {
    "support.whatsapp": "SUPPORT_WHATSAPP",
    "support.email": "SUPPORT_EMAIL",
}


def seed(apps, schema_editor):
    PlatformSetting = apps.get_model("platform_settings", "PlatformSetting")
    for key, env_name in KEYS.items():
        PlatformSetting.objects.get_or_create(
            key=key,
            defaults={
                "value_type": "STRING",
                "value": str(getattr(settings, env_name, "") or "").strip(),
            },
        )


class Migration(migrations.Migration):
    dependencies = [("platform_settings", "0003_seed_operations_settings")]
    operations = [migrations.RunPython(seed, migrations.RunPython.noop)]
