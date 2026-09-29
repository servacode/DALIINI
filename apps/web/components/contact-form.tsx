"use client";

import Link from "next/link";
import { useEffect, useRef, useState, useSyncExternalStore, type FormEvent, type ReactNode } from "react";
import { CONTACT_KINDS, MESSAGE_MAX, NAME_MAX, PHONE_MAX, parseContactKind, type ContactKind } from "../lib/contact";

/*
 * The contact form: the one request the site sends from the visitor's browser.
 * It posts straight to the API (POST /api/v1/contact/), never through the
 * site's server: the API allows each sender a few messages an hour, counted by
 * address, and through the server every visitor would share one address and
 * one limit. The API must list this site's origin in CORS_ALLOWED_ORIGINS; the
 * build adds the API's origin to the CSP's connect-src.
 *
 * The send button stays disabled until this script runs, so a message is never
 * submitted as a plain form that would go nowhere.
 */

type Field = "name" | "phone" | "message";
type FieldErrors = Partial<Record<Field, string>>;
type Failure = { text: string; requestId?: string };
type Outcome = { ok: true } | { ok: false; failure: Failure | null; fields?: FieldErrors };

const FIELDS: Field[] = ["name", "phone", "message"];

const FIELD_ERRORS: Record<Field, string> = {
  name: "اكتب اسمك.",
  /* One unbroken number: digit groups split by spaces would be laid out right to left in Arabic text. */
  phone: "اكتب رقم جوال سوري صحيح، مثل 0933123456، أو اترك الحقل فارغاً.",
  message: "اكتب رسالتك.",
};

const OFFLINE = "تعذّر الاتصال. تحقق من الشبكة وحاول مرة أخرى.";
const THROTTLED = "أرسلت عدة رسائل، حاول بعد ساعة.";
const MAINTENANCE = "الخدمة في صيانة الآن. حاول بعد قليل.";
const FAILED = "تعذّر إرسال رسالتك الآن. حاول مرة أخرى بعد قليل.";

/* «١٢٣»: the same digits the rest of the page uses, without the platform's number formatting. */
const arabicDigits = (n: number) => String(n).replace(/\d/g, (d) => "٠١٢٣٤٥٦٧٨٩"[Number(d)]);

/* Arabic-Indic and Persian digits typed on an Arabic keyboard, as ASCII. */
const asciiDigits = (s: string) =>
  s.replace(/[٠-٩]/g, (d) => String(d.charCodeAt(0) - 0x0660)).replace(/[۰-۹]/g, (d) => String(d.charCodeAt(0) - 0x06f0));

/* The API's rule (accounts/phone.py): a Syrian mobile, returned as +9639XXXXXXXX; null when it is not one. */
function syrianMobile(raw: string): string | null {
  let compact = asciiDigits(raw).replace(/[\s()-]/g, "");
  if (compact.startsWith("00963")) compact = `+963${compact.slice(5)}`;
  else if (compact.startsWith("963")) compact = `+${compact}`;
  else if (compact.startsWith("09")) compact = `+963${compact.slice(1)}`;
  return /^\+9639\d{8}$/.test(compact) ? compact : null;
}

async function send(endpoint: string, body: Record<string, string>): Promise<Outcome> {
  let response: Response;
  try {
    response = await fetch(endpoint, {
      method: "POST",
      mode: "cors",
      credentials: "omit",
      cache: "no-store",
      headers: { "Content-Type": "application/json", Accept: "application/json" },
      body: JSON.stringify(body),
    });
  } catch {
    return { ok: false, failure: { text: OFFLINE } };
  }
  if (response.ok) return { ok: true };
  const envelope = (await response.json().catch(() => null)) as {
    code?: string;
    message?: string;
    details?: Record<string, unknown>;
    requestId?: string;
  } | null;
  if (response.status === 429) return { ok: false, failure: { text: THROTTLED } };
  if (response.status === 503 && envelope?.code === "MAINTENANCE") {
    return { ok: false, failure: { text: envelope.message?.trim() || MAINTENANCE } };
  }
  if (response.status === 400 && envelope?.details) {
    const fields: FieldErrors = {};
    for (const field of FIELDS) if (envelope.details[field]) fields[field] = FIELD_ERRORS[field];
    if (Object.keys(fields).length > 0) return { ok: false, failure: null, fields };
  }
  return { ok: false, failure: { text: FAILED, requestId: envelope?.requestId || undefined } };
}

const subscribeNothing = () => () => {};

/* false while the server-rendered form is not yet interactive, true once this script runs. */
function useHydrated(): boolean {
  return useSyncExternalStore(subscribeNothing, () => true, () => false);
}

