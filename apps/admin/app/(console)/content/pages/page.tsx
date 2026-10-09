"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";

import { useCan } from "../../../../components/admin-shell";
import { type IconName, Icon } from "../../../../components/icons";
import {
  ConfirmDialog,
  EmptyState,
  ErrorState,
  LoadingState,
  PageHeader,
} from "../../../../components/ui";
import { CharCount, ItemCard, relativeTime } from "../../../../components/ui/extra";
import { PAGE_KINDS, SLUG_PATTERN } from "../../../../lib/client/content";
import { useMutation } from "../../../../lib/client/use-mutation";
import { useResource } from "../../../../lib/client/use-resource";
import { fieldErrorsFor } from "../../../../lib/errors/messages";
import { LOCALE } from "../../../../lib/locale";

type ContentPage = Readonly<{
  slug: string;
  kind: string;
  titleAr: string;
  published: boolean;
  version: number;
  publishedVersion: number | null;
  hasUnpublishedChanges: boolean;
  builtIn: boolean;
  publishedAt: string | null;
  updatedAt: string;
}>;

const NUMBER = new Intl.NumberFormat(LOCALE);

/** What each kind of page wears on its card. */
const KIND_ICONS: Readonly<Record<string, IconName>> = {
  LEGAL: "shield",
  FAQ: "info",
  PAGE: "layers",
};

const BLANK = { slug: "", titleAr: "", kind: "PAGE", bodyAr: "" };

const MESSAGES = {
  titleAr: "اكتب عنوان الصفحة، 180 حرفاً على الأكثر.",
  slug: "المعرّف أحرف إنجليزية وأرقام وشرطات، لا يبدأ بشرطة ولا ينتهي بها.",
  bodyAr: "اكتب نص الصفحة.",
};

/**
 * The platform's own pages: the legal texts, the FAQ page and any page the team adds.
 *
 * Six are built in (the apps link to them), so they can be taken offline but not deleted.
 * A page is written as a new version once it has been published, so the list says which
 * version the public is reading and whether newer words are waiting to be published.
 */
