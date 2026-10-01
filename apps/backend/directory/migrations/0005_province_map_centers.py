"""Give the launch province its map centre (INT-092).

Same pattern as `0004_launch_baseline`: the frozen dataset `province_map_centers_v1` is
written by `apply_map_centers`, which `seed_launch_baseline` also calls, and a centre that is
already set is never overwritten. The reverse is a no-op for the same reason 0004's is: by
then the value may be an operator's.
"""

from django.db import migrations

from directory.reference_data import province_map_centers_v1
from directory.reference_data.apply import apply_map_centers


def seed(apps, schema_editor):
    apply_map_centers(province_map_centers_v1, apps.get_model)


def unseed(apps, schema_editor):
    """Intentionally does nothing. See the module docstring."""


class Migration(migrations.Migration):
    dependencies = [
        ("directory", "0004_launch_baseline"),
        ("locations", "0002_province_map_center"),
    ]

    operations = [
        migrations.RunPython(seed, unseed),
    ]
