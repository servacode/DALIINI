from django.conf import settings
from django.core.exceptions import ValidationError
from django.db import models


class PlatformSetting(models.Model):
    class ValueType(models.TextChoices):
        STRING = "STRING", "String"
        INTEGER = "INTEGER", "Integer"
        BOOLEAN = "BOOLEAN", "Boolean"
        JSON = "JSON", "JSON"

    key = models.CharField(max_length=120, unique=True)
    value_type = models.CharField(max_length=16, choices=ValueType.choices)
    value = models.JSONField()
    updated_by = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        null=True,
        blank=True,
        on_delete=models.SET_NULL,
        related_name="+",
    )
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    def __str__(self) -> str:
        return self.key

    def clean(self):
        expected = {
            self.ValueType.STRING: str,
            self.ValueType.INTEGER: int,
            self.ValueType.BOOLEAN: bool,
        }.get(self.value_type)
        if expected is not None and type(self.value) is not expected:
            raise ValidationError({"value": f"Expected {self.value_type.lower()} value."})
