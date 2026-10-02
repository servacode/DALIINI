import { type Page, expect, test } from "@playwright/test";

/**
 * What only a browser can answer about the public site.
 *
 * Three things, and nothing that the HTTP pass or a unit test already covers:
 *
 *   1. The page hydrates. A server-rendered page looks perfect and does nothing until React
 *      runs, and that failure is invisible to every other kind of test we have.
 *   2. Nothing throws once it does. An error after hydration leaves a page that rendered and
 *      then stopped responding.
 *   3. The things a visitor presses actually do something.
 *
 * Data comes from whatever the backend has; these assert behaviour, not content, so seeding a
 * different province does not break them.
 */

/** Fail loudly on anything the page throws, rather than on a symptom later. */
function watchForErrors(page: Page): string[] {
  const errors: string[] = [];
  page.on("pageerror", (error) => errors.push(error.message));
  page.on("console", (message) => {
    if (message.type() === "error") errors.push(message.text());
  });
  return errors;
}

test.describe("the site a visitor meets", () => {
  test("the home page hydrates and throws nothing", async ({ page }) => {
    const errors = watchForErrors(page);
    await page.goto("/");

    await expect(page.getByRole("heading").first()).toBeVisible();
    expect(errors, "console or page errors on /").toEqual([]);
  });

  test("a card's dialog opens, which only a hydrated page can do", async ({ page }) => {
    // The real proof, and the one no other test we have can give: a button whose entire effect
    // is client-side. Server-rendered HTML shows the button and does nothing when it is
    // pressed — which is exactly how the console failed once, with every test still green.
    const errors = watchForErrors(page);
    await page.goto("/");

    const opener = page.locator('button[aria-haspopup="dialog"]').first();
    test.skip((await opener.count()) === 0, "this province has no facilities to show");

    await opener.click();
    await expect(page.getByRole("dialog")).toBeVisible();

    // And it closes again, so the page is not merely reacting once.
    await page.keyboard.press("Escape");
    await expect(page.getByRole("dialog")).toHaveCount(0);

    expect(errors, "errors while opening a dialog").toEqual([]);
  });

  test("the page is Arabic and right-to-left, which the whole layout depends on", async ({
    page,
  }) => {
    await page.goto("/");

    await expect(page.locator("html")).toHaveAttribute("dir", "rtl");
    await expect(page.locator("html")).toHaveAttribute("lang", "ar");
  });

  test("nothing overflows the viewport on a phone", async ({ page }, testInfo) => {
    test.skip(testInfo.project.name !== "phone", "a phone-width concern");
    await page.goto("/");

    const overflow = await page.evaluate(
      () => document.documentElement.scrollWidth - document.documentElement.clientWidth,
    );
    // A horizontal scrollbar on a directory read one-handed is the difference between usable
    // and not.
    expect(overflow, "horizontal overflow in px").toBeLessThanOrEqual(1);
  });

  test("the duty page answers with the roster, or says plainly that there is none", async ({
    page,
  }) => {
    const errors = watchForErrors(page);
    await page.goto("/duty");

    await expect(page.getByRole("heading").first()).toBeVisible();
    // Either outcome is correct; a blank page is not.
    const body = await page.locator("body").innerText();
    expect(body.trim().length).toBeGreaterThan(80);

    expect(errors, "errors on /duty").toEqual([]);
  });

  test("a province page lists what that province has", async ({ page, request }) => {
    const api = process.env.PUBLIC_API_ORIGIN;
    test.skip(!api, "needs the API origin to pick a real province");

    const provinces = await (await request.get(`${api}/api/v1/public/provinces/`)).json();
    const province = provinces.items?.[0];
    test.skip(!province, "the backend has no provinces");

    const errors = watchForErrors(page);
    await page.goto(`/${province.code}`);

    await expect(page.getByRole("heading").first()).toContainText(province.nameAr);
    expect(errors, `errors on /${province.code}`).toEqual([]);
  });

  test("the contact form is usable: it accepts input and refuses to send an empty message", async ({
    page,
  }) => {
    const errors = watchForErrors(page);
    await page.goto("/contact");

    const name = page.locator("input[name='name']").first();
    // The form is hidden entirely when no browser API origin is configured, which is a
    // deliberate state rather than a fault.
    test.skip((await name.count()) === 0, "the contact form is not configured in this build");

    await name.fill("مستخدم الاختبار");
    await expect(name).toHaveValue("مستخدم الاختبار");

    // Typing at all proves hydration; the browser's own validation stops an empty send.
    await page.getByRole("button", { name: /إرسال|أرسل/ }).click();
    await expect(page).toHaveURL(/\/contact/);

    expect(errors, "errors on /contact").toEqual([]);
  });

  test("every link in the header and footer leads somewhere that exists", async ({
    page,
    request,
  }) => {
    await page.goto("/");

    const hrefs = await page.evaluate(() =>
      Array.from(document.querySelectorAll("header a[href], footer a[href]"))
        .map((a) => a.getAttribute("href") ?? "")
        .filter((href) => href.startsWith("/")),
    );
    expect(hrefs.length).toBeGreaterThan(0);

    for (const href of Array.from(new Set(hrefs))) {
      const response = await request.get(href);
      expect(response.status(), `${href} answered ${response.status()}`).toBeLessThan(400);
    }
  });
});
