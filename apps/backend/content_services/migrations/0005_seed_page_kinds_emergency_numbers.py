"""Give the existing pages their kind, and seed the national emergency numbers.

`OPERATOR_VERIFICATION_REQUIRED`: 110 (ambulance), 113 (fire) and 112 (police) are the numbers
commonly cited for Syria. They are seeded because a directory of health services without them
would be worse, but they were not verified against an official source by the implementation.
Each row says so in its operator-only note, and an operator must confirm or correct them
before launch. Provincial numbers are not seeded: none is known with confidence.
"""

from django.db import migrations

KINDS = {"PRIVACY": "LEGAL", "TERMS": "LEGAL", "FAQ": "FAQ"}

NOTE = "رقم وطني شائع الاستخدام، أُضيف تلقائياً. يجب أن يتحقق منه المشغل قبل الإطلاق."
NUMBERS = [
    ("الإسعاف", "110", "AMBULANCE", 10),
    ("الإطفاء", "113", "FIRE", 20),
    ("الشرطة", "112", "POLICE", 30),
]


def seed(apps, schema_editor):
    LegalDocument = apps.get_model("content_services", "LegalDocument")
    for key, kind in KINDS.items():
        LegalDocument.objects.filter(key=key).update(kind=kind)
    EmergencyNumber = apps.get_model("content_services", "EmergencyNumber")
    for label, phone, kind, order in NUMBERS:
        EmergencyNumber.objects.get_or_create(
            province=None,
            phone=phone,
            defaults={
                "label_ar": label,
                "kind": kind,
                "sort_order": order,
                "active": True,
                "admin_note": NOTE,
            },
        )


class Migration(migrations.Migration):
    dependencies = [("content_services", "0004_content_pages_faq_emergency_contact")]
    operations = [migrations.RunPython(seed, migrations.RunPython.noop)]
