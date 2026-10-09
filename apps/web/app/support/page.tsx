import Link from "next/link";
import { EmailLink } from "../../components/email-link";
import { SupportWhatsApp } from "../../components/support-whatsapp";
import { Icon } from "../../components/ui";
import { getSupportContact } from "../../lib/api";
import { publicConfig } from "../../lib/config";
import { pageMetadata } from "../../lib/seo";

export const metadata = pageMetadata({
  title: "الدعم",
  description: "تواصل مع فريق دعم دليني بخصوص الحساب أو معلومات المنشآت أو استخدام التطبيق.",
  path: "/support",
});

export default async function SupportPage() {
  const contact = await getSupportContact();
  return (
    <article className="shell legal">
      <h1>الدعم</h1>
      <p>إذا واجهت مشكلة في الحساب أو معلومات منشأة أو استخدام التطبيق، يمكنك التواصل مع فريق الدعم.</p>
      {/* The quickest way, first: a chat with the person who answers. */}
      <SupportWhatsApp contact={contact} />
      <div className="card">
        <strong>نموذج التواصل</strong>
        <p>اكتب رسالتك من الموقع مباشرة، واترك رقمك إن أردت أن نتصل بك.</p>
        <Link className="button" href="/contact">
          <Icon name="inbox" />
          راسلنا
        </Link>
      </div>
      <div className="card more"><strong>البريد الإلكتروني</strong><p><EmailLink email={publicConfig.supportEmail} /></p></div>
      <p>لن يطلب فريق الدعم كلمة المرور أو رمز OTP الكامل.</p>
    </article>
  );
}
