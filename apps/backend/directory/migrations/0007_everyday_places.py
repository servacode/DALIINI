"""Seed the everyday places: a second group and six categories, on in Raqqa (DECISION-125).

The same `apply_dataset` as `0004_launch_baseline`, pinned to `launch_v2`, which is frozen. Rows
that already exist are left as an operator has them. The reverse is a no-op for the reason
`0004` gives: facilities reference categories under PROTECT.
"""

from django.db import migrations

from directory.reference_data import launch_v2
from directory.reference_data.apply import apply_dataset


def seed(apps, schema_editor):
    apply_dataset(launch_v2, apps.get_model)


def unseed(apps, schema_editor):
    """Intentionally does nothing. See the module docstring."""


class Migration(migrations.Migration):
    dependencies = [
        ("directory", "0006_category_group_icon"),
    ]

    operations = [
        migrations.RunPython(seed, unseed),
    ]
