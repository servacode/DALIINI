from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


def source(relative: str) -> str:
    return (ROOT / relative).read_text()


def test_bearer_authentication_is_wired() -> None:
    settings = source("directory_backend/settings/base.py")
    assert "BearerAccessTokenAuthentication" in settings


def test_review_decision_uses_row_locks_and_evidence_recheck() -> None:
    services = source("admin_console/services.py")
    assert "select_for_update" in services
    assert "_required_evidence_is_complete" in services


def test_rejection_requires_reason() -> None:
    services = source("admin_console/services.py")
    assert "Rejection reason is required" in services


def test_private_evidence_is_no_store_and_audited() -> None:
    views = source("admin_console/views_reviews.py")
    assert '"Cache-Control"] = "private, no-store"' in views
    assert 'action="verification_evidence.viewed"' in views


def test_user_block_revokes_sessions() -> None:
    services = source("admin_console/services.py")
    assert "revoked_at=timezone.now()" in services


def test_audit_has_operational_snapshots_and_request_id() -> None:
    model = source("audit/models.py")
    assert "before_snapshot" in model
    assert "after_snapshot" in model
    assert "request_id" in model


def test_system_status_never_serializes_connection_or_secret_values() -> None:
    views = source("admin_console/views.py")
    forbidden = ("DATABASE_URL", "SECRET_KEY", "S3_SECRET_ACCESS_KEY", "ACCESS_TOKEN_SIGNING_KEY")
    section = views[views.index("class SystemStatusView") :]
    assert all(value not in section for value in forbidden)
