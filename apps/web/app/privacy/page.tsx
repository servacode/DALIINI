import type { Metadata } from "next";
import { PublishedArticle } from "../../components/content";
import { EmailLink } from "../../components/email-link";
import { getContentPage } from "../../lib/api";
import { publicConfig } from "../../lib/config";
import { pageMetadata } from "../../lib/seo";

/*
 * /privacy — the privacy policy the team publishes from the console
 * (content/pages/privacy/, the same text the app shows), with its publication
 * date. While it is unpublished or the API is unreachable, the static text
 * below stands in. The privacy contact is site configuration, so it follows
 * either text.
 */
export const revalidate = 300;

const TITLE = "سياسة الخصوصية";
const DESCRIPTION = "كيف تعالج دليني البيانات الشخصية وبيانات الموقع وأدلة التحقق، وكيفية التواصل بشأن الخصوصية.";

export async function generateMetadata(): Promise<Metadata> {
  const page = await getContentPage("privacy");
  return pageMetadata({ title: page?.titleAr || TITLE, description: DESCRIPTION, path: "/privacy" });
}

function PrivacyContact() {
  return (
    <>
      <h2>التواصل</h2>
      <p>لأسئلة الخصوصية: <EmailLink email={publicConfig.privacyEmail} /></p>
    </>
  );
}

export default async function PrivacyPage() {
  const page = await getContentPage("privacy");
  if (page) {
    return (
      <PublishedArticle page={page}>
        <PrivacyContact />
      </PublishedArticle>
    );
  }
  return (
    <article className="shell legal">
      <h1>{TITLE}</h1>
      <p>تعالج المنصة الحد الأدنى اللازم لتقديم الحسابات، البحث، إدارة المنشآت، التقييمات والأمان التشغيلي. قد تشمل البيانات الاسم ورقم الهاتف وبيانات الجلسة وعلاقة المستخدم بالمنشأة وبيانات الموقع المستخدمة للاستعلام الحالي.</p>
      <h2>الموقع</h2>
      <p>يستخدم الموقع الدقيق للاستعلام الحالي عند منح الإذن، ولا يجب إدراج الإحداثيات الدقيقة الخام في التحليلات العامة.</p>
      <h2>الأدلة والملفات</h2>
      <p>أدلة التحقق الخاصة بأصحاب المنشآت تعامل كمحتوى خاص ولا تُعرض عبر الواجهات العامة.</p>
      <PrivacyContact />
    </article>
  );
}
