import re
import uuid
from urllib.parse import urlparse

from django.conf import settings
from django.core.exceptions import ValidationError
from django.db import models


class Advertisement(models.Model):
    class ActionType(models.TextChoices):
        NONE = "NONE", "None"
        FACILITY = "FACILITY", "Facility"
        CATEGORY = "CATEGORY", "Category"
        EXTERNAL_URL = "EXTERNAL_URL", "External URL"
        IN_APP_ROUTE = "IN_APP_ROUTE", "In-app route"

    class TargetScope(models.TextChoices):
        GLOBAL = "GLOBAL", "Global"
        PROVINCE = "PROVINCE", "Province"
        CATEGORY = "CATEGORY", "Category"

    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    image_key = models.CharField(max_length=500)
    title_ar = models.CharField(max_length=180, blank=True)
    title_en = models.CharField(max_length=180, blank=True)
    subtitle_ar = models.CharField(max_length=280, blank=True)
    subtitle_en = models.CharField(max_length=280, blank=True)
    action_type = models.CharField(
        max_length=24, choices=ActionType.choices, default=ActionType.NONE
    )
    action_payload = models.JSONField(default=dict, blank=True)
    target_scope = models.CharField(
        max_length=16, choices=TargetScope.choices, default=TargetScope.GLOBAL
    )
    province = models.ForeignKey(
        "locations.Province", null=True, blank=True, on_delete=models.CASCADE
    )
    category = models.ForeignKey(
        "directory.Category", null=True, blank=True, on_delete=models.CASCADE
    )
    starts_at = models.DateTimeField(null=True, blank=True)
    ends_at = models.DateTimeField(null=True, blank=True)
    enabled = models.BooleanField(default=False)
    sort_order = models.PositiveIntegerField(default=0)
    slide_duration_ms = models.PositiveIntegerField(default=5000)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        indexes = [
            models.Index(
                fields=["enabled", "starts_at", "ends_at"],
                name="content_ser_enabled_57493d_idx",
            ),
            models.Index(
                fields=["target_scope", "province", "category"],
                name="content_ser_target__67e917_idx",
            ),
        ]

    def __str__(self) -> str:
        return self.title_ar or str(self.id)

    def clean(self):
        errors = {}
        if self.starts_at and self.ends_at and self.ends_at <= self.starts_at:
            errors["ends_at"] = "Advertisement end must be after start."
        if not 2000 <= self.slide_duration_ms <= 30000:
            errors["slide_duration_ms"] = "Slide duration must be between 2000 and 30000 ms."
        if self.target_scope == self.TargetScope.GLOBAL and (self.province_id or self.category_id):
            errors["target_scope"] = "Global advertisements cannot target province/category."
        if self.target_scope == self.TargetScope.PROVINCE and not self.province_id:
            errors["province"] = "Province target requires province."
        if self.target_scope == self.TargetScope.CATEGORY and not self.category_id:
            errors["category"] = "Category target requires category."
        errors.update(_validate_action(self.action_type, self.action_payload))
        if errors:
            raise ValidationError(errors)


def _validate_action(action_type: str, payload: dict) -> dict:
    if not isinstance(payload, dict):
        return {"action_payload": "Action payload must be an object."}
    if action_type == Advertisement.ActionType.NONE:
        return {} if not payload else {"action_payload": "NONE action must have empty payload."}
    key_by_type = {
        Advertisement.ActionType.FACILITY: "facilityId",
        Advertisement.ActionType.CATEGORY: "categoryId",
        Advertisement.ActionType.EXTERNAL_URL: "url",
        Advertisement.ActionType.IN_APP_ROUTE: "route",
    }
    expected = key_by_type.get(action_type)
    if expected is None or set(payload) != {expected} or not isinstance(payload.get(expected), str):
        return {"action_payload": f"Action requires only {expected}."}
    if action_type == Advertisement.ActionType.EXTERNAL_URL:
        parsed = urlparse(payload["url"])
        if parsed.scheme != "https" or not parsed.netloc or parsed.username or parsed.password:
            return {"action_payload": "External URL must be a credential-free HTTPS URL."}
    if action_type == Advertisement.ActionType.IN_APP_ROUTE:
        route = payload["route"]
        if not route.startswith("/") or route.startswith("//"):
            return {"action_payload": "In-app route must be an absolute app route."}
    return {}


PAGE_KEY_PATTERN = re.compile(r"^[A-Z0-9](?:[A-Z0-9-]{0,62}[A-Z0-9])?$")


DIAL_PATTERN = re.compile(r"^\+?[0-9]{2,15}$")


def _validate_dial_string(value: str) -> None:
    if not DIAL_PATTERN.match(value or ""):
        raise ValidationError("Digits only, optionally with a leading +.")


def _validate_page_key(value: str) -> None:
    if not PAGE_KEY_PATTERN.match(value or ""):
        raise ValidationError("Use letters, digits and single hyphens (a URL slug).")


