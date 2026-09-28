import { pageMetadata } from "../../lib/seo";

/*
 * Static legal copy: the public API exposes no legal-content endpoint yet.
 * When one lands, fetch it here and keep this text as the fallback.
 */
export const metadata = pageMetadata({
  title: "شروط الاستخدام",
  description: "شروط استخدام دليني: دقة المعلومات، مسؤوليات أصحاب المنشآت، والاستخدام المقبول.",
  path: "/terms",
});

export default function TermsPage() {
  return (
    <article className="shell legal">
      <h1>شروط الاستخدام</h1>
      <p>تقدم المنصة معلومات دليل تساعد المستخدم في الوصول إلى المنشآت والخدمات. يجب على أصحاب المنشآت تقديم بيانات صحيحة والحفاظ على تحديثها، وتخضع المنشآت التي تتطلب تحققًا لسياسات المراجعة والتفعيل.</p>
      <h2>دقة المعلومات</h2>
      <p>حالة الفتح والمناوبة تعتمد على البيانات المسجلة والوقت الحالي، وقد تتأثر بتغييرات تشغيلية لم تُسجل بعد.</p>
      <h2>الاستخدام المقبول</h2>
      <p>يُمنع إساءة استخدام الحسابات أو محاولات الوصول غير المصرح به أو رفع محتوى ضار أو مضلل.</p>
    </article>
  );
}
