"""Response contract for ratings.

Public facility payloads expose the aggregate only; an individual rating is visible to
its own author through the account endpoint, per `03-PRODUCT-SPECIFICATION.md` section 7.
"""

from typing import Any

from rest_framework import serializers


class FacilityRatingSerializer(serializers.Serializer[Any]):
    facilityId = serializers.UUIDField()
    stars = serializers.IntegerField(min_value=1, max_value=5)


class AccountRatingSerializer(serializers.Serializer[Any]):
    facilityId = serializers.UUIDField()
    facilityNameAr = serializers.CharField()
    stars = serializers.IntegerField(min_value=1, max_value=5)
    updatedAt = serializers.DateTimeField()


class AccountRatingListSerializer(serializers.Serializer[Any]):
    items = AccountRatingSerializer(many=True)
