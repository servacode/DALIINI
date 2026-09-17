from django.db import migrations


PERMISSIONS = {
    "admin.dashboard.read": "View operational dashboard",
    "admin.reviews.read": "View facility review queue and detail",
    "admin.reviews.decide": "Approve or reject facility applications",
    "admin.evidence.read": "View private verification evidence",
    "admin.facilities.read": "View facility operations",
    "admin.facilities.manage": "Suspend, reactivate or close facilities",
    "admin.users.read": "View user records",
    "admin.users.manage": "Block or unblock users",
    "admin.roles.read": "View Admin roles",
    "admin.roles.manage": "Assign Admin roles",
    "admin.taxonomy.read": "View taxonomy configuration",
    "admin.taxonomy.manage": "Manage taxonomy configuration",
    "admin.provinces.read": "View province rollout configuration",
    "admin.provinces.manage": "Manage province rollout configuration",
    "admin.verification.read": "View verification requirements",
    "admin.verification.manage": "Manage verification requirements",
    "admin.ads.read": "View advertisements",
    "admin.ads.manage": "Manage advertisements",
    "admin.audit.read": "View audit log",
    "admin.analytics.read": "View operational analytics",
    "admin.settings.read": "View platform settings",
    "admin.settings.manage": "Manage platform settings",
    "admin.system.read": "View privileged system status",
    "realtime.admin": "Subscribe to Admin realtime invalidations",
}


def seed_permissions(apps, schema_editor):
    permission = apps.get_model("accounts", "AdminPermission")
    for code, description in PERMISSIONS.items():
        permission.objects.update_or_create(
            code=code,
            defaults={"description": description},
        )


class Migration(migrations.Migration):
    dependencies = [("accounts", "0002_admin_rbac")]
    operations = [migrations.RunPython(seed_permissions, migrations.RunPython.noop)]
