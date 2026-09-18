from core.pagination import CursorPage


class FacilityCursorPagination(CursorPage):
    """Facility listings, ordered by Arabic name with the id as the unique tiebreaker.

    Views that sort by distance replace `ordering` with `("distance_meters", "id")`; the id keeps
    the ordering total in both cases, which is what makes the cursor stable.
    """

    ordering = ("name_ar", "id")
