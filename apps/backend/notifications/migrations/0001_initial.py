import uuid
from django.conf import settings
from django.db import migrations, models
import django.db.models.deletion


class Migration(migrations.Migration):
    initial = True
    dependencies = [migrations.swappable_dependency(settings.AUTH_USER_MODEL), ("directory_sessions", "0001_initial")]
    operations = [
        migrations.CreateModel(name="Notification", fields=[
            ("id",models.UUIDField(default=uuid.uuid4,editable=False,primary_key=True,serialize=False)),
            ("type",models.CharField(max_length=80)),("payload",models.JSONField(blank=True,default=dict)),
            ("read_at",models.DateTimeField(blank=True,null=True)),("created_at",models.DateTimeField(auto_now_add=True)),
            ("user",models.ForeignKey(on_delete=django.db.models.deletion.CASCADE,related_name="notifications",to=settings.AUTH_USER_MODEL)),
        ]),
        migrations.CreateModel(name="DevicePushToken", fields=[
            ("id",models.UUIDField(default=uuid.uuid4,editable=False,primary_key=True,serialize=False)),
            ("platform",models.CharField(choices=[("ANDROID","Android"),("IOS","iOS")],max_length=16)),
            ("token_digest",models.CharField(max_length=64,unique=True)),("token_ciphertext",models.TextField()),
            ("active",models.BooleanField(default=True)),("last_seen_at",models.DateTimeField(auto_now=True)),("created_at",models.DateTimeField(auto_now_add=True)),
            ("session",models.ForeignKey(blank=True,null=True,on_delete=django.db.models.deletion.SET_NULL,related_name="push_tokens",to="directory_sessions.usersession")),
            ("user",models.ForeignKey(on_delete=django.db.models.deletion.CASCADE,related_name="push_tokens",to=settings.AUTH_USER_MODEL)),
        ]),
        migrations.AddIndex(model_name="notification",index=models.Index(fields=["user","read_at","-created_at"],name="notificatio_user_id_9c4029_idx")),
        migrations.AddIndex(model_name="devicepushtoken",index=models.Index(fields=["user","active","platform"],name="notificatio_user_id_67bc12_idx")),
    ]
