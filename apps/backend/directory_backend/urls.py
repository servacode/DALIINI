from django.urls import include, path

from core.schema_view import schema_urlpatterns

urlpatterns = [
    path("health/", include("health.urls")),
    *schema_urlpatterns(),
    path("api/v1/", include("accounts.urls")),
    path("api/v1/", include("facilities.urls")),
    path("api/v1/", include("business_hours.urls")),
    path("api/v1/", include("pharmacy_duty.urls")),
    path("api/v1/", include("locations.urls")),
    path("api/v1/", include("directory.urls")),
    path("api/v1/", include("search.urls")),
    path("api/v1/", include("ratings.urls")),
    path("api/v1/", include("favorites.urls")),
    path("api/v1/", include("content_services.urls")),
    path("api/v1/", include("analytics.urls")),
    path("api/v1/", include("admin_console.urls")),
    path("api/v1/", include("notifications.urls")),
]
