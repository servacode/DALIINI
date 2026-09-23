from django.urls import path

from .views import AccountFavoriteDetailView, AccountFavoritesView

urlpatterns = [
    path("account/favorites/", AccountFavoritesView.as_view()),
    path("account/favorites/<uuid:facility_id>/", AccountFavoriteDetailView.as_view()),
]
