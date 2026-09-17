export type OperationRoute = Readonly<{
  title: string;
  description: string;
  permission: string;
  endpoint: string;
  primaryAction?: string;
}>;

export const operationRoutes = {
  reviews: {
    title: "طلبات المراجعة",
    description: "مراجعة طلبات التسجيل وإعادة التحقق والأدلة الخاصة.",
    permission: "admin.reviews.read",
    endpoint: "/api/v1/admin/applications/",
    primaryAction: "فتح الطلب",
  },
  facilities: {
    title: "المنشآت",
    description: "متابعة المنشآت وحالات التعليق وإعادة التفعيل والإغلاق.",
    permission: "admin.facilities.read",
    endpoint: "/api/v1/admin/facilities/",
  },
  users: {
    title: "المستخدمون",
    description: "البحث عن الحسابات ومراجعة الحالة والأدوار المصرح بها.",
    permission: "admin.users.read",
    endpoint: "/api/v1/admin/users/",
  },
  taxonomyGroups: {
    title: "مجموعات التصنيفات",
    description: "إدارة بنية المجموعات والترتيب والتفعيل.",
    permission: "admin.taxonomy.read",
    endpoint: "/api/v1/admin/category-groups/",
  },
  taxonomyCategories: {
    title: "التصنيفات",
    description: "إدارة التصنيفات والقدرات ومفاتيح المحافظات وسياسات التحقق.",
    permission: "admin.taxonomy.read",
    endpoint: "/api/v1/admin/categories/",
  },
  provinces: {
    title: "المحافظات",
    description: "التحكم في تفعيل المحافظات ومتابعة جاهزية التوسع.",
    permission: "admin.provinces.read",
    endpoint: "/api/v1/admin/provinces/",
  },
  verification: {
    title: "متطلبات التحقق",
    description: "إدارة الأدلة المطلوبة لكل تصنيف بدون كشف ملفات خاصة للعامة.",
    permission: "admin.verification.read",
    endpoint: "/api/v1/admin/verification-requirements/",
  },
  ads: {
    title: "الإعلانات",
    description: "إعلانات الطرف الأول: الاستهداف والجدولة والترتيب والحالة.",
    permission: "admin.ads.read",
    endpoint: "/api/v1/admin/ads/",
  },
  audit: {
    title: "سجل التدقيق",
    description: "سجل العمليات مع actor وresource وrequestId بدون أسرار خام.",
    permission: "admin.audit.read",
    endpoint: "/api/v1/admin/audit/",
  },
  analytics: {
    title: "التحليلات",
    description: "مؤشرات تشغيلية وقياسات استخدام تراعي تقليل البيانات.",
    permission: "admin.analytics.read",
    endpoint: "/api/v1/admin/analytics/",
  },
  settings: {
    title: "الإعدادات",
    description: "إعدادات المنصة typed مع سجل تدقيق لكل تغيير.",
    permission: "admin.settings.read",
    endpoint: "/api/v1/admin/settings/",
  },
  system: {
    title: "حالة النظام",
    description: "حالة API وقاعدة البيانات وRedis وCelery والتخزين والعقد.",
    permission: "admin.system.read",
    endpoint: "/api/v1/admin/system/status/",
  },
} satisfies Record<string, OperationRoute>;
