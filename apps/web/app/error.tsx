"use client";

import Link from "next/link";
import { useEffect } from "react";
import { Illustration } from "../components/ui";

/**
 * What a reader sees when a page throws.
 *
 * Without this file Next shows its own screen: in development a stack trace, and in production a
 * bare line of English on a white page. Neither tells somebody looking for a pharmacy what has
 * happened or what to do, and both drop them out of the site entirely — no bar, no footer, no way
 * back except the browser's own button.
 *
 * It offers the two things that actually help: try again, which re-renders the route without a
 * full reload, and go home. The error itself is logged to the console rather than shown; a digest
 * means nothing to a reader and the message can carry internals.
 */
export default function Error({ error, reset }: { error: Error & { digest?: string }; reset: () => void }) {
  useEffect(() => {
    console.error(error);
  }, [error]);

  return (
    <div className="shell page">
      <div className="card state" role="alert">
        <Illustration name="error" size={96} />
        <strong>حدث خطأ غير متوقع.</strong>
        <p>تعذّر عرض هذه الصفحة. يمكنك المحاولة مرة أخرى أو العودة إلى الصفحة الرئيسية.</p>
        <div className="actions">
          <button type="button" className="button" onClick={reset}>
            حاول مرة أخرى
          </button>
          <Link className="button button-alt" href="/">
            الصفحة الرئيسية
          </Link>
        </div>
      </div>
    </div>
  );
}
