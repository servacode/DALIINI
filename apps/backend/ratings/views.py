from uuid import UUID

from django.db import IntegrityError, transaction
from django.shortcuts import get_object_or_404
from drf_spectacular.utils import extend_schema
from rest_framework.exceptions import NotAuthenticated, ValidationError
from rest_framework.permissions import IsAuthenticated
from rest_framework.request import Request
from rest_framework.response import Response
from rest_framework.views import APIView

from accounts.authentication import AuthenticatedRequest
from accounts.models import User
from core.openapi import NOT_FOUND_404, VALIDATION_400, protected
from core.throttles import RatingsWriteThrottle
from facilities.models import Facility
from search.selectors import public_facilities

from .models import Rating
from .schemas import AccountRatingListSerializer, FacilityRatingSerializer
from .serializers import RatingWriteSerializer


def _require_user(request: Request) -> User:
    if not request.user or not request.user.is_authenticated:
        raise NotAuthenticated()
    return request.user


def _public_facility(facility_id: UUID) -> Facility:
    """The facility as the public sees it; anything not publicly visible is a 404."""
    return get_object_or_404(public_facilities().filter(pk=facility_id))


def _ratings_enabled(facility: Facility) -> bool:
    capabilities = getattr(facility.category, "capabilities", None)
    return bool(capabilities and capabilities.supports_ratings)


class FacilityRatingView(APIView):
    throttle_classes = [RatingsWriteThrottle]
    permission_classes = [IsAuthenticated]

    @extend_schema(
        operation_id="facilityRatingUpsert",
        tags=["Ratings"],
        summary="Create or replace the caller's rating for a facility",
        description=(
            "One rating per user per facility, so repeating the call replaces the previous "
            "value. Only categories that declare the ratings capability accept this."
        ),
        request=RatingWriteSerializer,
        responses={
            200: FacilityRatingSerializer,
            400: VALIDATION_400,
            **protected(),
            404: NOT_FOUND_404,
        },
    )
    def put(self, request: AuthenticatedRequest, facility_id: UUID) -> Response:
        user = _require_user(request)
        facility = _public_facility(facility_id)
        if not _ratings_enabled(facility):
            raise ValidationError({"rating": "Ratings are not enabled for this category."})
        serializer = RatingWriteSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        stars = serializer.validated_data["stars"]
        with transaction.atomic():
            # Lock the caller's row (if any) so concurrent upserts serialise; the unique
            # constraint turns a lost create race into an update of the winner's row.
            rating = (
                Rating.objects.select_for_update().filter(user=user, facility=facility).first()
            )
            if rating is None:
                try:
                    with transaction.atomic():
                        rating = Rating.objects.create(user=user, facility=facility, stars=stars)
                except IntegrityError:
                    rating = Rating.objects.select_for_update().get(user=user, facility=facility)
            if rating.stars != stars:
                rating.stars = stars
                rating.save(update_fields=["stars", "updated_at"])
        return Response(
            {
                "facilityId": str(facility.id),
                "stars": rating.stars,
            }
        )

    @extend_schema(
        operation_id="facilityRatingDelete",
        tags=["Ratings"],
        summary="Remove the caller's rating for a facility",
        description=(
            "Idempotent for a publicly visible facility; a facility that is not publicly "
            "visible answers 404, exactly as the write does."
        ),
        responses={204: None, **protected(), 404: NOT_FOUND_404},
    )
    def delete(self, request: AuthenticatedRequest, facility_id: UUID) -> Response:
        user = _require_user(request)
        facility = _public_facility(facility_id)
        Rating.objects.filter(user=user, facility=facility).delete()
        return Response(status=204)


class AccountRatingsView(APIView):
    permission_classes = [IsAuthenticated]

    @extend_schema(
        operation_id="accountRatingsList",
        tags=["Account"],
        summary="List the ratings written by the caller",
        responses={200: AccountRatingListSerializer, **protected()},
    )
    def get(self, request: AuthenticatedRequest) -> Response:
        user = _require_user(request)
        ratings = Rating.objects.filter(user=user).select_related("facility").order_by(
            "-updated_at"
        )
        return Response(
            {
                "items": [
                    {
                        "facilityId": str(rating.facility_id),
                        "facilityNameAr": rating.facility.name_ar,
                        "stars": rating.stars,
                        "updatedAt": rating.updated_at.isoformat(),
                    }
                    for rating in ratings
                ]
            }
        )
