"use client";

import { useState } from "react";

import { MfaSetupPanel, type MfaState } from "../../../components/mfa";
import {
  ConfirmDialog,
  ErrorState,
  KeyValueList,
  LoadingState,
  PageHeader,
  Panel,
  StatusBadge,
  Toast,
} from "../../../components/ui";
import { useMutation } from "../../../lib/client/use-mutation";
import { useResource } from "../../../lib/client/use-resource";

/**
 * The operator's own sign-in security: the authenticator app that guards the console.
 *
 * Reached from the operator's name in the sidebar, whatever their permissions, because it is
 * about their account rather than about anything they administer.
 */
export default function SecurityPage() {
  const status = useResource<MfaState>("mfaStatus");
  const mutation = useMutation();
  const [disabling, setDisabling] = useState(false);
  const [code, setCode] = useState("");
  const [toast, setToast] = useState<string | null>(null);

  async function disable(): Promise<void> {
    const ok = await mutation.run("mfaDisable", { code: code.trim() });
    if (!ok) return;
    setDisabling(false);
    setCode("");
    setToast("أُوقف التحقق بخطوتين.");
    status.reload();
  }

  return (
    <div className="stack">
      <PageHeader
        title="الأمان والتحقق بخطوتين"
        description="خطوة ثانية عند الدخول إلى لوحة التحكم: رمز من تطبيق المصادقة على هاتفك."
      />
      {status.loading ? <LoadingState /> : null}
      {status.error ? <ErrorState error={status.error} onRetry={status.reload} /> : null}
      {status.data ? (
        <Panel title="التحقق بخطوتين">
          <KeyValueList
            items={[
              {
                label: "الحالة",
                value: (
                  <StatusBadge tone={status.data.enabled ? "positive" : "warning"}>
                    {status.data.enabled ? "مفعّل" : "غير مفعّل"}
                  </StatusBadge>
                ),
              },
              ...(status.data.enabled
                ? [
                    {
                      label: "الرموز الاحتياطية المتبقية",
                      value: (
                        <span className="tabular" data-testid="recovery-left">
                          {status.data.recoveryCodesLeft}
                        </span>
                      ),
                    },
                  ]
                : []),
              {
                label: "سياسة المنصة",
                value: status.data.required ? "إلزامي لفريق التشغيل" : "اختياري",
              },
            ]}
          />
          {status.data.enabled ? (
            status.data.required ? null : (
              <div className="button-row">
                <button
                  type="button"
                  className="button-danger"
                  data-testid="mfa-disable"
                  onClick={() => {
                    mutation.reset();
                    setDisabling(true);
                  }}
                >
                  إيقاف التحقق بخطوتين
                </button>
              </div>
            )
          ) : (
            <MfaSetupPanel
              onDone={() => {
                setToast("فُعّل التحقق بخطوتين.");
                status.reload();
              }}
            />
          )}
        </Panel>
      ) : null}

      <ConfirmDialog
        open={disabling}
        title="إيقاف التحقق بخطوتين"
        body="يكفي بعدها كلمة المرور وحدها لدخول لوحة التحكم. أدخل رمزاً حالياً من التطبيق للتأكيد."
        confirmLabel="إيقاف"
        destructive
        pending={mutation.pending}
        error={mutation.error}
        onConfirm={disable}
        onCancel={() => setDisabling(false)}
      >
        <label className="field">
          <span>الرمز من تطبيق المصادقة</span>
          <input
            dir="ltr"
            inputMode="numeric"
            autoComplete="one-time-code"
            maxLength={6}
            value={code}
            onChange={(event) => setCode(event.target.value)}
          />
        </label>
      </ConfirmDialog>

      <Toast message={toast} onDismiss={() => setToast(null)} />
    </div>
  );
}
