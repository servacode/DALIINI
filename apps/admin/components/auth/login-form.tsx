"use client";

import { useRouter } from "next/navigation";
import { useId, useRef, useState } from "react";

type FieldErrors = Readonly<Record<string, readonly string[]>>;

type ErrorBody = Readonly<{
  code?: string;
  message?: string;
  details?: FieldErrors;
}>;

/**
 * The sign-in form.
 *
 * It posts to the same-origin BFF and never sees a token: the response carries `{ok: true}`
 * and the session arrives as `HttpOnly` cookies the page cannot read. Nothing is written to
 * `localStorage` or `sessionStorage`.
 *
 * What it shows on failure is the `message` from the error envelope and the `details` map
 * keyed by field. Neither carries a stack trace, an upstream URL, a token or an exception
 * class — the BFF replaces anything it does not recognise with a generic message before it
 * gets here.
 */
export function LoginForm() {
  const router = useRouter();
  const phoneId = useId();
  const passwordId = useId();
  const errorId = useId();

  const [pending, setPending] = useState(false);
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});
  const [formError, setFormError] = useState<string | null>(null);
  const formErrorRef = useRef<HTMLParagraphElement>(null);

  async function onSubmit(event: React.FormEvent<HTMLFormElement>): Promise<void> {
    event.preventDefault();
    if (pending) return;

    const form = new FormData(event.currentTarget);
    setPending(true);
    setFieldErrors({});
    setFormError(null);

    try {
      const response = await fetch("/api/session/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        // Same-origin only. The BFF refuses anything else, and the CSP forbids the browser
        // from reaching Django directly in any case.
        credentials: "same-origin",
        body: JSON.stringify({
          phone: String(form.get("phone") ?? ""),
          password: String(form.get("password") ?? ""),
        }),
      });

      if (response.ok) {
        // `replace`, not `push`: the login screen should not be a back-button destination
        // once a session exists.
        router.replace("/dashboard");
        router.refresh();
        return;
      }

      const body = (await response.json().catch(() => ({}))) as ErrorBody;
      setFieldErrors(body.details ?? {});
      setFormError(body.message ?? "تعذر تسجيل الدخول. حاول مرة أخرى.");
      formErrorRef.current?.focus();
    } catch {
      setFormError("تعذر الاتصال بالخادم. تحقق من الشبكة وحاول مرة أخرى.");
      formErrorRef.current?.focus();
    } finally {
      setPending(false);
    }
  }

  const phoneErrors = fieldErrors.phone ?? [];
  const passwordErrors = fieldErrors.password ?? [];

  return (
    <form className="login-form" onSubmit={onSubmit} noValidate>
      {formError ? (
        <p
          className="form-error"
          role="alert"
          tabIndex={-1}
          ref={formErrorRef}
          id={errorId}
          data-testid="login-error"
        >
          {formError}
        </p>
      ) : null}

      <div className="field">
        <label htmlFor={phoneId}>رقم الهاتف</label>
        <input
          id={phoneId}
          name="phone"
          type="tel"
          inputMode="tel"
          dir="ltr"
          autoComplete="username"
          required
          disabled={pending}
          aria-invalid={phoneErrors.length > 0}
          aria-describedby={phoneErrors.length > 0 ? `${phoneId}-error` : undefined}
          data-testid="login-phone"
        />
        {phoneErrors.length > 0 ? (
          <p className="field-error" id={`${phoneId}-error`}>
            {phoneErrors.join(" ")}
          </p>
        ) : null}
      </div>

      <div className="field">
        <label htmlFor={passwordId}>كلمة المرور</label>
        <input
          id={passwordId}
          name="password"
          type="password"
          autoComplete="current-password"
          required
          disabled={pending}
          aria-invalid={passwordErrors.length > 0}
          aria-describedby={passwordErrors.length > 0 ? `${passwordId}-error` : undefined}
          data-testid="login-password"
        />
        {passwordErrors.length > 0 ? (
          <p className="field-error" id={`${passwordId}-error`}>
            {passwordErrors.join(" ")}
          </p>
        ) : null}
      </div>

      <button type="submit" className="button-primary" disabled={pending} data-testid="login-submit">
        {pending ? "جارٍ تسجيل الدخول…" : "تسجيل الدخول"}
      </button>

      <p className="muted login-note">
        الجلسة تُحفظ في كوكيز آمنة على الخادم. لا يُخزَّن أي رمز وصول في المتصفح.
      </p>
    </form>
  );
}
