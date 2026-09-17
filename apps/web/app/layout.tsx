import type { Metadata } from "next";
import "./globals.css";
import { SiteShell } from "../components/site-shell";

export const metadata: Metadata = {
  title: { default: "الدليل", template: "%s | الدليل" },
  description: "منصة دليل محلية للبحث عن المنشآت والخدمات المتاحة.",
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="ar" dir="rtl">
      <body><SiteShell>{children}</SiteShell></body>
    </html>
  );
}
