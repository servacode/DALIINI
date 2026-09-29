from django.urls import path

from .views import PublicCategoryTagsView, PublicProvinceCategoriesView

urlpatterns = [
    path(
        "public/provinces/<uuid:province_id>/categories/",
        PublicProvinceCategoriesView.as_view(),
    ),
    path("public/categories/<uuid:category_id>/tags/", PublicCategoryTagsView.as_view()),
]
