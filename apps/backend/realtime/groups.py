import hashlib
from uuid import UUID


def _safe(value: str) -> str:
    return hashlib.sha256(value.encode("utf-8")).hexdigest()[:32]


def province_group(province_id: UUID | str) -> str:
    return f"public.province.{_safe(str(province_id))}"


def user_group(user_id: UUID | str) -> str:
    return f"user.{_safe(str(user_id))}"


def admin_group(channel: str = "system") -> str:
    return f"admin.{_safe(channel)}"
