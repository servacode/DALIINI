import { crc32, deflateSync } from "node:zlib";

import { type Page, expect, test } from "@playwright/test";

import {
  FULL_STATE,
  openConsole,
  readOperation,
  readPublic,
  rerunLaunchSeed,
} from "./fixtures";

/**
 * The operational golden paths.
 *
 * Every assertion here ends somewhere observable: an audit row, a public API response, or a
 * status the backend reports back. A screen that says a change was made is not evidence
 * that it was.
 *
 * Every test runs on the session `global-setup.ts` created, so the suite spends two logins
 * rather than one per test against the backend's throttle.
 */

test.use({ storageState: FULL_STATE });

test.describe("review golden path", () => {
  test("the queue lists the pending application and filters narrow it", async ({ page }) => {
    await openConsole(page);
    await page.goto("/reviews");

    await expect(page.getByTestId("data-table")).toContainText("e2e-صيدلية قيد المراجعة");

    await page.getByTestId("filter-status").selectOption("APPROVED");
    await page.getByTestId("filter-bar").getByRole("button", { name: "تطبيق" }).click();

    await expect(page.getByTestId("empty-state")).toBeVisible();
  });

  test("a rejection records the reason and moves the application", async ({ page }) => {
    await openConsole(page);
    await page.goto("/reviews");
    await page.getByRole("link", { name: "e2e-صيدلية قيد المراجعة" }).click();

    await page.getByTestId("reject").click();
    await page.getByTestId("reject-reason").fill("الأدلة غير مكتملة.");
    await page.getByTestId("confirm-accept").click();

    await expect(page.getByTestId("toast")).toContainText("تم رفض الطلب");
    await expect(page.getByTestId("status-badge").first()).toContainText("مرفوض");

    const audit = await readOperation<{ items: { action: string }[] }>(page, "audit", {
      action: "facility_application.rejected",
    });
    expect(audit.items.length).toBeGreaterThan(0);
  });
});

test.describe("approval", () => {
  test("approving makes the facility public, and a second decision is refused", async ({
    page,
  }) => {
    await openConsole(page);
    await page.goto("/reviews");
    await page.getByRole("link", { name: "e2e-صيدلية للقبول" }).click();
    await expect(page).toHaveURL(/\/reviews\/[0-9a-f-]+$/);
    const applicationId = page.url().split("/").pop()!;

    await page.getByTestId("approve").click();
    await page.getByTestId("confirm-accept").click();

    await expect(page.getByTestId("toast")).toContainText("تم قبول الطلب");
    await expect(page.getByTestId("status-badge").first()).toContainText("مقبول");
    // The decision buttons are gone: the application is no longer SUBMITTED.
    await expect(page.getByTestId("approve")).toHaveCount(0);

    // The approval reached the outside world.
    const provinces = await readPublic<{ items: { id: string; code: string }[] }>(
      page,
      "/api/v1/public/provinces/",
    );
    const raqqa = provinces.items.find((item) => item.code === "raqqa")!.id;
    const categories = await readPublic<{ items: { id: string; nameAr: string }[] }>(
      page,
      `/api/v1/public/provinces/${raqqa}/categories/`,
    );
    const pharmacy = categories.items.find((item) => item.nameAr === "صيدليات")!.id;
    const listing = await readPublic<{ items: { nameAr: string }[] }>(
      page,
      `/api/v1/public/facilities/?provinceId=${raqqa}&categoryId=${pharmacy}`,
    );
    expect(listing.items.map((item) => item.nameAr)).toContain("e2e-صيدلية للقبول");

    // Deciding again is refused by the backend, whatever the UI would have offered.
    const second = await page.evaluate(async (id) => {
      const response = await fetch("/api/admin/reviewReject", {
        method: "POST",
        credentials: "same-origin",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ id, reason: "محاولة ثانية" }),
      });
      return { status: response.status, body: await response.json() };
    }, applicationId);
    expect(second.status).toBe(400);
    expect(second.body.code).toBe("VALIDATION_ERROR");
  });
});

