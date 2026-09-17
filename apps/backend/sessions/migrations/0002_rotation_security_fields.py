from django.db import migrations, models


class Migration(migrations.Migration):
    dependencies = [("sessions", "0001_initial")]

    operations = [
        migrations.RemoveField(model_name="usersession", name="rotated_to_digest"),
        migrations.AddField(model_name="usersession", name="previous_refresh_digest", field=models.CharField(blank=True, max_length=128)),
        migrations.AddField(model_name="usersession", name="previous_valid_until", field=models.DateTimeField(blank=True, null=True)),
        migrations.AddField(model_name="usersession", name="platform", field=models.CharField(blank=True, max_length=32)),
        migrations.AddField(model_name="usersession", name="device_name", field=models.CharField(blank=True, max_length=120)),
        migrations.AddField(model_name="usersession", name="last_seen_at", field=models.DateTimeField(blank=True, null=True)),
        migrations.AddField(model_name="usersession", name="compromised_at", field=models.DateTimeField(blank=True, null=True)),
    ]
