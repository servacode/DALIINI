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
  /** Rows per page, as the operator chose it for this list. */
  limit?: number;
  onLimit?: (limit: number) => void;
}>;

/** The page sizes on offer; the backend's own default is 50 and its ceiling 200. */
export const PAGE_SIZES = [25, 50, 100] as const;
const DEFAULT_LIMIT = 50;
const LIMIT_PREFIX = "daliini.limit.";

function savedLimit(operation: string): number {
  try {
    const value = Number(window.localStorage.getItem(LIMIT_PREFIX + operation));
    return (PAGE_SIZES as readonly number[]).includes(value) ? value : DEFAULT_LIMIT;
  } catch {
    return DEFAULT_LIMIT;
  }
}

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
  // Kept per list in this browser. A new size starts from the first page, like new filters.
  const [limit, setLimit] = useState<number>(() => savedLimit(operation));
  const filtersKey = JSON.stringify({ ...filters, limit });
  const [pagedKey, setPagedKey] = useState(filtersKey);
  if (pagedKey !== filtersKey) {
    setPagedKey(filtersKey);
    setCursor(undefined);
  }
  const current = pagedKey === filtersKey ? cursor : undefined;
  const resource = useResource<CursorPage<Row>>(
    operation,
    { ...filters, limit: String(limit), cursor: current },
    options,
  );
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
          limit,
          onLimit: (next: number) => {
            setLimit(next);
            try {
              window.localStorage.setItem(LIMIT_PREFIX + operation, String(next));
            } catch {
              /* The size still applies until the page is left. */
            }
          },
        }
      : null,
  };
}
