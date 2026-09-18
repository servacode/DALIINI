"""INT-006: the launch baseline exists, is deterministic, and never overrules an operator.

The test database is built by running migrations, so everything asserted here about the
baseline is asserting what `0004_launch_baseline` actually produced — not what a fixture
set up for the occasion.
"""

import uuid
from io import StringIO
from typing import Any

import pytest
from django.apps import apps as django_apps
from django.core.management import call_command
from django.core.management.base import CommandError
from rest_framework.test import APIClient

from accounts.models import User
from directory.models import Category, CategoryCapabilities, CategoryGroup, CategoryProvince
from directory.reference_data import launch_v1
from directory.reference_data.apply import ReferenceIdMismatch, apply_dataset
from facilities.models import Facility
from locations.models import Province

CANONICAL_PROVINCE_CODES = {code for code, *_ in launch_v1.PROVINCES}
CANONICAL_CATEGORY_CODES = {item["code"] for item in launch_v1.CATEGORIES}


def _canonical_provinces() -> Any:
    """Only the seeded rows; the shared `facility` fixture adds a `raqqa-test` province."""
    return Province.objects.filter(code__in=CANONICAL_PROVINCE_CODES)


def _canonical_categories() -> Any:
    return Category.objects.filter(code__in=CANONICAL_CATEGORY_CODES)


def _seed(*args: str) -> str:
    out = StringIO()
    call_command("seed_launch_baseline", *args, stdout=out)
    return out.getvalue()


# --------------------------------------------------------------------------------------
# Fresh database, straight after migrate
# --------------------------------------------------------------------------------------


@pytest.mark.django_db
def test_exactly_fourteen_provinces_are_seeded() -> None:
    assert _canonical_provinces().count() == 14
    assert len(launch_v1.PROVINCES) == 14


@pytest.mark.django_db
def test_every_province_carries_its_deterministic_id() -> None:
    for code, *_ in launch_v1.PROVINCES:
        province = Province.objects.get(code=code)
        assert province.pk == launch_v1.reference_id("province", code)


@pytest.mark.django_db
def test_raqqa_is_the_only_active_province() -> None:
    active = list(_canonical_provinces().filter(active=True).values_list("code", flat=True))

    assert active == ["raqqa"]
    assert _canonical_provinces().filter(active=False).count() == 13


@pytest.mark.django_db
def test_the_health_group_and_five_categories_are_seeded() -> None:
    group = CategoryGroup.objects.get(code="health")

    assert group.pk == launch_v1.reference_id("category-group", "health")
    assert group.active is True
    assert _canonical_categories().count() == 5
    assert set(_canonical_categories().values_list("code", flat=True)) == {
        "pharmacy",
        "medical-laboratory",
        "medical-clinic",
        "nursing-center",
        "medical-supplies",
    }
    for category in _canonical_categories():
        assert category.group_id == group.pk
        assert category.pk == launch_v1.reference_id("category", category.code)


@pytest.mark.django_db
def test_pharmacy_is_the_launch_category() -> None:
    pharmacy = Category.objects.get(code="pharmacy")
    caps = CategoryCapabilities.objects.get(category=pharmacy)

    assert pharmacy.specialization == Category.Specialization.PHARMACY
    assert pharmacy.active is True
    assert (caps.supports_duty, caps.supports_hours, caps.supports_photos) == (True, True, True)
    assert (caps.supports_ratings, caps.supports_temporary_closure) == (True, True)
    assert caps.supports_owner_onboarding is True
    assert (caps.supports_specialty_filter, caps.supports_service_filter) == (False, False)


@pytest.mark.django_db
@pytest.mark.parametrize(
    ("code", "specialization", "specialty_filter", "service_filter"),
    [
        ("medical-laboratory", "GENERIC", False, False),
        ("medical-clinic", "MEDICAL_CLINIC", True, False),
        ("nursing-center", "NURSING_CENTER", False, True),
        ("medical-supplies", "GENERIC", False, False),
    ],
)
def test_the_other_health_categories_are_configured_but_carry_no_duty(
    code: str, specialization: str, specialty_filter: bool, service_filter: bool
) -> None:
    category = Category.objects.get(code=code)
    caps = CategoryCapabilities.objects.get(category=category)

    assert category.specialization == specialization
    assert caps.supports_duty is False
    assert caps.supports_specialty_filter is specialty_filter
    assert caps.supports_service_filter is service_filter


