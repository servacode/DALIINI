from datetime import timedelta

import pytest
from django.db import IntegrityError, transaction
from django.utils import timezone

from pharmacy_duty.models import DutyShift


@pytest.mark.django_db(transaction=True)
def test_database_rejects_overlapping_duty(facility):
    start = timezone.now() + timedelta(hours=1)
    DutyShift.objects.create(
        facility=facility,
        starts_at=start,
        ends_at=start + timedelta(hours=2),
    )
    with pytest.raises(IntegrityError), transaction.atomic():
        DutyShift.objects.create(
            facility=facility,
            starts_at=start + timedelta(minutes=30),
            ends_at=start + timedelta(hours=3),
        )
