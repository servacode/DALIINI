from django.urls import path

from accounts.views_mfa import AdminUserMfaResetView

from . import (
    exports,
    views,
    views_ad_stats,
    views_ads,
    views_analytics,
    views_content,
    views_duty,
    views_duty_import,
    views_facilities,
    views_facility_lifecycle,
    views_provinces,
    views_reports,
    views_reviews,
    views_roles,
    views_smart,
    views_tags,
    views_taxonomy,
    views_users,
)

urlpatterns = [
    path("admin/me/", views.AdminMeView.as_view()),
    path("admin/dashboard/", views.DashboardView.as_view()),
    path("admin/tasks/", views_smart.TasksView.as_view()),
    path("admin/alerts/", views_smart.AlertsView.as_view()),
    path("admin/search/", views_smart.AdminSearchView.as_view()),
    path(
        "admin/facilities/<uuid:facility_id>/timeline/",
        views_smart.FacilityTimelineView.as_view(),
    ),
    path("admin/reports/bulk/", views_smart.ReportBulkDecisionView.as_view()),
    path("admin/rejection-templates/", views_smart.RejectionTemplateListView.as_view()),
    path(
        "admin/rejection-templates/<uuid:template_id>/",
        views_smart.RejectionTemplateDetailView.as_view(),
    ),
    path("admin/notifications/broadcast/", views_smart.BroadcastSendView.as_view()),
    path("admin/app-release/", views_smart.AppReleaseView.as_view()),
    path("admin/notifications/broadcasts/", views_smart.BroadcastHistoryView.as_view()),
    path(
        "admin/provinces/<uuid:province_id>/readiness/",
        views_smart.ProvinceReadinessView.as_view(),
    ),
    path("admin/analytics/staff/", views_smart.StaffAnalyticsView.as_view()),
    path("admin/ads/images/", views_smart.AdvertisementImageUploadView.as_view()),
    path("admin/exports/facilities.csv", exports.FacilitiesCsvView.as_view()),
    path("admin/exports/reports.csv", exports.ReportsCsvView.as_view()),
    path("admin/exports/audit.csv", exports.AuditCsvView.as_view()),
    path("admin/content/pages/", views_content.ContentPageListView.as_view()),
    path("admin/content/pages/<str:slug>/", views_content.ContentPageDetailView.as_view()),
    path("admin/content/faq/", views_content.FaqEntryListView.as_view()),
    path("admin/content/faq/<uuid:entry_id>/", views_content.FaqEntryDetailView.as_view()),
    path("admin/emergency-numbers/", views_content.EmergencyNumberListView.as_view()),
    path(
        "admin/emergency-numbers/<uuid:number_id>/",
        views_content.EmergencyNumberDetailView.as_view(),
    ),
    path("admin/contact-messages/", views_content.ContactMessageListView.as_view()),
    path(
        "admin/contact-messages/<uuid:message_id>/handle/",
        views_content.ContactMessageHandleView.as_view(),
    ),
    path("admin/duty/", views_duty.DutyRosterView.as_view()),
    path("admin/duty/import/", views_duty_import.DutyImportView.as_view()),
    path("admin/duty/rotations/", views_duty_import.DutyRotationListView.as_view()),
    path(
        "admin/duty/rotations/<uuid:rotation_id>/",
        views_duty_import.DutyRotationDetailView.as_view(),
    ),
    path(
        "admin/duty/rotations/<uuid:rotation_id>/generate/",
        views_duty_import.DutyRotationGenerateView.as_view(),
    ),
    path("admin/duty/<uuid:shift_id>/", views_duty.DutyShiftDetailView.as_view()),
    path("admin/applications/", views_reviews.ApplicationListView.as_view()),
    path(
        "admin/applications/<uuid:application_id>/", views_reviews.ApplicationDetailView.as_view()
    ),
    path(
        "admin/applications/<uuid:application_id>/approve/",
        views_reviews.ApplicationApproveView.as_view(),
    ),
    path(
        "admin/applications/<uuid:application_id>/reject/",
        views_reviews.ApplicationRejectView.as_view(),
    ),
    path("admin/evidence/<uuid:evidence_id>/content/", views_reviews.EvidenceContentView.as_view()),
    path("admin/facilities/", views_facilities.FacilityListView.as_view()),
    path("admin/facilities/map/", views_facilities.FacilityMapView.as_view()),
    path("admin/facilities/<uuid:facility_id>/", views_facilities.FacilityDetailView.as_view()),
    path(
        "admin/facilities/<uuid:facility_id>/suspend/",
        views_facility_lifecycle.FacilitySuspendView.as_view(),
    ),
    path(
        "admin/facilities/<uuid:facility_id>/reactivate/",
        views_facility_lifecycle.FacilityReactivateView.as_view(),
    ),
    path(
        "admin/facilities/<uuid:facility_id>/close/",
        views_facility_lifecycle.FacilityCloseView.as_view(),
    ),
    path("admin/users/", views_users.UserListView.as_view()),
    path("admin/users/<uuid:user_id>/", views_users.UserDetailView.as_view()),
    path("admin/users/<uuid:user_id>/block/", views_users.UserBlockView.as_view()),
    path("admin/users/<uuid:user_id>/unblock/", views_users.UserUnblockView.as_view()),
    path("admin/users/<uuid:user_id>/mfa/reset/", AdminUserMfaResetView.as_view()),
    path("admin/users/<uuid:user_id>/recovery/", views_users.UserRecoveryView.as_view()),
    path(
        "admin/users/<uuid:user_id>/sessions/revoke/",
        views_users.UserSessionsRevokeView.as_view(),
    ),
    path("admin/permissions/", views_roles.PermissionListView.as_view()),
    path("admin/roles/", views_roles.RoleListView.as_view()),
    path("admin/roles/<int:role_id>/", views_roles.RoleDetailView.as_view()),
    path("admin/users/<uuid:user_id>/roles/", views_users.UserRolesView.as_view()),
    path("admin/category-groups/", views_taxonomy.CategoryGroupListView.as_view()),
    path("admin/category-groups/create/", views_taxonomy.CategoryGroupCreateView.as_view()),
    path(
        "admin/category-groups/<uuid:group_id>/", views_taxonomy.CategoryGroupDetailView.as_view()
    ),
    path("admin/categories/", views_taxonomy.CategoryListView.as_view()),
    path("admin/categories/create/", views_taxonomy.CategoryCreateView.as_view()),
    path("admin/categories/<uuid:category_id>/", views_taxonomy.CategoryDetailView.as_view()),
    path(
        "admin/categories/<uuid:category_id>/capabilities/",
        views_taxonomy.CategoryCapabilitiesView.as_view(),
    ),
    path(
        "admin/categories/<uuid:category_id>/provinces/",
        views_taxonomy.CategoryProvinceView.as_view(),
    ),
    path(
        "admin/categories/<uuid:category_id>/specialties/",
        views_tags.CategorySpecialtiesView.as_view(),
    ),
    path(
        "admin/categories/<uuid:category_id>/service-tags/",
        views_tags.CategoryServiceTagsView.as_view(),
    ),
    path("admin/specialties/<int:specialty_id>/", views_tags.SpecialtyDetailView.as_view()),
    path(
        "admin/service-tags/<int:service_tag_id>/",
        views_tags.ServiceTagDetailView.as_view(),
    ),
    path("admin/provinces/", views_provinces.ProvinceListView.as_view()),
    path("admin/provinces/<uuid:province_id>/", views_provinces.ProvinceDetailView.as_view()),
    path(
        "admin/provinces/<uuid:province_id>/cities/", views_provinces.ProvinceCityListView.as_view()
    ),
    path(
        "admin/provinces/<uuid:province_id>/cities/<uuid:city_id>/",
        views_provinces.ProvinceCityDetailView.as_view(),
    ),
    path("admin/reports/", views_reports.ReportListView.as_view()),
    path("admin/reports/<uuid:report_id>/resolve/", views_reports.ReportDecisionView.as_view()),
    path("admin/reports/<uuid:report_id>/dismiss/", views_reports.ReportDismissView.as_view()),
    path(
        "admin/verification-requirements/", views_taxonomy.VerificationRequirementListView.as_view()
    ),
    path(
        "admin/verification-requirements/<int:requirement_id>/",
        views_taxonomy.VerificationRequirementDetailView.as_view(),
    ),
    path("admin/ads/", views_ads.AdvertisementListView.as_view()),
    path("admin/ads/stats/", views_ad_stats.AdStatsView.as_view()),
    path("admin/ads/<uuid:advertisement_id>/", views_ads.AdvertisementDetailView.as_view()),
    path("admin/audit/", views.AuditListView.as_view()),
    path("admin/analytics/", views.AnalyticsView.as_view()),
    path("admin/analytics/series/", views_analytics.AnalyticsSeriesView.as_view()),
    path("admin/settings/", views.SettingsView.as_view()),
    path("admin/system/status/", views.SystemStatusView.as_view()),
]
