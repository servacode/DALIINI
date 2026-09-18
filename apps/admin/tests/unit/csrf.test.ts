import { describe, expect, it } from "vitest";

import { checkSameOrigin } from "../../lib/http/csrf";

/**
 * The origin check is fail-closed on unsafe methods and absent on safe ones.
 *
 * The version this replaced compared `Origin` to `Host` but skipped the comparison when
 * `Origin` was missing — which is exactly the request a cross-site form submission
 * produces, so the check passed for the one case it existed to stop.
 */

function request(
  method: string,
  headers: Record<string, string>,
  url = "https://admin.example.com/api/admin/facilitySuspend",
): Request {
  return new Request(url, { method, headers, ...(method === "POST" ? { body: "{}" } : {}) });
}

const SAME_ORIGIN = {
  host: "admin.example.com",
  origin: "https://admin.example.com",
  "content-type": "application/json",
};

describe("checkSameOrigin", () => {
  it("accepts a same-origin JSON POST", () => {
    expect(checkSameOrigin(request("POST", SAME_ORIGIN))).toBeNull();
  });

  it("refuses a POST with no Origin header", () => {
    const { origin, ...withoutOrigin } = SAME_ORIGIN;
    void origin;

    const rejection = checkSameOrigin(request("POST", withoutOrigin));

    expect(rejection?.code).toBe("ORIGIN_REJECTED");
    expect(rejection?.reason).toMatch(/no Origin header/);
  });

  it("refuses a POST from a different origin", () => {
    const rejection = checkSameOrigin(
      request("POST", { ...SAME_ORIGIN, origin: "https://evil.example" }),
    );

    expect(rejection?.reason).toMatch(/does not match Host/);
  });

  it("refuses a POST whose Sec-Fetch-Site is not same-origin", () => {
    const rejection = checkSameOrigin(
      request("POST", { ...SAME_ORIGIN, "sec-fetch-site": "cross-site" }),
    );

    expect(rejection?.reason).toMatch(/Sec-Fetch-Site/);
  });

  it("refuses a form-encoded body, which a cross-site form can send", () => {
    const rejection = checkSameOrigin(
      request("POST", { ...SAME_ORIGIN, "content-type": "application/x-www-form-urlencoded" }),
    );

    expect(rejection?.reason).toMatch(/Content-Type/);
  });

  it("refuses a POST with no Host header", () => {
    // `Request` does not synthesise a `host` header from the URL, so this is the real
    // shape of a request that arrived without one.
    const rejection = checkSameOrigin(
      request("POST", { origin: "https://admin.example.com", "content-type": "application/json" }),
    );

    expect(rejection?.reason).toMatch(/no Host header/);
  });

  it("honours x-forwarded-host ahead of Host, for a deployment behind a proxy", () => {
    const rejection = checkSameOrigin(
      request("POST", {
        ...SAME_ORIGIN,
        "x-forwarded-host": "admin.example.com",
        origin: "https://admin.example.com",
      }),
    );

    expect(rejection).toBeNull();
  });

  it.each(["GET", "HEAD", "OPTIONS"])("leaves %s alone, with no Origin at all", (method) => {
    expect(checkSameOrigin(request(method, { host: "admin.example.com" }))).toBeNull();
  });
});
