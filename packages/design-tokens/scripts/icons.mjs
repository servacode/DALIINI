import { readFile, readdir, writeFile, mkdir } from 'node:fs/promises';
import { resolve } from 'node:path';

/*
 * The platform's icon set. Sources are 24px stroke drawings in icons/*.svg; this emits the same
 * drawings for the web (path data rendered by each app's <Icon>), for Android's XML (vector
 * drawables named dl_ic_<name>) and for the apps' shared Kotlin (DirectoryVectors.kt). Shapes are normalised to paths so both outputs agree.
 * Directional icons are marked to mirror in right-to-left layouts.
 */
const check = process.argv.includes('--check');
const root = resolve(import.meta.dirname, '..');
const MIRRORED = new Set(['arrowBack', 'chevron', 'logout', 'directions']);

const attrs = (s) => Object.fromEntries([...s.matchAll(/([\w-]+)="([^"]*)"/g)].map((m) => [m[1], m[2]]));
const n = (v) => Number(v ?? 0);
function toPath(tag, a) {
  if (tag === 'path') return a.d;
  if (tag === 'circle') {
    const [cx, cy, r] = [n(a.cx), n(a.cy), n(a.r)];
    return `M${cx - r} ${cy}a${r} ${r} 0 1 0 ${2 * r} 0a${r} ${r} 0 1 0 ${-2 * r} 0z`;
  }
  if (tag === 'rect') {
    const [x, y, w, h] = [n(a.x), n(a.y), n(a.width), n(a.height)];
    const r = Math.min(n(a.rx ?? a.ry), w / 2, h / 2);
    if (!r) return `M${x} ${y}h${w}v${h}h${-w}z`;
    return `M${x + r} ${y}h${w - 2 * r}a${r} ${r} 0 0 1 ${r} ${r}v${h - 2 * r}a${r} ${r} 0 0 1 ${-r} ${r}h${-(w - 2 * r)}a${r} ${r} 0 0 1 ${-r} ${-r}v${-(h - 2 * r)}a${r} ${r} 0 0 1 ${r} ${-r}z`;
  }
  if (tag === 'line') return `M${a.x1} ${a.y1}L${a.x2} ${a.y2}`;
  if (tag === 'polyline') return `M${a.points.trim().replace(/\s+/g, ' ')}`;
  throw new Error(`Unsupported shape <${tag}>`);
}

const files = (await readdir(resolve(root, 'icons'))).filter((f) => f.endsWith('.svg')).sort();
const icons = {};
for (const file of files) {
  const svg = await readFile(resolve(root, 'icons', file), 'utf8');
  const body = svg.replace(/^<svg[^>]*>/, '').replace(/<\/svg>\s*$/, '');
  icons[file.replace(/\.svg$/, '')] = [...body.matchAll(/<(\w+)\s([^>]*?)\/>/g)].map((m) => toPath(m[1], attrs(m[2])));
}

const ts = `// GENERATED — DO NOT EDIT (source: icons/*.svg)
/** 24px stroke icons; draw each path with stroke="currentColor", width 1.8, round caps and joins. */
export const iconPaths = ${JSON.stringify(icons, null, 2)} as const;
export type IconName = keyof typeof iconPaths;
/** Icons that point somewhere and must flip in a right-to-left layout. */
export const mirroredIcons: ReadonlySet<IconName> = new Set(${JSON.stringify([...MIRRORED])} as IconName[]);
`;

// Illustrations: 120px two-tone drawings for empty, offline, error and maintenance states.
// Three layers: `soft` (filled with the soft brand surface), `line` (muted strokes) and `accent`
// (brand strokes). Colours are applied by each platform from the semantic tokens, so the same
// drawing follows light and dark themes.
const illustrationFiles = (await readdir(resolve(root, 'illustrations'))).filter((f) => f.endsWith('.svg')).sort();
const illustrations = {};
for (const file of illustrationFiles) {
  const svg = await readFile(resolve(root, 'illustrations', file), 'utf8');
  const body = svg.replace(/^<svg[^>]*>/, '').replace(/<\/svg>\s*$/, '');
  const layers = { soft: [], line: [], accent: [] };
  for (const m of body.matchAll(/<(\w+)\s([^>]*?)\/>/g)) {
    const a = attrs(m[2]);
    layers[a.class === 'soft' ? 'soft' : a.class === 'accent' ? 'accent' : 'line'].push(toPath(m[1], a));
  }
  illustrations[file.replace(/\.svg$/, '')] = layers;
}
const tsFull = ts + `
/** 120px two-tone illustrations: fill \`soft\` with the soft brand surface, stroke \`line\` in the muted
 * content colour and \`accent\` in the brand colour, all at width 2.5 with round caps and joins. */
export const illustrationPaths = ${JSON.stringify(illustrations, null, 2)} as const;
export type IllustrationName = keyof typeof illustrationPaths;
`;

// The same drawings as Kotlin path data for the app's shared design system (DECISION-094), which
// builds them into vectors on Android and on the iPhone alike and colours them from the theme as
// it draws: no resource file, and an illustration follows a theme the reader chose, not only the
// phone's. Pure Kotlin, like DirectoryTokens, so it compiles anywhere.
const kotlinList = (paths, indent) =>
  paths.length === 0 ? 'emptyList()' : `listOf(\n${paths.map((d) => `${indent}    ${JSON.stringify(d)},`).join('\n')}\n${indent})`;
const kotlinVectors = `// GENERATED — DO NOT EDIT (source: icons/*.svg, illustrations/*.svg)
package com.servacode.directory.designsystem.generated

/** One 24px icon: path data drawn with a 1.8 stroke, round caps and joins, in the current colour. */
class IconPaths(val paths: List<String>, val mirrored: Boolean)

/**
 * One 120px illustration, in three layers: [soft] filled with the soft brand surface, [line]
 * stroked in the muted content colour and [accent] in the brand colour, at width 2.5.
 */
class IllustrationPaths(val soft: List<String>, val line: List<String>, val accent: List<String>)

object DirectoryIconPaths {
${Object.entries(icons).map(([name, paths]) => `    val ${name} = IconPaths(\n        ${kotlinList(paths, '        ')},\n        mirrored = ${MIRRORED.has(name)},\n    )`).join('\n')}
}

object DirectoryIllustrationPaths {
${Object.entries(illustrations).map(([name, l]) => `    val ${name} = IllustrationPaths(\n        soft = ${kotlinList(l.soft, '        ')},\n        line = ${kotlinList(l.line, '        ')},\n        accent = ${kotlinList(l.accent, '        ')},\n    )`).join('\n')}
}
`;

const snake = (s) => s.replace(/[A-Z]/g, (m) => `_${m.toLowerCase()}`);
const out = { 'generated/icons.ts': tsFull, 'generated/DirectoryVectors.kt': kotlinVectors };
for (const [name, layers] of Object.entries(illustrations)) {
  const draw = (paths, extra) => paths.map((d) => `    <path\n        android:pathData="${d}"\n${extra}" />`).join('\n');
  const stroke = (color) => `        android:fillColor="#00000000"\n        android:strokeColor="${color}"\n        android:strokeWidth="2.5"\n        android:strokeLineCap="round"\n        android:strokeLineJoin="round`;
  out[`generated/android/drawable/dl_illustration_${snake(name)}.xml`] = `<?xml version="1.0" encoding="utf-8"?>
<!-- GENERATED — DO NOT EDIT (source: illustrations/${name}.svg). Colours follow night mode. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="120dp"
    android:height="120dp"
    android:viewportWidth="120"
    android:viewportHeight="120">
${[draw(layers.soft, '        android:fillColor="@color/token_semantic_surface_brand_soft'), draw(layers.line, stroke('@color/token_semantic_content_muted')), draw(layers.accent, stroke('@color/token_semantic_action_primary'))].filter(Boolean).join('\n')}
</vector>
`;
}
for (const [name, paths] of Object.entries(icons)) {
  out[`generated/android/drawable/dl_ic_${snake(name)}.xml`] = `<?xml version="1.0" encoding="utf-8"?>
<!-- GENERATED — DO NOT EDIT (source: icons/${name}.svg) -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24"${MIRRORED.has(name) ? '\n    android:autoMirrored="true"' : ''}>
${paths.map((d) => `    <path
        android:pathData="${d}"
        android:fillColor="#00000000"
        android:strokeColor="#FF000000"
        android:strokeWidth="1.8"
        android:strokeLineCap="round"
        android:strokeLineJoin="round" />`).join('\n')}
</vector>
`;
}

await mkdir(resolve(root, 'generated', 'android', 'drawable'), { recursive: true });
for (const [name, body] of Object.entries(out)) {
  const path = resolve(root, name);
  if (check) {
    let current;
    try { current = await readFile(path, 'utf8'); } catch { throw new Error(`Generated file missing: ${name}`); }
    if (current !== body) throw new Error(`Generated icon drift: ${name}`);
  } else await writeFile(path, body);
}
console.log(`${check ? 'Generated icon outputs are current' : 'Generated icon outputs written'} (${files.length} icons, ${illustrationFiles.length} illustrations)`);
