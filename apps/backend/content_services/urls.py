from django.urls import path

from .views import (
    PublicAdsView,
    PublicContactView,
    PublicContentPageView,
    PublicEmergencyNumbersView,
    PublicFaqView,
    PublicLegalDocumentsView,
    PublicLegalDocumentView,
)

urlpatterns = [
    path("public/ads/", PublicAdsView.as_view(), name="public-ads"),
    path("public/legal/", PublicLegalDocumentsView.as_view(), name="public-legal-list"),
    path("public/legal/<str:key>/", PublicLegalDocumentView.as_view(), name="public-legal"),
    path("content/pages/<str:slug>/", PublicContentPageView.as_view(), name="content-page"),
    path("content/faq/", PublicFaqView.as_view(), name="content-faq"),
    path("emergency-numbers/", PublicEmergencyNumbersView.as_view(), name="emergency-numbers"),
    path("contact/", PublicContactView.as_view(), name="contact"),
]
