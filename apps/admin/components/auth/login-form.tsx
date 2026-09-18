"use client";

import { useRouter } from "next/navigation";
import { useId, useRef, useState, useSyncExternalStore } from "react";

import { type ApiErrorBody, fieldErrorsFor, messageFor } from "../../lib/errors/messages";

type FieldErrors = Readonly<Record<string, string>>;

// True only once React owns the page. On the server and during hydration the snapshot is
// false, so the submit button renders disabled until the handler below is attached.
const subscribeNever = () => () => undefined;
function useHydrated(): boolean {
  return useSyncExternalStore(
    subscribeNever,
    () => true,
    () => false,
  );
}

/**
 * The sign-in form.
 *
 * It posts to the same-origin BFF and never sees a token: the response carries `{ok: true}`
 * and the session arrives as `HttpOnly` cookies the page cannot read. Nothing is written to
 * `localStorage` or `sessionStorage`.
 *
 * Failures go through the central mapper in `lib/errors/messages.ts`, like every other
 * screen, so a wrong password reads the same here as anywhere else a credential is refused.
 * An earlier version printed the backend `message` directly and so bypassed that mapper.
 *
 * Two guards against the page being used before React has hydrated it. A click that lands
 * before the `onSubmit` handler is attached makes the browser submit the form itself, and a
 * form with no method submits by GET — which put the phone number and the password in the
 * URL, and from there into history, logs and any proxy in between. The first Playwright run
 * under load produced exactly that. So the form declares `method="post"`, which keeps the
 * credentials out of the URL even if JavaScript never runs, and the submit button stays
 * disabled until hydration completes, so the native path is not reachable at all.
 */
export function LoginForm() {
  const router = useRouter();
  const hydrated = useHydrated();
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

      const body = (await response.json().catch(() => null)) as ApiErrorBody | null;
      setFieldErrors(fieldErrorsFor(body));
      setFormError(messageFor(body));
      formErrorRef.current?.focus();
    } catch {
      setFormError(
        messageFor({ code: "NETWORK_UNAVAILABLE", message: "", details: {}, requestId: "" }),
      );
      formErrorRef.current?.focus();
    } finally {
      setPending(false);
    }
  }

  const phoneError = fieldErrors.phone ?? "";
  const passwordError = fieldErrors.password ?? "";

  return (
    <form className="login-form" method="post" onSubmit={onSubmit} noValidate>
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
          aria-invalid={Boolean(phoneError)}
          aria-describedby={phoneError ? `${phoneId}-error` : undefined}
          data-testid="login-phone"
        />
        {phoneError ? (
          <p className="field-error" id={`${phoneId}-error`}>
            {phoneError}
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
          aria-invalid={Boolean(passwordError)}
          aria-describedby={passwordError ? `${passwordId}-error` : undefined}
          data-testid="login-password"
        />
        {passwordError ? (
          <p className="field-error" id={`${passwordId}-error`}>
            {passwordError}
          </p>
        ) : null}
      </div>

      <button
        type="submit"
        className="button-primary"
        disabled={pending || !hydrated}
        data-testid="login-submit"
      >
        {pending ? "جارٍ تسجيل الدخول…" : "تسجيل الدخول"}
      </button>

      <p className="muted login-note">
        الجلسة تُحفظ في كوكيز آمنة على الخادم. لا يُخزَّن أي رمز وصول في المتصفح.
      </p>
    </form>
  );
}
