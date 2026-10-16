"""Switch «صرافة وحوالات» off everywhere (DECISION-126).

Google Play lets only organization accounts publish financial services, and a money-exchange
category reads as one. The category stays (its rows and any facility under it are the operator's),
but no province shows it until an operator turns it back on from the console.
"""

from django.db import migrations


def switch_off(apps, schema_editor):
    CategoryProvince = apps.get_model("directory", "CategoryProvince")
    CategoryProvince.objects.filter(category__code="exchange").update(
        public_enabled=False, owner_registration_enabled=False
    )


class Migration(migrations.Migration):
    dependencies = [("directory", "0007_everyday_places")]

    operations = [migrations.RunPython(switch_off, migrations.RunPython.noop)]
