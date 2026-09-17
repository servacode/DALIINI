"""Response contract for the public taxonomy endpoints."""

from rest_framework import serializers

from core.openapi import NamedRefSerializer


class CategoryCapabilitiesSerializer(serializers.Serializer):
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


class PublicCategorySerializer(serializers.Serializer):
    id = serializers.UUIDField()
    nameAr = serializers.CharField()
    nameEn = serializers.CharField(allow_null=True)
    iconKey = serializers.CharField(allow_null=True)
    group = NamedRefSerializer()
    capabilities = CategoryCapabilitiesSerializer()


class PublicCategoryListSerializer(serializers.Serializer):
    items = PublicCategorySerializer(many=True)
