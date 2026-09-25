"""The V3 launch baseline reference dataset. FROZEN — see the warning below.

`01-MASTER-SPECIFICATION.md` section 3 defines it: the active province is Raqqa, the active
category is pharmacies, owner onboarding and duty are on for pharmacies, every Syrian
province exists in the database, the other provinces are inactive, and other categories may
be seeded but stay invisible until they are activated.

Visibility follows the formula in the same document, section 6:

    province active AND group active AND category active AND public_enabled for province

so a category can be `active` and still invisible everywhere. That is what keeps the four
non-pharmacy health categories configured but dark at launch.

────────────────────────────────────────────────────────────────────────────────────────
THIS MODULE IS IMMUTABLE.

It has entered `directory/migrations/0003_launch_baseline.py`. A database migrated last
month and a database migrated today must receive byte-identical canonical data, so editing
any value here would make those two databases disagree while both claim to be at the same
migration. Add `launch_v2.py` and a new migration instead.

Only pure data lives here: no model imports, no QuerySets, no side effects. The
specialization and flag names are plain strings for the same reason — this file must not
break when a model is refactored.
────────────────────────────────────────────────────────────────────────────────────────
"""

import uuid

VERSION = "launch_v1"

# Fixed project namespace for UUIDv5 derivation. Hardcoded as a literal rather than
# computed at import time so it can never shift with a library or interpreter change. It is
# `uuid.uuid5(uuid.NAMESPACE_URL, "https://servacode.com/directory/reference-data")`.
NAMESPACE = uuid.UUID("bef3a19f-8102-5b0e-b9b5-f59e223953cb")


def reference_id(entity_type: str, code: str) -> uuid.UUID:
    """Derive the canonical UUID of a reference row from its immutable code.

    The same province or category therefore carries the same primary key in development,
    CI, staging, production and any restored database, which is what lets a client cache an
    id and lets an operator compare two environments by eye.
    """
    return uuid.uuid5(NAMESPACE, f"{entity_type}:{code}")


# --------------------------------------------------------------------------------------
# Provinces — all fourteen Syrian governorates
#
# No canonical province codes existed anywhere in the project, the specification or any
# legacy dataset, so these are established here and become the stable identity from now on.
# `sort_order` and `active` are launch defaults only; both pass to Admin authority after
# creation.
# --------------------------------------------------------------------------------------

PROVINCES = (
    # code, name_ar, name_en, active at launch, sort_order
    ("raqqa", "الرقة", "Raqqa", True, 0),
    ("damascus", "دمشق", "Damascus", False, 1),
    ("rif-dimashq", "ريف دمشق", "Rif Dimashq", False, 2),
    ("aleppo", "حلب", "Aleppo", False, 3),
    ("homs", "حمص", "Homs", False, 4),
    ("hama", "حماة", "Hama", False, 5),
    ("latakia", "اللاذقية", "Latakia", False, 6),
    ("tartus", "طرطوس", "Tartus", False, 7),
    ("idlib", "إدلب", "Idlib", False, 8),
    ("deir-ez-zor", "دير الزور", "Deir ez-Zor", False, 9),
    ("al-hasakah", "الحسكة", "Al-Hasakah", False, 10),
    ("daraa", "درعا", "Daraa", False, 11),
    ("as-suwayda", "السويداء", "As-Suwayda", False, 12),
    ("quneitra", "القنيطرة", "Quneitra", False, 13),
)

LAUNCH_PROVINCE = "raqqa"

# --------------------------------------------------------------------------------------
# Taxonomy
#
# One group, the health group. Commercial and service groups are architecture-supported
# future work and are deliberately not seeded.
# --------------------------------------------------------------------------------------

CATEGORY_GROUPS = (
    # code, name_ar, name_en, sort_order
    ("health", "الصحة", "Health", 0),
)

