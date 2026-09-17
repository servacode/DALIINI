import uuid
from django.contrib.gis.db import models
from django.db import migrations
import django.db.models.deletion
class Migration(migrations.Migration):
    initial=True; dependencies=[]
    operations=[
      migrations.CreateModel(name='Province',fields=[('id',models.UUIDField(default=uuid.uuid4,editable=False,primary_key=True,serialize=False)),('code',models.CharField(max_length=40,unique=True)),('name_ar',models.CharField(max_length=120)),('name_en',models.CharField(blank=True,max_length=120)),('active',models.BooleanField(default=False)),('sort_order',models.PositiveIntegerField(default=0))]),
      migrations.CreateModel(name='City',fields=[('id',models.UUIDField(default=uuid.uuid4,editable=False,primary_key=True,serialize=False)),('code',models.CharField(max_length=80)),('name_ar',models.CharField(max_length=120)),('name_en',models.CharField(blank=True,max_length=120)),('boundary',models.MultiPolygonField(blank=True,null=True,srid=4326)),('active',models.BooleanField(default=True)),('province',models.ForeignKey(on_delete=django.db.models.deletion.CASCADE,related_name='cities',to='locations.province'))]),
      migrations.CreateModel(name='Neighborhood',fields=[('id',models.UUIDField(default=uuid.uuid4,editable=False,primary_key=True,serialize=False)),('name_ar',models.CharField(max_length=120)),('name_en',models.CharField(blank=True,max_length=120)),('boundary',models.MultiPolygonField(blank=True,null=True,srid=4326)),('active',models.BooleanField(default=True)),('city',models.ForeignKey(on_delete=django.db.models.deletion.CASCADE,related_name='neighborhoods',to='locations.city'))]),
      migrations.AddConstraint(model_name='city',constraint=models.UniqueConstraint(fields=('province','code'),name='uniq_city_code_per_province')),
    ]