@pytest.mark.django_db
def test_only_raqqa_pharmacy_is_switched_on() -> None:
    switches = CategoryProvince.objects.filter(
        category__code__in=CANONICAL_CATEGORY_CODES,
        province__code__in=CANONICAL_PROVINCE_CODES,
    )

    assert switches.count() == 5 * 14
    on = [
        (row.category.code, row.province.code)
        for row in switches.filter(public_enabled=True).select_related("category", "province")
    ]
    assert on == [("pharmacy", "raqqa")]
    assert switches.filter(owner_registration_enabled=True).count() == 1
    assert switches.filter(
        province__code="raqqa", public_enabled=False, owner_registration_enabled=False
    ).count() == 4


@pytest.mark.django_db
def test_no_facility_user_or_other_operational_row_was_seeded() -> None:
    assert Facility.objects.count() == 0
    assert User.objects.count() == 0
    assert Province.objects.exclude(code__in=CANONICAL_PROVINCE_CODES).count() == 0


@pytest.mark.django_db
def test_no_verification_requirement_is_seeded() -> None:
    """LAUNCH_POLICY_PENDING: pharmacy requirements are a product decision, not a seed."""
    assert launch_v1.VERIFICATION_REQUIREMENTS == ()
    for category in _canonical_categories():
        assert category.verification_requirements.count() == 0


@pytest.mark.django_db
def test_no_city_or_neighborhood_is_invented() -> None:
    for province in _canonical_provinces():
        assert province.cities.count() == 0


# --------------------------------------------------------------------------------------
# The command
# --------------------------------------------------------------------------------------


def _snapshot() -> dict[str, Any]:
    return {
        "provinces": sorted(
            (p.code, str(p.pk), p.active, p.sort_order, p.name_ar)
            for p in _canonical_provinces()
        ),
        "categories": sorted(
            (c.code, str(c.pk), c.active, c.specialization) for c in _canonical_categories()
        ),
        "groups": sorted((g.code, str(g.pk), g.active) for g in CategoryGroup.objects.all()),
        "switches": sorted(
            (s.category.code, s.province.code, s.public_enabled, s.owner_registration_enabled)
            for s in CategoryProvince.objects.select_related("category", "province")
        ),
        "capabilities": sorted(
            (c.category.code, c.supports_duty, c.supports_specialty_filter)
            for c in CategoryCapabilities.objects.select_related("category")
        ),
    }


@pytest.mark.django_db
def test_the_command_is_idempotent_after_the_migration() -> None:
    before = _snapshot()

    first = _seed()
    after_first = _snapshot()
    second = _seed()
    after_second = _snapshot()

    assert "Nothing to do" in first
    assert "Nothing to do" in second
    assert before == after_first == after_second


@pytest.mark.django_db
def test_check_mode_passes_on_a_complete_database_and_writes_nothing() -> None:
    before = _snapshot()

    output = _seed("--check")

    assert "Reference data is complete." in output
    assert _snapshot() == before


@pytest.mark.django_db
def test_rerunning_does_not_undo_an_admin_decision() -> None:
    aleppo = Province.objects.get(code="aleppo")
    aleppo.active = True
    aleppo.sort_order = 99
    aleppo.save()
    clinic_switch = CategoryProvince.objects.get(
        category__code="medical-clinic", province__code="raqqa"
    )
    clinic_switch.public_enabled = True
    clinic_switch.owner_registration_enabled = True
    clinic_switch.save()
    pharmacy_switch = CategoryProvince.objects.get(
        category__code="pharmacy", province__code="raqqa"
    )
    pharmacy_switch.owner_registration_enabled = False
    pharmacy_switch.save()

    _seed()

    aleppo.refresh_from_db()
    clinic_switch.refresh_from_db()
    pharmacy_switch.refresh_from_db()
    assert aleppo.active is True, "the seed re-disabled a province an operator activated"
    assert aleppo.sort_order == 99
    assert clinic_switch.public_enabled is True
    assert clinic_switch.owner_registration_enabled is True
    assert pharmacy_switch.owner_registration_enabled is False, (
        "the seed re-enabled onboarding an operator had switched off"
    )


