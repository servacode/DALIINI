"use client";

import { useCallback, useState } from "react";

import { useCan } from "../../../../components/admin-shell";
import { Icon } from "../../../../components/icons";
import {
  type Column,
  ConfirmDialog,
  DataTable,
  EmptyState,
  ErrorState,
  LoadingState,
  PageHeader,
  Panel,
  StatusBadge,
  Toast,
} from "../../../../components/ui";
import { CharCount, Segmented } from "../../../../components/ui/extra";
import { useMutation } from "../../../../lib/client/use-mutation";
import { useResource } from "../../../../lib/client/use-resource";
import { useUrlFilters } from "../../../../lib/client/use-url-filters";
import { fieldErrorsFor } from "../../../../lib/errors/messages";
import { LOCALE } from "../../../../lib/locale";

type Category = Readonly<{ id: string; nameAr: string; specialization: string; active: boolean }>;

type Scope = "CATEGORY" | "SPECIALIZATION";

/** A specialty or a service, as the console lists it. Only a specialty carries a scope. */
type Tag = Readonly<{
  id: number;
  scope?: Scope;
  nameAr: string;
  nameEn: string;
  active: boolean;
  sortOrder: number;
  facilityCount: number;
}>;

type Kind = "specialty" | "service";

type Draft = Readonly<{ nameAr: string; nameEn: string; sortOrder: string; scope: Scope }>;

const NUMBER = new Intl.NumberFormat(LOCALE);
const NAME_MAX = 120;

/** What each shared specialization is called when a specialty is offered to all of it. */
const SPECIALIZATIONS: Readonly<Record<string, { one: string; all: string }>> = {
  GENERIC: { one: "عام", all: "التصنيفات العامة" },
  PHARMACY: { one: "صيدلية", all: "الصيدليات" },
  MEDICAL_CLINIC: { one: "عيادة", all: "العيادات" },
  NURSING_CENTER: { one: "مركز تمريض", all: "مراكز التمريض" },
};

/** «منشأة واحدة», «منشأتان», «3 منشآت», «11 منشأة» */
function facilitiesCount(count: number): string {
  if (count === 0) return "غير مستخدم";
  if (count === 1) return "منشأة واحدة";
  if (count === 2) return "منشأتان";
  if (count <= 10) return `${NUMBER.format(count)} منشآت`;
  return `${NUMBER.format(count)} منشأة`;
}

/**
 * The words of each panel. A specialty is masculine and a service feminine, so every
 * sentence that names one is written twice rather than assembled.
 */
const WORDS = {
  specialty: {
    title: "التخصصات",
    add: "إضافة تخصص",
    count: (n: number) =>
      n === 1 ? "تخصص واحد" : n === 2 ? "تخصصان" : n <= 10 ? `${NUMBER.format(n)} تخصصات` : `${NUMBER.format(n)} تخصصاً`,
    empty: "لا تخصصات لهذا التصنيف بعد",
    emptyHint: "أضف أول تخصص ليختاره المالكون ويصفّي به الزوار.",
    newTitle: "تخصص جديد",
    newBody: "يظهر في اختيارات المالكين، وفي فلتر الاختصاص إن كان مفعّلاً للتصنيف.",
    editTitle: "تعديل التخصص",
    deleteTitle: "حذف التخصص",
    deleteBody: (name: string) => `يُحذف «${name}» نهائياً، فلم تختره أي منشأة.`,
    created: "أُضيف التخصص.",
    saved: "حُفظ التخصص.",
    paused: "أُوقف التخصص: لم يعد يظهر للزوار ولا في اختيارات المالكين.",
    resumed: "فُعّل التخصص من جديد.",
    deleted: "حُذف التخصص.",
    ops: {
      list: "categorySpecialties",
      create: "specialtyCreate",
      update: "specialtyUpdate",
      remove: "specialtyDelete",
    },
  },
  service: {
    title: "الخدمات",
    add: "إضافة خدمة",
    count: (n: number) =>
      n === 1 ? "خدمة واحدة" : n === 2 ? "خدمتان" : n <= 10 ? `${NUMBER.format(n)} خدمات` : `${NUMBER.format(n)} خدمة`,
    empty: "لا خدمات لهذا التصنيف بعد",
    emptyHint: "أضف أول خدمة ليختارها المالكون ويصفّي بها الزوار.",
    newTitle: "خدمة جديدة",
    newBody: "تظهر في اختيارات المالكين، وفي فلتر الخدمات إن كان مفعّلاً للتصنيف.",
    editTitle: "تعديل الخدمة",
    deleteTitle: "حذف الخدمة",
    deleteBody: (name: string) => `تُحذف «${name}» نهائياً، فلم تخترها أي منشأة.`,
    created: "أُضيفت الخدمة.",
    saved: "حُفظت الخدمة.",
    paused: "أُوقفت الخدمة: لم تعد تظهر للزوار ولا في اختيارات المالكين.",
    resumed: "فُعّلت الخدمة من جديد.",
    deleted: "حُذفت الخدمة.",
    ops: {
      list: "categoryServiceTags",
      create: "serviceTagCreate",
      update: "serviceTagUpdate",
      remove: "serviceTagDelete",
    },
  },
} as const;

