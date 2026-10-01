"""The specialties and services a category offers: the public read and the owner configuration.

Both answer from `directory.tags`, so a filter chip on the site and a choice in the owner app
are the same list: active items only, in the operators' order, with integer ids. Specialties
come from two scopes, the category's own and its specialization's.
"""

import pytest
from rest_framework.test import APIClient

from accounts.models import User
from directory.models import (
    Category,
    CategoryCapabilities,
    CategoryGroup,
    CategoryProvince,
    ServiceTag,
    Specialty,
)
from locations.models import Province

MISSING = "00000000-0000-4000-8000-000000000000"


def tags_url(category: Category | str) -> str:
    key = category if isinstance(category, str) else category.pk
    return f"/api/v1/public/categories/{key}/tags/"


@pytest.fixture
def province(db: None) -> Province:
    return Province.objects.create(code="tags-province", name_ar="محافظة", active=True)


def clinic(province: Province, code: str, *, public: bool = True, owners: bool = False) -> Category:
    group, _ = CategoryGroup.objects.get_or_create(code="tags-health", defaults={"name_ar": "صحة"})
    category = Category.objects.create(
        group=group,
        code=code,
        slug=code,
        name_ar=f"عيادات {code}",
        specialization=Category.Specialization.MEDICAL_CLINIC,
    )
    CategoryCapabilities.objects.create(
        category=category, supports_specialty_filter=True, supports_service_filter=True
    )
    CategoryProvince.objects.create(
        category=category,
        province=province,
        public_enabled=public,
        owner_registration_enabled=owners,
    )
    return category


@pytest.fixture
def clinics(province: Province) -> Category:
    """A public clinic category with specialties in both scopes and services of its own."""
    category = clinic(province, "tags-clinics")
    other = clinic(province, "tags-dental")
    Specialty.objects.create(category=category, name_ar="أطفال", sort_order=20)
    Specialty.objects.create(category=category, name_ar="قلبية", sort_order=10)
    Specialty.objects.create(category=category, name_ar="متقاعد", sort_order=0, active=False)
    Specialty.objects.create(specialization="MEDICAL_CLINIC", name_ar="عامة", sort_order=15)
    Specialty.objects.create(specialization="PHARMACY", name_ar="صيدلة سريرية", sort_order=1)
    Specialty.objects.create(category=other, name_ar="تقويم", sort_order=1)
    ServiceTag.objects.create(category=category, name_ar="تخطيط قلب", sort_order=2)
    ServiceTag.objects.create(category=category, name_ar="تحاليل", sort_order=1)
    ServiceTag.objects.create(category=category, name_ar="قديمة", active=False)
    ServiceTag.objects.create(category=other, name_ar="تبييض")
    return category


@pytest.mark.django_db
def test_the_public_read_lists_active_choices_in_order_with_integer_ids(clinics: Category) -> None:
    response = APIClient().get(tags_url(clinics))

    assert response.status_code == 200, response.content
    body = response.json()
    assert [row["nameAr"] for row in body["specialties"]] == ["قلبية", "عامة", "أطفال"]
    assert [row["nameAr"] for row in body["services"]] == ["تحاليل", "تخطيط قلب"]
    assert all(isinstance(row["id"], int) for row in body["specialties"] + body["services"])
    assert {key for row in body["specialties"] for key in row} == {"id", "nameAr"}
    shared = Specialty.objects.get(name_ar="عامة")
    assert {"id": shared.pk, "nameAr": "عامة"} in body["specialties"]


@pytest.mark.django_db
def test_a_specialization_specialty_is_offered_by_every_category_of_it(
    clinics: Category, province: Province
) -> None:
    dental = Category.objects.get(code="tags-dental")

    body = APIClient().get(tags_url(dental)).json()

    assert [row["nameAr"] for row in body["specialties"]] == ["تقويم", "عامة"]
    assert [row["nameAr"] for row in body["services"]] == ["تبييض"]


@pytest.mark.django_db
def test_the_public_read_is_anonymous_and_cacheable(clinics: Category) -> None:
    response = APIClient().get(tags_url(clinics))

    assert response.status_code == 200
    assert response["Cache-Control"] == "public, max-age=300"


@pytest.mark.django_db
def test_a_category_with_nothing_configured_answers_two_empty_lists(province: Province) -> None:
    category = clinic(province, "tags-empty")

    assert APIClient().get(tags_url(category)).json() == {"specialties": [], "services": []}


@pytest.mark.django_db
@pytest.mark.parametrize(
    "hide",
    [
        "switch_off",
        "category_inactive",
        "group_inactive",
        "province_inactive",
    ],
)
def test_a_category_that_is_not_public_is_not_found(clinics: Category, hide: str) -> None:
    if hide == "switch_off":
        CategoryProvince.objects.filter(category=clinics).update(public_enabled=False)
    elif hide == "category_inactive":
        Category.objects.filter(pk=clinics.pk).update(active=False)
    elif hide == "group_inactive":
        CategoryGroup.objects.filter(pk=clinics.group_id).update(active=False)
    else:
        Province.objects.filter(code="tags-province").update(active=False)

    response = APIClient().get(tags_url(clinics))

    assert response.status_code == 404
    assert response.json()["code"] == "NOT_FOUND"


@pytest.mark.django_db
def test_an_unknown_category_is_not_found(db: None) -> None:
    assert APIClient().get(tags_url(MISSING)).status_code == 404


# --------------------------------------------------------------------------------------
# The owner configuration offers the same lists
# --------------------------------------------------------------------------------------


@pytest.mark.django_db
def test_the_owner_configuration_offers_the_same_choices(
    clinics: Category, province: Province, user: User
) -> None:
    CategoryProvince.objects.filter(category=clinics).update(owner_registration_enabled=True)
    client = APIClient()
    client.force_authenticate(user=user)

    config = client.get("/api/v1/owner/config/", {"provinceId": str(province.pk)}).json()
    public = APIClient().get(tags_url(clinics)).json()

    entry = next(row for row in config["categories"] if row["category"]["id"] == str(clinics.pk))
    assert entry["specialties"] == public["specialties"]
    assert entry["services"] == public["services"]


@pytest.mark.django_db
def test_every_owner_category_carries_its_own_lists(
    clinics: Category, province: Province, user: User
) -> None:
    CategoryProvince.objects.filter(province=province).update(owner_registration_enabled=True)
    client = APIClient()
    client.force_authenticate(user=user)

    config = client.get("/api/v1/owner/config/", {"provinceId": str(province.pk)}).json()

    by_id = {row["category"]["id"]: row for row in config["categories"]}
    dental = Category.objects.get(code="tags-dental")
    assert [row["nameAr"] for row in by_id[str(dental.pk)]["specialties"]] == ["تقويم", "عامة"]
    assert [row["nameAr"] for row in by_id[str(dental.pk)]["services"]] == ["تبييض"]
    assert [row["nameAr"] for row in by_id[str(clinics.pk)]["services"]] == [
        "تحاليل",
        "تخطيط قلب",
    ]