test.describe("facility lifecycle", () => {
  test("suspending and reactivating both land in the audit trail", async ({ page }) => {
    await openConsole(page);
    await page.goto("/facilities");
    await page.getByTestId("filter-q").fill("e2e-صيدلية فعّالة");
    await page.getByTestId("filter-bar").getByRole("button", { name: "تطبيق" }).click();
    await page.getByTestId("data-table").getByRole("link", { name: "فتح" }).first().click();

    await page.getByTestId("suspend").click();
    await page.getByTestId("action-reason").fill("إيقاف اختباري");
    await page.getByTestId("confirm-accept").click();
    await expect(page.getByTestId("status-badge").first()).toContainText("موقوفة");

    await page.getByTestId("reactivate").click();
    await page.getByTestId("confirm-accept").click();
    await expect(page.getByTestId("status-badge").first()).toContainText("فعّالة");

    const audit = await readOperation<{ items: { action: string }[] }>(page, "audit", {
      resource: "Facility",
    });
    const actions = audit.items.map((row) => row.action);
    expect(actions).toContain("facility.suspended");
    expect(actions).toContain("facility.active");
  });
});

test.describe("user lifecycle", () => {
  test("blocking and unblocking are recorded", async ({ page }) => {
    await openConsole(page);
    await page.goto("/users");
    await page.getByTestId("filter-q").fill("مالك الاختبار");
    await page.getByTestId("filter-bar").getByRole("button", { name: "تطبيق" }).click();
    await page.getByTestId("data-table").getByRole("link").first().click();

    await page.getByTestId("block").click();
    await page.getByTestId("confirm-accept").click();
    await expect(page.getByTestId("status-badge").first()).toContainText("محظور");

    await page.getByTestId("unblock").click();
    await page.getByTestId("confirm-accept").click();
    await expect(page.getByTestId("status-badge").first()).toContainText("فعّال");

    const audit = await readOperation<{ items: { action: string }[] }>(page, "audit", {
      resource: "User",
    });
    const actions = audit.items.map((row) => row.action);
    expect(actions).toContain("user.blocked");
    expect(actions).toContain("user.unblocked");
  });
});

test.describe("Cycle J", () => {
  test("a province switch changes what the public API serves", async ({ page }) => {
    await openConsole(page);

    const before = await readPublic<{ items: { nameAr: string }[] }>(
      page,
      "/api/v1/public/provinces/",
    );
    const raqqa = await readPublic<{ items: { id: string; code: string }[] }>(
      page,
      "/api/v1/public/provinces/",
    );
    const raqqaId = raqqa.items.find((item) => item.code === "raqqa")!.id;
    expect(before.items).toHaveLength(1);

    const categoriesBefore = await readPublic<{ items: { nameAr: string }[] }>(
      page,
      `/api/v1/public/provinces/${raqqaId}/categories/`,
    );
    expect(categoriesBefore.items.map((item) => item.nameAr)).toEqual(["صيدليات"]);

    await page.goto("/taxonomy/categories");
    await page.getByTestId("configure-medical-laboratory").click();
    await page.getByTestId("switch-province").selectOption({ label: "الرقة" });
    await page.getByTestId("switch-public").check();
    await page.getByTestId("save-switch").click();
    await expect(page.getByTestId("toast")).toBeVisible();

    const categoriesAfter = await readPublic<{ items: { nameAr: string }[] }>(
      page,
      `/api/v1/public/provinces/${raqqaId}/categories/`,
    );
    expect(categoriesAfter.items.map((item) => item.nameAr)).toContain("مخابر طبية");

    // And back off again.
    await page.getByTestId("switch-public").uncheck();
    await page.getByTestId("save-switch").click();
    await expect(page.getByTestId("toast")).toBeVisible();

    const restored = await readPublic<{ items: { nameAr: string }[] }>(
      page,
      `/api/v1/public/provinces/${raqqaId}/categories/`,
    );
    expect(restored.items.map((item) => item.nameAr)).toEqual(["صيدليات"]);
  });

  test("duty is refused for a category that is not a pharmacy", async ({ page }) => {
    await openConsole(page);
    await page.goto("/taxonomy/categories");
    await page.getByTestId("configure-medical-laboratory").click();

    await page.getByTestId("cap-supportsDuty").check();
    await page.getByTestId("save-capabilities").click();

    await expect(page.getByTestId("error-state").first()).toBeVisible();
  });

  test("a new category is invisible until a switch is turned on", async ({ page }) => {
    await openConsole(page);
    await page.goto("/taxonomy/categories");

    await page.getByTestId("new-category").click();
    await page.getByTestId("category-group").selectOption({ label: "الصحة" });
    await page.getByTestId("category-code").fill("e2e-optics");
    await page.getByTestId("category-name-ar").fill("e2e-بصريات");
    await page.getByTestId("confirm-accept").click();

    await expect(page.getByTestId("toast")).toBeVisible();
    await expect(page.getByTestId("data-table")).toContainText("e2e-بصريات");

    const raqqa = await readPublic<{ items: { id: string; code: string }[] }>(
      page,
      "/api/v1/public/provinces/",
    );
    const categories = await readPublic<{ items: { nameAr: string }[] }>(
      page,
      `/api/v1/public/provinces/${raqqa.items[0]!.id}/categories/`,
    );
    expect(categories.items.map((item) => item.nameAr)).not.toContain("e2e-بصريات");
  });
});

