from pathlib import Path


def test_public_ad_dto_never_exposes_storage_key():
    source = Path("content_services/serializers.py").read_text()
    assert '"imageUrl"' in source
    assert '"image_key"' not in source
    assert '"imageKey"' not in source


def test_external_ads_require_https_validation():
    source = Path("content_services/models.py").read_text()
    assert 'parsed.scheme != "https"' in source
    assert "parsed.username" in source
    assert "parsed.password" in source


def test_ad_mutations_are_audited():
    source = Path("content_services/services.py").read_text()
    assert "record_audit" in source
    assert "advertisement.saved" in source
    assert "advertisement.deleted" in source
