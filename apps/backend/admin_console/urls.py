from django.urls import path

from . import views

urlpatterns = [
    path("admin/dashboard/", views.DashboardView.as_view()),
    path("admin/applications/", views.ApplicationListView.as_view()),
    path("admin/applications/<uuid:application_id>/", views.ApplicationDetailView.as_view()),
    path(
        "admin/applications/<uuid:application_id>/approve/",
        views.ApplicationApproveView.as_view(),
    ),
    path("admin/applications/<uuid:application_id>/reject/", views.ApplicationRejectView.as_view()),
    path("admin/evidence/<uuid:evidence_id>/content/", views.EvidenceContentView.as_view()),
    path("admin/facilities/", views.FacilityListView.as_view()),
    path("admin/facilities/<uuid:facility_id>/", views.FacilityDetailView.as_view()),
    path("admin/facilities/<uuid:facility_id>/suspend/", views.FacilitySuspendView.as_view()),
    path("admin/facilities/<uuid:facility_id>/reactivate/", views.FacilityReactivateView.as_view()),
    path("admin/facilities/<uuid:facility_id>/close/", views.FacilityCloseView.as_view()),
    path("admin/users/", views.UserListView.as_view()),
    path("admin/users/<uuid:user_id>/", views.UserDetailView.as_view()),
    path("admin/users/<uuid:user_id>/block/", views.UserBlockView.as_view()),
    path("admin/users/<uuid:user_id>/unblock/", views.UserUnblockView.as_view()),
    path("admin/roles/", views.RoleListView.as_view()),
    path("admin/users/<uuid:user_id>/roles/", views.UserRolesView.as_view()),
    path("admin/category-groups/", views.CategoryGroupListView.as_view()),
    path("admin/categories/", views.CategoryListView.as_view()),
    path(
        "admin/categories/<uuid:category_id>/capabilities/",
        views.CategoryCapabilitiesView.as_view(),
    ),
    path("admin/categories/<uuid:category_id>/provinces/", views.CategoryProvinceView.as_view()),
    path("admin/provinces/", views.ProvinceListView.as_view()),
    path("admin/provinces/<uuid:province_id>/", views.ProvinceDetailView.as_view()),
    path("admin/verification-requirements/", views.VerificationRequirementListView.as_view()),
    path("admin/ads/", views.AdvertisementListView.as_view()),
    path("admin/ads/<uuid:advertisement_id>/", views.AdvertisementDetailView.as_view()),
    path("admin/audit/", views.AuditListView.as_view()),
    path("admin/analytics/", views.AnalyticsView.as_view()),
    path("admin/settings/", views.SettingsView.as_view()),
    path("admin/system/status/", views.SystemStatusView.as_view()),
]
