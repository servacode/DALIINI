import { expect, test } from "@playwright/test";

import { FULL, FULL_STATE, LIMITED_STATE, openConsole, signIn } from "./fixtures";

/**
 * Authentication, against a real Django and a real browser.
 *
 * The cookie assertions are the reason this suite exists rather than a unit test: INT-012
 * was a cookie a browser silently refused, and the CSP defect that left the console
 * un-hydrated was invisible to every HTTP-level check. Only a browser shows either.
 *
 * Tests about the session itself — logging in, rotating, logging out, tampering — sign in
 * fresh, because they spend or revoke the session they use. Everything else borrows the
 * session `global-setup.ts` created, which keeps the suite under the backend's login
 * throttle rather than loosening it.
 */

test.describe("sign in", () => {
  test("a wrong password is refused, with nothing technical on screen", async ({ page }) => {
    await page.goto("/login");
    await page.getByTestId("login-phone").fill(FULL.phone);
    await page.getByTestId("login-password").fill("WrongPass!");
    await page.getByTestId("login-submit").click();

    const error = page.getByTestId("login-error");
    await expect(error).toBeVisible();
    // The central mapper's phrase for AUTHENTICATION_FAILED, not the backend's own text.
    await expect(error).toHaveText("بيانات الدخول غير صحيحة.");

    const body = (await page.locator("body").textContent()) ?? "";
    for (const leak of ["Traceback", "django", "Bearer", "eyJ", "127.0.0.1", "WrongPass"]) {
      expect(body).not.toContain(leak);
    }
    await expect(page).toHaveURL(/\/login$/);
  });

  test("a correct password lands on the dashboard and sets usable cookies", async ({
    page,
    context,
  }) => {
    await signIn(page, FULL);

    await expect(page).toHaveURL(/\/dashboard$/);
    await expect(page.getByTestId("operator-name")).toContainText(FULL.name);

    const cookies = await context.cookies();
    for (const base of ["directory_admin_refresh", "directory_admin_access"]) {
      const cookie = cookies.find((item) => item.name === base);
      expect(cookie, `${base} must be accepted by the browser`).toBeTruthy();
      expect(cookie!.httpOnly).toBe(true);
      expect(cookie!.sameSite).toBe("Strict");
      expect(cookie!.path).toBe("/");
    }
  });
});

test.describe("session lifecycle", () => {
  test("refreshing rotates the secret and the session keeps working", async ({
    page,
    context,
  }) => {
    await signIn(page, FULL);
    const before = (await context.cookies()).find(
      (cookie) => cookie.name === "directory_admin_refresh",
    )?.value;

    const status = await page.evaluate(async () => {
      const response = await fetch("/api/session/refresh", {
        method: "POST",
        credentials: "same-origin",
        headers: { "Content-Type": "application/json" },
        body: "{}",
      });
      return response.status;
    });

    expect(status).toBe(200);
    const after = (await context.cookies()).find(
      (cookie) => cookie.name === "directory_admin_refresh",
    )?.value;
    expect(after).toBeTruthy();
    expect(after).not.toBe(before);

    await page.goto("/dashboard");
    await expect(page.getByTestId("operator-name")).toContainText(FULL.name);
  });

  test("signing out clears the session and the console is no longer reachable", async ({
    page,
    context,
  }) => {
    await signIn(page, FULL);

    await page.getByTestId("logout").click();
    await expect(page).toHaveURL(/\/login$/);

    const cookies = await context.cookies();
    expect(cookies.find((item) => item.name === "directory_admin_refresh")).toBeFalsy();
    expect(cookies.find((item) => item.name === "directory_admin_access")).toBeFalsy();

    await page.goto("/dashboard");
    await expect(page).toHaveURL(/\/login$/);
  });

  test("an invalid refresh cookie ends the session instead of looping", async ({
    page,
    context,
  }) => {
    await signIn(page, FULL);

    await context.addCookies([
      {
        name: "directory_admin_refresh",
        value: "not-a-valid-cookie",
        domain: "localhost",
        path: "/",
      },
    ]);
    await context.clearCookies({ name: "directory_admin_access" });

    await page.goto("/dashboard");

    await expect(page).toHaveURL(/\/login$/, { timeout: 15_000 });
  });
});

