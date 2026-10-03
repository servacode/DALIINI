// GENERATED — DO NOT EDIT (source: vocabulary.json)
import Foundation

enum DirectoryVocabulary {
    static let availability: [String: (ar: String, tone: String)] = [
        "OPEN": ("مفتوح الآن", "positive"),
        "CLOSED": ("مغلق الآن", "danger"),
        "DUTY": ("مناوب الآن", "accent"),
        "TEMP_CLOSED": ("مغلق مؤقتاً", "info")
    ]
    static let facilityStatus: [String: (ar: String, tone: String)] = [
        "DRAFT": ("مسودة", "neutral"),
        "SUBMITTED": ("قيد المراجعة", "info"),
        "ACTIVE": ("فعّالة", "positive"),
        "SUSPENDED": ("موقوفة", "warning"),
        "CLOSED": ("مغلقة نهائياً", "danger"),
        "REVERIFICATION_REQUIRED": ("تحتاج إعادة تحقق", "warning")
    ]
    static let applicationStatus: [String: (ar: String, tone: String)] = [
        "DRAFT": ("مسودة", "neutral"),
        "SUBMITTED": ("قيد المراجعة", "info"),
        "APPROVED": ("مقبول", "positive"),
        "REJECTED": ("مرفوض", "danger")
    ]
    static let applicationKind: [String: (ar: String, tone: String)] = [
        "INITIAL": ("تسجيل جديد", "info"),
        "REVERIFICATION": ("إعادة تحقق", "warning"),
        "CHANGE": ("تعديل بيانات", "info")
    ]
    static let reportReason: [String: (ar: String, tone: String)] = [
        "WRONG_INFO": ("معلومات خاطئة", "warning"),
        "CLOSED_PERMANENTLY": ("مغلقة نهائياً", "danger"),
        "WRONG_LOCATION": ("الموقع خاطئ", "warning"),
        "WRONG_HOURS": ("أوقات الدوام خاطئة", "warning"),
        "NOT_ON_DUTY": ("ليست مناوبة", "danger"),
        "OTHER": ("أخرى", "neutral")
    ]
    static let reportStatus: [String: (ar: String, tone: String)] = [
        "OPEN": ("مفتوح", "warning"),
        "RESOLVED": ("تمت المعالجة", "positive"),
        "DISMISSED": ("مرفوض", "neutral")
    ]
    static let accountStatus: [String: (ar: String, tone: String)] = [
        "ACTIVE": ("فعّال", "positive"),
        "BLOCKED": ("محظور", "danger")
    ]
    static let switch: [String: (ar: String, tone: String)] = [
        "ON": ("مفعّل", "positive"),
        "OFF": ("معطّل", "neutral")
    ]
    static let trust: [String: (ar: String, tone: String)] = [
        "VERIFIED": ("موثّقة", "brand")
    ]
    static let weekday: [String: (ar: String, tone: String)] = [
        "SATURDAY": ("السبت", "neutral"),
        "SUNDAY": ("الأحد", "neutral"),
        "MONDAY": ("الاثنين", "neutral"),
        "TUESDAY": ("الثلاثاء", "neutral"),
        "WEDNESDAY": ("الأربعاء", "neutral"),
        "THURSDAY": ("الخميس", "neutral"),
        "FRIDAY": ("الجمعة", "neutral")
    ]
    static let contactKind: [String: (ar: String, tone: String)] = [
        "GENERAL": ("استفسار أو اقتراح", "neutral"),
        "OWNER": ("صاحب منشأة", "info"),
        "CORRECTION": ("تصحيح معلومة", "warning")
    ]
    static let emergencyKind: [String: (ar: String, tone: String)] = [
        "AMBULANCE": ("إسعاف", "neutral"),
        "FIRE": ("إطفاء", "neutral"),
        "POLICE": ("شرطة", "neutral"),
        "HOSPITAL": ("مستشفى", "neutral"),
        "OTHER": ("أخرى", "neutral")
    ]
    static let pageKind: [String: (ar: String, tone: String)] = [
        "PAGE": ("صفحة عامة", "neutral"),
        "LEGAL": ("قانونية", "info"),
        "FAQ": ("أسئلة شائعة", "neutral")
    ]
    static let evidenceState: [String: (ar: String, tone: String)] = [
        "COMPLETE": ("الوثائق مكتملة", "positive"),
        "INCOMPLETE": ("وثائق ناقصة", "warning")
    ]
    static let contactState: [String: (ar: String, tone: String)] = [
        "PENDING": ("بانتظار المعالجة", "warning"),
        "HANDLED": ("تمت المعالجة", "positive")
    ]
    static let pageState: [String: (ar: String, tone: String)] = [
        "PUBLISHED": ("منشورة", "positive"),
        "UNPUBLISHED": ("غير منشورة", "neutral"),
        "CHANGES": ("تعديلات لم تُنشر", "warning"),
        "UNSAVED": ("تغييرات لم تُحفظ", "info")
    ]
    static let numberState: [String: (ar: String, tone: String)] = [
        "PUBLIC": ("ظاهر للعامة", "positive"),
        "HIDDEN": ("مخفي", "neutral"),
        "NEEDS_CHECK": ("يحتاج تحققاً", "warning")
    ]
    static let dutySource: [String: (ar: String, tone: String)] = [
        "ADMIN": ("الإدارة", "info"),
        "OWNER": ("المالك", "neutral"),
        "IMPORT": ("استيراد", "neutral")
    ]
}
