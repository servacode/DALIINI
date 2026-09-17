from django.conf import settings
from django.db import migrations, models
from django.db.models import Q
import django.db.models.deletion


class Migration(migrations.Migration):
    dependencies = [
        ("facilities", "0002_facility_specialties_services"),
        migrations.swappable_dependency(settings.AUTH_USER_MODEL),
    ]

    operations = [
        migrations.AddField(
            model_name="facilityimage",
            name="width",
            field=models.PositiveIntegerField(default=0),
        ),
        migrations.AddField(
            model_name="facilityimage",
            name="height",
            field=models.PositiveIntegerField(default=0),
        ),
        migrations.AddField(
            model_name="verificationevidence",
            name="uploaded_by",
            field=models.ForeignKey(
                blank=True,
                null=True,
                on_delete=django.db.models.deletion.SET_NULL,
                related_name="+",
                to=settings.AUTH_USER_MODEL,
            ),
        ),
        migrations.AddConstraint(
            model_name="facilityapplication",
            constraint=models.UniqueConstraint(
                fields=("facility", "kind"),
                condition=Q(status="SUBMITTED"),
                name="uniq_submitted_application_per_facility_kind",
            ),
        ),
    ]
