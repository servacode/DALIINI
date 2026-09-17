from django.db import migrations, models


class Migration(migrations.Migration):
    dependencies = [("audit", "0001_initial")]
    operations = [
        migrations.AddField(model_name="auditevent", name="before_snapshot", field=models.JSONField(blank=True, default=dict)),
        migrations.AddField(model_name="auditevent", name="after_snapshot", field=models.JSONField(blank=True, default=dict)),
        migrations.AddField(model_name="auditevent", name="request_id", field=models.CharField(blank=True, max_length=64)),
    ]
