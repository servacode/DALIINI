"""Versioned canonical reference data for the platform.

Each module here is a frozen dataset. `launch_v1` is the V3 launch baseline and is
**immutable**: it has entered a shared data migration, so editing it would silently give
two databases different canonical data depending on when they were migrated. A future
change is a new `launch_v2` module and a new migration, never an edit to an existing one.

`apply.py` holds the logic that writes a dataset into the database. It is deliberately the
only place that logic exists, so the data migration and the `seed_launch_baseline`
management command cannot drift apart.
"""
