import uuid
from django.db import migrations, models
import django.db.models.deletion

class Migration(migrations.Migration):
    initial=True
    dependencies=[('facilities','0001_initial')]
    operations=[
        migrations.CreateModel(name='BusinessHour',fields=[('id',models.UUIDField(default=uuid.uuid4,editable=False,primary_key=True,serialize=False)),('weekday',models.PositiveSmallIntegerField()),('opens_at',models.TimeField()),('closes_at',models.TimeField()),('sort_order',models.PositiveSmallIntegerField(default=0)),('facility',models.ForeignKey(on_delete=django.db.models.deletion.CASCADE,related_name='business_hours',to='facilities.facility'))],options={'ordering':['weekday','opens_at','sort_order']}),
        migrations.CreateModel(name='TemporaryClosure',fields=[('id',models.UUIDField(default=uuid.uuid4,editable=False,primary_key=True,serialize=False)),('starts_at',models.DateTimeField()),('ends_at',models.DateTimeField()),('reason',models.CharField(blank=True,max_length=240)),('created_at',models.DateTimeField(auto_now_add=True)),('facility',models.ForeignKey(on_delete=django.db.models.deletion.CASCADE,related_name='temporary_closures',to='facilities.facility'))]),
        migrations.AddConstraint(model_name='businesshour',constraint=models.CheckConstraint(condition=models.Q(('weekday__gte',0),('weekday__lte',6)),name='business_hour_valid_weekday')),
        migrations.AddConstraint(model_name='businesshour',constraint=models.UniqueConstraint(fields=('facility','weekday','opens_at','closes_at'),name='uniq_facility_business_hour')),
        migrations.AddIndex(model_name='temporaryclosure',index=models.Index(fields=['facility','starts_at','ends_at'],name='business_ho_facilit_idx')),
    ]
