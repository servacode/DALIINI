import { cleanup, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it } from "vitest";

import { BarChart, LineChart, niceTicks } from "../../../components/charts";

afterEach(cleanup);

describe("niceTicks", () => {
  it("rounds the axis up to a readable step", () => {
    expect(niceTicks(0)).toEqual([0, 1]);
    expect(niceTicks(7)).toEqual([0, 2.5, 5, 7.5]);
    expect(niceTicks(120)).toEqual([0, 50, 100, 150]);
    expect(niceTicks(1)).toEqual([0, 0.5, 1]);
  });
});

describe("LineChart", () => {
  it("carries every value as a table and each series' total in the legend", () => {
    render(
      <LineChart
        caption="الاستخدام"
        // One day as the generated client delivers it, as a midnight timestamp.
        days={["2026-09-01", "2026-09-02T00:00:00.000Z"]}
        series={[{ key: "s", label: "بحث", color: "red", values: [3, 4] }]}
      />,
    );
    const rows = screen.getAllByRole("row");
    expect(rows).toHaveLength(3);
    expect(rows[2]?.textContent).toContain("٢ أيلول");
    expect(rows[2]?.textContent).toContain("4");
    expect(screen.getByText("٧")).toBeTruthy();
  });
});

describe("BarChart", () => {
  it("draws each bar as a link to its list when it has one", () => {
    render(
      <BarChart
        caption="الحالات"
        bars={[
          { key: "a", label: "فعّالة", value: 26, color: "green", href: "/facilities?status=ACTIVE" },
          { key: "b", label: "مسودة", value: 0, color: "grey" },
        ]}
      />,
    );
    expect(screen.getByRole("link").getAttribute("href")).toBe("/facilities?status=ACTIVE");
    expect(screen.getByText("٢٦")).toBeTruthy();
  });
});
