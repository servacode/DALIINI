from rest_framework.exceptions import PermissionDenied

from facilities.models import FacilityMembership


def require_facility_manager(user, facility):
    if not user or not user.is_authenticated:
        raise PermissionDenied("Authentication required.")
    allowed = FacilityMembership.objects.filter(
        facility=facility,
        user=user,
        role__in=[FacilityMembership.Role.OWNER, FacilityMembership.Role.MANAGER],
    ).exists()
    if not allowed:
        raise PermissionDenied("Facility membership required.")
