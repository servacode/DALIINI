"""The everyday places of DECISION-125: seeded once, on in Raqqa, and the operator's afterwards."""

import pytest
from django.apps import apps

from directory.models import Category, CategoryGroup, CategoryProvince
from directory.reference_data import launch_v2
from directory.reference_data.apply import apply_dataset

CODES = ["bakery", "market", "restaurant", "fuel-station", "exchange", "workshop"]


@pytest.mark.django_db
def test_migration_leaves_six_places_in_their_own_group_on_in_raqqa() -> None:
    group = CategoryGroup.objects.get(code="places")
    places = Category.objects.filter(group=group).order_by("sort_order")

    assert [item.code for item in places] == CODES
    assert all(item.pk == launch_v2.reference_id("category", item.code) for item in places)
    for item in places.exclude(code="exchange"):
        switch = CategoryProvince.objects.get(category=item, province__code="raqqa")
        assert switch.public_enabled and switch.owner_registration_enabled
        assert (
            not CategoryProvince.objects.filter(category=item, public_enabled=True)
            .exclude(province__code="raqqa")
            .exists()
        )


@pytest.mark.django_db
def test_reapplying_leaves_an_operators_changes_alone() -> None:
    bakery = Category.objects.get(code="bakery")
    bakery.name_ar = "أفران"
    bakery.save(update_fields=["name_ar"])
    CategoryProvince.objects.filter(category=bakery).update(public_enabled=False)

    summary = apply_dataset(launch_v2, apps.get_model)

    bakery.refresh_from_db()
    assert bakery.name_ar == "أفران"
    assert not CategoryProvince.objects.filter(category=bakery, public_enabled=True).exists()
    assert sum(summary.created.values()) == 0


@pytest.mark.django_db
def test_exchange_is_switched_off_everywhere() -> None:
    """DECISION-126: a money-exchange category reads to Google Play as a financial service."""
    exchange = Category.objects.get(code="exchange")

    assert not CategoryProvince.objects.filter(category=exchange, public_enabled=True).exists()
    assert not CategoryProvince.objects.filter(
        category=exchange, owner_registration_enabled=True
    ).exists()
