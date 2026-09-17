from django.urls import path

from .views import AccountRatingsView, FacilityRatingView

urlpatterns = [
    path("facilities/<uuid:facility_id>/rating/", FacilityRatingView.as_view()),
    path("account/ratings/", AccountRatingsView.as_view()),
]
