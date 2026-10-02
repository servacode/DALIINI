"""Wire contract for content pages, the FAQ, emergency numbers and the contact form."""

from typing import Any

from rest_framework import serializers

from .models import AppRelease, ContactMessage, EmergencyNumber, LegalDocument


class ContentPageSerializer(serializers.Serializer[Any]):
    slug = serializers.CharField(help_text="Lower-case URL slug, for example `privacy`.")
    kind = serializers.ChoiceField(choices=LegalDocument.Kind.choices)
    titleAr = serializers.CharField()
    bodyAr = serializers.CharField(
        help_text=(
            "Plain text. Paragraphs are separated by a blank line; a line starting with `- ` "
            "is a list item and one starting with `## ` a heading. No HTML is ever sent."
        )
    )
    version = serializers.IntegerField()
    publishedAt = serializers.DateTimeField(allow_null=True)
    updatedAt = serializers.DateTimeField()


class FaqEntrySerializer(serializers.Serializer[Any]):
    id = serializers.UUIDField()
    questionAr = serializers.CharField()
    answerAr = serializers.CharField()
    sortOrder = serializers.IntegerField()


class FaqListSerializer(serializers.Serializer[Any]):
    items = FaqEntrySerializer(many=True)


EMERGENCY_SCOPES = [("NATIONAL", "National"), ("PROVINCE", "Province")]


class EmergencyNumberSerializer(serializers.Serializer[Any]):
    id = serializers.UUIDField()
    scope = serializers.ChoiceField(
        choices=EMERGENCY_SCOPES,
        help_text="NATIONAL numbers apply everywhere; PROVINCE ones only to `provinceId`.",
    )
    provinceId = serializers.UUIDField(allow_null=True)
    labelAr = serializers.CharField()
    phone = serializers.CharField(help_text="What to dial, digits with an optional leading +.")
    kind = serializers.ChoiceField(choices=EmergencyNumber.Kind.choices)
    sortOrder = serializers.IntegerField()


class EmergencyNumberListSerializer(serializers.Serializer[Any]):
    items = EmergencyNumberSerializer(many=True)


class ContactMessageRequestSerializer(serializers.Serializer[Any]):
    name = serializers.CharField(max_length=120)
    phone = serializers.CharField(
        max_length=20,
        required=False,
        allow_blank=True,
        help_text="Optional Syrian mobile, so the team can call back. Stored as E.164.",
    )
    message = serializers.CharField(max_length=1000)
    kind = serializers.ChoiceField(
        choices=ContactMessage.Kind.choices, required=False, default=ContactMessage.Kind.GENERAL
    )


class ContactMessageCreatedSerializer(serializers.Serializer[Any]):
    id = serializers.UUIDField()
    createdAt = serializers.DateTimeField()


class AppReleaseSerializer(serializers.Serializer[Any]):
    platform = serializers.ChoiceField(choices=AppRelease.Platform.choices)
    minimumVersionCode = serializers.IntegerField(
        help_text=(
            "A build below this must stop and say so. Zero means nothing is blocked, which is "
            "what an unconfigured backend answers."
        ),
    )
    latestVersionCode = serializers.IntegerField(
        help_text="A build below this may offer an update, but must keep working.",
    )
    storeUrl = serializers.CharField(
        allow_blank=True,
        help_text="Where to get the newer build. Empty means show the notice without a button.",
    )
    noticeAr = serializers.CharField(
        allow_blank=True,
        help_text="What the blocking screen says. Empty falls back to the app's own wording.",
    )
