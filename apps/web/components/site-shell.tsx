import Link from "next/link";
import type { ReactNode } from "react";

export function SiteShell({ children }: { children: ReactNode }) {
  return (
    <>
      <header className="shell header">
        <strong>الدليل</strong>
        <nav className="nav" aria-label="التنقل العام">
          <Link href="/privacy">الخصوصية</Link>
          <Link href="/terms">الشروط</Link>
          <Link href="/support">الدعم</Link>
          <Link href="/delete-account">حذف الحساب</Link>
        </nav>
      </header>
      <main>{children}</main>
      <footer className="shell footer">منصة دليل مبنية لتقديم معلومات محلية واضحة وقابلة للتحقق.</footer>
    </>
  );
}
