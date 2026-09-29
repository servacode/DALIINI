import { mkdir, readFile, writeFile } from 'node:fs/promises';
import { resolve } from 'node:path';
import { loadTokens, resolveRefs } from './lib.mjs';

const check = process.argv.includes('--check');
const root = resolve(import.meta.dirname, '..');
const output = resolve(root, 'generated');
await mkdir(resolve(output, 'android', 'values'), { recursive: true });
const tokens = await loadTokens();
const flat = resolveRefs(tokens);

const pascal = (s) => s.split('.').map((part) => part.replace(/(^|[-_])(\w)/g, (_, __, c) => c.toUpperCase())).join('');
const cssName = (s) => s.replaceAll('.', '-').replace(/[A-Z]/g, (m) => `-${m.toLowerCase()}`);
const ts = `// GENERATED — DO NOT EDIT\nexport const tokens = ${JSON.stringify(tokens, null, 2)} as const;\n`;
// A bare number is a length in pixels except where the token names a unitless quantity (a font
// weight, a shadow opacity) or a time (a motion duration). Emitting `700px` for a weight made the
// declaration invalid, so browsers silently dropped it.
const cssValue = (key, value) => {
  if (typeof value !== 'number') return value;
  if (/\.weight$|\.opacity$/.test(key)) return `${value}`;
  if (key.startsWith('motion.duration.')) return `${value}ms`;
  return `${value}px`;
};
const cssLines = (entries) => entries.filter(([,v]) => typeof v === 'string' || typeof v === 'number').map(([k,v]) => `  --sd-${cssName(k)}: ${cssValue(k, v)};`).join('\n');
// Dark mode re-points the same `--sd-semantic-*` names at the dark set, so anything written against
// the semantic layer follows the theme with no change. It applies when the device prefers dark,
// unless a page pins light with data-theme="light"; data-theme="dark" forces it either way.
const darkSemantic = Object.entries(flat).filter(([k]) => k.startsWith('semanticDark.')).map(([k, v]) => [k.replace(/^semanticDark\./, 'semantic.'), v]);
const css = `/* GENERATED — DO NOT EDIT */\n:root {\n${cssLines(Object.entries(flat))}\n}\n@media (prefers-color-scheme: dark) {\n  :root:not([data-theme="light"]) {\n  ${cssLines(darkSemantic).replaceAll('\n', '\n  ')}\n  }\n}\n:root[data-theme="dark"] {\n${cssLines(darkSemantic)}\n}\n`;
const kotlinLines = Object.entries(flat).filter(([,v]) => typeof v === 'string' || typeof v === 'number').map(([k,v]) => `    const val ${pascal(k)} = ${typeof v === 'number' ? `${v}` : JSON.stringify(v)}`);
const kotlin = `// GENERATED — DO NOT EDIT\npackage com.servacode.directory.designsystem.generated\n\nobject DirectoryTokens {\n${kotlinLines.join('\n')}\n}\n`;
const swiftLines = Object.entries(flat).filter(([,v]) => typeof v === 'string' || typeof v === 'number').map(([k,v]) => `    static let ${pascal(k).replace(/^./, (c) => c.toLowerCase())} = ${typeof v === 'number' ? `${v}` : JSON.stringify(v)}`);
const swift = `// GENERATED — DO NOT EDIT\nimport Foundation\n\nenum DirectoryTokens {\n${swiftLines.join('\n')}\n}\n`;
// Android resources cannot read Kotlin constants, so the colours are also emitted as colour
// resources, for XML that needs them (a vector drawable, the launch theme). Named token_<path>.
const androidName = (s) => `token_${s.replaceAll('.', '_').replace(/[A-Z]/g, (m) => `_${m.toLowerCase()}`)}`;
const androidColorLines = Object.entries(flat).filter(([,v]) => typeof v === 'string' && /^#[0-9A-Fa-f]{6}([0-9A-Fa-f]{2})?$/.test(v)).map(([k,v]) => `    <color name="${androidName(k)}">${v}</color>`);
const androidColors = `<?xml version="1.0" encoding="utf-8"?>\n<!-- GENERATED — DO NOT EDIT -->\n<resources xmlns:tools="http://schemas.android.com/tools" tools:ignore="UnusedResources">\n${androidColorLines.join('\n')}\n</resources>\n`;
// Night resources carry the same names with the dark values, so a drawable or theme that reads
// token_semantic_* follows the system's dark mode on its own.
const androidNightLines = darkSemantic.filter(([,v]) => typeof v === 'string' && /^#[0-9A-Fa-f]{6}([0-9A-Fa-f]{2})?$/.test(v)).map(([k,v]) => `    <color name="${androidName(k)}">${v}</color>`);
const androidNightColors = `<?xml version="1.0" encoding="utf-8"?>\n<!-- GENERATED — DO NOT EDIT -->\n<resources xmlns:tools="http://schemas.android.com/tools" tools:ignore="UnusedResources">\n${androidNightLines.join('\n')}\n</resources>\n`;
await mkdir(resolve(output, 'android', 'values-night'), { recursive: true });
const files = {'tokens.ts': ts, 'tokens.css': css, 'DirectoryTokens.kt': kotlin, 'DirectoryTokens.swift': swift, 'android/values/directory_token_colors.xml': androidColors, 'android/values-night/directory_token_colors.xml': androidNightColors};
for (const [name, body] of Object.entries(files)) {
  const path = resolve(output, name);
  if (check) {
    let current;
    try { current = await readFile(path, 'utf8'); } catch { throw new Error(`Generated file missing: ${name}`); }
    if (current !== body) throw new Error(`Generated token drift: ${name}`);
  } else await writeFile(path, body);
}
console.log(check ? 'Generated token outputs are current' : 'Generated token outputs written');
