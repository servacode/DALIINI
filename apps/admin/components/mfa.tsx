"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";

import { logout } from "../lib/client/api";
import { useMutation } from "../lib/client/use-mutation";
import { messageFor } from "../lib/errors/messages";
import { BrandMark } from "./ui";

/** What `/admin/me/` and `accountMfaRetrieve` say about the second sign-in step. */
export type MfaState = Readonly<{
  enabled: boolean;
  required: boolean;
  verified: boolean;
  recoveryCodesLeft: number;
}>;

type Setup = Readonly<{ secret: string; otpauthUri: string; qrSvgDataUri: string }>;

/** What the console must ask for before it shows anything else, if anything. */
export function mfaStep(state: MfaState | undefined): "verify" | "setup" | null {
  if (!state) return null;
  if (state.enabled && !state.verified) return "verify";
  if (state.required && !state.enabled) return "setup";
  return null;
}

function CodeField({
  value,
  onChange,
  allowRecovery,
}: {
  value: string;
  onChange: (value: string) => void;
  allowRecovery?: boolean;
}) {
  return (
    <label className="field">
      <span>{allowRecovery ? "رمز التطبيق أو رمز احتياطي" : "الرمز من تطبيق المصادقة"}</span>
      <input
        dir="ltr"
        inputMode={allowRecovery ? "text" : "numeric"}
        autoComplete="one-time-code"
        autoFocus
        maxLength={allowRecovery ? 12 : 6}
        value={value}
        data-testid="mfa-code"
        onChange={(event) => onChange(event.target.value)}
      />
    </label>
  );
}

/** Leave instead: whoever cannot give the second step must at least be able to sign out. */
function SignOut() {
  const router = useRouter();
  const [leaving, setLeaving] = useState(false);
  return (
    <button
      type="button"
      className="button-ghost"
      disabled={leaving}
      data-testid="mfa-sign-out"
      onClick={async () => {
        setLeaving(true);
        try {
          await logout();
          router.replace("/login");
          router.refresh();
        } finally {
          setLeaving(false);
        }
      }}
    >
      تسجيل الخروج
    </button>
  );
}

/** The second step for this session: six digits, or one of the recovery codes. */
export function MfaVerifyScreen({ onDone }: { onDone: () => void }) {
  const mutation = useMutation();
  const [code, setCode] = useState("");

  async function submit(): Promise<void> {
    if (await mutation.run("mfaVerify", { code: code.trim() })) onDone();
  }

  return (
    <main className="login-shell">
      <section className="panel login-card" data-testid="mfa-verify">
        <div className="login-brand">
          <BrandMark />
          <div className="brand-text">
            <strong>دليني</strong>
            <span>لوحة الإدارة</span>
          </div>
        </div>
        <header className="login-header">
          <h1>التحقق بخطوتين</h1>
          <p className="muted">
            افتح تطبيق المصادقة وأدخل الرمز المكوّن من ستة أرقام. إن فقدت هاتفك فاستعمل أحد الرموز
            الاحتياطية، فكل رمز منها يعمل مرة واحدة.
          </p>
        </header>
        <form
          className="login-form"
          onSubmit={(event) => {
            event.preventDefault();
            void submit();
          }}
        >
        <CodeField value={code} onChange={setCode} allowRecovery />
        {mutation.error ? (
          <p className="field-error" role="alert">
            {messageFor(mutation.error)}
          </p>
        ) : null}
        <button
          type="submit"
          className="button-primary"
          disabled={mutation.pending || code.trim().length < 6}
          data-testid="mfa-submit"
        >
          {mutation.pending ? "جارٍ التحقق…" : "متابعة"}
        </button>
        <SignOut />
        </form>
      </section>
    </main>
  );
}

/**
 * Setting up an authenticator: scan, confirm with the first code, keep the recovery codes.
 *
 * The secret is shown once, as a QR code and as text for typing in by hand. Nothing is
 * enabled until a code from the app confirms it, and the recovery codes are shown once.
 */
