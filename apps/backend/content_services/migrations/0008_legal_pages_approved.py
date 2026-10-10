"""The owner approved the privacy and terms pages (readiness run, DECISION-118).

The second draft (0007) ended both with «هذه النسخة مسودة تخضع لمراجعة قانونية قبل الإطلاق.».
Approved, that sentence goes: each page gets a new version without it, the rest word for word.
A page whose active version no longer carries the sentence is left as it is.
"""

from django.db import migrations
from django.utils import timezone

DRAFT_NOTE = "هذه النسخة مسودة تخضع لمراجعة قانونية قبل الإطلاق."
KEYS = ("PRIVACY", "TERMS")


def approve(apps, schema_editor):
    LegalDocument = apps.get_model("content_services", "LegalDocument")
    now = timezone.now()
    for key in KEYS:
        active = LegalDocument.objects.filter(key=key, active=True).first()
        if active is None or DRAFT_NOTE not in active.body_ar:
            continue
        newest = LegalDocument.objects.filter(key=key).order_by("-version").first()
        LegalDocument.objects.filter(pk=active.pk).update(active=False)
        LegalDocument.objects.create(
            key=key,
            kind=active.kind,
            title_ar=active.title_ar,
            body_ar=active.body_ar.replace(DRAFT_NOTE, "").rstrip(),
            version=newest.version + 1,
            active=True,
            published_at=now,
        )


def unapprove(apps, schema_editor):
    LegalDocument = apps.get_model("content_services", "LegalDocument")
    for key in KEYS:
        mine = LegalDocument.objects.filter(key=key, active=True).first()
        if mine is None:
            continue
        previous = (
            LegalDocument.objects.filter(key=key, version__lt=mine.version)
            .order_by("-version")
            .first()
        )
        if previous is None or DRAFT_NOTE not in previous.body_ar:
            continue
        mine.delete()
        LegalDocument.objects.filter(pk=previous.pk).update(active=True)


class Migration(migrations.Migration):
    dependencies = [("content_services", "0007_legal_drafts_v2")]

    operations = [migrations.RunPython(approve, unapprove)]
