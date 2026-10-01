import { NextResponse } from "next/server";
import { isUuid, reportFacility } from "../../../../../lib/api";

/**
 * A reader telling us a listing is wrong.
 *
 * The same endpoint the app's "report a problem" uses, so a report raised from a card and one
 * raised from the app arrive as one kind of thing in the console. It is deliberately reachable
 * without an account: somebody who drove to a closed pharmacy will not make an account to say so.
 *
 * The browser cannot post to the directory API itself — the origin is server-side configuration
 * and the call carries a server key — so it posts here and the server passes it on. Nothing is
 * accepted but a known reason and a short note; the API throttles the rest.
 */

const REASONS = new Set([
  "WRONG_INFO",
  "CLOSED_PERMANENTLY",
  "WRONG_LOCATION",
  "WRONG_HOURS",
  "NOT_ON_DUTY",
  "OTHER",
]);

const NOTE_MAX = 500;

export async function POST(request: Request, { params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  if (!isUuid(id)) return NextResponse.json({ error: "bad id" }, { status: 400 });

  let body: unknown;
  try {
    body = await request.json();
  } catch {
    return NextResponse.json({ error: "bad body" }, { status: 400 });
  }

  const { reason, note } = (body ?? {}) as { reason?: unknown; note?: unknown };
  if (typeof reason !== "string" || !REASONS.has(reason)) {
    return NextResponse.json({ error: "bad reason" }, { status: 400 });
  }
  const text = typeof note === "string" ? note.trim().slice(0, NOTE_MAX) : "";

  const outcome = await reportFacility(id, reason, text || undefined);
  if (outcome === "sent") return NextResponse.json({ ok: true }, { status: 201 });
  if (outcome === "throttled") return NextResponse.json({ error: outcome }, { status: 429 });
  if (outcome === "gone") return NextResponse.json({ error: outcome }, { status: 404 });
  return NextResponse.json({ error: "failed" }, { status: 502 });
}
