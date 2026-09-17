import { readFile } from 'node:fs/promises';
import { resolve } from 'node:path';

const root = resolve(import.meta.dirname, '..');
const names = ['colors', 'semantic', 'typography', 'spacing', 'radius', 'elevation', 'motion'];

export async function loadTokens() {
  const entries = await Promise.all(names.map(async (name) => [name, JSON.parse(await readFile(resolve(root, 'tokens', `${name}.json`), 'utf8'))]));
  return Object.fromEntries(entries);
}

export function flatten(value, prefix = '') {
  const out = {};
  for (const [key, item] of Object.entries(value)) {
    const next = prefix ? `${prefix}.${key}` : key;
    if (item && typeof item === 'object' && !Array.isArray(item)) Object.assign(out, flatten(item, next));
    else out[next] = item;
  }
  return out;
}

export function resolveRefs(tokens) {
  const flat = flatten(tokens);
  const resolveValue = (value, stack = []) => {
    if (typeof value !== 'string') return value;
    const match = value.match(/^\{([^}]+)\}$/);
    if (!match) return value;
    const key = match[1];
    if (stack.includes(key)) throw new Error(`Circular token reference: ${[...stack, key].join(' -> ')}`);
    if (!(key in flat)) throw new Error(`Unknown token reference: ${key}`);
    return resolveValue(flat[key], [...stack, key]);
  };
  return Object.fromEntries(Object.entries(flat).map(([key, value]) => [key, resolveValue(value, [key])]));
}

export function contrastRatio(hexA, hexB) {
  const lum = (hex) => {
    const rgb = hex.slice(1).match(/.{2}/g).map((x) => parseInt(x, 16) / 255).map((c) => c <= 0.04045 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4);
    return 0.2126 * rgb[0] + 0.7152 * rgb[1] + 0.0722 * rgb[2];
  };
  const [a, b] = [lum(hexA), lum(hexB)].sort((x, y) => y - x);
  return (a + 0.05) / (b + 0.05);
}
