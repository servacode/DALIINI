import uuid

from django.conf import settings
from django.contrib.gis.db import models
from django.core.exceptions import ValidationError


class Facility(models.Model):
    class Status(models.TextChoices):
        DRAFT = "DRAFT", "Draft"
        SUBMITTED = "SUBMITTED", "Submitted"
        ACTIVE = "ACTIVE", "Active"
        REVERIFICATION_REQUIRED = "REVERIFICATION_REQUIRED", "Reverification required"
        SUSPENDED = "SUSPENDED", "Suspended"
        CLOSED = "CLOSED", "Closed"

    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    category = models.ForeignKey(
        "directory.Category",
        on_delete=models.PROTECT,
        related_name="facilities",
    )
    province = models.ForeignKey(
        "locations.Province",
        on_delete=models.PROTECT,
        related_name="facilities",
    )
    city = models.ForeignKey(
        "locations.City",
        null=True,
        blank=True,
        on_delete=models.SET_NULL,
    )
    neighborhood = models.ForeignKey(
        "locations.Neighborhood",
        null=True,
        blank=True,
        on_delete=models.SET_NULL,
    )
    name_ar = models.CharField(max_length=160)
    name_en = models.CharField(max_length=160, blank=True)
    description_ar = models.TextField(blank=True)
    description_en = models.TextField(blank=True)
    phone = models.CharField(max_length=16, blank=True)
    address_ar = models.CharField(max_length=255, blank=True)
    address_en = models.CharField(max_length=255, blank=True)
    location = models.PointField(srid=4326, null=True, blank=True)
    status = models.CharField(max_length=40, choices=Status.choices, default=Status.DRAFT)
    activated_at = models.DateTimeField(null=True, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        indexes = [models.Index(fields=["province", "category", "status"])]


class FacilityMembership(models.Model):
    class Role(models.TextChoices):
        OWNER = "OWNER", "Owner"
        MANAGER = "MANAGER", "Manager"

    facility = models.ForeignKey(
        Facility,
        on_delete=models.CASCADE,
        related_name="memberships",
    )
    user = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.CASCADE,
        related_name="facility_memberships",
    )
    role = models.CharField(max_length=20, choices=Role.choices)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        constraints = [
            models.UniqueConstraint(
                fields=["facility", "user"],
                name="uniq_facility_member",
            )
        ]

    def clean(self):
        if self.pk and self.role != self.Role.OWNER:
            original = type(self).objects.get(pk=self.pk)
            another_owner_exists = (
                type(self)
                .objects.filter(facility=self.facility, role=self.Role.OWNER)
                .exclude(pk=self.pk)
                .exists()
            )
            if original.role == self.Role.OWNER and not another_owner_exists:
                raise ValidationError("Cannot remove the last owner.")


class FacilityApplication(models.Model):
    class Kind(models.TextChoices):
        INITIAL = "INITIAL", "Initial"
        REVERIFICATION = "REVERIFICATION", "Reverification"

    class Status(models.TextChoices):
        DRAFT = "DRAFT", "Draft"
        SUBMITTED = "SUBMITTED", "Submitted"
        APPROVED = "APPROVED", "Approved"
        REJECTED = "REJECTED", "Rejected"

    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    facility = models.ForeignKey(
        Facility,
        on_delete=models.CASCADE,
        related_name="applications",
    )
    kind = models.CharField(max_length=24, choices=Kind.choices)
    status = models.CharField(max_length=20, choices=Status.choices, default=Status.DRAFT)
    snapshot = models.JSONField(default=dict, blank=True)
    submitted_at = models.DateTimeField(null=True, blank=True)
    reviewed_at = models.DateTimeField(null=True, blank=True)
    reviewed_by = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        null=True,
        blank=True,
        on_delete=models.SET_NULL,
        related_name="+",
    )
    rejection_reason = models.TextField(blank=True)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)


class FacilityImage(models.Model):
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    facility = models.ForeignKey(
        Facility,
        on_delete=models.CASCADE,
        related_name="images",
    )
    storage_key = models.CharField(max_length=500, unique=True)
    sort_order = models.PositiveIntegerField(default=0)
    width = models.PositiveIntegerField(default=0)
    height = models.PositiveIntegerField(default=0)
    created_at = models.DateTimeField(auto_now_add=True)


class VerificationEvidence(models.Model):
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    facility = models.ForeignKey(
        Facility,
        on_delete=models.CASCADE,
        related_name="evidence",
    )
    requirement = models.ForeignKey(
        "directory.VerificationRequirement",
        on_delete=models.PROTECT,
        related_name="+",
    )
    storage_key = models.CharField(max_length=500, unique=True)
    uploaded_by = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        null=True,
        blank=True,
        on_delete=models.SET_NULL,
        related_name="+",
    )
    created_at = models.DateTimeField(auto_now_add=True)


class FacilitySpecialty(models.Model):
    facility = models.ForeignKey(
        Facility,
        on_delete=models.CASCADE,
        related_name="specialty_links",
    )
    specialty = models.ForeignKey(
        "directory.Specialty",
        on_delete=models.PROTECT,
        related_name="facility_links",
    )

    class Meta:
        constraints = [
            models.UniqueConstraint(
                fields=["facility", "specialty"],
                name="uniq_facility_specialty",
            )
        ]


class FacilityServiceTag(models.Model):
    facility = models.ForeignKey(
        Facility,
        on_delete=models.CASCADE,
        related_name="service_links",
    )
    service_tag = models.ForeignKey(
        "directory.ServiceTag",
        on_delete=models.PROTECT,
        related_name="facility_links",
    )

    class Meta:
        constraints = [
            models.UniqueConstraint(
                fields=["facility", "service_tag"],
                name="uniq_facility_service_tag",
            )
        ]
