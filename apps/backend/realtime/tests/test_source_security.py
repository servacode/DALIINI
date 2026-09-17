from pathlib import Path


def test_consumer_never_reads_access_token_from_query_string():
    source = Path("realtime/consumers.py").read_text()
    assert "query_string" not in source
    assert "accessToken" in source


def test_publisher_uses_transaction_on_commit():
    source = Path("realtime/publisher.py").read_text()
    assert "transaction.on_commit" in source
