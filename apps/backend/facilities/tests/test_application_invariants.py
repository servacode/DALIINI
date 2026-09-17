"""Connected proof for the submitted-application invariant.

06-DATA-MODEL: only one active submitted application of the applicable kind per
facility. The guarantee has to hold in three places at once, so all three are
asserted here rather than trusting a text match in a migration file:

1. the constraint is declared in model state,
2. the autodetector does not want to drop it,
3. PostgreSQL actually refuses the violating row.
"""

import pytest
from django.db import IntegrityError, transaction
from django.db.migrations.autodetector import MigrationAutodetector
from django.db.migrations.loader import MigrationLoader
from django.db.migrations.questioner import NonInteractiveMigrationQuestioner
from django.db.migrations.state import ProjectState

from facilities.models import FacilityApplication

CONSTRAINT = "uniq_submitted_application_per_facility_kind"


def _submitted(facility, kind=FacilityApplication.Kind.INITIAL):
    return FacilityApplication.objects.create(
        facility=facility,
        kind=kind,
        status=FacilityApplication.Status.SUBMITTED,
    )


def test_constraint_is_declared_in_model_state():
    names = [c.name for c in FacilityApplication._meta.constraints]
    assert CONSTRAINT in names, (
        "the invariant must live in model state; a migration keeps its historic "
        "AddConstraint forever and therefore proves nothing"
    )
    constraint = next(c for c in FacilityApplication._meta.constraints if c.name == CONSTRAINT)
    assert tuple(constraint.fields) == ("facility", "kind")
    assert constraint.condition is not None, "the constraint must stay partial"


@pytest.mark.django_db
def test_autodetector_does_not_propose_dropping_the_constraint():
    loader = MigrationLoader(None, ignore_no_migrations=True)
    autodetector = MigrationAutodetector(
        loader.project_state(),
        ProjectState.from_apps(__import__("django.apps", fromlist=["apps"]).apps),
        NonInteractiveMigrationQuestioner(specified_apps=set(), dry_run=True),
    )
    changes = autodetector.changes(graph=loader.graph, trim_to_apps={"facilities"})
    proposed = [
        operation
        for migration in changes.get("facilities", [])
        for operation in migration.operations
    ]
    dropped = [
        operation
        for operation in proposed
        if operation.__class__.__name__ == "RemoveConstraint"
        and getattr(operation, "name", None) == CONSTRAINT
    ]
    assert not dropped, f"autodetector wants to remove {CONSTRAINT}: model state drifted"


@pytest.mark.django_db
def test_database_rejects_a_second_submitted_application_of_the_same_kind(facility):
    _submitted(facility)
    with pytest.raises(IntegrityError):
        with transaction.atomic():
            _submitted(facility)


@pytest.mark.django_db
def test_database_allows_a_draft_alongside_a_submitted_application(facility):
    _submitted(facility)
    FacilityApplication.objects.create(
        facility=facility,
        kind=FacilityApplication.Kind.INITIAL,
        status=FacilityApplication.Status.DRAFT,
    )
    assert FacilityApplication.objects.filter(facility=facility).count() == 2


@pytest.mark.django_db
def test_database_allows_a_submitted_application_of_a_different_kind(facility):
    _submitted(facility)
    _submitted(facility, kind=FacilityApplication.Kind.REVERIFICATION)
    assert (
        FacilityApplication.objects.filter(
            facility=facility,
            status=FacilityApplication.Status.SUBMITTED,
        ).count()
        == 2
    )
