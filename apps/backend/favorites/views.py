"""Saving a facility, and reading back what an account has saved.

Saving is idempotent in both directions: saving twice is still saved once, and removing
something that was never saved is still not saved. That keeps the client honest without a
round trip to check first, and it is what lets a heart on a card be tapped twice quickly
without producing an error the user did not cause.
"""

from django.shortcuts import get_object_or_404
from drf_spectacular.utils import extend_schema
from rest_framework.permissions import IsAuthenticated
from rest_framework.request import Request
from rest_framework.response import Response
from rest_framework.views import APIView

from core.openapi import NOT_FOUND_404, VALIDATION_400, protected
from core.throttles import FavoritesWriteThrottle
from search.pagination import FacilityCursorPagination
from search.selectors import public_facilities, with_rating_summary
from search.serializers import compact_facility
from search.views import PAGE_PARAMS

from .models import Favorite
from .schemas import (
    FavoriteListSerializer,
    FavoriteStateSerializer,
    FavoriteWriteSerializer,
)


class FavoriteCursorPagination(FacilityCursorPagination):
    """Saved facilities, newest first, with the row id keeping the ordering total."""

    ordering = ("-created_at", "id")


class AccountFavoritesView(APIView):
    throttle_classes = [FavoritesWriteThrottle]
    permission_classes = [IsAuthenticated]

    @extend_schema(
        operation_id="accountFavoritesList",
        tags=["Account"],
        summary="List the facilities the caller has saved",
        description=(
            "Newest first, cursor-paginated. A saved facility that is no longer public — "
            "closed, suspended, or in a category the province stopped serving — is not "
            "returned, because this list is served by the same public query every other "
            "list uses."
        ),
        parameters=PAGE_PARAMS,
        responses={200: FavoriteListSerializer, **protected()},
    )
    def get(self, request: Request) -> Response:
        saved = (
            Favorite.objects.filter(user=request.user.pk, facility__in=public_facilities())
            .select_related("facility", "facility__category", "facility__city")
            .order_by("-created_at", "id")
        )
        paginator = FavoriteCursorPagination()
        page = paginator.paginate_queryset(saved, request, view=self) or []
        facilities = with_rating_summary(
            public_facilities().filter(pk__in=[row.facility_id for row in page])
        )
        by_id = {facility.id: facility for facility in facilities}
        items = []
        for row in page:
            facility = by_id.get(row.facility_id)
            if facility is None:
                continue
            items.append({**compact_facility(facility), "favoritedAt": row.created_at.isoformat()})
        return paginator.get_paginated_response(items)

    @extend_schema(
        operation_id="accountFavoriteAdd",
        tags=["Account"],
        summary="Save a facility",
        description="Idempotent: saving a facility that is already saved changes nothing.",
        request=FavoriteWriteSerializer,
        responses={
            200: FavoriteStateSerializer,
            400: VALIDATION_400,
            **protected(),
            404: NOT_FOUND_404,
        },
    )
    def post(self, request: Request) -> Response:
        body = FavoriteWriteSerializer(data=request.data)
        body.is_valid(raise_exception=True)
        facility = get_object_or_404(public_facilities(), pk=body.validated_data["facilityId"])
        Favorite.objects.get_or_create(user=request.user, facility=facility)  # type: ignore[misc]
        return Response({"facilityId": str(facility.id), "isFavorite": True})


class AccountFavoriteDetailView(APIView):
    throttle_classes = [FavoritesWriteThrottle]
    permission_classes = [IsAuthenticated]

    @extend_schema(
        operation_id="accountFavoriteRemove",
        tags=["Account"],
        summary="Remove a facility the caller had saved",
        description="Idempotent: removing what was not saved is not an error.",
        responses={200: FavoriteStateSerializer, **protected()},
    )
    def delete(self, request: Request, facility_id: str) -> Response:
        Favorite.objects.filter(user=request.user.pk, facility_id=facility_id).delete()
        return Response({"facilityId": str(facility_id), "isFavorite": False})
