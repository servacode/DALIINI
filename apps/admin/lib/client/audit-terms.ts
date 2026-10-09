/**
 * The audit trail in Arabic (DECISION-113).
 *
 * The backend records an action as a stable code (`facility.suspended`) and its target as a
 * model name (`FacilityApplication`). Those stay the record — they are what an export, a
 * filter and a support request quote — but the console reads them out as sentences: an
 * operator scanning the day's work should not have to translate `user.admin_roles.replaced`.
 *
 * A code this table does not know is still shown in words, built from its parts, and never
 * as an empty cell; the raw code stays one click away in the entry's details.
 */

const ACTIONS: Readonly<Record<string, string>> = {
  "account.deleted": "حُذف حساب",
  "account.deletion_requested": "طُلب حذف حساب",
  "account.phone.changed": "تغيّر رقم هاتف حساب",
  "account.welcome": "رُحّب بحساب جديد",
  "admin_role.created": "أُنشئ دور",
  "admin_role.deleted": "حُذف دور",
  "admin_role.granted_from_shell": "مُنح دور من الخادم",
  "admin_role.updated": "عُدّلت صلاحيات دور",
  "advertisement.created": "أُضيف إعلان",
  "advertisement.deleted": "حُذف إعلان",
  "advertisement.image.uploaded": "رُفعت صورة إعلان",
  "advertisement.updated": "عُدّل إعلان",
  "app_release.updated": "تغيّر إصدار التطبيق المطلوب",
  "category.capabilities.updated": "عُدّلت قدرات تصنيف",
  "category.created": "أُنشئ تصنيف",
  "category.province.updated": "تغيّر مفتاح تصنيف في محافظة",
  "category.updated": "عُدّل تصنيف",
  "category_group.created": "أُنشئت مجموعة تصنيفات",
  "category_group.updated": "عُدّلت مجموعة تصنيفات",
  "city.updated": "تغيّرت حالة مدينة",
  "contact_message.handled": "عولجت رسالة تواصل",
  "content_page.created": "أُنشئت صفحة",
  "content_page.deleted": "حُذفت صفحة",
  "content_page.updated": "عُدّلت صفحة",
  "duty.imported": "استُورد جدول مناوبات",
  "duty_rotation.applied": "طُبّق تناوب مناوبات",
  "duty_rotation.created": "أُنشئ تناوب مناوبات",
  "duty_rotation.deleted": "حُذف تناوب مناوبات",
  "duty_rotation.updated": "عُدّل تناوب مناوبات",
  "duty_shift.created": "أُضيفت مناوبة",
  "duty_shift.deleted": "حُذفت مناوبة",
  "duty_shift.updated": "عُدّلت مناوبة",
  "emergency_number.created": "أُضيف رقم طوارئ",
  "emergency_number.deleted": "حُذف رقم طوارئ",
  "emergency_number.updated": "عُدّل رقم طوارئ",
  "facility.active": "أُعيد تفعيل منشأة",
  "facility.admin.created": "أضافت الإدارة منشأة",
  "facility.admin.updated": "عدّلت الإدارة منشأة",
  "facility.claim.evidence_added": "أُرفق دليل بطلب ملكية",
  "facility.claim.evidence_removed": "أُزيل دليل من طلب ملكية",
  "facility.claim.started": "بدأ طلب ملكية منشأة",
  "facility.claim.submitted": "أُرسل طلب ملكية منشأة",
  "facility.claim.withdrawn": "سُحب طلب ملكية منشأة",
  "facility.closed": "أُغلقت منشأة",
  "facility.closure.cancelled": "أُلغي إغلاق مؤقت",
  "facility.closure.created": "سُجّل إغلاق مؤقت",
  "facility.evidence.created": "أُرفق دليل تحقق",
  "facility.evidence.deleted": "حُذف دليل تحقق",
  "facility.hours.confirmed": "أُكّدت أوقات الدوام",
  "facility.hours.replaced": "تغيّرت أوقات الدوام",
  "facility.invitation.accepted": "قُبلت دعوة إلى منشأة",
  "facility.invitation.declined": "رُفضت دعوة إلى منشأة",
  "facility.invitation.revoked": "سُحبت دعوة إلى منشأة",
  "facility.invitation.sent": "أُرسلت دعوة إلى منشأة",
  "facility.member.deleted": "أُزيل عضو من منشأة",
  "facility.member.upserted": "أُضيف عضو إلى منشأة",
  "facility.owner_change.submitted": "أرسل المالك تعديلاً",
  "facility.owner_change.withdrawn": "سحب المالك تعديلاً",
  "facility.owner_core.updated": "عدّل المالك بيانات منشأته",
  "facility.owner_draft.created": "بدأ مالك تسجيل منشأة",
  "facility.owner_location.updated": "عدّل المالك موقع منشأته",
  "facility.owner_submitted": "أرسل مالك منشأته للمراجعة",
  "facility.public_image.created": "أُضيفت صورة منشأة",
  "facility.public_image.deleted": "حُذفت صورة منشأة",
  "facility.suspended": "أُوقفت منشأة",
  "facility_application.approved": "قُبل طلب مراجعة",
  "facility_application.rejected": "رُفض طلب مراجعة",
  "facility_report.dismissed": "رُفض بلاغ",
  "facility_report.resolved": "عولج بلاغ",
  "faq_entry.created": "أُضيف سؤال شائع",
  "faq_entry.deleted": "حُذف سؤال شائع",
  "faq_entry.updated": "عُدّل سؤال شائع",
  "mfa.disabled": "أُوقف التحقق بخطوتين",
  "mfa.enabled": "فُعّل التحقق بخطوتين",
  "mfa.recovery_code_used": "استُعمل رمز احتياطي للدخول",
  "mfa.reset": "أُعيد ضبط التحقق بخطوتين",
  "mfa.verified": "تحقّق مشغّل بخطوتين",
  "notification.broadcast.sent": "أُرسل إشعار جماعي",
  "platform_setting.updated": "تغيّر إعداد في المنصة",
  "province.updated": "تغيّرت حالة محافظة",
  "rejection_template.created": "أُضيف قالب رفض",
  "rejection_template.deleted": "حُذف قالب رفض",
  "rejection_template.updated": "عُدّل قالب رفض",
  "service_tag.created": "أُضيفت خدمة",
  "service_tag.deleted": "حُذفت خدمة",
  "service_tag.updated": "عُدّلت خدمة",
  "specialty.created": "أُضيف تخصص",
  "specialty.deleted": "حُذف تخصص",
  "specialty.updated": "عُدّل تخصص",
  "user.admin_roles.replaced": "تغيّرت أدوار مشغّل",
  "user.blocked": "حُظر حساب",
  "user.created": "أُنشئ حساب من اللوحة",
  "user.recovery_sent": "أُرسل رمز استعادة لحساب",
  "user.unblocked": "فُكّ حظر حساب",
  "verification_evidence.viewed": "فُتح دليل تحقق خاص",
  "verification_requirement.created": "أُضيف متطلب تحقق",
  "verification_requirement.updated": "عُدّل متطلب تحقق",
};