test.describe("with an established session", () => {
  test.use({ storageState: FULL_STATE });

  test("neither token is readable from the page", async ({ page }) => {
    await openConsole(page);

    const exposed = await page.evaluate(() => ({
      cookie: document.cookie,
      local: JSON.stringify(window.localStorage),
      session: JSON.stringify(window.sessionStorage),
    }));

    expect(exposed.cookie).not.toContain("directory_admin");
    expect(exposed.local).not.toContain("eyJ");
    expect(exposed.session).not.toContain("eyJ");
  });

  test("the shell renders the operator returned by /admin/me/", async ({ page }) => {
    await openConsole(page);

    const me = await page.evaluate(async () => {
      const response = await fetch("/api/admin/me", { credentials: "same-origin" });
      return response.json();
    });

    expect(me.userId).toBeTruthy();
    expect(me.displayName).toBe(FULL.name);
    expect(me.permissions.length).toBeGreaterThan(10);
    expect(Object.keys(me).sort()).toEqual(["displayName", "permissions", "userId"]);
    await expect(page.getByTestId("operator-name")).toContainText(FULL.name);
  });

  test("a full operator sees every section", async ({ page }) => {
    await openConsole(page);

    await expect(page.getByTestId("admin-nav").locator("a")).toHaveCount(9);
  });

  test("a mutation has no GET form at all", async ({ page }) => {
    await openConsole(page);

    const status = await page.evaluate(async () => {
      const response = await fetch("/api/admin/facilityClose?id=x", {
        credentials: "same-origin",
      });
      return response.status;
    });

    expect(status).toBe(404);
  });
});

test.describe("as a limited operator", () => {
  test.use({ storageState: LIMITED_STATE });

  test("the navigation shows only what they hold", async ({ page }) => {
    await openConsole(page);

    const nav = page.getByTestId("admin-nav");
    await expect(nav.locator("a")).toHaveCount(2);
    await expect(nav).toContainText("المراجعات");
    await expect(nav).not.toContainText("المستخدمون");
    await expect(nav).not.toContainText("الإعدادات");
  });

  test("typing the URL does not get past the backend", async ({ page }) => {
    await page.goto("/settings");

    // The page renders — hiding a link was never the boundary — and the data call is refused.
    await expect(page.getByTestId("error-state")).toBeVisible();
    await expect(page.getByTestId("error-state")).toContainText("لا تملك الصلاحية");
  });

  test("a direct mutation is refused by the backend", async ({ page }) => {
    await openConsole(page);

    const result = await page.evaluate(async () => {
      const response = await fetch("/api/admin/provinceUpdate", {
        method: "POST",
        credentials: "same-origin",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ id: "00000000-0000-0000-0000-000000000000", active: true }),
      });
      return { status: response.status, body: await response.json() };
    });

    expect(result.status).toBe(403);
    expect(result.body.code).toBe("PERMISSION_DENIED");
  });

  test("private evidence is refused, not merely hidden", async ({ page }) => {
    await openConsole(page);

    const result = await page.evaluate(async () => {
      const response = await fetch("/api/admin/evidence/00000000-0000-4000-8000-000000000000", {
        credentials: "same-origin",
      });
      return { status: response.status, body: await response.json() };
    });

    expect(result.status).toBe(403);
    expect(result.body.code).toBe("PERMISSION_DENIED");
  });
});

test.describe("cross-origin protection", () => {
  test("a mutation without an Origin header is refused", async ({ request }) => {
    // A separate request context sends no Origin header at all.
    const response = await request.post("/api/session/logout", {
      headers: { "Content-Type": "application/json" },
      data: {},
    });

    expect(response.status()).toBe(403);
    expect((await response.json()).code).toBe("ORIGIN_REJECTED");
  });

  test("a mutation from a foreign Origin is refused", async ({ request }) => {
    const response = await request.post("/api/admin/provinceUpdate", {
      headers: { "Content-Type": "application/json", Origin: "https://evil.example" },
      data: { id: "x", active: true },
    });

    expect(response.status()).toBe(403);
  });
});
