from django.contrib.auth.models import AnonymousUser
from rest_framework.exceptions import NotFound, PermissionDenied

from accounts.models import User
from facilities.models import Facility, FacilityMembership


def require_facility_manager(user: User | AnonymousUser | None, facility: Facility) -> None:
    if not user or not user.is_authenticated:
        raise PermissionDenied("Authentication required.")
    allowed = FacilityMembership.objects.filter(
        facility=facility,
        user=user,
        role__in=[FacilityMembership.Role.OWNER, FacilityMembership.Role.MANAGER],
    ).exists()
    if not allowed:
        # Not found rather than forbidden: a stranger must not learn from the answer that a
        # facility with this id exists, which is what the owner's own facility routes already do.
        raise NotFound()
