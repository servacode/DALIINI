"use client";

import { useCallback, useState } from "react";

import { useCan } from "../../../../components/admin-shell";
import { Icons } from "../../../../components/icons";
import {
  type Column,
  ConfirmDialog,
  DataTable,
  EmptyState,
  ErrorState,
  LoadingState,
  PageHeader,
  Panel,
  TermBadge,
  Toast,
} from "../../../../components/ui";
import { CharCount } from "../../../../components/ui/extra";
import { NUMBER_KINDS } from "../../../../lib/client/content";
import { useMutation } from "../../../../lib/client/use-mutation";
import { useResource } from "../../../../lib/client/use-resource";
import { fieldErrorsFor } from "../../../../lib/errors/messages";

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

  const columns: readonly Column<EmergencyNumber>[] = [
    {
      key: "label",
      header: "الجهة",
      width: "26%",
      render: (row) => (
        <span className="cell-stack">
          <strong>{row.labelAr}</strong>
          <span className="muted">{NUMBER_KINDS[row.kind] ?? row.kind}</span>
        </span>
      ),
    },
    {
      key: "phone",
      header: "الرقم",
      ltr: true,
      width: "16%",
      render: (row) => <span className="phone-number">{row.phone}</span>,
    },
    {
      key: "status",
      header: "الحالة",
      width: "22%",
      render: (row) => (
        <span className="button-row">
          <TermBadge group="numberState" value={row.active ? "PUBLIC" : "HIDDEN"} />
          {row.adminNote.trim() ? <TermBadge group="numberState" value="NEEDS_CHECK" /> : null}
        </span>
      ),
    },
    {
      key: "note",
      header: "ملاحظة للمشغلين",
      render: (row) =>
        row.adminNote.trim() ? (
          <span className="note-text">{row.adminNote}</span>
        ) : (
          <span className="muted">—</span>
        ),
    },
    {
      key: "actions",
      header: "",
      width: "1%",
      render: (row) =>
        canManage ? (
          <div className="button-row">
            <button
              type="button"
              className="button-ghost"
              data-testid={`edit-number-${row.id}`}
              onClick={() => edit(row)}
            >
              تعديل
            </button>
            <button
              type="button"
              className="button-ghost"
              data-tone="danger"
              data-testid={`delete-number-${row.id}`}
              onClick={() => {
                mutation.reset();
                setRemoving(row);
              }}
            >
              حذف
            </button>
          </div>
        ) : null,
    },
  ];

  return (
    <div className="stack">
      <PageHeader
        title="أرقام الطوارئ"
        description="أرقام الإسعاف والإطفاء والشرطة وغيرها، على مستوى البلد أو لمحافظة بعينها."
        actions={
          canManage ? (
            <button
              type="button"
              className="button-primary"
              data-testid="new-number"
              onClick={() => edit(null)}
            >
              رقم جديد
            </button>
          ) : null
        }
      />

      {numbers.loading ? <LoadingState /> : null}
      {numbers.error ? <ErrorState error={numbers.error} onRetry={numbers.reload} /> : null}

      {flagged.length > 0 ? (
        <section className="verify-banner" data-testid="verify-banner" aria-labelledby="verify-title">
          <header className="verify-head">
            <span className="verify-icon" aria-hidden="true">
              <Icons.alert />
            </span>
            <div>
              <h2 id="verify-title">أرقام تحتاج تحققاً قبل الإطلاق</h2>
              <p>
                اتصل بكل رقم وتأكد أنه يعمل وأنه للجهة المكتوبة، ثم أكّد صحته لتُزال الملاحظة.
                الملاحظة للمشغلين فقط ولا تظهر للعامة.
              </p>
            </div>
          </header>
          <ul className="verify-list">
            {flagged.map((item) => (
              <li key={item.id} className="verify-item" data-testid={`verify-${item.id}`}>
                <div className="verify-text">
                  <p className="verify-title">
                    <strong>{item.labelAr}</strong>
                    <span className="phone-number cell-ltr">{item.phone}</span>
                    <span className="muted">{scopeOf(item)}</span>
                  </p>
                  <p className="verify-note">{item.adminNote}</p>
                </div>
                {canManage ? (
                  <div className="button-row">
                    <button
                      type="button"
                      className="button-primary"
                      data-testid={`confirm-number-${item.id}`}
                      onClick={() => {
                        mutation.reset();
                        setVerifying(item);
                      }}
                    >
                      <Icons.check />
                      تأكيد صحة الرقم
                    </button>
                    <button type="button" className="button-ghost" onClick={() => edit(item)}>
                      تصحيح الرقم
                    </button>
                  </div>
                ) : null}
              </li>
            ))}
          </ul>
        </section>
      ) : null}

      {numbers.data && items.length === 0 ? (
        <EmptyState
          title="لا أرقام طوارئ بعد"
          hint="أضف الأرقام الوطنية أولاً، فهي تظهر في كل المحافظات."
        />
      ) : null}

      {groups.map((group) => (
        <Panel key={group.key} title={group.title} description={group.description} flush>
          <DataTable
            caption={group.title}
            columns={columns}
            rows={group.rows}
            rowKey={(row) => row.id}
          />
        </Panel>
      ))}

      <ConfirmDialog
        open={draft !== null}
        title={draft?.id ? "تعديل الرقم" : "رقم جديد"}
        confirmLabel={draft?.id ? "حفظ" : "إضافة الرقم"}
        pending={mutation.pending}
        error={mutation.error}
        onConfirm={save}
        onCancel={() => setDraft(null)}
      >
        {draft ? (
          <>
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
          </>
        ) : null}
      </ConfirmDialog>

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
