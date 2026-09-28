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
    # Optional WhatsApp contact, a Syrian mobile in E.164 (+9639XXXXXXXX), shown publicly.
    whatsapp = models.CharField(max_length=16, blank=True)
    address_ar = models.CharField(max_length=255, blank=True)
    address_en = models.CharField(max_length=255, blank=True)
    location = models.PointField(srid=4326, null=True, blank=True)
    status = models.CharField(max_length=40, choices=Status.choices, default=Status.DRAFT)
    activated_at = models.DateTimeField(null=True, blank=True)
    # When an operator last approved an application of this facility (trust signal).
    last_verified_at = models.DateTimeField(null=True, blank=True)
    # When a member last confirmed the opening hours are still right, or replaced them. Feeds
    # the public `infoConfirmedAt` together with `last_verified_at`, which keeps its meaning.
    hours_confirmed_at = models.DateTimeField(null=True, blank=True)
    # When the weekly "are your hours still right?" reminder last went out, so a rerun of the
    # weekly task in the same week sends nothing twice.
    hours_reminder_sent_at = models.DateTimeField(null=True, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        indexes = [
            models.Index(
                fields=["province", "category", "status"],
                name="facility_prov_cat_status_idx",
            )
        ]


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

    class Meta:
        constraints = [
            # 06-DATA-MODEL: only one active submitted application of the applicable
            # kind per facility. Created by facilities/0003_owner_media_integrity;
            # declared here so model state matches the migration and the database.
            models.UniqueConstraint(
                fields=("facility", "kind"),
                condition=models.Q(status="SUBMITTED"),
                name="uniq_submitted_application_per_facility_kind",
            )
        ]


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


class FacilityReport(models.Model):
    """A public "report a problem" about a facility, triaged by operators."""

    class Reason(models.TextChoices):
        WRONG_INFO = "WRONG_INFO", "Wrong information"
        CLOSED_PERMANENTLY = "CLOSED_PERMANENTLY", "Closed permanently"
        WRONG_LOCATION = "WRONG_LOCATION", "Wrong location"
        WRONG_HOURS = "WRONG_HOURS", "Wrong hours"
        NOT_ON_DUTY = "NOT_ON_DUTY", "Not on duty"
        OTHER = "OTHER", "Other"

    class Status(models.TextChoices):
        OPEN = "OPEN", "Open"
        RESOLVED = "RESOLVED", "Resolved"
        DISMISSED = "DISMISSED", "Dismissed"

    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    facility = models.ForeignKey(Facility, on_delete=models.CASCADE, related_name="reports")
    reporter = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        null=True,
        blank=True,
        on_delete=models.SET_NULL,
        related_name="+",
    )
    reason = models.CharField(max_length=24, choices=Reason.choices)
    note = models.CharField(max_length=500, blank=True)
    status = models.CharField(max_length=16, choices=Status.choices, default=Status.OPEN)
    created_at = models.DateTimeField(auto_now_add=True)
    resolved_by = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        null=True,
        blank=True,
        on_delete=models.SET_NULL,
        related_name="+",
    )
    resolved_at = models.DateTimeField(null=True, blank=True)

    class Meta:
        indexes = [
            models.Index(fields=["status", "-created_at"], name="facility_report_status_idx"),
        ]


class RejectionTemplate(models.Model):
    """A reviewer's ready-made rejection reason; picking one fills the reason, nothing more."""

    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    title_ar = models.CharField(max_length=120)
    body_ar = models.CharField(max_length=1000)
    active = models.BooleanField(default=True)
    sort_order = models.PositiveIntegerField(default=0)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        ordering = ["sort_order", "title_ar"]

    def __str__(self) -> str:
        return self.title_ar
