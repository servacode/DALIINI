import brandSymbol from "@servacode/design-tokens/brand/symbol-128.webp";
import Link from "next/link";
import type { ReactNode } from "react";
import { getProvinces } from "../lib/api";
import { SiteHeader } from "./site-header";

/**
 * The frame every page sits in: a bar at the top, the page, and a bar at the bottom.
 *
 * The footer's links are grouped rather than laid in one row. Nine links side by side is a
 * list nobody reads to the end, and the groups are the three reasons somebody is down there:
 * to find something, to understand what this is, or to deal with their own account.
 */

const SECTIONS = [
  {
    title: "الدليل",
    links: [
      { href: "/duty", label: "المناوبات" },
      { href: "/search", label: "ابحث" },
      { href: "/emergency", label: "أرقام الطوارئ" },
    ],
  },
  {
    title: "عن دليني",
    links: [
      { href: "/how-we-verify", label: "كيف نتحقق" },
      { href: "/faq", label: "الأسئلة الشائعة" },
      { href: "/owners", label: "لأصحاب المنشآت" },
      { href: "/contact", label: "تواصل معنا" },
    ],
  },
  {
    title: "حسابك والشروط",
    links: [
      { href: "/support", label: "الدعم" },
      { href: "/privacy", label: "الخصوصية" },
      { href: "/terms", label: "الشروط" },
      { href: "/delete-account", label: "حذف الحساب" },
    ],
  },
] as const;

function BrandLink({ label }: { label: string }) {
  return (
    <Link href="/" className="brand" aria-label={label}>
      {/* eslint-disable-next-line @next/next/no-img-element -- small static brand asset */}
      <img className="brand-mark" src={brandSymbol.src} width={44} height={44} alt="" aria-hidden="true" />
      <span>دليني</span>
    </Link>
  );
}

export async function SiteShell({
  children,
  province,
}: {
  children: ReactNode;
  province?: string;
}) {
  const provinces = (await getProvinces()) ?? [];
  const current = provinces.find((p) => p.code === province)?.code ?? provinces[0]?.code ?? "";
  return (
    <>
      <SiteHeader provinces={provinces} province={current} />
      <main>{children}</main>
      <footer className="site-footer">
        <div className="shell footer">
          <div className="footer-top">
            <div className="footer-brand">
              <BrandLink label="دليني — الصفحة الرئيسية" />
              <p>دليل محلي لتقديم معلومات واضحة وقابلة للتحقق عن المنشآت والخدمات.</p>
            </div>
            <div className="footer-groups">
              {SECTIONS.map((section) => (
                <div className="footer-group" key={section.title}>
                  <h2>{section.title}</h2>
                  <nav aria-label={section.title}>
                    {section.links.map((link) => (
                      <Link key={link.href} href={link.href}>
                        {link.label}
                      </Link>
                    ))}
                  </nav>
                </div>
              ))}
            </div>
          </div>
          <div className="footer-bottom">
            <span>دليني</span>
            <span>المعلومات تُراجَع باستمرار، والمنشآت تُدار من أصحابها.</span>
          </div>
        </div>
      </footer>
    </>
  );
}
