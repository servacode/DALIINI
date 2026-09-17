import { NextResponse } from "next/server";
import { clearRefreshCookie } from "../../../../lib/auth/cookies";

export async function POST(request: Request) {
  const origin = request.headers.get("origin");
  const host = request.headers.get("host");
  if (origin && host && new URL(origin).host !== host) {
    return NextResponse.json({ code: "ORIGIN_MISMATCH" }, { status: 403 });
  }
  await clearRefreshCookie();
  return NextResponse.json({ ok: true });
}
