import Link from "next/link";

export const metadata = { title: "الصفحة غير موجودة", robots: { index: false } };

export default function NotFound() {
  return (
    <div className="shell page">
      <h1>الصفحة غير موجودة</h1>
      <p>ربما نُقلت هذه الصفحة أو لم تعد المنشأة منشورة في الدليل.</p>
      <div className="actions">
        <Link className="button" href="/">العودة إلى الرئيسية</Link>
        <Link className="button button-alt" href="/search">ابحث في الدليل</Link>
        <Link className="button button-alt" href="/duty">الصيدليات المناوبة</Link>
      </div>
    </div>
  );
}
