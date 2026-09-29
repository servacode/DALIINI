import brandSymbol from "@servacode/design-tokens/brand/symbol-128.webp";
import Link from "next/link";
import type { ReactNode } from "react";
import { SearchBox } from "./search-form";

export function SiteShell({ children }: { children: ReactNode }) {
  return (
    <>
      <header className="site-header">
        <div className="shell header">
          <Link href="/" className="brand" aria-label="دليني — الصفحة الرئيسية">
            {/* eslint-disable-next-line @next/next/no-img-element -- small static brand asset */}
            <img className="brand-mark" src={brandSymbol.src} width={36} height={36} alt="" aria-hidden="true" />
            <span>دليني</span>
          </Link>
          <SearchBox id="header-q" variant="header" />
          <nav className="nav header-nav" aria-label="التنقل العام">
            <Link href="/duty">المناوبات</Link>
            <Link href="/search">ابحث</Link>
            <Link href="/emergency">الطوارئ</Link>
            <Link href="/owners">لأصحاب المنشآت</Link>
            <Link href="/support">الدعم</Link>
          </nav>
        </div>
      </header>
      <main>{children}</main>
      <footer className="shell footer">
        <nav className="nav" aria-label="روابط الموقع">
          <Link href="/emergency">أرقام الطوارئ</Link>
          <Link href="/how-we-verify">كيف نتحقق</Link>
          <Link href="/faq">الأسئلة الشائعة</Link>
          <Link href="/owners">لأصحاب المنشآت</Link>
          <Link href="/contact">تواصل معنا</Link>
          <Link href="/privacy">الخصوصية</Link>
          <Link href="/terms">الشروط</Link>
          <Link href="/support">الدعم</Link>
          <Link href="/delete-account">حذف الحساب</Link>
        </nav>
        <p>دليني — دليل محلي لتقديم معلومات واضحة وقابلة للتحقق عن المنشآت والخدمات.</p>
      </footer>
    </>
  );
}
