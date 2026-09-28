from __future__ import annotations

import logging

logger = logging.getLogger(__name__)


def init_sentry(*, dsn: str, traces_sample_rate: float, environment: str | None) -> bool:
    """Initialise Sentry with Django and Celery integrations. Never sends PII."""
    try:
        import sentry_sdk
        from sentry_sdk.integrations.celery import CeleryIntegration
        from sentry_sdk.integrations.django import DjangoIntegration
        from sentry_sdk.integrations.logging import LoggingIntegration
    except ImportError:  # pragma: no cover - dependency is declared, this is defensive
        logger.warning("sentry.sdk_missing")
        return False
    sentry_sdk.init(
        dsn=dsn,
        environment=environment,
        send_default_pii=False,
        traces_sample_rate=max(0.0, min(1.0, traces_sample_rate)),
        integrations=[
            DjangoIntegration(),
            CeleryIntegration(),
            LoggingIntegration(level=logging.INFO, event_level=logging.ERROR),
        ],
    )
    return True
