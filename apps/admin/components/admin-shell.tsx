"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { type ReactNode, createContext, useContext } from "react";

import { logout } from "../lib/client/api";
import { useResource } from "../lib/client/use-resource";
import { Icon, type IconName } from "./icons";
import { ErrorState, LoadingState } from "./ui";

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
 *
 * The rail is grouped because thirteen flat links are a list to be read; five groups of two or
 * three are a place to be navigated. A group whose every entry is hidden by permissions
 * disappears with its heading, so an operator never reads a label for a section they do not have.
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

type Entry = Readonly<{ href: string; label: string; permission: string; icon: IconName }>;
type Group = Readonly<{ label: string; entries: readonly Entry[] }>;

const NAVIGATION: readonly Group[] = [
  {
    label: "العمليات",
    entries: [
      { href: "/dashboard", label: "لوحة المتابعة", permission: "admin.dashboard.read", icon: "dashboard" },
      { href: "/reviews", label: "المراجعات", permission: "admin.reviews.read", icon: "reviews" },
      { href: "/verification", label: "التحقق", permission: "admin.verification.read", icon: "verification" },
    ],
  },
  {
    label: "الدليل",
    entries: [
      { href: "/facilities", label: "المنشآت", permission: "admin.facilities.read", icon: "facilities" },
      { href: "/taxonomy/groups", label: "مجموعات التصنيفات", permission: "admin.taxonomy.read", icon: "groups" },
      { href: "/taxonomy/categories", label: "التصنيفات", permission: "admin.taxonomy.read", icon: "categories" },
      { href: "/provinces", label: "المحافظات", permission: "admin.provinces.read", icon: "provinces" },
    ],
  },
  {
    label: "المستخدمون والمحتوى",
    entries: [
      { href: "/users", label: "المستخدمون", permission: "admin.users.read", icon: "users" },
      { href: "/ads", label: "الإعلانات", permission: "admin.ads.read", icon: "ads" },
    ],
  },
  {
    label: "الرقابة",
    entries: [
      { href: "/audit", label: "سجل التدقيق", permission: "admin.audit.read", icon: "audit" },
      { href: "/analytics", label: "التحليلات", permission: "admin.analytics.read", icon: "analytics" },
    ],
  },
  {
    label: "النظام",
    entries: [
      { href: "/settings", label: "الإعدادات", permission: "admin.settings.read", icon: "settings" },
      { href: "/system", label: "النظام", permission: "admin.system.read", icon: "system" },
    ],
  },
] as const;

/** The first letter of a name, for the mark beside it. */
function initial(name: string): string {
  return name.trim().slice(0, 1) || "?";
}

export function AdminShell({ children }: { children: ReactNode }) {
  const router = useRouter();
  const pathname = usePathname();
  const me = useResource<AdminIdentity>("me");

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
    entries: group.entries.filter((entry) => identity.permissions.includes(entry.permission)),
  })).filter((group) => group.entries.length > 0);

  const current = groups
    .flatMap((group) => group.entries)
    .find((entry) => pathname === entry.href || pathname.startsWith(`${entry.href}/`));

  return (
    <IdentityContext.Provider value={identity}>
      <div className="admin-shell">
        <aside className="rail">
          <div className="rail-brand">
            <span className="rail-mark" aria-hidden="true">
              د
            </span>
            <span>
              <strong>دليني</strong>
              <span>لوحة الإدارة</span>
            </span>
          </div>

          <nav className="rail-nav" aria-label="أقسام اللوحة" data-testid="admin-nav">
            {groups.map((group) => (
              <div key={group.label}>
                <p className="rail-group-label">{group.label}</p>
                {group.entries.map((entry) => (
                  <Link
                    key={entry.href}
                    href={entry.href}
                    className="rail-link"
                    aria-current={pathname === entry.href ? "page" : undefined}
                    data-testid={`nav-${entry.href.replace(/\//g, "-").slice(1)}`}
                  >
                    <Icon name={entry.icon} />
                    <span>{entry.label}</span>
                  </Link>
                ))}
              </div>
            ))}
            {groups.length === 0 ? (
              <p className="rail-note" data-testid="no-permissions">
                لا توجد صلاحيات مرتبطة بحسابك بعد. راجع مدير النظام.
              </p>
            ) : null}
          </nav>

          <div className="rail-foot">
            <div className="rail-operator">
              <span className="avatar" aria-hidden="true">
                {initial(identity.displayName)}
              </span>
              <span className="rail-operator-name">{identity.displayName}</span>
            </div>
            <button
              type="button"
              className="button-ghost button-small"
              data-testid="logout"
              onClick={async () => {
                await logout();
                router.replace("/login");
                router.refresh();
              }}
            >
              تسجيل الخروج
            </button>
          </div>
        </aside>

        <section className="admin-main">
          <header className="topbar">
            <div className="topbar-context">
              <span className="topbar-place">لوحة الإدارة</span>
              <strong>{current?.label ?? "دليني"}</strong>
            </div>
            <div className="topbar-identity" data-testid="operator-name">
              <span>{identity.displayName}</span>
            </div>
          </header>
          <main className="content">{children}</main>
        </section>
      </div>
    </IdentityContext.Provider>
  );
}
