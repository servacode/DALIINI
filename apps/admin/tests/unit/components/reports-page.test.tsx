import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import ReportsPage from "../../../app/(console)/reports/page";

/**
 * «البلاغات», and deciding many at once.
 *
 * The single-row path was already reachable; this covers the batch, which exists in the
 * backend and had no way in. What is worth pinning is not that the button works but what it
 * refuses to do: only open reports can be ticked, nothing is offered to an operator who may
 * not decide, and the toast reports what the backend actually did rather than what was asked
 * — some reports are closed by somebody else between the list and the button.
 */

let canManage = true;

vi.mock("next/navigation", () => ({
  useRouter: () => ({ replace: vi.fn(), refresh: vi.fn() }),
  usePathname: () => "/reports",
  useSearchParams: () => new URLSearchParams(),
}));

vi.mock("../../../components/admin-shell", () => ({
  useCan: () => canManage,
}));

const REPORTS = {
  items: [
    {
      id: "r-1",
      facilityId: "f-1",
      facilityNameAr: "صيدلية الشفاء",
      reporterId: null,
      reason: "WRONG_HOURS",
      note: "مغلقة",
      status: "OPEN",
      createdAt: "2026-10-01T09:00:00Z",
      resolvedById: null,
      resolvedAt: null,
    },
    {
      id: "r-2",
      facilityId: "f-2",
      facilityNameAr: "صيدلية النور",
      reporterId: null,
      reason: "WRONG_PHONE",
      note: "",
      status: "OPEN",
      createdAt: "2026-10-01T10:00:00Z",
      resolvedById: null,
      resolvedAt: null,
    },
    {
      id: "r-3",
      facilityId: "f-3",
      facilityNameAr: "صيدلية الأمل",
      reporterId: null,
      reason: "WRONG_HOURS",
      note: "",
      status: "RESOLVED",
      createdAt: "2026-09-30T10:00:00Z",
      resolvedById: "u-1",
      resolvedAt: "2026-09-30T12:00:00Z",
    },
  ],
};

type Handler = (url: URL, init?: RequestInit) => { status?: number; body: unknown };

function stubBff(handler: Handler) {
  const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = new URL(String(input), "http://console.test");
    const { status = 200, body } = handler(url, init);
    return new Response(JSON.stringify(body), {
      status,
      headers: { "Content-Type": "application/json" },
    });
  });
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

function bff(bulk: { status?: number; body: unknown } = { body: { decided: 2, results: [] } }): Handler {
  return (url) => {
    const name = url.pathname.replace("/api/admin/", "");
    if (name === "reports") return { body: REPORTS };
    if (name === "reportsBulkDecide") return bulk;
    return { body: {} };
  };
}

beforeEach(() => {
  canManage = true;
});

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
});

describe("deciding many reports at once", () => {
  it("offers a tick only on reports that are still open", async () => {
    stubBff(bff());
    render(<ReportsPage />);

    await screen.findByText("صيدلية الشفاء");

    expect(screen.getByTestId("select-r-1")).toBeTruthy();
    expect(screen.getByTestId("select-r-2")).toBeTruthy();
    // Already resolved: there is nothing to decide, so there is nothing to tick.
    expect(screen.queryByTestId("select-r-3")).toBeNull();
  });

  it("says nothing until something is ticked, then says how many", async () => {
    stubBff(bff());
    render(<ReportsPage />);
    await screen.findByText("صيدلية الشفاء");

    expect(screen.queryByTestId("bulk-bar")).toBeNull();

    fireEvent.click(screen.getByTestId("select-r-1"));

    const bar = await screen.findByTestId("bulk-bar");
    expect(bar.textContent).toContain("1");
  });

  it("selects every open report, and only those", async () => {
    stubBff(bff());
    render(<ReportsPage />);
    await screen.findByText("صيدلية الشفاء");

    fireEvent.click(screen.getByTestId("select-all"));

    const bar = await screen.findByTestId("bulk-bar");
    expect(bar.textContent).toContain("2");
  });

  it("sends the ticked ids, the action and the note in one call", async () => {
    const fetchMock = stubBff(bff());
    render(<ReportsPage />);
    await screen.findByText("صيدلية الشفاء");

    fireEvent.click(screen.getByTestId("select-all"));
    fireEvent.click(screen.getByTestId("bulk-resolve"));
    fireEvent.change(screen.getByTestId("bulk-note"), { target: { value: "صُحّحت المواعيد" } });
    fireEvent.click(screen.getByText("تأكيد المعالجة"));

    await waitFor(() => {
      const call = fetchMock.mock.calls.find(([input]) =>
        String(input).endsWith("/reportsBulkDecide"),
      );
      expect(call).toBeTruthy();
      const body = JSON.parse(String((call![1] as RequestInit).body));
      expect(body.ids).toEqual(["r-1", "r-2"]);
      expect(body.action).toBe("resolve");
      expect(body.note).toBe("صُحّحت المواعيد");
    });
  });

  it("reports what the backend decided, not what was asked", async () => {
    // Two were ticked; one had been closed by somebody else in the meantime.
    stubBff(bff({ body: { decided: 1, results: [] } }));
    render(<ReportsPage />);
    await screen.findByText("صيدلية الشفاء");

    fireEvent.click(screen.getByTestId("select-all"));
    fireEvent.click(screen.getByTestId("bulk-dismiss"));
    fireEvent.click(screen.getByText("تأكيد الرفض"));

    const toast = await screen.findByText(/لم يعد مفتوحًا/);
    expect(toast.textContent).toContain("1");
  });

  it("offers nothing at all to an operator who may not decide", async () => {
    canManage = false;
    stubBff(bff());
    render(<ReportsPage />);

    await screen.findByText("صيدلية الشفاء");

    expect(screen.queryByTestId("select-all")).toBeNull();
    expect(screen.queryByTestId("select-r-1")).toBeNull();
    expect(screen.queryByTestId("bulk-bar")).toBeNull();
  });
});
