# Realtime, Notifications and Offline Strategy

## Realtime principle

REST = truth.  
WebSocket = invalidation/events.

## Event catalog

```text
public.province.configuration_changed
public.facility.changed
public.facility.availability_changed
public.duty.changed
user.application.changed
user.facility.changed
admin.review_queue.changed
admin.system.changed
```

Event payload:
- version.
- scope.
- resource id.
- occurredAt.
- no sensitive full object.

## Subscription scopes

Anonymous:
- selected province.

Authenticated:
- selected province.
- current user.

Admin:
- role-permitted admin channel.

## Reconnect

- base 1s.
- exponential.
- max cap 30–60s.
- jitter.
- reset after stable connection.
- resubscribe after reconnect.

## Cache invalidation

Event:
```text
facility changed
→ invalidate local facility/detail/list keys
→ active screens refetch
```

## Push

Use push when app may not be connected.

Owner notifications:
- application approved.
- rejected.
- reverification required.
- suspension.
- important policy update.

## Offline

Public:
- render cached content.
- show offline banner.
- disable fresh-only operations gracefully.

Owner:
- drafts can retain safe local progress.
- submission requires network.
- evidence upload requires network.

Admin:
- online-first. Do not pretend mutations succeeded offline.

## Freshness

Short TTL:
- availability.
- duty.

Medium:
- facility detail/list.

Long:
- province/category reference.

Cached time-sensitive content must not silently show stale “مفتوح الآن” as guaranteed truth.

## Conflict handling

Owner edit:
- use updatedAt/version or ETag if implementing optimistic concurrency.
- conflict produces explicit refresh/review UX.
