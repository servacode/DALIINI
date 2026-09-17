import Link from "next/link";
import type { ReactNode } from "react";

const navigation = [
  ["/dashboard", "لوحة المتابعة"], ["/reviews", "المراجعات"], ["/facilities", "المنشآت"],
  ["/users", "المستخدمون"], ["/taxonomy/categories", "التصنيفات"], ["/provinces", "المحافظات"],
  ["/ads", "الإعلانات"], ["/audit", "سجل التدقيق"], ["/analytics", "التحليلات"], ["/system", "النظام"],
] as const;

export function AdminShell({ children }: { children: ReactNode }) {
  return <div className="admin-shell"><aside className="sidebar"><strong>إدارة الدليل</strong><nav>{navigation.map(([href,label]) => <Link key={href} href={href}>{label}</Link>)}</nav></aside><section className="admin-main"><header className="topbar"><strong>لوحة الإدارة</strong><span>حالة النظام</span></header><main className="content">{children}</main></section></div>;
}
