from collections.abc import Iterable
from typing import Any

from rest_framework import serializers

from .models import BusinessHour, TemporaryClosure


class BusinessHourInputSerializer(serializers.Serializer):
    weekday = serializers.IntegerField(min_value=0, max_value=6)
    opensAt = serializers.TimeField(source="opens_at")
    closesAt = serializers.TimeField(source="closes_at")
    # `06-DATA-MODEL.md` names this field `sequence`; the column stays `sort_order`.
    # Renaming the column would rewrite a migration for a naming preference, so the
    # translation lives here, at the boundary, where it belongs.
    sequence = serializers.IntegerField(source="sort_order", min_value=0, default=0)

    def validate(self, attrs):
        if attrs["opens_at"] == attrs["closes_at"]:
            raise serializers.ValidationError(
                "Opening and closing time cannot be equal."
            )
        return attrs


class TemporaryClosureSerializer(serializers.ModelSerializer):
    startsAt = serializers.DateTimeField(source="starts_at")
    endsAt = serializers.DateTimeField(source="ends_at")

    class Meta:
        model = TemporaryClosure
        fields = ["id", "startsAt", "endsAt", "reason"]

    def validate(self, attrs):
        if attrs["ends_at"] <= attrs["starts_at"]:
            raise serializers.ValidationError(
                {"endsAt": "Must be after startsAt."}
            )
        return attrs


# See `DutyShiftInputSerializer` for why the request no longer reuses the response
# component (INT-049).
class TemporaryClosureInputSerializer(TemporaryClosureSerializer):
    """A temporary closure as the client sends it. The server assigns the id."""

    class Meta(TemporaryClosureSerializer.Meta):
        fields = ["startsAt", "endsAt", "reason"]


def serialize_hours(rows: Iterable[BusinessHour]) -> list[dict[str, Any]]:
    return [
        {
            "id": str(row.id),
            "weekday": row.weekday,
            "opensAt": row.opens_at.isoformat(),
            "closesAt": row.closes_at.isoformat(),
            "sequence": row.sort_order,
        }
        for row in rows
    ]
