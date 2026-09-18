"""Seed the V3 launch baseline: the fourteen provinces and the health taxonomy.

This runs as part of migration history so a fresh database — a developer's, CI's, staging's
or production's — arrives at the state `01-MASTER-SPECIFICATION.md` section 3 describes
without anyone remembering to run a command. `seed_launch_baseline` exists for the other
cases: qualification, verification, disaster recovery and repairing a missing reference row.

Both call the same `apply_dataset`, so they cannot drift apart. The dataset is pinned to
`launch_v1`, which is frozen; a future baseline is a new dataset module and a new migration.

The reverse is a deliberate no-op. Provinces are referenced by `Facility.province` under
`PROTECT` and categories the same way, so deleting them on reverse would either fail loudly
or, worse, cascade through configuration an operator has since changed. Unapplying this
migration therefore leaves the reference data in place; removing it is a manual decision
with its own review.
"""

from django.db import migrations

from directory.reference_data import launch_v1
from directory.reference_data.apply import apply_dataset


def seed(apps, schema_editor):
    apply_dataset(launch_v1, apps.get_model)


def unseed(apps, schema_editor):
    """Intentionally does nothing. See the module docstring."""


class Migration(migrations.Migration):
    dependencies = [
        ("directory", "0003_specialization_matches_specification"),
        ("locations", "0001_initial"),
    ]

    operations = [
        migrations.RunPython(seed, unseed),
    ]
