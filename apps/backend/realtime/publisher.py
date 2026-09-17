from __future__ import annotations

from asgiref.sync import async_to_sync
from channels.layers import get_channel_layer
from django.db import transaction

from .events import RealtimeEvent, ScopeType
from .groups import admin_group, province_group, user_group


def _group_for(event: RealtimeEvent) -> str:
    if event.scope_type is ScopeType.PROVINCE:
        return province_group(event.scope_id)
    if event.scope_type is ScopeType.USER:
        return user_group(event.scope_id)
    if event.scope_type is ScopeType.ADMIN:
        return admin_group(event.scope_id)
    raise ValueError(f"Unsupported realtime scope: {event.scope_type}")


def publish_after_commit(event: RealtimeEvent) -> None:
    payload = event.payload()
    group = _group_for(event)

    def publish():
        channel_layer = get_channel_layer()
        if channel_layer is None:
            return
        async_to_sync(channel_layer.group_send)(
            group,
            {
                "type": "directory.event",
                "payload": payload,
            },
        )

    transaction.on_commit(publish)
