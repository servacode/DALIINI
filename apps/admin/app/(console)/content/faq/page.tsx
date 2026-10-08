"use client";

import { useCallback, useState } from "react";

import { useCan } from "../../../../components/admin-shell";
import {
  ConfirmDialog,
  EmptyState,
  ErrorState,
  LoadingState,
  PageHeader,
  StatusBadge,
  Toast,
} from "../../../../components/ui";
import { CharCount } from "../../../../components/ui/extra";
import { useMutation } from "../../../../lib/client/use-mutation";
import { useResource } from "../../../../lib/client/use-resource";
import { type ApiErrorBody, fieldErrorsFor } from "../../../../lib/errors/messages";
import { LOCALE } from "../../../../lib/locale";

type Entry = Readonly<{
  id: string;
  questionAr: string;
  answerAr: string;
  sortOrder: number;
  published: boolean;
  updatedAt: string;
}>;

type Draft = Readonly<{
  questionAr: string;
  answerAr: string;
  sortOrder: string;
  published: boolean;
}>;

const NUMBER = new Intl.NumberFormat(LOCALE);
const QUESTION_MAX = 300;
const ANSWER_MAX = 4000;

/** «سؤال واحد», «سؤالان», «٣ أسئلة», «١١ سؤالاً» */
function questionsCount(count: number): string {
  if (count === 1) return "سؤال واحد";
  if (count === 2) return "سؤالان";
  if (count <= 10) return `${NUMBER.format(count)} أسئلة`;
  return `${NUMBER.format(count)} سؤالاً`;
}

/**
 * The questions the app answers in its help section.
 *
 * Edited in place, one at a time; an entry is published on its own switch, so a question
 * can be written and reviewed before anyone sees it. Order is what readers see first: the
 * arrows move an entry and renumber the list in tens, so there is always room to insert.
 */
