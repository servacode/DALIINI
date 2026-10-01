from django.urls import path

from .views import (
    PublicLocationResolveView,
    PublicProvinceCitiesView,
    PublicProvinceListView,
)

urlpatterns = [
    path("public/provinces/", PublicProvinceListView.as_view()),
    path("public/locations/resolve/", PublicLocationResolveView.as_view()),
    path(
        "public/provinces/<uuid:province_id>/cities/",
        PublicProvinceCitiesView.as_view(),
    ),
]
