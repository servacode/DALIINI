"use client";

import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { useMemo } from "react";

/**
 * List filters that live in the URL.
 *
 * An alert, a task or a colleague's link can open a list already filtered
 * (/facilities?issue=STALE), and applying filters writes them back to the address bar, so
 * the view can be bookmarked or pasted into a chat. Only the declared keys are read; anything
 * else in the query string is ignored rather than sent to the backend.
 *
 * The address is the only copy. The filters are read from it on every render rather than
 * seeded once, so following a link to the same list with other filters (an alert, the global
 * search, the back button) shows those filters instead of the ones the screen started with.
 */
export function useUrlFilters(
  defaults: Readonly<Record<string, string>>,
): readonly [Record<string, string>, (next: Record<string, string>) => void] {
  const params = useSearchParams();
  const router = useRouter();
  const pathname = usePathname();
  const query = params.toString();
  const defaultsKey = JSON.stringify(defaults);

  const filters = useMemo(() => {
    const base = JSON.parse(defaultsKey) as Record<string, string>;
    const current = new URLSearchParams(query);
    for (const key of Object.keys(base)) {
      const value = current.get(key);
      if (value !== null) base[key] = value;
    }
    return base;
  }, [query, defaultsKey]);

  function apply(next: Record<string, string>): void {
    const out = new URLSearchParams();
    for (const [key, value] of Object.entries(next)) {
      if (value && value !== defaults[key]) out.set(key, value);
    }
    const suffix = out.toString();
    router.replace(suffix ? `${pathname}?${suffix}` : pathname, { scroll: false });
  }

  return [filters, apply] as const;
}
