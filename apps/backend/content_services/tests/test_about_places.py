"""«من نحن» says what Dalini is: a directory of places, with no health service (DECISION-125)."""

import pytest
from rest_framework.test import APIClient


@pytest.mark.django_db
def test_about_describes_a_places_directory_and_no_health_service() -> None:
    body = APIClient().get("/api/v1/public/legal/about/").json()["bodyAr"]

    assert body.startswith("دليل الأماكن في محافظتك.")
    assert "الخدمات الصحية" not in body and "المنشآت الصحية" not in body
    assert "لا يقدّم خدمة أو استشارة طبية" in body
