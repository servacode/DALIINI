"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { type ReactNode, createContext, useContext, useState } from "react";

import { logout } from "../lib/client/api";
import { useResource } from "../lib/client/use-resource";
import { type IconName, Icons } from "./icons";
import { GlobalSearch } from "./global-search";
import { ThemeToggle } from "./theme-toggle";
import { BrandMark, ErrorState, LoadingState } from "./ui";

/**
 * The shell, and the one place the current operator is loaded.
 *
 * `/admin/me/` is fetched once here and shared through context, so thirteen screens do not
 * each ask who the caller is. Navigation hides what the operator cannot reach.
 *
 * Hiding a link is not access control. Typing the URL still renders the page, and the page
 * still calls the backend, which refuses — that refusal is the actual boundary. What the
 * hiding buys is a console that shows an operator their own job rather than a wall of
 * entries that all end in 403.
 */

export type AdminIdentity = Readonly<{
  userId: string;
  displayName: string;
  permissions: readonly string[];
}>;

const IdentityContext = createContext<AdminIdentity | null>(null);

export function useIdentity(): AdminIdentity {
  const identity = useContext(IdentityContext);
  if (!identity) throw new Error("useIdentity must be used inside AdminShell");
  return identity;
}

/** Convenience for a screen that only needs to branch on one code. */
export function useCan(permission: string): boolean {
  return useIdentity().permissions.includes(permission);
}

/** One page inside a section: its URL, its tab label and the permission that opens it. */
type Page = Readonly<{ href: string; label: string; permission: string }>;
type Section = Readonly<{ key: string; label: string; icon: IconName; pages: readonly Page[] }>;

/**
 * Nine sections, grouped by the job an operator is doing. Related screens share one section
 * and appear as tabs inside it, so the rail stays short and the operator's mental map is
 * "reviews and reports", "catalogue", "platform" rather than fifteen separate places. Every
 * page keeps its own URL; only the way to reach it changed.
 */
const NAVIGATION: readonly { label: string; sections: readonly Section[] }[] = [
  {
    label: "التشغيل",
    sections: [
      {
        key: "home",
        label: "الرئيسية",
        icon: "dashboard",
        pages: [{ href: "/dashboard", label: "الرئيسية", permission: "admin.dashboard.read" }],
      },
      {
        key: "tasks",
        label: "المراجعات والبلاغات",
        icon: "inbox",
        pages: [
          { href: "/reviews", label: "طلبات المراجعة", permission: "admin.reviews.read" },
          { href: "/reports", label: "البلاغات", permission: "admin.reports.read" },
        ],
      },
      {
        key: "facilities",
        label: "المنشآت",
        icon: "building",
        pages: [{ href: "/facilities", label: "المنشآت", permission: "admin.facilities.read" }],
      },
      {
        key: "users",
        label: "المستخدمون والصلاحيات",
        icon: "users",
        pages: [{ href: "/users", label: "المستخدمون", permission: "admin.users.read" }],
      },
    ],
  },
  {
    label: "الدليل",
    sections: [
      {
        key: "catalogue",
        label: "التصنيفات والتحقق",
        icon: "tag",
        pages: [
          { href: "/taxonomy/categories", label: "التصنيفات", permission: "admin.taxonomy.read" },
          { href: "/taxonomy/groups", label: "المجموعات", permission: "admin.taxonomy.read" },
          { href: "/verification", label: "متطلبات التحقق", permission: "admin.verification.read" },
        ],
      },
      {
        key: "regions",
        label: "المناطق",
        icon: "mapPin",
        pages: [{ href: "/provinces", label: "المحافظات والمدن", permission: "admin.provinces.read" }],
      },
      {
        key: "ads",
        label: "الإعلانات",
        icon: "megaphone",
        pages: [{ href: "/ads", label: "الإعلانات", permission: "admin.ads.read" }],
      },
    ],
  },
  {
    label: "المنصة",
    sections: [
      {
        key: "analytics",
        label: "التقارير والإحصاءات",
        icon: "chart",
        pages: [{ href: "/analytics", label: "الإحصاءات", permission: "admin.analytics.read" }],
      },
      {
        key: "platform",
        label: "الإعدادات والنظام",
        icon: "settings",
        pages: [
          { href: "/settings", label: "الإعدادات", permission: "admin.settings.read" },
          { href: "/system", label: "حالة النظام", permission: "admin.system.read" },
          { href: "/audit", label: "سجل العمليات", permission: "admin.audit.read" },
          { href: "/design", label: "نظام التصميم", permission: "admin.system.read" },
        ],
      },
    ],
  },
];

const ALL_SECTIONS = NAVIGATION.flatMap((group) => group.sections);

/** The section and page a path belongs to, so a detail page still lights its section and tab. */
function locate(pathname: string): { section: Section; page: Page } | undefined {
  let best: { section: Section; page: Page } | undefined;
  for (const section of ALL_SECTIONS) {
    for (const page of section.pages) {
      if (pathname === page.href || pathname.startsWith(`${page.href}/`)) {
        if (!best || page.href.length > best.page.href.length) best = { section, page };
      }
    }
  }
  return best;
}

function initials(name: string): string {
  const parts = name.trim().split(/\s+/).filter(Boolean);
  return (parts[0]?.[0] ?? "") + (parts[1]?.[0] ?? "") || "؟";
}

