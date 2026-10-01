from django.urls import path

from .views import PlatformStatusView

urlpatterns = [
    path("platform/status/", PlatformStatusView.as_view()),
]
