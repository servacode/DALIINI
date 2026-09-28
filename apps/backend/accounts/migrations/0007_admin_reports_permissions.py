from django.db import migrations

PERMISSIONS = {
    "admin.reports.read": "View facility problem reports",
    "admin.reports.manage": "Resolve or dismiss facility problem reports",
}
# Existing roles that can already see or act on facilities get the matching report grant,
# so no operator loses sight of reports about the facilities they manage.
GRANT_ALONGSIDE = {
    "admin.reports.read": "admin.facilities.read",
    "admin.reports.manage": "admin.facilities.manage",
}


def seed(apps, schema_editor):
    AdminPermission = apps.get_model("accounts", "AdminPermission")
    AdminRole = apps.get_model("accounts", "AdminRole")
    for code, description in PERMISSIONS.items():
        permission, _ = AdminPermission.objects.update_or_create(
            code=code, defaults={"description": description}
        )
        for role in AdminRole.objects.filter(permissions__code=GRANT_ALONGSIDE[code]):
            role.permissions.add(permission)


class Migration(migrations.Migration):
    dependencies = [("accounts", "0006_user_address_alter_otpchallenge_purpose")]
    operations = [migrations.RunPython(seed, migrations.RunPython.noop)]
