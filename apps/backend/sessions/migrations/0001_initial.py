import uuid
from django.conf import settings
from django.db import migrations, models
import django.db.models.deletion
class Migration(migrations.Migration):
    initial=True; dependencies=[migrations.swappable_dependency(settings.AUTH_USER_MODEL)]
    operations=[migrations.CreateModel(name='UserSession',fields=[('id',models.UUIDField(default=uuid.uuid4,editable=False,primary_key=True,serialize=False)),('refresh_digest',models.CharField(max_length=128,unique=True)),('created_at',models.DateTimeField(auto_now_add=True)),('expires_at',models.DateTimeField()),('revoked_at',models.DateTimeField(blank=True,null=True)),('rotated_to_digest',models.CharField(blank=True,max_length=128)),('user',models.ForeignKey(on_delete=django.db.models.deletion.CASCADE,related_name='sessions',to=settings.AUTH_USER_MODEL))])]
