import { describe, expect, it } from "vitest";

import { buildContentSecurityPolicy } from "../../proxy";

/**
 * The script policy is the one that broke the console, so it is pinned here.
 *
 * A static `script-src 'self'` blocked the App Router's inline RSC scripts and left every
 * page un-hydrated. The fix is a per-request nonce; these assertions keep anyone from
 * "simplifying" it back to a static policy or widening it to `'unsafe-inline'`.
 */

describe("buildContentSecurityPolicy", () => {
  it("allows scripts by nonce, not by 'unsafe-inline'", () => {
    const policy = buildContentSecurityPolicy("abc123", false, false);
    const script = policy.split("; ").find((part) => part.startsWith("script-src"))!;

    expect(script).toContain("'nonce-abc123'");
    expect(script).toContain("'strict-dynamic'");
    expect(script).not.toContain("'unsafe-inline'");
    expect(script).not.toContain("'unsafe-eval'");
  });

  it("adds 'unsafe-eval' only for the development server", () => {
    expect(buildContentSecurityPolicy("n", false, true)).toContain("'unsafe-eval'");
    expect(buildContentSecurityPolicy("n", true, false)).not.toContain("'unsafe-eval'");
  });

  it("keeps the browser away from every other origin", () => {
    const policy = buildContentSecurityPolicy("n", false, false);

    expect(policy).toContain("connect-src 'self'");
    expect(policy).toContain("frame-ancestors 'none'");
    expect(policy).toContain("object-src 'none'");
    expect(policy).toContain("base-uri 'self'");
    expect(policy).toContain("form-action 'self'");
  });

  it("upgrades insecure requests only on an https deployment", () => {
    expect(buildContentSecurityPolicy("n", true, false)).toContain("upgrade-insecure-requests");
    expect(buildContentSecurityPolicy("n", false, false)).not.toContain(
      "upgrade-insecure-requests",
    );
  });
});
