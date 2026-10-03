"""The role editor and the two rules every role change keeps (DECISION-072)."""

from io import StringIO
from typing import Any
from unittest import mock

import pytest
from django.core.management import call_command
from django.core.management.base import CommandError

from accounts.models import AdminPermission, AdminRole, User, UserAdminRole
from accounts.roles import OWNER_ROLE_CODE, sync_owner_role
from audit.models import AuditEvent

MANAGE = ("admin.roles.read", "admin.roles.manage")


def _role(client: Any) -> AdminRole:
    return UserAdminRole.objects.get(user=client.user).role


@pytest.mark.django_db
def test_migrate_leaves_an_owner_role_with_every_permission() -> None:
    owner = AdminRole.objects.get(code=OWNER_ROLE_CODE)
    assert set(owner.permissions.values_list("code", flat=True)) == set(
        AdminPermission.objects.values_list("code", flat=True)
    )
    AdminPermission.objects.create(code="admin.future.read")
    sync_owner_role()
    assert owner.permissions.filter(code="admin.future.read").exists()


@pytest.mark.django_db
def test_the_permission_catalogue_is_listed(admin_api: Any) -> None:
    client = admin_api("admin.roles.read")
    items = client.get("/api/v1/admin/permissions/").json()["items"]
    codes = [item["code"] for item in items]
    assert codes == sorted(codes)
    assert "admin.roles.manage" in codes
    assert all(set(item) == {"code", "description"} for item in items)


@pytest.mark.django_db
def test_roles_list_their_holders_and_whether_they_are_locked(admin_api: Any) -> None:
    client = admin_api(*MANAGE)
    blocked = User.objects.create_user(phone="+963933000001", password="x", name="Blocked")
    blocked.is_active = False
    blocked.save()
    UserAdminRole.objects.create(user=blocked, role=_role(client), active=True)

    items = {item["code"]: item for item in client.get("/api/v1/admin/roles/").json()["items"]}

    assert items[_role(client).code]["holderCount"] == 1
    assert items[_role(client).code]["locked"] is False
    assert items[OWNER_ROLE_CODE]["locked"] is True
    assert items[OWNER_ROLE_CODE]["holderCount"] == 0


@pytest.mark.django_db
def test_create_rename_repermission_and_delete_a_role(admin_api: Any) -> None:
    client = admin_api(*MANAGE)

    created = client.post(
        "/api/v1/admin/roles/",
        {"name": "  مراجع  ", "permissions": ["admin.reviews.read", "admin.reviews.decide"]},
        format="json",
    )
    assert created.status_code == 201, created.content
    body = created.json()
    assert body["name"] == "مراجع"
    assert body["code"].startswith("role-")
    assert body["permissions"] == ["admin.reviews.decide", "admin.reviews.read"]
    assert body["holderCount"] == 0

    updated = client.patch(
        f"/api/v1/admin/roles/{body['id']}/",
        {"name": "مراجع أول", "permissions": ["admin.reviews.read"]},
        format="json",
    )
    assert updated.status_code == 200, updated.content
    assert updated.json()["permissions"] == ["admin.reviews.read"]
    assert updated.json()["code"] == body["code"]

    assert client.delete(f"/api/v1/admin/roles/{body['id']}/").status_code == 204
    assert not AdminRole.objects.filter(pk=body["id"]).exists()
    actions = list(
        AuditEvent.objects.filter(target_type="AdminRole", target_id=str(body["id"]))
        .order_by("created_at")
        .values_list("action", flat=True)
    )
    assert actions == ["admin_role.created", "admin_role.updated", "admin_role.deleted"]


@pytest.mark.django_db
def test_bad_names_codes_and_permissions_are_refused(admin_api: Any) -> None:
    client = admin_api(*MANAGE)
    AdminRole.objects.create(code="desk", name="Desk")

    for payload, field in (
        ({"name": "desk", "permissions": []}, "name"),
        ({"name": "New", "code": "desk", "permissions": []}, "code"),
        ({"name": "New", "code": "Not A Code", "permissions": []}, "code"),
        ({"name": "New", "permissions": ["admin.nothing.here"]}, "permissions"),
    ):
        response = client.post("/api/v1/admin/roles/", payload, format="json")
        assert response.status_code == 400, payload
        assert field in response.json()["details"], response.json()


@pytest.mark.django_db
def test_reading_roles_does_not_allow_editing_them(admin_api: Any) -> None:
    client = admin_api("admin.roles.read")
    role = AdminRole.objects.create(code="desk", name="Desk")

    assert (
        client.post(
            "/api/v1/admin/roles/", {"name": "X", "permissions": []}, format="json"
        ).status_code
        == 403
    )
    assert (
        client.patch(f"/api/v1/admin/roles/{role.pk}/", {"name": "Y"}, format="json").status_code
        == 403
    )
    assert client.delete(f"/api/v1/admin/roles/{role.pk}/").status_code == 403


