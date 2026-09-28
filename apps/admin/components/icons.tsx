import type { SVGProps } from "react";

import {
  type IconName,
  type IllustrationName,
  iconPaths,
  illustrationPaths,
  mirroredIcons,
} from "@servacode/design-tokens/icons";

/**
 * The console draws the platform's shared icon set (packages/design-tokens/icons), the same
 * drawings the app and the site use. Icons are decorative: every one is `aria-hidden`, and the
 * accessible name always comes from the visible label next to it. Directional icons flip in
 * right-to-left layouts.
 */
export type { IconName, IllustrationName };

export function Icon({ name, ...props }: Omit<SVGProps<SVGSVGElement>, "name"> & { name: IconName }) {
  return (
    <svg
      viewBox="0 0 24 24"
      width="18"
      height="18"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.8}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      focusable="false"
      data-mirror={mirroredIcons.has(name) || undefined}
      {...props}
    >
      {iconPaths[name].map((d) => (
        <path key={d} d={d} />
      ))}
    </svg>
  );
}

type SvgProps = Omit<SVGProps<SVGSVGElement>, "name">;
type Glyph = (props: SvgProps) => React.JSX.Element;

/** `Icons.clock`, `Icons[name]` — a component per icon, for call sites that pass one around. */
function glyph(name: IconName): Glyph {
  return function IconGlyph(props: SvgProps) {
    return <Icon {...props} name={name} />;
  };
}

export const Icons = Object.fromEntries(
  (Object.keys(iconPaths) as IconName[]).map((name) => [name, glyph(name)]),
) as Record<IconName, Glyph>;

/** A two-tone state illustration (empty, offline, error…) that follows the theme. */
export function Illustration({ name, size = 96 }: { name: IllustrationName; size?: number }) {
  const layers = illustrationPaths[name];
  return (
    <svg
      className="illustration"
      viewBox="0 0 120 120"
      width={size}
      height={size}
      fill="none"
      strokeWidth={2.5}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      focusable="false"
    >
      {layers.soft.map((d) => (
        <path key={d} d={d} className="illustration-soft" />
      ))}
      {layers.line.map((d) => (
        <path key={d} d={d} className="illustration-line" />
      ))}
      {layers.accent.map((d) => (
        <path key={d} d={d} className="illustration-accent" />
      ))}
    </svg>
  );
}
