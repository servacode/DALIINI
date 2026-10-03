"use client";

import brandSymbol from "@servacode/design-tokens/brand/symbol-128.webp";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { Suspense, useState } from "react";
import type { Province } from "../lib/api";
import { publicConfig } from "../lib/config";
import { ProvincePicker } from "./province-picker";
import { SearchBox } from "./search-form";
import { ThemeToggle } from "./theme-toggle";
import { Icon } from "./ui";

/**
 * The bar across the top of every page.
 *
 * Three things decide how it is built:
 *
 *  - **The page you are on is shown, not guessed at.** A bar of five identical links tells a
 *    reader nothing about where they are; the current one is marked, and a facility page marks
 *    the section it belongs to rather than nothing at all.
 *  - **The province is chosen here**, because everything below is about one province and
 *    a choice that governs a page belongs at the top of it.
 *  - **A link closes the panel as it is followed**, rather than an effect watching the path
 *    and closing it after the fact — which is a render asking for another render.
 *  - **Below the breakpoint the links are behind a button.** They used to scroll sideways, which
 *    hides half of them off the edge of a phone with nothing to say they are there.
 */

const NAV = [
  { href: "/duty", label: "المناوبات" },
  { href: "/search", label: "ابحث" },
  { href: "/emergency", label: "الطوارئ" },
  { href: "/how-we-verify", label: "كيف نتحقق" },
  { href: "/support", label: "الدعم" },
] as const;

/** Where this path belongs, so a facility page still lights the section it came from. */
function activeHref(pathname: string): string | null {
  const match = NAV.filter((item) => pathname === item.href || pathname.startsWith(item.href + "/"))
    .sort((a, b) => b.href.length - a.href.length)[0];
  return match?.href ?? null;
}

export function SiteHeader({ provinces }: { provinces: Province[] }) {
  const pathname = usePathname() || "/";
  const [open, setOpen] = useState(false);
  const active = activeHref(pathname);

  return (
    <header className="site-header" data-open={open || undefined}>
      <div className="shell header">
        <Link href="/" className="brand" aria-label="دليني — الصفحة الرئيسية">
          {/* eslint-disable-next-line @next/next/no-img-element -- small static brand asset */}
          <img className="brand-mark" src={brandSymbol.src} width={56} height={56} alt="" aria-hidden="true" />
          <span className="brand-text">
            <strong>دليني</strong>
            <small>دليل الخدمات الصحية</small>
          </span>
        </Link>

        <nav className="header-nav" aria-label="التنقل العام">
          {NAV.map((item) => (
            <Link
              key={item.href}
              href={item.href}
              aria-current={active === item.href ? "page" : undefined}
            >
              {item.label}
            </Link>
          ))}
        </nav>

        <div className="header-end">
          {/* The picker reads the address, and reading the address is what stops a page being
              prerendered. Behind a boundary the rest of the bar still is. */}
          <Suspense fallback={null}>
            <ProvincePicker provinces={provinces} />
          </Suspense>
          <SearchBox id="header-q" variant="header" />
          {/* The app is the better way to use the directory, so the offer stands at the top of
              every page rather than at the bottom of one. */}
          {publicConfig.appDownloadUrl ? (
            <a
              className="button button-sm header-download"
              href={publicConfig.appDownloadUrl}
              rel="noopener"
            >
              <Icon name="download" size={16} />
              حمّل التطبيق
            </a>
          ) : null}
          <Link className="button button-alt button-sm header-cta" href="/owners">
            <Icon name="plus" size={16} />
            أضف منشأتك
          </Link>
          <ThemeToggle className="menu-button theme-toggle" />
          <button
            type="button"
            className="menu-button"
            aria-expanded={open}
            aria-controls="site-menu"
            aria-label={open ? "إغلاق القائمة" : "فتح القائمة"}
            onClick={() => setOpen((was) => !was)}
          >
            <Icon name={open ? "close" : "menu"} size={20} />
          </button>
        </div>
      </div>

      {/* The same links, as a panel, for the widths the row does not fit. */}
      <div className="header-panel" id="site-menu" hidden={!open}>
        <div className="shell">
          <SearchBox id="menu-q" variant="hero" />
          <nav aria-label="التنقل العام">
            {NAV.map((item) => (
              <Link
                key={item.href}
                href={item.href}
                aria-current={active === item.href ? "page" : undefined}
                onClick={() => setOpen(false)}
              >
                {item.label}
              </Link>
            ))}
          </nav>
          {publicConfig.appDownloadUrl ? (
            <a
              className="button"
              href={publicConfig.appDownloadUrl}
              rel="noopener"
              onClick={() => setOpen(false)}
            >
              <Icon name="download" size={16} />
              حمّل التطبيق
            </a>
          ) : null}
          <Link className="button button-alt" href="/owners" onClick={() => setOpen(false)}>
            <Icon name="plus" size={16} />
            أضف منشأتك
          </Link>
        </div>
      </div>
    </header>
  );
}
