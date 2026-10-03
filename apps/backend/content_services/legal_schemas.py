"""Response contract for the platform's own published pages."""

from rest_framework import serializers

from core.enums import LEGAL_DOCUMENT_KEY


class LegalDocumentSummarySerializer(serializers.Serializer):  # type: ignore[type-arg]
    key = serializers.ChoiceField(choices=LEGAL_DOCUMENT_KEY)
    titleAr = serializers.CharField()
    version = serializers.IntegerField()
    publishedAt = serializers.DateTimeField(allow_null=True)


class LegalDocumentSerializer(LegalDocumentSummarySerializer):
    bodyAr = serializers.CharField()


class LegalDocumentListSerializer(serializers.Serializer):  # type: ignore[type-arg]
    items = LegalDocumentSummarySerializer(many=True)
