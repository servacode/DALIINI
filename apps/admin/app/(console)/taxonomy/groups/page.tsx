"use client";

import { useState } from "react";

import { useCan } from "../../../../components/admin-shell";
import { type IconName, Icon, Icons } from "../../../../components/icons";
import { ErrorState, LoadingState, PageHeader, Toast } from "../../../../components/ui";
import { FormDialog, ItemCard, ItemChips } from "../../../../components/ui/extra";
import { useMutation } from "../../../../lib/client/use-mutation";
import { useResource } from "../../../../lib/client/use-resource";
import { fieldErrorsFor } from "../../../../lib/errors/messages";
import { LOCALE } from "../../../../lib/locale";

type Group = Readonly<{
  id: string;
  code: string;
  nameAr: string;
  nameEn: string;
  active: boolean;
  sortOrder: number;
  iconKey?: string;
}>;

type Category = Readonly<{ id: string; groupId: string; nameAr: string; active: boolean }>;

/** A group being added (no id) or edited. */
type Draft = Readonly<{
  id: string | null;
  code: string;
  nameAr: string;
  nameEn: string;
  iconKey: string;
  sortOrder: string;
}>;

/** The icons a group can wear: the shared set's drawings that read as a field of work. */
const GROUP_ICONS: readonly IconName[] = [
  "heart",
  "pharmacy",
  "clinic",
  "lab",
  "emergency",
  "building",
  "shield",
  "star",
  "tag",
  "grid",
];

const NUMBER = new Intl.NumberFormat(LOCALE);

function iconOf(key: string | undefined): IconName {
  return key && key in Icons ? (key as IconName) : "grid";
}

/**
 * Category groups: the first step of Cycle J, each a card with the categories under it.
 *
 * `code` is set once. The edit window does not offer it, and the backend refuses it outright
 * rather than ignoring it, so a rename that looks like it worked cannot happen. The icon is
 * chosen by picture from the platform's own set, not typed as a word the apps may not know.
 */
