import { type Page, expect, test } from "@playwright/test";

import { FULL_STATE, openConsole } from "./fixtures";

/**
 * Every page in the console, opened in a browser at least once.
 *
 * The other suites follow the operational golden paths, which is the right shape for them —
 * but it left fourteen of the thirty screens never loaded by a browser at all. A page that is
 * never opened can throw on render, hydrate into nothing, or ask for an operation the BFF does
 * not know, and no unit test and no API test would see any of it. The console's CSP broke
 * exactly this way once: every page rendered and none of them hydrated, so every button in the
 * product was dead while every test stayed green.
 *
 * So this suite is deliberately shallow and wide. It asserts what every page must manage no
 * matter what data exists: that it renders its own heading, that nothing failed, and that
 * React is actually alive on it. Depth belongs in the golden paths.
 */

test.use({ storageState: FULL_STATE });

/** Pages that take no parameters. The dynamic ones are covered by the golden paths. */
const PAGES: readonly { path: string; heading: string }[] = [
  { path: "/dashboard", heading: "لوحة المتابعة" },
  { path: "/reports", heading: "البلاغات" },
  { path: "/duty", heading: "جدول المناوبات" },
  { path: "/analytics", heading: "التحليلات" },
  { path: "/analytics/staff", heading: "أداء الفريق" },
  { path: "/content/pages", heading: "الصفحات" },
  { path: "/content/faq", heading: "الأسئلة الشائعة" },
  { path: "/content/emergency", heading: "أرقام الطوارئ" },
  { path: "/content/messages", heading: "رسائل التواصل" },
  { path: "/reviews/templates", heading: "قوالب أسباب الرفض" },
  { path: "/taxonomy/groups", heading: "مجموعات التصنيفات" },
  { path: "/taxonomy/tags", heading: "التخصصات والخدمات" },
  { path: "/users/broadcast", heading: "الإشعارات" },
  { path: "/users/roles", heading: "الأدوار والصلاحيات" },
  { path: "/facilities/map", heading: "خريطة المنشآت" },
  { path: "/settings", heading: "الإعدادات" },
];

/**
 * Anything the console itself would show if a page failed, plus React's own crash output.
 * A page that renders its error state has still "loaded" by any HTTP measure, which is why
 * this is checked rather than the status code.
 */
async function assertNothingBroke(page: Page): Promise<void> {
  await expect(page.getByTestId("error-state")).toHaveCount(0);
  await expect(page.locator("text=Application error")).toHaveCount(0);
  await expect(page.locator("text=Unhandled Runtime Error")).toHaveCount(0);
}

for (const { path, heading } of PAGES) {
  test(`${path} renders, hydrates and reports no failure`, async ({ page }) => {
    const broken: string[] = [];
    page.on("pageerror", (error) => broken.push(error.message));
    // A 5xx from the BFF means the page asked for something the server could not answer.
    page.on("response", (response) => {
      if (response.url().includes("/api/admin/") && response.status() >= 500) {
        broken.push(`${response.status()} ${new URL(response.url()).pathname}`);
      }
    });

    await openConsole(page);
    await page.goto(path);

    await expect(page.getByRole("heading", { name: heading }).first()).toBeVisible({
      timeout: 20_000,
    });
    await assertNothingBroke(page);

    // The shell's navigation is a client component: if it responds, React is running on this
    // page, which is the part a server-rendered screenshot cannot tell you.
    await expect(page.getByTestId("operator-name")).toBeVisible();

    expect(broken, `errors on ${path}`).toEqual([]);
  });
}

test("every page in the navigation can be reached by clicking", async ({ page }) => {
  // Routes are one thing; the menu that reaches them is another, and a link to a route that
  // no longer exists is invisible until somebody clicks it.
  await openConsole(page);

  const links = page.getByRole("navigation").getByRole("link");
  const count = await links.count();
  expect(count).toBeGreaterThan(0);

  const targets: string[] = [];
  for (let index = 0; index < count; index += 1) {
    const href = await links.nth(index).getAttribute("href");
    if (href && href.startsWith("/")) targets.push(href);
  }

  for (const href of targets) {
    const response = await page.goto(href);
    expect(response?.status(), `${href} answered ${response?.status()}`).toBeLessThan(400);
    await assertNothingBroke(page);
  }
});
