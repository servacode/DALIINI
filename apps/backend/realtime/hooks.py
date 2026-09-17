from django.db.models.signals import post_delete, post_save
from django.dispatch import receiver

from business_hours.models import BusinessHour, TemporaryClosure
from directory.models import CategoryProvince
from facilities.models import Facility, FacilityApplication, FacilityMembership
from pharmacy_duty.models import DutyShift

from .events import EventName, RealtimeEvent, ScopeType
from .publisher import publish_after_commit


def _province_event(name, province_id, resource_id=None):
    if province_id:
        publish_after_commit(
            RealtimeEvent(
                name=name,
                scope_type=ScopeType.PROVINCE,
                scope_id=str(province_id),
                resource_id=str(resource_id) if resource_id else None,
            )
        )


def _facility_from_schedule(instance):
    return instance.facility


@receiver([post_save, post_delete], sender=Facility)
def facility_changed(sender, instance, **kwargs):
    _province_event(
        EventName.FACILITY_CHANGED,
        instance.province_id,
        instance.pk,
    )


@receiver([post_save, post_delete], sender=BusinessHour)
@receiver([post_save, post_delete], sender=TemporaryClosure)
def availability_changed(sender, instance, **kwargs):
    facility = _facility_from_schedule(instance)
    _province_event(
        EventName.FACILITY_AVAILABILITY_CHANGED,
        facility.province_id,
        facility.pk,
    )


@receiver([post_save, post_delete], sender=DutyShift)
def duty_changed(sender, instance, **kwargs):
    facility = instance.facility
    _province_event(
        EventName.DUTY_CHANGED,
        facility.province_id,
        facility.pk,
    )


@receiver([post_save, post_delete], sender=CategoryProvince)
def province_configuration_changed(sender, instance, **kwargs):
    _province_event(
        EventName.PROVINCE_CONFIGURATION_CHANGED,
        instance.province_id,
        instance.category_id,
    )


@receiver(post_save, sender=FacilityApplication)
def application_changed(sender, instance, **kwargs):
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
