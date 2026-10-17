"""The test-data delete of DECISION-127: one facility, by its exact name, given twice."""

from io import StringIO

import pytest
from django.core.management import call_command
from django.core.management.base import CommandError

from audit.models import AuditEvent
from facilities.models import Facility


@pytest.mark.django_db
def test_it_deletes_one_facility_and_records_it(facility: Facility) -> None:
    name = facility.name_ar
    call_command("delete_facility", name, "--confirm", name, stdout=StringIO())

    assert not Facility.objects.filter(pk=facility.pk).exists()
    assert AuditEvent.objects.filter(action="facility.deleted_from_shell").exists()


@pytest.mark.django_db
def test_a_mismatched_confirmation_deletes_nothing(facility: Facility) -> None:
    with pytest.raises(CommandError, match="nothing was deleted"):
        call_command("delete_facility", facility.name_ar, "--confirm", "x", stdout=StringIO())
    assert Facility.objects.filter(pk=facility.pk).exists()


@pytest.mark.django_db
def test_an_unknown_name_deletes_nothing(facility: Facility) -> None:
    with pytest.raises(CommandError, match="0 facilities"):
        call_command("delete_facility", "لا يوجد", "--confirm", "لا يوجد", stdout=StringIO())
    assert Facility.objects.filter(pk=facility.pk).exists()
