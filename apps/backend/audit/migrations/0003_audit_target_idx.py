from django.db import migrations, models


class Migration(migrations.Migration):
    dependencies = [("audit", "0002_operational_fields")]

    operations = [
        migrations.AddIndex(
            model_name="auditevent",
            index=models.Index(fields=["target_id", "-created_at"], name="audit_target_idx"),
        ),
    ]
