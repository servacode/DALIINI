import uuid
from django.conf import settings
from django.db import migrations, models
import django.db.models.deletion
import django.core.validators


class Migration(migrations.Migration):
    initial = True
    dependencies = [
        migrations.swappable_dependency(settings.AUTH_USER_MODEL),
        ("facilities", "0001_initial"),
    ]
    operations = [
        migrations.CreateModel(
            name="Rating",
            fields=[
                ("id", models.UUIDField(default=uuid.uuid4, editable=False, primary_key=True, serialize=False)),
                ("stars", models.PositiveSmallIntegerField(validators=[django.core.validators.MinValueValidator(1), django.core.validators.MaxValueValidator(5)])),
                ("created_at", models.DateTimeField(auto_now_add=True)),
                ("updated_at", models.DateTimeField(auto_now=True)),
                ("facility", models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, related_name="ratings", to="facilities.facility")),
                ("user", models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, related_name="ratings", to=settings.AUTH_USER_MODEL)),
            ],
        ),
        migrations.AddConstraint(model_name="rating", constraint=models.UniqueConstraint(fields=("user", "facility"), name="uniq_user_facility_rating")),
        migrations.AddConstraint(model_name="rating", constraint=models.CheckConstraint(condition=models.Q(("stars__gte", 1), ("stars__lte", 5)), name="rating_stars_1_5")),
    ]
