"""Specialties and services in the Admin: who may see and change them, and what is refused.

A specialty is scoped to one category or to a specialization; a service to one category.
Reads need admin.taxonomy.read and writes admin.taxonomy.manage. An item a facility lists
cannot be deleted (409) and is retired with `active = false` instead. Every write is audited.
"""

from collections.abc import Callable
from typing import Any

import pytest

from audit.models import AuditEvent
from directory.models import Category, CategoryGroup, ServiceTag, Specialty
from facilities.models import Facility, FacilityServiceTag, FacilitySpecialty

READ = "admin.taxonomy.read"
MANAGE = "admin.taxonomy.manage"
MISSING = "00000000-0000-4000-8000-000000000000"

AdminApi = Callable[..., Any]


@pytest.fixture
def clinics(db: None) -> Category:
    group, _ = CategoryGroup.objects.get_or_create(code="tags-admin", defaults={"name_ar": "صحة"})
    return Category.objects.create(
        group=group,
        code="tags-clinics",
        slug="tags-clinics",
        name_ar="عيادات",
        specialization=Category.Specialization.MEDICAL_CLINIC,
    )


@pytest.fixture
def dental(clinics: Category) -> Category:
    return Category.objects.create(
        group=clinics.group,
        code="tags-dental",
        slug="tags-dental",
        name_ar="عيادات أسنان",
        specialization=Category.Specialization.MEDICAL_CLINIC,
    )


@pytest.fixture
def manager(admin_api: AdminApi) -> Any:
    return admin_api(READ, MANAGE)


def specialties_url(category: Category | str) -> str:
    key = category if isinstance(category, str) else category.pk
    return f"/api/v1/admin/categories/{key}/specialties/"


def services_url(category: Category | str) -> str:
    key = category if isinstance(category, str) else category.pk
    return f"/api/v1/admin/categories/{key}/service-tags/"


def listed_by(facility: Facility, item: Specialty | ServiceTag) -> None:
    if isinstance(item, Specialty):
        FacilitySpecialty.objects.create(facility=facility, specialty=item)
    else:
        FacilityServiceTag.objects.create(facility=facility, service_tag=item)


# --------------------------------------------------------------------------------------
# Permissions
# --------------------------------------------------------------------------------------


@pytest.mark.django_db
def test_listing_needs_the_taxonomy_read_permission(
    admin_api: AdminApi, clinics: Category
) -> None:
    outsider = admin_api("admin.ads.read")

    for url in (specialties_url(clinics), services_url(clinics)):
        response = outsider.get(url)
        assert response.status_code == 403
        assert response.json()["code"] == "PERMISSION_DENIED"
        assert admin_api(READ).get(url).status_code == 200


@pytest.mark.django_db
def test_a_reader_cannot_change_anything(admin_api: AdminApi, clinics: Category) -> None:
    reader = admin_api(READ)
    specialty = Specialty.objects.create(category=clinics, name_ar="قلبية")
    service = ServiceTag.objects.create(category=clinics, name_ar="تخطيط قلب")

    attempts = [
        reader.post(specialties_url(clinics), {"scope": "CATEGORY", "nameAr": "x"}, format="json"),
        reader.post(services_url(clinics), {"nameAr": "x"}, format="json"),
        reader.put(f"/api/v1/admin/specialties/{specialty.pk}/", {"active": False}, format="json"),
        reader.put(f"/api/v1/admin/service-tags/{service.pk}/", {"active": False}, format="json"),
        reader.delete(f"/api/v1/admin/specialties/{specialty.pk}/"),
        reader.delete(f"/api/v1/admin/service-tags/{service.pk}/"),
    ]

    assert [response.status_code for response in attempts] == [403] * 6
    assert Specialty.objects.get(pk=specialty.pk).active is True
    assert ServiceTag.objects.filter(pk=service.pk).exists()
    assert Specialty.objects.count() == 1 and ServiceTag.objects.count() == 1
    assert not AuditEvent.objects.filter(action__startswith="specialty.").exists()