/**
 * Specialties and services: what an owner picks for a facility, and what a visitor narrows a
 * category's list by.
 *
 * One category at a time, chosen from the same list the other taxonomy screens read, and
 * kept in the address so a link opens on it. A specialty belongs to the category alone or
 * is shared by every category of its specialization; that is chosen once, on creation.
 *
 * An item is deleted only while no facility has chosen it. Once one has, the backend refuses
 * and the screen offers «أوقف» instead: a stopped item leaves the public pages, the filters
 * and the owners' choices, and comes back with its facilities intact when it is resumed.
 */
export default function TaxonomyTagsPage() {
  const categories = useResource<{ items: Category[] }>("categories");
  const canManage = useCan("admin.taxonomy.manage");
  const [filters, setFilters] = useUrlFilters({ category: "" });
  const [toast, setToast] = useState<string | null>(null);
  const dismissToast = useCallback(() => setToast(null), []);

  const items = categories.data?.items ?? [];
  const category =
    items.find((item) => item.id === filters.category) ??
    (filters.category ? null : (items.find((item) => item.active) ?? items[0] ?? null));

  return (
    <div className="stack">
      <PageHeader
        eyebrow="الدليل"
        title="التخصصات والخدمات"
        description="ما يختاره المالك لمنشأته، وما يصفّي به الزائر قائمة التصنيف."
      />

      <p className="notice">
        لا تظهر هذه الفلاتر للزوار إلا في تصنيف فُعّل له «فلتر الاختصاص» أو «فلتر الخدمات» من صفحة
        التصنيفات. الموقوف يختفي من الصفحات والفلاتر واختيارات المالكين، ولا يُحذف إلا ما لم تختره
        أي منشأة.
      </p>

      {categories.loading ? <LoadingState /> : null}
      {categories.error ? (
        <ErrorState error={categories.error} onRetry={categories.reload} />
      ) : null}

      {categories.data ? (
        items.length === 0 ? (
          <EmptyState title="لا تصنيفات بعد" hint="أنشئ تصنيفاً أولاً من صفحة التصنيفات." />
        ) : (
          <>
            <div className="live-toolbar">
              <label className="toolbar-label">
                <Icon name="tag" />
                <span>التصنيف</span>
                <select
                  className="toolbar-select"
                  value={category?.id ?? ""}
                  data-testid="tags-category"
                  onChange={(event) => setFilters({ category: event.target.value })}
                >
                  {category ? null : <option value="">اختر تصنيفاً</option>}
                  {items.map((item) => (
                    <option key={item.id} value={item.id}>
                      {item.active ? item.nameAr : `${item.nameAr} (معطّل)`}
                    </option>
                  ))}
                </select>
              </label>
            </div>

            {category ? (
              <div className="tag-columns">
                <TagPanel
                  key={`specialty-${category.id}`}
                  kind="specialty"
                  category={category}
                  canManage={canManage}
                  onDone={setToast}
                />
                <TagPanel
                  key={`service-${category.id}`}
                  kind="service"
                  category={category}
                  canManage={canManage}
                  onDone={setToast}
                />
              </div>
            ) : (
              <EmptyState title="اختر تصنيفاً" hint="التصنيف المطلوب غير موجود في القائمة." />
            )}
          </>
        )
      ) : null}

      <Toast message={toast} onDismiss={dismissToast} />
    </div>
  );
}

