"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { type ReactNode, createContext, useContext, useState } from "react";

import { logout } from "../lib/client/api";
import { useResource } from "../lib/client/use-resource";
import { type IconName, Icons } from "./icons";
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

type NavItem = readonly [href: string, label: string, permission: string, icon: IconName];

/**
 * Grouped by the job an operator is doing, in the order they usually do it. The flat order
 * inside each group is the old order, so muscle memory survives the regrouping.
 */
const NAVIGATION: readonly { label: string; items: readonly NavItem[] }[] = [
  {
    label: "التشغيل",
    items: [
      ["/dashboard", "لوحة المتابعة", "admin.dashboard.read", "dashboard"],
      ["/reviews", "المراجعات", "admin.reviews.read", "inbox"],
      ["/facilities", "المنشآت", "admin.facilities.read", "building"],
      ["/users", "المستخدمون", "admin.users.read", "users"],
    ],
  },
  {
    label: "الكتالوج",
    items: [
      ["/taxonomy/groups", "مجموعات التصنيفات", "admin.taxonomy.read", "layers"],
      ["/taxonomy/categories", "التصنيفات", "admin.taxonomy.read", "tag"],
      ["/provinces", "المحافظات", "admin.provinces.read", "map"],
    ],
  },
  {
    label: "الثقة والمحتوى",
    items: [
      ["/reports", "البلاغات", "admin.reports.read", "flag"],
      ["/verification", "التحقق", "admin.verification.read", "shield"],
      ["/ads", "الإعلانات", "admin.ads.read", "megaphone"],
      ["/audit", "سجل التدقيق", "admin.audit.read", "history"],
    ],
  },
  {
    label: "المنصة",
    items: [
      ["/analytics", "التحليلات", "admin.analytics.read", "chart"],
      ["/settings", "الإعدادات", "admin.settings.read", "settings"],
      ["/system", "النظام", "admin.system.read", "server"],
    ],
  },
];

const ALL_ITEMS = NAVIGATION.flatMap((group) => group.items);

/** The section a path belongs to, so a detail page still highlights its list. */
function sectionFor(pathname: string): NavItem | undefined {
  return ALL_ITEMS.filter(([href]) => pathname === href || pathname.startsWith(`${href}/`)).sort(
    (a, b) => b[0].length - a[0].length,
  )[0];
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
  const groups = NAVIGATION.map((group) => ({
    label: group.label,
    items: group.items.filter(([, , permission]) => identity.permissions.includes(permission)),
  })).filter((group) => group.items.length > 0);
  const current = sectionFor(pathname);
  const isDetail = current ? pathname !== current[0] : false;

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
                  {group.items.map(([href, label, , icon]) => {
                    const Glyph = Icons[icon];
                    return (
                      <Link
                        key={href}
                        href={href}
                        className="nav-link"
                        aria-current={current?.[0] === href ? "page" : undefined}
                        data-testid={`nav-${href.replace(/\//g, "-").slice(1)}`}
                        onClick={() => setNavOpen(false)}
                      >
                        <Glyph />
                        <span>{label}</span>
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
                {current ? (
                  isDetail ? (
                    <>
                      <li>
                        <Link href={current[0]}>{current[1]}</Link>
                      </li>
                      <li aria-current="page">التفاصيل</li>
                    </>
                  ) : (
                    <li aria-current="page">{current[1]}</li>
                  )
                ) : null}
              </ol>
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
          <main className="content">{children}</main>
        </section>
      </div>
    </IdentityContext.Provider>
  );
}
