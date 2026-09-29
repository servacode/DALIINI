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

describe("smart console reads", () => {
  it("routes the task centre, alerts, search and timeline to their generated operations", async () => {
    const { apis, calls } = spyApis();

    await READS.tasks(apis);
    await READS.alerts(apis);
    await READS.globalSearch(apis, { q: "  شفاء " });
    await READS.facilityTimeline(apis, { id: "f-1" });

    expect(calls.map((call) => call.name)).toEqual([
      "system.adminTasksRetrieve",
      "system.adminAlertsList",
      "system.adminSearchRetrieve",
      "facilities.adminFacilityTimelineRetrieve",
    ]);
    expect(calls[2]?.args[0]).toEqual({ q: "شفاء" });
    expect(calls[3]?.args[0]).toEqual({ facilityId: "f-1" });
  });

  it("sends the review queue's day range and document filter, and drops what is empty", async () => {
    const { apis, calls } = spyApis();

    await READS.reviews(apis, {
      status: "SUBMITTED",
      kind: "",
      from: "2026-09-01",
      to: "",
      evidence: "incomplete",
    });

    expect(calls[0]).toEqual({
      name: "reviews.adminReviewsList",
      args: [{ status: "SUBMITTED", from: "2026-09-01", evidence: "incomplete" }],
    });
  });

  it("sends the quality filters on the facility list", async () => {
    const { apis, calls } = spyApis();

    await READS.facilities(apis, { issue: "STALE", ordering: "qualityScore", status: "" });

    expect(calls[0]?.args[0]).toEqual({ issue: "STALE", ordering: "qualityScore" });
  });
});

// Operations screens

describe("operations screens: registration", () => {
  it("reaches every new screen's reads over GET and its writes over POST only", () => {
    for (const name of [
      "dutyRoster",
      "contentPages",
      "contentPage",
      "faqEntries",
      "emergencyNumbers",
      "contactMessages",
      "broadcasts",
      "rejectionTemplates",
      "provinceReadiness",
      "analyticsPeriod",
      "staffPerformance",
    ]) {
      expect(isReadOperation(name)).toBe(true);
      expect(isWriteOperation(name)).toBe(false);
    }
    for (const name of [
      "dutyShiftCreate",
      "dutyShiftUpdate",
      "dutyShiftDelete",
      "contentPageCreate",
      "contentPageUpdate",
      "contentPageDelete",
      "faqCreate",
      "faqUpdate",
      "faqDelete",
      "emergencyNumberCreate",
      "emergencyNumberUpdate",
      "emergencyNumberDelete",
      "contactMessageHandle",
      "broadcastSend",
      "rejectionTemplateCreate",
      "rejectionTemplateUpdate",
      "rejectionTemplateDelete",
    ]) {
      expect(isWriteOperation(name)).toBe(true);
      expect(isReadOperation(name)).toBe(false);
    }
  });
});

describe("operations screens: reads", () => {
  it("asks for a province's roster with only the filters that carry a value", async () => {
    const { apis, calls } = spyApis();

    await READS.dutyRoster(apis, {
      provinceId: "p-1",
      cityId: "",
      from: "2026-09-26",
      to: "2026-10-09",
      stray: "x",
    });

    expect(calls[0]).toEqual({
      name: "duty.adminDutyRosterRetrieve",
      args: [{ provinceId: "p-1", from: "2026-09-26", to: "2026-10-09" }],
    });
  });

  it("sends the analytics period to both analytics reads", async () => {
    const { apis, calls } = spyApis();

    await READS.analyticsPeriod(apis, { from: "2026-09-01", to: "2026-09-28", q: "x" });
    await READS.staffPerformance(apis, { from: "2026-09-01", to: "" });

    expect(calls[0]).toEqual({
      name: "analytics.adminAnalyticsRetrieve",
      args: [{ from: "2026-09-01", to: "2026-09-28" }],
    });
    expect(calls[1]).toEqual({
      name: "analytics.adminAnalyticsStaffRetrieve",
      args: [{ from: "2026-09-01" }],
    });
  });

  it("pages the inbox and the broadcast history by cursor, with their filters", async () => {
    const { apis, calls } = spyApis();

    await READS.contactMessages(apis, { status: "open", kind: "", cursor: "c-2", limit: "999" });
    await READS.broadcasts(apis, { cursor: "" });

    expect(calls[0]?.args[0]).toEqual({ status: "open", cursor: "c-2" });
    expect(calls[1]?.args[0]).toEqual({});
  });

  it("addresses a page by slug, a readiness check by province, and filters templates", async () => {
    const { apis, calls } = spyApis();

    await READS.contentPage(apis, { slug: "privacy" });
    await READS.provinceReadiness(apis, { id: "p-9" });
    await READS.rejectionTemplates(apis, { active: "true" });
    await READS.rejectionTemplates(apis, {});
    await READS.emergencyNumbers(apis, { provinceId: "national" });

    expect(calls.map((call) => [call.name, call.args[0]])).toEqual([
      ["content.adminContentPageRetrieve", { slug: "privacy" }],
      ["provinces.adminProvinceReadinessRetrieve", { provinceId: "p-9" }],
      ["reviews.adminRejectionTemplatesList", { active: true }],
      ["reviews.adminRejectionTemplatesList", {}],
      ["content.adminEmergencyNumbersList", { provinceId: "national" }],
    ]);
  });
});