@pytest.mark.django_db
def test_the_locked_owner_role_cannot_be_edited_or_deleted(admin_api: Any) -> None:
    client = admin_api(*MANAGE)
    owner = AdminRole.objects.get(code=OWNER_ROLE_CODE)

    for response in (
        client.patch(f"/api/v1/admin/roles/{owner.pk}/", {"permissions": []}, format="json"),
        client.delete(f"/api/v1/admin/roles/{owner.pk}/"),
    ):
        assert response.status_code == 409
        assert response.json()["code"] == "ROLE_LOCKED"


@pytest.mark.django_db
def test_a_held_role_is_not_deleted(admin_api: Any) -> None:
    client = admin_api(*MANAGE)
    response = client.delete(f"/api/v1/admin/roles/{_role(client).pk}/")
    assert response.status_code == 409
    assert response.json()["code"] == "ROLE_IN_USE"


@pytest.mark.django_db
def test_nobody_can_remove_the_last_way_to_grant_roles(admin_api: Any) -> None:
    client = admin_api(*MANAGE)
    mine = _role(client)

    # Taking the permission off the only role that carries it…
    response = client.patch(
        f"/api/v1/admin/roles/{mine.pk}/", {"permissions": ["admin.roles.read"]}, format="json"
    )
    assert response.status_code == 409
    assert response.json()["code"] == "LAST_ROLE_MANAGER"
    assert mine.permissions.filter(code="admin.roles.manage").exists()

    # …taking the role off its only holder…
    response = client.put(
        f"/api/v1/admin/users/{client.user.pk}/roles/", {"roleIds": []}, format="json"
    )
    assert response.status_code == 409
    assert UserAdminRole.objects.get(user=client.user, role=mine).active

    # …and blocking that holder are all refused.
    blocker = admin_api("admin.users.manage")
    response = blocker.post(f"/api/v1/admin/users/{client.user.pk}/block/")
    assert response.status_code == 409
    client.user.refresh_from_db()
    assert client.user.is_active


@pytest.mark.django_db
def test_with_a_second_manager_the_first_can_step_down(admin_api: Any) -> None:
    client = admin_api(*MANAGE)
    admin_api(*MANAGE)
    response = client.put(
        f"/api/v1/admin/users/{client.user.pk}/roles/", {"roleIds": []}, format="json"
    )
    assert response.status_code == 204


@pytest.mark.django_db
def test_grant_operator_appoints_an_existing_account(user: User) -> None:
    out = StringIO()
    call_command("grant_operator", "0900000001", stdout=out)

    owner = AdminRole.objects.get(code=OWNER_ROLE_CODE)
    assert UserAdminRole.objects.get(user=user, role=owner).active
    assert "now holds" in out.getvalue()
    assert AuditEvent.objects.filter(action="admin_role.granted_from_shell").count() == 1

    again = StringIO()
    call_command("grant_operator", "+963900000001", stdout=again)
    assert "already holds" in again.getvalue()
    assert AuditEvent.objects.filter(action="admin_role.granted_from_shell").count() == 1


@pytest.mark.django_db
def test_grant_operator_refuses_what_it_should(user: User) -> None:
    with pytest.raises(CommandError, match="Syrian"):
        call_command("grant_operator", "12345")
    with pytest.raises(CommandError, match="--create"):
        call_command("grant_operator", "0911111111")
    user.is_active = False
    user.save()
    with pytest.raises(CommandError, match="blocked"):
        call_command("grant_operator", "0900000001")


@pytest.mark.django_db
def test_grant_operator_creates_an_account_with_a_prompted_password() -> None:
    with mock.patch(
        "admin_console.management.commands.grant_operator.getpass",
        side_effect=["Owner-Pass-2026!", "Owner-Pass-2026!"],
    ):
        call_command(
            "grant_operator", "0922222222", "--create", "--name", "المدير", stdout=StringIO()
        )

    created = User.objects.get(phone="+963922222222")
    assert created.check_password("Owner-Pass-2026!")
    assert created.admin_role_links.get(active=True).role.code == OWNER_ROLE_CODE

    with (
        mock.patch(
            "admin_console.management.commands.grant_operator.getpass", side_effect=["a", "b"]
        ),
        pytest.raises(CommandError, match="differ"),
    ):
        call_command("grant_operator", "0933333333", "--create", "--name", "X")
    assert not User.objects.filter(phone="+963933333333").exists()
