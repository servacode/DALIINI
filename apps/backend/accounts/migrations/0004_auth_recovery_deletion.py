import uuid

from django.db import migrations, models
import django.db.models.deletion


class Migration(migrations.Migration):
    dependencies = [
        ("accounts", "0003_admin_permission_catalog"),
    ]

    operations = [
        migrations.AddField(
            model_name="user",
            name="phone_verified_at",
            field=models.DateTimeField(blank=True, null=True),
        ),
        migrations.AddField(
            model_name="user",
            name="profile_image_key",
            field=models.CharField(blank=True, max_length=500),
        ),
        migrations.CreateModel(
            name="OTPChallenge",
            fields=[
                ("id", models.UUIDField(default=uuid.uuid4, editable=False, primary_key=True, serialize=False)),
                ("phone", models.CharField(db_index=True, max_length=16)),
                ("purpose", models.CharField(choices=[("REGISTER", "Register"), ("RECOVERY", "Recovery")], max_length=20)),
                ("otp_digest", models.CharField(max_length=128)),
                ("attempt_count", models.PositiveSmallIntegerField(default=0)),
                ("max_attempts", models.PositiveSmallIntegerField(default=5)),
                ("expires_at", models.DateTimeField()),
                ("verified_at", models.DateTimeField(blank=True, null=True)),
                ("consumed_at", models.DateTimeField(blank=True, null=True)),
                ("metadata", models.JSONField(blank=True, default=dict)),
                ("created_at", models.DateTimeField(auto_now_add=True)),
            ],
        ),
        migrations.CreateModel(
            name="AccountDeletionRequest",
            fields=[
                ("id", models.UUIDField(default=uuid.uuid4, editable=False, primary_key=True, serialize=False)),
                ("identity_digest", models.CharField(db_index=True, max_length=128)),
                ("channel", models.CharField(choices=[("IN_APP", "In app"), ("WEB", "Web")], max_length=16)),
                ("status", models.CharField(choices=[("REQUESTED", "Requested"), ("COMPLETED", "Completed"), ("REJECTED", "Rejected")], default="REQUESTED", max_length=20)),
                ("requested_at", models.DateTimeField(auto_now_add=True)),
                ("verified_at", models.DateTimeField(blank=True, null=True)),
                ("completed_at", models.DateTimeField(blank=True, null=True)),
                ("retention_notes", models.TextField(blank=True)),
                ("user", models.ForeignKey(blank=True, null=True, on_delete=django.db.models.deletion.SET_NULL, to="accounts.user")),
            ],
        ),
    ]
