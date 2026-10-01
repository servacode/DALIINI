"use client";

import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { useEffect, useId, useRef, useState } from "react";
import type { Province } from "../lib/api";
import { Icon } from "./ui";

/**
 * Which province the page is about, chosen in the bar.
 *
 * The choice lives in the address, not in the component. That keeps a chosen province in the
 * back button, in a shared link and in a bookmark, and it means the list below is rendered on
 * the server for whoever opens that link rather than assembled again in their browser.
 *
 * Choosing a province clears the category with it: a category belongs to the province it was
 * offered for, and carrying it across would ask for a list that cannot exist.
 *
 * This is a listbox and not a `<select>`. A native menu is drawn by the operating system: on the
 * dark bar it opened as a small white slab with a blue bar through it, in the system's own font,
 * at the system's own size — the one part of the site the site does not get to design. The cost
 * of owning it is the keyboard, which is handled here in full: arrows, Home and End move,
 * Enter and Space choose, Escape closes and returns the focus to the button.
 */
export function ProvincePicker({ provinces, current }: { provinces: Province[]; current: string }) {
  const router = useRouter();
  const pathname = usePathname();
  const params = useSearchParams();

  const listId = useId();
  const [open, setOpen] = useState(false);
  const [cursor, setCursor] = useState(0);
  const root = useRef<HTMLDivElement>(null);
  const button = useRef<HTMLButtonElement>(null);

  const at = Math.max(0, provinces.findIndex((province) => province.code === current));
  const chosen = provinces[at];

  /* A menu left open behind a click elsewhere is a menu in the way of the page. */
  useEffect(() => {
    if (!open) return;
    const dismiss = (event: MouseEvent) => {
      if (!root.current?.contains(event.target as Node)) setOpen(false);
    };
    document.addEventListener("pointerdown", dismiss);
    return () => document.removeEventListener("pointerdown", dismiss);
  }, [open]);

  if (provinces.length === 0 || !chosen) return null;

  const choose = (index: number) => {
    const province = provinces[index];
    setOpen(false);
    button.current?.focus();
    if (!province || province.code === current) return;
    const next = new URLSearchParams(params.toString());
    next.set("p", province.code);
    next.delete("c");
    router.replace(`${pathname}?${next.toString()}`, { scroll: false });
  };

  const show = () => {
    setCursor(at);
    setOpen(true);
  };

  return (
    <div
      className="province-picker"
      ref={root}
      onKeyDown={(event) => {
        if (event.key === "Escape" && open) {
          event.preventDefault();
          setOpen(false);
          button.current?.focus();
          return;
        }
        if (!open) {
          if (event.key === "ArrowDown" || event.key === "ArrowUp") {
            event.preventDefault();
            show();
          }
          return;
        }
        const last = provinces.length - 1;
        if (event.key === "ArrowDown") {
          event.preventDefault();
          setCursor((was) => (was >= last ? 0 : was + 1));
        } else if (event.key === "ArrowUp") {
          event.preventDefault();
          setCursor((was) => (was <= 0 ? last : was - 1));
        } else if (event.key === "Home") {
          event.preventDefault();
          setCursor(0);
        } else if (event.key === "End") {
          event.preventDefault();
          setCursor(last);
        } else if (event.key === "Enter" || event.key === " ") {
          event.preventDefault();
          choose(cursor);
        }
      }}
    >
      <button
        type="button"
        ref={button}
        className="province-button"
        aria-haspopup="listbox"
        aria-expanded={open}
        aria-controls={open ? listId : undefined}
        aria-label={`المحافظة: ${chosen.nameAr}. اختر محافظتك`}
        onClick={() => (open ? setOpen(false) : show())}
      >
        <Icon name="mapPin" size={18} />
        <span>{chosen.nameAr}</span>
        <Icon name="chevron" size={16} />
      </button>

      {open ? (
        <ul className="province-menu" id={listId} role="listbox" aria-label="المحافظات" tabIndex={-1}>
          {provinces.map((province, index) => (
            <li key={province.id}>
              <button
                type="button"
                role="option"
                aria-selected={index === at}
                data-cursor={index === cursor || undefined}
                onMouseEnter={() => setCursor(index)}
                onClick={() => choose(index)}
              >
                {province.nameAr}
                {index === at ? <Icon name="check" size={16} /> : null}
              </button>
            </li>
          ))}
        </ul>
      ) : null}
    </div>
  );
}
