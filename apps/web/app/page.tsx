import Link from "next/link";

export default function HomePage() {
  return (
    <div className="shell hero">
      <span className="eyebrow">منصة الدليل</span>
      <h1>اعثر على المعلومات المحلية التي تحتاجها بصورة أبسط.</h1>
      <p>يبدأ الإطلاق من محافظة الرقة، مع بنية قابلة لتفعيل محافظات وتصنيفات إضافية من الإدارة.</p>
      <Link className="button" href="/support">الدعم والمساعدة</Link>
      <section className="grid" aria-label="روابط مهمة">
        <article className="card"><h2>الخصوصية</h2><p>تعرف على أنواع البيانات التي قد تعالجها المنصة والغرض منها.</p><Link href="/privacy">عرض سياسة الخصوصية</Link></article>
        <article className="card"><h2>حذف الحساب</h2><p>تعرف على مسار طلب حذف الحساب والبيانات المرتبطة به.</p><Link href="/delete-account">عرض تعليمات الحذف</Link></article>
      </section>
    </div>
  );
}
