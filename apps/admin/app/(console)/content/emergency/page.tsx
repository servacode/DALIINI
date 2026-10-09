"use client";

import { useCallback, useState } from "react";

import { useCan } from "../../../../components/admin-shell";
import { Icon, Icons } from "../../../../components/icons";
import {
  ConfirmDialog,
  EmptyState,
  ErrorState,
  LoadingState,
  PageHeader,
  Toast,
} from "../../../../components/ui";
import { CharCount, FormDialog, ItemCard } from "../../../../components/ui/extra";
import { NUMBER_KINDS } from "../../../../lib/client/content";
import { useMutation } from "../../../../lib/client/use-mutation";
import { useResource } from "../../../../lib/client/use-resource";
import { fieldErrorsFor } from "../../../../lib/errors/messages";
import { LOCALE } from "../../../../lib/locale";

const NUMBER = new Intl.NumberFormat(LOCALE);

type EmergencyNumber = Readonly<{
  id: string;
  provinceId: string | null;
  provinceNameAr: string | null;
  labelAr: string;
  phone: string;
  kind: string;
  sortOrder: number;
  active: boolean;
  adminNote: string;
}>;

type Province = Readonly<{ id: string; nameAr: string; sortOrder: number }>;

type Draft = Readonly<{
  id?: string;
  labelAr: string;
  phone: string;
  kind: string;
  provinceId: string;
  sortOrder: string;
  active: boolean;
  adminNote: string;
}>;

const BLANK: Draft = {
  labelAr: "",
  phone: "",
  kind: "AMBULANCE",
  provinceId: "",
  sortOrder: "0",
  active: true,
  adminNote: "",
};

const PHONE = /^\+?[0-9]{2,15}$/;

/** A number inside an Arabic sentence, isolated so a leading + stays in front of it. */
const ltr = (value: string) => `\u2066${value}\u2069`;
const NOTE_MAX = 240;

const MESSAGES = {
  labelAr: "اكتب اسم الرقم، 120 حرفاً على الأكثر.",
  phone: "الرقم أرقام فقط، من خانتين إلى 15 خانة، ويجوز أن يبدأ بعلامة +.",
  adminNote: "الملاحظة 240 حرفاً على الأكثر.",
};

/**
 * The numbers a person in trouble needs, national or for one province.
 *
 * National numbers show everywhere; a provincial one only to people in that province. The
 * operator-only note is where a number's origin and what still needs checking are written:
 * the seeded national numbers carry one because nobody has confirmed them against an
 * official source yet, so they are listed first, and confirming a number clears its note.
 */
