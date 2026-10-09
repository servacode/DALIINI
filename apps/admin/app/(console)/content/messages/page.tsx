"use client";

import Link from "next/link";
import { useCallback, useState } from "react";

import { useCan } from "../../../../components/admin-shell";
import {
  ConfirmDialog,
  EmptyState,
  ErrorState,
  LoadingState,
  PageHeader,
  Pagination,
  TermBadge,
  Toast,
  formatDateTime,
} from "../../../../components/ui";
import { Icon } from "../../../../components/icons";
import { CharCount, FilterChips } from "../../../../components/ui/extra";
import { MESSAGE_KINDS } from "../../../../lib/client/content";
import { useMutation } from "../../../../lib/client/use-mutation";
import { useResource } from "../../../../lib/client/use-resource";

type Message = Readonly<{
  id: string;
  name: string;
  phone: string | null;
  message: string;
  kind: string;
  userId: string | null;
  handled: boolean;
  handledAt: string | null;
  createdAt: string;
}>;

type MessagePage = Readonly<{ items: Message[]; nextCursor: string | null; hasMore: boolean }>;

const NOTE_MAX = 500;

const STATUS = [
  { value: "open", label: "بانتظار المعالجة" },
  { value: "handled", label: "تمت معالجتها" },
] as const;

/**
 * A chat with the sender on WhatsApp, where the platform's support already answers by hand
 * (DECISION-102): a Syrian number written locally (09…) or internationally (+963…), or none.
 */
function whatsappLink(phone: string): string | null {
  const digits = phone.replace(/[^0-9]/g, "");
  const international = digits.startsWith("963")
    ? digits
    : digits.startsWith("09")
      ? `963${digits.slice(1)}`
      : null;
  return international && international.length >= 11 ? `https://wa.me/${international}` : null;
}

/**
 * What people wrote through the contact form, newest first.
 *
 * An operator reads, acts wherever the fix belongs (a facility, an account), and marks the
 * message handled with a note, which is kept in the audit trail. Handling is final and
 * idempotent: a handled message keeps who handled it and when.
 */
export default function ContactMessagesPage() {
  const [filters, setFilters] = useState<Record<string, string>>({ status: "open", kind: "" });
  const [cursor, setCursor] = useState<string | undefined>(undefined);
  const messages = useResource<MessagePage>("contactMessages", { ...filters, cursor });
  const canManage = useCan("admin.content.manage");
  const mutation = useMutation();

  const [handling, setHandling] = useState<Message | null>(null);
  const [note, setNote] = useState("");
  const [toast, setToast] = useState<string | null>(null);
  const dismissToast = useCallback(() => setToast(null), []);

  async function handle(): Promise<void> {
    if (!handling) return;
    const ok = await mutation.run("contactMessageHandle", { id: handling.id, note: note.trim() });
    if (!ok) return;
    setHandling(null);
    setNote("");
    setToast("عُلّمت الرسالة كمُعالجة.");
    messages.reload();
  }

  return (
    <div className="stack">
      <PageHeader
        eyebrow="المحتوى"
        title="رسائل التواصل"
        description="ما يصل من نموذج «تواصل معنا» في الموقع والتطبيق."
      />
      <div className="live-toolbar">
        <FilterChips
          label="الحالة"
          value={filters.status as "open" | "handled"}
          options={STATUS}
          testId="messages-status"
          onChange={(status) => {
            setFilters({ ...filters, status });
            setCursor(undefined);
          }}
        />
        <select
          className="toolbar-select"
          aria-label="الموضوع"
          value={filters.kind}
          data-testid="messages-kind"
          onChange={(event) => {
            setFilters({ ...filters, kind: event.target.value });
            setCursor(undefined);
          }}
        >
          <option value="">كل المواضيع</option>
          {Object.entries(MESSAGE_KINDS).map(([value, label]) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </select>
      </div>

      {messages.loading ? <LoadingState /> : null}
      {messages.error ? <ErrorState error={messages.error} onRetry={messages.reload} /> : null}

      {messages.data ? (
        messages.data.items.length === 0 ? (
          <EmptyState
            title={filters.status === "open" ? "لا رسائل بانتظار المعالجة" : "لا رسائل"}
            hint={filters.status === "open" ? "كل ما وصل عولج." : undefined}
            illustration={filters.status === "open" ? "success" : "empty"}
          />
        ) : (
          <ul className="message-list" data-testid="message-list">
            {messages.data.items.map((item) => (
              <li
                key={item.id}
                className="message-card"
                data-handled={item.handled || undefined}
                data-testid={`message-${item.id}`}
              >
                <header className="message-meta">
                  <strong>{item.name}</strong>
                  <TermBadge group="contactKind" value={item.kind} />
                  {item.phone ? (
                    <a className="cell-ltr message-phone" href={`tel:${item.phone}`}>
                      {item.phone}
                    </a>
                  ) : null}
                  {item.userId ? (
                    <Link href={`/users?id=${encodeURIComponent(item.userId)}`}>حساب مسجّل</Link>
                  ) : null}
                  <time className="muted cell-ltr" dateTime={item.createdAt}>
                    {formatDateTime(item.createdAt)}
                  </time>
                </header>
                <p className="message-body">{item.message}</p>
                <footer className="message-foot">
                  {item.handled ? (
                    <span className="button-row">
                      <TermBadge group="contactState" value="HANDLED" />
                      <span className="muted cell-ltr">{formatDateTime(item.handledAt)}</span>
                    </span>
                  ) : (
                    <TermBadge group="contactState" value="PENDING" />
                  )}
                  {item.phone && whatsappLink(item.phone) ? (
                    <a
                      className="button-ghost message-whatsapp"
                      href={whatsappLink(item.phone)!}
                      target="_blank"
                      rel="noreferrer"
                    >
                      <Icon name="whatsapp" />
                      رد على واتساب
                    </a>
                  ) : null}
                  {canManage && !item.handled ? (
                    <button
                      type="button"
                      className="button-ghost"
                      data-testid={`handle-${item.id}`}
                      onClick={() => {
                        mutation.reset();
                        setNote("");
                        setHandling(item);
                      }}
                    >
                      تعليم كمُعالجة
                    </button>
                  ) : null}
                </footer>
              </li>
            ))}
          </ul>
        )
      ) : null}

      {messages.data ? (
        <Pagination
          hasMore={messages.data.hasMore}
          atFirst={cursor === undefined}
          loading={messages.loading}
          onFirst={() => setCursor(undefined)}
          onNext={() => setCursor(messages.data?.nextCursor ?? undefined)}
        />
      ) : null}

      <ConfirmDialog
        open={handling !== null}
        title="تعليم الرسالة كمُعالجة"
        body="تنتقل الرسالة إلى المعالَجة، وتُحفظ ملاحظتك في سجل العمليات."
        confirmLabel="تأكيد المعالجة"
        pending={mutation.pending}
        error={mutation.error}
        onConfirm={handle}
        onCancel={() => setHandling(null)}
      >
        <label className="field">
          <span className="field-label-row">
            ملاحظة (اختيارية)
            <CharCount value={note} max={NOTE_MAX} />
          </span>
          <textarea
            rows={3}
            value={note}
            maxLength={NOTE_MAX}
            placeholder="ما الذي فعلته، مثلاً: صُحّح رقم الهاتف في صفحة المنشأة."
            data-testid="handle-note"
            onChange={(event) => setNote(event.target.value)}
          />
        </label>
      </ConfirmDialog>

      <Toast message={toast} onDismiss={dismissToast} />
    </div>
  );
}
