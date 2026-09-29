from collections.abc import Callable, Iterator
from typing import Any

import pytest

from directory.models import Category, CategoryCapabilities, CategoryGroup
from facilities.models import Facility
from locations.models import Province


@pytest.fixture(autouse=True)
def _isolated_cache() -> Iterator[None]:
    """Database rows roll back after each test; cached copies of them must go too."""
    from django.core.cache import cache

    cache.clear()
    yield
    cache.clear()


@pytest.fixture
def facility(db):
    province=Province.objects.create(code='raqqa-test',name_ar='الرقة',active=True)
    group=CategoryGroup.objects.create(code='health-test',name_ar='الصحة')
    category=Category.objects.create(group=group,code='pharmacy-test',slug='pharmacy-test',name_ar='صيدلية',specialization=Category.Specialization.PHARMACY)
    CategoryCapabilities.objects.create(category=category,supports_duty=True)
    return Facility.objects.create(
        category=category,
        province=province,
        name_ar='صيدلية اختبار',
        status=Facility.Status.ACTIVE,
    )


@pytest.fixture
def user(db):
    from accounts.models import User

    return User.objects.create_user(
        phone="+963900000001",
        password="StrongPass123!",
        name="Test User",
    )


@pytest.fixture
def admin_api(db: Any) -> Callable[..., Any]:
    """Build an authenticated APIClient for an operator holding exactly `codes`."""
    from rest_framework.test import APIClient

    from accounts.models import AdminPermission, AdminRole, User, UserAdminRole

    counter = {"n": 0}

    def make(*codes: str, user: Any = None) -> Any:
        counter["n"] += 1
        if user is None:
            user = User.objects.create_user(
                phone=f"+96399{counter['n']:07d}", password="StrongPass123!", name="Operator"
            )
        role = AdminRole.objects.create(code=f"fixture-{counter['n']}", name="Fixture")
        for code in codes:
            role.permissions.add(AdminPermission.objects.get_or_create(code=code)[0])
        UserAdminRole.objects.create(user=user, role=role, active=True)
        client = APIClient()
        client.force_authenticate(user=user)
        client.user = user  # type: ignore[attr-defined]
        return client

    return make
