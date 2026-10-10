"use client";

import { useState } from "react";

import { useMutation } from "../lib/client/use-mutation";
import { useResource } from "../lib/client/use-resource";
import { ConfirmDialog, ErrorState, LoadingState, Panel, StatusBadge, Toast } from "./ui";

type WhatsAppState = Readonly<{
  provider: string;
  configured: boolean;
  reachable: boolean;
  connected: boolean;
  loggedOut: boolean;
  linkedNumber: string | null;
  qrSvgDataUri: string | null;
}>;

/**
 * «ربط واتساب»: the account the codes are sent from, linked by scanning a QR code here instead of
 * reading it off a server log (DECISION-116). Read every five seconds, so the card turns to
 * «مربوط» on its own the moment the phone has scanned.
 */
export function WhatsAppLinkPanel({ canManage }: { canManage: boolean }) {
  const state = useResource<WhatsAppState>("whatsapp", {}, { enabled: canManage, refreshMs: 5_000 });
  const mutation = useMutation();
  const [confirming, setConfirming] = useState(false);
  const [toast, setToast] = useState<string | null>(null);
  if (!canManage) return null;
  const value = state.data;

  return (
    <Panel
      title="ربط واتساب"
      description="الحساب الذي تُرسل منه رموز التسجيل والاستعادة. يُربط مرة واحدة من الجوال الذي سيرسل."
    >
      <div className="stack" data-testid="whatsapp-link">
        {state.loading && !value ? <LoadingState /> : null}
        {state.error ? <ErrorState error={state.error} onRetry={state.reload} /> : null}
        {value && !value.configured ? (
          <p className="field-hint" data-testid="whatsapp-unconfigured">
            البوت غير مُعدّ على الخادم بعد: يحتاج عنوانه ومفتاحه في إعدادات التشغيل
            (WHATSAPP_BOT_URL وWHATSAPP_BOT_TOKEN).
          </p>
        ) : null}
        {value && value.configured && !value.reachable ? (
          <p className="field-error" data-testid="whatsapp-unreachable">
            البوت لا يجيب. تأكد أنه يعمل على الخادم ثم انتظر قليلاً.
          </p>
        ) : null}
        {value?.connected ? (
          <p data-testid="whatsapp-connected">
            <StatusBadge tone="positive">مربوط</StatusBadge>{" "}
            تُرسل الرموز من الرقم <bdi dir="ltr">{value.linkedNumber ?? "—"}</bdi>.
          </p>
        ) : null}
        {value?.loggedOut ? (
          <p className="field-error" data-testid="whatsapp-logged-out">
            انقطع الربط من جهة واتساب (أُزيل الجهاز أو حُظر الحساب). اضغط «ربط من جديد».
          </p>
        ) : null}
        {value?.qrSvgDataUri ? (
          <div className="stack" data-testid="whatsapp-qr">
            <ol className="steps">
              <li>افتح واتساب على الجوال الذي سيرسل الرموز.</li>
              <li>الإعدادات ← الأجهزة المرتبطة ← ربط جهاز.</li>
              <li>امسح هذا الرمز. يتغير كل نصف دقيقة تقريباً، والصفحة تعرض أحدثه.</li>
            </ol>
            {/* eslint-disable-next-line @next/next/no-img-element -- a data URI, nothing to optimise */}
            <img
              src={value.qrSvgDataUri}
              alt="رمز QR لربط حساب واتساب بالمنصة"
              width={240}
              height={240}
            />
          </div>
        ) : null}
        {value && value.reachable && !value.connected && !value.qrSvgDataUri && !value.loggedOut ? (
          <p className="field-hint">ينتظر البوت رمزاً جديداً من واتساب…</p>
        ) : null}
        {value?.reachable && value.provider !== "whatsapp_bot" ? (
          <p className="field-hint" data-testid="whatsapp-provider">
            مزوّد الرموز الحالي «{value.provider}»: لن تُرسل الرموز عبر هذا الحساب حتى يُضبط
            OTP_PROVIDER=whatsapp_bot على الخادم.
          </p>
        ) : null}
        {value?.reachable && (value.connected || value.loggedOut) ? (
          <div className="button-row">
            <button
              type="button"
              className="button-ghost"
              data-testid="whatsapp-relink"
              onClick={() => {
                mutation.reset();
                setConfirming(true);
              }}
            >
              {value.connected ? "ربط رقم آخر" : "ربط من جديد"}
            </button>
          </div>
        ) : null}
      </div>
      <ConfirmDialog
        open={confirming}
        title="ربط من جديد"
        body="يُفصل الحساب الحالي ويظهر رمز جديد للمسح. لا تُرسل رموز التسجيل والاستعادة حتى يُمسح."
        confirmLabel="فصل وإظهار رمز جديد"
        destructive
        pending={mutation.pending}
        error={mutation.error}
        onConfirm={async () => {
          if (await mutation.run("whatsappRelink", {})) {
            setConfirming(false);
            setToast("فُصل الحساب. امسح الرمز الجديد.");
            state.reload();
          }
        }}
        onCancel={() => setConfirming(false)}
      />
      <Toast message={toast} onDismiss={() => setToast(null)} />
    </Panel>
  );
}
