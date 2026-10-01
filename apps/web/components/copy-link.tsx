"use client";

import { useState } from "react";

/*
 * The only client script on facility pages: copies the page link. Falls back to
 * a hidden textarea + execCommand where the Clipboard API is unavailable (plain
 * http, older WebViews), and finally to showing the link for manual copying.
 */
export function CopyLink({ url }: { url: string }) {
  const [state, setState] = useState<"idle" | "copied" | "manual">("idle");

  async function copy() {
    try {
      await navigator.clipboard.writeText(url);
      setState("copied");
      return;
    } catch {
      /* fall through to the legacy path */
    }
    const area = document.createElement("textarea");
    area.value = url;
    area.setAttribute("readonly", "");
    area.style.position = "fixed";
    area.style.opacity = "0";
    document.body.appendChild(area);
    area.select();
    let ok = false;
    try {
      ok = document.execCommand("copy");
    } catch {
      ok = false;
    }
    area.remove();
    setState(ok ? "copied" : "manual");
  }

  return (
    <>
      <button type="button" className="button button-alt" onClick={copy}>
        {state === "copied" ? "تم نسخ الرابط" : "انسخ الرابط"}
      </button>
      <span className="sr-only" role="status">{state === "copied" ? "تم نسخ الرابط" : ""}</span>
      {state === "manual" ? (
        <input className="copy-manual ltr" readOnly value={url} aria-label="رابط الصفحة، انسخه يدوياً" onFocus={(e) => e.currentTarget.select()} autoFocus />
      ) : null}
    </>
  );
}