@pytest.mark.django_db
def test_a_reader_learns_nothing_about_a_category_it_may_not_write_to(
    admin_api: AdminApi,
) -> None:
    response = admin_api(READ).post(
        specialties_url(MISSING), {"scope": "CATEGORY", "nameAr": "x"}, format="json"
    )

    assert response.status_code == 403


# --------------------------------------------------------------------------------------
# Listing
# --------------------------------------------------------------------------------------


@pytest.mark.django_db
def test_a_category_lists_both_scopes_in_order_with_their_use(
    manager: Any, clinics: Category, dental: Category, facility: Facility
) -> None:
    own = Specialty.objects.create(
        category=clinics, name_ar="قلبية", name_en="Cardiology", sort_order=20
    )
    shared = Specialty.objects.create(
        specialization="MEDICAL_CLINIC", name_ar="عامة", sort_order=10
    )
    retired = Specialty.objects.create(
        category=clinics, name_ar="قديمة", sort_order=30, active=False
    )
    Specialty.objects.create(category=dental, name_ar="تقويم")
    Specialty.objects.create(specialization="PHARMACY", name_ar="سريرية")
    listed_by(facility, own)

    items = manager.get(specialties_url(clinics)).json()["items"]

    assert [row["id"] for row in items] == [shared.pk, own.pk, retired.pk]
    assert items[1] == {
        "id": own.pk,
        "scope": "CATEGORY",
        "categoryId": str(clinics.pk),
        "specialization": None,
        "nameAr": "قلبية",
        "nameEn": "Cardiology",
        "active": True,
        "sortOrder": 20,
        "facilityCount": 1,
    }
    assert items[0]["scope"] == "SPECIALIZATION"
    assert items[0]["categoryId"] is None
    assert items[0]["specialization"] == "MEDICAL_CLINIC"
    assert items[0]["facilityCount"] == 0
    assert items[2]["active"] is False


@pytest.mark.django_db
def test_a_category_lists_its_own_services(
    manager: Any, clinics: Category, dental: Category, facility: Facility
) -> None:
    second = ServiceTag.objects.create(category=clinics, name_ar="تحاليل", sort_order=2)
    first = ServiceTag.objects.create(category=clinics, name_ar="تخطيط قلب", sort_order=1)
    ServiceTag.objects.create(category=dental, name_ar="تبييض")
    listed_by(facility, first)

    items = manager.get(services_url(clinics)).json()["items"]

    assert [row["id"] for row in items] == [first.pk, second.pk]
    assert set(items[0]) == {
        "id",
        "categoryId",
        "nameAr",
        "nameEn",
        "active",
        "sortOrder",
        "facilityCount",
    }
    assert [row["facilityCount"] for row in items] == [1, 0]


@pytest.mark.django_db
def test_an_unknown_category_is_not_found(manager: Any) -> None:
    assert manager.get(specialties_url(MISSING)).status_code == 404
    assert manager.get(services_url(MISSING)).status_code == 404
    response = manager.post(services_url(MISSING), {"nameAr": "x"}, format="json")
    assert response.status_code == 404


# --------------------------------------------------------------------------------------
# Creating, and the scope rules
# --------------------------------------------------------------------------------------


@pytest.mark.django_db
def test_a_specialty_can_be_scoped_to_the_category(manager: Any, clinics: Category) -> None:
    response = manager.post(
        specialties_url(clinics),
        {"scope": "CATEGORY", "nameAr": "  قلبية ", "nameEn": "Cardiology", "sortOrder": 5},
        format="json",
    )

    assert response.status_code == 201, response.content
    body = response.json()
    specialty = Specialty.objects.get(pk=body["id"])
    assert (specialty.category_id, specialty.specialization) == (clinics.pk, "")
    assert (specialty.name_ar, specialty.sort_order, specialty.active) == ("قلبية", 5, True)
    assert body["scope"] == "CATEGORY"
    assert body["facilityCount"] == 0


