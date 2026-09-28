import { describe, expect, it, vi } from "vitest";

import type { AdminApis } from "../../lib/api/client";
import { READS, WRITES, isReadOperation, isWriteOperation } from "../../lib/api/operations";

/**
 * The operation registry is the whole transport surface.
 *
 * What matters is that it reaches the generated client and nothing else: no URL is built
 * here, no query string is assembled by hand, and a name that is not in the registry cannot
 * be reached at all. A read cannot be invoked as a write, which is what keeps a mutation
 * out of reach of a link or a prefetch.
 */

function spyApis(): { apis: AdminApis; calls: { name: string; args: unknown[] }[] } {
  const calls: { name: string; args: unknown[] }[] = [];
  const group = (prefix: string): Record<string, unknown> =>
    new Proxy(
      {},
      {
        get: (_target, method: string) =>
          vi.fn(async (...args: unknown[]) => {
            calls.push({ name: `${prefix}.${method}`, args });
            return { ok: true };
          }),
      },
    );
  const apis = new Proxy({}, { get: (_t, key: string) => group(key) }) as unknown as AdminApis;
  return { apis, calls };
}

describe("registry membership", () => {
  it("separates reads from writes with no overlap", () => {
    const reads = new Set(Object.keys(READS));
    const writes = new Set(Object.keys(WRITES));

    expect([...reads].filter((name) => writes.has(name))).toEqual([]);
  });

  it("refuses a name that is not registered", () => {
    expect(isReadOperation("dashboard")).toBe(true);
    expect(isReadOperation("dropEverything")).toBe(false);
    expect(isWriteOperation("__proto__")).toBe(false);
    expect(isWriteOperation("constructor")).toBe(false);
  });

  it("does not expose any mutation as a read", () => {
    for (const name of ["reviewApprove", "facilityClose", "userBlock", "adDelete"]) {
      expect(isReadOperation(name)).toBe(false);
      expect(isWriteOperation(name)).toBe(true);
    }
  });
});

describe("filter serialisation", () => {
  it("sends only the filters that carry a value", async () => {
    const { apis, calls } = spyApis();

    await READS.facilities(apis, { q: "صيدلية", status: "", province: undefined, category: "  " });

    expect(calls[0]?.args[0]).toEqual({ q: "صيدلية" });
  });

  it("trims a value rather than sending the operator's stray spaces", async () => {
    const { apis, calls } = spyApis();

    await READS.users(apis, { q: "  أحمد  ", status: "active" });

    expect(calls[0]?.args[0]).toEqual({ q: "أحمد", status: "active" });
  });

  it("sends an empty object when nothing is filtered, not a bag of blanks", async () => {
    const { apis, calls } = spyApis();

    await READS.audit(apis, { action: "", actor: "", resource: "", requestId: "" });

    expect(calls[0]?.args[0]).toEqual({});
  });

  it("ignores a parameter the operation does not declare", async () => {
    const { apis, calls } = spyApis();

    await READS.users(apis, { q: "x", province: "should-not-travel" });

    expect(calls[0]?.args[0]).toEqual({ q: "x" });
  });
});

describe("dispatch", () => {
  it("routes each read to its generated operation", async () => {
    const { apis, calls } = spyApis();

    await READS.me(apis);
    await READS.dashboard(apis);
    await READS.provinces(apis);

    expect(calls.map((call) => call.name)).toEqual([
      "system.adminMeRetrieve",
      "system.adminDashboardRetrieve",
      "provinces.adminProvincesList",
    ]);
  });

  it("routes each write to its generated operation with the id in place", async () => {
    const { apis, calls } = spyApis();

    await WRITES.reviewApprove(apis, { id: "app-1", reason: "ok" });
    await WRITES.provinceUpdate(apis, { id: "prov-1", active: true });

    expect(calls[0]?.name).toBe("reviews.adminReviewApprove");
    expect(calls[0]?.args[0]).toMatchObject({ applicationId: "app-1" });
    expect(calls[1]?.name).toBe("provinces.adminProvinceUpdate");
    expect(calls[1]?.args[0]).toMatchObject({ provinceId: "prov-1" });
  });

  it("coerces the verification requirement id to a number, as its key is an integer", async () => {
    const { apis, calls } = spyApis();

    await WRITES.verificationUpdate(apis, { id: "7", active: false });

    expect(calls[0]?.args[0]).toMatchObject({ requirementId: 7 });
  });
});

describe("problem reports, cities and the new filters", () => {
  it("sends the audit date range and the user role filter", async () => {
    const { apis, calls } = spyApis();

    await READS.audit(apis, { from: "2026-09-01", to: "2026-09-28", action: "" });
    await READS.users(apis, { role: "none" });

    expect(calls[0]?.args[0]).toEqual({ from: "2026-09-01", to: "2026-09-28" });
    expect(calls[1]?.args[0]).toEqual({ role: "none" });
  });

  it("routes report decisions with the note and report id", async () => {
    const { apis, calls } = spyApis();

    await WRITES.reportResolve(apis, { id: "r-1", note: "صُحّحت الساعات" });
    await WRITES.reportDismiss(apis, { id: "r-2" });

    expect(calls[0]?.name).toBe("reports.adminReportResolve");
    expect(calls[0]?.args[0]).toEqual({
      reportId: "r-1",
      adminReportDecisionRequest: { note: "صُحّحت الساعات" },
    });
    expect(calls[1]?.name).toBe("reports.adminReportDismiss");
    expect(calls[1]?.args[0]).toEqual({ reportId: "r-2", adminReportDecisionRequest: { note: "" } });
  });

  it("scopes a city toggle to its province and sends only the switch", async () => {
    const { apis, calls } = spyApis();

    await READS.provinceCities(apis, { id: "p-1" });
    await WRITES.cityUpdate(apis, { provinceId: "p-1", id: "c-1", active: true, name: "x" });

    expect(calls[0]).toMatchObject({
      name: "provinces.adminProvinceCitiesList",
      args: [{ provinceId: "p-1" }],
    });
    expect(calls[1]?.args[0]).toEqual({
      provinceId: "p-1",
      cityId: "c-1",
      adminCityUpdateRequest: { active: true },
    });
  });
});
