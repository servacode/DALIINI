/*
 * The contact form's vocabulary and limits, shared by the /contact page (server)
 * and the form itself (client). The limits are the API's (ContactMessageRequest).
 */
export type ContactKind = "GENERAL" | "OWNER" | "CORRECTION";

export const CONTACT_KINDS: readonly { value: ContactKind; label: string }[] = [
  { value: "GENERAL", label: "استفسار أو اقتراح" },
  { value: "OWNER", label: "صاحب منشأة" },
  { value: "CORRECTION", label: "تصحيح معلومة" },
];

export const NAME_MAX = 120;
export const PHONE_MAX = 20;
export const MESSAGE_MAX = 1000;

/* ?kind=owner, ?kind=CORRECTION…; undefined for anything else. */
export function parseContactKind(value: string | null | undefined): ContactKind | undefined {
  const wanted = value?.trim().toUpperCase();
  return CONTACT_KINDS.find((k) => k.value === wanted)?.value;
}
