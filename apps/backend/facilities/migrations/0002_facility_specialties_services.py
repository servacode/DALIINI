from django.db import migrations, models
import django.db.models.deletion


class Migration(migrations.Migration):
    dependencies = [
        ("directory", "0001_initial"),
        ("facilities", "0001_initial"),
    ]
    operations = [
        migrations.CreateModel(
            name="FacilitySpecialty",
            fields=[
                (
                    "id",
                    models.BigAutoField(
                        auto_created=True,
                        primary_key=True,
                        serialize=False,
                        verbose_name="ID",
                    ),
                ),
                (
                    "facility",
                    models.ForeignKey(
                        on_delete=django.db.models.deletion.CASCADE,
                        related_name="specialty_links",
                        to="facilities.facility",
                    ),
                ),
                (
                    "specialty",
                    models.ForeignKey(
                        on_delete=django.db.models.deletion.PROTECT,
                        related_name="facility_links",
                        to="directory.specialty",
                    ),
                ),
            ],
        ),
        migrations.CreateModel(
            name="FacilityServiceTag",
            fields=[
                (
                    "id",
                    models.BigAutoField(
                        auto_created=True,
                        primary_key=True,
                        serialize=False,
                        verbose_name="ID",
                    ),
                ),
                (
                    "facility",
                    models.ForeignKey(
                        on_delete=django.db.models.deletion.CASCADE,
                        related_name="service_links",
                        to="facilities.facility",
                    ),
                ),
                (
                    "service_tag",
                    models.ForeignKey(
                        on_delete=django.db.models.deletion.PROTECT,
                        related_name="facility_links",
                        to="directory.servicetag",
                    ),
                ),
            ],
        ),
        migrations.AddConstraint(
            model_name="facilityspecialty",
            constraint=models.UniqueConstraint(
                fields=("facility", "specialty"),
                name="uniq_facility_specialty",
            ),
        ),
        migrations.AddConstraint(
            model_name="facilityservicetag",
            constraint=models.UniqueConstraint(
                fields=("facility", "service_tag"),
                name="uniq_facility_service_tag",
            ),
        ),
    ]
