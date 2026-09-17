from django.db.models import Q
from django.utils import timezone

from .models import Advertisement


def active_ads(*, province_id=None, category_id=None, now=None):
    now = now or timezone.now()
    schedule = (Q(starts_at__isnull=True) | Q(starts_at__lte=now)) & (
        Q(ends_at__isnull=True) | Q(ends_at__gt=now)
    )
    target = Q(target_scope=Advertisement.TargetScope.GLOBAL)
    if province_id:
        target |= Q(target_scope=Advertisement.TargetScope.PROVINCE, province_id=province_id)
    if category_id:
        target |= Q(target_scope=Advertisement.TargetScope.CATEGORY, category_id=category_id)
    return (
        Advertisement.objects.filter(enabled=True)
        .filter(schedule)
        .filter(target)
        .order_by("sort_order", "created_at")
    )
