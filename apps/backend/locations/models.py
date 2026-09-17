import uuid
from django.contrib.gis.db import models
class Province(models.Model):
    id=models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    code=models.CharField(max_length=40, unique=True)
    name_ar=models.CharField(max_length=120)
    name_en=models.CharField(max_length=120, blank=True)
    active=models.BooleanField(default=False)
    sort_order=models.PositiveIntegerField(default=0)
class City(models.Model):
    id=models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    province=models.ForeignKey(Province,on_delete=models.CASCADE,related_name='cities')
    code=models.CharField(max_length=80)
    name_ar=models.CharField(max_length=120)
    name_en=models.CharField(max_length=120,blank=True)
    boundary=models.MultiPolygonField(srid=4326,null=True,blank=True)
    active=models.BooleanField(default=True)
    class Meta: constraints=[models.UniqueConstraint(fields=['province','code'],name='uniq_city_code_per_province')]
class Neighborhood(models.Model):
    id=models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    city=models.ForeignKey(City,on_delete=models.CASCADE,related_name='neighborhoods')
    name_ar=models.CharField(max_length=120)
    name_en=models.CharField(max_length=120,blank=True)
    boundary=models.MultiPolygonField(srid=4326,null=True,blank=True)
    active=models.BooleanField(default=True)
