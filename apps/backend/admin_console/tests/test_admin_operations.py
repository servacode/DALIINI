"""The operations Cycle J and Cycle K need, and the invariants they must not break.

Cycle J is create/configure a category, set capabilities, set the verification policy, set
the province switches, commit, invalidate. Cycle K is the advertisement loop. Everything
here exercises one of those steps against a real database.
"""

from datetime import timedelta
from typing import Any

import pytest
from django.utils import timezone
from rest_framework.test import APIClient

from accounts.models import AdminPermission, AdminRole, User, UserAdminRole
from audit.models import AuditEvent
from content_services.models import Advertisement
from directory.models import Category, CategoryCapabilities, CategoryGroup, CategoryProvince
from directory.reference_data import launch_v1
from facilities.models import Facility
from locations.models import Province

ALL_PERMISSIONS = (
    "admin.taxonomy.read",
    "admin.taxonomy.manage",
    "admin.verification.read",
    "admin.verification.manage",
    "admin.ads.read",
    "admin.ads.manage",
    "admin.provinces.read",
    "admin.provinces.manage",
    "admin.audit.read",
)


@pytest.fixture
def operator(db: Any, user: User) -> APIClient:
    role = AdminRole.objects.create(code="ops", name="Operations")
    for code in ALL_PERMISSIONS:
        role.permissions.add(AdminPermission.objects.get_or_create(code=code)[0])
    UserAdminRole.objects.create(user=user, role=role, active=True)
    client = APIClient()
    client.force_authenticate(user=user)
    return client


@pytest.fixture
def health_group() -> CategoryGroup:
    return CategoryGroup.objects.get(code="health")


def _pharmacy() -> Category:
    return Category.objects.get(code="pharmacy")


# --------------------------------------------------------------------------------------
# Category groups
# --------------------------------------------------------------------------------------


@pytest.mark.django_db
def test_a_group_can_be_created_and_renamed(operator: APIClient) -> None:
    created = operator.post(
        "/api/v1/admin/category-groups/create/",
        {"code": "commerce", "nameAr": "تجارة", "sortOrder": 5},
        format="json",
    )
    assert created.status_code == 201, created.content
    group_id = created.json()["id"]

    updated = operator.put(
        f"/api/v1/admin/category-groups/{group_id}/",
        {"nameAr": "تجارة وخدمات", "active": False},
        format="json",
    )

    assert updated.status_code == 200, updated.content
    body = updated.json()
    assert body["nameAr"] == "تجارة وخدمات"
    assert body["active"] is False
    assert body["code"] == "commerce"


@pytest.mark.django_db
def test_a_group_code_cannot_be_changed(operator: APIClient, health_group: CategoryGroup) -> None:
    response = operator.put(
        f"/api/v1/admin/category-groups/{health_group.pk}/",
        {"code": "renamed", "nameAr": "الصحة"},
        format="json",
    )

    assert response.status_code == 400
    assert "code" in response.json()["details"]
    health_group.refresh_from_db()
    assert health_group.code == "health"


@pytest.mark.django_db
def test_creating_a_group_is_audited(operator: APIClient) -> None:
    operator.post(
        "/api/v1/admin/category-groups/create/",
        {"code": "services", "nameAr": "خدمات"},
        format="json",
    )

    assert AuditEvent.objects.filter(action="category_group.created").exists()


# --------------------------------------------------------------------------------------
# Categories
# --------------------------------------------------------------------------------------


@pytest.mark.django_db
def test_a_category_can_be_created(operator: APIClient, health_group: CategoryGroup) -> None:
    response = operator.post(
        "/api/v1/admin/categories/create/",
        {
            "groupId": str(health_group.pk),
            "code": "dental-clinic",
            "slug": "dental-clinic",
            "nameAr": "عيادات أسنان",
            "specialization": "MEDICAL_CLINIC",
            "sortOrder": 9,
        },
        format="json",
    )

    assert response.status_code == 201, response.content
    category = Category.objects.get(code="dental-clinic")
    assert category.specialization == "MEDICAL_CLINIC"
    assert category.group_id == health_group.pk


