"use client";

import { term } from "@servacode/design-tokens/vocabulary";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { Icon } from "./ui";

/**
 * Narrowing a category to what is useful right now.
 *
 * Both filters are the API's own, not a sieve over the page that happens to be loaded: asking
 * for "open now" asks the directory for the pharmacies that are open, so the second page of
 * results is filtered the same way the first one was. That is the whole reason they are in the
 * address rather than in a piece of state — the server does the filtering, and a filtered list
 * is a link somebody can send.
 *
 * They are separate questions, so both can be on at once: a pharmacy can be on tonight's roster
 * and shut at this minute, and somebody who wants one that is on duty *and* open should be able
 * to say so.
 */

/* The two words come from the shared vocabulary, so a chip and a badge cannot disagree. */
const FILTERS = [
  { key: "open", label: term("availability", "OPEN").ar, icon: "clock" as const },
  { key: "duty", label: term("availability", "DUTY").ar, icon: "shield" as const },
];

export function FacilityFilters() {
  const router = useRouter();
  const pathname = usePathname();
  const params = useSearchParams();

  const toggle = (key: string) => {
    const next = new URLSearchParams(params.toString());
    if (next.get(key) === "1") next.delete(key);
    else next.set(key, "1");
    router.replace(`${pathname}?${next.toString()}`, { scroll: false });
  };

  const any = FILTERS.some((filter) => params.get(filter.key) === "1");

  return (
    <div className="filter-bar" role="group" aria-label="تصفية النتائج">
      <button
        type="button"
        className="filter-chip"
        aria-pressed={!any}
        onClick={() => {
          const next = new URLSearchParams(params.toString());
          for (const filter of FILTERS) next.delete(filter.key);
          router.replace(`${pathname}?${next.toString()}`, { scroll: false });
        }}
      >
        الكل
      </button>
      {FILTERS.map((filter) => (
        <button
          key={filter.key}
          type="button"
          className="filter-chip"
          aria-pressed={params.get(filter.key) === "1"}
          onClick={() => toggle(filter.key)}
        >
          <Icon name={filter.icon} size={15} />
          {filter.label}
        </button>
      ))}
    </div>
  );
}
