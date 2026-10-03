"""First-party advertisements: content, targeting and schedule."""

from uuid import UUID

from django.core.exceptions import ValidationError as DjangoValidationError
from django.shortcuts import get_object_or_404
from drf_spectacular.utils import (
    extend_schema,
)
from rest_framework.response import Response

from accounts.authentication import AuthenticatedRequest
from content_services.models import Advertisement
from content_services.services import (
    apply_fields as apply_advertisement_fields,
)
from content_services.services import (
    delete_advertisement,
    save_advertisement,
    update_advertisement,
)
from core.openapi import NOT_FOUND_404, VALIDATION_400, protected

from .permissions import HasAdminPermission
from .schemas import (
    AdminAdvertisementListSerializer,
    AdminAdvertisementRequestSerializer,
    AdminAdvertisementSerializer,
    AdminAdvertisementUpdateRequestSerializer,
    AdminIdSerializer,
)
from .views import (
    AdminView,
    _validation_error,
)


class AdvertisementListView(AdminView):
    required_permission = "admin.ads.read"

    @extend_schema(
        operation_id="adminAdsList",
        tags=["Admin Ads"],
        summary="List advertisements",
        responses={200: AdminAdvertisementListSerializer, **protected()},
    )
    def get(self, request: AuthenticatedRequest) -> Response:
        return Response(
            {
                "items": AdminAdvertisementSerializer(
                    Advertisement.objects.order_by("sort_order", "-updated_at").values(
                        "id",
                        "title_ar",
                        "target_scope",
                        "province_id",
                        "category_id",
                        "image_key",
                        "enabled",
                        "starts_at",
                        "ends_at",
                        "sort_order",
                        "slide_duration_ms",
                    ),
                    many=True,
                ).data
            }
        )

    @extend_schema(
        operation_id="adminAdCreate",
        tags=["Admin Ads"],
        summary="Create an advertisement",
        description=(
            "Requires the manage permission, which is re-checked inside the handler. "
            "Action payloads are validated per action type."
        ),
        request=AdminAdvertisementRequestSerializer,
        responses={201: AdminIdSerializer, 400: VALIDATION_400, **protected()},
    )
    def post(self, request: AuthenticatedRequest) -> Response:
        self.required_permission = "admin.ads.manage"
        if not HasAdminPermission().has_permission(request, self):
            self.permission_denied(request)
        payload = AdminAdvertisementRequestSerializer(data=request.data)
        payload.is_valid(raise_exception=True)
        # One field mapping, shared with the update path, so the two cannot drift.
        ad = apply_advertisement_fields(Advertisement(), request.data)
        try:
            save_advertisement(actor=request.user, advertisement=ad)
        except DjangoValidationError as exc:
            raise _validation_error(exc) from exc
        return Response({"id": str(ad.pk)}, status=201)


class AdvertisementDetailView(AdminView):
    required_permission = "admin.ads.manage"

    @extend_schema(
        operation_id="adminAdUpdate",
        tags=["Admin Ads"],
        summary="Edit an advertisement, its schedule or its activation",
        description=(
            "Omitted fields keep their current value. Schedule, targeting and action "
            "payload are validated together, so an end before its start or a global "
            "advertisement carrying a target is refused."
        ),
        request=AdminAdvertisementUpdateRequestSerializer,
        responses={
            200: AdminIdSerializer,
            400: VALIDATION_400,
            **protected(),
            404: NOT_FOUND_404,
        },
    )
    def put(self, request: AuthenticatedRequest, advertisement_id: UUID) -> Response:
        payload = AdminAdvertisementUpdateRequestSerializer(data=request.data, partial=True)
        payload.is_valid(raise_exception=True)
        ad = get_object_or_404(Advertisement, pk=advertisement_id)
        try:
            update_advertisement(actor=request.user, advertisement=ad, data=request.data)
        except DjangoValidationError as exc:
            raise _validation_error(exc) from exc
        return Response({"id": str(ad.pk)})

    @extend_schema(
        operation_id="adminAdDelete",
        tags=["Admin Ads"],
        summary="Delete an advertisement",
        responses={204: None, **protected(), 404: NOT_FOUND_404},
    )
    def delete(self, request: AuthenticatedRequest, advertisement_id: UUID) -> Response:
        ad = get_object_or_404(Advertisement, pk=advertisement_id)
        delete_advertisement(actor=request.user, advertisement=ad)
        return Response(status=204)
