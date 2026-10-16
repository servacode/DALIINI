"""«من نحن» describes a directory of places, not a health service (DECISION-125).

The first text opened with «دليل الخدمات الصحية في محافظتك» and spoke of «المنشآت الصحية». Dalini
lists a city's places, of which pharmacies and clinics are some, and offers no health service of
its own. The page gets a new version saying so. A page an operator has already rewritten (its
active text no longer opens with the old line) is left as it is.
"""

from django.db import migrations
from django.utils import timezone

OLD_OPENING = "دليل الخدمات الصحية في محافظتك."

ABOUT = """دليل الأماكن في محافظتك.

يجمع التطبيق أماكن المدينة في مكان واحد: الصيدليات والعيادات والمخابر، والمخابز والأسواق والمطاعم، ومحطات الوقود والصرافة والورشات. لكل مكان عنوانه وهاتفه وساعات عمله والطريق إليه، ومعها أيّ الصيدليات مناوبة الليلة.

كل مكان يظهر هنا أضافه صاحبه ومرّ على مراجعة قبل النشر. البيانات تأتي من أصحاب الأماكن ومن مراجعة المنصة، وتتحدث من مكان واحد حتى تصل إلى كل من يستخدم التطبيق.

دليني دليل معلومات فقط: لا يقدّم خدمة أو استشارة طبية، ولا يبيع شيئاً."""


def rewrite(apps, schema_editor):
    LegalDocument = apps.get_model("content_services", "LegalDocument")
    active = LegalDocument.objects.filter(key="ABOUT", active=True).first()
    if active is None or not active.body_ar.startswith(OLD_OPENING):
        return
    newest = LegalDocument.objects.filter(key="ABOUT").order_by("-version").first()
    LegalDocument.objects.filter(pk=active.pk).update(active=False)
    LegalDocument.objects.create(
        key="ABOUT",
        kind=active.kind,
        title_ar=active.title_ar,
        body_ar=ABOUT,
        version=newest.version + 1,
        active=True,
        published_at=timezone.now(),
    )


def restore(apps, schema_editor):
    LegalDocument = apps.get_model("content_services", "LegalDocument")
    mine = LegalDocument.objects.filter(key="ABOUT", active=True, body_ar=ABOUT).first()
    if mine is None:
        return
    previous = (
        LegalDocument.objects.filter(key="ABOUT", version__lt=mine.version)
        .order_by("-version")
        .first()
    )
    if previous is None:
        return
    mine.delete()
    LegalDocument.objects.filter(pk=previous.pk).update(active=True)


class Migration(migrations.Migration):
    dependencies = [("content_services", "0008_legal_pages_approved")]

    operations = [migrations.RunPython(rewrite, restore)]
