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

Specialties and services are the one exception to "no hard delete": an item no facility
lists yet can be deleted, since a facility listing it is the only thing that points at one.
An item in use is refused with 409 and retired with `active = False` instead.

Realtime invalidation is not called from here. `realtime/hooks.py` already listens on
`post_save` and `post_delete` for `Category`, `CategoryGroup`, `VerificationRequirement`
and `CategoryProvince`, so a save inside these transactions publishes after commit. Calling
it again here would emit every event twice.
"""

from typing import Any

from django.core.exceptions import ValidationError
from django.db import transaction
from django.db.models import ProtectedError, QuerySet

from audit.services import record_audit
from core.exceptions import ConflictError

from .models import Category, CategoryGroup, ServiceTag, Specialty, VerificationRequirement

IMMUTABLE_CATEGORY_FIELDS = ("code", "slug")


def _request_id(request: Any) -> str:
    return getattr(request, "request_id", "")


def _group_snapshot(group: CategoryGroup) -> dict[str, Any]:
    return {
        "code": group.code,
        "nameAr": group.name_ar,
        "nameEn": group.name_en,
        "iconKey": group.icon_key,
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
        icon_key=data.get("iconKey", ""),
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
    for wire, field in (("nameAr", "name_ar"), ("nameEn", "name_en"), ("iconKey", "icon_key")):
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


# --------------------------------------------------------------------------------------
# Specialties and services
#
# The choices behind the public specialty and service filters, and what an owner picks
# from. A specialty is scoped to one category or to a specialization, never both
# (`Specialty.clean`); a service belongs to one category. The scope is fixed at creation:
# moving an item would carry the facilities that list it somewhere they may not belong.
# --------------------------------------------------------------------------------------

SCOPE_CATEGORY = "CATEGORY"
SCOPE_SPECIALIZATION = "SPECIALIZATION"
#: The two scopes of a specialty, as the wire names them (`SpecialtyScopeEnum`).
SPECIALTY_SCOPES = [
    (SCOPE_CATEGORY, "One category"),
    (SCOPE_SPECIALIZATION, "Every category of a specialization"),
]

#: Wire name -> column, for the fields an operator may change after creation.
TAG_FIELDS = (
    ("nameAr", "name_ar"),
    ("nameEn", "name_en"),
    ("active", "active"),
    ("sortOrder", "sort_order"),
)


def specialty_scope(specialty: Specialty) -> str:
    return SCOPE_CATEGORY if specialty.category_id else SCOPE_SPECIALIZATION


def _specialty_snapshot(specialty: Specialty) -> dict[str, Any]:
    return {
        "scope": specialty_scope(specialty),
        "categoryId": str(specialty.category_id) if specialty.category_id else None,
        "specialization": specialty.specialization or None,
        "nameAr": specialty.name_ar,
        "nameEn": specialty.name_en,
        "active": specialty.active,
        "sortOrder": specialty.sort_order,
    }


def _service_tag_snapshot(tag: ServiceTag) -> dict[str, Any]:
    return {
        "categoryId": str(tag.category_id),
        "nameAr": tag.name_ar,
        "nameEn": tag.name_en,
        "active": tag.active,
        "sortOrder": tag.sort_order,
    }


def _apply_tag_fields(row: Specialty | ServiceTag, data: dict[str, Any]) -> None:
    for wire, column in TAG_FIELDS:
        if wire in data:
            setattr(row, column, data[wire])


def _refuse_duplicate_name(
    row: Specialty | ServiceTag, siblings: QuerySet[Specialty] | QuerySet[ServiceTag]
) -> None:
    """Two items of one name in one scope would be two identical filter choices.

    Retired items count too: bringing one back is the operator's way to reuse its name.
    """
    if siblings.exclude(pk=row.pk).filter(name_ar__iexact=row.name_ar.strip()).exists():
        raise ValidationError(
            {"nameAr": "An item with this name already exists here; reactivate it instead."}
        )


def _specialty_siblings(specialty: Specialty) -> QuerySet[Specialty]:
    if specialty.category_id:
        return Specialty.objects.filter(category_id=specialty.category_id)
    return Specialty.objects.filter(
        category__isnull=True, specialization=specialty.specialization
    )


@transaction.atomic
def create_specialty(*, request: Any, category: Category, data: dict[str, Any]) -> Specialty:
    """A specialty for `category` alone, or for every category of its specialization."""
    if data["scope"] == SCOPE_SPECIALIZATION:
        if category.specialization == Category.Specialization.GENERIC:
            raise ValidationError(
                {"scope": "A general category has no specialization to share with."}
            )
        specialty = Specialty(specialization=category.specialization)
    else:
        specialty = Specialty(category=category)
    _apply_tag_fields(specialty, data)
    specialty.full_clean()
    _refuse_duplicate_name(specialty, _specialty_siblings(specialty))
    specialty.save()
    record_audit(
        actor=request.user,
        action="specialty.created",
        target=specialty,
        after_snapshot=_specialty_snapshot(specialty),
        request_id=_request_id(request),
    )
    return specialty


@transaction.atomic
def update_specialty(*, request: Any, specialty_id: int, data: dict[str, Any]) -> Specialty:
    specialty = Specialty.objects.select_for_update().get(pk=specialty_id)
    before = _specialty_snapshot(specialty)
    _apply_tag_fields(specialty, data)
    specialty.full_clean()
    if "nameAr" in data:
        _refuse_duplicate_name(specialty, _specialty_siblings(specialty))
    specialty.save()
    record_audit(
        actor=request.user,
        action="specialty.updated",
        target=specialty,
        before_snapshot=before,
        after_snapshot=_specialty_snapshot(specialty),
        request_id=_request_id(request),
    )
    return specialty


@transaction.atomic
def delete_specialty(*, request: Any, specialty_id: int) -> None:
    """Delete a specialty no facility lists; one in use is refused with SPECIALTY_IN_USE."""
    # Locked, so a facility cannot start listing it between the check and the delete.
    specialty = Specialty.objects.select_for_update().get(pk=specialty_id)
    in_use = ConflictError(
        "SPECIALTY_IN_USE",
        message="هذا التخصص مسجّل لدى منشآت ولا يمكن حذفه. أوقفه بدلاً من ذلك.",
    )
    if specialty.facility_links.exists():
        raise in_use
    # Recorded first, while the row still has its id; a failed delete rolls it back.
    record_audit(
        actor=request.user,
        action="specialty.deleted",
        target=specialty,
        before_snapshot=_specialty_snapshot(specialty),
        request_id=_request_id(request),
    )
    try:
        specialty.delete()
    except ProtectedError as exc:
        raise in_use from exc


@transaction.atomic
def create_service_tag(*, request: Any, category: Category, data: dict[str, Any]) -> ServiceTag:
    tag = ServiceTag(category=category)
    _apply_tag_fields(tag, data)
    tag.full_clean()
    _refuse_duplicate_name(tag, ServiceTag.objects.filter(category=category))
    tag.save()
    record_audit(
        actor=request.user,
        action="service_tag.created",
        target=tag,
        after_snapshot=_service_tag_snapshot(tag),
        request_id=_request_id(request),
    )
    return tag


@transaction.atomic
def update_service_tag(*, request: Any, service_tag_id: int, data: dict[str, Any]) -> ServiceTag:
    tag = ServiceTag.objects.select_for_update().get(pk=service_tag_id)
    before = _service_tag_snapshot(tag)
    _apply_tag_fields(tag, data)
    tag.full_clean()
    if "nameAr" in data:
        _refuse_duplicate_name(tag, ServiceTag.objects.filter(category_id=tag.category_id))
    tag.save()
    record_audit(
        actor=request.user,
        action="service_tag.updated",
        target=tag,
        before_snapshot=before,
        after_snapshot=_service_tag_snapshot(tag),
        request_id=_request_id(request),
    )
    return tag


@transaction.atomic
def delete_service_tag(*, request: Any, service_tag_id: int) -> None:
    """Delete a service no facility lists; one in use is refused with SERVICE_TAG_IN_USE."""
    tag = ServiceTag.objects.select_for_update().get(pk=service_tag_id)
    in_use = ConflictError(
        "SERVICE_TAG_IN_USE",
        message="هذه الخدمة مسجّلة لدى منشآت ولا يمكن حذفها. أوقفها بدلاً من ذلك.",
    )
    if tag.facility_links.exists():
        raise in_use
    record_audit(
        actor=request.user,
        action="service_tag.deleted",
        target=tag,
        before_snapshot=_service_tag_snapshot(tag),
        request_id=_request_id(request),
    )
    try:
        tag.delete()
    except ProtectedError as exc:
        raise in_use from exc
