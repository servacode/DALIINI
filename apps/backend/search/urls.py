from django.urls import path

from .views import (
    PublicFacilityDetailView,
    PublicFacilityListView,
    PublicHomeView,
    PublicMapFacilitiesView,
    PublicSearchView,
)

urlpatterns = [
    path("public/home/", PublicHomeView.as_view()),
    path("public/facilities/", PublicFacilityListView.as_view()),
    path("public/facilities/<uuid:facility_id>/", PublicFacilityDetailView.as_view()),
    path("public/map/facilities/", PublicMapFacilitiesView.as_view()),
    path("public/search/", PublicSearchView.as_view()),
]