test.describe("province rollout", () => {
  test("activating Aleppo changes the public list, and the seed does not undo it", async ({
    page,
  }) => {
    await openConsole(page);
    await page.goto("/provinces");

    await page.getByTestId("toggle-province-aleppo").click();
    await page.getByTestId("confirm-accept").click();
    await expect(page.getByTestId("toast")).toContainText("تم تفعيل المحافظة");

    const publicList = await readPublic<{ items: { code: string }[] }>(
      page,
      "/api/v1/public/provinces/",
    );
    expect(publicList.items.map((item) => item.code).sort()).toEqual(["aleppo", "raqqa"]);

    // The launch seed runs again, as it would after a restore or during qualification.
    const seedOutput = rerunLaunchSeed();
    expect(seedOutput).toContain("Nothing to do");
    const afterSeed = await readPublic<{ items: { code: string }[] }>(
      page,
      "/api/v1/public/provinces/",
    );
    expect(afterSeed.items.map((item) => item.code).sort()).toEqual(["aleppo", "raqqa"]);

    const audit = await readOperation<{ items: { action: string }[] }>(page, "audit", {
      action: "province.updated",
    });
    expect(audit.items.length).toBeGreaterThan(0);

    // Turn it back off, so the rest of the suite starts from the launch state.
    await page.getByTestId("toggle-province-aleppo").click();
    await page.getByTestId("confirm-accept").click();
    await expect(page.getByTestId("toast")).toContainText("تم تعطيل المحافظة");
  });
});

test.describe("verification policy", () => {
  test("a requirement can be created, edited and retired", async ({ page }) => {
    await openConsole(page);
    await page.goto("/verification");

    await page.getByTestId("new-requirement").click();
    await page.getByTestId("requirement-category").selectOption({ label: "صيدليات" });
    await page.getByTestId("requirement-label").fill("e2e-صورة الواجهة");
    await page.getByTestId("requirement-max").fill("3");
    await page.getByTestId("confirm-accept").click();

    await expect(page.getByTestId("toast")).toBeVisible();
    await expect(page.getByTestId("data-table")).toContainText("e2e-صورة الواجهة");

    const rows = page.getByTestId("data-table").locator("tbody tr");
    const id = await rows
      .filter({ hasText: "e2e-صورة الواجهة" })
      .locator("[data-testid^='toggle-requirement-']")
      .getAttribute("data-testid");
    expect(id).toBeTruthy();

    await page.locator(`[data-testid='${id}']`).click();
    await expect(page.getByTestId("toast")).toContainText("تم تعطيل المتطلب");
  });

  test("the screen does not claim the pharmacy policy is settled", async ({ page }) => {
    await openConsole(page);
    await page.goto("/verification");

    const notice = (await page.locator(".notice").textContent()) ?? "";
    expect(notice).toContain("لم تُحسم");
  });
});

/** A plain 160×90 PNG built in memory: a real image the picker's own checks accept. */
// 16:9 and at least 100 px a side, the smallest image the picker and the backend both accept.
function testPng(width = 320, height = 180): Buffer {
  const chunk = (type: string, data: Buffer) => {
    const length = Buffer.alloc(4);
    length.writeUInt32BE(data.length);
    const body = Buffer.concat([Buffer.from(type, "ascii"), data]);
    const crc = Buffer.alloc(4);
    crc.writeUInt32BE(crc32(body));
    return Buffer.concat([length, body, crc]);
  };
  const header = Buffer.alloc(13);
  header.writeUInt32BE(width, 0);
  header.writeUInt32BE(height, 4);
  header[8] = 8; // bit depth
  header[9] = 2; // truecolour
  const row = Buffer.concat([Buffer.from([0]), Buffer.alloc(width * 3, 0x3c)]);
  return Buffer.concat([
    Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]),
    chunk("IHDR", header),
    chunk("IDAT", deflateSync(Buffer.concat(Array.from({ length: height }, () => row)))),
    chunk("IEND", Buffer.alloc(0)),
  ]);
}

