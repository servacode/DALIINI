import uuid
from django.db import migrations, models
import django.db.models.deletion


class Migration(migrations.Migration):
    initial = True
    dependencies = [("locations", "0001_initial"), ("directory", "0002_category_icon_key")]
    operations = [
        migrations.CreateModel(
            name="Advertisement",
            fields=[
                ("id", models.UUIDField(default=uuid.uuid4, editable=False, primary_key=True, serialize=False)),
                ("image_key", models.CharField(max_length=500)),
                ("title_ar", models.CharField(blank=True, max_length=180)),
                ("title_en", models.CharField(blank=True, max_length=180)),
                ("subtitle_ar", models.CharField(blank=True, max_length=280)),
                ("subtitle_en", models.CharField(blank=True, max_length=280)),
                ("action_type", models.CharField(choices=[("NONE", "None"),("FACILITY", "Facility"),("CATEGORY", "Category"),("EXTERNAL_URL", "External URL"),("IN_APP_ROUTE", "In-app route")], default="NONE", max_length=24)),
                ("action_payload", models.JSONField(blank=True, default=dict)),
                ("target_scope", models.CharField(choices=[("GLOBAL", "Global"),("PROVINCE", "Province"),("CATEGORY", "Category")], default="GLOBAL", max_length=16)),
                ("starts_at", models.DateTimeField(blank=True, null=True)),
                ("ends_at", models.DateTimeField(blank=True, null=True)),
                ("enabled", models.BooleanField(default=False)),
                ("sort_order", models.PositiveIntegerField(default=0)),
                ("slide_duration_ms", models.PositiveIntegerField(default=5000)),
                ("created_at", models.DateTimeField(auto_now_add=True)),
                ("updated_at", models.DateTimeField(auto_now=True)),
                ("category", models.ForeignKey(blank=True, null=True, on_delete=django.db.models.deletion.CASCADE, to="directory.category")),
                ("province", models.ForeignKey(blank=True, null=True, on_delete=django.db.models.deletion.CASCADE, to="locations.province")),
            ],
            options={"indexes":[models.Index(fields=["enabled","starts_at","ends_at"],name="content_ser_enabled_57493d_idx"),models.Index(fields=["target_scope","province","category"],name="content_ser_target__67e917_idx")]},
        )
    ]
