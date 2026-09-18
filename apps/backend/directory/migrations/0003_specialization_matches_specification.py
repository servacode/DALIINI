"""Align Category.specialization with the canonical list in the specification.

`01-MASTER-SPECIFICATION.md` section 5 names exactly four values: GENERIC, PHARMACY,
MEDICAL_CLINIC and NURSING_CENTER. The model declared GENERIC, PHARMACY, DOCTOR, NURSING
and MEDICAL_SUPPLIES instead. That is INT-040.

`choices` is not a database constraint on PostgreSQL, so this operation emits no DDL; it
exists to keep the migration state, the model and the generated OpenAPI enum in agreement.
No row is affected: no Category exists in any environment yet, which is precisely why the
correction is cheap now and would not have been later.
"""

from django.db import migrations, models


class Migration(migrations.Migration):
    dependencies = [
        ("directory", "0002_category_icon_key"),
    ]

    operations = [
        migrations.AlterField(
            model_name="category",
            name="specialization",
            field=models.CharField(
                choices=[
                    ("GENERIC", "Generic"),
                    ("PHARMACY", "Pharmacy"),
                    ("MEDICAL_CLINIC", "Medical clinic"),
                    ("NURSING_CENTER", "Nursing center"),
                ],
                default="GENERIC",
                max_length=40,
            ),
        ),
    ]
