import { createServer } from "node:http";
import { timingSafeEqual } from "node:crypto";
import pino from "pino";

import { codeMessage } from "./message.js";
import { SendPacer, toWhatsAppId } from "./queue.js";
import { WhatsAppSession } from "./session.js";

/*
 * The bot's whole surface: one route that sends a code, and one that says whether it can.
 *
 * It is not on the internet. It listens on a private network and the backend is the only thing
 * that calls it, with a shared secret — a service that sends WhatsApp messages to any number on
 * request is a spam cannon if anybody else can reach it.
 *
 * Nothing here logs a code or a number.
 */

const logger = pino({ level: process.env.LOG_LEVEL ?? "warn" });

const PORT = Number(process.env.PORT ?? 8085);
const TOKEN = process.env.WHATSAPP_BOT_TOKEN ?? "";
const AUTH_DIR = process.env.WHATSAPP_AUTH_DIR ?? "./auth";
const MAX_BODY_BYTES = 4096;

if (!TOKEN) {
  logger.error("WHATSAPP_BOT_TOKEN is not set; refusing to start");
  process.exit(1);
}

const session = await new WhatsAppSession({ authDir: AUTH_DIR }).start();
const pacer = new SendPacer();

/** Compares in constant time, so the secret cannot be guessed a character at a time. */
function authorised(header) {
  const given = String(header ?? "").replace(/^Bearer\s+/i, "");
  const a = Buffer.from(given);
  const b = Buffer.from(TOKEN);
  return a.length === b.length && timingSafeEqual(a, b);
}

function reply(response, status, body) {
  const payload = JSON.stringify(body);
  response.writeHead(status, {
    "Content-Type": "application/json",
    "Content-Length": Buffer.byteLength(payload),
  });
  response.end(payload);
}

async function readJson(request) {
  const chunks = [];
  let size = 0;
  for await (const chunk of request) {
    size += chunk.length;
    // A body this size is not a phone number and a code; stop reading rather than buffer it.
    if (size > MAX_BODY_BYTES) throw new Error("body too large");
    chunks.push(chunk);
  }
  return JSON.parse(Buffer.concat(chunks).toString("utf8"));
}

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

async function handleSend(request, response) {
  let body;
  try {
    body = await readJson(request);
  } catch {
    return reply(response, 400, { reason: "bad_request" });
  }

  const jid = toWhatsAppId(body?.phone);
  const code = String(body?.code ?? "");
  if (!jid) {
    return reply(response, 400, { reason: "invalid_number" });
  }
  // Told apart from the number on purpose: the backend treats invalid_number as a verdict on
  // the recipient and never asks again, and a code we malformed ourselves is not that.
  if (!/^\d{4,8}$/.test(code)) {
    return reply(response, 400, { reason: "invalid_code" });
  }

  const status = session.status();
  if (!status.connected) {
    // Loudly, so the backend tells the person rather than leaving them waiting for a code
    // that was never sent.
    return reply(response, 503, {
      reason: status.loggedOut ? "logged_out" : "disconnected",
    });
  }

  const verdict = pacer.check(body.phone);
  if (!verdict.allow) {
    return reply(response, 429, {
      reason: verdict.reason,
      retryAfterMs: Math.max(0, Math.round(verdict.retryAfterMs)),
    });
  }

  try {
    if (!(await session.exists(jid))) {
      return reply(response, 422, { reason: "not_on_whatsapp" });
    }
    // The pacing gap, waited out here rather than by the caller, so the backend's own request
    // simply takes a moment longer and nothing has to queue on its side.
    if (verdict.waitMs) await sleep(verdict.waitMs);
    await session.sendText(jid, codeMessage(code));
    pacer.record(body.phone);
    return reply(response, 200, { sent: true });
  } catch (error) {
    logger.error({ err: error?.message }, "whatsapp.send_failed");
    return reply(response, 502, { reason: "send_failed" });
  }
}

createServer(async (request, response) => {
  if (request.method === "GET" && request.url === "/health") {
    const status = session.status();
    return reply(response, status.connected ? 200 : 503, status);
  }

  if (!authorised(request.headers.authorization)) {
    return reply(response, 401, { reason: "unauthorised" });
  }

  if (request.method === "POST" && request.url === "/send") {
    return handleSend(request, response);
  }

  return reply(response, 404, { reason: "not_found" });
}).listen(PORT, () => logger.warn({ port: PORT }, "whatsapp-bot.listening"));
