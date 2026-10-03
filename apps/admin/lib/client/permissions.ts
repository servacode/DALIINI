/**
 * The console's permissions in the operator's words, grouped by the job they belong to.
 *
 * The backend owns the catalogue (`adminPermissionsList`); this file owns how each code reads.
 * A code the backend adds before this file learns it still appears, under «أخرى» and with
 * the backend's own description, so a new permission is never invisible in the role editor.
 */

export type PermissionArea = Readonly<{
  key: string;
  label: string;
  permissions: readonly Readonly<{ code: string; label: string }>[];
}>;

export const PERMISSION_AREAS: readonly PermissionArea[] = [
  {
    key: "operations",
    label: "التشغيل",
    permissions: [
      { code: "admin.dashboard.read", label: "عرض لوحة المتابعة" },
      { code: "admin.analytics.read", label: "عرض التقارير والإحصاءات" },
      { code: "admin.audit.read", label: "عرض سجل العمليات" },
      { code: "realtime.admin", label: "استلام التحديثات الفورية" },
    ],
  },
  {
    key: "reviews",
    label: "المراجعات والبلاغات",
    permissions: [
      { code: "admin.reviews.read", label: "عرض طلبات المراجعة" },
      { code: "admin.reviews.decide", label: "قبول الطلبات ورفضها" },
      { code: "admin.evidence.read", label: "فتح وثائق التحقق الخاصة" },
      { code: "admin.reports.read", label: "عرض البلاغات" },
      { code: "admin.reports.manage", label: "معالجة البلاغات وإغلاقها" },
    ],
  },
  {
    key: "facilities",
    label: "المنشآت والمناوبات",
    permissions: [
      { code: "admin.facilities.read", label: "عرض المنشآت" },
      { code: "admin.facilities.edit", label: "إضافة المنشآت وتصحيح بياناتها" },
      { code: "admin.facilities.manage", label: "إيقاف المنشآت وإعادتها وإغلاقها" },
      { code: "admin.duty.read", label: "عرض جدول المناوبات" },
      { code: "admin.duty.manage", label: "إدارة المناوبات واستيرادها" },
    ],
  },
  {
    key: "people",
    label: "المستخدمون والأدوار",
    permissions: [
      { code: "admin.users.read", label: "عرض المستخدمين" },
      { code: "admin.users.manage", label: "حظر المستخدمين وإلغاء الحظر" },
      { code: "admin.roles.read", label: "عرض الأدوار" },
      { code: "admin.roles.manage", label: "منح الأدوار وتعديلها" },
      { code: "admin.notifications.send", label: "إرسال الإشعارات العامة" },
    ],
  },
  {
    key: "catalogue",
    label: "الدليل",
    permissions: [
      { code: "admin.taxonomy.read", label: "عرض التصنيفات" },
      { code: "admin.taxonomy.manage", label: "إدارة التصنيفات والتخصصات" },
      { code: "admin.verification.read", label: "عرض متطلبات التحقق" },
      { code: "admin.verification.manage", label: "إدارة متطلبات التحقق" },
      { code: "admin.provinces.read", label: "عرض المحافظات والمدن" },
      { code: "admin.provinces.manage", label: "إطلاق المحافظات وإدارتها" },
    ],
  },
  {
    key: "content",
    label: "المحتوى والإعلانات",
    permissions: [
      { code: "admin.content.read", label: "عرض الصفحات والأسئلة والرسائل" },
      { code: "admin.content.manage", label: "تحرير المحتوى والرد على الرسائل" },
      { code: "admin.ads.read", label: "عرض الإعلانات" },
      { code: "admin.ads.manage", label: "إدارة الإعلانات" },
    ],
  },
  {
    key: "platform",
    label: "المنصة",
    permissions: [
      { code: "admin.settings.read", label: "عرض الإعدادات" },
      { code: "admin.settings.manage", label: "تغيير الإعدادات" },
      { code: "admin.system.read", label: "عرض حالة النظام" },
    ],
  },
];

const LABELS: ReadonlyMap<string, string> = new Map(
  PERMISSION_AREAS.flatMap((area) => area.permissions.map((p) => [p.code, p.label] as const)),
);

export function permissionLabel(code: string, fallback?: string): string {
  return LABELS.get(code) ?? (fallback?.trim() || code);
}

/**
 * The areas, with only the codes the backend actually offers, plus «أخرى» for any code this
 * file does not know yet.
 */
export function areasFor(
  catalogue: readonly Readonly<{ code: string; description: string }>[],
): PermissionArea[] {
  const offered = new Set(catalogue.map((item) => item.code));
  const areas = PERMISSION_AREAS.map((area) => ({
    ...area,
    permissions: area.permissions.filter((p) => offered.has(p.code)),
  })).filter((area) => area.permissions.length > 0);
  const unknown = catalogue
    .filter((item) => !LABELS.has(item.code))
    .map((item) => ({ code: item.code, label: permissionLabel(item.code, item.description) }));
  if (unknown.length > 0) areas.push({ key: "other", label: "أخرى", permissions: unknown });
  return areas;
}