export function MfaSetupPanel({ onDone }: { onDone: () => void }) {
  const mutation = useMutation();
  const [setup, setSetup] = useState<Setup | null>(null);
  const [code, setCode] = useState("");
  const [recovery, setRecovery] = useState<readonly string[] | null>(null);

  async function begin(): Promise<void> {
    const answer = await mutation.runFor<Setup>("mfaSetup");
    if (answer) setSetup(answer);
  }

  async function confirm(): Promise<void> {
    const answer = await mutation.runFor<{ recoveryCodes: string[] }>("mfaConfirm", {
      code: code.trim(),
    });
    if (answer) setRecovery(answer.recoveryCodes);
  }

  if (recovery) {
    return (
      <div className="stack" data-testid="mfa-recovery">
        <p className="notice">
          فُعّل التحقق بخطوتين. احفظ هذه الرموز الاحتياطية في مكان آمن خارج هاتفك: كل رمز يدخلك مرة
          واحدة إن فقدت تطبيق المصادقة، ولن تظهر مرة أخرى.
        </p>
        <ol className="recovery-codes" dir="ltr">
          {recovery.map((item) => (
            <li key={item}>
              <code>{item}</code>
            </li>
          ))}
        </ol>
        <div className="button-row">
          <button
            type="button"
            className="button-ghost"
            onClick={() => void navigator.clipboard?.writeText(recovery.join("\n"))}
          >
            نسخ الرموز
          </button>
          <button
            type="button"
            className="button-primary"
            data-testid="mfa-done"
            onClick={onDone}
          >
            حفظتها، تابع
          </button>
        </div>
      </div>
    );
  }

  if (!setup) {
    return (
      <div className="stack">
        <p className="muted">
          ستحتاج تطبيق مصادقة على هاتفك، مثل Google Authenticator أو Microsoft Authenticator أو
          أي تطبيق يدعم TOTP.
        </p>
        {mutation.error ? (
          <p className="field-error" role="alert">
            {messageFor(mutation.error)}
          </p>
        ) : null}
        <div className="button-row">
          <button
            type="button"
            className="button-primary"
            disabled={mutation.pending}
            data-testid="mfa-begin"
            onClick={() => void begin()}
          >
            {mutation.pending ? "جارٍ التحضير…" : "بدء الإعداد"}
          </button>
        </div>
      </div>
    );
  }

  return (
    <form
      className="stack"
      data-testid="mfa-setup"
      onSubmit={(event) => {
        event.preventDefault();
        void confirm();
      }}
    >
      <div className="mfa-setup">
        {/* eslint-disable-next-line @next/next/no-img-element -- a data URI, nothing to optimise */}
        <img src={setup.qrSvgDataUri} alt="رمز QR لإضافة الحساب إلى تطبيق المصادقة" width={200} height={200} />
        <div className="stack">
          <p>١. امسح الرمز بتطبيق المصادقة، أو أدخل هذا المفتاح يدوياً:</p>
          <code className="mfa-secret" dir="ltr" data-testid="mfa-secret">
            {setup.secret.replace(/(.{4})/g, "$1 ").trim()}
          </code>
          <p>٢. أدخل الرمز الذي يظهر في التطبيق لتأكيد الإعداد.</p>
          <CodeField value={code} onChange={setCode} />
        </div>
      </div>
      {mutation.error ? (
        <p className="field-error" role="alert">
          {messageFor(mutation.error)}
        </p>
      ) : null}
      <div className="button-row">
        <button
          type="submit"
          className="button-primary"
          disabled={mutation.pending || code.trim().length !== 6}
          data-testid="mfa-confirm"
        >
          {mutation.pending ? "جارٍ التأكيد…" : "تأكيد وتفعيل"}
        </button>
      </div>
    </form>
  );
}

/** Before the console: setting it up is required by this deployment. */
export function MfaSetupScreen({ onDone }: { onDone: () => void }) {
  return (
    <main className="login-shell">
      <section className="panel login-card login-card-wide" data-testid="mfa-required">
        <div className="login-brand">
          <BrandMark />
          <div className="brand-text">
            <strong>دليني</strong>
            <span>لوحة الإدارة</span>
          </div>
        </div>
        <header className="login-header">
          <h1>فعّل التحقق بخطوتين</h1>
          <p className="muted">
            لوحة التحكم تتطلب خطوة ثانية عند الدخول لحماية بيانات المنشآت والمستخدمين. يستغرق
            الإعداد دقيقة واحدة.
          </p>
        </header>
        <div className="login-form">
          <MfaSetupPanel onDone={onDone} />
          <SignOut />
        </div>
      </section>
    </main>
  );
}
