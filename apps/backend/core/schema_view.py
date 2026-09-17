"""Exposure policy for the interactive schema endpoint.

`07-BACKEND-DJANGO.md` requires the schema to be served only in development, or behind a
privileged check in staging, and to be disabled in production. CI does not need the HTTP
route at all: it generates the document with the `spectacular` management command.

Controlled by `OPENAPI_SCHEMA_EXPOSURE`:

    public      served to anyone, development only
    privileged  served to an authenticated admin holding `admin.system.read`
    disabled    not routed at all
"""

from django.conf import settings
from drf_spectacular.views import SpectacularAPIView
from rest_framework.permissions import IsAuthenticated

PUBLIC = "public"
PRIVILEGED = "privileged"
DISABLED = "disabled"


class PrivilegedSchemaView(SpectacularAPIView):
    """Schema served only to an admin who may already read system status."""

    required_permission = "admin.system.read"

    def get_permissions(self):
        from admin_console.permissions import HasAdminPermission

        return [IsAuthenticated(), HasAdminPermission()]


def schema_urlpatterns():
    """Return the schema route for the current environment, or nothing."""
    from django.urls import path

    exposure = getattr(settings, "OPENAPI_SCHEMA_EXPOSURE", PRIVILEGED)
    if exposure == DISABLED:
        return []
    view = SpectacularAPIView if exposure == PUBLIC else PrivilegedSchemaView
    return [path("api/schema/", view.as_view(), name="schema")]