@pytest.mark.django_db
def test_a_new_category_is_invisible_until_its_switches_are_on(
    operator: APIClient, health_group: CategoryGroup
) -> None:
    """Cycle J: creating a category must not expose it anywhere by itself."""
    operator.post(
        "/api/v1/admin/categories/create/",
        {
            "groupId": str(health_group.pk),
            "code": "optics",
            "slug": "optics",
            "nameAr": "بصريات",
        },
        format="json",
    )
    raqqa = launch_v1.reference_id("province", "raqqa")

    public = APIClient().get(f"/api/v1/public/provinces/{raqqa}/categories/").json()

    assert [item["nameAr"] for item in public["items"]] == ["صيدليات"]


@pytest.mark.django_db
@pytest.mark.parametrize("field", ["code", "slug"])
def test_a_category_identity_cannot_be_changed(operator: APIClient, field: str) -> None:
    pharmacy = _pharmacy()
    original = getattr(pharmacy, field)

    response = operator.put(
        f"/api/v1/admin/categories/{pharmacy.pk}/",
        {field: "something-else"},
        format="json",
    )

    assert response.status_code == 400, response.content
    assert field in response.json()["details"]
    pharmacy.refresh_from_db()
    assert getattr(pharmacy, field) == original


@pytest.mark.django_db
def test_a_category_can_be_renamed_moved_and_deactivated(
    operator: APIClient, health_group: CategoryGroup
) -> None:
    other = CategoryGroup.objects.create(code="other", name_ar="أخرى")
    laboratory = Category.objects.get(code="medical-laboratory")

    response = operator.put(
        f"/api/v1/admin/categories/{laboratory.pk}/",
        {"nameAr": "مخابر", "groupId": str(other.pk), "active": False, "sortOrder": 7},
        format="json",
    )

    assert response.status_code == 200, response.content
    laboratory.refresh_from_db()
    assert (laboratory.name_ar, laboratory.group_id, laboratory.active) == (
        "مخابر",
        other.pk,
        False,
    )


@pytest.mark.django_db
def test_a_duty_category_cannot_be_moved_off_pharmacy(operator: APIClient) -> None:
    """The capability invariant holds even when the change comes from the other side."""
    pharmacy = _pharmacy()
    assert pharmacy.capabilities.supports_duty is True

    response = operator.put(
        f"/api/v1/admin/categories/{pharmacy.pk}/",
        {"specialization": "GENERIC"},
        format="json",
    )

    assert response.status_code == 400, response.content
    pharmacy.refresh_from_db()
    assert pharmacy.specialization == "PHARMACY"


@pytest.mark.django_db
def test_there_is_no_delete_for_a_category(operator: APIClient) -> None:
    """A category is referenced by facilities under PROTECT; retirement is `active=false`."""
    response = operator.delete(f"/api/v1/admin/categories/{_pharmacy().pk}/")

    assert response.status_code == 405
    assert response.json()["code"] == "METHOD_NOT_ALLOWED"


# --------------------------------------------------------------------------------------
# INT-039 — capabilities on the wire
# --------------------------------------------------------------------------------------


@pytest.mark.django_db
def test_capabilities_are_camel_case_in_both_directions(operator: APIClient) -> None:
    laboratory = Category.objects.get(code="medical-laboratory")

    response = operator.put(
        f"/api/v1/admin/categories/{laboratory.pk}/capabilities/",
        {"supportsRatings": False, "supportsSpecialtyFilter": True},
        format="json",
    )

    assert response.status_code == 200, response.content
    body = response.json()
    assert not [key for key in body if "_" in key], body
    assert body["supportsRatings"] is False
    assert body["supportsSpecialtyFilter"] is True
    assert body["supportsHours"] is True, "an omitted flag must keep its value"
    capabilities = CategoryCapabilities.objects.get(category=laboratory)
    assert capabilities.supports_ratings is False
    assert capabilities.supports_specialty_filter is True


@pytest.mark.django_db
def test_the_old_snake_case_capability_names_no_longer_work(operator: APIClient) -> None:
    laboratory = Category.objects.get(code="medical-laboratory")

    operator.put(
        f"/api/v1/admin/categories/{laboratory.pk}/capabilities/",
        {"supports_ratings": False},
        format="json",
    )

    assert CategoryCapabilities.objects.get(category=laboratory).supports_ratings is True


@pytest.mark.django_db
def test_duty_is_refused_for_a_non_pharmacy_category(operator: APIClient) -> None:
    laboratory = Category.objects.get(code="medical-laboratory")

    response = operator.put(
        f"/api/v1/admin/categories/{laboratory.pk}/capabilities/",
        {"supportsDuty": True},
        format="json",
    )

    assert response.status_code == 400, response.content
    assert CategoryCapabilities.objects.get(category=laboratory).supports_duty is False


