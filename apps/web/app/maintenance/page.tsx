import { redirect } from "next/navigation";

import { Illustration } from "../../components/ui";

export const metadata = { title: "الخدمة قيد الصيانة", robots: { index: false } };

// Read on every request: the page is shown only while the switch is on.
export const dynamic = "force-dynamic";

/** What every page answers with while the platform is in maintenance (see proxy.ts). */
export default async function Maintenance() {
  const { on, message } = await status();
  // Opened by hand, or kept from a visit during maintenance: there is nothing to wait for.
  if (!on) redirect("/");
  return (
    <div className="shell page" style={{ textAlign: "center" }}>
      <Illustration name="maintenance" size={120} />
      <h1>الخدمة قيد الصيانة</h1>
      <p>{message}</p>
      <p className="muted">نعود خلال وقت قصير. حدّث الصفحة بعد قليل.</p>
    </div>
  );
}

async function status(): Promise<{ on: boolean; message: string }> {
  const fallback = "المنصة قيد الصيانة حالياً. يرجى المحاولة لاحقاً.";
  const origin = process.env.PUBLIC_API_ORIGIN?.trim();
  // Unknown is not «off»: the proxy sent the visitor here because it was on.
  if (!origin) return { on: true, message: fallback };
  try {
    const response = await fetch(`${origin.replace(/\/$/, "")}/api/v1/platform/status/`, {
      cache: "no-store",
      signal: AbortSignal.timeout(1500),
    });
    const body = response.ok ? await response.json() : null;
    if (!body) return { on: true, message: fallback };
    const message =
      typeof body.messageAr === "string" && body.messageAr.trim() ? body.messageAr : fallback;
    return { on: body.maintenance === true, message };
  } catch {
    return { on: true, message: fallback };
  }
}