export default function EmergencyNumbersPage() {
  const numbers = useResource<{ items: EmergencyNumber[] }>("emergencyNumbers");
  const provinces = useResource<{ items: Province[] }>("provinces");
  const canManage = useCan("admin.content.manage");
  const mutation = useMutation();

  const [draft, setDraft] = useState<Draft | null>(null);
  const [problem, setProblem] = useState<Record<string, string>>({});
  const [verifying, setVerifying] = useState<EmergencyNumber | null>(null);
  const [removing, setRemoving] = useState<EmergencyNumber | null>(null);
  const [toast, setToast] = useState<string | null>(null);
  const dismissToast = useCallback(() => setToast(null), []);

  const provinceItems = [...(provinces.data?.items ?? [])].sort((a, b) => a.sortOrder - b.sortOrder);
  const items = numbers.data?.items ?? [];
  const flagged = items.filter((item) => item.adminNote.trim());

  const server = fieldErrorsFor(mutation.error);
  const errors: Record<string, string> = {
    ...(server.labelAr ? { labelAr: MESSAGES.labelAr } : {}),
    ...(server.phone ? { phone: MESSAGES.phone } : {}),
    ...(server.adminNote ? { adminNote: MESSAGES.adminNote } : {}),
    ...problem,
  };

  const scopeOf = (item: EmergencyNumber) =>
    item.provinceId
      ? (provinceItems.find((province) => province.id === item.provinceId)?.nameAr ??
        item.provinceNameAr ??
        "محافظة")
      : "وطني";

  // National first, then each province in the platform's own order.
  const groups: { key: string; title: string; description: string; rows: EmergencyNumber[] }[] = [];
  const national = items.filter((item) => !item.provinceId);
  if (national.length > 0) {
    groups.push({
      key: "national",
      title: "أرقام وطنية",
      description: "تظهر في كل المحافظات.",
      rows: national,
    });
  }
  const provincial = new Map<string, EmergencyNumber[]>();
  for (const item of items) {
    if (!item.provinceId) continue;
    provincial.set(item.provinceId, [...(provincial.get(item.provinceId) ?? []), item]);
  }
  const rank = (id: string) => {
    const index = provinceItems.findIndex((province) => province.id === id);
    return index === -1 ? Number.MAX_SAFE_INTEGER : index;
  };
  for (const [provinceId, rows] of [...provincial.entries()].sort((a, b) => rank(a[0]) - rank(b[0]))) {
    groups.push({
      key: provinceId,
      title: scopeOf(rows[0]!),
      description: "تظهر لمن اختار هذه المحافظة فقط.",
      rows,
    });
  }

  function edit(item: EmergencyNumber | null): void {
    mutation.reset();
    setProblem({});
    setDraft(
      item
        ? {
            id: item.id,
            labelAr: item.labelAr,
            phone: item.phone,
            kind: item.kind,
            provinceId: item.provinceId ?? "",
            sortOrder: String(item.sortOrder),
            active: item.active,
            adminNote: item.adminNote,
          }
        : { ...BLANK },
    );
  }

  async function save(): Promise<void> {
    if (!draft) return;
    const local: Record<string, string> = {};
    if (!draft.labelAr.trim()) local.labelAr = MESSAGES.labelAr;
    if (!PHONE.test(draft.phone.trim())) local.phone = MESSAGES.phone;
    if (Array.from(draft.adminNote).length > NOTE_MAX) local.adminNote = MESSAGES.adminNote;
    setProblem(local);
    if (Object.keys(local).length > 0) return;

    const payload = {
      labelAr: draft.labelAr.trim(),
      phone: draft.phone.trim(),
      kind: draft.kind,
      provinceId: draft.provinceId || null,
      sortOrder: Math.max(0, Math.round(Number(draft.sortOrder) || 0)),
      active: draft.active,
      adminNote: draft.adminNote.trim(),
    };
    const ok = await mutation.run(
      draft.id ? "emergencyNumberUpdate" : "emergencyNumberCreate",
      draft.id ? { id: draft.id, ...payload } : payload,
    );
    if (!ok) return;
    setToast(draft.id ? "حُفظ الرقم." : "أُضيف الرقم.");
    setDraft(null);
    numbers.reload();
  }

  async function confirmVerified(): Promise<void> {
    if (!verifying) return;
    const ok = await mutation.run("emergencyNumberUpdate", { id: verifying.id, adminNote: "" });
    if (!ok) return;
    setVerifying(null);
    setToast("أُكّد الرقم، وأُزيلت ملاحظة التحقق.");
    numbers.reload();
  }

  async function remove(): Promise<void> {
    if (!removing) return;
    const ok = await mutation.run("emergencyNumberDelete", { id: removing.id });
    if (!ok) return;
    setRemoving(null);
    setToast("حُذف الرقم.");
    numbers.reload();
  }

  return (
    <div className="stack">
      <PageHeader
        eyebrow="المحتوى"
        title="أرقام الطوارئ"
        description="أرقام الإسعاف والإطفاء والشرطة وغيرها، على مستوى البلد أو لمحافظة بعينها."
        actions={
          canManage ? (
            <button
              type="button"
              className="page-hero-action"
              data-testid="new-number"
              onClick={() => edit(null)}
            >
              <Icon name="plus" />
              رقم جديد
            </button>
          ) : null
        }
      />

      {numbers.loading ? <LoadingState /> : null}
      {numbers.error ? <ErrorState error={numbers.error} onRetry={numbers.reload} /> : null}

      {flagged.length > 0 ? (
        <p className="notice notice-warning" data-testid="verify-banner">
          <Icons.alert />
          {`${NUMBER.format(flagged.length)} من الأرقام تحتاج تحققاً قبل الإطلاق: اتصل بكل رقم وتأكد أنه للجهة المكتوبة، ثم اضغط «تأكيد صحة الرقم» على بطاقته.`}
        </p>
      ) : null}

      {numbers.data && items.length === 0 ? (
        <EmptyState
          title="لا أرقام طوارئ بعد"
          hint="أضف الأرقام الوطنية أولاً، فهي تظهر في كل المحافظات."
        />
      ) : null}

      {groups.map((group) => (
        <section key={group.key} className="card-section" aria-labelledby={`group-${group.key}`}>
          <header className="card-section-head">
            <h2 id={`group-${group.key}`}>{group.title}</h2>
            <span>{group.description}</span>
          </header>
          <ul className="profile-grid">
            {group.rows.map((row, index) => {
              const unchecked = Boolean(row.adminNote.trim());
              return (
                <ItemCard
                  key={row.id}
                  index={index}
                  icon="emergency"
                  glyph={<span dir="ltr">{row.phone}</span>}
                  tone={!row.active ? "neutral" : unchecked ? "gold" : "danger"}
                  muted={!row.active}
                  title={row.labelAr}
                  subtitle={`${NUMBER_KINDS[row.kind] ?? row.kind} · ${scopeOf(row)}`}
                  state={
                    !row.active
                      ? { label: "مخفي" }
                      : unchecked
                        ? { label: "يحتاج تحققاً", tone: "warning" }
                        : { label: "ظاهر للعامة", tone: "positive" }
                  }
                  testId={`number-${row.id}`}
                  actions={
                    canManage ? (
                      <>
                        {unchecked ? (
                          <button
                            type="button"
                            className="profile-act-main"
                            data-testid={`confirm-number-${row.id}`}
                            onClick={() => {
                              mutation.reset();
                              setVerifying(row);
                            }}
                          >
                            <Icon name="check" width={16} height={16} />
                            تأكيد صحة الرقم
                          </button>
                        ) : null}
                        <button
                          type="button"
                          className={unchecked ? "profile-act-quiet" : "profile-act-main"}
                          data-testid={`edit-number-${row.id}`}
                          onClick={() => edit(row)}
                        >
                          {unchecked ? null : <Icon name="edit" width={16} height={16} />}
                          تعديل
                        </button>
                        <button
                          type="button"
                          className="profile-act-icon"
                          aria-label="حذف الرقم"
                          title="حذف الرقم"
                          data-testid={`delete-number-${row.id}`}
                          onClick={() => {
                            mutation.reset();
                            setRemoving(row);
                          }}
                        >
                          <Icon name="trash" width={16} height={16} />
                        </button>
                      </>
                    ) : null
                  }
                >
                  <p className={unchecked ? "number-note" : "item-card-quiet item-card-clamp"}>
                    {unchecked ? row.adminNote : "لا ملاحظات: الرقم مؤكَّد."}
                  </p>
                </ItemCard>
              );
            })}
          </ul>
        </section>
      ))}

      <FormDialog
        open={draft !== null}
        icon="emergency"
        title={draft?.id ? "تعديل الرقم" : "رقم جديد"}
        description="رقم وطني يظهر في كل المحافظات، أو رقم لمحافظة بعينها."
        onClose={() => setDraft(null)}
        locked={mutation.pending}
        testId="number-dialog"
        footer={
          <div className="button-row">
            <button
              type="button"
              className="button-ghost"
              disabled={mutation.pending}
              onClick={() => setDraft(null)}
            >
              إلغاء
            </button>
            <button
              type="button"
              className="button-primary"
              data-testid="save-number"
              disabled={mutation.pending}
              onClick={save}
            >
              {mutation.pending ? "جارٍ الحفظ…" : draft?.id ? "حفظ" : "إضافة الرقم"}
            </button>
          </div>
        }
      >
        {draft ? (
          <div className="stack">
            {mutation.error && Object.keys(server).length === 0 ? (
              <ErrorState error={mutation.error} />
            ) : null}
            <label className="field">
              <span>اسم الجهة</span>
              <input
                value={draft.labelAr}
                placeholder="الإسعاف"
                data-testid="number-label"
                aria-invalid={Boolean(errors.labelAr)}
                onChange={(event) => setDraft({ ...draft, labelAr: event.target.value })}
              />
              {errors.labelAr ? <span className="field-error">{errors.labelAr}</span> : null}
            </label>
            <label className="field">
              <span>الرقم</span>
              <input
                dir="ltr"
                inputMode="tel"
                value={draft.phone}
                placeholder="110"
                data-testid="number-phone"
                aria-invalid={Boolean(errors.phone)}
                onChange={(event) => setDraft({ ...draft, phone: event.target.value })}
              />
              {errors.phone ? <span className="field-error">{errors.phone}</span> : null}
            </label>
            <label className="field">
              <span>النوع</span>
              <select
                value={draft.kind}
                data-testid="number-kind"
                onChange={(event) => setDraft({ ...draft, kind: event.target.value })}
              >
                {Object.entries(NUMBER_KINDS).map(([value, label]) => (
                  <option key={value} value={value}>
                    {label}
                  </option>
                ))}
              </select>
            </label>
            <label className="field">
              <span>يظهر في</span>
              <select
                value={draft.provinceId}
                data-testid="number-province"
                onChange={(event) => setDraft({ ...draft, provinceId: event.target.value })}
              >
                <option value="">كل المحافظات (رقم وطني)</option>
                {provinceItems.map((province) => (
                  <option key={province.id} value={province.id}>
                    {province.nameAr}
                  </option>
                ))}
              </select>
            </label>
            <label className="field">
              <span>الترتيب</span>
              <input
                type="number"
                dir="ltr"
                min={0}
                value={draft.sortOrder}
                data-testid="number-order"
                onChange={(event) => setDraft({ ...draft, sortOrder: event.target.value })}
              />
              <span className="field-hint">الأصغر يظهر أولاً.</span>
            </label>
            <label className="field">
              <span className="field-label-row">
                ملاحظة للمشغلين
                <CharCount value={draft.adminNote} max={NOTE_MAX} />
              </span>
              <textarea
                rows={2}
                value={draft.adminNote}
                data-testid="number-note"
                aria-invalid={Boolean(errors.adminNote)}
                onChange={(event) => setDraft({ ...draft, adminNote: event.target.value })}
              />
              <span className="field-hint">
                مصدر الرقم وما يلزم التحقق منه. لا تظهر للعامة، وتُبقي الرقم في قائمة التحقق ما
                دامت مكتوبة.
              </span>
              {errors.adminNote ? <span className="field-error">{errors.adminNote}</span> : null}
            </label>
            <label className="switch-row">
              <span>ظاهر للعامة</span>
              <input
                type="checkbox"
                checked={draft.active}
                data-testid="number-active"
                onChange={(event) => setDraft({ ...draft, active: event.target.checked })}
              />
            </label>
          </div>
        ) : null}
      </FormDialog>

      <ConfirmDialog
        open={verifying !== null}
        title="تأكيد صحة الرقم"
        body={
          verifying
            ? `اتصلت بالرقم ${ltr(verifying.phone)} وتأكدت أنه يصل إلى ${verifying.labelAr}؟ تُزال ملاحظة التحقق ويبقى الرقم كما هو.`
            : undefined
        }
        confirmLabel="تأكيد صحة الرقم"
        pending={mutation.pending}
        error={mutation.error}
        onConfirm={confirmVerified}
        onCancel={() => setVerifying(null)}
      />

      <ConfirmDialog
        open={removing !== null}
        title="حذف الرقم"
        body={
          removing
            ? `يُحذف ${removing.labelAr} (${ltr(removing.phone)}) ويختفي من التطبيق والموقع فوراً.`
            : undefined
        }
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
