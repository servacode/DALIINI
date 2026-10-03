"use client";

import { useSyncExternalStore } from "react";
import { Icon } from "./ui";

/**
 * Light, dark, or whatever the device is set to.
 *
 * The tokens already carry both themes (`:root[data-theme]`, and the system's preference when
 * there is no attribute), so the choice is one attribute on `<html>` and one key in storage.
 * `THEME_SCRIPT` puts it back before the first paint, so a reader who chose dark never sees a
 * white flash on the way in. «تلقائي» removes the attribute and hands the decision back to the
 * device, which is where it starts.
 */

type Choice = "system" | "light" | "dark";

const KEY = "daliini-theme";
const ORDER: Choice[] = ["system", "light", "dark"];
const LABELS: Record<Choice, string> = { system: "تلقائي", light: "فاتح", dark: "داكن" };
const ICONS = { system: "palette", light: "sun", dark: "moon" } as const;

/* Runs in <head> before the body is drawn; kept tiny and dependency-free on purpose. */
export const THEME_SCRIPT = `try{var t=localStorage.getItem("${KEY}");if(t==="light"||t==="dark")document.documentElement.dataset.theme=t}catch(e){}`;

const listeners = new Set<() => void>();

function subscribe(listener: () => void): () => void {
  listeners.add(listener);
  window.addEventListener("storage", listener);
  return () => {
    listeners.delete(listener);
    window.removeEventListener("storage", listener);
  };
}

function read(): Choice {
  try {
    const value = window.localStorage.getItem(KEY);
    return value === "light" || value === "dark" ? value : "system";
  } catch {
    return "system";
  }
}

function choose(choice: Choice): void {
  const root = document.documentElement;
  if (choice === "system") delete root.dataset.theme;
  else root.dataset.theme = choice;
  try {
    if (choice === "system") window.localStorage.removeItem(KEY);
    else window.localStorage.setItem(KEY, choice);
  } catch {
    /* Without storage the choice lasts for this page, which is still what was asked. */
  }
  listeners.forEach((listener) => listener());
}

export function ThemeToggle({ className = "theme-toggle" }: { className?: string }) {
  const current = useSyncExternalStore(subscribe, read, () => "system" as const);
  const next = ORDER[(ORDER.indexOf(current) + 1) % ORDER.length]!;
  return (
    <button
      type="button"
      className={className}
      onClick={() => choose(next)}
      aria-label={`المظهر: ${LABELS[current]}. اضغط للتبديل إلى ${LABELS[next]}`}
      title={`المظهر: ${LABELS[current]}`}
    >
      <Icon name={ICONS[current]} size={20} />
    </button>
  );
}
