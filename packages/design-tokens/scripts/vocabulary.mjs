import { readFile, writeFile, mkdir } from 'node:fs/promises';
import { resolve } from 'node:path';

/*
 * The shared vocabulary: one Arabic label and one colour tone per state, emitted for every
 * platform from vocabulary.json. `--check` fails on drift, the same contract as the tokens.
 */
const check = process.argv.includes('--check');
const root = resolve(import.meta.dirname, '..');
const vocab = JSON.parse(await readFile(resolve(root, 'vocabulary.json'), 'utf8'));
delete vocab.$comment;

const TONES = new Set(['neutral', 'positive', 'warning', 'danger', 'info', 'brand', 'accent']);
for (const [group, entries] of Object.entries(vocab)) {
  for (const [key, entry] of Object.entries(entries)) {
    if (!entry.ar || !TONES.has(entry.tone)) throw new Error(`Invalid vocabulary entry ${group}.${key}`);
  }
}

const snake = (s) => s.replace(/[A-Z]/g, (m) => `_${m.toLowerCase()}`).replace(/^_/, '');
/*
 * Android string resources, escaped in the order the escaping requires.
 *
 * The backslash goes first, because it is the escape character: replacing it after the
 * apostrophe would escape the backslash this function had just added and undo the escape. The
 * previous version skipped it altogether, along with `>` and `"`, so a word carrying any of
 * those four produced a resource file Android would not parse.
 */
const xmlEscape = (s) =>
  s
    .replace(/\\/g, '\\\\')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, "\\'");

const ts = `// GENERATED — DO NOT EDIT (source: vocabulary.json)
export type Tone = "neutral" | "positive" | "warning" | "danger" | "info" | "brand" | "accent";
export type Term = Readonly<{ ar: string; tone: Tone }>;
export const vocabulary = ${JSON.stringify(vocab, null, 2)} as const satisfies Record<string, Record<string, Term>>;
export type VocabularyGroup = keyof typeof vocabulary;
/** The label and tone for a state, falling back to the raw value so an unknown state still shows. */
export function term(group: VocabularyGroup, key: string | null | undefined): Term {
  const entries = vocabulary[group] as Record<string, Term>;
  return (key && entries[key]) || { ar: key ?? "—", tone: "neutral" };
}
`;

const androidLines = [];
for (const [group, entries] of Object.entries(vocab)) {
  for (const [key, entry] of Object.entries(entries)) {
    androidLines.push(`    <string name="vocab_${snake(group)}_${key.toLowerCase()}">${xmlEscape(entry.ar)}</string>`);
  }
}
const android = `<?xml version="1.0" encoding="utf-8"?>
<!-- GENERATED — DO NOT EDIT (source: vocabulary.json) -->
<resources xmlns:tools="http://schemas.android.com/tools" tools:ignore="UnusedResources">
${androidLines.join('\n')}
</resources>
`;

const swiftCases = Object.entries(vocab).map(([group, entries]) =>
  `    static let ${group}: [String: (ar: String, tone: String)] = [\n${Object.entries(entries).map(([k, e]) => `        ${JSON.stringify(k)}: (${JSON.stringify(e.ar)}, ${JSON.stringify(e.tone)})`).join(',\n')}\n    ]`,
).join('\n');
const swift = `// GENERATED — DO NOT EDIT (source: vocabulary.json)
import Foundation

enum DirectoryVocabulary {
${swiftCases}
}
`;

await mkdir(resolve(root, 'generated', 'android', 'values'), { recursive: true });
const files = {
  'generated/vocabulary.ts': ts,
  'generated/android/values/directory_vocabulary.xml': android,
  'generated/DirectoryVocabulary.swift': swift,
};
for (const [name, body] of Object.entries(files)) {
  const path = resolve(root, name);
  if (check) {
    let current;
    try { current = await readFile(path, 'utf8'); } catch { throw new Error(`Generated file missing: ${name}`); }
    if (current !== body) throw new Error(`Generated vocabulary drift: ${name}`);
  } else await writeFile(path, body);
}
console.log(check ? 'Generated vocabulary outputs are current' : 'Generated vocabulary outputs written');