@pytest.mark.django_db
def test_a_specialty_can_be_shared_by_the_specialization(
    manager: Any, clinics: Category, dental: Category
) -> None:
    response = manager.post(
        specialties_url(clinics), {"scope": "SPECIALIZATION", "nameAr": "عامة"}, format="json"
    )

    assert response.status_code == 201, response.content
    specialty = Specialty.objects.get(pk=response.json()["id"])
    assert (specialty.category_id, specialty.specialization) == (None, "MEDICAL_CLINIC")
    # Every category of the specialization now offers it.
    assert [row["nameAr"] for row in manager.get(specialties_url(dental)).json()["items"]] == [
        "عامة"
    ]


@pytest.mark.django_db
def test_a_general_category_has_no_specialization_to_share(
    manager: Any, clinics: Category
) -> None:
    Category.objects.filter(pk=clinics.pk).update(specialization="GENERIC")

    response = manager.post(
        specialties_url(clinics), {"scope": "SPECIALIZATION", "nameAr": "عامة"}, format="json"
    )

    assert response.status_code == 400, response.content
    assert "scope" in response.json()["details"]
    assert not Specialty.objects.exists()


@pytest.mark.django_db
@pytest.mark.parametrize(
    ("body", "field"),
    [
        ({"scope": "CATEGORY"}, "nameAr"),
        ({"scope": "CATEGORY", "nameAr": "   "}, "nameAr"),
        ({"scope": "CATEGORY", "nameAr": "ا" * 121}, "nameAr"),
        ({"scope": "CATEGORY", "nameAr": "x", "nameEn": "e" * 121}, "nameEn"),
        ({"scope": "EVERYWHERE", "nameAr": "x"}, "scope"),
        ({"nameAr": "x"}, "scope"),
        ({"scope": "CATEGORY", "nameAr": "x", "sortOrder": -1}, "sortOrder"),
        ({"scope": "CATEGORY", "nameAr": "x", "sortOrder": "first"}, "sortOrder"),
    ],
)
def test_a_malformed_specialty_is_refused(
    manager: Any, clinics: Category, body: dict[str, Any], field: str
) -> None:
    response = manager.post(specialties_url(clinics), body, format="json")

    assert response.status_code == 400, response.content
    assert response.json()["code"] == "VALIDATION_ERROR"
    assert field in response.json()["details"]
    assert not Specialty.objects.exists()


@pytest.mark.django_db
def test_a_name_is_used_once_per_scope(manager: Any, clinics: Category, dental: Category) -> None:
    Specialty.objects.create(category=clinics, name_ar="قلبية", active=False)
    ServiceTag.objects.create(category=clinics, name_ar="تحاليل")

    twice = manager.post(
        specialties_url(clinics), {"scope": "CATEGORY", "nameAr": " قلبية"}, format="json"
    )
    elsewhere = manager.post(
        specialties_url(dental), {"scope": "CATEGORY", "nameAr": "قلبية"}, format="json"
    )
    service_twice = manager.post(services_url(clinics), {"nameAr": "تحاليل"}, format="json")

    assert twice.status_code == 400 and "nameAr" in twice.json()["details"]
    assert elsewhere.status_code == 201
    assert service_twice.status_code == 400 and "nameAr" in service_twice.json()["details"]


@pytest.mark.django_db
def test_a_service_is_created_for_the_category(manager: Any, clinics: Category) -> None:
    response = manager.post(
        services_url(clinics),
        {"nameAr": "تخطيط قلب", "active": False, "sortOrder": 3},
        format="json",
    )

    assert response.status_code == 201, response.content
    tag = ServiceTag.objects.get(pk=response.json()["id"])
    assert (tag.category_id, tag.name_ar, tag.active, tag.sort_order) == (
        clinics.pk,
        "تخطيط قلب",
        False,
        3,
    )