describe("operations screens: writes", () => {
  it("turns a shift's ISO times into Dates, as the generated serialiser needs", async () => {
    const { apis, calls } = spyApis();

    await WRITES.dutyShiftCreate(apis, {
      facilityId: "f-1",
      startsAt: "2026-09-28T19:00:00.000Z",
      endsAt: "2026-09-29T05:00:00.000Z",
    });

    const request = (calls[0]?.args[0] as { adminDutyShiftCreateRequest: Record<string, unknown> })
      .adminDutyShiftCreateRequest;
    expect(calls[0]?.name).toBe("duty.adminDutyShiftCreate");
    expect(request.facilityId).toBe("f-1");
    expect(request.startsAt).toBeInstanceOf(Date);
    expect((request.endsAt as Date).toISOString()).toBe("2026-09-29T05:00:00.000Z");
  });

  it("moves a shift with its times only, and cancels it by id", async () => {
    const { apis, calls } = spyApis();

    await WRITES.dutyShiftUpdate(apis, {
      id: "s-1",
      startsAt: "2026-09-28T19:00:00.000Z",
      endsAt: "2026-09-29T05:00:00.000Z",
      facilityId: "must-not-travel",
    });
    await WRITES.dutyShiftDelete(apis, { id: "s-1" });

    const update = calls[0]?.args[0] as {
      shiftId: string;
      patchedAdminDutyShiftUpdateRequest: Record<string, unknown>;
    };
    expect(update.shiftId).toBe("s-1");
    expect(Object.keys(update.patchedAdminDutyShiftUpdateRequest).sort()).toEqual([
      "endsAt",
      "startsAt",
    ]);
    expect(calls[1]).toEqual({ name: "duty.adminDutyShiftDelete", args: [{ shiftId: "s-1" }] });
  });

  it("keeps partial updates partial: a key the screen did not send stays absent", async () => {
    const { apis, calls } = spyApis();

    await WRITES.faqUpdate(apis, { id: "q-1", published: false });
    await WRITES.contentPageUpdate(apis, { slug: "about", published: true });
    await WRITES.rejectionTemplateUpdate(apis, { id: "t-1", active: false });

    expect(calls[0]?.args[0]).toEqual({
      entryId: "q-1",
      adminFaqEntryRequest: { published: false },
    });
    expect(calls[1]?.args[0]).toEqual({
      slug: "about",
      adminContentPageUpdateRequest: { published: true },
    });
    expect(calls[2]?.args[0]).toEqual({
      templateId: "t-1",
      adminRejectionTemplateRequest: { active: false },
    });
  });

  it("keeps an explicit null province, which makes an emergency number national", async () => {
    const { apis, calls } = spyApis();

    await WRITES.emergencyNumberUpdate(apis, { id: "n-1", provinceId: null, adminNote: "" });

    expect(calls[0]?.args[0]).toEqual({
      numberId: "n-1",
      adminEmergencyNumberRequest: { provinceId: null, adminNote: "" },
    });
  });

  it("sends a broadcast's audience as given and an empty province as none", async () => {
    const { apis, calls } = spyApis();

    await WRITES.broadcastSend(apis, {
      titleAr: "عنوان",
      bodyAr: "نص",
      audience: "OWNERS",
      provinceId: "",
    });
    await WRITES.broadcastSend(apis, { titleAr: "ع", bodyAr: "ن", audience: "EVERYONE" });

    expect(calls[0]?.args[0]).toEqual({
      adminBroadcastRequest: {
        titleAr: "عنوان",
        bodyAr: "نص",
        audience: "OWNERS",
        provinceId: null,
      },
    });
    // An unknown audience is not widened to "everyone" here; the backend refuses it.
    expect(
      (calls[1]?.args[0] as { adminBroadcastRequest: { audience: string } }).adminBroadcastRequest
        .audience,
    ).toBe("EVERYONE");
  });

  it("records the handling note on a contact message", async () => {
    const { apis, calls } = spyApis();

    await WRITES.contactMessageHandle(apis, { id: "m-1", note: "اتُّصل بصاحب الرسالة" });
    await WRITES.contactMessageHandle(apis, { id: "m-2" });

    expect(calls[0]?.args[0]).toEqual({
      messageId: "m-1",
      adminContactHandleRequest: { note: "اتُّصل بصاحب الرسالة" },
    });
    expect(calls[1]?.args[0]).toEqual({
      messageId: "m-2",
      adminContactHandleRequest: { note: "" },
    });
  });
});
