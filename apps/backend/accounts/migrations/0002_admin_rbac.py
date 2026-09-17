from django.db import migrations, models
import django.db.models.deletion


class Migration(migrations.Migration):
    dependencies = [("accounts", "0001_initial")]
    operations = [
        migrations.CreateModel(
            name="AdminPermission",
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
                ("code", models.CharField(max_length=120, unique=True)),
                ("description", models.CharField(blank=True, max_length=240)),
            ],
        ),
        migrations.CreateModel(
            name="AdminRole",
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
                ("code", models.CharField(max_length=80, unique=True)),
                ("name", models.CharField(max_length=120)),
                (
                    "permissions",
                    models.ManyToManyField(
                        blank=True,
                        related_name="roles",
                        to="accounts.adminpermission",
                    ),
                ),
            ],
        ),
        migrations.CreateModel(
            name="UserAdminRole",
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
                ("active", models.BooleanField(default=True)),
                ("created_at", models.DateTimeField(auto_now_add=True)),
                (
                    "role",
                    models.ForeignKey(
                        on_delete=django.db.models.deletion.CASCADE,
                        related_name="user_links",
                        to="accounts.adminrole",
                    ),
                ),
                (
                    "user",
                    models.ForeignKey(
                        on_delete=django.db.models.deletion.CASCADE,
                        related_name="admin_role_links",
                        to="accounts.user",
                    ),
                ),
            ],
        ),
        migrations.AddConstraint(
            model_name="useradminrole",
            constraint=models.UniqueConstraint(
                fields=("user", "role"),
                name="uniq_user_admin_role",
            ),
        ),
    ]
