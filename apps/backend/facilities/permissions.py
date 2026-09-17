from rest_framework.exceptions import PermissionDenied

from .models import FacilityMembership


def require_facility_member(user, facility):
    if not user or not user.is_authenticated:
        raise PermissionDenied("Authentication required.")
    membership = FacilityMembership.objects.filter(
        facility=facility,
        user=user,
        role__in=[FacilityMembership.Role.OWNER, FacilityMembership.Role.MANAGER],
    ).first()
    if membership is None:
        raise PermissionDenied("Facility membership required.")
    return membership


def require_facility_owner(user, facility):
    if not user or not user.is_authenticated:
        raise PermissionDenied("Authentication required.")
    membership = FacilityMembership.objects.filter(
        facility=facility,
        user=user,
        role=FacilityMembership.Role.OWNER,
    ).first()
    if membership is None:
        raise PermissionDenied("Facility owner permission required.")
    return membership
