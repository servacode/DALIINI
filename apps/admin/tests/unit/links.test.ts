import { describe, expect, it } from "vitest";

import { hrefFor } from "../../lib/client/links";

describe("hrefFor", () => {
  it("opens a record by its id", () => {
    expect(hrefFor({ entityType: "FACILITY", entityId: "f-1" })).toBe("/facilities/f-1");
    expect(hrefFor({ entityType: "APPLICATION", entityId: "a-1" })).toBe("/reviews/a-1");
  });

  it("opens a list with the backend's filter query", () => {
    expect(hrefFor({ entityType: "FACILITY_LIST", query: "issue=STALE" })).toBe("/facilities?issue=STALE");
    expect(hrefFor({ entityType: "REPORT_LIST", query: "?status=OPEN" })).toBe("/reports?status=OPEN");
    expect(hrefFor({ entityType: "DUTY_ROSTER", query: null })).toBe("/duty");
  });

  it("returns nothing for an unknown destination rather than a broken link", () => {
    expect(hrefFor({ entityType: "SOMETHING_NEW" })).toBeNull();
    expect(hrefFor(null)).toBeNull();
  });
});
