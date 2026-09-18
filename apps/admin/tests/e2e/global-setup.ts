import { mkdirSync } from "node:fs";
import { dirname } from "node:path";

import { request } from "@playwright/test";

import { FULL, FULL_STATE, LIMITED, LIMITED_STATE } from "./fixtures";

/**
 * Sign each operator in once, through the real BFF, and keep the session for the suite.
 *
 * The backend throttles login at ten attempts a minute, and it should. A suite that signed
 * in before every test tripped that limit around the sixteenth test and failed for a reason
 * that had nothing to do with what it was testing. Loosening the throttle would be weakening
 * production to suit a test, so the suite spends fewer logins instead.
 *
 * Tests that are *about* the session — logging in, rotating it, logging out, tampering with
 * it — still create their own, because sharing a session they revoke or rotate would break
 * every test after them.
 */
export default async function globalSetup(): Promise<void> {
  const baseURL = process.env.ADMIN_E2E_URL ?? "http://localhost:3000";

  for (const [operator, path] of [
    [FULL, FULL_STATE],
    [LIMITED, LIMITED_STATE],
  ] as const) {
    const context = await request.newContext({ baseURL });
    const response = await context.post("/api/session/login", {
      headers: { Origin: baseURL, "Content-Type": "application/json" },
      data: { phone: operator.phone, password: operator.password },
    });
    if (!response.ok()) {
      throw new Error(`global setup could not sign in ${operator.phone}: ${response.status()}`);
    }
    mkdirSync(dirname(path), { recursive: true });
    await context.storageState({ path });
    await context.dispose();
  }
}
