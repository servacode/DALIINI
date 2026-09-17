from django.urls import path

from .views import DutyDetailView, DutyListCreateView

urlpatterns = [
    path(
        "owner/facilities/<uuid:facility_id>/duty/",
        DutyListCreateView.as_view(),
    ),
    path(
        "owner/facilities/<uuid:facility_id>/duty/<uuid:shift_id>/",
        DutyDetailView.as_view(),
    ),
]