# --------------------------------------------------------------------------------------
# Editing
# --------------------------------------------------------------------------------------


@pytest.mark.django_db
def test_a_specialty_is_renamed_reordered_and_retired(manager: Any, clinics: Category) -> None:
    specialty = Specialty.objects.create(category=clinics, name_ar="قلبيه", name_en="Cardio")

    response = manager.put(
        f"/api/v1/admin/specialties/{specialty.pk}/",
        {"nameAr": "قلبية", "sortOrder": 7, "active": False},
        format="json",
    )

    assert response.status_code == 200, response.content
    specialty.refresh_from_db()
    assert (specialty.name_ar, specialty.sort_order, specialty.active) == ("قلبية", 7, False)
    assert specialty.name_en == "Cardio"
    assert response.json()["nameAr"] == "قلبية"


@pytest.mark.django_db
@pytest.mark.parametrize(
    "body",
    [
        {"scope": "SPECIALIZATION"},
        {"categoryId": MISSING},
        {"specialization": "PHARMACY", "nameAr": "x"},
    ],
)
def test_a_specialty_cannot_move(manager: Any, clinics: Category, body: dict[str, Any]) -> None:
    specialty = Specialty.objects.create(category=clinics, name_ar="قلبية")

    response = manager.put(f"/api/v1/admin/specialties/{specialty.pk}/", body, format="json")

    assert response.status_code == 400, response.content
    specialty.refresh_from_db()
    assert (specialty.category_id, specialty.specialization, specialty.name_ar) == (
        clinics.pk,
        "",
        "قلبية",
    )


@pytest.mark.django_db
def test_a_service_is_edited_but_stays_in_its_category(
    manager: Any, clinics: Category, dental: Category
) -> None:
    tag = ServiceTag.objects.create(category=clinics, name_ar="تحاليل")
    url = f"/api/v1/admin/service-tags/{tag.pk}/"

    moved = manager.put(url, {"categoryId": str(dental.pk)}, format="json")
    renamed = manager.put(url, {"nameAr": "تحاليل دم", "sortOrder": 4}, format="json")

    assert moved.status_code == 400
    assert renamed.status_code == 200, renamed.content
    tag.refresh_from_db()
    assert (tag.category_id, tag.name_ar, tag.sort_order) == (clinics.pk, "تحاليل دم", 4)


@pytest.mark.django_db
def test_a_rename_onto_a_sibling_is_refused(manager: Any, clinics: Category) -> None:
    Specialty.objects.create(category=clinics, name_ar="قلبية")
    other = Specialty.objects.create(category=clinics, name_ar="أطفال")

    response = manager.put(
        f"/api/v1/admin/specialties/{other.pk}/", {"nameAr": "قلبية"}, format="json"
    )
    unchanged = manager.put(
        f"/api/v1/admin/specialties/{other.pk}/", {"nameAr": "أطفال", "sortOrder": 2},
        format="json",
    )

    assert response.status_code == 400 and "nameAr" in response.json()["details"]
    assert unchanged.status_code == 200


@pytest.mark.django_db
def test_an_unknown_item_is_not_found(manager: Any) -> None:
    for url in ("/api/v1/admin/specialties/999999/", "/api/v1/admin/service-tags/999999/"):
        assert manager.put(url, {"active": False}, format="json").status_code == 404
        assert manager.delete(url).status_code == 404


# --------------------------------------------------------------------------------------
# Deleting: only what nobody uses
# --------------------------------------------------------------------------------------


@pytest.mark.django_db
def test_an_unused_item_is_deleted(manager: Any, clinics: Category) -> None:
    specialty = Specialty.objects.create(category=clinics, name_ar="قلبية")
    tag = ServiceTag.objects.create(category=clinics, name_ar="تحاليل")

    assert manager.delete(f"/api/v1/admin/specialties/{specialty.pk}/").status_code == 204
    assert manager.delete(f"/api/v1/admin/service-tags/{tag.pk}/").status_code == 204
    assert not Specialty.objects.exists()
    assert not ServiceTag.objects.exists()


