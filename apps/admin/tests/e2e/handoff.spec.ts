import { expect, test } from "@playwright/test";

import { FULL_STATE, openConsole, readOperation } from "./fixtures";

/**
 * The Admin half of the Android -> Admin -> Android hand-off.
 *
 * `scripts/e2e-android.sh` runs it between the two halves the app plays
 * (`HandoffConnectedTest` in apps/android/jvm-verification): the owner has submitted a
 * facility with its evidence, stored in the private bucket of a real MinIO; here an operator
 * opens that evidence and approves; afterwards the app finds the facility in public
 * discovery. On its own, as in `scripts/e2e.sh`, there is no submission to review, so it is
 * skipped.
 */

const NAME = "e2e-m-تسليم للمراجعة";
const REQUIREMENT = "e2e-m-ترخيص مزاولة (اختبار)";

test.skip(!process.env.E2E_HANDOFF, "runs inside scripts/e2e-android.sh, after the owner submits");
test.use({ storageState: FULL_STATE });

test("an operator opens the owner's evidence from private storage, audited, and approves", async ({
  page,
}) => {
  await openConsole(page);
  await page.goto("/reviews");
  await page.getByRole("link", { name: NAME }).click();
  await expect(page).toHaveURL(/\/reviews\/[0-9a-f-]+$/);
  const applicationId = page.url().split("/").pop()!;

  const evidence = page.getByTestId("evidence-list");
  await expect(evidence).toContainText(REQUIREMENT);
  const link = evidence.locator('a[data-testid^="evidence-"]');
  await expect(link).toHaveCount(1);
  const href = (await link.getAttribute("href"))!;
  expect(href).toMatch(/^\/api\/admin\/evidence\/[0-9a-f-]+$/);
  const evidenceId = href.split("/").pop()!;

  // The document, as the operator's browser receives it: the owner's JPEG, from this origin,
  // typed, never cached, and named after nothing in storage.
  const response = await page.request.get(href);
  expect(response.status()).toBe(200);
  const headers = response.headers();
  expect(headers["content-type"]).toBe("image/jpeg");
  expect(headers["cache-control"]).toContain("no-store");
  expect(headers["x-content-type-options"]).toBe("nosniff");
  expect(JSON.stringify(headers)).not.toMatch(/facilities\/|directory-private|X-Amz-/);
  const bytes = await response.body();
  expect([...bytes.subarray(0, 3)]).toEqual([0xff, 0xd8, 0xff]);

  // Opening it is a recorded event, tied to that document.
  const audit = await readOperation<{ items: { action: string; targetId: string }[] }>(
    page,
    "audit",
    { action: "verification_evidence.viewed", resource: evidenceId },
  );
  expect(audit.items.length).toBeGreaterThan(0);

  // Neither the page nor what it loaded names the private object.
  const detail = await readOperation<Record<string, unknown>>(page, "review", {
    id: applicationId,
  });
  for (const text of [JSON.stringify(detail), await page.content()]) {
    expect(text).not.toMatch(/facilities\/[0-9a-f-]+\/evidence\//);
    expect(text).not.toContain("directory-private");
    expect(text).not.toContain("X-Amz-");
  }

  await page.getByTestId("approve").click();
  await page.getByTestId("confirm-accept").click();

  await expect(page.getByTestId("toast")).toContainText("تم قبول الطلب");
  await expect(page.getByTestId("status-badge").first()).toContainText("مقبول");
  const approved = await readOperation<{ items: { action: string }[] }>(page, "audit", {
    action: "facility_application.approved",
    resource: applicationId,
  });
  expect(approved.items.length).toBe(1);
});
