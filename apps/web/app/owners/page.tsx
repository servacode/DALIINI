import type { IconName } from "@servacode/design-tokens/icons";
import Link from "next/link";
import { DownloadCta, Icon, Section } from "../../components/ui";
import { pageMetadata } from "../../lib/seo";

/*
 * /owners — why and how to list a facility. Static: every step happens in the
 * app, so this page only explains and points there. The documents list stays
 * general on purpose: what each category requires is set by the team per
 * category and shown in the app before submission.
 */
export const metadata = pageMetadata({
  title: "لأصحاب المنشآت",
  description: "أضف صيدليتك أو عيادتك أو منشأتك إلى دليني ليجدك الناس بسهولة، بمعلومات موثوقة تُراجَع قبل النشر.",
  path: "/owners",
});

const REASONS: { icon: IconName; title: string; body: string }[] = [
  { icon: "verified", title: "ثقة الزوار", body: "تُنشر منشأتك بعد مراجعة مستنداتها، ويظهر تاريخ آخر تحقق على صفحتها." },
  { icon: "search", title: "يجدك الناس", body: "تظهر في البحث وفي قائمة تصنيفك بمحافظتك، مع أوقات الدوام والهاتف والموقع." },
  { icon: "pharmacy", title: "مناوباتك ظاهرة", body: "سجّل مناوبات صيدليتك من التطبيق، فتظهر في صفحة المناوبات في وقتها." },
  { icon: "chart", title: "أرقام تساعدك", body: "اعرف كم مرة شوهدت صفحتك وكم مرة اتصل بك الناس خلال آخر ٣٠ يوماً." },
];

const STEPS: { title: string; body: string }[] = [
  { title: "حمّل التطبيق", body: "ثبّت تطبيق دليني على هاتفك." },
  { title: "أنشئ حسابك", body: "سجّل الدخول من صفحة الحساب في التطبيق." },
  { title: "أضف منشأتك", body: "من حسابك اختر «منشآتي» ثم «إضافة منشأة»، وأكمل الاسم والعنوان والموقع وأوقات الدوام." },
  { title: "ارفع المستندات", body: "أرفق الإثباتات التي يطلبها التطبيق لتصنيف منشأتك." },
  { title: "انتظر المراجعة", body: "يراجع فريقنا الطلب، وتصلك النتيجة في التطبيق، ومعها السبب إن احتاج إلى تعديل." },
];

const QUESTIONS: { q: string; a: string }[] = [
  { q: "هل يرى الزوار مستنداتي؟", a: "لا. المستندات خاصة بالمراجعة فقط، ولا تظهر في صفحة المنشأة." },
  { q: "كم تستغرق المراجعة؟", a: "تصلك النتيجة في التطبيق فور صدور القرار، ومعها سبب الرفض إن رُفض الطلب." },
  {
    q: "ماذا يحدث إذا عدّلت بيانات منشأتي؟",
    a: "تعديل البيانات الأساسية، مثل الاسم أو الهاتف أو العنوان أو الموقع، يعيد المنشأة إلى المراجعة: أرسلها من جديد من التطبيق، وتغيب عن الدليل حتى نوافق على التعديل. أوقات الدوام والمناوبات تتغير مباشرة.",
  },
  { q: "كيف أسجّل مناوبات صيدليتي؟", a: "من صفحة منشأتك في التطبيق، أضف موعد كل مناوبة. تظهر الصيدلية «مناوبة الآن» خلال وقتها فقط." },
  { q: "وجدت منشأتي مضافة بمعلومات خاطئة، ماذا أفعل؟", a: "افتح صفحتها في التطبيق واضغط «الإبلاغ عن مشكلة»، أو راسل الدعم." },
];

export default function OwnersPage() {
  return (
    <div className="shell page">
      <section className="hero">
        <span className="eyebrow">لأصحاب المنشآت</span>
        <h1>أضف منشأتك إلى دليني ليجدك الناس بسهولة.</h1>
        <p className="page-intro">صيدلية أو عيادة أو مخبر أو أي خدمة محلية: أضفها من التطبيق، ونراجعها قبل النشر حتى تبقى المعلومات موثوقة.</p>
      </section>

      <Section id="why" title="لماذا تضيف منشأتك؟">
        <ul className="features">
          {REASONS.map((r) => (
            <li key={r.title} className="card">
              <span className="feature-icon"><Icon name={r.icon} size={22} /></span>
              <h3>{r.title}</h3>
              <p>{r.body}</p>
            </li>
          ))}
        </ul>
      </Section>

      <Section id="steps" title="الخطوات">
        <ol className="steps card">
          {STEPS.map((s) => (
            <li key={s.title}>
              <div>
                <strong>{s.title}</strong>
                <p>{s.body}</p>
              </div>
            </li>
          ))}
        </ol>
      </Section>

      <Section id="documents" title="المستندات المطلوبة">
        <p>تختلف المستندات حسب نوع المنشأة، ويعرض لك التطبيق المطلوب بالضبط قبل الإرسال. غالباً ما يُطلب:</p>
        <ul className="bullets">
          <li>ترخيص مزاولة المهنة أو ترخيص المنشأة.</li>
          <li>إثبات موقع المنشأة وعنوانها.</li>
          <li>ما يثبت أنك صاحب المنشأة أو مسؤول عنها.</li>
        </ul>
        <div className="card note more">
          <p>المستندات خاصة بالمراجعة ولا تظهر للزوار. <Link href="/how-we-verify">كيف نتحقق من المعلومات</Link></p>
        </div>
      </Section>

      <Section id="questions" title="أسئلة سريعة">
        <div className="faq">
          {QUESTIONS.map((item) => (
            <details key={item.q} className="card">
              <summary>{item.q}</summary>
              <p>{item.a}</p>
            </details>
          ))}
        </div>
        <p className="more">
          عندك سؤال آخر؟ <Link href="/faq">الأسئلة الشائعة</Link> أو <Link href="/support">راسل الدعم</Link>.
        </p>
      </Section>

      <DownloadCta title="ابدأ من التطبيق" body="حمّل تطبيق دليني، ثم أضف منشأتك من صفحة الحساب." />
    </div>
  );
}