# --------------------------------------------------------------------------------------
# Cycle J end to end: a province switch changes what the public sees
# --------------------------------------------------------------------------------------


@pytest.mark.django_db
def test_turning_a_province_switch_on_and_off_moves_the_public_listing(
    operator: APIClient,
) -> None:
    raqqa = launch_v1.reference_id("province", "raqqa")
    laboratory = Category.objects.get(code="medical-laboratory")
    public = APIClient()

    def visible() -> list[str]:
        body = public.get(f"/api/v1/public/provinces/{raqqa}/categories/").json()
        return [item["nameAr"] for item in body["items"]]

    assert "مخابر طبية" not in visible()

    on = operator.put(
        f"/api/v1/admin/categories/{laboratory.pk}/provinces/",
        {"provinceId": str(raqqa), "publicEnabled": True, "ownerRegistrationEnabled": True},
        format="json",
    )
    assert on.status_code in (200, 201), on.content
    assert "مخابر طبية" in visible()

    off = operator.put(
        f"/api/v1/admin/categories/{laboratory.pk}/provinces/",
        {"provinceId": str(raqqa), "publicEnabled": False, "ownerRegistrationEnabled": False},
        format="json",
    )
    assert off.status_code in (200, 201), off.content
    assert "مخابر طبية" not in visible()

    switch = CategoryProvince.objects.get(category=laboratory, province_id=raqqa)
    assert (switch.public_enabled, switch.owner_registration_enabled) == (False, False)


@pytest.mark.django_db
def test_activating_a_province_changes_the_public_province_list(operator: APIClient) -> None:
    aleppo = launch_v1.reference_id("province", "aleppo")
    public = APIClient()

    assert [p["code"] for p in public.get("/api/v1/public/provinces/").json()["items"]] == [
        "raqqa"
    ]

    response = operator.put(
        f"/api/v1/admin/provinces/{aleppo}/", {"active": True}, format="json"
    )

    assert response.status_code == 200, response.content
    codes = [p["code"] for p in public.get("/api/v1/public/provinces/").json()["items"]]
    assert set(codes) == {"raqqa", "aleppo"}
    assert Province.objects.get(pk=aleppo).active is True
    assert AuditEvent.objects.filter(action="province.updated").exists()


# --------------------------------------------------------------------------------------
# Verification requirements — the tool, not the policy
# --------------------------------------------------------------------------------------


@pytest.mark.django_db
def test_a_requirement_can_be_created_edited_and_retired(operator: APIClient) -> None:
    created = operator.post(
        "/api/v1/admin/verification-requirements/",
        {
            "categoryId": str(_pharmacy().pk),
            "labelAr": "صورة الواجهة",
            "required": True,
            "minFiles": 1,
            "maxFiles": 3,
        },
        format="json",
    )
    assert created.status_code == 201, created.content
    requirement_id = created.json()["id"]

    edited = operator.put(
        f"/api/v1/admin/verification-requirements/{requirement_id}/",
        {"labelAr": "صورة واجهة الصيدلية", "maxFiles": 5},
        format="json",
    )
    assert edited.status_code == 200, edited.content
    assert edited.json()["labelAr"] == "صورة واجهة الصيدلية"
    assert edited.json()["maxFiles"] == 5

    retired = operator.put(
        f"/api/v1/admin/verification-requirements/{requirement_id}/",
        {"active": False},
        format="json",
    )
    assert retired.status_code == 200
    assert retired.json()["active"] is False
    assert AuditEvent.objects.filter(action="verification_requirement.updated").exists()


@pytest.mark.django_db
def test_a_requirement_cannot_move_between_categories(operator: APIClient) -> None:
    created = operator.post(
        "/api/v1/admin/verification-requirements/",
        {"categoryId": str(_pharmacy().pk), "labelAr": "سجل تجاري"},
        format="json",
    )
    requirement_id = created.json()["id"]
    laboratory = Category.objects.get(code="medical-laboratory")

    response = operator.put(
        f"/api/v1/admin/verification-requirements/{requirement_id}/",
        {"categoryId": str(laboratory.pk)},
        format="json",
    )

    assert response.status_code == 400
    assert "categoryId" in response.json()["details"]


