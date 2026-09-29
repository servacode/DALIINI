"""Specialties and services: which ones a category offers, and how they appear on the wire.

A specialty has exactly one scope (`Specialty.clean`): one category, or a specialization, in
which case every category of that specialization offers it. A service belongs to one
category. Only active rows are offered, in the order the operators set.

The public choices read and the owner configuration both answer from here, so a filter chip
on the site and a picker in the owner app always list the same things.
"""

from __future__ import annotations

from collections import defaultdict
from collections.abc import Iterable
from typing import Any
from uuid import UUID

from django.db.models import Q, QuerySet

from .models import Category, ServiceTag, Specialty

#: The operators' order, then a stable tie-break.
ORDER = ("sort_order", "name_ar", "id")

TagChoices = dict[str, list[dict[str, Any]]]


def named(row: Specialty | ServiceTag) -> dict[str, Any]:
    """The `NamedIntRef` wire shape."""
    return {"id": row.pk, "nameAr": row.name_ar}


def active_in_order[T: (Specialty, ServiceTag)](rows: Iterable[T]) -> list[T]:
    """`rows` without the retired ones, in `ORDER`, for rows already in memory."""
    return sorted(
        (row for row in rows if row.active),
        key=lambda row: (row.sort_order, row.name_ar, row.pk),
    )


def specialties_for(category: Category) -> QuerySet[Specialty]:
    """Every specialty `category` can carry, active or not: its own, and its specialization's."""
    return Specialty.objects.filter(
        Q(category_id=category.pk)
        | Q(category__isnull=True, specialization=category.specialization)
    )


def tag_choices(categories: Iterable[Category]) -> dict[UUID, TagChoices]:
    """The active specialties and services of each category, keyed by category id.

    Two queries however many categories are asked about, so the owner configuration does
    not cost two more per category it lists.
    """
    wanted = list(categories)
    result: dict[UUID, TagChoices] = {
        category.pk: {"specialties": [], "services": []} for category in wanted
    }
    if not wanted:
        return result
    by_specialization: dict[str, list[UUID]] = defaultdict(list)
    for category in wanted:
        by_specialization[category.specialization].append(category.pk)
    specialties = Specialty.objects.filter(
        Q(category_id__in=result.keys())
        | Q(category__isnull=True, specialization__in=by_specialization.keys()),
        active=True,
    ).order_by(*ORDER)
    for specialty in specialties:
        owners = (
            [specialty.category_id]
            if specialty.category_id is not None
            else by_specialization[specialty.specialization]
        )
        for category_id in owners:
            result[category_id]["specialties"].append(named(specialty))
    services = ServiceTag.objects.filter(category_id__in=result.keys(), active=True).order_by(
        *ORDER
    )
    for service in services:
        result[service.category_id]["services"].append(named(service))
    return result


def category_tag_choices(category: Category) -> TagChoices:
    return tag_choices([category])[category.pk]
