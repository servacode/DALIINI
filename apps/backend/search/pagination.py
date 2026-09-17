from rest_framework.pagination import CursorPagination


class FacilityCursorPagination(CursorPagination):
    page_size = 30
    page_size_query_param = "limit"
    max_page_size = 100
    ordering = ("name_ar", "id")
