// What the console's link card is given (DECISION-116): the QR code only while nothing is linked,
// the linked number only while connected, and never both.
import assert from "node:assert/strict";
import { test } from "node:test";

import { WhatsAppSession } from "../src/session.js";

test("while nothing is linked, the pairing shows the code and no number", () => {
  const session = new WhatsAppSession({ authDir: "./unused" });
  session.qr = "2@pairing-code";
  session.qrAt = Date.now();

  const pairing = session.pairing();

  assert.equal(pairing.connected, false);
  assert.equal(pairing.qr, "2@pairing-code");
  assert.equal(pairing.linkedNumber, null);
});

test("once linked, the pairing shows the number and never a code", () => {
  const session = new WhatsAppSession({ authDir: "./unused" });
  session.connected = true;
  session.linkedNumber = "963900000000";
  session.qr = "a stale code";

  const pairing = session.pairing();

  assert.equal(pairing.qr, null);
  assert.equal(pairing.linkedNumber, "963900000000");
});

test("the unauthenticated health check carries neither the code nor the number", () => {
  const session = new WhatsAppSession({ authDir: "./unused" });
  session.qr = "2@pairing-code";
  session.linkedNumber = "963900000000";

  const health = JSON.stringify(session.status());

  assert.doesNotMatch(health, /pairing-code/);
  assert.doesNotMatch(health, /963900000000/);
});