export default function TaxonomyGroupsPage() {
  const groups = useResource<{ items: Group[] }>("categoryGroups");
  const categories = useResource<{ items: Category[] }>("categories");
  const mutation = useMutation();
  const canManage = useCan("admin.taxonomy.manage");

  const [draft, setDraft] = useState<Draft | null>(null);
  const [toast, setToast] = useState<string | null>(null);
  const errors = fieldErrorsFor(mutation.error);

  function open(group: Group | null): void {
    mutation.reset();
    setDraft(
      group
        ? {
            id: group.id,
            code: group.code,
            nameAr: group.nameAr,
            nameEn: group.nameEn ?? "",
            iconKey: group.iconKey ?? "",
            sortOrder: String(group.sortOrder),
          }
        : { id: null, code: "", nameAr: "", nameEn: "", iconKey: "", sortOrder: "0" },
    );
  }

  async function save(): Promise<void> {
    if (!draft) return;
    const ok =
      draft.id === null
        ? await mutation.run("categoryGroupCreate", {
            code: draft.code.trim(),
            nameAr: draft.nameAr.trim(),
            nameEn: draft.nameEn.trim(),
            sortOrder: Number(draft.sortOrder) || 0,
          })
        : await mutation.run("categoryGroupUpdate", {
            id: draft.id,
            nameAr: draft.nameAr.trim(),
            nameEn: draft.nameEn.trim(),
            iconKey: draft.iconKey.trim(),
            sortOrder: Number(draft.sortOrder) || 0,
          });
    if (!ok) return;
    setToast(draft.id === null ? "تمت إضافة المجموعة." : "تم حفظ المجموعة.");
    setDraft(null);
    groups.reload();
  }

  async function toggle(group: Group): Promise<void> {
    const ok = await mutation.run("categoryGroupUpdate", { id: group.id, active: !group.active });
    if (!ok) return;
    setToast(group.active ? "تم تعطيل المجموعة." : "تم تفعيل المجموعة.");
    groups.reload();
  }

  const members = (group: Group) =>
    (categories.data?.items ?? []).filter((category) => category.groupId === group.id);

  return (
    <div className="stack">
      <PageHeader
        eyebrow="الدليل"
        title="مجموعات التصنيفات"
        description="البنية العليا للتصنيفات: كل تصنيف ينتمي إلى مجموعة واحدة."
        actions={
          canManage ? (
            <button
              type="button"
              className="page-hero-action"
              data-testid="new-group"
              onClick={() => open(null)}
            >
              <Icon name="plus" />
              مجموعة جديدة
            </button>
          ) : null
        }
      />
      {groups.loading ? <LoadingState /> : null}
      {groups.error ? <ErrorState error={groups.error} onRetry={groups.reload} /> : null}
      {mutation.error && draft === null ? <ErrorState error={mutation.error} /> : null}

      {groups.data ? (
        <ul className="profile-grid" data-testid="groups">
          {groups.data.items.map((group, index) => {
            const inside = members(group);
            return (
              <ItemCard
                key={group.id}
                index={index}
                icon={iconOf(group.iconKey)}
                tone={group.active ? "brand" : "neutral"}
                muted={!group.active}
                title={group.nameAr}
                subtitle={group.nameEn || "بلا اسم إنكليزي"}
                state={group.active ? { label: "مفعّلة", tone: "positive" } : { label: "معطّلة" }}
                facts={[
                  { label: "التصنيفات", value: NUMBER.format(inside.length) },
                  {
                    label: "المفعّل منها",
                    value: NUMBER.format(inside.filter((category) => category.active).length),
                  },
                  { label: "الترتيب", value: NUMBER.format(group.sortOrder) },
                ]}
                testId={`group-${group.code}`}
                actions={
                  canManage ? (
                    <>
                      <button
                        type="button"
                        className="profile-act-main"
                        data-testid={`edit-group-${group.code}`}
                        onClick={() => open(group)}
                      >
                        <Icon name="edit" width={16} height={16} />
                        تعديل
                      </button>
                      <button
                        type="button"
                        className={group.active ? "profile-act-danger" : "profile-act-quiet"}
                        disabled={mutation.pending}
                        data-testid={`toggle-group-${group.code}`}
                        onClick={() => toggle(group)}
                      >
                        {group.active ? "تعطيل" : "تفعيل"}
                      </button>
                    </>
                  ) : null
                }
              >
                <ItemChips
                  items={inside.map((category) => category.nameAr)}
                  max={4}
                  empty="لا تصنيفات فيها بعد."
                />
              </ItemCard>
            );
          })}
        </ul>
      ) : null}

      <FormDialog
        open={draft !== null}
        icon={iconOf(draft?.iconKey)}
        title={draft?.id === null ? "مجموعة جديدة" : "تعديل المجموعة"}
        description={
          draft?.id === null
            ? "الرمز يُحدَّد مرة واحدة ولا يمكن تغييره لاحقاً."
            : "الرمز ثابت. الاسم والأيقونة والترتيب تظهر فوراً في التطبيقات."
        }
        onClose={() => setDraft(null)}
        locked={mutation.pending}
        testId="group-dialog"
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
              data-testid={draft?.id === null ? "create-group" : "save-group"}
              disabled={
                mutation.pending ||
                !draft?.nameAr.trim() ||
                (draft.id === null && !draft.code.trim())
              }
              onClick={save}
            >
              {mutation.pending ? "جارٍ الحفظ…" : draft?.id === null ? "إضافة المجموعة" : "حفظ"}
            </button>
          </div>
        }
      >
        {draft ? (
          <div className="stack">
            {mutation.error && Object.keys(errors).length === 0 ? (
              <ErrorState error={mutation.error} />
            ) : null}
            <label className="field">
              <span>الاسم بالعربية</span>
              <input
                value={draft.nameAr}
                placeholder="مثال: الصحة"
                data-testid={draft.id === null ? "group-name-ar" : "edit-group-name-ar"}
                aria-invalid={Boolean(errors.nameAr)}
                onChange={(event) => setDraft({ ...draft, nameAr: event.target.value })}
              />
              {errors.nameAr ? <span className="field-error">{errors.nameAr}</span> : null}
            </label>
            {draft.id === null ? (
              <label className="field">
                <span>الرمز</span>
                <input
                  dir="ltr"
                  value={draft.code}
                  placeholder="health"
                  data-testid="group-code"
                  aria-invalid={Boolean(errors.code)}
                  onChange={(event) => setDraft({ ...draft, code: event.target.value })}
                />
                {errors.code ? <span className="field-error">{errors.code}</span> : null}
              </label>
            ) : null}
            <div className="form-grid-2">
              <label className="field">
                <span>الاسم بالإنكليزية</span>
                <input
                  dir="ltr"
                  value={draft.nameEn}
                  onChange={(event) => setDraft({ ...draft, nameEn: event.target.value })}
                />
              </label>
              <label className="field">
                <span>الترتيب</span>
                <input
                  type="number"
                  dir="ltr"
                  value={draft.sortOrder}
                  onChange={(event) => setDraft({ ...draft, sortOrder: event.target.value })}
                />
              </label>
            </div>
            {draft.id !== null ? (
              <fieldset className="icon-picker" data-testid="edit-group-icon">
                <legend>الأيقونة</legend>
                {GROUP_ICONS.map((name) => (
                  <button
                    key={name}
                    type="button"
                    aria-pressed={draft.iconKey === name}
                    aria-label={name}
                    data-testid={`group-icon-${name}`}
                    onClick={() => setDraft({ ...draft, iconKey: name })}
                  >
                    <Icon name={name} width={22} height={22} />
                  </button>
                ))}
                {errors.iconKey ? <span className="field-error">{errors.iconKey}</span> : null}
              </fieldset>
            ) : null}
          </div>
        ) : null}
      </FormDialog>

      <Toast message={toast} onDismiss={() => setToast(null)} />
    </div>
  );
}
