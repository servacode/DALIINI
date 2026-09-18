import { describe, expect, it } from "vitest";

import {
  type ApiErrorBody,
  fieldErrorsFor,
  isPermissionDenied,
  isSessionExpired,
  messageFor,
  requestIdFor,
} from "../../lib/errors/messages";

/**
 * The error mapper is the only place a code becomes a sentence.
 *
 * Without it, thirteen screens would each carry a `switch` and the same refusal would read
 * differently depending on where an operator happened to hit it.
 */

function envelope(overrides: Partial<ApiErrorBody> = {}): ApiErrorBody {
  return { code: "INTERNAL_ERROR", message: "", details: {}, requestId: "", ...overrides };
}

describe("messageFor", () => {
  it.each([
    ["AUTHENTICATION_FAILED", "بيانات الدخول غير صحيحة."],
    ["AUTHENTICATION_REQUIRED", "انتهت الجلسة. سجّل الدخول من جديد."],
    ["PERMISSION_DENIED", "لا تملك الصلاحية لهذا الإجراء."],
  ])("gives %s one application-wide phrase", (code, expected) => {
    expect(messageFor(envelope({ code }))).toBe(expected);
  });

  it("prefers the backend message for a domain code, which only it can explain", () => {
    const message = messageFor(
      envelope({ code: "LAST_OWNER_PROTECTED", message: "لا يمكن ترك المنشأة بلا مالك." }),
    );

    expect(message).toBe("لا يمكن ترك المنشأة بلا مالك.");
  });

  it("falls back rather than showing an empty box", () => {
    expect(messageFor(envelope({ code: "SOMETHING_NEW", message: "" }))).toBe(
      "حدث خطأ مؤقت. حاول مرة أخرى.",
    );
    expect(messageFor(null)).toBe("حدث خطأ مؤقت. حاول مرة أخرى.");
  });

  it("never returns a transport code as the message a person reads", () => {
    for (const code of ["VALIDATION_ERROR", "NOT_FOUND", "THROTTLED", "INTERNAL_ERROR"]) {
      expect(messageFor(envelope({ code }))).not.toContain(code);
    }
  });
});

describe("fieldErrorsFor", () => {
  it("flattens the details map into one message per field", () => {
    const errors = fieldErrorsFor(
      envelope({ details: { phone: ["مطلوب.", "غير صالح."], password: ["مطلوب."] } }),
    );

    expect(errors).toEqual({ phone: "مطلوب. غير صالح.", password: "مطلوب." });
  });

  it("keeps the nested paths the backend produces", () => {
    const errors = fieldErrorsFor(envelope({ details: { "contacts[1].phone": ["غير صالح."] } }));

    expect(errors["contacts[1].phone"]).toBe("غير صالح.");
  });

  it("is empty when there is nothing field-scoped", () => {
    expect(fieldErrorsFor(envelope())).toEqual({});
    expect(fieldErrorsFor(null)).toEqual({});
  });
});

describe("classification", () => {
  it("recognises an expired session, which is what triggers the redirect", () => {
    expect(isSessionExpired(envelope({ code: "AUTHENTICATION_REQUIRED" }))).toBe(true);
    expect(isSessionExpired(envelope({ code: "PERMISSION_DENIED" }))).toBe(false);
  });

  it("keeps a permission refusal distinct from a session failure", () => {
    expect(isPermissionDenied(envelope({ code: "PERMISSION_DENIED" }))).toBe(true);
    expect(isSessionExpired(envelope({ code: "PERMISSION_DENIED" }))).toBe(false);
  });
});

describe("requestIdFor", () => {
  it("returns the id so a support report can quote it", () => {
    expect(requestIdFor(envelope({ requestId: "abc-123" }))).toBe("abc-123");
  });

  it("returns null rather than an empty label", () => {
    expect(requestIdFor(envelope({ requestId: "   " }))).toBeNull();
    expect(requestIdFor(null)).toBeNull();
  });
});
