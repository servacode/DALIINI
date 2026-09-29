import { Fragment, type ReactNode } from "react";
import type { ContentPage } from "../lib/api";
import { parseContent } from "../lib/content";
import { calendarDate } from "../lib/dates";

/* A published text rendered block by block; headings start at h2 under the page's h1. */
export function ContentBody({ body }: { body: string }) {
  return (
    <>
      {parseContent(body).map((block, i) => {
        if (block.type === "heading") return <h2 key={i}>{block.text}</h2>;
        if (block.type === "list") {
          return (
            <ul key={i} className="bullets">
              {block.items.map((item, j) => <li key={j}>{item}</li>)}
            </ul>
          );
        }
        return (
          <p key={i}>
            {block.lines.map((line, j) => (
              <Fragment key={j}>
                {j > 0 ? <br /> : null}
                {line}
              </Fragment>
            ))}
          </p>
        );
      })}
    </>
  );
}

/* «آخر تحديث: ٢٨ أيلول ٢٠٢٦»: when the version shown was published. */
export function LastUpdated({ page }: { page: ContentPage }) {
  const date = calendarDate(page.publishedAt ?? page.updatedAt);
  if (!date) return null;
  return (
    <p className="muted updated">
      آخر تحديث: <time dateTime={date.iso}>{date.text}</time>
    </p>
  );
}

/* A page published from the console: its title, date and text, then anything the route adds. */
export function PublishedArticle({ page, children }: { page: ContentPage; children?: ReactNode }) {
  return (
    <article className="shell legal">
      <h1>{page.titleAr}</h1>
      <LastUpdated page={page} />
      <ContentBody body={page.bodyAr} />
      {children}
    </article>
  );
}
