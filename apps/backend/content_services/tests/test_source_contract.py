from pathlib import Path

import pytest

from audit.models import AuditEvent
from content_services.models import Advertisement
from content_services.services import (
    delete_advertisement,
    save_advertisement,
    update_advertisement,
)


def test_public_ad_dto_never_exposes_storage_key() -> None:
    source = Path("content_services/serializers.py").read_text()
    assert '"imageUrl"' in source
    assert '"image_key"' not in source
    assert '"imageKey"' not in source


def test_external_ads_require_https_validation() -> None:
    source = Path("content_services/models.py").read_text()
    assert 'parsed.scheme != "https"' in source
    assert "parsed.username" in source
    assert "parsed.password" in source


@pytest.mark.django_db
def test_ad_mutations_are_audited() -> None:
    """Executed rather than grepped.

    This asserted that certain strings appeared in the source, which kept passing when the
    action names changed and would equally have passed if `record_audit` were never called.
    It now runs the three mutations and reads the audit trail back.
    """
    advertisement = Advertisement(image_key="ads/a.jpg", title_ar="إعلان")
    save_advertisement(actor=None, advertisement=advertisement)
    update_advertisement(actor=None, advertisement=advertisement, data={"titleAr": "محدّث"})
    delete_advertisement(actor=None, advertisement=advertisement)

    actions = list(AuditEvent.objects.order_by("created_at").values_list("action", flat=True))

    assert actions == [
        "advertisement.created",
        "advertisement.updated",
        "advertisement.deleted",
    ]