@pytest.mark.django_db
def test_an_item_in_use_is_kept_and_named_by_its_code(
    manager: Any, clinics: Category, facility: Facility
) -> None:
    specialty = Specialty.objects.create(category=clinics, name_ar="قلبية")
    tag = ServiceTag.objects.create(category=clinics, name_ar="تحاليل")
    listed_by(facility, specialty)
    listed_by(facility, tag)

    refused = manager.delete(f"/api/v1/admin/specialties/{specialty.pk}/")
    refused_service = manager.delete(f"/api/v1/admin/service-tags/{tag.pk}/")

    assert refused.status_code == 409
    assert refused.json()["code"] == "SPECIALTY_IN_USE"
    assert refused_service.status_code == 409
    assert refused_service.json()["code"] == "SERVICE_TAG_IN_USE"
    assert Specialty.objects.filter(pk=specialty.pk).exists()
    assert ServiceTag.objects.filter(pk=tag.pk).exists()
    assert not AuditEvent.objects.filter(action__endswith=".deleted").exists()
    # The way out the message names: retire it.
    retired = manager.put(
        f"/api/v1/admin/specialties/{specialty.pk}/", {"active": False}, format="json"
    )
    assert retired.status_code == 200
    assert retired.json()["facilityCount"] == 1


# --------------------------------------------------------------------------------------
# Audit
# --------------------------------------------------------------------------------------


@pytest.mark.django_db
def test_every_write_is_audited(manager: Any, clinics: Category) -> None:
    created = manager.post(
        specialties_url(clinics), {"scope": "CATEGORY", "nameAr": "قلبية"}, format="json"
    ).json()
    manager.put(
        f"/api/v1/admin/specialties/{created['id']}/", {"active": False}, format="json"
    )
    manager.delete(f"/api/v1/admin/specialties/{created['id']}/")
    service = manager.post(services_url(clinics), {"nameAr": "تحاليل"}, format="json").json()
    manager.put(f"/api/v1/admin/service-tags/{service['id']}/", {"sortOrder": 2}, format="json")
    manager.delete(f"/api/v1/admin/service-tags/{service['id']}/")

    events = list(AuditEvent.objects.order_by("created_at"))

    assert [event.action for event in events] == [
        "specialty.created",
        "specialty.updated",
        "specialty.deleted",
        "service_tag.created",
        "service_tag.updated",
        "service_tag.deleted",
    ]
    assert {event.actor_id for event in events} == {manager.user.pk}
    assert [event.target_type for event in events] == ["Specialty"] * 3 + ["ServiceTag"] * 3
    assert {event.target_id for event in events[:3]} == {str(created["id"])}
    assert events[0].after_snapshot == {
        "scope": "CATEGORY",
        "categoryId": str(clinics.pk),
        "specialization": None,
        "nameAr": "قلبية",
        "nameEn": "",
        "active": True,
        "sortOrder": 0,
    }
    assert events[1].before_snapshot["active"] is True
    assert events[1].after_snapshot["active"] is False
    assert events[2].before_snapshot["nameAr"] == "قلبية"
    assert events[4].after_snapshot["sortOrder"] == 2


@pytest.mark.django_db
def test_a_refused_write_is_not_audited(manager: Any, clinics: Category) -> None:
    manager.post(specialties_url(clinics), {"scope": "CATEGORY"}, format="json")
    Category.objects.filter(pk=clinics.pk).update(specialization="GENERIC")
    manager.post(
        specialties_url(clinics), {"scope": "SPECIALIZATION", "nameAr": "عامة"}, format="json"
    )

    assert not AuditEvent.objects.exists()
