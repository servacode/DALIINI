import { cleanup, fireEvent, render, screen, within } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { type Column, DataTable, compareValues, pageSummary } from "../../../components/ui";

/**
 * The console's table: header sorting (in hand or by the backend), the column chooser and the
 * density, which are kept per table in this browser.
 */

type Row = Readonly<{ id: string; name: string; count: number | null }>;

const ROWS: readonly Row[] = [
  { id: "1", name: "باسل", count: 3 },
  { id: "2", name: "أحمد", count: null },
  { id: "3", name: "تامر", count: 10 },
];

const COLUMNS: readonly Column<Row>[] = [
  { key: "name", header: "الاسم", required: true, sortValue: (row) => row.name, render: (row) => row.name },
  { key: "count", header: "العدد", sortValue: (row) => row.count, sortFirst: "desc", render: (row) => row.count ?? "—" },
];

function names(): string[] {
  const table = screen.getByTestId("data-table");
  return within(table)
    .getAllByRole("row")
    .slice(1)
    .map((row) => row.querySelector("td")?.textContent ?? "");
}

beforeEach(() => window.localStorage.clear());
afterEach(cleanup);

describe("DataTable sorting", () => {
  it("sorts the rows in hand from a header, and a third press restores the list's order", () => {
    render(<DataTable caption="t" columns={COLUMNS} rows={ROWS} rowKey={(row) => row.id} />);
    const header = screen.getByTestId("sort-name");

    fireEvent.click(header);
    expect(names()).toEqual(["أحمد", "باسل", "تامر"]);
    expect(header.closest("th")?.getAttribute("aria-sort")).toBe("ascending");

    fireEvent.click(header);
    expect(names()).toEqual(["تامر", "باسل", "أحمد"]);
    expect(header.closest("th")?.getAttribute("aria-sort")).toBe("descending");

    fireEvent.click(header);
    expect(names()).toEqual(["باسل", "أحمد", "تامر"]);
    expect(header.closest("th")?.getAttribute("aria-sort")).toBe("none");
  });

  it("starts where the column says, and keeps empty values last", () => {
    render(<DataTable caption="t" columns={COLUMNS} rows={ROWS} rowKey={(row) => row.id} />);
    fireEvent.click(screen.getByTestId("sort-count"));
    expect(names()).toEqual(["تامر", "باسل", "أحمد"]);
  });

  it("asks the backend for an ordering instead of sorting a page by itself", () => {
    const onSort = vi.fn();
    const columns: readonly Column<Row>[] = [
      { key: "name", header: "الاسم", sortKey: "name", render: (row) => row.name },
      { key: "created", header: "أُنشئ", sortKey: "createdAt", sortFirst: "desc", render: () => "" },
    ];
    const { rerender } = render(
      <DataTable caption="t" columns={columns} rows={ROWS} rowKey={(row) => row.id} sort="" onSort={onSort} />,
    );

    fireEvent.click(screen.getByTestId("sort-created"));
    expect(onSort).toHaveBeenLastCalledWith("-createdAt");
    // The rows are left in the order the backend sent them.
    expect(names()).toEqual(["باسل", "أحمد", "تامر"]);

    rerender(
      <DataTable caption="t" columns={columns} rows={ROWS} rowKey={(row) => row.id} sort="-createdAt" onSort={onSort} />,
    );
    expect(screen.getByTestId("sort-created").closest("th")?.getAttribute("aria-sort")).toBe("descending");
    fireEvent.click(screen.getByTestId("sort-created"));
    expect(onSort).toHaveBeenLastCalledWith("createdAt");

    rerender(
      <DataTable caption="t" columns={columns} rows={ROWS} rowKey={(row) => row.id} sort="createdAt" onSort={onSort} />,
    );
    fireEvent.click(screen.getByTestId("sort-created"));
    expect(onSort).toHaveBeenLastCalledWith("");
  });
});

describe("DataTable columns and density", () => {
  it("hides a column on request, never a required one, and remembers the choice", () => {
    const { unmount } = render(
      <DataTable id="people" caption="t" columns={COLUMNS} rows={ROWS} rowKey={(row) => row.id} />,
    );
    expect(screen.queryByTestId("column-name")).toBeNull();

    fireEvent.click(screen.getByTestId("column-count"));
    expect(screen.queryByTestId("sort-count")).toBeNull();
    fireEvent.click(screen.getByTestId("table-density"));
    expect(screen.getByTestId("data-table").getAttribute("data-density")).toBe("compact");
    unmount();

    render(<DataTable id="people" caption="t" columns={COLUMNS} rows={ROWS} rowKey={(row) => row.id} />);
    expect(screen.queryByTestId("sort-count")).toBeNull();
    expect(screen.getByTestId("data-table").getAttribute("data-density")).toBe("compact");
  });

  it("starts with a column hidden when the screen asks, until the operator shows it", () => {
    const columns = [COLUMNS[0]!, { ...COLUMNS[1]!, hiddenByDefault: true }];
    render(<DataTable id="quiet" caption="t" columns={columns} rows={ROWS} rowKey={(row) => row.id} />);
    expect(screen.queryByTestId("sort-count")).toBeNull();
    fireEvent.click(screen.getByTestId("column-count"));
    expect(screen.getByTestId("sort-count")).toBeTruthy();
  });

  it("works without storage", () => {
    vi.spyOn(Storage.prototype, "getItem").mockImplementation(() => {
      throw new Error("blocked");
    });
    vi.spyOn(Storage.prototype, "setItem").mockImplementation(() => {
      throw new Error("blocked");
    });
    render(<DataTable id="x" caption="t" columns={COLUMNS} rows={ROWS} rowKey={(row) => row.id} />);
    fireEvent.click(screen.getByTestId("table-density"));
    expect(screen.getByTestId("data-table").getAttribute("data-density")).toBe("compact");
    vi.restoreAllMocks();
  });
});

describe("helpers", () => {
  it("compares numbers as numbers and Arabic as Arabic, empty last", () => {
    expect(compareValues(2, 10)).toBeLessThan(0);
    expect(compareValues("أحمد", "باسل")).toBeLessThan(0);
    expect(compareValues(null, 1)).toBeGreaterThan(0);
    expect(compareValues("", "")).toBe(0);
  });

  it("says what a page holds without inventing a total", () => {
    expect(pageSummary(50, true)).toBe("النتائج في هذه الصفحة: 50، وبعدها المزيد");
    expect(pageSummary(12, false)).toBe("النتائج: 12");
  });
});
