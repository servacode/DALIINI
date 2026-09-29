from __future__ import annotations

from typing import Any

from django.db.models.signals import post_delete, post_save
from django.dispatch import receiver

from .maintenance import TYPED_DEFAULTS, invalidate_maintenance_cache
from .models import PlatformSetting


@receiver(post_save, sender=PlatformSetting)
@receiver(post_delete, sender=PlatformSetting)
def _invalidate(sender: type[PlatformSetting], instance: PlatformSetting, **kwargs: Any) -> None:
    if instance.key in TYPED_DEFAULTS:
        invalidate_maintenance_cache()