export function ContactForm({
  endpoint,
  initialKind,
  initialMessage = "",
  successArt,
}: {
  endpoint: string;
  initialKind: ContactKind;
  initialMessage?: string;
  successArt?: ReactNode;
}) {
  const hydrated = useHydrated();
  const [message, setMessage] = useState(initialMessage);
  const [phase, setPhase] = useState<"idle" | "sending" | "sent">("idle");
  const [errors, setErrors] = useState<FieldErrors>({});
  const [failure, setFailure] = useState<Failure | null>(null);
  const [formKey, setFormKey] = useState(0);
  const formRef = useRef<HTMLFormElement>(null);
  const sentRef = useRef<HTMLHeadingElement>(null);
  /* Where focus goes once the next render is on screen: a field in error, the confirmation, or the form again. */
  const focusNext = useRef<Field | "sent" | null>(null);

  useEffect(() => {
    const target = focusNext.current;
    if (!target) return;
    focusNext.current = null;
    if (target === "sent") sentRef.current?.focus();
    else formRef.current?.querySelector<HTMLElement>(`[name="${target}"]`)?.focus();
  });

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (phase === "sending") return;
    const data = new FormData(event.currentTarget);
    const name = String(data.get("name") ?? "").trim();
    const rawPhone = String(data.get("phone") ?? "").trim();
    const phone = rawPhone ? syrianMobile(rawPhone) : "";
    const text = message.trim();
    const kind = parseContactKind(String(data.get("kind") ?? "")) ?? "GENERAL";

    const found: FieldErrors = {};
    if (!name) found.name = FIELD_ERRORS.name;
    if (phone === null) found.phone = FIELD_ERRORS.phone;
    if (!text) found.message = FIELD_ERRORS.message;
    setErrors(found);
    setFailure(null);
    const invalid = FIELDS.find((f) => found[f]);
    if (invalid) {
      focusNext.current = invalid;
      return;
    }

    setPhase("sending");
    const outcome = await send(endpoint, { name, message: text, kind, ...(phone ? { phone } : {}) });
    if (outcome.ok) {
      setPhase("sent");
      focusNext.current = "sent";
      return;
    }
    setPhase("idle");
    setErrors(outcome.fields ?? {});
    setFailure(outcome.failure);
    focusNext.current = FIELDS.find((f) => outcome.fields?.[f]) ?? null;
  }

  function again() {
    setMessage("");
    setErrors({});
    setFailure(null);
    setFormKey((k) => k + 1);
    setPhase("idle");
    focusNext.current = "name";
  }

  if (phase === "sent") {
    return (
      <div className="card state contact-sent">
        {successArt}
        <h2 ref={sentRef} tabIndex={-1}>وصلت رسالتك</h2>
        <p>شكراً لك. يقرأ فريقنا كل رسالة، وإن تركت رقمك فقد نتصل بك.</p>
        <div className="actions">
          <button type="button" className="button button-alt" onClick={again}>أرسل رسالة أخرى</button>
          <Link className="button button-alt" href="/">العودة إلى الرئيسية</Link>
        </div>
      </div>
    );
  }

  const sending = phase === "sending";
  const describedBy = (field: Field, hint?: string) =>
    [hint, errors[field] ? `contact-${field}-error` : undefined].filter(Boolean).join(" ") || undefined;

  return (
    <form key={formKey} ref={formRef} className="card contact-form" method="post" noValidate onSubmit={submit} aria-busy={sending || undefined}>
      <div className="field">
        <label htmlFor="contact-name">الاسم</label>
        <input
          id="contact-name"
          name="name"
          type="text"
          autoComplete="name"
          maxLength={NAME_MAX}
          aria-invalid={errors.name ? true : undefined}
          aria-describedby={describedBy("name")}
        />
        {errors.name ? <p id="contact-name-error" className="field-error">{errors.name}</p> : null}
      </div>

      <div className="field">
        <label htmlFor="contact-phone">رقم الجوال <span className="optional">(اختياري)</span></label>
        <input
          id="contact-phone"
          name="phone"
          type="tel"
          inputMode="tel"
          autoComplete="tel"
          dir="ltr"
          maxLength={PHONE_MAX}
          placeholder="09XX XXX XXX"
          aria-invalid={errors.phone ? true : undefined}
          aria-describedby={describedBy("phone", "contact-phone-hint")}
        />
        <p id="contact-phone-hint" className="field-hint">اكتبه إن أردت أن نتصل بك.</p>
        {errors.phone ? <p id="contact-phone-error" className="field-error">{errors.phone}</p> : null}
      </div>

      <div className="field">
        <label htmlFor="contact-kind">موضوع الرسالة</label>
        <select id="contact-kind" name="kind" defaultValue={initialKind}>
          {CONTACT_KINDS.map((k) => <option key={k.value} value={k.value}>{k.label}</option>)}
        </select>
      </div>

      <div className="field">
        <label htmlFor="contact-message">رسالتك</label>
        <textarea
          id="contact-message"
          name="message"
          rows={6}
          maxLength={MESSAGE_MAX}
          value={message}
          onChange={(e) => setMessage(e.target.value)}
          aria-invalid={errors.message ? true : undefined}
          aria-describedby={describedBy("message", "contact-message-count")}
        />
        <p id="contact-message-count" className="field-hint counter" data-near={MESSAGE_MAX - message.length <= 100 || undefined}>
          <span className="sr-only">عدد الأحرف: </span>
          {arabicDigits(message.length)} / {arabicDigits(MESSAGE_MAX)}
        </p>
        {errors.message ? <p id="contact-message-error" className="field-error">{errors.message}</p> : null}
      </div>

      {failure ? (
        <div className="form-error" role="alert">
          <p>{failure.text}</p>
          {failure.requestId ? (
            <p className="muted">معرّف الطلب للدعم: <span className="ltr">{failure.requestId}</span></p>
          ) : null}
        </div>
      ) : null}

      <button type="submit" className="button" disabled={!hydrated || sending}>
        {sending ? "جارٍ الإرسال…" : "أرسل الرسالة"}
      </button>
      <noscript>
        <p className="muted">يحتاج إرسال الرسالة إلى تفعيل JavaScript في المتصفح.</p>
      </noscript>
    </form>
  );
}
