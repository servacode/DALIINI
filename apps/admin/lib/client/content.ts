import { type VocabularyGroup, vocabulary } from "@servacode/design-tokens/vocabulary";

/**
 * What the content screens share: the kinds of pages, emergency numbers and contact
 * messages, and how a page body is read.
 *
 * The kinds' words come from the shared vocabulary, so the site's contact form and the
 * console's inbox name a message the same way.
 */

function labels(group: VocabularyGroup): Record<string, string> {
  return Object.fromEntries(Object.entries(vocabulary[group]).map(([key, entry]) => [key, entry.ar]));
}

export const PAGE_KINDS = labels("pageKind");

export const NUMBER_KINDS = labels("emergencyKind");

export const MESSAGE_KINDS = labels("contactKind");

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
