import uuid
from django.conf import settings
from django.db import models
class AuditEvent(models.Model):
    id=models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    actor=models.ForeignKey(settings.AUTH_USER_MODEL, null=True, blank=True, on_delete=models.SET_NULL)
    action=models.CharField(max_length=120)
    target_type=models.CharField(max_length=120)
    target_id=models.CharField(max_length=64)
    metadata=models.JSONField(default=dict, blank=True)
    created_at=models.DateTimeField(auto_now_add=True)
