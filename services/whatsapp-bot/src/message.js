/*
 * What the person reads.
 *
 * One line, the code on its own line so it is easy to copy, and no link — a message with a
 * link in it is the shape of every phishing attempt and is also what gets an account reported.
 * The wording says who it is from and what it is for, because a bare six digits from an unknown
 * number is indistinguishable from a scam.
 */

/** How long the code is good for, in minutes. Must match the backend's own expiry. */
export const CODE_MINUTES = 5;

/**
 * The platform's first word to a new account (DECISION-101), sent a while after the code.
 *
 * Fixed words, held here rather than sent by the backend: this service will deliver only the
 * messages it knows. A bot that sends whatever text it is handed is a spam cannon the day its
 * secret leaks. No name in it either — a name is text a stranger typed, and it would be
 * delivered under our number.
 *
 * It also carries the support line: the number it arrives from is the one the team answers.
 */
export function welcomeMessage() {
  return [
    "أهلاً بك في دليني 🌿",
    "",
    "حسابك جاهز. تستطيع الآن تقييم المنشآت وحفظ ما يهمّك، وإن كانت لك منشأة فأضفها من «حسابي» لتظهر للناس بعد المراجعة.",
    "",
    "وهذا رقمنا: راسلنا هنا لأي مساعدة أو ملاحظة.",
  ].join("\n");
}

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
