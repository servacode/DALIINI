from django.urls import path

from .public import PublicDutyByDateView
from .views import DutyDetailView, DutyListCreateView

urlpatterns = [
    path("public/duty/", PublicDutyByDateView.as_view()),
    path(
        "owner/facilities/<uuid:facility_id>/duty/",
        DutyListCreateView.as_view(),
    ),
    path(
        "owner/facilities/<uuid:facility_id>/duty/<uuid:shift_id>/",
        DutyDetailView.as_view(),
    ),
]
