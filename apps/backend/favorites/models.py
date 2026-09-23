import uuid

from django.conf import settings
from django.db import models


class Favorite(models.Model):
    """A facility one account has saved.

    The relation carries nothing but who saved what and when: a saved facility is a pointer,
    not a copy, so a facility that closes or is suspended stops being served to the public
    from the one place that decides that — the public facility query — rather than from a
    stale row here.
    """

    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    user = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.CASCADE,
        related_name="favorites",
    )
    facility = models.ForeignKey(
        "facilities.Facility",
        on_delete=models.CASCADE,
        related_name="favorited_by",
    )
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        constraints = [
            models.UniqueConstraint(
                fields=["user", "facility"],
                name="uniq_user_facility_favorite",
            ),
        ]
        indexes = [
            # The account's own list, newest first, and the membership test a facility page
            # makes for one facility.
            models.Index(fields=["user", "-created_at"], name="favorites_user_recent_idx"),
            models.Index(fields=["facility", "user"], name="favorites_facility_user_idx"),
        ]

    def __str__(self) -> str:
        return f"{self.user_id} → {self.facility_id}"
