import uuid
from django.conf import settings
from django.db import migrations, models
import django.db.models.deletion
class Migration(migrations.Migration):
    initial=True; dependencies=[migrations.swappable_dependency(settings.AUTH_USER_MODEL)]
    operations=[migrations.CreateModel(name='AuditEvent',fields=[('id',models.UUIDField(default=uuid.uuid4,editable=False,primary_key=True,serialize=False)),('action',models.CharField(max_length=120)),('target_type',models.CharField(max_length=120)),('target_id',models.CharField(max_length=64)),('metadata',models.JSONField(blank=True,default=dict)),('created_at',models.DateTimeField(auto_now_add=True)),('actor',models.ForeignKey(blank=True,null=True,on_delete=django.db.models.deletion.SET_NULL,to=settings.AUTH_USER_MODEL))])]
