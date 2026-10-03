"use client";

import { useState } from "react";

import { type Resource, useResource } from "./use-resource";

/** The contract's cursor envelope: `nextCursor` is opaque and only ever handed back. */
export type CursorPage<Row> = Readonly<{
  items: readonly Row[];
  nextCursor: string | null;
  hasMore: boolean;
}>;

export type PaginationProps = Readonly<{
  hasMore: boolean;
  atFirst: boolean;
  loading: boolean;
  onFirst: () => void;
  onNext: () => void;
}>;

/**
 * One page of a cursor list, and the controls to move through it.
 *
 * Every console list is paged by the backend now; before, each one simply stopped at 200 or
 * 250 rows and the rest could not be reached. A change of filters returns to the first page:
 * a cursor belongs to the query that produced it, and handing it to another query would skip
 * or repeat rows. The reset happens during render, the way React resets state on a prop
 * change, so no request is ever sent with a stale cursor.
 */
export function useCursorPage<Row>(
  operation: string,
  filters: Readonly<Record<string, string | undefined>>,
  options: { enabled?: boolean; refreshMs?: number } = {},
): Resource<CursorPage<Row>> & Readonly<{ pagination: PaginationProps | null }> {
  const [cursor, setCursor] = useState<string | undefined>(undefined);
  const filtersKey = JSON.stringify(filters);
  const [pagedKey, setPagedKey] = useState(filtersKey);
  if (pagedKey !== filtersKey) {
    setPagedKey(filtersKey);
    setCursor(undefined);
  }
  const current = pagedKey === filtersKey ? cursor : undefined;
  const resource = useResource<CursorPage<Row>>(operation, { ...filters, cursor: current }, options);
  const data = resource.data;

  return {
    ...resource,
    pagination: data
      ? {
          hasMore: data.hasMore,
          atFirst: current === undefined,
          loading: resource.loading,
          onFirst: () => setCursor(undefined),
          onNext: () => setCursor(data.nextCursor ?? undefined),
        }
      : null,
  };
}
