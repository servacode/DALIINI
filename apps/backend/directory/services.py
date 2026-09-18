"""Taxonomy mutations for the Admin, as `Cycle J` describes them.

`04-OPERATING-CYCLES.md` Cycle J is: create or configure a category, set capabilities, set
the verification policy, set the province switches, commit, invalidate. Everything here is
one of those steps. Nothing is a CRUD convenience.

Three rules the specification fixes and this module enforces:

* **`code` and `slug` are immutable.** `06-DATA-MODEL.md` marks both unique and immutable,
  and `09-ADMIN-NEXTJS.md` says to prevent changing them silently. An update that names
  either is refused rather than ignored — silently dropping a field the caller sent is how
  an operator comes to believe a rename worked.
* **No hard delete.** A category is referenced by facilities, applications and evidence
  under `PROTECT`, and the specification offers no delete for either taxonomy level.
  Retirement is `active = False`, which the visibility formula already honours.
* **Capability invariants stay in the model.** `CategoryCapabilities.clean()` decides
  whether duty is allowed; this module calls `full_clean()` and lets it.

Realtime invalidation is not called from here. `realtime/hooks.py` already listens on
`post_save` and `post_delete` for `Category`, `CategoryGroup`, `VerificationRequirement`
and `CategoryProvince`, so a save inside these transactions publishes after commit. Calling
it again here would emit every event twice.
"""

from typing import Any

from django.core.exceptions import ValidationError
from django.db import transaction

from audit.services import record_audit

from .models import Category, CategoryGroup, VerificationRequirement

IMMUTABLE_CATEGORY_FIELDS = ("code", "slug")


def _request_id(request: Any) -> str:
    return getattr(request, "request_id", "")


def _group_snapshot(group: CategoryGroup) -> dict[str, Any]:
    return {
        "code": group.code,
        "nameAr": group.name_ar,
        "nameEn": group.name_en,
        "active": group.active,
        "sortOrder": group.sort_order,
    }


def _category_snapshot(category: Category) -> dict[str, Any]:
    return {
        "code": category.code,
        "slug": category.slug,
        "groupId": str(category.group_id),
        "nameAr": category.name_ar,
        "nameEn": category.name_en,
        "iconKey": category.icon_key,
        "specialization": category.specialization,
        "active": category.active,
        "sortOrder": category.sort_order,
    }


def _requirement_snapshot(requirement: VerificationRequirement) -> dict[str, Any]:
    return {
        "categoryId": str(requirement.category_id),
        "labelAr": requirement.label_ar,
        "labelEn": requirement.label_en,
        "required": requirement.required,
        "active": requirement.active,
        "minFiles": requirement.min_files,
        "maxFiles": requirement.max_files,
        "sortOrder": requirement.sort_order,
    }


# --------------------------------------------------------------------------------------
# Category groups
# --------------------------------------------------------------------------------------


@transaction.atomic
def create_category_group(*, request: Any, data: dict[str, Any]) -> CategoryGroup:
    group = CategoryGroup(
        code=data["code"],
        name_ar=data["nameAr"],
        name_en=data.get("nameEn", ""),
        active=data.get("active", True),
        sort_order=data.get("sortOrder", 0),
    )
    group.full_clean()
    group.save()
    record_audit(
        actor=request.user,
        action="category_group.created",
        target=group,
        after_snapshot=_group_snapshot(group),
        request_id=_request_id(request),
    )
    return group


@transaction.atomic
def update_category_group(
    *, request: Any, group_id: Any, data: dict[str, Any]
) -> CategoryGroup:
    group = CategoryGroup.objects.select_for_update().get(pk=group_id)
    if "code" in data and data["code"] != group.code:
        raise ValidationError({"code": "The group code is immutable."})
    before = _group_snapshot(group)
    for wire, field in (("nameAr", "name_ar"), ("nameEn", "name_en")):
        if wire in data:
            setattr(group, field, data[wire])
    if "active" in data:
        group.active = bool(data["active"])
    if "sortOrder" in data:
        group.sort_order = int(data["sortOrder"])
    group.full_clean()
    group.save()
    record_audit(
        actor=request.user,
        action="category_group.updated",
        target=group,
        before_snapshot=before,
        after_snapshot=_group_snapshot(group),
        request_id=_request_id(request),
    )
    return group


# --------------------------------------------------------------------------------------
# Categories
# --------------------------------------------------------------------------------------


