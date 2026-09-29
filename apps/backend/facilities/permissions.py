from django.contrib.auth.models import AnonymousUser
from rest_framework.exceptions import PermissionDenied

from accounts.models import User

from .models import Facility, FacilityMembership


def require_facility_member(
    user: User | AnonymousUser | None, facility: Facility
) -> FacilityMembership:
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


def require_facility_owner(
    user: User | AnonymousUser | None, facility: Facility
) -> FacilityMembership:
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
