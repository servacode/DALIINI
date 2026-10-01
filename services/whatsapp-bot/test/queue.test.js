import assert from "node:assert/strict";
import { test } from "node:test";

import { SendPacer, toWhatsAppId } from "../src/queue.js";

/*
 * The rules that keep this account alive, proven against a clock the test owns.
 *
 * Nothing here needs a network, a WhatsApp session or real time: the pacer takes its clock and
 * its jitter as arguments precisely so these can be facts rather than hopes.
 */

/** A pacer whose clock and jitter the test controls. */
function pacer(overrides = {}) {
  const state = { now: 1_000_000 };
  const instance = new SendPacer({
    now: () => state.now,
    random: () => 0,
    minGapMs: 4000,
    hourlyCap: 5,
    cooldownMs: 60_000,
    ...overrides,
  });
  return [instance, state];
}

test("the first message goes out with no wait", () => {
  const [p] = pacer();
  assert.deepEqual(p.check("+963900000001"), { allow: true, waitMs: 0 });
});

test("a second message waits out the gap", () => {
  const [p, clock] = pacer();
  p.record("+963900000001");
  clock.now += 1000;
  const verdict = p.check("+963900000002");
  assert.equal(verdict.allow, true);
  assert.equal(verdict.waitMs, 3000);
});

test("once the gap has passed there is nothing to wait for", () => {
  const [p, clock] = pacer();
  p.record("+963900000001");
  clock.now += 5000;
  assert.deepEqual(p.check("+963900000002"), { allow: true, waitMs: 0 });
});

test("jitter lengthens the gap, so the interval is not a metronome", () => {
  const [p, clock] = pacer({ random: () => 1 });
  p.record("+963900000001");
  clock.now += 1000;
  // 4000 * (1 + 1 * 0.5) = 6000, less the 1000 already elapsed.
  assert.equal(p.check("+963900000002").waitMs, 5000);
});

test("the same number cannot be sent to twice in a minute", () => {
  const [p, clock] = pacer();
  p.record("+963900000001");
  clock.now += 30_000;

  const again = p.check("+963900000001");
  assert.equal(again.allow, false);
  assert.equal(again.reason, "too_soon_for_this_number");
  assert.equal(again.retryAfterMs, 30_000);

  // Somebody else is unaffected: the cooldown is per number, not a global freeze.
  assert.equal(p.check("+963900000002").allow, true);
});

test("after the cooldown the same number may be sent to again", () => {
  const [p, clock] = pacer();
  p.record("+963900000001");
  clock.now += 61_000;
  assert.equal(p.check("+963900000001").allow, true);
});

test("the hourly ceiling refuses rather than queues", () => {
  const [p, clock] = pacer();
  for (let i = 0; i < 5; i += 1) {
    p.record(`+96390000000${i}`);
    clock.now += 5000;
  }
  const verdict = p.check("+963900000099");
  assert.equal(verdict.allow, false);
  assert.equal(verdict.reason, "hourly_cap");
  // A queue that drains later is a burst later, so the caller is told to come back instead.
  assert.ok(verdict.retryAfterMs > 0);
});

test("the ceiling is a rolling hour, not a calendar one", () => {
  const [p, clock] = pacer();
  for (let i = 0; i < 5; i += 1) {
    p.record(`+96390000000${i}`);
    clock.now += 5000;
  }
  assert.equal(p.check("+963900000099").allow, false);

  clock.now += 3_600_000;
  assert.equal(p.check("+963900000099").allow, true);
});

test("a number becomes a WhatsApp address, in any spelling", () => {
  assert.equal(toWhatsAppId("+963933000000"), "963933000000@s.whatsapp.net");
  assert.equal(toWhatsAppId("963933000000"), "963933000000@s.whatsapp.net");
  assert.equal(toWhatsAppId("+963 933 000 000"), "963933000000@s.whatsapp.net");
});

test("what is not a number is refused here rather than at the socket", () => {
  assert.equal(toWhatsAppId(""), null);
  assert.equal(toWhatsAppId("12345"), null);
  assert.equal(toWhatsAppId("not a number"), null);
  assert.equal(toWhatsAppId(undefined), null);
  assert.equal(toWhatsAppId("1".repeat(16)), null);
});
