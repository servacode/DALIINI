"""Request and response contract for the analytics intake endpoint."""

from rest_framework import serializers


class AnalyticsEventRequestSerializer(serializers.Serializer):
    """The view reads the body directly; this describes the accepted shape.

    `properties` is validated server-side against the central event registry, which
    rejects the forbidden keys listed in `16-ADS-ANALYTICS-AUDIT.md` such as raw
    coordinates, phone numbers and tokens.
    """

    name = serializers.CharField(help_text="Registered event name.")
    properties = serializers.DictField(
        required=False,
        help_text="Event properties, restricted to the keys declared for this event.",
    )
    occurredAt = serializers.DateTimeField(
        required=False,
        help_text="Client-side event time, ISO-8601. Defaults to receipt time.",
    )


class AnalyticsEventAcceptedSerializer(serializers.Serializer):
    accepted = serializers.BooleanField()
    id = serializers.UUIDField()
