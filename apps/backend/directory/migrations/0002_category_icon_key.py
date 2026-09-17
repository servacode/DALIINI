from django.db import migrations, models


class Migration(migrations.Migration):
    dependencies = [("directory", "0001_initial")]
    operations = [
        migrations.AddField(
            model_name="category",
            name="icon_key",
            field=models.CharField(blank=True, max_length=80),
        )
    ]
