"""Taking a published facility down and putting it back: suspend, reactivate, close."""

from uuid import UUID

from django.core.exceptions import ObjectDoesNotExist
from django.core.exceptions import ValidationError as DjangoValidationError
from drf_spectacular.utils import (
    extend_schema,
    extend_schema_view,
)
from rest_framework.exceptions import NotFound
from rest_framework.response import Response

from accounts.authentication import AuthenticatedRequest
from core.openapi import NOT_FOUND_404, VALIDATION_400, protected
from facilities.models import Facility

from .schemas import (
    AdminDecisionRequestSerializer,
    AdminFacilitySerializer,
)
from .serializers import (
    facility_payload,
)
from .services import (
    transition_facility,
)
from .views import (
    AdminView,
    _validation_error,
)


class FacilityTransitionView(AdminView):
    required_permission = "admin.facilities.manage"
    target_status = ""

    @extend_schema(
        operation_id="adminFacilityTransition",
        tags=["Admin Facilities"],
        summary="Move a facility to a lifecycle state",
        request=AdminDecisionRequestSerializer,
        responses={
            200: AdminFacilitySerializer,
            400: VALIDATION_400,
            **protected(),
            404: NOT_FOUND_404,
        },
    )
    def post(self, request: AuthenticatedRequest, facility_id: UUID) -> Response:
        try:
            facility = transition_facility(
                request=request,
                facility_id=facility_id,
                target_status=self.target_status,
                reason=str(request.data.get("reason", "")),
            )
        except ObjectDoesNotExist as exc:
            # The service locks the row itself; an id that does not exist is the
            # caller's 404, not a server fault.
            raise NotFound() from exc
        except DjangoValidationError as exc:
            raise _validation_error(exc) from exc
        return Response(facility_payload(facility))


@extend_schema_view(
    post=extend_schema(
        operation_id="adminFacilitySuspend",
        tags=["Admin Facilities"],
        summary="Suspend a facility",
        description="A suspended facility leaves public discovery and cannot self-reactivate.",
        request=AdminDecisionRequestSerializer,
        responses={
            200: AdminFacilitySerializer,
            400: VALIDATION_400,
            **protected(),
            404: NOT_FOUND_404,
        },
    )
)
class FacilitySuspendView(FacilityTransitionView):
    target_status = Facility.Status.SUSPENDED


@extend_schema_view(
    post=extend_schema(
        operation_id="adminFacilityReactivate",
        tags=["Admin Facilities"],
        summary="Reactivate a suspended facility",
        request=AdminDecisionRequestSerializer,
        responses={
            200: AdminFacilitySerializer,
            400: VALIDATION_400,
            **protected(),
            404: NOT_FOUND_404,
        },
    )
)
class FacilityReactivateView(FacilityTransitionView):
    target_status = Facility.Status.ACTIVE


@extend_schema_view(
    post=extend_schema(
        operation_id="adminFacilityClose",
        tags=["Admin Facilities"],
        summary="Close a facility",
        request=AdminDecisionRequestSerializer,
        responses={
            200: AdminFacilitySerializer,
            400: VALIDATION_400,
            **protected(),
            404: NOT_FOUND_404,
        },
    )
)
class FacilityCloseView(FacilityTransitionView):
    target_status = Facility.Status.CLOSED
