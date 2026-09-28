"use client";

import { useRouter } from "next/navigation";
import { useEffect, useId, useRef, useState } from "react";

import { read } from "../lib/client/api";
import { Icon, type IconName } from "./icons";

type Hit = Readonly<{ type: string; id: string; titleAr: string; subtitle: string }>;
type Group = Readonly<{ type: string; items: readonly Hit[] }>;

const GROUPS: Record<string, { label: string; icon: IconName; href: (id: string) => string }> = {
  FACILITY: { label: "المنشآت", icon: "building", href: (id) => `/facilities/${id}` },
  USER: { label: "المستخدمون", icon: "user", href: (id) => `/users/${id}` },
  APPLICATION: { label: "الطلبات", icon: "inbox", href: (id) => `/reviews/${id}` },
};

/**
 * One box that finds a facility, a person or an application from anywhere.
 *
 * It asks after the operator pauses typing (250 ms) and only from two characters, keeps the
 * answer for the latest query only, and is driven fully by keyboard: "/" focuses it, arrows
 * move, Enter opens, Escape closes. What an operator may not read never comes back from the
 * backend, so there is nothing to hide here.
 */
export function GlobalSearch() {
  const router = useRouter();
  const listId = useId();
  const input = useRef<HTMLInputElement>(null);
  const [q, setQ] = useState("");
  const [groups, setGroups] = useState<readonly Group[] | null>(null);
  const [open, setOpen] = useState(false);
  const [active, setActive] = useState(0);
  const latest = useRef("");

  useEffect(() => {
    const onKey = (event: KeyboardEvent) => {
      const target = event.target as HTMLElement | null;
      const typing = target && (target.tagName === "INPUT" || target.tagName === "TEXTAREA" || target.isContentEditable);
      if (event.key === "/" && !typing) {
        event.preventDefault();
        input.current?.focus();
      }
    };
    document.addEventListener("keydown", onKey);
    return () => document.removeEventListener("keydown", onKey);
  }, []);

  useEffect(() => {
    const term = q.trim();
    latest.current = term;
    if (term.length < 2) return;
    const timer = window.setTimeout(() => {
      void read<{ groups: Group[] }>("globalSearch", { q: term }).then((result) => {
        if (latest.current !== term) return;
        setGroups(result.ok ? result.data.groups : []);
        setActive(0);
      });
    }, 250);
    return () => window.clearTimeout(timer);
  }, [q]);

  const flat = (q.trim().length >= 2 ? groups ?? [] : []).flatMap((group) =>
    group.items.map((hit) => ({ ...hit, group: group.type })),
  );

  function go(index: number): void {
    const hit = flat[index];
    if (!hit) return;
    const meta = GROUPS[hit.group];
    if (!meta) return;
    setOpen(false);
    setQ("");
    setGroups(null);
    router.push(meta.href(hit.id));
  }

  const showList = open && q.trim().length >= 2;
  let index = -1;

  return (
    <div className="global-search" role="search">
      <Icon name="search" />
      <input
        ref={input}
        type="search"
        value={q}
        placeholder="ابحث عن منشأة أو مستخدم أو رقم هاتف…"
        aria-label="بحث شامل"
        aria-expanded={showList}
        aria-controls={listId}
        aria-autocomplete="list"
        role="combobox"
        data-testid="global-search"
        onChange={(event) => {
          setQ(event.target.value);
          setOpen(true);
        }}
        onFocus={() => setOpen(true)}
        onBlur={() => window.setTimeout(() => setOpen(false), 150)}
        onKeyDown={(event) => {
          if (event.key === "ArrowDown") {
            event.preventDefault();
            setActive((value) => Math.min(value + 1, Math.max(flat.length - 1, 0)));
          } else if (event.key === "ArrowUp") {
            event.preventDefault();
            setActive((value) => Math.max(value - 1, 0));
          } else if (event.key === "Enter") {
            event.preventDefault();
            go(active);
          } else if (event.key === "Escape") {
            setOpen(false);
            input.current?.blur();
          }
        }}
      />
      <kbd aria-hidden="true">/</kbd>
      {showList ? (
        <div className="global-search-results" id={listId} role="listbox">
          {groups === null ? (
            <p className="muted">جارٍ البحث…</p>
          ) : flat.length === 0 ? (
            <p className="muted">لا نتائج لـ «{q.trim()}».</p>
          ) : (
            groups.map((group) =>
              group.items.length === 0 || !GROUPS[group.type] ? null : (
                <div key={group.type} className="global-search-group">
                  <span className="global-search-label">{GROUPS[group.type]!.label}</span>
                  {group.items.map((hit) => {
                    index += 1;
                    const mine = index;
                    return (
                      <button
                        key={hit.id}
                        type="button"
                        role="option"
                        aria-selected={active === mine}
                        className="global-search-hit"
                        onMouseEnter={() => setActive(mine)}
                        onMouseDown={(event) => {
                          event.preventDefault();
                          go(mine);
                        }}
                      >
                        <Icon name={GROUPS[group.type]!.icon} />
                        <span>
                          <strong>{hit.titleAr}</strong>
                          {hit.subtitle ? <span className="muted">{hit.subtitle}</span> : null}
                        </span>
                      </button>
                    );
                  })}
                </div>
              ),
            )
          )}
        </div>
      ) : null}
    </div>
  );
}