/** What each audited model is called, for the target column. */
const TARGETS: Readonly<Record<string, string>> = {
  AdminRole: "دور",
  Advertisement: "إعلان",
  AppRelease: "إصدار التطبيق",
  Category: "تصنيف",
  CategoryGroup: "مجموعة تصنيفات",
  CategoryProvince: "مفتاح تصنيف",
  City: "مدينة",
  ContactMessage: "رسالة تواصل",
  ContentPage: "صفحة",
  DutyRotation: "تناوب مناوبات",
  DutyShift: "مناوبة",
  EmergencyNumber: "رقم طوارئ",
  Facility: "منشأة",
  FacilityApplication: "طلب مراجعة",
  FacilityClosure: "إغلاق مؤقت",
  FacilityEvidence: "دليل تحقق",
  FacilityImage: "صورة منشأة",
  FacilityInvitation: "دعوة",
  FacilityMember: "عضو منشأة",
  FacilityReport: "بلاغ",
  FaqEntry: "سؤال شائع",
  NotificationBroadcast: "إشعار جماعي",
  PlatformSetting: "إعداد",
  Province: "محافظة",
  RejectionTemplate: "قالب رفض",
  ServiceTag: "خدمة",
  Specialty: "تخصص",
  User: "حساب",
  VerificationRequirement: "متطلب تحقق",
};

const NOUNS: Readonly<Record<string, string>> = {
  account: "حساب",
  admin_role: "دور",
  advertisement: "إعلان",
  category: "تصنيف",
  city: "مدينة",
  facility: "منشأة",
  facility_application: "طلب مراجعة",
  facility_report: "بلاغ",
  province: "محافظة",
  user: "حساب",
};

const VERBS: Readonly<Record<string, string>> = {
  created: "إنشاء",
  updated: "تعديل",
  deleted: "حذف",
  approved: "قبول",
  rejected: "رفض",
  submitted: "إرسال",
  sent: "إرسال",
  blocked: "حظر",
  unblocked: "فك حظر",
  suspended: "إيقاف",
  closed: "إغلاق",
  active: "تفعيل",
  resolved: "معالجة",
  dismissed: "رفض",
};

/** «أُوقفت منشأة», or for an unknown code, its parts in words: «تعديل · منشأة». */
export function actionLabel(action: string): string {
  const known = ACTIONS[action];
  if (known) return known;
  const parts = action.split(".");
  const verb = VERBS[parts.at(-1) ?? ""];
  const noun = NOUNS[parts[0] ?? ""];
  if (verb && noun) return `${verb} ${noun}`;
  if (verb) return verb;
  return "عملية إدارية";
}

/** «منشأة», «طلب مراجعة»; an unknown model is a «سجل». */
export function targetLabel(type: string | null | undefined): string {
  if (!type) return "—";
  return TARGETS[type] ?? "سجل";
}

/** Every known action, in Arabic order, for a filter that picks one. */
export const ACTION_OPTIONS: readonly { value: string; label: string }[] = Object.entries(ACTIONS)
  .map(([value, label]) => ({ value, label }))
  .sort((a, b) => a.label.localeCompare(b.label, "ar"));
