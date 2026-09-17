"""Stable component names for reused choice sets.

drf-spectacular invents hash-suffixed names such as `Status652Enum` when two domains both
declare a field called `status`, and those names are not stable across runs. Binding each
choice set to a module-level variable here lets `ENUM_NAME_OVERRIDES` name them
explicitly while the model definitions stay the single source of the values.

This module is imported lazily during schema generation, after the app registry is ready.
"""

from accounts.models import AccountDeletionRequest
from business_hours.services import AvailabilityState
from content_services.models import Advertisement
from directory.models import Category
from facilities.models import Facility, FacilityApplication, FacilityMembership

FACILITY_STATUS = Facility.Status.choices
FACILITY_APPLICATION_STATUS = FacilityApplication.Status.choices
FACILITY_APPLICATION_KIND = FacilityApplication.Kind.choices
FACILITY_MEMBER_ROLE = FacilityMembership.Role.choices
ACCOUNT_DELETION_STATUS = AccountDeletionRequest.Status.choices
CATEGORY_SPECIALIZATION = Category.Specialization.choices
ADVERTISEMENT_ACTION_TYPE = Advertisement.ActionType.choices
ADVERTISEMENT_TARGET_SCOPE = Advertisement.TargetScope.choices
AVAILABILITY_STATE = [state.value for state in AvailabilityState]

# Not model-backed: produced directly by the views.
DEPENDENCY_CONFIGURED = ["configured", "unconfigured"]
DATABASE_HEALTH = ["ok", "unavailable"]
OWNER_REQUIRED_ACTION = [
    "REVIEW_REJECTION",
    "COMPLETE_AND_SUBMIT",
    "REVERIFY_AND_SUBMIT",
    "WAIT_FOR_REVIEW",
    "CONTACT_SUPPORT",
]
