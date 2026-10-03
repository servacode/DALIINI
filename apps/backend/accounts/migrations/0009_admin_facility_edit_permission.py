from django.db import migrations

PERMISSIONS = {
    "admin.facilities.edit": "Add facilities to the directory and correct their details",
}
# Roles that may already suspend, reactivate or close a facility get the new grant with it, so
# nobody who runs facilities loses the ability to fix one. The full e2e role picks it up itself.
GRANT_ALONGSIDE = {
    "admin.facilities.edit": "admin.facilities.manage",
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
    dependencies = [("accounts", "0008_admin_smart_console_permissions")]
    operations = [migrations.RunPython(seed, migrations.RunPython.noop)]
