"""Wire representations shared by every endpoint that describes a category."""

from typing import Any

from .models import Category


def category_capabilities(category: Category) -> dict[str, bool]:
    """The capability flags a client branches on, one definition for every endpoint.

    The public category list, the owner configuration and each owner facility all return
    this, so a client never has to look a category's capabilities up somewhere else to know
    which controls a facility supports (INT-056).
    """
    caps: Any = category.capabilities
    return {
        "hours": caps.supports_hours,
        "photos": caps.supports_photos,
        "ratings": caps.supports_ratings,
        "duty": caps.supports_duty,
        "specialtyFilter": caps.supports_specialty_filter,
        "serviceFilter": caps.supports_service_filter,
        "temporaryClosure": caps.supports_temporary_closure,
        "ownerOnboarding": caps.supports_owner_onboarding,
    }
