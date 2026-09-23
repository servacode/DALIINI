"""The first version of the platform's own pages.

The words here describe only what this codebase actually does. What the app collects was read
off the code, not assumed: an account is a phone number and a name, the location is asked for
in the foreground and only to order and measure distance, a facility's own data is public by
the owner's act, verification evidence is private to the review, and a push token is stored
encrypted and tied to a session.

`LEGAL_REVIEW_REQUIRED`: this is a first draft written by the implementation, not legal
advice, and it must be reviewed before production. It is versioned in the database precisely
so that the reviewed text replaces it without a release.

Contact is deliberately not seeded: no support address or number is configured anywhere in
this repository, and inventing one would be worse than the app saying the page is not ready.
"""

from django.db import migrations
from django.utils import timezone

ABOUT = """دليل الخدمات الصحية في محافظتك.

يجمع التطبيق المنشآت الصحية المعتمدة في المحافظة ويعرضها في مكان واحد: أين هي، متى تفتح، وأيها مناوب الآن.

كل منشأة تظهر هنا أضافها صاحبها ومرت على مراجعة قبل النشر. البيانات تأتي من أصحاب المنشآت ومن مراجعة المنصة، وتتحدث من مكان واحد حتى تصل إلى كل من يستخدم التطبيق."""

PRIVACY = """ما الذي يجمعه التطبيق، ولماذا.

الحساب: رقم هاتفك واسمك. الرقم هو هويتك في المنصة ويُستخدم للدخول ولاستعادة كلمة المرور.

الموقع: يُطلب منك أثناء استخدام التطبيق فقط، ولا يُطلب في الخلفية أبداً. يُستخدم لترتيب المنشآت بالأقرب إليك ولحساب المسافة، والحساب يتم على الخادم. رفضك للموقع يبقي التطبيق كاملاً ضمن محافظتك، ويكلفك المسافات وترتيب الأقرب فقط.

منشآتك إن كنت مالكاً: اسم المنشأة ووصفها وهاتفها وعنوانها وموقعها وساعات عملها وصورها العامة. هذه بيانات تنشرها أنت للجمهور.

إثباتات التحقق: الملفات التي ترفعها لإثبات منشأتك خاصة بالمراجعة، ولا تظهر في صفحة المنشأة ولا لأي مستخدم آخر.

إشعارات الجهاز: إن سمحت بها، يُخزَّن معرّف الإشعارات مشفراً ومرتبطاً بجلستك، ويتوقف عند انتهاء الجلسة.

تقييماتك ومفضلتك: مرتبطة بحسابك. التقييم يظهر للجمهور كمعدّل فقط، لا باسمك.

ما لا نجمعه: لا نجمع جهات اتصالك، ولا محتوى رسائلك، ولا موقعك في الخلفية.

حذف الحساب: يمكنك طلب حذف حسابك من داخل التطبيق. تُلغى جلساتك وتُزال بياناتك الشخصية، مع الاحتفاظ بسجلات التدقيق والأمان وفق سياسة الاحتفاظ الموثقة.

هذه النسخة مسودة أولى تخضع لمراجعة قانونية قبل الإطلاق."""

TERMS = """شروط استخدام التطبيق.

التطبيق دليل معلوماتي. المعلومات المعروضة عن المنشآت — ساعات العمل، المناوبة، الإغلاق المؤقت — مصدرها أصحاب المنشآت، وقد تتغير قبل أن تصل إليك. تحقق قبل التوجه عند الضرورة.

التطبيق لا يقدّم استشارة طبية ولا يبيع دواءً ولا يحجز موعداً. لا تعتمد عليه وحده في حالة طارئة.

إن كنت مالك منشأة: أنت مسؤول عن صحة ما تنشره. المنصة تراجع طلبات التسجيل وقد ترفضها أو توقف منشأة إذا خالفت ما هو معلن هنا.

التقييمات تخص أصحابها. المنصة قد تزيل تقييماً مخالفاً.

هذه النسخة مسودة أولى تخضع لمراجعة قانونية قبل الإطلاق."""

INSTRUCTIONS = """كيف تستخدم التطبيق.

الرئيسية: تعرض منشآت محافظتك. إن سمحت بالموقع، تُرتَّب بالأقرب إليك وتظهر المسافة.

البحث: اكتب اسم منشأة أو اختصاصاً للبحث داخل محافظتك.

الفلاتر: أظهر المفتوح الآن أو المناوب الآن فقط.

الخريطة: تعرض المنشآت في أماكنها. اضغط علامة لترى المنشأة.

صفحة المنشأة: العنوان وساعات العمل والهاتف، ومنها تتصل أو تفتح الطريق إليها أو تحفظها في المفضلة.

المفضلة: المنشآت التي حفظتها، محفوظة في حسابك وتظهر على أي جهاز تدخل منه.

الإشعارات: ما أرسلته إليك المنصة، محفوظ في حسابك حتى لو لم يصلك إشعار على الجهاز.

للمالك: من حسابك، أضف منشأتك، أكمل بياناتها وموقعها وساعاتها، ارفع الإثباتات المطلوبة، ثم أرسلها للمراجعة وتابع حالتها."""

FAQ = """أسئلة شائعة.

لماذا لا أرى المسافة إلى المنشأة؟
لأن التطبيق لا يعرف موقعك. المسافة تُحسب على الخادم من موقعك، وبدون إذن الموقع لا تظهر.

هل يجب أن أسمح بالموقع؟
لا. التطبيق يعمل كاملاً ضمن محافظتك بدون الموقع، وما تخسره هو المسافة وترتيب الأقرب.

المنشأة مكتوب أنها مفتوحة وهي مغلقة، لماذا؟
الحالة محسوبة من ساعات العمل التي أدخلها صاحب المنشأة، ومن الإغلاقات المؤقتة التي يسجلها. إن لم يحدّثها فقد تتأخر.

ما معنى مناوب الآن؟
صيدلية سجّل صاحبها مناوبة في هذا الوقت.

كيف أضيف منشأتي؟
من حسابك، اختر منشآتي ثم إضافة منشأة، وأكمل الخطوات حتى إرسال الطلب للمراجعة.

كم تستغرق المراجعة؟
تصلك النتيجة داخل التطبيق فور صدور القرار، ومعها سبب الرفض إن رُفض الطلب.

هل يمكنني حذف حسابي؟
نعم، من صفحة الحساب. إن كنت المالك الوحيد لمنشأة غير مغلقة، انقل ملكيتها أو أغلقها أولاً."""


def seed(apps, schema_editor):
    LegalDocument = apps.get_model("content_services", "LegalDocument")
    now = timezone.now()
    pages = [
        ("ABOUT", "من نحن", ABOUT),
        ("PRIVACY", "سياسة الخصوصية", PRIVACY),
        ("TERMS", "الشروط والأحكام", TERMS),
        ("INSTRUCTIONS", "تعليمات الاستخدام", INSTRUCTIONS),
        ("FAQ", "الأسئلة الشائعة", FAQ),
    ]
    for key, title, body in pages:
        # Non-destructive: an operator's own published version is never overwritten by a re-run.
        if LegalDocument.objects.filter(key=key).exists():
            continue
        LegalDocument.objects.create(
            key=key,
            title_ar=title,
            body_ar=body,
            version=1,
            active=True,
            published_at=now,
        )


def unseed(apps, schema_editor):
    LegalDocument = apps.get_model("content_services", "LegalDocument")
    LegalDocument.objects.filter(version=1).delete()


class Migration(migrations.Migration):
    dependencies = [("content_services", "0002_legaldocument")]

    operations = [migrations.RunPython(seed, unseed)]