export function AdminShell({ children }: { children: ReactNode }) {
  const router = useRouter();
  const pathname = usePathname();
  const me = useResource<AdminIdentity>("me");
  const [navOpen, setNavOpen] = useState(false);
  const [signingOut, setSigningOut] = useState(false);

  if (me.loading) {
    return (
      <main className="login-shell">
        <LoadingState label="جارٍ تحميل الجلسة…" />
      </main>
    );
  }

  if (!me.data) {
    // A 401 already redirected inside `useResource`; anything else is shown here rather
    // than rendering a shell whose navigation would be a guess.
    return (
      <main className="login-shell">
        <ErrorState error={me.error} onRetry={me.reload} />
      </main>
    );
  }

  const identity = me.data;
  const can = (page: Page) => identity.permissions.includes(page.permission);
  const groups = NAVIGATION.map((group) => ({
    label: group.label,
    sections: group.sections
      .map((section) => ({ ...section, pages: section.pages.filter(can) }))
      .filter((section) => section.pages.length > 0),
  })).filter((group) => group.sections.length > 0);
  const here = locate(pathname);
  const tabs = here ? here.section.pages.filter(can) : [];
  const isDetail = here ? pathname !== here.page.href : false;

  return (
    <IdentityContext.Provider value={identity}>
      <div className="admin-shell" data-nav-open={navOpen}>
        <aside className="sidebar" id="admin-sidebar">
          <div className="sidebar-brand">
            <BrandMark />
            <div className="brand-text">
              <strong>دليني</strong>
              <span>لوحة الإدارة</span>
            </div>
          </div>
          <div className="sidebar-scroll">
            <nav aria-label="أقسام اللوحة" data-testid="admin-nav">
              {groups.map((group) => (
                <div key={group.label} className="nav-group" role="group" aria-label={group.label}>
                  <span className="nav-group-label" aria-hidden="true">
                    {group.label}
                  </span>
                  {group.sections.map((section) => {
                    const Glyph = Icons[section.icon];
                    const href = section.pages[0]!.href;
                    return (
                      <Link
                        key={section.key}
                        href={href}
                        className="nav-link"
                        aria-current={here?.section.key === section.key ? "page" : undefined}
                        data-testid={`nav-${href.replace(/\//g, "-").slice(1)}`}
                        onClick={() => setNavOpen(false)}
                      >
                        <Glyph />
                        <span>{section.label}</span>
                      </Link>
                    );
                  })}
                </div>
              ))}
            </nav>
            {groups.length === 0 ? (
              <p className="notice" data-testid="no-permissions">
                لا توجد صلاحيات مرتبطة بحسابك بعد. راجع مدير النظام.
              </p>
            ) : null}
          </div>
          <div className="sidebar-footer">
            <span className="avatar" aria-hidden="true">
              {initials(identity.displayName)}
            </span>
            <div className="sidebar-user">
              <strong>{identity.displayName}</strong>
              <span>فريق التشغيل</span>
            </div>
            <button
              type="button"
              className="sidebar-logout"
              data-testid="logout"
              aria-label="تسجيل الخروج"
              title="تسجيل الخروج"
              disabled={signingOut}
              onClick={async () => {
                setSigningOut(true);
                try {
                  await logout();
                  router.replace("/login");
                  router.refresh();
                } finally {
                  setSigningOut(false);
                }
              }}
            >
              <Icons.logout />
            </button>
          </div>
        </aside>
        {navOpen ? (
          <button
            type="button"
            className="sidebar-scrim"
            aria-label="إغلاق القائمة"
            onClick={() => setNavOpen(false)}
          />
        ) : null}
        <section className="admin-main">
          <header className="topbar">
            <div className="topbar-start">
              <button
                type="button"
                className="menu-toggle"
                aria-label="القائمة"
                aria-controls="admin-sidebar"
                aria-expanded={navOpen}
                onClick={() => setNavOpen(!navOpen)}
              >
                <Icons.menu />
              </button>
              <ol className="breadcrumb" aria-label="المسار">
                <li>لوحة الإدارة</li>
                {here ? (
                  <>
                    {here.section.pages.length > 1 ? <li>{here.section.label}</li> : null}
                    {isDetail ? (
                      <>
                        <li>
                          <Link href={here.page.href}>{here.page.label}</Link>
                        </li>
                        <li aria-current="page">التفاصيل</li>
                      </>
                    ) : (
                      <li aria-current="page">{here.page.label}</li>
                    )}
                  </>
                ) : null}
              </ol>
              <GlobalSearch />
            </div>
            <div className="topbar-end">
              <ThemeToggle />
              <div className="topbar-identity" data-testid="operator-name">
              <span className="identity-name">{identity.displayName}</span>
                <span className="avatar" aria-hidden="true">
                  {initials(identity.displayName)}
                </span>
              </div>
            </div>
          </header>
          <main className="content">
            {tabs.length > 1 && !isDetail ? (
              <nav className="section-tabs" aria-label={here!.section.label} data-testid="section-tabs">
                {tabs.map((tab) => (
                  <Link
                    key={tab.href}
                    href={tab.href}
                    className="section-tab"
                    aria-current={here!.page.href === tab.href ? "page" : undefined}
                  >
                    {tab.label}
                  </Link>
                ))}
              </nav>
            ) : null}
            {children}
          </main>
        </section>
      </div>
    </IdentityContext.Provider>
  );
}
