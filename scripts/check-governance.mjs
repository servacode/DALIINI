import { access, readFile } from 'node:fs/promises';
import { createHash } from 'node:crypto';
import { resolve } from 'node:path';

const required = [
  'plan.md', 'PROJECT-STATUS.md', 'IMPLEMENTATION-LOG.md', 'DECISIONS.md',
  'BLOCKERS.md', 'HANDOFF.md', 'EVIDENCE.md', 'README.md', 'SECURITY.md',
  'docs/spec/00-START-HERE.md', 'docs/spec/01-MASTER-SPECIFICATION.md',
  'docs/spec/24-IMPLEMENTATION-ROADMAP.md', 'docs/adr/ADR-TEMPLATE.md',
  '.editorconfig', '.gitignore', '.env.example'
];
for (const path of required) await access(resolve(path));

const sums = await readFile('docs/spec/SHA256SUMS.txt', 'utf8');
for (const line of sums.trim().split(/\r?\n/)) {
  const match = line.match(/^([a-f0-9]{64})\s+\*?(.+)$/);
  if (!match) throw new Error(`Invalid checksum line: ${line}`);
  const [, expected, file] = match;
  const body = await readFile(resolve('docs/spec', file));
  const actual = createHash('sha256').update(body).digest('hex');
  if (actual !== expected) throw new Error(`Spec checksum mismatch: ${file}`);
}
console.log('P0 governance checks passed');
