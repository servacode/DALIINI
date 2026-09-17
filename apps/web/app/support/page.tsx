import { publicConfig } from "../../lib/config";
export const metadata = { title: "الدعم" };
export default function SupportPage() { return <article className="shell legal"><h1>الدعم</h1><p>إذا واجهت مشكلة في الحساب أو معلومات منشأة أو استخدام التطبيق، يمكنك التواصل مع فريق الدعم.</p><div className="card"><strong>البريد الإلكتروني</strong><p>{publicConfig.supportEmail}</p></div><p>لن يطلب فريق الدعم كلمة المرور أو رمز OTP الكامل.</p></article>; }
