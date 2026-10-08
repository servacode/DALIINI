"""Which notices an account wants pushed (`NotificationPreference`, DECISION-067).

The categories, and which notification types fall in each, are the app's own
(`NotificationCategory.of` on Android): a type outside them is always announced, because an
unknown kind is not a reason to hide something the platform sent, and a staff change to an
owner's own shift is not news but their roster.
"""

from __future__ import annotations

from typing import Any

from .models import NotificationPreference

DUTY_REMINDERS = "dutyReminders"
PROVINCE_NEWS = "provinceNews"
APPLICATION_STATUS = "applicationStatus"
FIELDS = {
    DUTY_REMINDERS: "duty_reminders",
    PROVINCE_NEWS: "province_news",
    APPLICATION_STATUS: "application_status",
}
# Kept the same as `NotificationCategory.of` on Android, which is the list's home. A staff
# change to an owner's own shift is not news but their roster; an invitation is addressed to
# this person rather than news about a facility. Both fall under a prefix below, so without
# this set the backend would decline to push them while the app considers them unmutable —
# and the backend is the side that decides, so the reader would simply never hear.
ALWAYS_SHOWN = {"duty.shift.admin_changed", "facility.invitation.received"}


def category_of(notification_type: str) -> str | None:
    value = notification_type.strip().lower()
    if value in ALWAYS_SHOWN:
        return None
    if value in {"duty.gap_nudge", "duty_gap"} or value.startswith("duty."):
        return DUTY_REMINDERS
    if value.startswith("facility."):
        return APPLICATION_STATUS
    if value == "platform.broadcast" or value.startswith("province."):
        return PROVINCE_NEWS
    return None


def preferences_of(user: Any) -> dict[str, bool]:
    row = NotificationPreference.objects.filter(user=user).first()
    return {wire: bool(getattr(row, column)) if row else True for wire, column in FIELDS.items()}


def save_preferences(user: Any, changes: dict[str, bool]) -> dict[str, bool]:
    row, _ = NotificationPreference.objects.get_or_create(user=user)
    for wire, value in changes.items():
        setattr(row, FIELDS[wire], bool(value))
    row.save()
    return preferences_of(user)


def push_wanted(user: Any, notification_type: str) -> bool:
    category = category_of(notification_type)
    if category is None:
        return True
    return preferences_of(user)[category]
