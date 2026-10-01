import qrcode from "qrcode-terminal";

import { WhatsAppSession } from "./session.js";

/*
 * Pairing: run once, scan the code, never again.
 *
 * WhatsApp on the phone → Settings → Linked devices → Link a device, and scan what appears
 * below. The credentials are written into the auth directory and the server reconnects with
 * them on its own after that.
 *
 * Keep that directory. It is the account: lose it and nobody can register until somebody is
 * physically present with the phone to scan again.
 */

const AUTH_DIR = process.env.WHATSAPP_AUTH_DIR ?? "./auth";

const session = new WhatsAppSession({
  authDir: AUTH_DIR,
  onQr(qr) {
    console.log("\nامسح هذا الرمز من واتساب على هاتفك:");
    console.log("الإعدادات ← الأجهزة المرتبطة ← ربط جهاز\n");
    qrcode.generate(qr, { small: true });
  },
});

await session.start();

const started = Date.now();
const timer = setInterval(() => {
  if (session.connected) {
    console.log("\nتمّ الربط. أوقف هذا الأمر وشغّل الخادم.");
    clearInterval(timer);
    process.exit(0);
  }
  if (Date.now() - started > 180_000) {
    console.error("\nانتهت المهلة بلا ربط. أعد المحاولة.");
    clearInterval(timer);
    process.exit(1);
  }
}, 1000);