@pytest.mark.django_db
def test_max_files_below_min_files_is_refused(operator: APIClient) -> None:
    created = operator.post(
        "/api/v1/admin/verification-requirements/",
        {"categoryId": str(_pharmacy().pk), "labelAr": "وثيقة", "minFiles": 2, "maxFiles": 4},
        format="json",
    )

    response = operator.put(
        f"/api/v1/admin/verification-requirements/{created.json()['id']}/",
        {"maxFiles": 1},
        format="json",
    )

    assert response.status_code == 400, response.content


@pytest.mark.django_db
def test_the_launch_baseline_still_seeds_no_requirement() -> None:
    """LAUNCH_POLICY_PENDING stays open. The tool exists; the policy is not decided here."""
    assert launch_v1.VERIFICATION_REQUIREMENTS == ()
    assert _pharmacy().verification_requirements.count() == 0


# --------------------------------------------------------------------------------------
# Advertisements
# --------------------------------------------------------------------------------------


def _ad_payload(**overrides: Any) -> dict[str, Any]:
    now = timezone.now()
    payload: dict[str, Any] = {
        "imageKey": "ads/banner.jpg",
        "titleAr": "إعلان",
        "targetScope": "GLOBAL",
        "startsAt": now.isoformat(),
        "endsAt": (now + timedelta(days=7)).isoformat(),
        "enabled": False,
        "slideDurationMs": 5000,
    }
    payload.update(overrides)
    return payload


@pytest.mark.django_db
def test_an_advertisement_can_be_created_then_edited(operator: APIClient) -> None:
    created = operator.post("/api/v1/admin/ads/", _ad_payload(), format="json")
    assert created.status_code == 201, created.content
    ad_id = created.json()["id"]

    response = operator.put(
        f"/api/v1/admin/ads/{ad_id}/",
        {"titleAr": "إعلان محدّث", "enabled": True, "sortOrder": 3},
        format="json",
    )

    assert response.status_code == 200, response.content
    ad = Advertisement.objects.get(pk=ad_id)
    assert (ad.title_ar, ad.enabled, ad.sort_order) == ("إعلان محدّث", True, 3)
    assert ad.image_key == "ads/banner.jpg", "an omitted field must keep its value"
    assert AuditEvent.objects.filter(action="advertisement.updated").exists()


@pytest.mark.django_db
def test_an_end_before_its_start_is_refused(operator: APIClient) -> None:
    created = operator.post("/api/v1/admin/ads/", _ad_payload(), format="json")
    now = timezone.now()

    response = operator.put(
        f"/api/v1/admin/ads/{created.json()['id']}/",
        {"startsAt": now.isoformat(), "endsAt": (now - timedelta(days=1)).isoformat()},
        format="json",
    )

    assert response.status_code == 400, response.content


@pytest.mark.django_db
def test_a_global_advertisement_cannot_carry_a_target(operator: APIClient) -> None:
    created = operator.post("/api/v1/admin/ads/", _ad_payload(), format="json")
    raqqa = launch_v1.reference_id("province", "raqqa")

    response = operator.put(
        f"/api/v1/admin/ads/{created.json()['id']}/",
        {"targetScope": "GLOBAL", "provinceId": str(raqqa)},
        format="json",
    )

    assert response.status_code == 400, response.content


@pytest.mark.django_db
def test_a_slide_duration_outside_its_bounds_is_refused(operator: APIClient) -> None:
    created = operator.post("/api/v1/admin/ads/", _ad_payload(), format="json")

    response = operator.put(
        f"/api/v1/admin/ads/{created.json()['id']}/",
        {"slideDurationMs": 100},
        format="json",
    )

    assert response.status_code == 400, response.content


@pytest.mark.django_db
def test_a_disabled_advertisement_is_not_served_publicly(
    operator: APIClient, facility: Facility
) -> None:
    raqqa = launch_v1.reference_id("province", "raqqa")
    operator.post("/api/v1/admin/ads/", _ad_payload(titleAr="مخفي"), format="json")

    body = APIClient().get("/api/v1/public/home/", {"provinceId": str(raqqa)}).json()

    assert body["ads"] == []


# --------------------------------------------------------------------------------------
# Every new mutation is permission-gated
# --------------------------------------------------------------------------------------


