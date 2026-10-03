import { describe, expect, it } from "vitest";

import { PERMISSION_AREAS, areasFor, permissionLabel } from "../../lib/client/permissions";

describe("permission labels", () => {
  it("names every code once", () => {
    const codes = PERMISSION_AREAS.flatMap((area) => area.permissions.map((p) => p.code));
    expect(new Set(codes).size).toBe(codes.length);
  });

  it("keeps only what the backend offers, and shows an unknown code under «أخرى»", () => {
    const areas = areasFor([
      { code: "admin.roles.manage", description: "Assign Admin roles" },
      { code: "admin.future.read", description: "Something new" },
    ]);

    expect(areas.map((area) => area.key)).toEqual(["people", "other"]);
    expect(areas[0]?.permissions.map((p) => p.code)).toEqual(["admin.roles.manage"]);
    expect(areas[1]?.permissions).toEqual([{ code: "admin.future.read", label: "Something new" }]);
  });

  it("falls back to the code when there is no description either", () => {
    expect(permissionLabel("admin.future.read", "  ")).toBe("admin.future.read");
    expect(permissionLabel("admin.roles.manage")).toBe("منح الأدوار وتعديلها");
  });
});
