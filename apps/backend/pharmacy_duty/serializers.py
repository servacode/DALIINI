from rest_framework import serializers

from .models import DutyShift


class DutyShiftSerializer(serializers.ModelSerializer):
    startsAt = serializers.DateTimeField(source="starts_at")
    endsAt = serializers.DateTimeField(source="ends_at")

    class Meta:
        model = DutyShift
        fields = ["id", "startsAt", "endsAt"]

    def validate(self, attrs):
        starts_at = attrs.get("starts_at", getattr(self.instance, "starts_at", None))
        ends_at = attrs.get("ends_at", getattr(self.instance, "ends_at", None))
        if starts_at and ends_at and ends_at <= starts_at:
            raise serializers.ValidationError(
                {"endsAt": "Must be after startsAt."}
            )
        return attrs


# The response component carries the server-assigned `id` as required, so a request that
# reused it forced generated clients to invent an id before the server had assigned one
# (INT-049). The request is the same shape minus that field.
class DutyShiftInputSerializer(DutyShiftSerializer):
    """A duty shift as the client sends it. The server assigns the id."""

    class Meta(DutyShiftSerializer.Meta):
        fields = ["startsAt", "endsAt"]
