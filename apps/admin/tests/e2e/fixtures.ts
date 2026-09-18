import { execFileSync } from "node:child_process";
import { join } from "node:path";

import { type Page, expect } from "@playwright/test";

/**
 * The operators the suite signs in as, matching `seed_e2e_fixtures`.
 *
 * Two of them, because the sharpest assertions in this suite are the negative ones: what a
 * limited operator cannot see, cannot reach by typing a URL, and cannot perform.
 */

export const FULL = {
  phone: "+963900111222",
  password: "OperatorPass123!",
  name: "مشغّل كامل",
} as const;

export const LIMITED = {
  phone: "+963900333444",
  password: "LimitedPass123!",
  name: "مشغّل محدود",
} as const;

/** Sessions created once by `global-setup.ts`; see the throttle note there. */
export const FULL_STATE = join(__dirname, ".auth", "full.json");
export const LIMITED_STATE = join(__dirname, ".auth", "limited.json");

/** Open the console with a session already in the context's storage state. */
export async function openConsole(page: Page): Promise<void> {
  await page.goto("/dashboard");
  await expect(page.getByTestId("operator-name")).toBeVisible({ timeout: 20_000 });
}

/**
 * Sign in through the login screen. Spends one attempt against the backend's login
 * throttle, so it is reserved for tests about the session itself.
 */
export async function signIn(
  page: Page,
  operator: { phone: string; password: string },
): Promise<void> {
  await page.goto("/login");
  await page.getByTestId("login-phone").fill(operator.phone);
  await page.getByTestId("login-password").fill(operator.password);
  await page.getByTestId("login-submit").click();
  await expect(page).toHaveURL(/\/dashboard$/, { timeout: 20_000 });
}

/** Read an Admin operation through the BFF, as the page itself would. */
export async function readOperation<T>(
  page: Page,
  operation: string,
  params: Record<string, string> = {},
): Promise<T> {
  return page.evaluate(
    async ([name, query]) => {
      const suffix = new URLSearchParams(query as Record<string, string>).toString();
      const response = await fetch(`/api/admin/${name}${suffix ? `?${suffix}` : ""}`, {
        credentials: "same-origin",
      });
      return response.json();
    },
    [operation, params] as const,
  ) as Promise<T>;
}

/** Read the public API directly, to prove an Admin change reached the outside world. */
export async function readPublic<T>(page: Page, path: string): Promise<T> {
  const apiOrigin = process.env.E2E_API_ORIGIN ?? "http://127.0.0.1:8000";
  const response = await page.request.get(`${apiOrigin}${path}`);
  return response.json() as Promise<T>;
}

/**
 * Re-run the launch seed inside the running Django container.
 *
 * The point is to prove, end to end, that an operator's change survives it: the seed is
 * non-destructive by design, and this is where that design meets a real browser action.
 */
export function rerunLaunchSeed(): string {
  const container = process.env.E2E_API_CONTAINER ?? "e2e-api";
  return execFileSync(
    "docker",
    ["exec", container, "sh", "-c", "uv run python manage.py seed_launch_baseline"],
    { encoding: "utf8", env: { ...process.env, MSYS_NO_PATHCONV: "1" } },
  );
}
