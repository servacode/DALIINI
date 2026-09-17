from __future__ import annotations

import base64
import hashlib
import hmac

from cryptography.fernet import Fernet, InvalidToken
from django.conf import settings
from django.core.exceptions import ImproperlyConfigured


def _fernet() -> Fernet:
    secret = getattr(settings, "PUSH_TOKEN_ENCRYPTION_KEY", "")
    if not secret:
        raise ImproperlyConfigured("PUSH_TOKEN_ENCRYPTION_KEY is required")
    key = base64.urlsafe_b64encode(hashlib.sha256(secret.encode("utf-8")).digest())
    return Fernet(key)


def encrypt_push_token(token: str) -> str:
    return _fernet().encrypt(token.encode("utf-8")).decode("ascii")


def decrypt_push_token(ciphertext: str) -> str:
    try:
        return _fernet().decrypt(ciphertext.encode("ascii")).decode("utf-8")
    except InvalidToken as exc:
        raise ValueError("Unable to decrypt push token") from exc


def push_token_digest(token: str) -> str:
    secret = getattr(settings, "PUSH_TOKEN_ENCRYPTION_KEY", "")
    if not secret:
        raise ImproperlyConfigured("PUSH_TOKEN_ENCRYPTION_KEY is required")
    return hmac.new(secret.encode(), token.encode(), hashlib.sha256).hexdigest()
