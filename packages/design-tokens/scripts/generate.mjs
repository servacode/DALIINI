import { mkdir, readFile, writeFile } from 'node:fs/promises';
import { resolve } from 'node:path';
import { loadTokens, resolveRefs } from './lib.mjs';

const check = process.argv.includes('--check');
const root = resolve(import.meta.dirname, '..');
const output = resolve(root, 'generated');
await mkdir(output, { recursive: true });
const tokens = await loadTokens();
const flat = resolveRefs(tokens);

const pascal = (s) => s.split('.').map((part) => part.replace(/(^|[-_])(\w)/g, (_, __, c) => c.toUpperCase())).join('');
const cssName = (s) => s.replaceAll('.', '-').replace(/[A-Z]/g, (m) => `-${m.toLowerCase()}`);
const ts = `// GENERATED — DO NOT EDIT\nexport const tokens = ${JSON.stringify(tokens, null, 2)} as const;\n`;
const css = `/* GENERATED — DO NOT EDIT */\n:root {\n${Object.entries(flat).filter(([,v]) => typeof v === 'string' || typeof v === 'number').map(([k,v]) => `  --sd-${cssName(k)}: ${typeof v === 'number' ? `${v}px` : v};`).join('\n')}\n}\n`;
const kotlinLines = Object.entries(flat).filter(([,v]) => typeof v === 'string' || typeof v === 'number').map(([k,v]) => `    const val ${pascal(k)} = ${typeof v === 'number' ? `${v}` : JSON.stringify(v)}`);
const kotlin = `// GENERATED — DO NOT EDIT\npackage com.servacode.directory.designsystem.generated\n\nobject DirectoryTokens {\n${kotlinLines.join('\n')}\n}\n`;
const swiftLines = Object.entries(flat).filter(([,v]) => typeof v === 'string' || typeof v === 'number').map(([k,v]) => `    static let ${pascal(k).replace(/^./, (c) => c.toLowerCase())} = ${typeof v === 'number' ? `${v}` : JSON.stringify(v)}`);
const swift = `// GENERATED — DO NOT EDIT\nimport Foundation\n\nenum DirectoryTokens {\n${swiftLines.join('\n')}\n}\n`;
const files = {'tokens.ts': ts, 'tokens.css': css, 'DirectoryTokens.kt': kotlin, 'DirectoryTokens.swift': swift};
for (const [name, body] of Object.entries(files)) {
  const path = resolve(output, name);
  if (check) {
    let current;
    try { current = await readFile(path, 'utf8'); } catch { throw new Error(`Generated file missing: ${name}`); }
    if (current !== body) throw new Error(`Generated token drift: ${name}`);
  } else await writeFile(path, body);
}
console.log(check ? 'Generated token outputs are current' : 'Generated token outputs written');