class LegalDocument(models.Model):
    """A page of words the platform owes its users: who it is, what it does with their data,
    the terms, how to use it, the common questions, and how to reach someone.

    These change for legal and product reasons long after an APK is signed, so they are not
    frozen into one. Each key keeps its history: a new version is a new row, and exactly one
    row per key is active at a time. A client caches what it last read and shows that when it
    is offline.
    """

    class Key(models.TextChoices):
        """The built-in pages the apps link to. They can be unpublished, never deleted."""

        ABOUT = "ABOUT", "About us"
        PRIVACY = "PRIVACY", "Privacy policy"
        TERMS = "TERMS", "Terms and conditions"
        INSTRUCTIONS = "INSTRUCTIONS", "How to use the app"
        FAQ = "FAQ", "Frequently asked questions"
        CONTACT = "CONTACT", "Contact us"

    class Kind(models.TextChoices):
        LEGAL = "LEGAL", "Legal"
        FAQ = "FAQ", "FAQ"
        PAGE = "PAGE", "Page"

    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    # The page's slug, stored upper-case ("PRIVACY", "HOW-TO-REPORT"). The built-in keys above
    # keep working; operators may add pages under any other slug (content pages, 2026-09-28).
    key = models.CharField(max_length=64, validators=[_validate_page_key])
    kind = models.CharField(max_length=8, choices=Kind.choices, default=Kind.PAGE)
    title_ar = models.CharField(max_length=180)
    body_ar = models.TextField()
    version = models.PositiveIntegerField(default=1)
    active = models.BooleanField(default=False)
    published_at = models.DateTimeField(null=True, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        constraints = [
            models.UniqueConstraint(fields=["key", "version"], name="uniq_legal_key_version"),
            # One published version per key: the app asks for a key and gets one answer.
            models.UniqueConstraint(
                fields=["key"],
                condition=models.Q(active=True),
                name="uniq_active_legal_document_per_key",
            ),
        ]
        indexes = [models.Index(fields=["key", "active"], name="content_legal_key_idx")]

    def __str__(self) -> str:
        return f"{self.key} v{self.version}"

    def clean(self):
        if self.active and self.published_at is None:
            raise ValidationError({"published_at": "An active document needs a publication time."})


class FaqEntry(models.Model):
    """One question and its answer, shown in order on the public FAQ."""

    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    question_ar = models.CharField(max_length=300)
    answer_ar = models.TextField(max_length=4000)
    sort_order = models.PositiveIntegerField(default=0)
    published = models.BooleanField(default=False)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        ordering = ["sort_order", "created_at"]

    def __str__(self) -> str:
        return self.question_ar


class EmergencyNumber(models.Model):
    """A number to call in an emergency. No province means national: shown everywhere."""

    class Kind(models.TextChoices):
        AMBULANCE = "AMBULANCE", "Ambulance"
        FIRE = "FIRE", "Fire"
        POLICE = "POLICE", "Police"
        HOSPITAL = "HOSPITAL", "Hospital"
        OTHER = "OTHER", "Other"

    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    province = models.ForeignKey(
        "locations.Province",
        null=True,
        blank=True,
        on_delete=models.CASCADE,
        related_name="emergency_numbers",
    )
    label_ar = models.CharField(max_length=120)
    # Short codes (110) as well as full numbers, so not forced into E.164.
    phone = models.CharField(max_length=20, validators=[_validate_dial_string])
    kind = models.CharField(max_length=16, choices=Kind.choices, default=Kind.OTHER)
    sort_order = models.PositiveIntegerField(default=0)
    active = models.BooleanField(default=True)
    # For operators only, never public: where the number came from, what to verify.
    admin_note = models.CharField(max_length=240, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        ordering = ["sort_order", "label_ar"]
        indexes = [models.Index(fields=["province", "active"], name="content_emergency_prov_idx")]

    def __str__(self) -> str:
        return f"{self.label_ar} {self.phone}"


class ContactMessage(models.Model):
    """A message sent through the public contact form. No IP address or device is kept."""

    class Kind(models.TextChoices):
        GENERAL = "GENERAL", "General"
        OWNER = "OWNER", "Facility owner"
        CORRECTION = "CORRECTION", "Correction"

    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    user = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        null=True,
        blank=True,
        on_delete=models.SET_NULL,
        related_name="+",
    )
    name = models.CharField(max_length=120)
    phone = models.CharField(max_length=20, blank=True)
    message = models.CharField(max_length=1000)
    kind = models.CharField(max_length=16, choices=Kind.choices, default=Kind.GENERAL)
    handled_at = models.DateTimeField(null=True, blank=True)
    handled_by = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        null=True,
        blank=True,
        on_delete=models.SET_NULL,
        related_name="+",
    )
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        indexes = [models.Index(fields=["handled_at", "-created_at"], name="content_contact_idx")]

    def __str__(self) -> str:
        return f"{self.kind} {self.created_at:%Y-%m-%d}"
