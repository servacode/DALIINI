"""How many a broadcast would reach, asked before it is sent."""

from typing import Any

import pytest

from accounts.models import User
from locations.models import Province

URL = "/api/v1/admin/notifications/broadcast/audience/"


@pytest.mark.django_db
def test_the_count_follows_the_audience_and_the_province(admin_api: Any) -> None:
    raqqa = Province.objects.create(code="qa-raqqa", name_ar="الرقة", active=True)
    empty = Province.objects.create(code="qa-empty", name_ar="فارغة", active=True)
    User.objects.create_user(phone="+963933000811", password="Daliini2026x", province=raqqa)
    sender = admin_api("admin.notifications.send")

    everyone = sender.get(URL, {"audience": "ALL"}).json()["count"]
    in_raqqa = sender.get(URL, {"audience": "ALL", "provinceId": str(raqqa.pk)}).json()["count"]
    nobody = sender.get(URL, {"audience": "ALL", "provinceId": str(empty.pk)}).json()["count"]

    assert everyone >= in_raqqa == 1
    assert nobody == 0


@pytest.mark.django_db
def test_an_unknown_audience_is_refused(admin_api: Any) -> None:
    assert admin_api("admin.notifications.send").get(URL, {"audience": "X"}).status_code == 400


@pytest.mark.django_db
def test_counting_needs_the_sending_permission(admin_api: Any) -> None:
    assert admin_api("admin.users.read").get(URL, {"audience": "ALL"}).status_code == 403
