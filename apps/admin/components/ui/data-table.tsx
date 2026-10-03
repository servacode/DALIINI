"use client";

import { type ReactNode, useState } from "react";

import { Icons } from "../icons";
import { EmptyState } from "./index";

/**
 * The console's table (phase 4.3).
 *
 * The columns, the rows and nothing else is the old contract, and still works unchanged.
 * A screen can ask for more:
 *
 * * **Sorting from the headers.**
 *   * A column with `sortKey` asks the backend for that order, through `sort` and `onSort`.
 *     A paged list has to work this way: sorting one page in the browser would be a lie
 *     about the rest.
 *   * A column with `sortValue` sorts the rows in hand, for a list the backend sends whole.
 *   * Pressing a header cycles ascending, descending, then back to the list's own order.
 *     The header carries `aria-sort`, so a screen reader hears the order too.
 * * **Choosing columns and density** with an `id`. The choice is kept in this browser under
 *   that id. A column marked `required` (the name, the actions) cannot be hidden.
 * * **A count** above the table, through `summary`.
 *
 * The header row stays in view while the rows scroll: the wrapper is the scroll container in
 * both directions, with a height bound, which is what `position: sticky` needs.
 */

export type Column<T> = Readonly<{
  key: string;
  /** Usually a word. A node when the header is a control, such as a select-all checkbox. */
  header: ReactNode;
  render: (row: T) => ReactNode;
  /** Identifiers and timestamps read left-to-right even in an RTL table. */
  ltr?: boolean;
  width?: string;
  /** The name in the column chooser, when `header` is not plain text. */
  label?: string;
  /** Never hidden by the column chooser. */
  required?: boolean;
  /** Hidden until the operator asks for it. */
  hiddenByDefault?: boolean;
  /** Sort the rows in hand by this value. */
  sortValue?: (row: T) => string | number | null | undefined;
  /** Ask the backend for this ordering (ascending; descending is the same with a leading "-"). */
  sortKey?: string;
  /** Which way a first press sorts. Dates read newest first, so they default to descending there. */
  sortFirst?: "asc" | "desc";
}>;

type Direction = "asc" | "desc";
type SortState = Readonly<{ key: string; dir: Direction }> | null;
type Density = "comfortable" | "compact";
type Prefs = Readonly<{ hidden: readonly string[]; density: Density }>;

const COLLATOR = new Intl.Collator("ar", { numeric: true, sensitivity: "base" });
const STORAGE_PREFIX = "daliini.table.";

function readPrefs(id: string | undefined): Prefs | null {
  if (!id) return null;
  try {
    const raw = window.localStorage.getItem(STORAGE_PREFIX + id);
    if (!raw) return null;
    const parsed = JSON.parse(raw) as Partial<Prefs>;
    return {
      hidden: Array.isArray(parsed.hidden) ? parsed.hidden.filter((v) => typeof v === "string") : [],
      density: parsed.density === "compact" ? "compact" : "comfortable",
    };
  } catch {
    return null;
  }
}

function writePrefs(id: string | undefined, prefs: Prefs): void {
  if (!id) return;
  try {
    window.localStorage.setItem(STORAGE_PREFIX + id, JSON.stringify(prefs));
  } catch {
    /* Without storage the choice lasts until the page is left, which is still what was asked. */
  }
}

type CellValue = string | number | null | undefined;

function isEmpty(value: CellValue): boolean {
  return value === null || value === undefined || value === "";
}

/** Compare two cell values: numbers as numbers, text as Arabic text, empty values last. */
export function compareValues(a: CellValue, b: CellValue): number {
  const aEmpty = isEmpty(a);
  const bEmpty = isEmpty(b);
  if (aEmpty || bEmpty) return aEmpty === bEmpty ? 0 : aEmpty ? 1 : -1;
  if (typeof a === "number" && typeof b === "number") return a - b;
  return COLLATOR.compare(String(a), String(b));
}

/** The server ordering as a header state: "-updatedAt" is updatedAt, descending. */
function fromOrdering(ordering: string | undefined): SortState {
  if (!ordering) return null;
  return ordering.startsWith("-")
    ? { key: ordering.slice(1), dir: "desc" }
    : { key: ordering, dir: "asc" };
}

function nextState(current: SortState, key: string, first: Direction): SortState {
  if (!current || current.key !== key) return { key, dir: first };
  if (current.dir === first) return { key, dir: first === "asc" ? "desc" : "asc" };
  return null;
}

