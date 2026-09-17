from django.urls import path

from .views import (
    OwnerConfigView,
    OwnerFacilityDetailView,
    OwnerFacilityEvidenceDeleteView,
    OwnerFacilityEvidenceView,
    OwnerFacilityImageDeleteView,
    OwnerFacilityImagesView,
    OwnerFacilityListCreateView,
    OwnerFacilityLocationView,
    OwnerFacilityMemberDeleteView,
    OwnerFacilityMembersView,
    OwnerFacilitySubmitView,
)

urlpatterns = [
    path("owner/config/", OwnerConfigView.as_view()),
    path("owner/facilities/", OwnerFacilityListCreateView.as_view()),
    path("owner/facilities/<uuid:facility_id>/", OwnerFacilityDetailView.as_view()),
    path(
        "owner/facilities/<uuid:facility_id>/submit/",
        OwnerFacilitySubmitView.as_view(),
    ),
    path(
        "owner/facilities/<uuid:facility_id>/location/",
        OwnerFacilityLocationView.as_view(),
    ),
    path(
        "owner/facilities/<uuid:facility_id>/images/",
        OwnerFacilityImagesView.as_view(),
    ),
    path(
        "owner/facilities/<uuid:facility_id>/images/<uuid:image_id>/",
        OwnerFacilityImageDeleteView.as_view(),
    ),
    path(
        "owner/facilities/<uuid:facility_id>/evidence/",
        OwnerFacilityEvidenceView.as_view(),
    ),
    path(
        "owner/facilities/<uuid:facility_id>/evidence/<uuid:evidence_id>/",
        OwnerFacilityEvidenceDeleteView.as_view(),
    ),
    path(
        "owner/facilities/<uuid:facility_id>/members/",
        OwnerFacilityMembersView.as_view(),
    ),
    path(
        "owner/facilities/<uuid:facility_id>/members/<uuid:user_id>/",
        OwnerFacilityMemberDeleteView.as_view(),
    ),
]
