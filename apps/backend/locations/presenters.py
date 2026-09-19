"""Response shapes shared by every endpoint that returns a province."""

from .models import Province


def map_center(province: Province) -> dict[str, float] | None:
    """Where a map opens for the province, or None when none is set (INT-092)."""
    point = province.map_center
    return {"latitude": point.y, "longitude": point.x} if point else None