function TagPanel({
  kind,
  category,
  canManage,
  onDone,
}: {
  kind: Kind;
  category: Category;
  canManage: boolean;
  onDone: (message: string) => void;
}) {
  const words = WORDS[kind];
  const list = useResource<{ items: Tag[] }>(words.ops.list, { id: category.id });
  const mutation = useMutation();
  const [editing, setEditing] = useState<Tag | "new" | null>(null);
  const [draft, setDraft] = useState<Draft | null>(null);
  const [removing, setRemoving] = useState<Tag | null>(null);
  const [busy, setBusy] = useState(false);

  const rows = list.data?.items ?? [];
  const shared = SPECIALIZATIONS[category.specialization];
  // A GENERIC category has no specialization to share with; the backend refuses it too.
  const canShare = kind === "specialty" && category.specialization !== "GENERIC";

  function scopeLabel(tag: Tag): string {
    return tag.scope === "SPECIALIZATION" ? `كل ${shared?.all ?? category.specialization}` : "هذا التصنيف";
  }

  function start(tag: Tag | null): void {
    mutation.reset();
    setEditing(tag ?? "new");
    setDraft(
      tag
        ? {
            nameAr: tag.nameAr,
            nameEn: tag.nameEn,
            sortOrder: String(tag.sortOrder),
            scope: tag.scope ?? "CATEGORY",
          }
        : {
            nameAr: "",
            nameEn: "",
            sortOrder: String((rows.at(-1)?.sortOrder ?? 0) + 10),
            scope: "CATEGORY",
          },
    );
  }

  function stop(): void {
    setEditing(null);
    setDraft(null);
  }

  async function save(): Promise<void> {
    if (!draft || !editing || !draft.nameAr.trim()) return;
    const fields = {
      nameAr: draft.nameAr.trim(),
      nameEn: draft.nameEn.trim(),
      sortOrder: Math.max(0, Math.round(Number(draft.sortOrder) || 0)),
    };
    const creating = editing === "new";
    const ok = creating
      ? await mutation.run(words.ops.create, {
          categoryId: category.id,
          ...fields,
          ...(kind === "specialty" ? { scope: canShare ? draft.scope : "CATEGORY" } : {}),
        })
      : await mutation.run(words.ops.update, { id: editing.id, ...fields });
    if (!ok) return;
    stop();
    onDone(creating ? words.created : words.saved);
    list.reload();
  }

  async function toggle(tag: Tag): Promise<void> {
    mutation.reset();
    const ok = await mutation.run(words.ops.update, { id: tag.id, active: !tag.active });
    if (!ok) return;
    onDone(tag.active ? words.paused : words.resumed);
    list.reload();
  }

  /** Swap with a neighbour, then renumber in tens; only the rows whose number moved are sent. */
  async function move(index: number, delta: -1 | 1): Promise<void> {
    const order = [...rows];
    const target = index + delta;
    if (target < 0 || target >= order.length) return;
    [order[index], order[target]] = [order[target]!, order[index]!];
    const changes = order
      .map((tag, position) => ({ tag, sortOrder: (position + 1) * 10 }))
      .filter(({ tag, sortOrder }) => tag.sortOrder !== sortOrder);
    mutation.reset();
    setBusy(true);
    for (const change of changes) {
      const ok = await mutation.run(words.ops.update, { id: change.tag.id, sortOrder: change.sortOrder });
      if (!ok) break;
    }
    setBusy(false);
    list.reload();
  }

  async function remove(): Promise<void> {
    if (!removing) return;
    const ok = await mutation.run(words.ops.remove, { id: removing.id });
    if (!ok) return;
    setRemoving(null);
    onDone(words.deleted);
    list.reload();
  }

  const locked = busy || mutation.pending;
  const columns: Column<Tag>[] = [
    {
      key: "order",
      header: "الترتيب",
      width: "112px",
      render: (tag) => {
        const index = rows.indexOf(tag);
        return (
          <div className="order-cell">
            <span className="order-index" aria-hidden="true">
              {NUMBER.format(index + 1)}
            </span>
            {canManage ? (
              <span className="order-move">
                <button
                  type="button"
                  className="icon-button"
                  aria-label={`تحريك «${tag.nameAr}» إلى الأعلى`}
                  disabled={index === 0 || locked}
                  data-testid={`${kind}-up-${tag.id}`}
                  onClick={() => move(index, -1)}
                >
                  <span aria-hidden="true">↑</span>
                </button>
                <button
                  type="button"
                  className="icon-button"
                  aria-label={`تحريك «${tag.nameAr}» إلى الأسفل`}
                  disabled={index === rows.length - 1 || locked}
                  data-testid={`${kind}-down-${tag.id}`}
                  onClick={() => move(index, 1)}
                >
                  <span aria-hidden="true">↓</span>
                </button>
              </span>
            ) : null}
          </div>
        );
      },
    },
    {
      key: "name",
      header: "الاسم",
      render: (tag) => (
        <div className="cell-stack">
          <strong>{tag.nameAr}</strong>
          {tag.nameEn ? (
            <span className="muted" dir="ltr">
              {tag.nameEn}
            </span>
          ) : null}
        </div>
      ),
    },
    ...(kind === "specialty"
      ? [
          {
            key: "scope",
            header: "يظهر في",
            render: (tag: Tag) => (
              <StatusBadge tone={tag.scope === "SPECIALIZATION" ? "info" : "neutral"}>
                {scopeLabel(tag)}
              </StatusBadge>
            ),
          },
        ]
      : []),
    {
      key: "status",
      header: "الحالة",
      render: (tag) => (
        <StatusBadge tone={tag.active ? "positive" : "neutral"}>
          {tag.active ? "مفعّل" : "موقوف"}
        </StatusBadge>
      ),
    },
    {
      key: "use",
      header: "المنشآت",
      render: (tag) => <span className="muted">{facilitiesCount(tag.facilityCount)}</span>,
    },
    {
      key: "actions",
      header: "",
      render: (tag) =>
        canManage ? (
          <div className="button-row">
            <button
              type="button"
              className="button-ghost"
              disabled={locked}
              data-testid={`${kind}-edit-${tag.id}`}
              onClick={() => start(tag)}
            >
              تعديل
            </button>
            <button
              type="button"
              className="button-ghost"
              disabled={locked}
              data-testid={`${kind}-toggle-${tag.id}`}
              onClick={() => toggle(tag)}
            >
              {tag.active ? "أوقف" : "فعّل"}
            </button>
            {tag.facilityCount === 0 ? (
              <button
                type="button"
                className="button-ghost"
                data-tone="danger"
                disabled={locked}
                data-testid={`${kind}-delete-${tag.id}`}
                onClick={() => {
                  mutation.reset();
                  setRemoving(tag);
                }}
              >
                حذف
              </button>
            ) : null}
          </div>
        ) : null,
    },
  ];

  const active = rows.filter((tag) => tag.active).length;
  const creating = editing === "new";
  const nameError = fieldErrorsFor(mutation.error).nameAr;

  return (
    <Panel
      flush
      title={words.title}
      testId={`${kind}-panel`}
      description={
        rows.length > 0
          ? `${words.count(rows.length)}، المفعّل منها ${NUMBER.format(active)}. الأعلى في القائمة يظهر أولاً.`
          : undefined
      }
      actions={
        canManage ? (
          <button
            type="button"
            className="button-primary"
            disabled={!list.data || locked}
            data-testid={`${kind}-add`}
            onClick={() => start(null)}
          >
            {words.add}
          </button>
        ) : null
      }
    >
      {list.loading ? <LoadingState /> : null}
      {list.error ? <ErrorState error={list.error} onRetry={list.reload} /> : null}
      {mutation.error && !editing && !removing ? <ErrorState error={mutation.error} /> : null}
      {list.data ? (
        <DataTable
          columns={columns}
          rows={rows}
          rowKey={(tag) => String(tag.id)}
          caption={`${words.title}: ${category.nameAr}`}
          empty={
            <EmptyState title={words.empty} hint={canManage ? words.emptyHint : undefined} />
          }
        />
      ) : null}

      <ConfirmDialog
        open={draft !== null && editing !== null}
        title={creating ? words.newTitle : words.editTitle}
        body={
          creating
            ? words.newBody
            : editing?.scope === "SPECIALIZATION"
              ? `هذا التخصص مشترك بين كل ${shared?.all ?? category.specialization}، فالتعديل يسري عليها جميعاً.`
              : undefined
        }
        confirmLabel={creating ? "إضافة" : "حفظ"}
        pending={mutation.pending}
        error={mutation.error}
        onConfirm={save}
        onCancel={stop}
      >
        {draft ? (
          <TagFields
            draft={draft}
            onChange={setDraft}
            nameError={nameError}
            scopeChoice={
              creating && canShare
                ? {
                    categoryName: category.nameAr,
                    one: shared?.one ?? category.specialization,
                    all: shared?.all ?? category.specialization,
                  }
                : null
            }
          />
        ) : null}
      </ConfirmDialog>

      <ConfirmDialog
        open={removing !== null}
        title={words.deleteTitle}
        body={removing ? words.deleteBody(removing.nameAr) : undefined}
        confirmLabel="حذف"
        destructive
        pending={mutation.pending}
        error={mutation.error}
        onConfirm={remove}
        onCancel={() => setRemoving(null)}
      />
    </Panel>
  );
}

