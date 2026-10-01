from typing import Any
from uuid import UUID

from django.db.models import Model
from django.db.models.signals import post_delete, post_save
from django.dispatch import receiver

from business_hours.models import BusinessHour, TemporaryClosure
from directory.models import Category, CategoryGroup, CategoryProvince, VerificationRequirement
from facilities.models import Facility, FacilityApplication, FacilityMembership
from locations.models import Province
from pharmacy_duty.models import DutyShift

from .events import EventName, RealtimeEvent, ScopeType
from .publisher import publish_after_commit


def _province_event(
    name: EventName, province_id: UUID | None, resource_id: UUID | None = None
) -> None:
    if province_id:
        publish_after_commit(
            RealtimeEvent(
                name=name,
                scope_type=ScopeType.PROVINCE,
                scope_id=str(province_id),
                resource_id=str(resource_id) if resource_id else None,
            )
        )


def _facility_from_schedule(instance: BusinessHour | TemporaryClosure) -> Facility:
    return instance.facility


@receiver([post_save, post_delete], sender=Facility)
def facility_changed(sender: type[Facility], instance: Facility, **kwargs: Any) -> None:
    _province_event(
        EventName.FACILITY_CHANGED,
        instance.province_id,
        instance.pk,
    )


@receiver([post_save, post_delete], sender=BusinessHour)
@receiver([post_save, post_delete], sender=TemporaryClosure)
def availability_changed(
    sender: type[Model], instance: BusinessHour | TemporaryClosure, **kwargs: Any
) -> None:
    facility = _facility_from_schedule(instance)
    _province_event(
        EventName.FACILITY_AVAILABILITY_CHANGED,
        facility.province_id,
        facility.pk,
    )


@receiver([post_save, post_delete], sender=DutyShift)
def duty_changed(sender: type[DutyShift], instance: DutyShift, **kwargs: Any) -> None:
    facility = instance.facility
    _province_event(
        EventName.DUTY_CHANGED,
        facility.province_id,
        facility.pk,
    )


@receiver([post_save, post_delete], sender=CategoryProvince)
def province_configuration_changed(
    sender: type[CategoryProvince], instance: CategoryProvince, **kwargs: Any
) -> None:
    _province_event(
        EventName.PROVINCE_CONFIGURATION_CHANGED,
        instance.province_id,
        instance.category_id,
    )


@receiver(post_save, sender=FacilityApplication)
def application_changed(
    sender: type[FacilityApplication], instance: FacilityApplication, **kwargs: Any
) -> None:
    owner_ids = FacilityMembership.objects.filter(
        facility_id=instance.facility_id,
        role=FacilityMembership.Role.OWNER,
    ).values_list("user_id", flat=True)
    for user_id in owner_ids:
        publish_after_commit(
            RealtimeEvent(
                name=EventName.USER_APPLICATION_CHANGED,
                scope_type=ScopeType.USER,
                scope_id=str(user_id),
                resource_id=str(instance.pk),
            )
        )
    publish_after_commit(
        RealtimeEvent(
            name=EventName.ADMIN_REVIEW_QUEUE_CHANGED,
            scope_type=ScopeType.ADMIN,
            scope_id="review_queue",
            resource_id=str(instance.pk),
        )
    )


@receiver([post_save, post_delete], sender=Category)
@receiver([post_save, post_delete], sender=CategoryGroup)
@receiver([post_save, post_delete], sender=VerificationRequirement)
def admin_configuration_changed(sender: type[Model], instance: Model, **kwargs: Any) -> None:
    publish_after_commit(
        RealtimeEvent(
            name=EventName.ADMIN_SYSTEM_CHANGED,
            scope_type=ScopeType.ADMIN,
            scope_id="system",
            resource_id=str(instance.pk),
        )
    )


@receiver([post_save, post_delete], sender=Province)
def province_rollout_changed(sender: type[Province], instance: Province, **kwargs: Any) -> None:
    _province_event(
        EventName.PROVINCE_CONFIGURATION_CHANGED,
        instance.pk,
        instance.pk,
    )
    publish_after_commit(
        RealtimeEvent(
            name=EventName.ADMIN_SYSTEM_CHANGED,
            scope_type=ScopeType.ADMIN,
            scope_id="system",
            resource_id=str(instance.pk),
        )
    )
