"""The console needs a second step; the rest of the account does not (DECISION-065)."""

from typing import Any

import pytest
from django.test import override_settings
from rest_framework.test import APIClient

from accounts import mfa
from accounts.models import AdminPermission, AdminRole, StaffTotpDevice, User, UserAdminRole
from accounts.services import create_session
from audit.models import AuditEvent

DASHBOARD = "/api/v1/admin/dashboard/"


def test_codes_match_the_rfc_6238_reference_vectors() -> None:
    # RFC 6238 appendix B, SHA-1, secret "12345678901234567890", truncated to six digits.
    secret = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ"
    for unix, expected in ((59, "287082"), (1111111109, "081804"), (1234567890, "005924")):
        assert mfa.code_at(secret, mfa.current_step(unix)) == expected


def test_a_code_is_accepted_once_and_only_near_its_time() -> None:
    secret = mfa.new_secret()
    now = 1_800_000_000.0
    step = mfa.current_step(now)
    code = mfa.code_at(secret, step)

    assert mfa.matching_step(secret, code, after=0, now=now) == step
    assert mfa.matching_step(secret, code, after=step, now=now) is None
    assert mfa.matching_step(secret, code, after=0, now=now + 5 * 30) is None
    assert mfa.matching_step(secret, "12345", after=0, now=now) is None


@pytest.fixture
def operator(db: Any) -> User:
    user = User.objects.create_user(phone="+963955000001", password="x" * 12, name="مشغّل")
    role = AdminRole.objects.create(code="ops-mfa", name="Ops")
    for code in ("admin.dashboard.read", "admin.roles.manage"):
        role.permissions.add(AdminPermission.objects.get_or_create(code=code)[0])
    UserAdminRole.objects.create(user=user, role=role, active=True)
    return user


def _signed_in(user: User) -> APIClient:
    tokens = create_session(user=user, platform="web", device_name="console")
    client = APIClient()
    client.credentials(HTTP_AUTHORIZATION=f"Bearer {tokens['accessToken']}")
    return client


def _enroll(client: APIClient) -> tuple[str, list[str]]:
    setup = client.post("/api/v1/account/mfa/setup/")
    assert setup.status_code == 200, setup.content
    secret = setup.json()["secret"]
    assert setup.json()["qrSvgDataUri"].startswith("data:image/svg+xml")
    code = mfa.code_at(secret, mfa.current_step())
    confirmed = client.post("/api/v1/account/mfa/confirm/", {"code": code}, format="json")
    assert confirmed.status_code == 200, confirmed.content
    return secret, confirmed.json()["recoveryCodes"]


@pytest.mark.django_db
def test_an_enrolled_operator_needs_the_code_in_every_new_session(operator: User) -> None:
    first = _signed_in(operator)
    secret, codes = _enroll(first)
    # The session that confirmed it has passed the step.
    assert first.get(DASHBOARD).status_code == 200
    assert len(codes) == 10

    second = _signed_in(operator)
    blocked = second.get(DASHBOARD)
    assert blocked.status_code == 403
    assert blocked.json()["code"] == "MFA_REQUIRED"
    # The rest of the account still works on a password alone.
    assert second.get("/api/v1/account/profile/").status_code == 200
    me = second.get("/api/v1/admin/me/").json()
    assert me["mfa"] == {
        "enabled": True,
        "required": False,
        "verified": False,
        "recoveryCodesLeft": 10,
    }

    StaffTotpDevice.objects.filter(user=operator).update(last_used_step=0)
    code = mfa.code_at(secret, mfa.current_step())
    assert (
        second.post("/api/v1/account/mfa/verify/", {"code": code}, format="json").status_code == 200
    )
    assert second.get(DASHBOARD).status_code == 200


@pytest.mark.django_db
def test_a_recovery_code_works_once(operator: User) -> None:
    _, codes = _enroll(_signed_in(operator))
    lost = _signed_in(operator)

    used = lost.post("/api/v1/account/mfa/verify/", {"code": codes[0].lower()}, format="json")
    again = _signed_in(operator).post(
        "/api/v1/account/mfa/verify/", {"code": codes[0]}, format="json"
    )

    assert used.status_code == 200
    assert used.json()["recoveryCodesLeft"] == 9
    assert again.status_code == 400
    assert again.json()["code"] == "MFA_CODE_INVALID"
    assert AuditEvent.objects.filter(action="mfa.recovery_code_used").exists()


@pytest.mark.django_db
@override_settings(STAFF_MFA_REQUIRED=True)
def test_where_it_is_required_an_operator_without_one_must_set_it_up(operator: User) -> None:
    client = _signed_in(operator)

    blocked = client.get(DASHBOARD)

    assert blocked.status_code == 403
    assert blocked.json()["code"] == "MFA_ENROLLMENT_REQUIRED"
    _enroll(client)
    assert client.get(DASHBOARD).status_code == 200
    refused = client.post("/api/v1/account/mfa/disable/", {"code": "000000"}, format="json")
    assert refused.status_code == 409
    assert refused.json()["code"] == "MFA_REQUIRED_BY_POLICY"


@pytest.mark.django_db
def test_only_operators_set_one_up_and_switching_off_needs_a_code(operator: User) -> None:
    member = User.objects.create_user(phone="+963955000002", password="x" * 12, name="عضو")
    assert _signed_in(member).post("/api/v1/account/mfa/setup/").status_code == 403

    client = _signed_in(operator)
    secret, _ = _enroll(client)
    wrong = client.post("/api/v1/account/mfa/disable/", {"code": "000000"}, format="json")
    assert wrong.status_code == 400
    StaffTotpDevice.objects.filter(user=operator).update(last_used_step=0)
    code = mfa.code_at(secret, mfa.current_step())
    off = client.post("/api/v1/account/mfa/disable/", {"code": code}, format="json")
    assert off.status_code == 200
    assert off.json()["enabled"] is False
    assert not StaffTotpDevice.objects.filter(user=operator).exists()


@pytest.mark.django_db
def test_an_administrator_resets_a_colleagues_lost_authenticator(operator: User) -> None:
    colleague = User.objects.create_user(phone="+963955000003", password="x" * 12, name="زميل")
    role = AdminRole.objects.create(code="ops-colleague", name="Ops")
    role.permissions.add(AdminPermission.objects.get_or_create(code="admin.dashboard.read")[0])
    UserAdminRole.objects.create(user=colleague, role=role, active=True)
    _enroll(_signed_in(colleague))
    admin = _signed_in(operator)

    reset = admin.post(f"/api/v1/admin/users/{colleague.pk}/mfa/reset/")

    assert reset.status_code == 204
    assert not StaffTotpDevice.objects.filter(user=colleague).exists()
    assert AuditEvent.objects.filter(action="mfa.reset", target_id=str(colleague.pk)).exists()
    # With nothing set up and nothing required, the colleague is back in on a password.
    assert _signed_in(colleague).get(DASHBOARD).status_code == 200


@pytest.mark.django_db
def test_the_console_events_need_the_second_step_too(operator: User) -> None:
    from asgiref.sync import async_to_sync

    from accounts.tokens import decode_access_token
    from realtime.consumers import DirectoryConsumer

    _enroll(_signed_in(operator))
    tokens = create_session(user=operator, platform="web", device_name="console")
    consumer = DirectoryConsumer()
    consumer.authenticated_user_id = str(operator.pk)
    consumer.authenticated_claims = decode_access_token(tokens["accessToken"])

    assert async_to_sync(consumer._has_admin_permission)("admin.dashboard.read") is False
