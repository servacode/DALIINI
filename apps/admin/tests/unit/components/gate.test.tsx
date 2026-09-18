import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import {
  ConfirmDialog,
  DataTable,
  DiffViewer,
  ErrorState,
  FilterBar,
  PermissionGate,
} from "../../../components/ui";

/**
 * The components that decide what an operator can see and what they must confirm.
 *
 * `PermissionGate` is presentation. Hiding a control is a courtesy, not a boundary — the
 * backend re-checks every call — but it has to be right, or an operator spends their day
 * clicking things that answer 403.
 */

afterEach(cleanup);

describe("PermissionGate", () => {
  it("renders the control when the operator holds the code", () => {
    render(
      <PermissionGate permissions={["admin.reviews.decide"]} require="admin.reviews.decide">
        <button type="button">قبول</button>
      </PermissionGate>,
    );

    expect(screen.getByRole("button", { name: "قبول" })).toBeTruthy();
  });

  it("renders nothing when the operator does not", () => {
    render(
      <PermissionGate permissions={["admin.reviews.read"]} require="admin.reviews.decide">
        <button type="button">قبول</button>
      </PermissionGate>,
    );

    expect(screen.queryByRole("button", { name: "قبول" })).toBeNull();
  });

  it("does not treat a related code as the one required", () => {
    render(
      <PermissionGate permissions={["admin.reviews.read"]} require="admin.reviews">
        <span>مخفي</span>
      </PermissionGate>,
    );

    expect(screen.queryByText("مخفي")).toBeNull();
  });

  it("shows the fallback instead, where one is given", () => {
    render(
      <PermissionGate
        permissions={[]}
        require="admin.evidence.read"
        fallback={<span>لا تملك صلاحية عرض المستندات</span>}
      >
        <a href="/x">عرض</a>
      </PermissionGate>,
    );

    expect(screen.getByText("لا تملك صلاحية عرض المستندات")).toBeTruthy();
    expect(screen.queryByRole("link")).toBeNull();
  });
});

describe("ConfirmDialog", () => {
  it("renders nothing until it is opened", () => {
    render(
      <ConfirmDialog
        open={false}
        title="إغلاق"
        confirmLabel="تأكيد"
        onConfirm={vi.fn()}
        onCancel={vi.fn()}
      />,
    );

    expect(screen.queryByTestId("confirm-dialog")).toBeNull();
  });

  it("does not act until the operator confirms", () => {
    const onConfirm = vi.fn();
    render(
      <ConfirmDialog
        open
        title="إغلاق المنشأة"
        confirmLabel="تأكيد الإغلاق"
        onConfirm={onConfirm}
        onCancel={vi.fn()}
      />,
    );

    expect(onConfirm).not.toHaveBeenCalled();
    fireEvent.click(screen.getByTestId("confirm-accept"));
    expect(onConfirm).toHaveBeenCalledTimes(1);
  });

  it("locks both buttons while the mutation is in flight, so it cannot double-fire", () => {
    render(
      <ConfirmDialog
        open
        pending
        title="إغلاق"
        confirmLabel="تأكيد"
        onConfirm={vi.fn()}
        onCancel={vi.fn()}
      />,
    );

    expect(screen.getByTestId("confirm-accept").hasAttribute("disabled")).toBe(true);
    expect(screen.getByTestId("confirm-cancel").hasAttribute("disabled")).toBe(true);
  });

  it("shows the failure in place rather than closing on a refusal", () => {
    render(
      <ConfirmDialog
        open
        title="إغلاق"
        confirmLabel="تأكيد"
        error={{
          code: "PERMISSION_DENIED",
          message: "",
          details: {},
          requestId: "req-9",
        }}
        onConfirm={vi.fn()}
        onCancel={vi.fn()}
      />,
    );

    expect(screen.getByTestId("confirm-dialog")).toBeTruthy();
    expect(screen.getByText("لا تملك الصلاحية لهذا الإجراء.")).toBeTruthy();
    expect(screen.getByText("req-9")).toBeTruthy();
  });

  it("is a modal dialog with an accessible name", () => {
    render(
      <ConfirmDialog
        open
        title="حظر الحساب"
        confirmLabel="تأكيد"
        onConfirm={vi.fn()}
        onCancel={vi.fn()}
      />,
    );

    const dialog = screen.getByRole("dialog");
    expect(dialog.getAttribute("aria-modal")).toBe("true");
    expect(screen.getByRole("heading", { name: "حظر الحساب" })).toBeTruthy();
  });
});

describe("ErrorState", () => {
  it("shows the mapped sentence and the correlation id, and nothing technical", () => {
    render(
      <ErrorState
        error={{
          code: "INTERNAL_ERROR",
          message: "",
          details: {},
          requestId: "abc-123",
        }}
      />,
    );

    const text = screen.getByTestId("error-state").textContent ?? "";
    expect(text).toContain("حدث خطأ مؤقت");
    expect(text).toContain("abc-123");
    expect(text).not.toContain("INTERNAL_ERROR");
  });
});

describe("FilterBar", () => {
  it("lifts values on submit, not on every keystroke", () => {
    const onApply = vi.fn();
    render(
      <FilterBar
        fields={[{ name: "q", label: "بحث" }]}
        values={{ q: "" }}
        onApply={onApply}
      />,
    );

    fireEvent.change(screen.getByTestId("filter-q"), { target: { value: "صيدلية" } });
    expect(onApply).not.toHaveBeenCalled();

    fireEvent.submit(screen.getByTestId("filter-bar"));
    expect(onApply).toHaveBeenCalledWith({ q: "صيدلية" });
  });

  it("offers a clear action only while a filter is active", () => {
    const { rerender } = render(
      <FilterBar fields={[{ name: "q", label: "بحث" }]} values={{ q: "" }} onApply={vi.fn()} />,
    );
    expect(screen.queryByTestId("filter-clear")).toBeNull();

    rerender(
      <FilterBar fields={[{ name: "q", label: "بحث" }]} values={{ q: "x" }} onApply={vi.fn()} />,
    );
    expect(screen.getByTestId("filter-clear")).toBeTruthy();
  });
});

describe("DataTable", () => {
  it("shows an empty state rather than an empty grid", () => {
    render(
      <DataTable
        caption="المنشآت"
        columns={[{ key: "a", header: "الاسم", render: () => null }]}
        rows={[]}
        rowKey={() => "x"}
      />,
    );

    expect(screen.getByTestId("empty-state")).toBeTruthy();
    expect(screen.queryByTestId("data-table")).toBeNull();
  });
});

describe("DiffViewer", () => {
  it("shows only what changed", () => {
    render(<DiffViewer before={{ active: false, name: "x" }} after={{ active: true, name: "x" }} />);

    const text = screen.getByTestId("diff-viewer").textContent ?? "";
    expect(text).toContain("active");
    expect(text).not.toContain("name");
  });

  it("says so when nothing changed", () => {
    render(<DiffViewer before={{ a: 1 }} after={{ a: 1 }} />);

    expect(screen.getByText("لا تغييرات مسجّلة.")).toBeTruthy();
  });

  it("passes a redacted value through rather than hiding the redaction", () => {
    render(<DiffViewer before={{ token: "x" }} after={{ token: "[REDACTED]" }} />);

    expect(screen.getByTestId("diff-viewer").textContent).toContain("[REDACTED]");
  });
});
