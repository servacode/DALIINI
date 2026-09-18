import { ResponseError } from "@servacode/api-typescript";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

/**
 * Session behaviour: refresh coalescing, single retry, and cleanup on failure.
 *
 * The refresh secret rotates on use and the backend treats a replayed one as stolen, so two
 * concurrent requests must not both spend it. That is the property this file exists for; it
 * cannot be seen by reading the code and it cannot be caught by a type.
 *
 * `next/headers` and the generated client are stubbed because the subject here is the
 * coordination, not the transport. The transport is exercised end to end by the Playwright
 * suite against a real Django.
 */

const store = new Map<string, string>();
let refreshCalls = 0;
let refreshShouldFail = false;
let adminCallResults: (("ok" | 401 | 403)[]) = [];

vi.mock("next/headers", () => ({
  cookies: async () => ({
    get: (name: string) => (store.has(name) ? { value: store.get(name) } : undefined),
    set: (name: string, value: string) => store.set(name, value),
    delete: (name: string) => store.delete(name),
  }),
}));

/**
 * The real `ResponseError` from the generated client, not a look-alike.
 *
 * `readApiError` narrows with `instanceof`, so a stub that merely sets `name` would be
 * treated as an unrecognised transport failure and every assertion below would be about a
 * 502 instead of the status under test.
 */
function failure(status: number): ResponseError {
  const code = status === 401 ? "AUTHENTICATION_REQUIRED" : "PERMISSION_DENIED";
  return new ResponseError(
    new Response(JSON.stringify({ code, message: "", details: {}, requestId: "" }), {
      status,
      headers: { "Content-Type": "application/json" },
    }),
  );
}

vi.mock("../../lib/api/client", async () => {
  const actual =
    await vi.importActual<typeof import("../../lib/api/client")>("../../lib/api/client");
  return {
    ...actual,
    authApi: () => ({
      authRefresh: async () => {
        refreshCalls += 1;
        // A real rotation takes a tick; without one, concurrent callers would serialise by
        // accident and the coalescing would not actually be under test.
        await new Promise((resolve) => setTimeout(resolve, 10));
        if (refreshShouldFail) throw failure(401);
        return {
          accessToken: `access-${refreshCalls}`,
          refreshToken: `refresh-${refreshCalls}`,
          sessionId: "session-1",
          expiresAt: new Date(Date.now() + 86_400_000),
        };
      },
    }),
    adminApis: () => ({
      auth: { authLogout: async () => undefined },
      account: { accountProfileRetrieve: async () => ({}) },
    }),
  };
});

beforeEach(() => {
  process.env.ADMIN_PUBLIC_ORIGIN = "http://localhost:3000";
  process.env.ADMIN_API_ORIGIN = "http://127.0.0.1:8000";
  store.clear();
  refreshCalls = 0;
  refreshShouldFail = false;
  adminCallResults = [];
  // Deliberately no `vi.resetModules()`. Re-importing the graph would hand the subject a
  // fresh copy of `ResponseError`, and `readApiError` narrows with `instanceof` — every
  // status assertion below would then be about a 502. The in-flight refresh map clears
  // itself in a `finally`, so there is no module state left to reset.
});

afterEach(() => {
  vi.clearAllMocks();
});

function seedSession(): void {
  store.set(
    "directory_admin_refresh",
    Buffer.from(JSON.stringify({ refreshToken: "refresh-0", sessionId: "session-1" })).toString(
      "base64url",
    ),
  );
}

describe("refresh coalescing", () => {
  it("spends the rotating secret once when several calls refresh at the same time", async () => {
    seedSession();
    const { callWithSession } = await import("../../lib/auth/session");

    // No access cookie, so every one of these needs a refresh before it can proceed.
    const results = await Promise.all([
      callWithSession(async () => "a"),
      callWithSession(async () => "b"),
      callWithSession(async () => "c"),
    ]);

    expect(results.every((result) => result.ok)).toBe(true);
    expect(refreshCalls).toBe(1);
  });

  it("stores the rotated secret, not the spent one", async () => {
    seedSession();
    const { callWithSession } = await import("../../lib/auth/session");

    await callWithSession(async () => "x");

    const material = JSON.parse(
      Buffer.from(store.get("directory_admin_refresh")!, "base64url").toString("utf8"),
    );
    expect(material.refreshToken).toBe("refresh-1");
    expect(store.get("directory_admin_access")).toBe("access-1");
  });
});

describe("failure handling", () => {
  it("clears both cookies when the refresh is rejected", async () => {
    seedSession();
    refreshShouldFail = true;
    const { callWithSession } = await import("../../lib/auth/session");

    const result = await callWithSession(async () => "x");

    expect(result.ok).toBe(false);
    expect(store.has("directory_admin_refresh")).toBe(false);
    expect(store.has("directory_admin_access")).toBe(false);
  });

  it("does not retry a rejected refresh, which is what reuse detection watches for", async () => {
    seedSession();
    refreshShouldFail = true;
    const { callWithSession } = await import("../../lib/auth/session");

    await callWithSession(async () => "x");

    expect(refreshCalls).toBe(1);
  });

  it("answers 401 with the contract envelope when there is no session at all", async () => {
    const { callWithSession } = await import("../../lib/auth/session");

    const result = await callWithSession(async () => "x");

    expect(result.ok).toBe(false);
    if (!result.ok) {
      expect(result.status).toBe(401);
      expect(result.body.code).toBe("AUTHENTICATION_REQUIRED");
    }
  });
});

describe("retry policy", () => {
  it("retries once after a 401 and then succeeds", async () => {
    seedSession();
    store.set("directory_admin_access", "stale-token");
    adminCallResults = [401, "ok"];
    const { callWithSession } = await import("../../lib/auth/session");

    let attempts = 0;
    const result = await callWithSession(async () => {
      const outcome = adminCallResults[attempts++];
      if (outcome === 401) throw failure(401);
      return "done";
    });

    expect(result).toEqual({ ok: true, data: "done" });
    expect(attempts).toBe(2);
    expect(refreshCalls).toBe(1);
  });

  it("does not retry a 403, which is an answer rather than a session problem", async () => {
    seedSession();
    store.set("directory_admin_access", "fresh-token");
    const { callWithSession } = await import("../../lib/auth/session");

    let attempts = 0;
    const result = await callWithSession(async () => {
      attempts++;
      throw failure(403);
    });

    expect(attempts).toBe(1);
    expect(refreshCalls).toBe(0);
    expect(result.ok).toBe(false);
    if (!result.ok) expect(result.status).toBe(403);
  });

  it("stops after one retry rather than looping", async () => {
    seedSession();
    store.set("directory_admin_access", "stale-token");
    const { callWithSession } = await import("../../lib/auth/session");

    let attempts = 0;
    const result = await callWithSession(async () => {
      attempts++;
      throw failure(401);
    });

    expect(attempts).toBe(2);
    expect(result.ok).toBe(false);
    expect(store.has("directory_admin_refresh")).toBe(false);
  });
});
