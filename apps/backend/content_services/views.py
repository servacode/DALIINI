from drf_spectacular.utils import OpenApiParameter, extend_schema
from rest_framework.response import Response
from rest_framework.views import APIView

from .schemas import PublicAdvertisementListSerializer
from .selectors import active_ads
from .serializers import public_ad


class PublicAdsView(APIView):
    authentication_classes = []
    permission_classes = []

    @extend_schema(
        operation_id="publicAdsList",
        tags=["Ads"],
        summary="List advertisements currently in flight",
        description=(
            "First-party advertisements only, filtered server-side by schedule, enabled "
            "state and targeting scope. Capped at 50 items."
        ),
        parameters=[
            OpenApiParameter(
                "provinceId",
                str,
                OpenApiParameter.QUERY,
                required=False,
                description="Restrict to advertisements targeting this province.",
            ),
            OpenApiParameter(
                "categoryId",
                str,
                OpenApiParameter.QUERY,
                required=False,
                description="Restrict to advertisements targeting this category.",
            ),
        ],
        responses={200: PublicAdvertisementListSerializer},
    )
    def get(self, request):
        rows = active_ads(
            province_id=request.query_params.get("provinceId"),
            category_id=request.query_params.get("categoryId"),
        )[:50]
        return Response({"items": [public_ad(row) for row in rows]})
