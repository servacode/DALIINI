"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { type ReactNode, createContext, useContext } from "react";

import { logout } from "../lib/client/api";
import { useResource } from "../lib/client/use-resource";
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

const NAVIGATION = [
  ["/dashboard", "لوحة المتابعة", "admin.dashboard.read"],
  ["/reviews", "المراجعات", "admin.reviews.read"],
  ["/facilities", "المنشآت", "admin.facilities.read"],
  ["/users", "المستخدمون", "admin.users.read"],
  ["/taxonomy/groups", "مجموعات التصنيفات", "admin.taxonomy.read"],
  ["/taxonomy/categories", "التصنيفات", "admin.taxonomy.read"],
  ["/provinces", "المحافظات", "admin.provinces.read"],
  ["/verification", "التحقق", "admin.verification.read"],
  ["/ads", "الإعلانات", "admin.ads.read"],
  ["/audit", "سجل التدقيق", "admin.audit.read"],
  ["/analytics", "التحليلات", "admin.analytics.read"],
  ["/settings", "الإعدادات", "admin.settings.read"],
  ["/system", "النظام", "admin.system.read"],
] as const;

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
  const visible = NAVIGATION.filter(([, , permission]) =>
    identity.permissions.includes(permission),
  );

  return (
    <IdentityContext.Provider value={identity}>
      <div className="admin-shell">
        <aside className="sidebar">
          <strong>إدارة الدليل</strong>
          <nav aria-label="أقسام اللوحة" data-testid="admin-nav">
            {visible.map(([href, label]) => (
              <Link
                key={href}
                href={href}
                aria-current={pathname === href ? "page" : undefined}
                data-testid={`nav-${href.replace(/\//g, "-").slice(1)}`}
              >
                {label}
              </Link>
            ))}
          </nav>
          {visible.length === 0 ? (
            <p className="notice" data-testid="no-permissions">
              لا توجد صلاحيات مرتبطة بحسابك بعد. راجع مدير النظام.
            </p>
          ) : null}
          <div className="sidebar-footer">
            <span className="muted">{identity.displayName}</span>
            <button
              type="button"
              className="button-ghost"
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
            <strong>لوحة الإدارة</strong>
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
