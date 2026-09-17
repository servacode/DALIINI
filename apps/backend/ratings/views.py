from django.shortcuts import get_object_or_404
from rest_framework.exceptions import NotAuthenticated, ValidationError
from rest_framework.response import Response
from rest_framework.views import APIView

from facilities.models import Facility

from .models import Rating
from .serializers import RatingWriteSerializer


def _require_user(request):
    if not request.user or not request.user.is_authenticated:
        raise NotAuthenticated()
    return request.user


def _public_facility(facility_id):
    return get_object_or_404(
        Facility.objects.filter(
            pk=facility_id,
            status=Facility.Status.ACTIVE,
            province__active=True,
            category__active=True,
            category__province_switches__province_id=models_f("province_id"),
            category__province_switches__public_enabled=True,
        ).distinct()
    )


def models_f(name):
    from django.db.models import F

    return F(name)


class FacilityRatingView(APIView):
    def put(self, request, facility_id):
        user = _require_user(request)
        facility = _public_facility(facility_id)
        if not facility.category.capabilities.supports_ratings:
            raise ValidationError({"rating": "Ratings are not enabled for this category."})
        serializer = RatingWriteSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        rating, _ = Rating.objects.update_or_create(
            user=user,
            facility=facility,
            defaults={"stars": serializer.validated_data["stars"]},
        )
        return Response(
            {
                "facilityId": str(facility.id),
                "stars": rating.stars,
            }
        )

    def delete(self, request, facility_id):
        user = _require_user(request)
        Rating.objects.filter(user=user, facility_id=facility_id).delete()
        return Response(status=204)


class AccountRatingsView(APIView):
    def get(self, request):
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
