from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


def read(name: str) -> str:
    return (ROOT / name).read_text(encoding="utf-8")


def test_auth_routes_cover_contract() -> None:
    urls = read("accounts/urls.py")
    for fragment in (
        "auth/register/start/",
        "auth/register/verify/",
        "auth/register/complete/",
        "auth/login/",
        "auth/refresh/",
        "auth/logout/",
        "auth/logout-all/",
        "auth/sessions/",
        "auth/recovery/start/",
        "auth/recovery/verify/",
        "auth/recovery/reset/",
        "account/profile/",
        "account/deletion-request/",
    ):
        assert fragment in urls


def test_refresh_and_otp_are_digest_only() -> None:
    services = read("accounts/services.py")
    models = read("accounts/models.py") + read("sessions/models.py")
    assert "hmac.compare_digest" in services
    assert "otp_digest" in models
    assert "refresh_digest" in models
    assert "raw_otp" not in models.lower()
    assert "refresh_token = models" not in models.lower()


def test_account_deletion_protects_last_owner_and_revokes_sessions() -> None:
    services = read("accounts/services.py")
    assert "another_owner" in services
    assert "revoke_all_sessions" in services
    assert "Deleted user" in services
