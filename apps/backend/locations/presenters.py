"""Response shapes shared by every endpoint that returns a province."""

from .models import Province


def map_center(province: Province) -> dict[str, float] | None:
    """Where a map opens for the province, or None when none is set (INT-092)."""
    point = province.map_center
    return {"latitude": point.y, "longitude": point.x} if point else None


def province_payload(province: Province | None) -> dict | None:
    """A province as every public endpoint returns it."""
    if province is None:
        return None
    return {
        "id": str(province.id),
        "code": province.code,
        "nameAr": province.name_ar,
        "nameEn": province.name_en or None,
        "mapCenter": map_center(province),
    }


def place(row) -> dict | None:
    """A city or a neighbourhood, named and nothing more."""
    if row is None:
        return None
    return {"id": str(row.id), "nameAr": row.name_ar, "nameEn": row.name_en or None}
