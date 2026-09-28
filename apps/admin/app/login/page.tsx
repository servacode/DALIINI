import { redirect } from "next/navigation";

import { LoginForm } from "../../components/auth/login-form";
import { hasSessionCookies } from "../../lib/auth/session";

export const metadata = { title: "تسجيل الدخول" };
export const dynamic = "force-dynamic";

export default async function LoginPage() {
  // A browser that already carries session cookies has no business on this screen. The
  // cookies are only a hint here; whether the session is still valid is settled by the
  // first data call, which redirects back if it is not.
  if (await hasSessionCookies()) redirect("/dashboard");

  return (
    <main className="login-shell">
      <section className="panel login-card">
        <header className="login-header">
          <p className="eyebrow">دليل سيرفا كود</p>
          <h1>تسجيل دخول الموظفين</h1>
          <p className="muted">هذه اللوحة مخصّصة لفريق التشغيل. الوصول مقيّد بالصلاحيات.</p>
        </header>
        <LoginForm />
      </section>
    </main>
  );
}
