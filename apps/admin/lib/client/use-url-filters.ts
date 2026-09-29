"use client";

import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { useState } from "react";

/**
 * List filters that live in the URL.
 *
 * An alert, a task or a colleague's link can open a list already filtered
 * (/facilities?issue=STALE), and applying filters writes them back to the address bar, so
 * the view can be bookmarked or pasted into a chat. Only the declared keys are read; anything
 * else in the query string is ignored rather than sent to the backend.
 */
export function useUrlFilters(
  defaults: Readonly<Record<string, string>>,
): readonly [Record<string, string>, (next: Record<string, string>) => void] {
  const params = useSearchParams();
  const router = useRouter();
  const pathname = usePathname();
  const [filters, setFilters] = useState<Record<string, string>>(() => {
    const seeded: Record<string, string> = { ...defaults };
    for (const key of Object.keys(defaults)) {
      const value = params.get(key);
      if (value !== null) seeded[key] = value;
    }
    return seeded;
  });

  function apply(next: Record<string, string>): void {
    setFilters(next);
    const query = new URLSearchParams();
    for (const [key, value] of Object.entries(next)) {
      if (value && value !== defaults[key]) query.set(key, value);
    }
    const suffix = query.toString();
    router.replace(suffix ? `${pathname}?${suffix}` : pathname, { scroll: false });
  }

  return [filters, apply] as const;
}
