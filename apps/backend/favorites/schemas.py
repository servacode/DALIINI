"""Response contract for saved facilities.

These serializers describe what the views return; the runtime payload stays the single
source of truth, as in every other app here.
"""

from rest_framework import serializers

from search.schemas import CompactFacilitySerializer


class FavoriteFacilitySerializer(CompactFacilitySerializer):
    favoritedAt = serializers.DateTimeField(
        help_text="When the caller saved this facility."
    )


class FavoriteListSerializer(serializers.Serializer):  # type: ignore[type-arg]
    items = FavoriteFacilitySerializer(many=True)
    nextCursor = serializers.CharField(allow_null=True)
    hasMore = serializers.BooleanField()


class FavoriteWriteSerializer(serializers.Serializer):  # type: ignore[type-arg]
    """Which facility to save. The caller is whoever the token says it is."""

    facilityId = serializers.UUIDField()


class FavoriteStateSerializer(serializers.Serializer):  # type: ignore[type-arg]
    facilityId = serializers.UUIDField()
    isFavorite = serializers.BooleanField()
