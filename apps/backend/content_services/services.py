from django.db import transaction

from audit.services import record_audit

from .models import Advertisement


@transaction.atomic
def save_advertisement(*, actor, advertisement: Advertisement) -> Advertisement:
    advertisement.full_clean()
    advertisement.save()
    record_audit(
        actor=actor,
        action="advertisement.saved",
        target=advertisement,
        metadata={
            "enabled": advertisement.enabled,
            "targetScope": advertisement.target_scope,
            "actionType": advertisement.action_type,
        },
    )
    return advertisement


@transaction.atomic
def delete_advertisement(*, actor, advertisement: Advertisement) -> None:
    record_audit(
        actor=actor,
        action="advertisement.deleted",
        target=advertisement,
        metadata={"enabled": advertisement.enabled},
    )
    advertisement.delete()
