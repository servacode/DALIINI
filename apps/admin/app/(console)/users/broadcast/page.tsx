"use client";

import { useRouter } from "next/navigation";
import { useCallback, useState } from "react";

import { Icons } from "../../../../components/icons";
import {
  type Column,
  ConfirmDialog,
  DataTable,
  EmptyState,
  ErrorState,
  LoadingState,
  PageHeader,
  Pagination,
  Panel,
  StatusBadge,
  Toast,
  formatDateTime,
} from "../../../../components/ui";
import { CharCount, NotificationPreview, Segmented } from "../../../../components/ui/extra";
import { write } from "../../../../lib/client/api";
import { useResource } from "../../../../lib/client/use-resource";
import {
  type ApiErrorBody,
  fieldErrorsFor,
  isSessionExpired,
} from "../../../../lib/errors/messages";
import { LOCALE } from "../../../../lib/locale";

type Audience = "ALL" | "OWNERS";

type Broadcast = Readonly<{
  id: string;
  titleAr: string;
  bodyAr: string;
  audience: Audience;
  provinceId: string | null;
  recipientCount: number;
  actorName: string | null;
  createdAt: string;
}>;

type BroadcastPage = Readonly<{ items: Broadcast[]; nextCursor: string | null; hasMore: boolean }>;
type Province = Readonly<{ id: string; nameAr: string; sortOrder: number }>;

const TITLE_MAX = 180;
const BODY_MAX = 400;
const NUMBER = new Intl.NumberFormat(LOCALE);

const AUDIENCES = [
  { value: "ALL", label: "الجميع", hint: "كل الحسابات الفعّالة" },
  { value: "OWNERS", label: "أصحاب المنشآت", hint: "المالكون والمديرون" },
] as const satisfies readonly { value: Audience; label: string; hint: string }[];

/** Who a broadcast reaches, in words, for the confirmation and the history. */
function scopeOf(audience: Audience, provinceName: string | null): string {
  if (audience === "OWNERS") {
    return provinceName
      ? `أصحاب المنشآت ومديروها في ${provinceName}`
      : "كل أصحاب المنشآت ومديريها";
  }
  return provinceName ? `المستخدمون الذين اختاروا ${provinceName}` : "كل المستخدمين";
}

/**
 * Five an hour per operator, enforced upstream. Said as the rule it is, rather than as a
 * generic "too many attempts", so the operator knows when they can send again.
 */
function broadcastError(error: ApiErrorBody | null): ApiErrorBody | null {
  if (error?.code !== "THROTTLED") return error;
  return {
    ...error,
    code: "BROADCAST_LIMIT",
    message: "أُرسلت خمسة إشعارات خلال الساعة الأخيرة، وهو الحد المسموح. حاول بعد قليل.",
  };
}

/**
 * Send one notification to many people: every user, or the owners of facilities, across
 * the country or in one province.
 *
 * It cannot be recalled, so the preview shows exactly what a phone will show, the
 * confirmation says who it reaches, and the result states how many received it. Each send
 * is audited and appears in the history below.
 */
