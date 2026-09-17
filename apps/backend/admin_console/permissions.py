from rest_framework.permissions import BasePermission

from accounts.rbac import user_has_admin_permission


class HasAdminPermission(BasePermission):
    message = "Admin permission is required."

    def has_permission(self, request, view):
        code = getattr(view, "required_permission", "")
        return bool(code) and user_has_admin_permission(request.user, code)
