/**
 * The console's icons, drawn here rather than installed.
 *
 * Thirteen shapes at one size, one stroke weight and `currentColor` — a dependency would bring a
 * thousand and a second visual language with them. They are geometric on purpose: at 18px in a
 * navigation rail an icon is a landmark, not an illustration, and the label beside it carries the
 * meaning.
 */

export type IconName =
  | "dashboard"
  | "reviews"
  | "facilities"
  | "users"
  | "groups"
  | "categories"
  | "provinces"
  | "verification"
  | "ads"
  | "audit"
  | "analytics"
  | "settings"
  | "system";

const PATHS: Record<IconName, string> = {
  dashboard: "M4 4h6v6H4zM14 4h6v4h-6zM14 12h6v8h-6zM4 14h6v6H4z",
  reviews: "M4 6h16M4 12h10M4 18h7M15 17l2 2 4-4",
  facilities: "M5 20V6l7-3 7 3v14M9 20v-5h6v5M9 9h2M13 9h2M9 12h2M13 12h2",
  users: "M4 20v-1a4 4 0 0 1 4-4h2a4 4 0 0 1 4 4v1M9 11a3 3 0 1 0 0-6 3 3 0 0 0 0 6M16 20v-1a4 4 0 0 0-3-3.9M15 5.1a3 3 0 0 1 0 5.8",
  groups: "M12 3 3 8l9 5 9-5-9-5M3 13l9 5 9-5M3 17.5l9 5 9-5",
  categories: "M4 4h7v7H4zM13 4h7v7h-7zM4 13h7v7H4zM13 13h7v7h-7z",
  provinces: "M12 21s7-5.5 7-11a7 7 0 1 0-14 0c0 5.5 7 11 7 11M12 12a2.5 2.5 0 1 0 0-5 2.5 2.5 0 0 0 0 5",
  verification: "M12 3 5 6v5c0 4.5 3 8.3 7 10 4-1.7 7-5.5 7-10V6l-7-3M9 12l2 2 4-4",
  ads: "M4 8h16v11H4zM4 5h16M8 12h8M8 15.5h5",
  audit: "M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18M12 7.5V12l3 2",
  analytics: "M4 20h16M7.5 20v-6M12 20V8M16.5 20v-9",
  settings: "M4 7h10M18 7h2M4 17h4M12 17h8M16 7a2 2 0 1 0 0-.01M10 17a2 2 0 1 0 0-.01",
  system: "M4 5h16v5H4zM4 14h16v5H4zM7.5 7.5h.01M7.5 16.5h.01",
};

export function Icon({ name }: { name: IconName }) {
  return (
    <svg
      className="rail-icon"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.6"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      focusable="false"
    >
      <path d={PATHS[name]} />
    </svg>
  );
}
