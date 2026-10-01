from django.db import migrations

PERMISSIONS = {
    "admin.notifications.send": "Broadcast notifications to users and read broadcast history",
    "admin.content.read": "View content pages, FAQ, emergency numbers and contact messages",
    "admin.content.manage": "Edit content pages, FAQ, emergency numbers; handle contact messages",
    "admin.duty.read": "View the pharmacy duty roster and its gaps",
    "admin.duty.manage": "Create, change or cancel duty shifts on behalf of pharmacies",
}
# Existing roles that already hold the nearest grant get the new one, so no operator loses a
# capability they effectively had. The full e2e role picks every permission up by itself.
GRANT_ALONGSIDE = {
    "admin.notifications.send": "admin.settings.manage",
    "admin.content.read": "admin.ads.read",
    "admin.content.manage": "admin.ads.manage",
    "admin.duty.read": "admin.facilities.read",
    "admin.duty.manage": "admin.facilities.manage",
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
    dependencies = [("accounts", "0007_admin_reports_permissions")]
    operations = [migrations.RunPython(seed, migrations.RunPython.noop)]
