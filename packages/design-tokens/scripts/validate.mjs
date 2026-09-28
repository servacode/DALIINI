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
// The dark set is checked on the same terms. Its primary button carries dark text on a light
// green, which is what keeps it legible; the selected tab in dark mode uses primary text on the
// soft brand surface rather than the bar colour.
const requiredDark = ['primary','primarySoft','onPrimary','background','surface','textPrimary','textSecondary','border','success','warning','danger','info'];
for (const key of requiredDark) {
  if (!/^#[0-9A-F]{6}$/.test(tokens.colorsDark[key] ?? '')) throw new Error(`Missing/invalid dark color: ${key}`);
}
for (const key of Object.keys(resolved).filter((k) => k.startsWith('semantic.'))) {
  if (!(key.replace(/^semantic\./, 'semanticDark.') in resolved)) throw new Error(`Dark set is missing ${key}`);
}
const d = (k) => resolved[`semanticDark.${k}`];
checks.push(
  ['dark: text primary on surface', d('content.primary'), d('surface.default'), 4.5],
  ['dark: text secondary on surface', d('content.secondary'), d('surface.default'), 4.5],
  ['dark: text muted on surface', d('content.muted'), d('surface.default'), 4.5],
  ['dark: on-primary on primary', d('content.onPrimary'), d('action.primary'), 4.5],
  ['dark: text primary on canvas', d('content.primary'), d('surface.canvas'), 4.5],
  ['dark: on-bar muted on bar', d('content.onBarMuted'), d('surface.bar'), 4.5],
  ['dark: text primary on brand-soft', d('content.primary'), d('surface.brandSoft'), 4.5],
  ['dark: danger on surface', d('feedback.danger'), d('surface.default'), 4.5],
  ['dark: warning on surface', d('content.warning'), d('surface.default'), 4.5],
  ['light: warning text on warning-soft', resolved['semantic.content.warning'], resolved['semantic.feedback.warningSoft'], 4.5],
  ['light: success on success-soft', resolved['semantic.feedback.success'], resolved['semantic.feedback.successSoft'], 3],
  ['light: danger on danger-soft', resolved['semantic.feedback.danger'], resolved['semantic.feedback.dangerSoft'], 4.5],
);
for (const [name, fg, bg, min] of checks) {
  const ratio = contrastRatio(fg, bg);
  if (ratio < min) throw new Error(`${name} contrast ${ratio.toFixed(2)} < ${min}`);
  console.log(`${name}: ${ratio.toFixed(2)} PASS`);
}
console.log('Design token validation passed');
