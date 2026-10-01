import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { useState } from "react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { RejectionTemplatePicker } from "../../../components/rejection-template-picker";
import { ConfirmDialog } from "../../../components/ui";
import { CharCount, SidePanel, Trend, percentChange } from "../../../components/ui/extra";

/**
 * The shared pieces the operations screens are built from: the change against a previous
 * period, the character counter, the side panel, and the rejection-template picker the
 * review screen mounts in its reject dialog.
 */

vi.mock("next/navigation", () => ({
  useRouter: () => ({ replace: vi.fn(), refresh: vi.fn() }),
}));

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
});

describe("percentChange and Trend", () => {
  it("computes the change, and refuses to invent one from nothing", () => {
    expect(percentChange(120, 100)).toBe(20);
    expect(percentChange(50, 100)).toBe(-50);
    expect(percentChange(0, 0)).toBe(0);
    expect(percentChange(5, 0)).toBeNull();
    expect(percentChange(null, 3)).toBeNull();
  });

  it("colours a rise as good by default, and as bad when lower is better", () => {
    const { rerender } = render(<Trend current={120} previous={100} />);
    expect(screen.getByTestId("trend").dataset.tone).toBe("positive");
    expect(screen.getByTestId("trend").textContent).toContain("ارتفاع");

    rerender(<Trend current={120} previous={100} lowerIsBetter />);
    expect(screen.getByTestId("trend").dataset.tone).toBe("negative");

    rerender(<Trend current={80} previous={100} lowerIsBetter />);
    expect(screen.getByTestId("trend").dataset.tone).toBe("positive");
    expect(screen.getByTestId("trend").textContent).toContain("انخفاض");
  });

  it("says there is no comparison rather than showing an infinite rise", () => {
    render(<Trend current={7} previous={0} />);
    expect(screen.getByTestId("trend").textContent).toBe("لا مقارنة");
  });
});

describe("CharCount", () => {
  it("counts characters as the backend does and flags the overflow", () => {
    const { container, rerender } = render(<CharCount value="أهلاً" max={5} />);
    expect(container.querySelector(".char-count")?.hasAttribute("data-over")).toBe(false);

    rerender(<CharCount value="أهلاً بك" max={5} />);
    expect(container.querySelector(".char-count")?.getAttribute("data-over")).toBe("true");
  });
});

describe("SidePanel", () => {
  it("closes on Escape, but not while locked", () => {
    const onClose = vi.fn();
    const { rerender } = render(
      <SidePanel open title="السبت" onClose={onClose} locked>
        <p>محتوى</p>
      </SidePanel>,
    );

    fireEvent.keyDown(document, { key: "Escape" });
    expect(onClose).not.toHaveBeenCalled();

    rerender(
      <SidePanel open title="السبت" onClose={onClose}>
        <p>محتوى</p>
      </SidePanel>,
    );
    fireEvent.keyDown(document, { key: "Escape" });
    expect(onClose).toHaveBeenCalledTimes(1);
  });

  it("is a modal dialog named by its title", () => {
    render(
      <SidePanel open title="جاهزية دمشق" onClose={vi.fn()}>
        <p>محتوى</p>
      </SidePanel>,
    );

    const dialog = screen.getByRole("dialog", { name: "جاهزية دمشق" });
    expect(dialog.getAttribute("aria-modal")).toBe("true");
  });
});

describe("RejectionTemplatePicker", () => {
  it("offers the active templates and hands the chosen text to the screen", async () => {
    const fetchMock = vi.fn<(input: RequestInfo | URL) => Promise<Response>>(async () =>
      new Response(
        JSON.stringify({
          items: [
            { id: "t-1", titleAr: "الترخيص غير واضح", bodyAr: "صورة الترخيص غير مقروءة.", active: true, sortOrder: 10 },
            { id: "t-2", titleAr: "الموقع غير مطابق", bodyAr: "الموقع على الخريطة لا يطابق العنوان.", active: true, sortOrder: 20 },
          ],
        }),
        { headers: { "Content-Type": "application/json" } },
      ),
    );
    vi.stubGlobal("fetch", fetchMock);
    const onPick = vi.fn();

    render(<RejectionTemplatePicker onPick={onPick} />);
    const select = await screen.findByTestId("rejection-template");
    fireEvent.change(select, { target: { value: "t-2" } });

    expect(String(fetchMock.mock.calls[0]?.[0])).toBe("/api/admin/rejectionTemplates?active=true");
    expect(onPick).toHaveBeenCalledWith(
      "الموقع على الخريطة لا يطابق العنوان.",
      expect.objectContaining({ id: "t-2" }),
    );
    expect((select as HTMLSelectElement).value).toBe("");
  });

  it("stays out of the way when there is nothing to offer", async () => {
    const fetchMock = vi.fn(async () =>
      new Response(JSON.stringify({ code: "PERMISSION_DENIED", message: "", details: {}, requestId: "" }), {
        status: 403,
        headers: { "Content-Type": "application/json" },
      }),
    );
    vi.stubGlobal("fetch", fetchMock);

    const { container } = render(<RejectionTemplatePicker onPick={vi.fn()} />);
    await waitFor(() => expect(fetchMock).toHaveBeenCalled());

    expect(container.innerHTML).toBe("");
  });
});

describe("ConfirmDialog with a form inside", () => {
  function Screen({ pending = false, onCancel }: { pending?: boolean; onCancel: () => void }) {
    const [value, setValue] = useState("");
    return (
      <ConfirmDialog
        open
        title="ملاحظة"
        confirmLabel="حفظ"
        pending={pending}
        onConfirm={() => undefined}
        // A new function on every render, as every screen passes it.
        onCancel={() => onCancel()}
      >
        <input data-testid="note" value={value} onChange={(event) => setValue(event.target.value)} />
      </ConfirmDialog>
    );
  }

  it("keeps focus in the field being typed in while the screen re-renders", () => {
    render(<Screen onCancel={vi.fn()} />);
    const input = screen.getByTestId("note");

    input.focus();
    fireEvent.change(input, { target: { value: "أ" } });
    fireEvent.change(input, { target: { value: "أه" } });

    expect(document.activeElement).toBe(input);
    expect((input as HTMLInputElement).value).toBe("أه");
  });

  it("still cancels on Escape, and not while the action is in flight", () => {
    const onCancel = vi.fn();
    const { rerender } = render(<Screen pending onCancel={onCancel} />);

    fireEvent.keyDown(document, { key: "Escape" });
    expect(onCancel).not.toHaveBeenCalled();

    rerender(<Screen onCancel={onCancel} />);
    fireEvent.keyDown(document, { key: "Escape" });
    expect(onCancel).toHaveBeenCalledTimes(1);
  });
});
