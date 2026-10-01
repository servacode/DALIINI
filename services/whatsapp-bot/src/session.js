import { Boom } from "@hapi/boom";
import makeWASocket, {
  DisconnectReason,
  fetchLatestBaileysVersion,
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
  }

  async start() {
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
    this.socket.ev.on("connection.update", (update) => this.#onUpdate(update));
    return this;
  }

  #onUpdate({ connection, lastDisconnect, qr }) {
    if (qr && this.onQr) this.onQr(qr);

    if (connection === "open") {
      this.connected = true;
      this.loggedOut = false;
      logger.warn("whatsapp.connected");
      return;
    }

    if (connection !== "close") return;

    this.connected = false;
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
