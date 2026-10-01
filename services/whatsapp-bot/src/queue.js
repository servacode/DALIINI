/*
 * The pacing rules, kept apart from the socket so they can be proven without one.
 *
 * An ordinary WhatsApp account that suddenly sends a hundred messages to a hundred strangers
 * looks exactly like what it is, and the account is banned without warning or appeal. Nothing
 * here makes that safe — this account is outside WhatsApp's terms whatever it does — but these
 * three rules are what keep the traffic looking like a person rather than a broadcaster:
 *
 *   - **A gap between messages**, jittered, so the intervals are not a metronome.
 *   - **A ceiling per hour**, after which sends are refused rather than queued, because a
 *     queue that drains at midnight is a burst at midnight.
 *   - **One code per number per minute**, because a person pressing "resend" four times should
 *     cost one message, not four.
 */

/** The gap between two sends, in milliseconds, before jitter. */
export const MIN_GAP_MS = 4000;

/** How much of the gap is random, as a fraction. A fixed interval is itself a signature. */
export const JITTER = 0.5;

/** The most messages this account will send in any rolling hour. */
export const HOURLY_CAP = 60;

/** How long one number must wait before the same bot sends it another code. */
export const PER_NUMBER_COOLDOWN_MS = 60_000;

export class SendPacer {
  /**
   * @param {object} [options]
   * @param {() => number} [options.now] the clock, injected so tests need no real time
   * @param {number} [options.minGapMs]
   * @param {number} [options.hourlyCap]
   * @param {number} [options.cooldownMs]
   * @param {() => number} [options.random] jitter source, injected for the same reason
   */
  constructor(options = {}) {
    this.now = options.now ?? Date.now;
    this.minGapMs = options.minGapMs ?? MIN_GAP_MS;
    this.hourlyCap = options.hourlyCap ?? HOURLY_CAP;
    this.cooldownMs = options.cooldownMs ?? PER_NUMBER_COOLDOWN_MS;
    this.random = options.random ?? Math.random;

    /** Timestamps of sends inside the rolling hour, oldest first. */
    this.recent = [];
    /** The last time each number was sent to. */
    this.lastPerNumber = new Map();
  }

  /**
   * What to do with a send right now.
   *
   * @param {string} phone
   * @returns {{allow: true, waitMs: number} | {allow: false, reason: string, retryAfterMs: number}}
   */
  check(phone) {
    const now = this.now();
    this.#forget(now);

    const last = this.lastPerNumber.get(phone);
    if (last !== undefined && now - last < this.cooldownMs) {
      return {
        allow: false,
        reason: "too_soon_for_this_number",
        retryAfterMs: this.cooldownMs - (now - last),
      };
    }

    if (this.recent.length >= this.hourlyCap) {
      // Refused, not queued: a queue that drains later is a burst later.
      return {
        allow: false,
        reason: "hourly_cap",
        retryAfterMs: this.recent[0] + 3_600_000 - now,
      };
    }

    const sinceLast = this.recent.length ? now - this.recent[this.recent.length - 1] : Infinity;
    const gap = this.minGapMs * (1 + this.random() * JITTER);
    return { allow: true, waitMs: sinceLast >= gap ? 0 : Math.ceil(gap - sinceLast) };
  }

  /** Record that a message actually went out. Called after the send, never before. */
  record(phone) {
    const now = this.now();
    this.recent.push(now);
    this.lastPerNumber.set(phone, now);
    this.#forget(now);
  }

  #forget(now) {
    const hourAgo = now - 3_600_000;
    while (this.recent.length && this.recent[0] < hourAgo) this.recent.shift();
    for (const [phone, at] of this.lastPerNumber) {
      if (at < hourAgo) this.lastPerNumber.delete(phone);
    }
  }
}

/**
 * The WhatsApp address for a phone number.
 *
 * Numbers arrive from the backend in E.164 (`+9639XXXXXXXX`); WhatsApp wants the digits with
 * its own suffix and nothing else. Anything that is not a plausible international number is
 * rejected here rather than handed to the socket, which would answer with a less useful error.
 */
export function toWhatsAppId(phone) {
  const digits = String(phone ?? "").replace(/[^\d]/g, "");
  if (digits.length < 8 || digits.length > 15) return null;
  return `${digits}@s.whatsapp.net`;
}
