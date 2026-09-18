"""Write a reference dataset into the database, without ever undoing an operator.

This is the only implementation. The data migration and the `seed_launch_baseline`
management command both call `apply_dataset`, so the two cannot drift apart. They differ
only in which `get_model` they hand over: the migration passes `apps.get_model` from its
historical registry, the command passes the live one.

**Constraint that comes with that.** Because a historical model registry has no custom
methods, no `clean()`, no properties and no managers beyond the default, everything here
uses plain field access and the default manager only. Anything richer would work in the
command and fail in the migration.

Semantics, decided in the CONTRACT ALIGNMENT + LAUNCH BASELINE batch:

* **Create is where defaults apply.** A row that does not exist is created with the
  dataset's launch defaults and its deterministic UUID.
* **A row that exists is left alone.** `active`, `public_enabled` and
  `owner_registration_enabled` become operational state the moment the row exists, and an
  operator's decision outranks a re-run. Names and sort orders are not overwritten either,
  because the Admin owns those too.
* **Identity is verified, never rewritten.** If a row with the dataset's code already
  carries a different primary key, that is reported as `REFERENCE_ID_MISMATCH` and nothing
  is changed. Rewriting a primary key under live foreign keys is not something a seed may
  do quietly.

There is no force or reset mode. Reconciling a diverged production database is a separate,
audited operation, not a flag on a command anyone can run.
"""

from collections.abc import Callable
from dataclasses import dataclass, field
from typing import Any

# Both `apps.get_model` implementations: (app_label, model_name) -> model class.
GetModel = Callable[[str, str], Any]


class ReferenceIdMismatch(Exception):
    """A canonical row exists under the right code but the wrong primary key."""

    code = "REFERENCE_ID_MISMATCH"

    def __init__(self, entity: str, entity_code: str, expected: Any, actual: Any) -> None:
        self.entity = entity
        self.entity_code = entity_code
        self.expected = expected
        self.actual = actual
        super().__init__(
            f"{self.code}: {entity} {entity_code!r} should have id {expected} but has "
            f"{actual}. The seed will not rewrite a primary key that foreign keys may "
            f"already reference. Reconcile this deliberately."
        )


@dataclass
class Summary:
    """What a run did, for the command's output and for the qualification tests."""

    version: str = ""
    created: dict[str, int] = field(default_factory=dict)
    verified: dict[str, int] = field(default_factory=dict)

    def _bump(self, bucket: dict[str, int], entity: str) -> None:
        bucket[entity] = bucket.get(entity, 0) + 1

    def created_one(self, entity: str) -> None:
        self._bump(self.created, entity)

    def verified_one(self, entity: str) -> None:
        self._bump(self.verified, entity)

    def lines(self) -> list[str]:
        entities = sorted(set(self.created) | set(self.verified))
        return [
            f"{entity:20s} created {self.created.get(entity, 0):3d}  "
            f"left unchanged {self.verified.get(entity, 0):3d}"
            for entity in entities
        ]


def apply_dataset(dataset: Any, get_model: GetModel) -> Summary:
    """Reconcile the database with `dataset`. Returns what changed.

    `get_model` takes an app label and a model name, matching both
    `django.apps.apps.get_model` and the `apps.get_model` a migration receives.
    """
    summary = Summary(version=dataset.VERSION)

    provinces = _apply_provinces(dataset, get_model, summary)
    groups = _apply_groups(dataset, get_model, summary)
    categories = _apply_categories(dataset, get_model, groups, summary)
    _apply_capabilities(dataset, get_model, categories, summary)
    _apply_switches(dataset, get_model, categories, provinces, summary)

    return summary


def _reconcile(
    model: Any,
    entity: str,
    code: str,
    expected_id: Any,
    defaults: dict[str, Any],
    summary: Summary,
) -> Any:
    """Return the row for `code`, creating it only if it is absent.

    An existing row is returned untouched; only its identity is checked.
    """
    existing = model.objects.filter(code=code).first()
    if existing is not None:
        if existing.pk != expected_id:
            raise ReferenceIdMismatch(entity, code, expected_id, existing.pk)
        summary.verified_one(entity)
        return existing
    row = model.objects.create(id=expected_id, code=code, **defaults)
    summary.created_one(entity)
    return row


def _apply_provinces(dataset: Any, get_model: GetModel, summary: Summary) -> dict[str, Any]:
    model = get_model("locations", "Province")
    rows: dict[str, Any] = {}
    for code, name_ar, name_en, active, sort_order in dataset.PROVINCES:
        rows[code] = _reconcile(
            model,
            "province",
            code,
            dataset.reference_id("province", code),
            {
                "name_ar": name_ar,
                "name_en": name_en,
                "active": active,
                "sort_order": sort_order,
            },
            summary,
        )
    return rows


def _apply_groups(dataset: Any, get_model: GetModel, summary: Summary) -> dict[str, Any]:
    model = get_model("directory", "CategoryGroup")
    rows: dict[str, Any] = {}
    for code, name_ar, name_en, sort_order in dataset.CATEGORY_GROUPS:
        rows[code] = _reconcile(
            model,
            "category group",
            code,
            dataset.reference_id("category-group", code),
            {
                "name_ar": name_ar,
                "name_en": name_en,
                "active": True,
                "sort_order": sort_order,
            },
            summary,
        )
    return rows


def _apply_categories(
    dataset: Any, get_model: GetModel, groups: dict[str, Any], summary: Summary
) -> dict[str, Any]:
    model = get_model("directory", "Category")
    rows: dict[str, Any] = {}
    for item in dataset.CATEGORIES:
        rows[item["code"]] = _reconcile(
            model,
            "category",
            item["code"],
            dataset.reference_id("category", item["code"]),
            {
                "group": groups[item["group"]],
                "slug": item["slug"],
                "name_ar": item["name_ar"],
                "name_en": item["name_en"],
                "icon_key": item["icon_key"],
                "specialization": item["specialization"],
                # Visibility is decided by the province switch, not by this flag; see the
                # formula quoted in the dataset module.
                "active": True,
                "sort_order": item["sort_order"],
            },
            summary,
        )
    return rows


def _apply_capabilities(
    dataset: Any, get_model: GetModel, categories: dict[str, Any], summary: Summary
) -> None:
    model = get_model("directory", "CategoryCapabilities")
    for item in dataset.CATEGORIES:
        category = categories[item["code"]]
        if model.objects.filter(category=category).exists():
            # Capability flags are Admin-managed through adminCategoryCapabilitiesReplace.
            summary.verified_one("capabilities")
            continue
        model.objects.create(category=category, **item["capabilities"])
        summary.created_one("capabilities")


def _apply_switches(
    dataset: Any,
    get_model: GetModel,
    categories: dict[str, Any],
    provinces: dict[str, Any],
    summary: Summary,
) -> None:
    model = get_model("directory", "CategoryProvince")
    enabled = set(dataset.ENABLED_SWITCHES)
    for item in dataset.CATEGORIES:
        for province_code, _, _, _, _ in dataset.PROVINCES:
            category = categories[item["code"]]
            province = provinces[province_code]
            if model.objects.filter(category=category, province=province).exists():
                summary.verified_one("province switch")
                continue
            on = (item["code"], province_code) in enabled
            model.objects.create(
                category=category,
                province=province,
                public_enabled=on,
                owner_registration_enabled=on,
                sort_order=item["sort_order"],
            )
            summary.created_one("province switch")
