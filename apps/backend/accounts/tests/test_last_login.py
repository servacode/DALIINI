"""A sign-in is recorded as the account's last one, which the console's account card shows.

Nothing set it, so every card read «لم يدخل بعد» — of people who had signed in that day.
"""

import pytest

from accounts.models import User
from accounts.services import create_session


@pytest.mark.django_db
def test_a_new_session_is_the_last_sign_in() -> None:
    user = User.objects.create_user(phone="+963900444092", password="Daliini2026x", name="قارئ")
    assert user.last_login is None

    create_session(user=user, platform="ANDROID", device_name="test device")

    user.refresh_from_db()
    assert user.last_login is not None
