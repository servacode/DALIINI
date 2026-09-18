from typing import Any

from rest_framework.permissions import BasePermission

from accounts.rbac import is_admin_operator, user_has_admin_permission


class HasAdminPermission(BasePermission):
    message = "Admin permission is required."

    def has_permission(self, request: Any, view: Any) -> bool:
        code = getattr(view, "required_permission", "")
        return bool(code) and user_has_admin_permission(request.user, code)


class IsAdminOperator(BasePermission):
    """Holds an active Admin role, whatever permissions that role carries.

    Used only by `/admin/me/`, which has to answer "what may I do" before the caller knows
    what they may do. Requiring a specific permission there would be circular. It is not a
    substitute for `HasAdminPermission` anywhere else: it says the caller is an operator,
    not that they may perform anything.
    """

    message = "An active Admin role is required."

    def has_permission(self, request: Any, view: Any) -> bool:
        return is_admin_operator(request.user)
