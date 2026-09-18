"""INT-042: `/admin/me/` answers who the caller is and what they may do.

The endpoint exists because nothing else could answer it. `accountProfileRetrieve` omits
permissions, and reading them through `adminUserRetrieve` plus `adminRolesList` needs
`admin.users.read` and `admin.roles.read` — which is circular, since most operators hold
neither.
"""

from typing import Any

import pytest
from rest_framework.test import APIClient

from accounts.models import AdminPermission, AdminRole, User, UserAdminRole

URL = "/api/v1/admin/me/"


def _role(code: str, *permissions: str) -> AdminRole:
    role = AdminRole.objects.create(code=code, name=code.upper())
    for permission in permissions:
        role.permissions.add(AdminPermission.objects.get_or_create(code=permission)[0])
    return role


def _client(user: User | None = None) -> APIClient:
    client = APIClient()
    if user is not None:
        client.force_authenticate(user=user)
    return client


@pytest.mark.django_db
def test_an_anonymous_caller_is_refused() -> None:
    response = _client().get(URL)

    assert response.status_code == 401
    assert response.json()["code"] == "AUTHENTICATION_REQUIRED"


@pytest.mark.django_db
def test_an_ordinary_account_is_refused(user: User) -> None:
    """Holding no Admin role at all is a refusal, not an empty permission list."""
    response = _client(user).get(URL)

    assert response.status_code == 403
    assert response.json()["code"] == "PERMISSION_DENIED"


@pytest.mark.django_db
def test_an_operator_whose_roles_carry_nothing_gets_an_empty_list(user: User) -> None:
    """A role with no permissions is a valid state: someone granted it deliberately."""
    UserAdminRole.objects.create(user=user, role=_role("empty"), active=True)

    response = _client(user).get(URL)

    assert response.status_code == 200
    body = response.json()
    assert body["permissions"] == []
    assert body["userId"] == str(user.pk)
    assert body["displayName"] == user.name


@pytest.mark.django_db
def test_one_permission_appears_once(user: User) -> None:
    UserAdminRole.objects.create(
        user=user, role=_role("reviewer", "admin.reviews.read"), active=True
    )

    body = _client(user).get(URL).json()

    assert body["permissions"] == ["admin.reviews.read"]


@pytest.mark.django_db
def test_overlapping_roles_are_deduplicated_and_sorted(user: User) -> None:
    UserAdminRole.objects.create(
        user=user,
        role=_role("desk", "admin.reviews.read", "admin.reviews.decide"),
        active=True,
    )
    UserAdminRole.objects.create(
        user=user,
        role=_role("ops", "admin.reviews.read", "admin.facilities.manage"),
        active=True,
    )

    permissions = _client(user).get(URL).json()["permissions"]

    assert permissions == [
        "admin.facilities.manage",
        "admin.reviews.decide",
        "admin.reviews.read",
    ]
    assert len(permissions) == len(set(permissions))


@pytest.mark.django_db
def test_an_inactive_role_link_grants_nothing(user: User) -> None:
    UserAdminRole.objects.create(
        user=user, role=_role("revoked", "admin.reviews.read"), active=False
    )

    assert _client(user).get(URL).status_code == 403


@pytest.mark.django_db
def test_a_blocked_account_is_refused(user: User) -> None:
    UserAdminRole.objects.create(
        user=user, role=_role("desk", "admin.reviews.read"), active=True
    )
    user.is_active = False
    user.save(update_fields=["is_active"])

    assert _client(user).get(URL).status_code == 403


@pytest.mark.django_db
def test_is_superuser_alone_grants_no_admin_permission(user: User) -> None:
    """`is_superuser` is a Django admin-site concept and is not an authorization path here.

    A stray `createsuperuser`, or a flag flipped in a database shell, must not confer
    operator access. See DECISION-009 and DEBT-003.
    """
    user.is_superuser = True
    user.is_staff = True
    user.save(update_fields=["is_superuser", "is_staff"])

    assert _client(user).get(URL).status_code == 403


@pytest.mark.django_db
def test_the_response_carries_nothing_sensitive(user: User) -> None:
    user.set_password("OperatorPass123!")
    user.save()
    UserAdminRole.objects.create(
        user=user, role=_role("desk", "admin.reviews.read"), active=True
    )

    body: dict[str, Any] = _client(user).get(URL).json()

    assert set(body) == {"userId", "displayName", "permissions"}
    serialised = str(body)
    for forbidden in (user.phone, "password", "pbkdf2", "refresh", "session", "superuser"):
        assert forbidden not in serialised, f"{forbidden} leaked"


@pytest.mark.django_db
def test_the_endpoint_and_the_permission_class_agree(user: User) -> None:
    """The shell must not render against a different answer from the one enforced.

    Both read `accounts.rbac`, so a code reported here is a code that will pass the guard.
    """
    UserAdminRole.objects.create(
        user=user, role=_role("desk", "admin.dashboard.read"), active=True
    )
    client = _client(user)

    reported = client.get(URL).json()["permissions"]

    assert reported == ["admin.dashboard.read"]
    assert client.get("/api/v1/admin/dashboard/").status_code == 200
    assert client.get("/api/v1/admin/audit/").status_code == 403
