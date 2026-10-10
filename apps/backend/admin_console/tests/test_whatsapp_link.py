"""Linking the WhatsApp bot from the console (DECISION-116).

The bot is replaced by a stand-in that answers as it would; what is tested is what the console is
told, who may ask, and that a fresh pairing is audited.
"""

from __future__ import annotations

import json
from typing import Any

import pytest
from django.test import override_settings
from rest_framework.test import APIClient

from accounts.models import AdminPermission, AdminRole, User, UserAdminRole
from admin_console.views_whatsapp import WhatsAppBotView
from audit.models import AuditEvent

pytestmark = pytest.mark.django_db

URL = "/api/v1/admin/whatsapp/"
RELINK = "/api/v1/admin/whatsapp/relink/"
BOT = {"WHATSAPP_BOT_URL": "http://bot:8080", "WHATSAPP_BOT_TOKEN": "secret"}


def client_with(*permissions: str) -> APIClient:
    user = User.objects.create_user(phone="+963900111223", password="OperatorPass123!")
    role = AdminRole.objects.create(code=f"role-{len(permissions)}", name="Role")
    for code in permissions:
        role.permissions.add(AdminPermission.objects.get_or_create(code=code)[0])
    UserAdminRole.objects.create(user=user, role=role, active=True)
    client = APIClient()
    client.force_authenticate(user=user)
    return client


class FakeBot(list[tuple[str, str, dict[str, str]]]):
    """Every call the backend makes, and the answer to give by path."""

    def __init__(self) -> None:
        super().__init__()
        self.answers: dict[str, tuple[int, dict[str, Any]]] = {
            "/pairing": (
                200,
                {"connected": False, "loggedOut": False, "qr": "2@code", "linkedNumber": None},
            ),
            "/pairing/relink": (202, {"started": True}),
        }

    def __call__(self, method: str, url: str, headers: dict[str, str]) -> tuple[int, bytes]:
        self.append((method, url, headers))
        status, body = self.answers[url.removeprefix("http://bot:8080")]
        return status, json.dumps(body).encode()


@pytest.fixture
def bot(monkeypatch: pytest.MonkeyPatch) -> FakeBot:
    fake = FakeBot()
    monkeypatch.setattr(WhatsAppBotView, "caller", staticmethod(fake))
    return fake


@override_settings(**BOT)
def test_unlinked_the_console_gets_the_code_to_scan_drawn(bot: FakeBot) -> None:
    body = client_with("admin.settings.manage").get(URL).json()

    assert body["configured"] and body["reachable"] and not body["connected"]
    assert body["qrSvgDataUri"].startswith("data:image/svg+xml")
    assert "2@code" not in json.dumps(body), "the code leaves only as a drawing"
    assert bot[0][2]["Authorization"] == "Bearer secret"


@override_settings(**BOT)
def test_linked_the_console_gets_the_number_and_no_code(bot: FakeBot) -> None:
    bot.answers["/pairing"] = (200, {"connected": True, "linkedNumber": "963933000999", "qr": None})

    body = client_with("admin.settings.manage").get(URL).json()

    assert body["connected"] and body["linkedNumber"] == "+963933000999"
    assert body["qrSvgDataUri"] is None


@override_settings(WHATSAPP_BOT_URL="", WHATSAPP_BOT_TOKEN="")
def test_without_a_bot_configured_it_says_so() -> None:
    body = client_with("admin.settings.manage").get(URL).json()

    assert body == {**body, "configured": False, "reachable": False, "qrSvgDataUri": None}


@override_settings(**BOT)
def test_a_fresh_pairing_is_asked_for_and_audited(bot: FakeBot) -> None:
    response = client_with("admin.settings.manage").post(RELINK)

    assert response.status_code == 202
    assert ("POST", "http://bot:8080/pairing/relink") in [(m, u) for m, u, _ in bot]
    assert AuditEvent.objects.filter(action="whatsapp.relinked").count() == 1


@override_settings(**BOT)
def test_a_bot_that_cannot_relink_is_said_in_arabic(bot: FakeBot) -> None:
    bot.answers["/pairing/relink"] = (502, {"reason": "relink_failed"})

    response = client_with("admin.settings.manage").post(RELINK)

    assert response.status_code == 400
    assert response.json()["code"] == "WHATSAPP_BOT_UNAVAILABLE"
    assert not AuditEvent.objects.filter(action="whatsapp.relinked").exists()


@override_settings(**BOT)
def test_only_who_may_change_the_settings_sees_the_code(bot: FakeBot) -> None:
    reader = client_with("admin.settings.read")

    assert reader.get(URL).status_code == 403
    assert reader.post(RELINK).status_code == 403
    assert bot == []
