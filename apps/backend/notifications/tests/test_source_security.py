from pathlib import Path


def test_push_tokens_are_encrypted_and_hashed_at_rest():
    model_source = Path("notifications/models.py").read_text()
    service_source = Path("notifications/services.py").read_text()
    assert "token_ciphertext" in model_source
    assert "token_digest" in model_source
    assert "encrypt_push_token" in service_source
    assert "push_token_digest" in service_source
    assert "token = models." not in model_source


def test_push_payload_contains_only_notification_reference_and_type():
    source = Path("notifications/services.py").read_text()
    assert 'data = {"notificationId": str(notification.id), "type": notification.type}' in source


def test_celery_push_delivery_has_retry_policy():
    source = Path("notifications/tasks.py").read_text()
    # Only transient failures retry (explicit self.retry with backoff), never every Exception.
    assert "autoretry_for=(Exception,)" not in source
    assert "acks_late=True" in source
    assert "max_retries=MAX_RETRIES" in source
    assert "except TransientPushError" in source
