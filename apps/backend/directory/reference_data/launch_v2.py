"""The second reference dataset: places beyond health (DECISION-125). FROZEN once migrated.

Dalini is a directory of the places of a city, of which pharmacies are one kind. This adds a
second group, everyday places and services, with six categories, each on in Raqqa from the
start, the province the platform opened in. It is reconciled the way `launch_v1` is: a row that
exists is left exactly as an operator has it, so names, order, switches and capabilities pass to
the console the moment they are created.

The provinces are `launch_v1`'s, listed again only so the switches can be made for each; they
already exist and are only verified. Like `launch_v1`, this module must not be edited once a
migration has applied it: a later change is `launch_v3.py` and a new migration.
"""

from directory.reference_data.launch_v1 import NAMESPACE, PROVINCES, reference_id

__all__ = ["NAMESPACE", "PROVINCES", "reference_id"]

VERSION = "launch_v2"

CATEGORY_GROUPS = (
    # code, name_ar, name_en, sort_order
    ("places", "أماكن وخدمات", "Places & Services", 1),
)

_EVERYDAY = {
    "supports_hours": True,
    "supports_photos": True,
    "supports_ratings": True,
    "supports_duty": False,
    "supports_specialty_filter": False,
    "supports_service_filter": False,
    "supports_temporary_closure": True,
    "supports_owner_onboarding": True,
}


def _category(code: str, name_ar: str, name_en: str, icon_key: str, sort_order: int) -> dict:
    return {
        "code": code,
        "slug": code,
        "group": "places",
        "name_ar": name_ar,
        "name_en": name_en,
        "icon_key": icon_key,
        "specialization": "GENERIC",
        "sort_order": sort_order,
        "capabilities": dict(_EVERYDAY),
    }


CATEGORIES = (
    _category("bakery", "مخابز", "Bakeries", "bakery", 10),
    _category("market", "أسواق ومواد غذائية", "Markets & Groceries", "cart", 11),
    _category("restaurant", "مطاعم", "Restaurants", "restaurant", 12),
    _category("fuel-station", "محطات وقود", "Fuel Stations", "fuel", 13),
    _category("exchange", "صرافة وحوالات", "Exchange & Transfers", "cash", 14),
    _category("workshop", "ورشات وصيانة", "Workshops & Repairs", "tool", 15),
)

ENABLED_SWITCHES = tuple((item["code"], "raqqa") for item in CATEGORIES)

VERIFICATION_REQUIREMENTS = ()
