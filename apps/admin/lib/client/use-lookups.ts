"use client";

import type { FilterField } from "../../components/ui";
import { useResource } from "./use-resource";

type Named = Readonly<{ id: string; nameAr: string }>;

/**
 * Province and category names, loaded once per screen that needs them.
 *
 * Several screens hold only ids (a facility row carries `provinceId`, a filter sends
 * `category`). Asking an operator to paste a UUID into a filter box, or reading one back
 * to them in a table, is the console leaking its storage model. This turns ids into names
 * and filter boxes into pickers.
 *
 * Both lists sit behind their own read permissions. An operator without them still gets a
 * working screen: the filter falls back to the raw id box and the table shows the id, so a
 * missing permission degrades the view rather than breaking it.
 */
export function useLookups() {
  const provinces = useResource<{ items: Named[] }>("provinces");
  const categories = useResource<{ items: Named[] }>("categories");

  const provinceItems = provinces.data?.items ?? null;
  const categoryItems = categories.data?.items ?? null;

  function nameIn(items: readonly Named[] | null, id: string | null | undefined): string {
    if (!id) return "—";
    return items?.find((item) => item.id === id)?.nameAr ?? id;
  }

  function filterFor(
    name: string,
    label: string,
    items: readonly Named[] | null,
  ): FilterField {
    return items
      ? {
          name,
          label,
          type: "select",
          options: items.map((item) => ({ value: item.id, label: item.nameAr })),
        }
      : { name, label: `معرّف ${label}`, placeholder: "UUID" };
  }

  return {
    ready: !provinces.loading && !categories.loading,
    provinceName: (id: string | null | undefined) => nameIn(provinceItems, id),
    categoryName: (id: string | null | undefined) => nameIn(categoryItems, id),
    provinceFilter: filterFor("province", "المحافظة", provinceItems),
    categoryFilter: filterFor("category", "التصنيف", categoryItems),
  };
}