export function DataTable<T>({
  columns,
  rows,
  rowKey,
  caption,
  empty,
  id,
  sort,
  onSort,
  summary,
}: {
  columns: readonly Column<T>[];
  rows: readonly T[];
  rowKey: (row: T) => string;
  caption: string;
  empty?: ReactNode;
  /** Keeps the operator's columns and density for this table, in this browser. */
  id?: string;
  /** The backend ordering in force, as the list's filters hold it. */
  sort?: string;
  /** Ask for another backend ordering; an empty string returns to the list's default. */
  onSort?: (ordering: string) => void;
  summary?: ReactNode;
}) {
  const [prefs, setPrefs] = useState<Prefs>(
    () =>
      readPrefs(id) ?? {
        hidden: columns.filter((c) => c.hiddenByDefault).map((c) => c.key),
        density: "comfortable",
      },
  );
  const [localSort, setLocalSort] = useState<SortState>(null);

  if (rows.length === 0) {
    return <>{empty ?? <EmptyState title="لا توجد نتائج" />}</>;
  }

  function update(next: Prefs): void {
    setPrefs(next);
    writePrefs(id, next);
  }

  const visible = columns.filter((c) => c.required || !prefs.hidden.includes(c.key));
  const serverSort = fromOrdering(sort);
  const sorted = (() => {
    if (!localSort) return rows;
    const column = columns.find((c) => c.key === localSort.key);
    if (!column?.sortValue) return rows;
    const value = column.sortValue;
    const factor = localSort.dir === "asc" ? 1 : -1;
    return [...rows].sort((a, b) => {
      const [x, y] = [value(a), value(b)];
      // Empty values stay at the end whichever way the column is read.
      if (isEmpty(x) || isEmpty(y)) return compareValues(x, y);
      return factor * compareValues(x, y);
    });
  })();

  function stateFor(column: Column<T>): Direction | null {
    if (column.sortKey && onSort) {
      return serverSort?.key === column.sortKey ? serverSort.dir : null;
    }
    if (column.sortValue) return localSort?.key === column.key ? localSort.dir : null;
    return null;
  }

  function press(column: Column<T>): void {
    const first = column.sortFirst ?? "asc";
    if (column.sortKey && onSort) {
      const next = nextState(serverSort, column.sortKey, first);
      onSort(next ? `${next.dir === "desc" ? "-" : ""}${next.key}` : "");
      return;
    }
    setLocalSort(nextState(localSort, column.key, first));
  }

  const hideable = columns.filter((c) => !c.required);

  return (
    <div className="table-block">
      {summary || id ? (
        <div className="table-toolbar">
          <span className="table-summary muted">{summary}</span>
          {id ? (
            <div className="table-tools">
              <button
                type="button"
                className="button-ghost"
                aria-pressed={prefs.density === "compact"}
                data-testid="table-density"
                onClick={() =>
                  update({
                    ...prefs,
                    density: prefs.density === "compact" ? "comfortable" : "compact",
                  })
                }
              >
                <Icons.menu />
                {prefs.density === "compact" ? "عرض مريح" : "عرض مضغوط"}
              </button>
              {hideable.length > 0 ? (
                <details className="table-menu">
                  <summary className="button-ghost" data-testid="table-columns">
                    <Icons.layers />
                    الأعمدة
                  </summary>
                  <div className="table-menu-body" role="group" aria-label="الأعمدة الظاهرة">
                    {hideable.map((column) => (
                      <label key={column.key} className="table-menu-item">
                        <input
                          type="checkbox"
                          checked={!prefs.hidden.includes(column.key)}
                          data-testid={`column-${column.key}`}
                          onChange={(event) =>
                            update({
                              ...prefs,
                              hidden: event.target.checked
                                ? prefs.hidden.filter((key) => key !== column.key)
                                : [...prefs.hidden, column.key],
                            })
                          }
                        />
                        <span>
                          {column.label ?? (typeof column.header === "string" ? column.header : column.key)}
                        </span>
                      </label>
                    ))}
                  </div>
                </details>
              ) : null}
            </div>
          ) : null}
        </div>
      ) : null}
      <div className="table-wrap">
        <table className="data-table" data-testid="data-table" data-density={prefs.density}>
          <caption className="sr-only">{caption}</caption>
          <thead>
            <tr>
              {visible.map((column) => {
                const sortable = Boolean((column.sortKey && onSort) || column.sortValue);
                const state = stateFor(column);
                return (
                  <th
                    key={column.key}
                    scope="col"
                    style={column.width ? { width: column.width } : undefined}
                    aria-sort={
                      sortable
                        ? state === "asc"
                          ? "ascending"
                          : state === "desc"
                            ? "descending"
                            : "none"
                        : undefined
                    }
                  >
                    {sortable ? (
                      <button
                        type="button"
                        className="th-sort"
                        data-testid={`sort-${column.key}`}
                        onClick={() => press(column)}
                      >
                        {column.header}
                        <span className="th-sort-mark" data-state={state ?? "none"} aria-hidden="true">
                          {state === "asc" ? "▲" : state === "desc" ? "▼" : "↕"}
                        </span>
                      </button>
                    ) : (
                      column.header
                    )}
                  </th>
                );
              })}
            </tr>
          </thead>
          <tbody>
            {sorted.map((row) => (
              <tr key={rowKey(row)}>
                {visible.map((column) => (
                  <td key={column.key} className={column.ltr ? "cell-ltr" : undefined}>
                    {column.render(row)}
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}


const COUNT = new Intl.NumberFormat("ar-SY");

/**
 * The line above a paged table. A cursor list has no total, so it says what this page holds
 * and whether more follow, rather than inventing a count it cannot know.
 */
export function pageSummary(count: number, hasMore: boolean): string {
  return hasMore
    ? `النتائج في هذه الصفحة: ${COUNT.format(count)}، وبعدها المزيد`
    : `النتائج: ${COUNT.format(count)}`;
}
