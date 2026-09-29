import pytest
from django.db import IntegrityError, transaction

from accounts.models import User
from facilities.models import Facility
from ratings.models import Rating


@pytest.mark.django_db
def test_one_rating_per_user_facility(facility: Facility, user: User) -> None:
    Rating.objects.create(user=user, facility=facility, stars=5)
    with pytest.raises(IntegrityError), transaction.atomic():
        Rating.objects.create(user=user, facility=facility, stars=4)
