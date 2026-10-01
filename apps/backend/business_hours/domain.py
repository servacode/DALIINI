from __future__ import annotations

from dataclasses import dataclass
from datetime import time

from django.core.exceptions import ValidationError


@dataclass(frozen=True)
class HourInterval:
    weekday: int
    opens_at: time
    closes_at: time
    sort_order: int = 0


def _segments(interval: HourInterval) -> list[tuple[int, int, int]]:
    start = interval.opens_at.hour * 60 + interval.opens_at.minute
    end = interval.closes_at.hour * 60 + interval.closes_at.minute
    if start == end:
        raise ValidationError("Opening and closing time cannot be equal.")
    if end > start:
        return [(interval.weekday, start, end)]
    return [
        (interval.weekday, start, 24 * 60),
        ((interval.weekday + 1) % 7, 0, end),
    ]


def validate_weekly_schedule(intervals: list[HourInterval]) -> None:
    occupied: dict[int, list[tuple[int, int]]] = {i: [] for i in range(7)}
    for interval in intervals:
        if interval.weekday < 0 or interval.weekday > 6:
            raise ValidationError("weekday must be between 0 and 6.")
        for weekday, start, end in _segments(interval):
            occupied[weekday].append((start, end))

    for weekday, segments in occupied.items():
        segments.sort()
        previous_end = -1
        for start, end in segments:
            if start < previous_end:
                raise ValidationError(
                    {"hours": f"Overlapping business hours on weekday {weekday}."}
                )
            previous_end = max(previous_end, end)
