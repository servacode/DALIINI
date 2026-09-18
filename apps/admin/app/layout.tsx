import type { Metadata } from "next";
import { connection } from "next/server";
import "./globals.css";

export const metadata: Metadata = {
  title: { default: "إدارة الدليل", template: "%s | إدارة الدليل" },
  robots: { index: false, follow: false },
};

/**
 * Every page renders per request.
 *
 * The Content Security Policy carries a nonce generated for each request in `proxy.ts`,
 * and Next can only stamp that nonce on its scripts while rendering one. A page prerendered
 * at build time has no request and so no nonce, and its inline scripts would be blocked.
 * An operations console has nothing worth prerendering, so the whole tree opts in here.
 */
export default async function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  await connection();
  return (
    <html lang="ar" dir="rtl">
      <body>{children}</body>
    </html>
  );
}