# Specialization values come from `01-MASTER-SPECIFICATION.md` section 5: GENERIC,
# PHARMACY, MEDICAL_CLINIC, NURSING_CENTER. There is no laboratory or supplies
# specialization in that list, so both of those categories are GENERIC — specialization
# exists only where specialized logic exists, and neither has any.
CATEGORIES = (
    {
        "code": "pharmacy",
        "slug": "pharmacy",
        "group": "health",
        "name_ar": "صيدليات",
        "name_en": "Pharmacies",
        "icon_key": "pharmacy",
        "specialization": "PHARMACY",
        "sort_order": 0,
        "capabilities": {
            "supports_hours": True,
            "supports_photos": True,
            "supports_ratings": True,
            "supports_duty": True,
            "supports_specialty_filter": False,
            "supports_service_filter": False,
            "supports_temporary_closure": True,
            "supports_owner_onboarding": True,
        },
    },
    {
        "code": "medical-laboratory",
        "slug": "medical-laboratory",
        "group": "health",
        "name_ar": "مخابر طبية",
        "name_en": "Medical Laboratories",
        "icon_key": "laboratory",
        "specialization": "GENERIC",
        "sort_order": 1,
        "capabilities": {
            "supports_hours": True,
            "supports_photos": True,
            "supports_ratings": True,
            "supports_duty": False,
            "supports_specialty_filter": False,
            "supports_service_filter": False,
            "supports_temporary_closure": True,
            "supports_owner_onboarding": True,
        },
    },
    {
        "code": "medical-clinic",
        "slug": "medical-clinic",
        "group": "health",
        "name_ar": "عيادات طبية",
        "name_en": "Medical Clinics",
        "icon_key": "clinic",
        "specialization": "MEDICAL_CLINIC",
        "sort_order": 2,
        "capabilities": {
            "supports_hours": True,
            "supports_photos": True,
            "supports_ratings": True,
            "supports_duty": False,
            "supports_specialty_filter": True,
            "supports_service_filter": False,
            "supports_temporary_closure": True,
            "supports_owner_onboarding": True,
        },
    },
    {
        "code": "nursing-center",
        "slug": "nursing-center",
        "group": "health",
        "name_ar": "مراكز تمريض",
        "name_en": "Nursing Centers",
        "icon_key": "nursing",
        "specialization": "NURSING_CENTER",
        "sort_order": 3,
        "capabilities": {
            "supports_hours": True,
            "supports_photos": True,
            "supports_ratings": True,
            "supports_duty": False,
            "supports_specialty_filter": False,
            "supports_service_filter": True,
            "supports_temporary_closure": True,
            "supports_owner_onboarding": True,
        },
    },
    {
        "code": "medical-supplies",
        "slug": "medical-supplies",
        "group": "health",
        "name_ar": "مستلزمات طبية",
        "name_en": "Medical Supplies",
        "icon_key": "supplies",
        "specialization": "GENERIC",
        "sort_order": 4,
        "capabilities": {
            "supports_hours": True,
            "supports_photos": True,
            "supports_ratings": True,
            "supports_duty": False,
            "supports_specialty_filter": False,
            "supports_service_filter": False,
            "supports_temporary_closure": True,
            "supports_owner_onboarding": True,
        },
    },
)

# --------------------------------------------------------------------------------------
# Per-province switches
#
# A row is created for every (category, province) pair so the Admin grid is complete and
# the launch state is explicit rather than inferred from an absent row. Only Raqqa plus
# pharmacies is on. These are launch defaults; they pass to Admin authority immediately.
# --------------------------------------------------------------------------------------

ENABLED_SWITCHES = (
    # (category code, province code)
    ("pharmacy", "raqqa"),
)

# --------------------------------------------------------------------------------------
# Verification requirements
#
# Deliberately empty. A VerificationRequirement is not UI configuration: it decides what
# blocks submission, approval and re-verification, so inventing a mandatory requirement
# would be a product policy decision taken inside a migration. The specification describes
# a storefront photo and a business-card proof as a possible default but does not settle
# whether either is required, how many files each takes, or whether a pharmacy licence
# document is needed instead.
#
# Tracked as LAUNCH_POLICY_PENDING in docs/project/DECISIONS.md: pharmacy verification requirements must
# be configured and qualified before owner onboarding is opened publicly in production.
# This does not block backend or Admin development.
# --------------------------------------------------------------------------------------

VERIFICATION_REQUIREMENTS = ()
