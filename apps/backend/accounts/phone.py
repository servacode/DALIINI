import re

SYRIAN_MOBILE = re.compile(r"^\+9639\d{8}$")


def normalize_syrian_phone(value: str) -> str:
    compact = re.sub(r"[\s()-]", "", (value or "").strip())
    if compact.startswith("00963"):
        compact = "+963" + compact[5:]
    elif compact.startswith("963"):
        compact = "+" + compact
    elif compact.startswith("09"):
        compact = "+963" + compact[1:]
    if not SYRIAN_MOBILE.fullmatch(compact):
        raise ValueError("Invalid Syrian mobile number.")
    return compact
