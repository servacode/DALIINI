"""A search finds a place however its name is spelled.

The owner writes «صيدلية الأمل»; people type «صيدليه الامل», «صيدلية الامل», or paste a
vowelled «صَيْدَلِيَّة». Raw `icontains` found only the owner's spelling.
"""

import pytest
from django.db import connection
from rest_framework.test import APIClient

from directory.models import CategoryProvince
from facilities.models import Facility
from search.arabic import normalize_arabic

SEARCH = "/api/v1/public/search/"

SAMPLES = [
    "صيدلية الأمل",
    "أحمد إبراهيم آل",
    "مستشفى الرَّحمة",
    "مؤسسة الشفاء الطبية",
    "صيـــدلية",
    "Al-Amal PHARMACY",
    "  مسافات   كثيرة  ",
    "یاسمین کریم",
    "ٱلنور",
]


@pytest.fixture
def listed(db: None, facility: Facility) -> Facility:
    CategoryProvince.objects.create(
        province=facility.province,
        category=facility.category,
        public_enabled=True,
        owner_registration_enabled=True,
    )
    facility.name_ar = "صيدلية الأمل"
    facility.name_en = "Al-Amal Pharmacy"
    facility.address_ar = "شارع المنصور، قرب مستشفى الرحمة"
    facility.save(update_fields=["name_ar", "name_en", "address_ar"])
    return facility


def _found(facility: Facility, term: str) -> bool:
    response = APIClient().get(SEARCH, {"q": term, "provinceId": str(facility.province_id)})
    assert response.status_code == 200, response.content
    return str(facility.id) in {item["id"] for item in response.json()["items"]}


@pytest.mark.parametrize(
    ("text", "expected"),
    [
        ("صيدلية", "صيدليه"),
        ("الأمل", "الامل"),
        ("إبراهيم", "ابراهيم"),
        ("مستشفى", "مستشفي"),
        ("صَيْدَلِيَّة", "صيدليه"),
        ("صيـــدلية", "صيدليه"),
        ("مؤسسة", "موسسه"),
        ("  كلمتان   هنا ", "كلمتان هنا"),
        ("PHARMACY", "pharmacy"),
    ],
)
def test_the_python_fold(text: str, expected: str) -> None:
    assert normalize_arabic(text) == expected


@pytest.mark.django_db
@pytest.mark.parametrize("text", SAMPLES)
def test_the_database_folds_exactly_as_python_does(text: str) -> None:
    with connection.cursor() as cursor:
        cursor.execute("SELECT directory_normalize_ar(%s)", [text])
        (folded,) = cursor.fetchone()

    assert folded == normalize_arabic(text)


@pytest.mark.django_db
@pytest.mark.parametrize(
    "term",
    ["صيدلية الأمل", "صيدليه الامل", "صيدلية الامل", "صَيْدَلِيَّة", "الأمل", "amal", "AMAL"],
)
def test_every_common_spelling_finds_the_place(listed: Facility, term: str) -> None:
    assert _found(listed, term)


@pytest.mark.django_db
def test_the_address_folds_too(listed: Facility) -> None:
    assert _found(listed, "مستشفي الرحمه")


@pytest.mark.django_db
def test_an_unrelated_term_still_finds_nothing(listed: Facility) -> None:
    assert not _found(listed, "مخبز")


@pytest.mark.django_db
def test_like_wildcards_typed_by_a_person_are_literal(listed: Facility) -> None:
    assert not _found(listed, "%%")
    assert not _found(listed, "__")