@pytest.mark.django_db
@pytest.mark.parametrize(
    ("method", "path", "body"),
    [
        ("post", "/api/v1/admin/category-groups/create/", {"code": "x", "nameAr": "x"}),
        ("post", "/api/v1/admin/categories/create/", {"code": "x", "slug": "x", "nameAr": "x"}),
        ("post", "/api/v1/admin/ads/", {"imageKey": "x"}),
    ],
)
def test_a_reader_cannot_mutate(
    user: User, method: str, path: str, body: dict[str, Any]
) -> None:
    role = AdminRole.objects.create(code="reader", name="Reader")
    for code in ("admin.taxonomy.read", "admin.ads.read"):
        role.permissions.add(AdminPermission.objects.get_or_create(code=code)[0])
    UserAdminRole.objects.create(user=user, role=role, active=True)
    client = APIClient()
    client.force_authenticate(user=user)

    response = getattr(client, method)(path, body, format="json")

    assert response.status_code == 403
    assert response.json()["code"] == "PERMISSION_DENIED"


# --------------------------------------------------------------------------------------
# INT-044 — the schedule travels on create, not only on update
# --------------------------------------------------------------------------------------


@pytest.mark.django_db
def test_a_new_advertisement_keeps_its_schedule(operator: APIClient) -> None:
    payload = _ad_payload()

    created = operator.post("/api/v1/admin/ads/", payload, format="json")

    assert created.status_code == 201, created.content
    ad = Advertisement.objects.get(pk=created.json()["id"])
    assert ad.starts_at is not None and ad.ends_at is not None
    assert ad.ends_at > ad.starts_at


@pytest.mark.django_db
def test_creating_an_advertisement_that_ends_before_it_starts_is_refused(
    operator: APIClient,
) -> None:
    now = timezone.now()

    response = operator.post(
        "/api/v1/admin/ads/",
        _ad_payload(startsAt=now.isoformat(), endsAt=(now - timedelta(days=1)).isoformat()),
        format="json",
    )

    assert response.status_code == 400, response.content
    assert not Advertisement.objects.exists()


def test_the_create_contract_declares_the_schedule() -> None:
    from admin_console.schemas import AdminAdvertisementRequestSerializer

    fields = AdminAdvertisementRequestSerializer().fields

    assert "startsAt" in fields and "endsAt" in fields


# --------------------------------------------------------------------------------------
# INT-015 — a missing resource is a 404, as the contract says, never a 500
# --------------------------------------------------------------------------------------

MISSING = "00000000-0000-4000-8000-000000000000"


@pytest.fixture
def full_operator(db: Any, user: User) -> APIClient:
    role = AdminRole.objects.create(code="everything", name="Everything")
    for permission in AdminPermission.objects.all():
        role.permissions.add(permission)
    UserAdminRole.objects.create(user=user, role=role, active=True)
    client = APIClient()
    client.force_authenticate(user=user)
    return client


@pytest.mark.django_db
@pytest.mark.parametrize(
    ("method", "path"),
    [
        ("get", f"/api/v1/admin/evidence/{MISSING}/content/"),
        ("get", f"/api/v1/admin/facilities/{MISSING}/"),
        ("get", f"/api/v1/admin/users/{MISSING}/"),
        ("post", f"/api/v1/admin/users/{MISSING}/block/"),
        ("put", f"/api/v1/admin/users/{MISSING}/roles/"),
        ("put", f"/api/v1/admin/categories/{MISSING}/capabilities/"),
        ("put", f"/api/v1/admin/provinces/{MISSING}/"),
        ("put", f"/api/v1/admin/ads/{MISSING}/"),
        ("delete", f"/api/v1/admin/ads/{MISSING}/"),
        ("post", f"/api/v1/admin/applications/{MISSING}/approve/"),
        ("post", f"/api/v1/admin/facilities/{MISSING}/suspend/"),
    ],
)
def test_a_missing_resource_is_not_found(
    full_operator: APIClient, method: str, path: str
) -> None:
    response = getattr(full_operator, method)(path, {}, format="json")

    assert response.status_code == 404, (path, response.status_code, response.content)
    assert response.json()["code"] == "NOT_FOUND"


@pytest.mark.django_db
def test_evidence_is_refused_before_it_is_looked_up(user: User) -> None:
    """An operator without `admin.evidence.read` learns nothing, not even whether it exists."""
    role = AdminRole.objects.create(code="desk", name="Desk")
    role.permissions.add(AdminPermission.objects.get_or_create(code="admin.reviews.read")[0])
    UserAdminRole.objects.create(user=user, role=role, active=True)
    client = APIClient()
    client.force_authenticate(user=user)

    response = client.get(f"/api/v1/admin/evidence/{MISSING}/content/")

    assert response.status_code == 403
    assert response.json()["code"] == "PERMISSION_DENIED"
