import Link from "next/link";
import type { ReactNode } from "react";

export function SiteShell({ children }: { children: ReactNode }) {
  return (
    <>
      <header className="site-header">
        <div className="shell header">
          <Link href="/" className="brand" aria-label="دليني — الصفحة الرئيسية">
            <span className="brand-mark" aria-hidden="true">د</span>
            <span>دليني</span>
          </Link>
          <nav className="nav" aria-label="التنقل العام">
            <Link href="/duty">المناوبات</Link>
            <Link href="/support">الدعم</Link>
          </nav>
        </div>
      </header>
      <main>{children}</main>
      <footer className="shell footer">
        <nav className="nav" aria-label="روابط قانونية">
          <Link href="/privacy">سياسة الخصوصية</Link>
          <Link href="/terms">شروط الاستخدام</Link>
          <Link href="/support">الدعم</Link>
          <Link href="/delete-account">حذف الحساب</Link>
        </nav>
        <p>دليني — دليل محلي لتقديم معلومات واضحة وقابلة للتحقق عن المنشآت والخدمات.</p>
      </footer>
    </>
  );
}
