import { term } from "@servacode/design-tokens/vocabulary";

/*
 * The contact form's vocabulary and limits, shared by the /contact page (server)
 * and the form itself (client). The limits are the API's (ContactMessageRequest);
 * the words are the shared vocabulary's, so the console files a message under the
 * same name the visitor chose.
 */
export type ContactKind = "GENERAL" | "OWNER" | "CORRECTION";

export const CONTACT_KINDS: readonly { value: ContactKind; label: string }[] = (
  ["GENERAL", "OWNER", "CORRECTION"] as const
).map((value) => ({ value, label: term("contactKind", value).ar }));

export const NAME_MAX = 120;
export const PHONE_MAX = 20;
export const MESSAGE_MAX = 1000;

/* ?kind=owner, ?kind=CORRECTION…; undefined for anything else. */
export function parseContactKind(value: string | null | undefined): ContactKind | undefined {
  const wanted = value?.trim().toUpperCase();
  return CONTACT_KINDS.find((k) => k.value === wanted)?.value;
}