@pytest.mark.django_db
def test_a_missing_canonical_row_is_recreated_with_the_same_id() -> None:
    original = Province.objects.get(code="quneitra")
    original_id = original.pk
    original.delete()
    assert not Province.objects.filter(code="quneitra").exists()

    output = _seed()

    restored = Province.objects.get(code="quneitra")
    assert restored.pk == original_id
    assert restored.pk == launch_v1.reference_id("province", "quneitra")
    assert restored.active is False
    assert "Created" in output
    # The switches that cascaded away with it come back too.
    assert CategoryProvince.objects.filter(province=restored).count() == 5


@pytest.mark.django_db
def test_check_mode_reports_a_missing_row_and_still_writes_nothing() -> None:
    Province.objects.get(code="quneitra").delete()

    with pytest.raises(CommandError, match="reference rows are missing"):
        _seed("--check")

    assert not Province.objects.filter(code="quneitra").exists()


@pytest.mark.django_db
def test_a_wrong_primary_key_is_reported_and_nothing_is_rewritten() -> None:
    Province.objects.get(code="quneitra").delete()
    intruder = Province.objects.create(
        id=uuid.UUID("00000000-0000-4000-8000-000000000001"),
        code="quneitra",
        name_ar="القنيطرة",
    )

    with pytest.raises(ReferenceIdMismatch) as caught:
        apply_dataset(launch_v1, django_apps.get_model)

    assert caught.value.code == "REFERENCE_ID_MISMATCH"
    intruder.refresh_from_db()
    assert intruder.pk == uuid.UUID("00000000-0000-4000-8000-000000000001")


@pytest.mark.django_db
def test_the_command_surfaces_the_mismatch_as_a_command_error() -> None:
    Province.objects.get(code="quneitra").delete()
    Province.objects.create(
        id=uuid.UUID("00000000-0000-4000-8000-000000000002"),
        code="quneitra",
        name_ar="القنيطرة",
    )

    with pytest.raises(CommandError, match="REFERENCE_ID_MISMATCH"):
        _seed()


# --------------------------------------------------------------------------------------
# What the API actually serves
# --------------------------------------------------------------------------------------


@pytest.mark.django_db
def test_public_provinces_serves_raqqa_and_is_no_longer_empty() -> None:
    body = APIClient().get("/api/v1/public/provinces/").json()

    assert body["items"], "the province endpoint is still empty"
    assert [item["code"] for item in body["items"]] == ["raqqa"]
    assert body["items"][0]["id"] == str(launch_v1.reference_id("province", "raqqa"))
    assert body["items"][0]["nameAr"] == "الرقة"


@pytest.mark.django_db
def test_raqqa_public_taxonomy_shows_pharmacies_only() -> None:
    raqqa = launch_v1.reference_id("province", "raqqa")

    body = APIClient().get(f"/api/v1/public/provinces/{raqqa}/categories/").json()

    assert [item["nameAr"] for item in body["items"]] == ["صيدليات"]
    assert body["items"][0]["capabilities"]["duty"] is True


@pytest.mark.django_db
def test_an_inactive_province_is_not_publicly_addressable() -> None:
    aleppo = launch_v1.reference_id("province", "aleppo")

    response = APIClient().get(f"/api/v1/public/provinces/{aleppo}/categories/")

    assert response.status_code == 404
    assert response.json()["code"] == "NOT_FOUND"


@pytest.mark.django_db
def test_owner_onboarding_is_offered_for_raqqa_pharmacies_only(user: User) -> None:
    client = APIClient()
    client.force_authenticate(user=user)
    raqqa = launch_v1.reference_id("province", "raqqa")

    body = client.get("/api/v1/owner/config/", {"provinceId": str(raqqa)}).json()

    assert [item["category"]["nameAr"] for item in body["categories"]] == ["صيدليات"]


@pytest.mark.django_db
def test_owner_onboarding_is_unavailable_in_an_inactive_province(user: User) -> None:
    client = APIClient()
    client.force_authenticate(user=user)
    aleppo = launch_v1.reference_id("province", "aleppo")

    response = client.get("/api/v1/owner/config/", {"provinceId": str(aleppo)})

    assert response.status_code == 404
