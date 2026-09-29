import type { Metadata, Viewport } from "next";
import "./globals.css";
import { SiteShell } from "../components/site-shell";
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

export const viewport: Viewport = { width: "device-width", initialScale: 1 };

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="ar" dir="rtl">
      <body><SiteShell>{children}</SiteShell></body>
    </html>
  );
}
