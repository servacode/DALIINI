"""INT-037: the ordering field is `sequence` on the wire and `sort_order` in the database.

`06-DATA-MODEL.md` names it `sequence`. The column is not renamed for that — a migration
that rewrites a table to satisfy a naming preference buys nothing — so the translation has
to happen in the serializer, and these tests prove it happens in both directions.
"""

from datetime import time

import pytest
from rest_framework.test import APIClient

from accounts.models import User
from business_hours.models import BusinessHour
from business_hours.serializers import BusinessHourInputSerializer, serialize_hours
from facilities.models import Facility, FacilityMembership


def test_input_accepts_sequence_and_writes_sort_order() -> None:
    serializer = BusinessHourInputSerializer(
        data={"weekday": 0, "opensAt": "08:00", "closesAt": "16:00", "sequence": 2}
    )

    assert serializer.is_valid(), serializer.errors
    assert serializer.validated_data["sort_order"] == 2
    assert "sequence" not in serializer.validated_data


def test_input_rejects_the_old_wire_name() -> None:
    serializer = BusinessHourInputSerializer(
        data={"weekday": 0, "opensAt": "08:00", "closesAt": "16:00", "sortOrder": 5}
    )

    assert serializer.is_valid(), serializer.errors
    # `sortOrder` is not a field any more, so it is ignored and the default applies.
    assert serializer.validated_data["sort_order"] == 0


@pytest.mark.django_db
def test_output_emits_sequence(facility: Facility) -> None:
    row = BusinessHour.objects.create(
        facility=facility,
        weekday=0,
        opens_at=time(8, 0),
        closes_at=time(16, 0),
        sort_order=3,
    )

    payload = serialize_hours([row])[0]

    assert payload["sequence"] == 3
    assert "sortOrder" not in payload


@pytest.mark.django_db
def test_replace_hours_round_trips_sequence(facility: Facility, user: User) -> None:
    FacilityMembership.objects.create(
        facility=facility,
        user=user,
        role=FacilityMembership.Role.OWNER,
    )
    client = APIClient()
    client.force_authenticate(user=user)

    response = client.put(
        f"/api/v1/owner/facilities/{facility.pk}/hours/",
        [
            {"weekday": 0, "opensAt": "08:00", "closesAt": "12:00", "sequence": 0},
            {"weekday": 0, "opensAt": "16:00", "closesAt": "20:00", "sequence": 1},
        ],
        format="json",
    )

    assert response.status_code == 200, response.content
    items = response.json()["items"]
    assert [item["sequence"] for item in items] == [0, 1]
    assert all("sortOrder" not in item for item in items)
    assert sorted(
        BusinessHour.objects.filter(facility=facility).values_list("sort_order", flat=True)
    ) == [0, 1]


@pytest.mark.django_db
def test_the_column_keeps_its_name(facility: Facility) -> None:
    assert [field.name for field in BusinessHour._meta.fields if field.name == "sort_order"]
    assert not [field.name for field in BusinessHour._meta.fields if field.name == "sequence"]
