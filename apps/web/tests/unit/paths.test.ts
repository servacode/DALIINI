import { describe, expect, it } from "vitest";
import { categoryPath, decodedSegment, facilityPath } from "../../lib/paths";

const ID = "00abd477-222f-4dd2-9c9c-c5520c05ed18";

describe("facilityPath", () => {
  it("puts the name's words after the id, percent-encoded", () => {
    expect(facilityPath({ id: ID, slug: "صيدلية-الأمل" })).toBe(
      `/f/${ID}/${encodeURIComponent("صيدلية-الأمل")}`,
    );
  });

  it("is the id alone when a name left no words", () => {
    expect(facilityPath({ id: ID, slug: "" })).toBe(`/f/${ID}`);
    expect(facilityPath({ id: ID })).toBe(`/f/${ID}`);
  });
});

describe("categoryPath", () => {
  it("addresses a category by its slug within the province", () => {
    expect(categoryPath("raqqa", { id: ID, slug: "pharmacy" })).toBe("/raqqa/pharmacy");
  });

  it("falls back to the id for a category the API sent without one", () => {
    expect(categoryPath("raqqa", { id: ID })).toBe(`/raqqa/${ID}`);
  });
});

describe("decodedSegment", () => {
  it("decodes what arrives encoded and leaves the rest alone", () => {
    expect(decodedSegment(encodeURIComponent("صيدلية-الأمل"))).toBe("صيدلية-الأمل");
    expect(decodedSegment("pharmacy")).toBe("pharmacy");
    expect(decodedSegment("100%")).toBe("100%");
  });
});