function TagFields({
  draft,
  onChange,
  nameError,
  scopeChoice,
}: {
  draft: Draft;
  onChange: (draft: Draft) => void;
  nameError: string | undefined;
  scopeChoice: { categoryName: string; one: string; all: string } | null;
}) {
  const missing = !draft.nameAr.trim();
  return (
    <>
      {scopeChoice ? (
        <Segmented<Scope>
          label="يظهر في"
          name="tag-scope"
          value={draft.scope}
          options={[
            { value: "CATEGORY", label: "هذا التصنيف", hint: `«${scopeChoice.categoryName}» وحده` },
            { value: "SPECIALIZATION", label: `كل ${scopeChoice.all}`, hint: `كل تصنيف من نوع ${scopeChoice.one}` },
          ]}
          onChange={(scope) => onChange({ ...draft, scope })}
        />
      ) : null}
      <label className="field">
        <span className="field-label-row">
          الاسم بالعربية
          <CharCount value={draft.nameAr} max={NAME_MAX} />
        </span>
        <input
          value={draft.nameAr}
          data-testid="tag-name-ar"
          aria-invalid={Boolean(nameError) || undefined}
          onChange={(event) => onChange({ ...draft, nameAr: event.target.value })}
        />
        {nameError ? (
          <span className="field-error">اكتب اسماً غير مكرر في هذا النطاق، 120 حرفاً على الأكثر.</span>
        ) : missing ? (
          <span className="field-hint">الاسم مطلوب.</span>
        ) : null}
      </label>
      <label className="field">
        <span className="field-label-row">
          الاسم بالإنجليزية <span className="field-hint">اختياري</span>
        </span>
        <input
          dir="ltr"
          value={draft.nameEn}
          data-testid="tag-name-en"
          onChange={(event) => onChange({ ...draft, nameEn: event.target.value })}
        />
      </label>
      <label className="field">
        <span>الترتيب</span>
        <input
          type="number"
          dir="ltr"
          min={0}
          step={10}
          value={draft.sortOrder}
          data-testid="tag-order"
          onChange={(event) => onChange({ ...draft, sortOrder: event.target.value })}
        />
        <span className="field-hint">الأصغر يظهر أولاً.</span>
      </label>
    </>
  );
}
