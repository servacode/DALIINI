from rest_framework.response import Response
from rest_framework.views import APIView

from .selectors import active_ads
from .serializers import public_ad


class PublicAdsView(APIView):
    authentication_classes = []
    permission_classes = []

    def get(self, request):
        rows = active_ads(
            province_id=request.query_params.get("provinceId"),
            category_id=request.query_params.get("categoryId"),
        )[:50]
        return Response({"items": [public_ad(row) for row in rows]})
