/*
 * What the person reads.
 *
 * One line, the code on its own line so it is easy to copy, and no link — a message with a
 * link in it is the shape of every phishing attempt and is also what gets an account reported.
 * The wording says who it is from and what it is for, because a bare six digits from an unknown
 * number is indistinguishable from a scam.
 */

/** How long the code is good for, in minutes. Must match the backend's own expiry. */
export const CODE_MINUTES = 10;

export function codeMessage(code) {
  return [
    "رمز التحقّق في دليني:",
    "",
    code,
    "",
    `صالح ${CODE_MINUTES} دقائق. لا تشاركه مع أحد.`,
    "إن لم تطلبه، تجاهل هذه الرسالة.",
  ].join("\n");
}
