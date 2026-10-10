import { rm } from "node:fs/promises";

import { Boom } from "@hapi/boom";
// Named, including the socket factory: Baileys is CommonJS, so its default export is the whole
// module object rather than the factory, and `import makeWASocket from …` yields an object.
import {
  DisconnectReason,
  fetchLatestBaileysVersion,
  makeWASocket,
  useMultiFileAuthState,
} from "@whiskeysockets/baileys";
import pino from "pino";

/*
 * The WhatsApp connection, and what keeps it alive.
 *
 * The session is the whole asset here. Pairing scans a QR code once; after that the credentials
 * on disk are what let the process reconnect without a human. Lose that directory and
 * registration stops for everyone until somebody scans a new code — so it belongs on a volume
 * that survives a restart and a redeploy, and nowhere else: it is the account.
 *
 * Reconnection is automatic except for one case. When WhatsApp says `loggedOut` the credentials
 * are dead — the account was unlinked, or banned — and reconnecting in a loop only hammers it.
 * The socket then stays down and `status()` says so, which is what makes the backend fail
 * loudly instead of accepting registrations it cannot complete.
 *
 * Pairing is done from the console (DECISION-116): while no account is linked the session keeps
 * the latest QR code WhatsApp offers, and `pairing()` hands it to the backend, which draws it for
 * the platform's owner to scan. `relink()` forgets the account and starts a fresh pairing — the
 * way back from `loggedOut`, and the way to move the bot to another number.
 */

const logger = pino({ level: process.env.LOG_LEVEL ?? "warn" });

export class WhatsAppSession {
  /** @param {{authDir: string, onQr?: (qr: string) => void}} options */
  constructor({ authDir, onQr }) {
    this.authDir = authDir;
    this.onQr = onQr;
    this.socket = null;
    this.connected = false;
    /** Set when the credentials are dead: no amount of reconnecting will help. */
    this.loggedOut = false;
    this.lastDisconnect = null;
    /** The QR code WhatsApp offers right now, while nothing is linked. Never logged. */
    this.qr = null;
    this.qrAt = null;
    /** The linked account's number, digits only, once connected. */
    this.linkedNumber = null;
    /** Which socket is current: events from one replaced by `relink()` are ignored. */
    this.generation = 0;
  }

  async start() {
    const generation = ++this.generation;
    const { state, saveCreds } = await useMultiFileAuthState(this.authDir);
    const { version } = await fetchLatestBaileysVersion();

    this.socket = makeWASocket({
      version,
      auth: state,
      logger,
      // The pairing code is printed by `pair.js`, which is the only thing that should ever show
      // a QR; the server never does.
      printQRInTerminal: false,
      // A phone, because that is what the account is. An unfamiliar client string is one more
      // thing that makes the traffic look automated.
      browser: ["Daliini", "Chrome", "120.0.0"],
      markOnlineOnConnect: false,
    });

    this.socket.ev.on("creds.update", saveCreds);
    this.socket.ev.on("connection.update", (update) => {
      if (generation === this.generation) this.#onUpdate(update);
    });
    return this;
  }

  #onUpdate({ connection, lastDisconnect, qr }) {
    if (qr) {
      this.qr = qr;
      this.qrAt = Date.now();
      if (this.onQr) this.onQr(qr);
    }

    if (connection === "open") {
      this.connected = true;
      this.loggedOut = false;
      this.qr = null;
      this.qrAt = null;
      // «9639xxxxxxxx:12@s.whatsapp.net» — the number is what comes before the device and host.
      const id = String(this.socket?.user?.id ?? "");
      this.linkedNumber = id.split(/[:@]/)[0] || null;
      logger.warn("whatsapp.connected");
      return;
    }

    if (connection !== "close") return;

    this.connected = false;
    this.qr = null;
    const status = new Boom(lastDisconnect?.error)?.output?.statusCode;
    this.lastDisconnect = status ?? null;

    if (status === DisconnectReason.loggedOut) {
      // The credentials are gone. Reconnecting cannot bring them back, and trying in a loop
      // is exactly what a banned account does.
      this.loggedOut = true;
      logger.error("whatsapp.logged_out");
      return;
    }

    logger.warn({ status }, "whatsapp.reconnecting");
    // Baileys drops the socket on any close; a fresh one with the same credentials is the
    // documented way back.
    setTimeout(() => this.start().catch((error) => logger.error(error, "whatsapp.restart_failed")), 3000);
  }

  status() {
    return {
      connected: this.connected,
      loggedOut: this.loggedOut,
      lastDisconnect: this.lastDisconnect,
    };
  }

  /**
   * What the console's link card shows: whether an account is linked and which number, or the
   * QR code to scan. Only behind the shared secret — the code is an invitation to take the bot.
   */
  pairing() {
    return {
      connected: this.connected,
      loggedOut: this.loggedOut,
      linkedNumber: this.connected ? this.linkedNumber : null,
      qr: this.connected ? null : this.qr,
      qrAgeMs: this.qrAt ? Date.now() - this.qrAt : null,
    };
  }

  /**
   * Forget the linked account and start a fresh pairing.
   *
   * WhatsApp is told first when it can be (the device disappears from the phone's «linked
   * devices»), then the credentials on disk are removed and a new socket asks for a QR code.
   */
  async relink() {
    const old = this.socket;
    this.generation += 1;
    if (old && this.connected) {
      await old.logout().catch(() => undefined);
    }
    old?.end?.(undefined);
    this.connected = false;
    this.loggedOut = false;
    this.linkedNumber = null;
    this.qr = null;
    this.qrAt = null;
    await rm(this.authDir, { recursive: true, force: true });
    await this.start();
  }

  /**
   * Whether this number has a WhatsApp account.
   *
   * Asked before sending, because a message to a number with no account is not delivered and
   * the backend should say "this number is not on WhatsApp" rather than leave somebody waiting.
   */
  async exists(jid) {
    const [result] = await this.socket.onWhatsApp(jid.split("@")[0]);
    return Boolean(result?.exists);
  }

  async sendText(jid, text) {
    await this.socket.sendMessage(jid, { text });
  }
}
