import { redirect } from "next/navigation";

import { AdminShell } from "../../components/admin-shell";
import { hasSessionCookies } from "../../lib/auth/session";

/**
 * Everything behind sign-in shares this layout, so `/admin/me/` is fetched once per session
 * rather than once per page. A route group keeps the URLs unchanged.
 *
 * The cookie check here is a cheap redirect for a browser that clearly has no session; it
 * is not the security boundary. Whether the session is still valid is settled by the first
 * data call, which returns 401 and sends the operator back to the login screen.
 */
export const dynamic = "force-dynamic";

export default async function ConsoleLayout({ children }: { children: React.ReactNode }) {
  if (!(await hasSessionCookies())) redirect("/login");
  return <AdminShell>{children}</AdminShell>;
}