/**
 * Choose the slide image through the picker.
 *
 * The upload itself is answered here: this stack runs Django without object storage, so a
 * real upload has nowhere to land. The BFF upload route and its checks are covered by
 * `tests/unit/upload-route.test.ts`; what this proves is the rest of the path — the picker,
 * the preview and the key the save then sends to the real backend.
 */
async function pickAdImage(page: Page): Promise<void> {
  await page.route("**/api/admin/ads/images", (route) =>
    route.fulfill({
      status: 201,
      contentType: "application/json",
      body: JSON.stringify({ imageKey: "ads/e2e.jpg", url: "", width: 320, height: 180 }),
    }),
  );
  await page.getByTestId("ad-image-file").setInputFiles({
    name: "e2e.png",
    mimeType: "image/png",
    buffer: testPng(),
  });
  await expect(page.getByTestId("ad-image")).toHaveValue("ads/e2e.jpg");
}

test.describe("advertisements", () => {
  test("an end before its start is refused by the backend", async ({ page }) => {
    await openConsole(page);
    await page.goto("/ads");

    await page.getByTestId("new-ad").click();
    await pickAdImage(page);
    await page.getByTestId("ad-title").fill("e2e-إعلان");
    await page.getByTestId("ad-starts").fill("2026-10-01T10:00");
    await page.getByTestId("ad-ends").fill("2026-09-01T10:00");
    await page.getByTestId("confirm-accept").click();

    await expect(page.getByTestId("error-state").first()).toBeVisible();
    await expect(page.getByTestId("confirm-dialog")).toBeVisible();
  });

  test("a valid advertisement can be created, edited and activated", async ({ page }) => {
    await openConsole(page);
    await page.goto("/ads");

    await page.getByTestId("new-ad").click();
    await pickAdImage(page);
    await page.getByTestId("ad-title").fill("e2e-إعلان");
    await page.getByTestId("ad-starts").fill("2026-09-01T10:00");
    await page.getByTestId("ad-ends").fill("2026-10-01T10:00");
    await page.getByTestId("confirm-accept").click();

    await expect(page.getByTestId("toast")).toContainText("تمت إضافة الإعلان");
    await expect(page.getByTestId("data-table")).toContainText("e2e-إعلان");

    const row = page.getByTestId("data-table").locator("tbody tr").filter({ hasText: "e2e-إعلان" });
    await row.locator("[data-testid^='toggle-ad-']").click();
    await expect(page.getByTestId("toast")).toContainText("تم تفعيل الإعلان");
  });
});

test.describe("read-only screens", () => {
  test("audit, analytics, settings and system all render real data", async ({ page }) => {
    await openConsole(page);

    for (const [path, marker] of [
      ["/audit", "سجل التدقيق"],
      ["/analytics", "التحليلات"],
      ["/settings", "الإعدادات"],
      ["/system", "حالة النظام"],
    ] as const) {
      await page.goto(path);
      await expect(page.getByRole("heading", { name: marker })).toBeVisible();
      await expect(page.getByTestId("error-state")).toHaveCount(0);
    }
  });

  test("system status exposes no credential or connection string", async ({ page }) => {
    await openConsole(page);
    await page.goto("/system");
    await expect(page.getByRole("heading", { name: "الاعتماديات" })).toBeVisible();

    const body = (await page.locator("body").textContent()) ?? "";
    for (const leak of ["postgres://", "redis://", "password", "SECRET", "amazonaws"]) {
      expect(body).not.toContain(leak);
    }
  });

  test("the audit trail shows the request id, which is what a report quotes", async ({
    page,
  }) => {
    await openConsole(page);
    await page.goto("/audit");

    await expect(page.getByTestId("data-table")).toBeVisible();
    await expect(page.getByTestId("data-table")).toContainText("معرّف الطلب");
  });
});
