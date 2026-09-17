from rest_framework import serializers

from .models import TemporaryClosure


class BusinessHourInputSerializer(serializers.Serializer):
    weekday = serializers.IntegerField(min_value=0, max_value=6)
    opensAt = serializers.TimeField(source="opens_at")
    closesAt = serializers.TimeField(source="closes_at")
    sortOrder = serializers.IntegerField(source="sort_order", min_value=0, default=0)

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


def serialize_hours(rows):
    return [
        {
            "id": str(row.id),
            "weekday": row.weekday,
            "opensAt": row.opens_at.isoformat(),
            "closesAt": row.closes_at.isoformat(),
            "sortOrder": row.sort_order,
        }
        for row in rows
    ]
