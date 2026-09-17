from django.urls import path

from .views import PublicProvinceCategoriesView

urlpatterns = [
    path(
        "public/provinces/<uuid:province_id>/categories/",
        PublicProvinceCategoriesView.as_view(),
    )
]
