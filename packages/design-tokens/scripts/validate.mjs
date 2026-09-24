import { loadTokens, resolveRefs, contrastRatio } from './lib.mjs';

const tokens = await loadTokens();
const requiredColors = ['primary','primaryStrong','primaryDeep','primarySoft','background','surface','textPrimary','textSecondary','border','success','warning','danger','info'];
for (const key of requiredColors) {
  if (!/^#[0-9A-F]{6}$/.test(tokens.colors[key] ?? '')) throw new Error(`Missing/invalid color: ${key}`);
}
const requiredTypography = ['display','headlineLarge','headlineMedium','titleLarge','titleMedium','bodyLarge','bodyMedium','bodySmall','labelLarge','labelMedium'];
for (const role of requiredTypography) {
  const value = tokens.typography.roles[role];
  if (!value || value.size <= 0 || value.lineHeight < value.size || ![400,500,600,700].includes(value.weight)) throw new Error(`Invalid typography role: ${role}`);
}
const spacing = Object.values(tokens.spacing);
if (![2,4,8,12,16,20,24,32,40,48,64].every((v) => spacing.includes(v))) throw new Error('Spacing scale does not match baseline');
const resolved = resolveRefs(tokens);
const checks = [
  ['text primary on surface', resolved['semantic.content.primary'], resolved['semantic.surface.default'], 4.5],
  ['text secondary on surface', resolved['semantic.content.secondary'], resolved['semantic.surface.default'], 4.5],
  ['on-primary on primary', resolved['semantic.content.onPrimary'], resolved['semantic.action.primary'], 4.5],
  ['primary text on canvas', resolved['semantic.content.primary'], resolved['semantic.surface.canvas'], 4.5],
  // The app's two bars are nearly black, so anything written on them has to be checked here
  // rather than trusted: the muted tone exists to be quieter than white, and quieter is
  // exactly the direction that runs out of contrast.
  ['on-bar on bar', resolved['semantic.content.onBar'], resolved['semantic.surface.bar'], 4.5],
  ['on-bar muted on bar', resolved['semantic.content.onBarMuted'], resolved['semantic.surface.bar'], 4.5],
  // The selected tab is a pale pill with the bar's own colour inside it.
  ['bar on brand-soft', resolved['semantic.surface.bar'], resolved['semantic.surface.brandSoft'], 4.5]
];
for (const [name, fg, bg, min] of checks) {
  const ratio = contrastRatio(fg, bg);
  if (ratio < min) throw new Error(`${name} contrast ${ratio.toFixed(2)} < ${min}`);
  console.log(`${name}: ${ratio.toFixed(2)} PASS`);
}
console.log('Design token validation passed');
