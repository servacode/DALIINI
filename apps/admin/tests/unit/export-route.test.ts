import { ResponseError } from "@servacode/api-typescript";
import { beforeEach, describe, expect, it, vi } from "vitest";

import type { AdminApis } from "../../lib/api/client";
import { exportFileName, exportQuery, isExportName } from "../../lib/api/exports";

/**
 * The CSV export route: three names, their own filters, and a body streamed through.
 *
 * The allow-list is the security property. The route is a GET that carries the operator's
 * session to the backend, so anything it forwards is something a crafted link could ask
 * for; it must only ever reach the three exports, with only their declared filters.
 */

const calls: { method: string; query: unknown }[] = [];
let upstream: () => Promise<{ raw: Response }>;

function fakeApis(): AdminApis {
  const record =
    (method: string) =>
    async (query: unknown): Promise<{ raw: Response }> => {
      calls.push({ method, query });
      return upstream();
    };
  return {
    exports: {
      adminExportFacilitiesCsvRaw: record("facilities"),
      adminExportReportsCsvRaw: record("reports"),
      adminExportAuditCsvRaw: record("audit"),
    },
  } as unknown as AdminApis;
}

const callWithSession = vi.fn(async (run: (apis: AdminApis) => Promise<unknown>) => {
  try {
    return { ok: true as const, data: await run(fakeApis()) };
  } catch (error) {
    if (error instanceof ResponseError) {
      return {
        ok: false as const,
        status: error.response.status,
        body: (await error.response.json()) as Record<string, unknown>,
      };
    }
    throw error;
  }
});

vi.mock("../../lib/auth/session", () => ({ callWithSession }));

const { GET } = await import("../../app/api/admin/export/[name]/route");

function get(path: string): Promise<Response> {
  const url = new URL(path, "https://admin.example.com");
  const name = url.pathname.split("/").pop()!;
  return GET(new Request(url), { params: Promise.resolve({ name }) });
}

beforeEach(() => {
  calls.length = 0;
  callWithSession.mockClear();
  upstream = async () => ({
    raw: new Response("﻿id,nameAr\n1,صيدلية الشفاء\n", {
      headers: {
        "Content-Type": "text/csv; charset=utf-8",
        "Content-Disposition": 'attachment; filename="../../etc/passwd"',
      },
    }),
  });
});

describe("the export allow-list", () => {
  it("knows exactly three exports", () => {
    expect(["facilities", "reports", "audit"].every(isExportName)).toBe(true);
    for (const name of ["users", "__proto__", "constructor", "facilities.csv", "", "AUDIT"]) {
      expect(isExportName(name)).toBe(false);
    }
  });

  it("forwards only an export's declared filters, trimmed, and only when filled", () => {
    const search = new URLSearchParams({
      action: "  facility.suspended ",
      from: "2026-09-01",
      to: "",
      format: "json",
      cursor: "x",
    });

    expect(exportQuery("audit", search)).toEqual({ action: "facility.suspended", from: "2026-09-01" });
    expect(exportQuery("reports", new URLSearchParams({ status: "OPEN", q: "x" }))).toEqual({
      status: "OPEN",
    });
  });

  it("names the file itself, from the export and the Damascus clock", () => {
    expect(exportFileName("audit", new Date("2026-09-28T21:05:00Z"))).toBe(
      "daliini-audit-20260929-0005.csv",
    );
  });
});

describe("GET /api/admin/export/[name]", () => {
  it("refuses an unknown export before any upstream call", async () => {
    const response = await get("/api/admin/export/users");

    expect(response.status).toBe(404);
    expect(((await response.json()) as { code: string }).code).toBe("NOT_FOUND");
    expect(callWithSession).not.toHaveBeenCalled();
  });

  it("streams the CSV as a private, uncached attachment with a name it chose", async () => {
    const response = await get("/api/admin/export/facilities?province=p-1&q=%D8%B5&secret=1");

    expect(response.status).toBe(200);
    expect(calls).toEqual([{ method: "facilities", query: { province: "p-1", q: "ص" } }]);
    expect(response.headers.get("content-type")).toBe("text/csv; charset=utf-8");
    expect(response.headers.get("cache-control")).toBe("no-store, private");
    expect(response.headers.get("x-content-type-options")).toBe("nosniff");
    const disposition = response.headers.get("content-disposition") ?? "";
    expect(disposition).toMatch(/^attachment; filename="daliini-facilities-\d{8}-\d{4}\.csv"$/);
    expect(disposition).not.toContain("passwd");
    expect(await response.text()).toContain("صيدلية الشفاء");
  });

  it("passes a refusal on as the JSON envelope, not as a file", async () => {
    upstream = async () => {
      throw new ResponseError(
        new Response(
          JSON.stringify({
            code: "PERMISSION_DENIED",
            message: "",
            details: {},
            requestId: "req-7",
          }),
          { status: 403, headers: { "Content-Type": "application/json" } },
        ),
      );
    };

    const response = await get("/api/admin/export/audit?action=x");

    expect(response.status).toBe(403);
    expect(response.headers.get("content-disposition")).toBeNull();
    expect(await response.json()).toMatchObject({ code: "PERMISSION_DENIED", requestId: "req-7" });
  });
});
