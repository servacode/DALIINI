import { NextResponse, type NextRequest } from "next/server";

/**
 * Maintenance mode for the site, decided by the platform's own switch.
 *
 * Pages are kept for minutes and kept on a failed refresh, so during maintenance the site went
 * on showing what it last fetched while the app showed the maintenance screen. Every page
 * request now asks the backend's status (remembered for 15 seconds, the backend's own window)
 * and, while maintenance is on, answers with /maintenance instead.
 *
 * A status that cannot be read is not maintenance: an unreachable API leaves the kept pages up,
 * which is what they are kept for.
 */
const REMEMBER_MS = 15_000;
let known: { on: boolean; at: number } | null = null;

async function maintenanceOn(): Promise<boolean> {
  const now = Date.now();
  if (known && now - known.at < REMEMBER_MS) return known.on;
  const origin = process.env.PUBLIC_API_ORIGIN?.trim();
  if (!origin) return false;
  let on = false;
  try {
    const response = await fetch(`${origin.replace(/\/$/, "")}/api/v1/platform/status/`, {
      cache: "no-store",
      signal: AbortSignal.timeout(1500),
    });
    if (response.ok) on = (await response.json())?.maintenance === true;
  } catch {
    on = known?.on ?? false;
  }
  known = { on, at: now };
  return on;
}

export async function proxy(request: NextRequest) {
  if (!(await maintenanceOn())) return NextResponse.next();
  return NextResponse.rewrite(new URL("/maintenance", request.url));
}

export const config = {
  // Pages only: not the maintenance page itself, Next's own files, or the site's fixed files.
  matcher: [
    "/((?!maintenance|_next|api|\.well-known|favicon|icon|apple-icon|opengraph-image|robots|sitemap|manifest).*)",
  ],
};
