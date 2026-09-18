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
