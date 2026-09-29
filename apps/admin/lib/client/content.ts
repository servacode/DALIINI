/**
 * The words the content screens share: page kinds, emergency-number kinds, contact-message
 * kinds, and how a page body is read.
 *
 * These are console-only states (the app and the site never show a page's kind or a
 * message's category), so they are kept here, once, rather than in each screen.
 */

export const PAGE_KINDS: Record<string, string> = {
  PAGE: "صفحة عامة",
  LEGAL: "قانونية",
  FAQ: "أسئلة شائعة",
};

export const NUMBER_KINDS: Record<string, string> = {
  AMBULANCE: "إسعاف",
  FIRE: "إطفاء",
  POLICE: "شرطة",
  HOSPITAL: "مستشفى",
  OTHER: "أخرى",
};

export const MESSAGE_KINDS: Record<string, string> = {
  GENERAL: "استفسار عام",
  OWNER: "صاحب منشأة",
  CORRECTION: "تصحيح بيانات",
};

/** The slug rule the backend applies: letters, digits and inner hyphens, 1–64 long. */
export const SLUG_PATTERN = /^[A-Za-z0-9](?:[A-Za-z0-9-]{0,62}[A-Za-z0-9])?$/;

export type BodyBlock =
  | Readonly<{ kind: "question"; question: string; answer: readonly string[] }>
  | Readonly<{ kind: "text"; lines: readonly string[] }>;

/**
 * A page body as the site and the app read it: plain text, blocks separated by a blank
 * line, and a block whose first line ends in a question mark is a question and its answer.
 * The editor's preview uses the same reading, so what the operator previews is what ships.
 */
export function bodyBlocks(body: string): BodyBlock[] {
  return body
    .replace(/\r\n/g, "\n")
    .split(/\n\s*\n/)
    .map((chunk) =>
      chunk
        .split("\n")
        .map((line) => line.trim())
        .filter(Boolean),
    )
    .filter((lines) => lines.length > 0)
    .map((lines): BodyBlock =>
      lines[0]!.endsWith("؟") || lines[0]!.endsWith("?")
        ? { kind: "question", question: lines[0]!, answer: lines.slice(1) }
        : { kind: "text", lines },
    );
}
