import { cleanup, fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import TaxonomyTagsPage from "../../../app/(console)/taxonomy/tags/page";

/**
 * «التخصصات والخدمات», against a stubbed BFF: which category opens, what each row offers,
 * and the loading, empty and error states. Delete is offered only for an item no facility
 * has chosen; one in use offers «أوقف» instead.
 */

let canManage = true;

vi.mock("next/navigation", () => ({
  useRouter: () => ({ replace: vi.fn(), refresh: vi.fn() }),
  usePathname: () => "/taxonomy/tags",
  useSearchParams: () => new URLSearchParams(),
}));

vi.mock("../../../components/admin-shell", () => ({
  useCan: () => canManage,
}));

const CATEGORIES = {
  items: [
    { id: "c-old", nameAr: "قديم", specialization: "GENERIC", active: false },
    { id: "c-clinic", nameAr: "عيادات", specialization: "MEDICAL_CLINIC", active: true },
    { id: "c-lab", nameAr: "مخابر", specialization: "GENERIC", active: true },
  ],
};

const SPECIALTIES = {
  items: [
    {
      id: 1,
      scope: "SPECIALIZATION",
      categoryId: null,
      specialization: "MEDICAL_CLINIC",
      nameAr: "عامة",
      nameEn: "",
      active: true,
      sortOrder: 10,
      facilityCount: 3,
    },
    {
      id: 2,
      scope: "CATEGORY",
      categoryId: "c-clinic",
      specialization: null,
      nameAr: "قلبية",
      nameEn: "Cardiology",
      active: true,
      sortOrder: 20,
      facilityCount: 0,
    },
  ],
};

type Handler = (url: URL, init?: RequestInit) => { status?: number; body: unknown };

function stubBff(handler: Handler) {
  const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = new URL(String(input), "http://console.test");
    const { status = 200, body } = handler(url, init);
    return new Response(JSON.stringify(body), {
      status,
      headers: { "Content-Type": "application/json" },
    });
  });
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

function lists(overrides: Partial<Record<string, { status?: number; body: unknown }>> = {}): Handler {
  return (url) => {
    const name = url.pathname.replace("/api/admin/", "");
    if (overrides[name]) return overrides[name]!;
    if (name === "categories") return { body: CATEGORIES };
    if (name === "categorySpecialties") return { body: SPECIALTIES };
    if (name === "categoryServiceTags") return { body: { items: [] } };
    return { body: {} };
  };
}

beforeEach(() => {
  canManage = true;
});

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
});

describe("TaxonomyTagsPage", () => {
  it("opens on the first active category and lists its specialties and services", async () => {
    const fetchMock = stubBff(lists());

    render(<TaxonomyTagsPage />);

    const specialties = await screen.findByTestId("specialty-panel");
    await within(specialties).findByText("قلبية");
    expect((screen.getByTestId("tags-category") as HTMLSelectElement).value).toBe("c-clinic");
    expect(within(specialties).getByText("كل العيادات")).toBeTruthy();
    expect(within(specialties).getByText("٣ منشآت")).toBeTruthy();
    expect(within(specialties).getByText("غير مستخدم")).toBeTruthy();
    const services = screen.getByTestId("service-panel");
    await within(services).findByText("لا خدمات لهذا التصنيف بعد");
    const asked = fetchMock.mock.calls.map(([input]) => String(input));
    expect(asked).toContain("/api/admin/categorySpecialties?id=c-clinic");
    expect(asked).toContain("/api/admin/categoryServiceTags?id=c-clinic");
  });

  it("offers delete only for an item no facility has chosen", async () => {
    stubBff(lists());

    render(<TaxonomyTagsPage />);

    await screen.findByText("قلبية");
    expect(screen.queryByTestId("specialty-delete-1")).toBeNull();
    expect(screen.getByTestId("specialty-toggle-1").textContent).toBe("أوقف");
    expect(screen.getByTestId("specialty-delete-2")).toBeTruthy();
  });

  it("creates a specialty in the chosen scope for the category", async () => {
    const fetchMock = stubBff(lists());

    render(<TaxonomyTagsPage />);
    await screen.findByText("قلبية");
    fireEvent.click(screen.getByTestId("specialty-add"));
    fireEvent.click(screen.getByTestId("tag-scope-SPECIALIZATION"));
    fireEvent.change(screen.getByTestId("tag-name-ar"), { target: { value: " أطفال " } });
    fireEvent.click(screen.getByTestId("confirm-accept"));

    await waitFor(() =>
      expect(fetchMock.mock.calls.some(([input]) => String(input) === "/api/admin/specialtyCreate")).toBe(
        true,
      ),
    );
    const [, init] = fetchMock.mock.calls.find(([input]) => String(input) === "/api/admin/specialtyCreate")!;
    expect(init?.method).toBe("POST");
    expect(JSON.parse(String(init?.body))).toEqual({
      categoryId: "c-clinic",
      nameAr: "أطفال",
      nameEn: "",
      sortOrder: 30,
      scope: "SPECIALIZATION",
    });
  });

  it("does not offer to share a specialty from a general category", async () => {
    stubBff(lists({ categorySpecialties: { body: { items: [] } } }));

    render(<TaxonomyTagsPage />);
    await screen.findByText("لا تخصصات لهذا التصنيف بعد");
    fireEvent.change(screen.getByTestId("tags-category"), { target: { value: "c-lab" } });
    await waitFor(() =>
      expect((screen.getByTestId("tags-category") as HTMLSelectElement).value).toBe("c-lab"),
    );
    await screen.findByText("لا تخصصات لهذا التصنيف بعد");
    fireEvent.click(screen.getByTestId("specialty-add"));

    expect(screen.getByTestId("tag-name-ar")).toBeTruthy();
    expect(screen.queryByTestId("tag-scope-SPECIALIZATION")).toBeNull();
  });

  it("shows a failed list with a way to retry, and hides the controls from a reader", async () => {
    canManage = false;
    stubBff(
      lists({
        categoryServiceTags: {
          status: 500,
          body: { code: "INTERNAL_ERROR", message: "x", details: {}, requestId: "r-1" },
        },
      }),
    );

    render(<TaxonomyTagsPage />);

    const services = await screen.findByTestId("service-panel");
    await within(services).findByTestId("error-state");
    expect(within(services).getByText("إعادة المحاولة")).toBeTruthy();
    await screen.findByText("قلبية");
    expect(screen.queryByTestId("specialty-add")).toBeNull();
    expect(screen.queryByTestId("specialty-edit-2")).toBeNull();
  });
});
