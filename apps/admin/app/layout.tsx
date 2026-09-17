import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = { title: { default: "إدارة الدليل", template: "%s | إدارة الدليل" }, robots: { index: false, follow: false } };
export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) { return <html lang="ar" dir="rtl"><body>{children}</body></html>; }
