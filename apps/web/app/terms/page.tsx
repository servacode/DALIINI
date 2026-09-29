import type { Metadata } from "next";
import { PublishedArticle } from "../../components/content";
import { getContentPage } from "../../lib/api";
import { pageMetadata } from "../../lib/seo";

/*
 * /terms — the terms the team publishes from the console
 * (content/pages/terms/, the same text the app shows), with their publication
 * date. While they are unpublished or the API is unreachable, the static text
 * below stands in.
 */
export const revalidate = 300;

const TITLE = "شروط الاستخدام";
const DESCRIPTION = "شروط استخدام دليني: دقة المعلومات، مسؤوليات أصحاب المنشآت، والاستخدام المقبول.";

export async function generateMetadata(): Promise<Metadata> {
  const page = await getContentPage("terms");
  return pageMetadata({ title: page?.titleAr || TITLE, description: DESCRIPTION, path: "/terms" });
}

export default async function TermsPage() {
  const page = await getContentPage("terms");
  if (page) return <PublishedArticle page={page} />;
  return (
    <article className="shell legal">
      <h1>{TITLE}</h1>
      <p>تقدم المنصة معلومات دليل تساعد المستخدم في الوصول إلى المنشآت والخدمات. يجب على أصحاب المنشآت تقديم بيانات صحيحة والحفاظ على تحديثها، وتخضع المنشآت التي تتطلب تحققًا لسياسات المراجعة والتفعيل.</p>
      <h2>دقة المعلومات</h2>
      <p>حالة الفتح والمناوبة تعتمد على البيانات المسجلة والوقت الحالي، وقد تتأثر بتغييرات تشغيلية لم تُسجل بعد.</p>
      <h2>الاستخدام المقبول</h2>
      <p>يُمنع إساءة استخدام الحسابات أو محاولات الوصول غير المصرح به أو رفع محتوى ضار أو مضلل.</p>
    </article>
  );
}
