from django.urls import path

from .views import (
    FacilityHoursView,
    TemporaryClosureDeleteView,
    TemporaryClosureListCreateView,
)

urlpatterns = [
    path(
        "owner/facilities/<uuid:facility_id>/hours/",
        FacilityHoursView.as_view(),
    ),
    path(
        "owner/facilities/<uuid:facility_id>/temporary-closures/",
        TemporaryClosureListCreateView.as_view(),
    ),
    path(
        "owner/facilities/<uuid:facility_id>/temporary-closures/"
        "<uuid:closure_id>/",
        TemporaryClosureDeleteView.as_view(),
    ),
]
