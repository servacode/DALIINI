"""The two rules every change to console roles keeps (DECISION-072).

**The owner role holds every permission.** `owner` is the platform's own role, created by
`migrate` and brought up to the full catalogue every time `migrate` runs, so a permission added
by a later release reaches the people running the platform without anybody editing the role.
The console cannot change or delete it; `grant_operator` is how the first person gets it.

**Somebody can always grant roles.** No change may leave the platform without an active
account that holds `admin.roles.manage`. Without one, the next operator can only be appointed
from a database shell, and a misclick in the role editor should never be what sends the team
there.
"""

from typing import Any

from core.exceptions import DomainError

from .models import AdminPermission, AdminRole, UserAdminRole

OWNER_ROLE_CODE = "owner"
OWNER_ROLE_NAME = "مدير المنصة"
ROLE_MANAGER_PERMISSION = "admin.roles.manage"


def sync_owner_role(**_: Any) -> AdminRole:
    """Create the owner role if it is missing and give it every permission there is.

    Connected to `post_migrate`, hence the keyword arguments it ignores.
    """
    role, _created = AdminRole.objects.get_or_create(
        code=OWNER_ROLE_CODE, defaults={"name": OWNER_ROLE_NAME}
    )
    role.permissions.set(AdminPermission.objects.all())
    return role


def role_managers_remain() -> bool:
    return UserAdminRole.objects.filter(
        active=True,
        user__is_active=True,
        role__permissions__code=ROLE_MANAGER_PERMISSION,
    ).exists()


def require_role_manager() -> None:
    """Refuse the change in hand when it would leave nobody able to grant roles.

    Called inside the change's transaction, after it is applied, so the check sees the result
    and the error rolls the change back.
    """
    if not role_managers_remain():
        raise DomainError(
            "LAST_ROLE_MANAGER",
            message="لا يمكن تنفيذ هذا التغيير: لن يبقى أحد يستطيع منح الأدوار.",
            status_code=409,
        )
