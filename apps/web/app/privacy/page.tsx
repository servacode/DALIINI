import { EmailLink } from "../../components/email-link";
import { publicConfig } from "../../lib/config";
import { pageMetadata } from "../../lib/seo";

/*
 * Static legal copy: the public API exposes no legal-content endpoint yet.
 * When one lands, fetch it here and keep this text as the fallback.
 */
export const metadata = pageMetadata({
  title: "سياسة الخصوصية",
  description: "كيف تعالج دليني البيانات الشخصية وبيانات الموقع وأدلة التحقق، وكيفية التواصل بشأن الخصوصية.",
  path: "/privacy",
});

export default function PrivacyPage() {
  return (
    <article className="shell legal">
      <h1>سياسة الخصوصية</h1>
      <p>تعالج المنصة الحد الأدنى اللازم لتقديم الحسابات، البحث، إدارة المنشآت، التقييمات والأمان التشغيلي. قد تشمل البيانات الاسم ورقم الهاتف وبيانات الجلسة وعلاقة المستخدم بالمنشأة وبيانات الموقع المستخدمة للاستعلام الحالي.</p>
      <h2>الموقع</h2>
      <p>يستخدم الموقع الدقيق للاستعلام الحالي عند منح الإذن، ولا يجب إدراج الإحداثيات الدقيقة الخام في التحليلات العامة.</p>
      <h2>الأدلة والملفات</h2>
      <p>أدلة التحقق الخاصة بأصحاب المنشآت تعامل كمحتوى خاص ولا تُعرض عبر الواجهات العامة.</p>
      <h2>التواصل</h2>
      <p>لأسئلة الخصوصية: <EmailLink email={publicConfig.privacyEmail} /></p>
    </article>
  );
}
