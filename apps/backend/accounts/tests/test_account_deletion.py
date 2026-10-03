"""Deleting an account removes what was only ever the person's own.

The anonymisation used to blank the name and phone but keep the free-text address, leave the
profile picture reachable at its public address, and keep the saved places and the inbox.
"""

import pytest

from accounts.models import User
from accounts.services import request_account_deletion
from facilities.models import Facility
from favorites.models import Favorite
from notifications.models import Notification
from ratings.models import Rating


@pytest.mark.django_db
def test_deletion_clears_personal_data(
    monkeypatch: pytest.MonkeyPatch,
    django_capture_on_commit_callbacks: object,
    facility: Facility,
    user: User,
) -> None:
    removed: list[str] = []
    monkeypatch.setattr("accounts.services.delete_profile_image", removed.append)
    user.address = "حي الثكنة، قرب الجامع"
    user.profile_image_key = "accounts/x/avatar/abc.jpg"
    user.save(update_fields=["address", "profile_image_key"])
    Favorite.objects.create(user=user, facility=facility)
    Notification.objects.create(user=user, type="system.notice", title_ar="رسالة")
    Rating.objects.create(user=user, facility=facility, stars=4)

    with django_capture_on_commit_callbacks(execute=True):  # type: ignore[operator]
        request_account_deletion(user=user)

    user.refresh_from_db()
    assert user.address == ""
    assert user.profile_image_key == ""
    assert user.name == "Deleted user"
    assert not user.is_active
    assert removed == ["accounts/x/avatar/abc.jpg"]
    assert not Favorite.objects.filter(user=user).exists()
    assert not Notification.objects.filter(user=user).exists()
    # A rating is part of the facility's public score and stays, no longer attributable.
    assert Rating.objects.filter(user=user, facility=facility).exists()


@pytest.mark.django_db
def test_deletion_without_a_picture_removes_nothing_from_storage(
    monkeypatch: pytest.MonkeyPatch,
    django_capture_on_commit_callbacks: object,
    user: User,
) -> None:
    removed: list[str] = []
    monkeypatch.setattr("accounts.services.delete_profile_image", removed.append)

    with django_capture_on_commit_callbacks(execute=True):  # type: ignore[operator]
        request_account_deletion(user=user)

    assert removed == []
