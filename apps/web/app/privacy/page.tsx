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
      <p>تجمع المنصة الحد الأدنى لتقديم الخدمة: رقم هاتفك واسمك لحسابك، وما تنشره بنفسك عن منشأتك إن كنت مالكاً.</p>
      <h2>الموقع</h2>
      <p>يُستخدم موقعك أثناء استخدام التطبيق لترتيب المنشآت بالأقرب وحساب المسافة، وأثناء رحلة بدأتها أنت لتوجيهك إلى منشأة. لا يُحفظ في حسابك، ولا يدخل في إحصاءات الاستخدام، ولا يُقرأ خارج ذلك.</p>
      <h2>الإثباتات والملفات</h2>
      <p>الملفات التي يرفعها أصحاب المنشآت لإثبات منشآتهم خاصة بالمراجعة، ولا تظهر لأي مستخدم آخر.</p>
      <h2>حذف الحساب</h2>
      <p>يمكنك حذف حسابك من داخل التطبيق أو من صفحة حذف الحساب في هذا الموقع.</p>
      <PrivacyContact />
    </article>
  );
}
