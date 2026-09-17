import os
from collections.abc import Iterable

from django.core.exceptions import ImproperlyConfigured


def env(name: str, default: str | None = None, *, required: bool = False) -> str:
    value = os.getenv(name, default)
    if required and (value is None or not value.strip()):
        raise ImproperlyConfigured(f"Required environment variable is missing: {name}")
    return value or ""


def env_bool(name: str, default: bool = False) -> bool:
    value = os.getenv(name)
    if value is None:
        return default
    normalized = value.strip().lower()
    if normalized in {"1", "true", "yes", "on"}:
        return True
    if normalized in {"0", "false", "no", "off"}:
        return False
    raise ImproperlyConfigured(f"Invalid boolean environment value: {name}")


def env_csv(name: str, default: Iterable[str] = ()) -> list[str]:
    raw = os.getenv(name)
    if raw is None:
        return list(default)
    return [part.strip() for part in raw.split(",") if part.strip()]