@transaction.atomic
def create_category(*, request: Any, data: dict[str, Any]) -> Category:
    category = Category(
        group=CategoryGroup.objects.get(pk=data["groupId"]),
        code=data["code"],
        slug=data["slug"],
        name_ar=data["nameAr"],
        name_en=data.get("nameEn", ""),
        icon_key=data.get("iconKey", ""),
        specialization=data.get("specialization", Category.Specialization.GENERIC),
        active=data.get("active", True),
        sort_order=data.get("sortOrder", 0),
    )
    category.full_clean()
    category.save()
    record_audit(
        actor=request.user,
        action="category.created",
        target=category,
        after_snapshot=_category_snapshot(category),
        request_id=_request_id(request),
    )
    return category


@transaction.atomic
def update_category(
    *, request: Any, category_id: Any, data: dict[str, Any]
) -> Category:
    category = Category.objects.select_for_update().get(pk=category_id)
    refused = {
        field: f"The category {field} is immutable."
        for field in IMMUTABLE_CATEGORY_FIELDS
        if field in data and data[field] != getattr(category, field)
    }
    if refused:
        raise ValidationError(refused)

    before = _category_snapshot(category)
    if "groupId" in data:
        # "Move category" in 09-ADMIN-NEXTJS.md: the group is mutable, the identity is not.
        category.group = CategoryGroup.objects.get(pk=data["groupId"])
    for wire, field in (
        ("nameAr", "name_ar"),
        ("nameEn", "name_en"),
        ("iconKey", "icon_key"),
        ("specialization", "specialization"),
    ):
        if wire in data:
            setattr(category, field, data[wire])
    if "active" in data:
        category.active = bool(data["active"])
    if "sortOrder" in data:
        category.sort_order = int(data["sortOrder"])

    category.full_clean()
    category.save()
    # Changing the specialization can invalidate a capability set that was valid before —
    # duty on a category that is no longer a pharmacy, for instance. The capability row
    # owns that rule, so it is re-validated rather than re-implemented here.
    capabilities = getattr(category, "capabilities", None)
    if capabilities is not None:
        capabilities.full_clean()

    record_audit(
        actor=request.user,
        action="category.updated",
        target=category,
        before_snapshot=before,
        after_snapshot=_category_snapshot(category),
        request_id=_request_id(request),
    )
    return category


# --------------------------------------------------------------------------------------
# Verification requirements
#
# The tool, not the policy. LAUNCH_POLICY_PENDING stays open: what a pharmacy must supply
# is decided by whoever operates the platform, through this surface, and is not settled by
# the existence of the surface.
# --------------------------------------------------------------------------------------


@transaction.atomic
def create_verification_requirement(
    *, request: Any, data: dict[str, Any]
) -> VerificationRequirement:
    requirement = VerificationRequirement(
        category=Category.objects.get(pk=data["categoryId"]),
        label_ar=data["labelAr"],
        label_en=data.get("labelEn", ""),
        instructions_ar=data.get("instructionsAr", ""),
        instructions_en=data.get("instructionsEn", ""),
        required=data.get("required", True),
        active=data.get("active", True),
        min_files=data.get("minFiles", 1),
        max_files=data.get("maxFiles", 1),
        sort_order=data.get("sortOrder", 0),
    )
    requirement.full_clean()
    requirement.save()
    record_audit(
        actor=request.user,
        action="verification_requirement.created",
        target=requirement,
        after_snapshot=_requirement_snapshot(requirement),
        request_id=_request_id(request),
    )
    return requirement


@transaction.atomic
def update_verification_requirement(
    *, request: Any, requirement_id: Any, data: dict[str, Any]
) -> VerificationRequirement:
    requirement = VerificationRequirement.objects.select_for_update().get(pk=requirement_id)
    if "categoryId" in data and str(data["categoryId"]) != str(requirement.category_id):
        # Evidence rows point at (facility, requirement). Moving a requirement between
        # categories would leave that evidence attached to a rule its facility never had.
        raise ValidationError({"categoryId": "A requirement cannot move between categories."})

    before = _requirement_snapshot(requirement)
    for wire, field in (
        ("labelAr", "label_ar"),
        ("labelEn", "label_en"),
        ("instructionsAr", "instructions_ar"),
        ("instructionsEn", "instructions_en"),
    ):
        if wire in data:
            setattr(requirement, field, data[wire])
    for wire, field in (("required", "required"), ("active", "active")):
        if wire in data:
            setattr(requirement, field, bool(data[wire]))
    for wire, field in (
        ("minFiles", "min_files"),
        ("maxFiles", "max_files"),
        ("sortOrder", "sort_order"),
    ):
        if wire in data:
            setattr(requirement, field, int(data[wire]))

    requirement.full_clean()
    requirement.save()
    record_audit(
        actor=request.user,
        action="verification_requirement.updated",
        target=requirement,
        before_snapshot=before,
        after_snapshot=_requirement_snapshot(requirement),
        request_id=_request_id(request),
    )
    return requirement
