"use client";

import { useRouter } from "next/navigation";
import { use, useCallback, useEffect, useState } from "react";

import { useCan } from "../../../../../components/admin-shell";
import {
  ConfirmDialog,
  ErrorState,
  KeyValueList,
  LoadingState,
  PageHeader,
  Panel,
  TermBadge,
  Toast,
  formatDateTime,
} from "../../../../../components/ui";
import { CharCount } from "../../../../../components/ui/extra";
import { PAGE_KINDS, bodyBlocks } from "../../../../../lib/client/content";
import { useMutation } from "../../../../../lib/client/use-mutation";
import { useResource } from "../../../../../lib/client/use-resource";
import { fieldErrorsFor } from "../../../../../lib/errors/messages";
import { LOCALE } from "../../../../../lib/locale";

type ContentPage = Readonly<{
  slug: string;
  kind: string;
  titleAr: string;
  bodyAr: string;
  published: boolean;
  version: number;
  publishedVersion: number | null;
  hasUnpublishedChanges: boolean;
  builtIn: boolean;
  publishedAt: string | null;
  updatedAt: string;
}>;

const NUMBER = new Intl.NumberFormat(LOCALE);
const TITLE_MAX = 180;
const BODY_MAX = 50_000;

/**
 * One page: its words, its preview, and whether the public reads it.
 *
 * Once a page has been published, saving writes a new version and leaves the published one
 * live until the operator publishes again, so an edit in progress never reaches the app
 * half-done. The aside says which version is live and whether newer words are waiting.
 */
export default function ContentPageEditorPage({ params }: { params: Promise<{ slug: string }> }) {
  const { slug } = use(params);
  const page = useResource<ContentPage>("contentPage", { slug });

  // The saved page stays on screen while it reloads after a save, instead of flashing back
  // to a spinner; the editor below re-seeds from each new version.
  const [shown, setShown] = useState<ContentPage | null>(null);
  if (page.data && page.data !== shown) setShown(page.data);
  // Held here, not in the editor: the editor remounts with each saved version, and a message
  // kept inside it would vanish with the version it announced.
  const [toast, setToast] = useState<string | null>(null);
  const dismissToast = useCallback(() => setToast(null), []);

  return (
    <div className="stack">
      {page.loading && !shown ? <LoadingState /> : null}
      {page.error ? (
        <>
          <PageHeader title="الصفحة" back={{ href: "/content/pages", label: "الصفحات" }} />
          <ErrorState error={page.error} onRetry={page.reload} />
        </>
      ) : null}
      {shown && !page.error ? (
        <Editor
          key={`${shown.slug}:${shown.version}:${String(shown.updatedAt)}:${String(shown.published)}`}
          page={shown}
          onChanged={(message) => {
            setToast(message);
            page.reload();
          }}
        />
      ) : null}
      <Toast message={toast} onDismiss={dismissToast} />
    </div>
  );
}

