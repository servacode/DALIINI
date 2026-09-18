"""The one place Admin authorization is decided.

Every check — the `HasAdminPermission` permission class, the `/admin/me/` endpoint, and
anything added later — resolves through here. A second resolver written inside a view is
how an endpoint ends up disagreeing with the permission class guarding it.

Authority comes from `AdminRole` and `AdminPermission` only. Django's `groups`,
`user_permissions` and `is_superuser` grant nothing: `is_superuser` in particular is a
Django admin-site concept, and treating it as an operator grant would mean a database
shell or a stray `createsuperuser` silently confers every Admin capability. See DECISION-009
and DEBT-003.
"""

from typing import Any

from django.db.models import QuerySet

from .models import UserAdminRole


def _active_links(user: Any) -> QuerySet[UserAdminRole]:
    if user is None or not getattr(user, "is_authenticated", False) or not user.is_active:
        return UserAdminRole.objects.none()
    return UserAdminRole.objects.filter(user=user, active=True)


def is_admin_operator(user: Any) -> bool:
    """True when the account holds at least one active Admin role.

    Holding a role with no permissions attached still makes someone an operator: the role
    exists, an administrator granted it, and the correct answer is an empty permission set
    rather than a refusal that looks like a broken account.
    """
    return _active_links(user).exists()


def admin_permissions_for(user: Any) -> list[str]:
    """Every permission code the account holds, deduplicated and sorted.

    Two roles carrying the same permission yield it once. Sorted so the wire order is
    stable and a diff of two operators is readable.
    """
    if user is None or not getattr(user, "is_authenticated", False) or not user.is_active:
        return []
    codes = UserAdminRole.objects.filter(user=user, active=True).values_list(
        "role__permissions__code", flat=True
    )
    return sorted({code for code in codes if code})


def user_has_admin_permission(user: Any, permission_code: str) -> bool:
    if user is None or not getattr(user, "is_authenticated", False) or not user.is_active:
        return False
    return UserAdminRole.objects.filter(
        user=user,
        active=True,
        role__permissions__code=permission_code,
    ).exists()
