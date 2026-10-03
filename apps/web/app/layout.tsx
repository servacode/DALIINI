import { tokens } from "@servacode/design-tokens/tokens";
import type { Metadata, Viewport } from "next";
import Script from "next/script";
import "./globals.css";
import { SiteShell } from "../components/site-shell";
import { THEME_SCRIPT } from "../components/theme-toggle";
import { SITE_NAME, siteUrl } from "../lib/config";

const description = "دليني: دليل محلي للبحث عن الصيدليات والعيادات والمنشآت والخدمات، مع أوقات الدوام والمناوبات وطرق التواصل.";

export const metadata: Metadata = {
  metadataBase: siteUrl(),
  title: { default: `${SITE_NAME} — دليلك المحلي`, template: `%s | ${SITE_NAME}` },
  description,
  applicationName: SITE_NAME,
  openGraph: { type: "website", locale: "ar_SY", siteName: SITE_NAME, title: SITE_NAME, description },
  twitter: { card: "summary_large_image", title: SITE_NAME, description },
};

/*
 * `themeColor` is the bar's own green, so a phone's browser chrome continues the page instead of
 * sitting white above it. The pair is given so a reader who has asked for a dark system gets the
 * darker of the brand's two bar colours rather than being pulled towards light.
 */
export const viewport: Viewport = {
  width: "device-width",
  initialScale: 1,
  themeColor: [
    { media: "(prefers-color-scheme: light)", color: tokens.colors.barDeep },
    { media: "(prefers-color-scheme: dark)", color: tokens.colorsDark.barDeep },
  ],
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  // The theme script sets `data-theme` on <html> before React hydrates it, so the one attribute
  // the server could not know is expected to differ. `beforeInteractive` is Next's way to put a
  // script in the document's head ahead of everything else; a bare <script> rendered by React
  // also runs, but React warns about it whenever the layout renders on the client (a 404 does).
  return (
    <html lang="ar" dir="rtl" suppressHydrationWarning>
      <body>
        <Script id="theme" strategy="beforeInteractive">
          {THEME_SCRIPT}
        </Script>
        <SiteShell>{children}</SiteShell>
      </body>
    </html>
  );
}