export default function FaqPage() {
  const entries = useResource<{ items: Entry[] }>("faqEntries");
  const canManage = useCan("admin.content.manage");
  const mutation = useMutation();

  const [editing, setEditing] = useState<string | null>(null);
  const [draft, setDraft] = useState<Draft | null>(null);
  const [removing, setRemoving] = useState<Entry | null>(null);
  const [busy, setBusy] = useState(false);
  const [toast, setToast] = useState<string | null>(null);
  const dismissToast = useCallback(() => setToast(null), []);

  const items = [...(entries.data?.items ?? [])].sort(
    (a, b) => a.sortOrder - b.sortOrder || a.updatedAt.localeCompare(b.updatedAt),
  );
  const published = items.filter((item) => item.published).length;

  function start(entry: Entry | null): void {
    mutation.reset();
    setEditing(entry?.id ?? "new");
    setDraft(
      entry
        ? {
            questionAr: entry.questionAr,
            answerAr: entry.answerAr,
            sortOrder: String(entry.sortOrder),
            published: entry.published,
          }
        : {
            questionAr: "",
            answerAr: "",
            sortOrder: String(((items.at(-1)?.sortOrder ?? 0) + 10)),
            published: false,
          },
    );
  }

  function stop(): void {
    setEditing(null);
    setDraft(null);
  }

  async function save(): Promise<void> {
    if (!draft || !editing) return;
    const payload = {
      questionAr: draft.questionAr.trim(),
      answerAr: draft.answerAr.trim(),
      sortOrder: Math.max(0, Math.round(Number(draft.sortOrder) || 0)),
      published: draft.published,
    };
    const creating = editing === "new";
    const ok = await mutation.run(
      creating ? "faqCreate" : "faqUpdate",
      creating ? payload : { id: editing, ...payload },
    );
    if (!ok) return;
    stop();
    setToast(creating ? "أُضيف السؤال." : "حُفظ السؤال.");
    entries.reload();
  }

  async function togglePublished(entry: Entry): Promise<void> {
    const ok = await mutation.run("faqUpdate", { id: entry.id, published: !entry.published });
    if (!ok) return;
    setToast(entry.published ? "أُخفي السؤال عن العامة." : "نُشر السؤال.");
    entries.reload();
  }

  /** Swap with a neighbour, then renumber in tens; only the entries whose number moved are sent. */
  async function move(index: number, delta: -1 | 1): Promise<void> {
    const order = [...items];
    const target = index + delta;
    if (target < 0 || target >= order.length) return;
    [order[index], order[target]] = [order[target]!, order[index]!];
    const changes = order
      .map((entry, position) => ({ entry, sortOrder: (position + 1) * 10 }))
      .filter(({ entry, sortOrder }) => entry.sortOrder !== sortOrder);
    setBusy(true);
    for (const change of changes) {
      const ok = await mutation.run("faqUpdate", { id: change.entry.id, sortOrder: change.sortOrder });
      if (!ok) break;
    }
    setBusy(false);
    entries.reload();
  }

  async function remove(): Promise<void> {
    if (!removing) return;
    const ok = await mutation.run("faqDelete", { id: removing.id });
    if (!ok) return;
    setRemoving(null);
    setToast("حُذف السؤال.");
    entries.reload();
  }

  const editor =
    draft && editing ? (
      <FaqEditor
        draft={draft}
        creating={editing === "new"}
        pending={mutation.pending}
        error={mutation.error}
        onChange={setDraft}
        onSave={save}
        onCancel={stop}
      />
    ) : null;

  return (
    <div className="stack">
      <PageHeader
        title="الأسئلة الشائعة"
        description="الأسئلة وأجوبتها كما تظهر في قسم المساعدة في التطبيق."
        actions={
          canManage ? (
            <button
              type="button"
              className="button-primary"
              data-testid="new-faq"
              disabled={editing === "new"}
              onClick={() => start(null)}
            >
              سؤال جديد
            </button>
          ) : null
        }
      />

      {entries.loading ? <LoadingState /> : null}
      {entries.error ? <ErrorState error={entries.error} onRetry={entries.reload} /> : null}
      {mutation.error && !editing && !removing ? <ErrorState error={mutation.error} /> : null}

      {entries.data ? (
        <section className="panel panel-flush">
          <header className="panel-header">
            <div>
              <h2>{items.length === 0 ? "لا أسئلة بعد" : questionsCount(items.length)}</h2>
              {items.length > 0 ? (
                <p>{`المنشور منها ${NUMBER.format(published)}. الأعلى في القائمة يظهر أولاً.`}</p>
              ) : null}
            </div>
          </header>
          {editing === "new" ? <div className="faq-new">{editor}</div> : null}
          {items.length === 0 && editing !== "new" ? (
            <EmptyState
              title="لا أسئلة بعد"
              hint="أضف أول سؤال، وانشره حين يكون جاهزاً."
            />
          ) : (
            <ol className="faq-list" data-testid="faq-list">
              {items.map((entry, index) =>
                editing === entry.id ? (
                  <li key={entry.id} className="faq-item" data-editing="true">
                    {editor}
                  </li>
                ) : (
                  <li
                    key={entry.id}
                    className="faq-item"
                    data-published={entry.published}
                    data-testid={`faq-${entry.id}`}
                  >
                    <div className="faq-order">
                      <span className="faq-index" aria-hidden="true">
                        {NUMBER.format(index + 1)}
                      </span>
                      {canManage ? (
                        <span className="faq-move">
                          <button
                            type="button"
                            className="icon-button"
                            aria-label={`تحريك «${entry.questionAr}» إلى الأعلى`}
                            disabled={index === 0 || busy || mutation.pending}
                            data-testid={`faq-up-${entry.id}`}
                            onClick={() => move(index, -1)}
                          >
                            <span aria-hidden="true">↑</span>
                          </button>
                          <button
                            type="button"
                            className="icon-button"
                            aria-label={`تحريك «${entry.questionAr}» إلى الأسفل`}
                            disabled={index === items.length - 1 || busy || mutation.pending}
                            data-testid={`faq-down-${entry.id}`}
                            onClick={() => move(index, 1)}
                          >
                            <span aria-hidden="true">↓</span>
                          </button>
                        </span>
                      ) : null}
                    </div>
                    <div className="faq-text">
                      <strong className="faq-question">{entry.questionAr}</strong>
                      <p className="faq-answer">{entry.answerAr}</p>
                    </div>
                    <div className="faq-controls">
                      {canManage ? (
                        <label className="switch-inline">
                          <input
                            type="checkbox"
                            className="switch"
                            checked={entry.published}
                            disabled={busy || mutation.pending}
                            data-testid={`faq-published-${entry.id}`}
                            onChange={() => togglePublished(entry)}
                          />
                          <span>{entry.published ? "منشور" : "غير منشور"}</span>
                        </label>
                      ) : (
                        <StatusBadge tone={entry.published ? "positive" : "neutral"}>
                          {entry.published ? "منشور" : "غير منشور"}
                        </StatusBadge>
                      )}
                      {canManage ? (
                        <span className="button-row">
                          <button
                            type="button"
                            className="button-ghost"
                            disabled={busy}
                            data-testid={`faq-edit-${entry.id}`}
                            onClick={() => start(entry)}
                          >
                            تعديل
                          </button>
                          <button
                            type="button"
                            className="button-ghost"
                            data-tone="danger"
                            disabled={busy}
                            data-testid={`faq-delete-${entry.id}`}
                            onClick={() => {
                              mutation.reset();
                              setRemoving(entry);
                            }}
                          >
                            حذف
                          </button>
                        </span>
                      ) : null}
                    </div>
                  </li>
                ),
              )}
            </ol>
          )}
        </section>
      ) : null}

      <ConfirmDialog
        open={removing !== null}
        title="حذف السؤال"
        body={removing ? `يُحذف «${removing.questionAr}» وجوابه نهائياً.` : undefined}
        confirmLabel="تأكيد الحذف"
        destructive
        pending={mutation.pending}
        error={mutation.error}
        onConfirm={remove}
        onCancel={() => setRemoving(null)}
      />
      <Toast message={toast} onDismiss={dismissToast} />
    </div>
  );
}