function Editor({
  page,
  onChanged,
}: {
  page: ContentPage;
  /** After a save or a publishing change: what to tell the operator. */
  onChanged: (message: string) => void;
}) {
  const canManage = useCan("admin.content.manage");
  const mutation = useMutation();
  const router = useRouter();
  const [draft, setDraft] = useState({ titleAr: page.titleAr, bodyAr: page.bodyAr, kind: page.kind });
  const [view, setView] = useState<"edit" | "preview">("edit");
  const [confirm, setConfirm] = useState<"publish" | "unpublish" | "delete" | null>(null);

  const dirty =
    draft.titleAr !== page.titleAr || draft.bodyAr !== page.bodyAr || draft.kind !== page.kind;
  const server = fieldErrorsFor(mutation.error);
  const titleProblem = !draft.titleAr.trim()
    ? "اكتب عنوان الصفحة."
    : server.titleAr
      ? `العنوان ${NUMBER.format(TITLE_MAX)} حرفاً على الأكثر.`
      : null;
  const bodyProblem = !draft.bodyAr.trim()
    ? "اكتب نص الصفحة."
    : server.bodyAr
      ? "تعذّر حفظ النص. تأكد أنه لا يتجاوز الحد المسموح."
      : null;
  const invalid = Boolean(titleProblem || bodyProblem);

  // Leaving with unsaved words asks first; the browser writes the question itself.
  useEffect(() => {
    if (!dirty) return;
    const warn = (event: BeforeUnloadEvent) => event.preventDefault();
    window.addEventListener("beforeunload", warn);
    return () => window.removeEventListener("beforeunload", warn);
  }, [dirty]);

  const words = { titleAr: draft.titleAr.trim(), bodyAr: draft.bodyAr, kind: draft.kind };

  async function save(): Promise<void> {
    if (invalid) return;
    const ok = await mutation.run("contentPageUpdate", { slug: page.slug, ...words });
    if (!ok) return;
    onChanged(
      page.publishedVersion !== null
        ? "حُفظت التعديلات في إصدار جديد. انشره ليظهر للعامة."
        : "حُفظت التعديلات.",
    );
  }

  async function decide(): Promise<void> {
    if (confirm === "delete") {
      const ok = await mutation.run("contentPageDelete", { slug: page.slug });
      if (!ok) return;
      setConfirm(null);
      router.replace("/content/pages");
      return;
    }
    const publishing = confirm === "publish";
    const ok = await mutation.run("contentPageUpdate", {
      slug: page.slug,
      published: publishing,
      // Publishing with unsaved words saves them first, as one version, so what goes live is
      // exactly what is on screen.
      ...(publishing && dirty ? words : {}),
    });
    if (!ok) return;
    setConfirm(null);
    onChanged(publishing ? "نُشرت الصفحة، وتظهر الآن في التطبيق والموقع." : "أُلغي نشر الصفحة.");
  }

  const pendingPublish = page.hasUnpublishedChanges || (page.published && dirty);

  return (
    <>
      <PageHeader
        back={{ href: "/content/pages", label: "الصفحات" }}
        eyebrow={PAGE_KINDS[page.kind] ?? page.kind}
        title={page.titleAr}
        description={`\u2066/${page.slug}\u2069`}
        actions={
          canManage ? (
            <>
              {dirty ? (
                <button
                  type="button"
                  className="button-ghost"
                  disabled={mutation.pending}
                  onClick={() => {
                    mutation.reset();
                    setDraft({ titleAr: page.titleAr, bodyAr: page.bodyAr, kind: page.kind });
                  }}
                >
                  تراجع عن التغييرات
                </button>
              ) : null}
              <button
                type="button"
                className="button-primary"
                disabled={!dirty || invalid || mutation.pending}
                aria-busy={(mutation.pending && !confirm) || undefined}
                data-testid="save-page"
                onClick={save}
              >
                {mutation.pending && !confirm ? "جارٍ الحفظ…" : "حفظ التعديلات"}
              </button>
            </>
          ) : null
        }
      />

      {mutation.error && !confirm ? <ErrorState error={mutation.error} /> : null}

      <div className="grid-main-aside">
        <Panel title="المحتوى">
          <label className="field">
            <span className="field-label-row">
              العنوان
              <CharCount value={draft.titleAr} max={TITLE_MAX} />
            </span>
            <input
              value={draft.titleAr}
              disabled={!canManage}
              data-testid="page-title"
              aria-invalid={Boolean(titleProblem)}
              onChange={(event) => setDraft({ ...draft, titleAr: event.target.value })}
            />
            {titleProblem ? <span className="field-error">{titleProblem}</span> : null}
          </label>
          <label className="field">
            <span>النوع</span>
            <select
              value={draft.kind}
              disabled={!canManage}
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

          <div className="field">
            <div className="field-label-row">
              <div className="editor-tabs" role="tablist" aria-label="النص">
                <button
                  type="button"
                  role="tab"
                  aria-selected={view === "edit"}
                  data-testid="tab-edit"
                  onClick={() => setView("edit")}
                >
                  تحرير النص
                </button>
                <button
                  type="button"
                  role="tab"
                  aria-selected={view === "preview"}
                  data-testid="tab-preview"
                  onClick={() => setView("preview")}
                >
                  معاينة
                </button>
              </div>
              <CharCount value={draft.bodyAr} max={BODY_MAX} />
            </div>
            {view === "edit" ? (
              <textarea
                className="body-editor"
                rows={18}
                value={draft.bodyAr}
                disabled={!canManage}
                aria-label="نص الصفحة"
                aria-invalid={Boolean(bodyProblem)}
                data-testid="page-body"
                onChange={(event) => setDraft({ ...draft, bodyAr: event.target.value })}
              />
            ) : (
              <ContentPreview title={draft.titleAr} body={draft.bodyAr} />
            )}
            {bodyProblem ? <span className="field-error">{bodyProblem}</span> : null}
            <span className="field-hint">
              اترك سطراً فارغاً بين الفقرات. السطر الذي ينتهي بعلامة استفهام يُعرض سؤالاً وما بعده
              جوابه.
            </span>
          </div>
        </Panel>

        <div className="stack">
          <Panel title="النشر">
            <div className="button-row">
              <TermBadge group="pageState" value={page.published ? "PUBLISHED" : "UNPUBLISHED"} />
              {page.hasUnpublishedChanges ? <TermBadge group="pageState" value="CHANGES" /> : null}
              {dirty ? <TermBadge group="pageState" value="UNSAVED" /> : null}
            </div>
            {canManage ? (
              <label className="switch-row">
                <span>
                  ظاهرة للعامة
                  <span className="field-hint setting-hint">
                    في التطبيق والموقع، بآخر إصدار منشور.
                  </span>
                </span>
                <input
                  type="checkbox"
                  checked={page.published}
                  disabled={mutation.pending || invalid}
                  data-testid="page-published"
                  onChange={() => {
                    mutation.reset();
                    setConfirm(page.published ? "unpublish" : "publish");
                  }}
                />
              </label>
            ) : null}
            {page.published && pendingPublish ? (
              <div className="publish-pending">
                <p className="notice">
                  {page.hasUnpublishedChanges
                    ? `النسخة المنشورة هي الإصدار ${NUMBER.format(page.publishedVersion ?? 0)}، وفي الصفحة تعديلات أحدث لم تُنشر.`
                    : "في الصفحة تغييرات لم تُحفظ بعد. انشرها لتحفظ وتظهر للعامة معاً."}
                </p>
                {canManage ? (
                  <button
                    type="button"
                    className="button-primary"
                    disabled={mutation.pending || invalid}
                    data-testid="publish-latest"
                    onClick={() => {
                      mutation.reset();
                      setConfirm("publish");
                    }}
                  >
                    نشر آخر التعديلات
                  </button>
                ) : null}
              </div>
            ) : null}
          </Panel>

          <Panel title="الإصدارات">
            <KeyValueList
              items={[
                { label: "الإصدار الحالي", value: NUMBER.format(page.version) },
                {
                  label: "الإصدار المنشور",
                  value:
                    page.publishedVersion === null
                      ? "لم يُنشر بعد"
                      : NUMBER.format(page.publishedVersion),
                },
                { label: "آخر نشر", value: formatDateTime(page.publishedAt), ltr: true },
                { label: "آخر تعديل", value: formatDateTime(page.updatedAt), ltr: true },
              ]}
            />
          </Panel>

          {canManage ? (
            page.builtIn ? (
              <p className="notice">
                صفحة أساسية تعتمد عليها التطبيقات: يمكن إلغاء نشرها، ولا يمكن حذفها.
              </p>
            ) : (
              <Panel title="حذف الصفحة" description="تُحذف الصفحة وكل إصداراتها نهائياً.">
                <div>
                  <button
                    type="button"
                    className="button-ghost"
                    data-tone="danger"
                    data-testid="delete-page"
                    onClick={() => {
                      mutation.reset();
                      setConfirm("delete");
                    }}
                  >
                    حذف الصفحة
                  </button>
                </div>
              </Panel>
            )
          ) : null}
        </div>
      </div>

      <ConfirmDialog
        open={confirm !== null}
        title={
          confirm === "publish"
            ? "نشر الصفحة"
            : confirm === "unpublish"
              ? "إلغاء نشر الصفحة"
              : "حذف الصفحة"
        }
        body={
          confirm === "publish"
            ? dirty
              ? "تُحفظ التغييرات التي على الشاشة في إصدار جديد، ويظهر للعامة فوراً."
              : "يظهر آخر إصدار للعامة فوراً في التطبيق والموقع."
            : confirm === "unpublish"
              ? "تختفي الصفحة من التطبيق والموقع حتى تنشرها من جديد."
              : `تُحذف «${page.titleAr}» وكل إصداراتها نهائياً، ولا يمكن استرجاعها.`
        }
        confirmLabel={
          confirm === "publish"
            ? "تأكيد النشر"
            : confirm === "unpublish"
              ? "تأكيد إلغاء النشر"
              : "تأكيد الحذف"
        }
        destructive={confirm !== "publish"}
        pending={mutation.pending}
        error={mutation.error}
        onConfirm={decide}
        onCancel={() => setConfirm(null)}
      />
    </>
  );
}

/** The body as the site and the app will read it (see `bodyBlocks`). */
function ContentPreview({ title, body }: { title: string; body: string }) {
  const blocks = bodyBlocks(body);
  return (
    <article className="content-preview" data-testid="page-preview">
      <h2>{title.trim() || "بلا عنوان"}</h2>
      {blocks.length === 0 ? <p className="muted">لا نص بعد.</p> : null}
      {blocks.map((block, index) =>
        block.kind === "question" ? (
          <div key={index} className="preview-qa">
            <strong>{block.question}</strong>
            {block.answer.map((line, row) => (
              <p key={row}>{line}</p>
            ))}
          </div>
        ) : (
          <p key={index}>
            {block.lines.map((line, row) => (
              <span key={row}>
                {row > 0 ? <br /> : null}
                {line}
              </span>
            ))}
          </p>
        ),
      )}
    </article>
  );
}
