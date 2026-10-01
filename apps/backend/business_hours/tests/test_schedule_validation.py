from datetime import time

import pytest
from django.core.exceptions import ValidationError

from business_hours.domain import HourInterval, validate_weekly_schedule


def test_rejects_overlap_same_day() -> None:
    with pytest.raises(ValidationError):
        validate_weekly_schedule(
            [
                HourInterval(0, time(9), time(13)),
                HourInterval(0, time(12), time(16)),
            ]
        )


def test_rejects_overlap_created_by_overnight_interval() -> None:
    with pytest.raises(ValidationError):
        validate_weekly_schedule(
            [
                HourInterval(0, time(20), time(2)),
                HourInterval(1, time(1), time(5)),
            ]
        )


def test_accepts_split_shift() -> None:
    validate_weekly_schedule(
        [
            HourInterval(0, time(9), time(12)),
            HourInterval(0, time(14), time(18)),
        ]
    )
