"""The reviewer's starting set of rejection reasons. Operators edit or retire them freely."""

from django.db import migrations

TEMPLATES = [
    (
        "الترخيص غير واضح",
        "صورة الترخيص غير واضحة أو غير مقروءة. يرجى رفع صورة واضحة وكاملة للترخيص تظهر فيها "
        "البيانات والختم، ثم إعادة إرسال الطلب.",
    ),
    (
        "الموقع لا يطابق العنوان",
        "الموقع المحدد على الخريطة لا يطابق العنوان المكتوب. يرجى تصحيح موقع المنشأة على "
        "الخريطة أو تعديل العنوان، ثم إعادة إرسال الطلب.",
    ),
    (
        "وثائق ناقصة",
        "بعض الوثائق المطلوبة للتحقق غير مرفقة. يرجى رفع جميع الإثباتات المطلوبة، ثم إعادة "
        "إرسال الطلب.",
    ),
    (
        "منشأة مكررة",
        "هذه المنشأة مسجلة مسبقاً في الدليل. إن كنت مالكها فتواصل معنا لنقل إدارتها إلى حسابك "
        "بدلاً من تسجيلها من جديد.",
    ),
    (
        "تصنيف غير صحيح",
        "التصنيف المختار لا يطابق نشاط المنشأة. يرجى اختيار التصنيف الصحيح، ثم إعادة إرسال "
        "الطلب.",
    ),
    (
        "ساعات العمل ناقصة",
        "ساعات العمل غير مكتملة أو غير صحيحة. يرجى إدخال ساعات العمل لكل يوم تفتح فيه المنشأة، "
        "ثم إعادة إرسال الطلب.",
    ),
]


def seed(apps, schema_editor):
    RejectionTemplate = apps.get_model("facilities", "RejectionTemplate")
    for index, (title, body) in enumerate(TEMPLATES):
        RejectionTemplate.objects.get_or_create(
            title_ar=title, defaults={"body_ar": body, "sort_order": (index + 1) * 10}
        )


class Migration(migrations.Migration):
    dependencies = [("facilities", "0006_hours_confirmation_rejection_templates")]
    operations = [migrations.RunPython(seed, migrations.RunPython.noop)]
