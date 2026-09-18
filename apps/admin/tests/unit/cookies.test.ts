import { afterEach, describe, expect, it } from "vitest";

import { cookieName, secureCookies } from "../../lib/auth/cookies";

/**
 * INT-012: the `__Host-` prefix and the `Secure` attribute are one decision.
 *
 * A browser rejects a `__Host-` cookie that arrives without `Secure`, so a build that pairs
 * the prefix with an insecure cookie silently has no session at all. That is what happened
 * before, and it happened because the decision was keyed on `NODE_ENV`.
 */

const original = process.env.ADMIN_PUBLIC_ORIGIN;

afterEach(() => {
  process.env.ADMIN_PUBLIC_ORIGIN = original;
});

describe("cookie naming", () => {
  it("uses the __Host- prefix and Secure on an https origin", () => {
    process.env.ADMIN_PUBLIC_ORIGIN = "https://admin.example.com";

    expect(secureCookies()).toBe(true);
    expect(cookieName("directory_admin_refresh")).toBe("__Host-directory_admin_refresh");
  });

  it("drops the prefix on a loopback http origin, because it cannot be Secure", () => {
    process.env.ADMIN_PUBLIC_ORIGIN = "http://localhost:3000";

    expect(secureCookies()).toBe(false);
    expect(cookieName("directory_admin_refresh")).toBe("directory_admin_refresh");
  });

  it.each(["http://127.0.0.1:3000", "http://[::1]:3000"])(
    "treats %s as loopback too",
    (origin) => {
      process.env.ADMIN_PUBLIC_ORIGIN = origin;

      expect(secureCookies()).toBe(false);
    },
  );

  it("refuses an http origin on a real host rather than downgrading", () => {
    process.env.ADMIN_PUBLIC_ORIGIN = "http://admin.example.com";

    expect(() => secureCookies()).toThrow(/must use https outside loopback/);
  });

  it("refuses to run without a declared public origin", () => {
    delete process.env.ADMIN_PUBLIC_ORIGIN;

    expect(() => secureCookies()).toThrow(/ADMIN_PUBLIC_ORIGIN is required/);
  });

  it("does not consult NODE_ENV, which was the original defect", () => {
    process.env.ADMIN_PUBLIC_ORIGIN = "http://localhost:3000";
    const previous = process.env.NODE_ENV;

    // A production build served on loopback http is a real configuration: it is how the
    // qualification run works. It must still get usable cookies.
    expect(secureCookies()).toBe(false);
    expect(process.env.NODE_ENV).toBe(previous);
  });
});
