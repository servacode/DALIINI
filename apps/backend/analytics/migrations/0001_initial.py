import uuid
from django.conf import settings
from django.db import migrations, models
import django.db.models.deletion


class Migration(migrations.Migration):
    initial = True
    dependencies = [migrations.swappable_dependency(settings.AUTH_USER_MODEL)]
    operations = [
        migrations.CreateModel(
            name="ProductAnalyticsEvent",
            fields=[
                ("id",models.UUIDField(default=uuid.uuid4,editable=False,primary_key=True,serialize=False)),
                ("anonymous_id_hash",models.CharField(blank=True,max_length=64)),
                ("name",models.CharField(max_length=80)),
                ("properties",models.JSONField(blank=True,default=dict)),
                ("occurred_at",models.DateTimeField()),
                ("received_at",models.DateTimeField(auto_now_add=True)),
                ("user",models.ForeignKey(blank=True,null=True,on_delete=django.db.models.deletion.SET_NULL,related_name="+",to=settings.AUTH_USER_MODEL)),
            ],
            options={"indexes":[models.Index(fields=["name","-occurred_at"],name="analytics_p_name_30ed27_idx"),models.Index(fields=["-received_at"],name="analytics_p_receive_7d02b7_idx")]},
        )
    ]
