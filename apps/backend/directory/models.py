import uuid
from django.core.exceptions import ValidationError
from django.db import models
class CategoryGroup(models.Model):
    id=models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    code=models.CharField(max_length=80,unique=True)
    name_ar=models.CharField(max_length=120)
    name_en=models.CharField(max_length=120,blank=True)
    active=models.BooleanField(default=True)
    sort_order=models.PositiveIntegerField(default=0)
class Category(models.Model):
    class Specialization(models.TextChoices):
        GENERIC='GENERIC','Generic'; PHARMACY='PHARMACY','Pharmacy'; DOCTOR='DOCTOR','Doctor'; NURSING='NURSING','Nursing'; MEDICAL_SUPPLIES='MEDICAL_SUPPLIES','Medical supplies'
    id=models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    group=models.ForeignKey(CategoryGroup,on_delete=models.PROTECT,related_name='categories')
    code=models.CharField(max_length=80,unique=True)
    slug=models.SlugField(max_length=100,unique=True)
    name_ar=models.CharField(max_length=120)
    name_en=models.CharField(max_length=120,blank=True)
    icon_key=models.CharField(max_length=80,blank=True)
    specialization=models.CharField(max_length=40,choices=Specialization.choices,default=Specialization.GENERIC)
    active=models.BooleanField(default=True)
    sort_order=models.PositiveIntegerField(default=0)
    def clean(self):
        if self.pk and hasattr(self,'capabilities') and self.capabilities.supports_duty and self.specialization != self.Specialization.PHARMACY:
            raise ValidationError({'specialization':'Duty is only supported for pharmacy specialization.'})
class CategoryCapabilities(models.Model):
    category=models.OneToOneField(Category,on_delete=models.CASCADE,related_name='capabilities')
    supports_hours=models.BooleanField(default=True); supports_photos=models.BooleanField(default=True); supports_ratings=models.BooleanField(default=True)
    supports_duty=models.BooleanField(default=False); supports_specialty_filter=models.BooleanField(default=False); supports_service_filter=models.BooleanField(default=False)
    supports_temporary_closure=models.BooleanField(default=True); supports_owner_onboarding=models.BooleanField(default=True)
    def clean(self):
        if self.supports_duty and self.category.specialization != Category.Specialization.PHARMACY:
            raise ValidationError({'supports_duty':'Duty capability requires pharmacy specialization.'})
class CategoryProvince(models.Model):
    category=models.ForeignKey(Category,on_delete=models.CASCADE,related_name='province_switches')
    province=models.ForeignKey('locations.Province',on_delete=models.CASCADE,related_name='category_switches')
    public_enabled=models.BooleanField(default=False); owner_registration_enabled=models.BooleanField(default=False); sort_order=models.PositiveIntegerField(default=0)
    class Meta: constraints=[models.UniqueConstraint(fields=['category','province'],name='uniq_category_province')]
class VerificationRequirement(models.Model):
    category=models.ForeignKey(Category,on_delete=models.CASCADE,related_name='verification_requirements')
    label_ar=models.CharField(max_length=160); label_en=models.CharField(max_length=160,blank=True); instructions_ar=models.TextField(blank=True); instructions_en=models.TextField(blank=True)
    required=models.BooleanField(default=True); active=models.BooleanField(default=True); min_files=models.PositiveSmallIntegerField(default=1); max_files=models.PositiveSmallIntegerField(default=1); sort_order=models.PositiveIntegerField(default=0)
    def clean(self):
        if self.max_files < self.min_files: raise ValidationError({'max_files':'Must be >= min_files.'})
class Specialty(models.Model):
    category=models.ForeignKey(Category,null=True,blank=True,on_delete=models.CASCADE,related_name='specialties')
    specialization=models.CharField(max_length=40,blank=True)
    name_ar=models.CharField(max_length=120); name_en=models.CharField(max_length=120,blank=True); active=models.BooleanField(default=True); sort_order=models.PositiveIntegerField(default=0)
    def clean(self):
        scoped=bool(self.category_id); spec=bool(self.specialization.strip())
        if scoped == spec: raise ValidationError('Exactly one scope is required.')
class ServiceTag(models.Model):
    category=models.ForeignKey(Category,on_delete=models.CASCADE,related_name='service_tags')
    name_ar=models.CharField(max_length=120); name_en=models.CharField(max_length=120,blank=True); active=models.BooleanField(default=True); sort_order=models.PositiveIntegerField(default=0)
