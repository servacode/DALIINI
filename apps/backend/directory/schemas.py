"""Response contract for the public taxonomy endpoints."""

from typing import Any

from rest_framework import serializers

from core.openapi import NamedIntRefSerializer, NamedRefSerializer


class CategoryCapabilitiesSerializer(serializers.Serializer[Any]):
    """Capability flags that drive client UI, as returned by the public category list.

    Clients branch on these flags rather than on a category name, per
    `01-MASTER-SPECIFICATION.md` section 7.
    """

    hours = serializers.BooleanField()
    photos = serializers.BooleanField()
    ratings = serializers.BooleanField()
    duty = serializers.BooleanField()
    specialtyFilter = serializers.BooleanField()
    serviceFilter = serializers.BooleanField()
    temporaryClosure = serializers.BooleanField()
    ownerOnboarding = serializers.BooleanField()


class PublicCategorySerializer(serializers.Serializer[Any]):
    id = serializers.UUIDField()
    nameAr = serializers.CharField()
    nameEn = serializers.CharField(allow_null=True)
    iconKey = serializers.CharField(allow_null=True)
    group = NamedRefSerializer()
    capabilities = CategoryCapabilitiesSerializer()


class PublicCategoryListSerializer(serializers.Serializer[Any]):
    items = PublicCategorySerializer(many=True)


class PublicCategoryTagsSerializer(serializers.Serializer[Any]):
    """What a category's facilities can be filtered by, and what an owner can pick."""

    specialties = NamedIntRefSerializer(
        many=True,
        help_text=(
            "Active specialties, in the operators' order: the category's own and those of "
            "its specialization. Send an id back as `specialtyId`."
        ),
    )
    services = NamedIntRefSerializer(
        many=True,
        help_text="Active services of the category, in order. Send an id back as `serviceTagId`.",
    )
