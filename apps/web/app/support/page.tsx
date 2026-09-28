import { EmailLink } from "../../components/email-link";
import { publicConfig } from "../../lib/config";
import { pageMetadata } from "../../lib/seo";

export const metadata = pageMetadata({
  title: "الدعم",
  description: "تواصل مع فريق دعم دليني بخصوص الحساب أو معلومات المنشآت أو استخدام التطبيق.",
  path: "/support",
});

export default function SupportPage() {
  return (
    <article className="shell legal">
      <h1>الدعم</h1>
      <p>إذا واجهت مشكلة في الحساب أو معلومات منشأة أو استخدام التطبيق، يمكنك التواصل مع فريق الدعم.</p>
      <div className="card"><strong>البريد الإلكتروني</strong><p><EmailLink email={publicConfig.supportEmail} /></p></div>
      <p>لن يطلب فريق الدعم كلمة المرور أو رمز OTP الكامل.</p>
    </article>
  );
}