function FaqEditor({
  draft,
  creating,
  pending,
  error,
  onChange,
  onSave,
  onCancel,
}: {
  draft: Draft;
  creating: boolean;
  pending: boolean;
  error: ApiErrorBody | null;
  onChange: (draft: Draft) => void;
  onSave: () => void;
  onCancel: () => void;
}) {
  const errors = fieldErrorsFor(error);
  const questionMissing = !draft.questionAr.trim();
  const answerMissing = !draft.answerAr.trim();
  return (
    <form
      className="faq-editor"
      data-testid="faq-editor"
      onSubmit={(event) => {
        event.preventDefault();
        onSave();
      }}
    >
      <label className="field">
        <span className="field-label-row">
          السؤال
          <CharCount value={draft.questionAr} max={QUESTION_MAX} />
        </span>
        <input
          value={draft.questionAr}
          autoFocus
          data-testid="faq-question"
          aria-invalid={Boolean(errors.questionAr)}
          onChange={(event) => onChange({ ...draft, questionAr: event.target.value })}
        />
        {errors.questionAr ? (
          <span className="field-error">{`السؤال مطلوب، ${NUMBER.format(QUESTION_MAX)} حرف على الأكثر.`}</span>
        ) : null}
      </label>
      <label className="field">
        <span className="field-label-row">
          الجواب
          <CharCount value={draft.answerAr} max={ANSWER_MAX} />
        </span>
        <textarea
          rows={5}
          value={draft.answerAr}
          data-testid="faq-answer"
          aria-invalid={Boolean(errors.answerAr)}
          onChange={(event) => onChange({ ...draft, answerAr: event.target.value })}
        />
        {errors.answerAr ? (
          <span className="field-error">{`الجواب مطلوب، ${NUMBER.format(ANSWER_MAX)} حرف على الأكثر.`}</span>
        ) : null}
      </label>
      <div className="faq-editor-row">
        <label className="field faq-editor-order">
          <span>الترتيب</span>
          <input
            type="number"
            dir="ltr"
            min={0}
            step={10}
            value={draft.sortOrder}
            data-testid="faq-order"
            onChange={(event) => onChange({ ...draft, sortOrder: event.target.value })}
          />
        </label>
        <label className="switch-inline">
          <input
            type="checkbox"
            className="switch"
            checked={draft.published}
            data-testid="faq-publish"
            onChange={(event) => onChange({ ...draft, published: event.target.checked })}
          />
          <span>منشور للعامة</span>
        </label>
      </div>
      {error ? <ErrorState error={error} /> : null}
      <div className="button-row">
        <button
          type="submit"
          className="button-primary"
          disabled={pending || questionMissing || answerMissing}
          aria-busy={pending || undefined}
          data-testid="faq-save"
        >
          {pending ? "جارٍ الحفظ…" : creating ? "إضافة السؤال" : "حفظ السؤال"}
        </button>
        <button type="button" className="button-ghost" disabled={pending} onClick={onCancel}>
          إلغاء
        </button>
      </div>
    </form>
  );
}
