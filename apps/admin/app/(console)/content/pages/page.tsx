"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";

import { useCan } from "../../../../components/admin-shell";
import {
  type Column,
  ConfirmDialog,
  DataTable,
  EmptyState,
  ErrorState,
  LoadingState,
  PageHeader,
  formatDateTime,
  TermBadge,
} from "../../../../components/ui";
import { CharCount } from "../../../../components/ui/extra";
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

const BLANK = { slug: "", titleAr: "", kind: "PAGE", bodyAr: "" };

const MESSAGES = {
  titleAr: "اكتب عنوان الصفحة، ١٨٠ حرفاً على الأكثر.",
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

  const columns: readonly Column<ContentPage>[] = [
    {
      key: "title",
      header: "الصفحة",
      required: true,
      sortValue: (row) => row.titleAr,
      render: (row) => (
        <div className="cell-stack">
          <Link href={`/content/pages/${encodeURIComponent(row.slug)}`}>{row.titleAr}</Link>
          <span className="muted cell-ltr">/{row.slug}</span>
        </div>
      ),
    },
    {
      key: "kind",
      header: "النوع",
      render: (row) => (
        <span className="cell-stack">
          <span>{PAGE_KINDS[row.kind] ?? row.kind}</span>
          {row.builtIn ? <span className="muted">صفحة أساسية</span> : null}
        </span>
      ),
    },
    {
      key: "status",
      header: "الحالة",
      render: (row) => (
        <span className="button-row">
          <TermBadge group="pageState" value={row.published ? "PUBLISHED" : "UNPUBLISHED"} />
          {row.hasUnpublishedChanges ? <TermBadge group="pageState" value="CHANGES" /> : null}
        </span>
      ),
    },
    {
      key: "version",
      header: "الإصدار",
      render: (row) => (
        <span className="cell-stack">
          <span>{`الإصدار ${NUMBER.format(row.version)}`}</span>
          <span className="muted">
            {row.publishedVersion === null
              ? "لم يُنشر أي إصدار"
              : `المنشور: ${NUMBER.format(row.publishedVersion)}`}
          </span>
        </span>
      ),
    },
    {
      key: "updatedAt",
      header: "آخر تعديل",
      ltr: true,
      sortValue: (row) => row.updatedAt,
      sortFirst: "desc",
      render: (row) => formatDateTime(row.updatedAt),
    },
    {
      key: "actions",
      header: "",
      width: "1%",
      render: (row) => (
        <Link
          className="button-ghost"
          href={`/content/pages/${encodeURIComponent(row.slug)}`}
          data-testid={`edit-page-${row.slug}`}
        >
          {canManage ? "تحرير" : "عرض"}
        </Link>
      ),
    },
  ];

  return (
    <div className="stack">
      <PageHeader
        title="الصفحات"
        description="نصوص المنصة: الشروط والخصوصية والأسئلة الشائعة وكل صفحة تضيفها."
        actions={
          canManage ? (
            <button
              type="button"
              className="button-primary"
              data-testid="new-page"
              onClick={() => {
                mutation.reset();
                setProblem({});
                setDraft({ ...BLANK });
              }}
            >
              صفحة جديدة
            </button>
          ) : null
        }
      />

      {pages.loading ? <LoadingState /> : null}
      {pages.error ? <ErrorState error={pages.error} onRetry={pages.reload} /> : null}
      {pages.data ? (
        <DataTable
          caption="الصفحات"
          columns={columns}
          rows={pages.data.items}
          rowKey={(row) => row.slug}
          empty={<EmptyState title="لا صفحات بعد" />}
        />
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
