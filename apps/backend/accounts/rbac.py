from .models import UserAdminRole


def user_has_admin_permission(user, permission_code: str) -> bool:
    if user is None or not user.is_active:
        return False
    return UserAdminRole.objects.filter(
        user=user,
        active=True,
        role__permissions__code=permission_code,
    ).exists()
