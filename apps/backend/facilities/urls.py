from django.urls import path

from .hours_confirmation import OwnerFacilityConfirmHoursView
from .insights import OwnerFacilityInsightsView
from .reports import PublicFacilityReportView
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
from .views_claims import (
    ClaimableFacilitiesView,
    ClaimDetailView,
    ClaimEvidenceDeleteView,
    ClaimEvidenceView,
    ClaimListCreateView,
    ClaimSubmitView,
)
from .views_invitations import (
    AccountInvitationAcceptView,
    AccountInvitationDeclineView,
    AccountInvitationsView,
    OwnerFacilityInvitationRevokeView,
    OwnerFacilityInvitationsView,
)

urlpatterns = [
    path("owner/claimable-facilities/", ClaimableFacilitiesView.as_view()),
    path("owner/claims/", ClaimListCreateView.as_view()),
    path("owner/claims/<uuid:claim_id>/", ClaimDetailView.as_view()),
    path("owner/claims/<uuid:claim_id>/submit/", ClaimSubmitView.as_view()),
    path("owner/claims/<uuid:claim_id>/evidence/", ClaimEvidenceView.as_view()),
    path(
        "owner/claims/<uuid:claim_id>/evidence/<uuid:evidence_id>/",
        ClaimEvidenceDeleteView.as_view(),
    ),
    path("account/invitations/", AccountInvitationsView.as_view()),
    path(
        "account/invitations/<uuid:invitation_id>/accept/",
        AccountInvitationAcceptView.as_view(),
    ),
    path(
        "account/invitations/<uuid:invitation_id>/decline/",
        AccountInvitationDeclineView.as_view(),
    ),
    path(
        "owner/facilities/<uuid:facility_id>/invitations/",
        OwnerFacilityInvitationsView.as_view(),
    ),
    path(
        "owner/facilities/<uuid:facility_id>/invitations/<uuid:invitation_id>/",
        OwnerFacilityInvitationRevokeView.as_view(),
    ),
    path("facilities/<uuid:facility_id>/reports/", PublicFacilityReportView.as_view()),
    path("owner/config/", OwnerConfigView.as_view()),
    path(
        "owner/facilities/<uuid:facility_id>/insights/",
        OwnerFacilityInsightsView.as_view(),
    ),
    path(
        "owner/facilities/<uuid:facility_id>/confirm-hours/",
        OwnerFacilityConfirmHoursView.as_view(),
    ),
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
