/*
 * The three ways to act on a facility, built in one place.
 *
 * A card in a list and the facility's own page offer the same three things — call, WhatsApp,
 * directions — and they were only on the page, which put a page load between somebody and the
 * pharmacy they were already looking at. One definition here so a row and a page cannot drift
 * into building the same link two different ways.
 *
 * Nothing here touches the network or the server, so a client component may import it; `lib/api`
 * is `server-only` and deliberately cannot be.
 */

/**
 * The number as somebody here would write it down.
 *
 * The backend stores E.164 (`+9639XXXXXXXX`), which is what a machine needs and nobody says out
 * loud: a Syrian reading a Syrian number reads `09XX XXX XXX`. The country code is dropped, the
 * national zero restored, and the digits grouped the way they are spoken. Anything that is not a
 * Syrian mobile is returned as it was stored rather than forced into a shape it does not have.
 */
export function localPhone(raw: string | null | undefined): string | null {
  const trimmed = raw?.trim();
  if (!trimmed) return null;
  // `00` is the international prefix written out, which the backend accepts on the way in
  // (accounts/phone.py) and a person pasting a number may well use. Dropping it first makes
  // both ends read the same number the same way.
  const digits = trimmed.replace(/\D/g, "").replace(/^00/, "");
  const national = digits.startsWith("963") ? digits.slice(3) : digits.replace(/^0+/, "");
  const grouped = /^9\d{8}$/.test(national)
    ? `0${national}`.replace(/^(\d{4})(\d{3})(\d{3})$/, "$1 $2 $3")
    : null;
  return grouped ?? trimmed;
}

/** `tel:` wants no spaces in the number. Null when there is nothing to dial. */
export function telLink(raw: string | null | undefined): string | null {
  const number = raw?.replace(/\s+/g, "");
  return number ? `tel:${number}` : null;
}

/* WhatsApp wants international digits only; tolerate "+963 9…" style input. */
export function waLink(raw: string | null | undefined): string | null {
  const digits = raw?.replace(/[^\d]/g, "");
  return digits && digits.length >= 8 ? `https://wa.me/${digits}` : null;
}

/**
 * How to reach this facility on WhatsApp.
 *
 * The owner's WhatsApp number when they gave one. When they did not, a Syrian mobile
 * (`+9639XXXXXXXX`) is offered anyway: here a mobile line and a WhatsApp account are all but the
 * same thing, and the link opens a conversation with that number rather than asserting anything
 * about it. A landline is never offered, because messaging one goes nowhere.
 */
export function whatsAppFor(
  whatsapp: string | null | undefined,
  phone: string | null | undefined,
): string | null {
  const given = waLink(whatsapp);
  if (given) return given;
  const digits = phone?.replace(/[^\d]/g, "") ?? "";
  return /^9639\d{8}$/.test(digits) ? `https://wa.me/${digits}` : null;
}

/** Directions from wherever the visitor is, which is the map's business and not ours. */
export function directionsLink(
  location: { latitude: number; longitude: number } | null | undefined,
): string | null {
  if (!location) return null;
  const to = `${location.latitude},${location.longitude}`;
  return `https://www.google.com/maps/dir/?api=1&destination=${to}`;
}
