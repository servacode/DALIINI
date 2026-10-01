import { NextResponse } from "next/server";
import { getFacility, isUuid } from "../../../../lib/api";

/**
 * One facility's public details, for the card that opens without leaving the page.
 *
 * The browser cannot call the directory API itself: the origin is server-side configuration and
 * the call carries a server key that must never reach a bundle. So the page asks its own server,
 * which asks the API — and answers with exactly what `/f/{id}` already renders in public, no
 * more. Nothing here is reachable that a visitor could not already read on that page.
 *
 * It is fetched only when somebody opens a card, which is why the details are not loaded for
 * twelve facilities up front on the chance that one of them is wanted.
 */
export const revalidate = 300;

export async function GET(_request: Request, { params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  if (!isUuid(id)) return NextResponse.json({ error: "bad id" }, { status: 400 });

  const facility = await getFacility(id);
  if (facility === undefined) return NextResponse.json({ error: "not found" }, { status: 404 });
  if (facility === null) return NextResponse.json({ error: "unavailable" }, { status: 503 });

  return NextResponse.json(facility, {
    headers: { "Cache-Control": "public, s-maxage=300, stale-while-revalidate=600" },
  });
}
