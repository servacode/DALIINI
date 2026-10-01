"use client";

import { useEffect, useState } from "react";

import { Icon, type IconName } from "./icons";

type Theme = "auto" | "light" | "dark";

const ORDER: readonly Theme[] = ["auto", "light", "dark"];
const META: Record<Theme, { label: string; icon: IconName }> = {
  auto: { label: "المظهر: تلقائي حسب الجهاز", icon: "palette" },
  light: { label: "المظهر: فاتح", icon: "sun" },
  dark: { label: "المظهر: داكن", icon: "moon" },
};
const KEY = "dl-admin-theme";

function apply(theme: Theme): void {
  if (theme === "auto") delete document.documentElement.dataset.theme;
  else document.documentElement.dataset.theme = theme;
}

/**
 * Light, dark or follow the device.
 *
 * The choice is a per-operator convenience, so it lives in this browser only; storage can be
 * unavailable (private windows, blocked site data), in which case the console simply follows
 * the device. The colours themselves come from the shared tokens, so nothing here knows a hex.
 */
export function ThemeToggle() {
  const [theme, setTheme] = useState<Theme>("auto");

  useEffect(() => {
    let saved: Theme = "auto";
    try {
      const raw = window.localStorage.getItem(KEY);
      if (raw === "light" || raw === "dark") saved = raw;
    } catch {
      // Storage blocked: follow the device.
    }
    apply(saved);
    // Reading storage can only happen after mount; one extra render is the price.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setTheme(saved);
  }, []);

  function next(): void {
    const following = ORDER[(ORDER.indexOf(theme) + 1) % ORDER.length]!;
    setTheme(following);
    apply(following);
    try {
      if (following === "auto") window.localStorage.removeItem(KEY);
      else window.localStorage.setItem(KEY, following);
    } catch {
      // Not persisted; the choice still holds for this page.
    }
  }

  const meta = META[theme];
  return (
    <button
      type="button"
      className="icon-button"
      onClick={next}
      aria-label={meta.label}
      title={meta.label}
      data-testid="theme-toggle"
    >
      <Icon name={meta.icon} />
    </button>
  );
}
