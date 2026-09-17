from .models import AuditEvent
SENSITIVE={'password','token','secret','database_url','otp','refresh_token'}
def _redact(value):
    if isinstance(value, dict):
        return {k:('[REDACTED]' if k.lower() in SENSITIVE else _redact(v)) for k,v in value.items()}
    if isinstance(value, list): return [_redact(v) for v in value]
    return value

def record_audit(*, actor, action, target, metadata=None):
    return AuditEvent.objects.create(actor=actor, action=action, target_type=target.__class__.__name__, target_id=str(target.pk), metadata=_redact(metadata or {}))
