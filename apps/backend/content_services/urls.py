from django.urls import path

from .views import PublicAdsView

urlpatterns = [path("public/ads/", PublicAdsView.as_view(), name="public-ads")]