export default function BroadcastPage() {
  const provinces = useResource<{ items: Province[] }>("provinces");
  const [cursor, setCursor] = useState<string | undefined>(undefined);
  const history = useResource<BroadcastPage>("broadcasts", { cursor });
  const router = useRouter();
  // The send is run here rather than through `useMutation` because the answer matters: it
  // carries how many people the notification reached, which the confirmation reports.
  const [sending, setSending] = useState(false);
  const [sendError, setSendError] = useState<ApiErrorBody | null>(null);

  const [audience, setAudience] = useState<Audience>("ALL");
  const [provinceId, setProvinceId] = useState("");
  const [titleAr, setTitleAr] = useState("");
  const [bodyAr, setBodyAr] = useState("");
  const [confirming, setConfirming] = useState(false);
  const [toast, setToast] = useState<string | null>(null);
  const dismissToast = useCallback(() => setToast(null), []);

  const provinceItems = [...(provinces.data?.items ?? [])].sort((a, b) => a.sortOrder - b.sortOrder);
  const provinceName = (id: string | null) =>
    id ? (provinceItems.find((item) => item.id === id)?.nameAr ?? "محافظة محددة") : null;

  const titleLength = Array.from(titleAr.trim()).length;
  const bodyLength = Array.from(bodyAr.trim()).length;
  const ready =
    titleLength > 0 && bodyLength > 0 && titleLength <= TITLE_MAX && bodyLength <= BODY_MAX;
  const server = fieldErrorsFor(sendError);
  const scope = scopeOf(audience, provinceName(provinceId || null));

  async function send(): Promise<void> {
    setSending(true);
    setSendError(null);
    const result = await write<Broadcast>("broadcastSend", {
      titleAr: titleAr.trim(),
      bodyAr: bodyAr.trim(),
      audience,
      provinceId: provinceId || null,
    });
    setSending(false);
    if (!result.ok) {
      setSendError(result.error);
      if (isSessionExpired(result.error)) router.replace("/login");
      return;
    }
    setConfirming(false);
    setTitleAr("");
    setBodyAr("");
    setToast(`أُرسل الإشعار. عدد المستلمين: ${NUMBER.format(result.data.recipientCount)}.`);
    setCursor(undefined);
    history.reload();
  }

  const columns: readonly Column<Broadcast>[] = [
    {
      key: "createdAt",
      header: "أُرسل في",
      ltr: true,
      render: (row) => formatDateTime(row.createdAt),
    },
    {
      key: "message",
      header: "الإشعار",
      render: (row) => (
        <span className="cell-stack">
          <strong>{row.titleAr}</strong>
          <span className="muted text-clamp-2">{row.bodyAr}</span>
        </span>
      ),
    },
    {
      key: "scope",
      header: "إلى",
      render: (row) => scopeOf(row.audience, provinceName(row.provinceId)),
    },
    {
      key: "recipients",
      header: "المستلمون",
      render: (row) => <strong className="tabular">{NUMBER.format(row.recipientCount)}</strong>,
    },
    {
      key: "actor",
      header: "المرسِل",
      render: (row) => row.actorName ?? <span className="muted">—</span>,
    },
  ];

  return (
    <div className="stack">
      <PageHeader
        title="إرسال إشعار"
        description="إشعار واحد يصل إلى هواتف كثيرة وإلى صندوق الإشعارات في التطبيق. لا يمكن سحبه بعد الإرسال."
      />

      <div className="grid-main-aside broadcast-layout">
        <Panel title="الإشعار" testId="broadcast-form">
          <Segmented
            label="إلى من"
            name="audience"
            value={audience}
            options={AUDIENCES}
            onChange={setAudience}
          />
          <label className="field">
            <span>المحافظة</span>
            <select
              value={provinceId}
              data-testid="broadcast-province"
              onChange={(event) => setProvinceId(event.target.value)}
            >
              <option value="">كل المحافظات</option>
              {provinceItems.map((item) => (
                <option key={item.id} value={item.id}>
                  {item.nameAr}
                </option>
              ))}
            </select>
            <span className="field-hint">
              {audience === "OWNERS"
                ? "أصحاب المنشآت الموجودة في هذه المحافظة فقط."
                : "المستخدمون الذين اختاروا هذه المحافظة في التطبيق فقط."}
            </span>
          </label>
          <label className="field">
            <span className="field-label-row">
              العنوان
              <CharCount value={titleAr.trim()} max={TITLE_MAX} />
            </span>
            <input
              value={titleAr}
              placeholder="مثال: صيدليات مناوبة جديدة في حلب"
              data-testid="broadcast-title"
              aria-invalid={titleLength > TITLE_MAX || Boolean(server.titleAr)}
              onChange={(event) => setTitleAr(event.target.value)}
            />
            {titleLength > TITLE_MAX ? (
              <span className="field-error">{`العنوان ${NUMBER.format(TITLE_MAX)} حرفاً على الأكثر.`}</span>
            ) : null}
          </label>
          <label className="field">
            <span className="field-label-row">
              النص
              <CharCount value={bodyAr.trim()} max={BODY_MAX} />
            </span>
            <textarea
              rows={4}
              value={bodyAr}
              placeholder="جملة أو جملتان تقولان ما الجديد وماذا يفعل القارئ."
              data-testid="broadcast-body"
              aria-invalid={bodyLength > BODY_MAX || Boolean(server.bodyAr)}
              onChange={(event) => setBodyAr(event.target.value)}
            />
            {bodyLength > BODY_MAX ? (
              <span className="field-error">{`النص ${NUMBER.format(BODY_MAX)} حرف على الأكثر.`}</span>
            ) : null}
          </label>
          {sendError && !confirming ? <ErrorState error={broadcastError(sendError)} /> : null}
          <div className="broadcast-send">
            <p className="muted">
              يصل إلى: <strong>{scope}</strong>
            </p>
            <button
              type="button"
              className="button-primary"
              disabled={!ready || sending}
              data-testid="broadcast-send"
              onClick={() => {
                setSendError(null);
                setConfirming(true);
              }}
            >
              <Icons.bell />
              إرسال الإشعار
            </button>
          </div>
        </Panel>

        <Panel title="المعاينة">
          <NotificationPreview title={titleAr} body={bodyAr} />
        </Panel>
      </div>

      <Panel title="الإشعارات المرسلة" description="الأحدث أولاً." flush testId="broadcast-history">
        {history.loading ? <LoadingState /> : null}
        {history.error ? <ErrorState error={history.error} onRetry={history.reload} /> : null}
        {history.data ? (
          <DataTable
            caption="الإشعارات المرسلة"
            columns={columns}
            rows={history.data.items}
            rowKey={(row) => row.id}
            empty={<EmptyState title="لم يُرسل أي إشعار بعد" illustration="empty" />}
          />
        ) : null}
      </Panel>
      {history.data ? (
        <Pagination
          hasMore={history.data.hasMore}
          atFirst={cursor === undefined}
          loading={history.loading}
          onFirst={() => setCursor(undefined)}
          onNext={() => setCursor(history.data?.nextCursor ?? undefined)}
        />
      ) : null}

      <ConfirmDialog
        open={confirming}
        title="إرسال الإشعار"
        body={`سيصل الإشعار إلى ${scope}، على هواتفهم وفي صندوق الإشعارات. لا يمكن سحبه بعد الإرسال.`}
        confirmLabel="تأكيد الإرسال"
        pending={sending}
        error={broadcastError(sendError)}
        onConfirm={send}
        onCancel={() => setConfirming(false)}
      >
        <div className="broadcast-summary">
          <StatusBadge tone="info">
            {AUDIENCES.find((item) => item.value === audience)?.label}
          </StatusBadge>
          <StatusBadge tone="neutral">{provinceName(provinceId || null) ?? "كل المحافظات"}</StatusBadge>
        </div>
        <NotificationPreview title={titleAr} body={bodyAr} />
      </ConfirmDialog>

      <Toast message={toast} onDismiss={dismissToast} />
    </div>
  );
}