export default function ContentPagesPage() {
  const pages = useResource<{ items: ContentPage[] }>("contentPages");
  const canManage = useCan("admin.content.manage");
  const mutation = useMutation();
  const router = useRouter();

  const [draft, setDraft] = useState<typeof BLANK | null>(null);
  const [problem, setProblem] = useState<Record<string, string>>({});
  // Django words its field errors in English; each field gets its Arabic rule instead.
  const server = fieldErrorsFor(mutation.error);
  const errors: Record<string, string> = {
    ...(server.titleAr ? { titleAr: MESSAGES.titleAr } : {}),
    ...(server.slug ? { slug: MESSAGES.slug } : {}),
    ...(server.bodyAr ? { bodyAr: MESSAGES.bodyAr } : {}),
    ...problem,
  };

  async function create(): Promise<void> {
    if (!draft) return;
    const slug = draft.slug.trim().toLowerCase();
    const local: Record<string, string> = {};
    if (!draft.titleAr.trim()) local.titleAr = MESSAGES.titleAr;
    if (!SLUG_PATTERN.test(slug)) local.slug = MESSAGES.slug;
    if (!draft.bodyAr.trim()) local.bodyAr = MESSAGES.bodyAr;
    setProblem(local);
    if (Object.keys(local).length > 0) return;

    const ok = await mutation.run("contentPageCreate", {
      slug,
      kind: draft.kind,
      titleAr: draft.titleAr.trim(),
      bodyAr: draft.bodyAr.trim(),
      published: false,
    });
    if (!ok) return;
    setDraft(null);
    router.push(`/content/pages/${encodeURIComponent(slug)}`);
  }

  return (
    <div className="stack">
      <PageHeader
        eyebrow="المحتوى"
        title="الصفحات"
        description="نصوص المنصة: الشروط والخصوصية والأسئلة الشائعة وكل صفحة تضيفها."
        actions={
          canManage ? (
            <button
              type="button"
              className="page-hero-action"
              data-testid="new-page"
              onClick={() => {
                mutation.reset();
                setProblem({});
                setDraft({ ...BLANK });
              }}
            >
              <Icon name="plus" />
              صفحة جديدة
            </button>
          ) : null
        }
      />

      {pages.loading ? <LoadingState /> : null}
      {pages.error ? <ErrorState error={pages.error} onRetry={pages.reload} /> : null}
      {pages.data && pages.data.items.length === 0 ? <EmptyState title="لا صفحات بعد" /> : null}
      {pages.data && pages.data.items.length > 0 ? (
        <ul className="profile-grid" data-testid="pages">
          {pages.data.items.map((row, index) => {
            const href = `/content/pages/${encodeURIComponent(row.slug)}`;
            return (
              <ItemCard
                key={row.slug}
                index={index}
                icon={KIND_ICONS[row.kind] ?? "layers"}
                tone={
                  !row.published ? "neutral" : row.hasUnpublishedChanges ? "gold" : "brand"
                }
                title={row.titleAr}
                subtitle={<span dir="ltr">/{row.slug}</span>}
                state={
                  !row.published
                    ? { label: "غير منشورة" }
                    : row.hasUnpublishedChanges
                      ? { label: "تعديلات تنتظر النشر", tone: "warning" }
                      : { label: "منشورة", tone: "positive" }
                }
                facts={[
                  { label: "الإصدار الحالي", value: NUMBER.format(row.version) },
                  {
                    label: "المنشور للعامة",
                    value:
                      row.publishedVersion === null ? "لا شيء" : NUMBER.format(row.publishedVersion),
                  },
                  { label: "آخر تعديل", value: relativeTime(row.updatedAt) },
                ]}
                testId={`page-${row.slug}`}
                actions={
                  <Link
                    className="profile-act-main"
                    href={href}
                    data-testid={`edit-page-${row.slug}`}
                  >
                    <Icon name={canManage ? "edit" : "eye"} width={16} height={16} />
                    {canManage ? "تحرير الصفحة" : "عرض الصفحة"}
                  </Link>
                }
              >
                <p className="item-card-quiet">
                  {`${PAGE_KINDS[row.kind] ?? row.kind}${row.builtIn ? " · صفحة أساسية يربطها التطبيق، تُخفى ولا تُحذف" : ""}`}
                </p>
              </ItemCard>
            );
          })}
        </ul>
      ) : null}

      <ConfirmDialog
        open={draft !== null}
        title="صفحة جديدة"
        body="تُحفظ الصفحة غير منشورة. تنشرها من صفحة التحرير بعد مراجعتها."
        confirmLabel="إنشاء الصفحة"
        pending={mutation.pending}
        error={mutation.error}
        onConfirm={create}
        onCancel={() => setDraft(null)}
      >
        {draft ? (
          <>
            <label className="field">
              <span className="field-label-row">
                العنوان
                <CharCount value={draft.titleAr} max={180} />
              </span>
              <input
                value={draft.titleAr}
                maxLength={180}
                data-testid="page-title"
                aria-invalid={Boolean(errors.titleAr)}
                onChange={(event) => setDraft({ ...draft, titleAr: event.target.value })}
              />
              {errors.titleAr ? <span className="field-error">{errors.titleAr}</span> : null}
            </label>
            <label className="field">
              <span>المعرّف في الرابط</span>
              <input
                dir="ltr"
                value={draft.slug}
                placeholder="about-us"
                maxLength={64}
                data-testid="page-slug"
                aria-invalid={Boolean(errors.slug)}
                onChange={(event) => setDraft({ ...draft, slug: event.target.value })}
              />
              <span className="field-hint">أحرف إنجليزية وأرقام وشرطات، مثل about-us.</span>
              {errors.slug ? <span className="field-error">{errors.slug}</span> : null}
            </label>
            <label className="field">
              <span>النوع</span>
              <select
                value={draft.kind}
                data-testid="page-kind"
                onChange={(event) => setDraft({ ...draft, kind: event.target.value })}
              >
                {Object.entries(PAGE_KINDS).map(([value, label]) => (
                  <option key={value} value={value}>
                    {label}
                  </option>
                ))}
              </select>
            </label>
            <label className="field">
              <span>النص</span>
              <textarea
                rows={5}
                value={draft.bodyAr}
                data-testid="page-body"
                aria-invalid={Boolean(errors.bodyAr)}
                onChange={(event) => setDraft({ ...draft, bodyAr: event.target.value })}
              />
              <span className="field-hint">يمكنك إكماله لاحقاً في صفحة التحرير.</span>
              {errors.bodyAr ? <span className="field-error">{errors.bodyAr}</span> : null}
            </label>
          </>
        ) : null}
      </ConfirmDialog>
    </div>
  );
}
