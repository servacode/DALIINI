"""Content pages, built on the versioned `LegalDocument` rather than beside it.

A page is every `LegalDocument` row sharing one key. Its slug on the wire is the key in lower
case (`privacy`, `how-to-report`), so the six built-in pages the apps already read through
`/public/legal/<KEY>/` are content pages too, with no second copy of their words.

- Editing the words of a page that was ever published writes a new version, so the legal
  history is never overwritten. A page that never went live is edited in place.
- Publishing makes the newest version the one active row for the key; unpublishing leaves
  the key with no active row, so the public endpoint answers 404.
- The built-in keys can be unpublished but not deleted: the apps link to them.
"""

from __future__ import annotations

from typing import Any

from django.core.exceptions import ValidationError
from django.db import transaction
from django.db.models import QuerySet
from django.utils import timezone

from audit.services import record_audit
from core.exceptions import ConflictError

from .models import LegalDocument

BUILT_IN_KEYS = frozenset(LegalDocument.Key.values)


def key_for(slug: str) -> str:
    return (slug or "").strip().upper()


def slug_for(key: str) -> str:
    return key.lower()


def page_payload(rows: list[LegalDocument]) -> dict[str, Any]:
    """A page's admin view: newest version's words plus what is live. `rows` newest first."""
    latest = rows[0]
    active = next((row for row in rows if row.active), None)
    return {
        "slug": slug_for(latest.key),
        "kind": latest.kind,
        "titleAr": latest.title_ar,
        "bodyAr": latest.body_ar,
        "published": active is not None,
        "version": latest.version,
        "publishedVersion": active.version if active else None,
        "hasUnpublishedChanges": active is not None and active.pk != latest.pk,
        "builtIn": latest.key in BUILT_IN_KEYS,
        "publishedAt": active.published_at.isoformat() if active and active.published_at else None,
        "updatedAt": latest.updated_at.isoformat(),
    }


def public_page_payload(document: LegalDocument) -> dict[str, Any]:
    return {
        "slug": slug_for(document.key),
        "kind": document.kind,
        "titleAr": document.title_ar,
        "bodyAr": document.body_ar,
        "version": document.version,
        "publishedAt": document.published_at.isoformat() if document.published_at else None,
        "updatedAt": document.updated_at.isoformat(),
    }


def all_pages() -> list[dict[str, Any]]:
    rows = LegalDocument.objects.order_by("key", "-version")
    grouped: dict[str, list[LegalDocument]] = {}
    for row in rows:
        grouped.setdefault(row.key, []).append(row)
    return [page_payload(versions) for _, versions in sorted(grouped.items())]


def page_rows(key: str, *, lock: bool = False) -> list[LegalDocument]:
    queryset: QuerySet[LegalDocument] = LegalDocument.objects.filter(key=key)
    if lock:
        queryset = queryset.select_for_update()
    return list(queryset.order_by("-version"))


def _snapshot(rows: list[LegalDocument]) -> dict[str, Any]:
    payload = page_payload(rows)
    return {key: payload[key] for key in ("kind", "titleAr", "published", "version")}


def _publish(rows: list[LegalDocument], publish: bool) -> None:
    """Make the newest version the only active one, or deactivate every version."""
    latest = rows[0]
    for row in rows:
        if row.active and (not publish or row.pk != latest.pk):
            row.active = False
            row.save(update_fields=["active", "updated_at"])
    if publish and not latest.active:
        latest.active = True
        latest.published_at = timezone.now()
        latest.full_clean()
        latest.save(update_fields=["active", "published_at", "updated_at"])


@transaction.atomic
def create_page(*, actor: Any, data: dict[str, Any], request_id: str = "") -> list[LegalDocument]:
    key = key_for(data["slug"])
    if LegalDocument.objects.filter(key=key).exists():
        raise ConflictError("CONTENT_PAGE_EXISTS", message="توجد صفحة بهذا المعرّف مسبقاً.")
    document = LegalDocument(
        key=key,
        kind=data.get("kind", LegalDocument.Kind.PAGE),
        title_ar=data["titleAr"],
        body_ar=data["bodyAr"],
        version=1,
    )
    document.full_clean()
    document.save()
    rows = [document]
    if data.get("published"):
        _publish(rows, True)
    record_audit(
        actor=actor,
        action="content_page.created",
        target=document,
        after_snapshot=_snapshot(rows),
        metadata={"slug": slug_for(key)},
        request_id=request_id,
    )
    return rows


@transaction.atomic
def update_page(
    *, actor: Any, key: str, data: dict[str, Any], request_id: str = ""
) -> list[LegalDocument]:
    rows = page_rows(key, lock=True)
    if not rows:
        raise LegalDocument.DoesNotExist
    before = _snapshot(rows)
    latest = rows[0]
    title = data.get("titleAr", latest.title_ar)
    body = data.get("bodyAr", latest.body_ar)
    if (title, body) != (latest.title_ar, latest.body_ar):
        if latest.published_at is None:
            latest.title_ar, latest.body_ar = title, body
            latest.full_clean()
            latest.save(update_fields=["title_ar", "body_ar", "updated_at"])
        else:
            latest = LegalDocument(
                key=key, kind=latest.kind, title_ar=title, body_ar=body, version=latest.version + 1
            )
            latest.full_clean()
            latest.save()
            rows.insert(0, latest)
    if "kind" in data and data["kind"] != latest.kind:
        if data["kind"] not in LegalDocument.Kind.values:
            raise ValidationError({"kind": "Unknown kind."})
        LegalDocument.objects.filter(key=key).update(kind=data["kind"])
        for row in rows:
            row.kind = data["kind"]
    if "published" in data:
        _publish(rows, bool(data["published"]))
    record_audit(
        actor=actor,
        action="content_page.updated",
        target=latest,
        before_snapshot=before,
        after_snapshot=_snapshot(rows),
        metadata={"slug": slug_for(key)},
        request_id=request_id,
    )
    return rows


@transaction.atomic
def delete_page(*, actor: Any, key: str, request_id: str = "") -> None:
    rows = page_rows(key, lock=True)
    if not rows:
        raise LegalDocument.DoesNotExist
    if key in BUILT_IN_KEYS:
        raise ConflictError(
            "CONTENT_PAGE_BUILT_IN",
            message="هذه صفحة أساسية تعتمد عليها التطبيقات؛ يمكن إلغاء نشرها لا حذفها.",
        )
    record_audit(
        actor=actor,
        action="content_page.deleted",
        target=rows[0],
        before_snapshot=_snapshot(rows),
        metadata={"slug": slug_for(key), "versions": len(rows)},
        request_id=request_id,
    )
    LegalDocument.objects.filter(key=key).delete()
