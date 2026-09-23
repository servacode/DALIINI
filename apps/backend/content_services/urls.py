from django.urls import path

from .views import PublicAdsView, PublicLegalDocumentsView, PublicLegalDocumentView

urlpatterns = [
    path("public/ads/", PublicAdsView.as_view(), name="public-ads"),
    path("public/legal/", PublicLegalDocumentsView.as_view(), name="public-legal-list"),
    path("public/legal/<str:key>/", PublicLegalDocumentView.as_view(), name="public-legal"),
]
