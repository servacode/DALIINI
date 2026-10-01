"use client";

import { useEffect } from "react";

/**
 * The last resort: a throw in the root layout itself.
 *
 * `app/error.tsx` catches everything inside the layout, which is almost everything — but when the
 * layout is what failed, there is no layout to render a message into. This file replaces the
 * whole document, which is why it carries its own `<html>`, its own `<body>` and its own styles:
 * the stylesheet the site loads belongs to the layout that just failed, so nothing here may
 * depend on it.
 *
 * It is therefore deliberately plain, and written so it cannot itself fail.
 */
export default function GlobalError({ error }: { error: Error & { digest?: string } }) {
  useEffect(() => {
    console.error(error);
  }, [error]);

  return (
    <html lang="ar" dir="rtl">
      <body
        style={{
          margin: 0,
          minHeight: "100dvh",
          display: "grid",
          placeItems: "center",
          padding: "24px",
          background: "#042623",
          color: "#f7f7f5",
          fontFamily: "Tajawal, 'Segoe UI', Tahoma, Arial, sans-serif",
          textAlign: "center",
        }}
      >
        <main style={{ maxWidth: "40ch", display: "grid", gap: "16px" }}>
          <h1 style={{ margin: 0, fontSize: "28px" }}>تعذّر تحميل الموقع</h1>
          <p style={{ margin: 0, color: "#9fb8ae", lineHeight: 1.6 }}>
            حدث خطأ غير متوقع. يرجى إعادة تحميل الصفحة، وإن تكرّر الأمر فحاول بعد قليل.
          </p>
          {/*
            * A plain anchor, deliberately. `next/link` navigates with the router, and the router
            * lives in the tree that has just failed; this has to leave the broken document behind
            * and fetch a new one.
            */}
          {/* eslint-disable-next-line @next/next/no-html-link-for-pages -- a reload is the point */}
          <a
            href="/"
            style={{
              justifySelf: "center",
              padding: "12px 24px",
              borderRadius: "999px",
              background: "#0b6b47",
              color: "#fff",
              textDecoration: "none",
              fontWeight: 700,
            }}
          >
            إعادة التحميل
          </a>
        </main>
      </body>
    </html>
  );
}
