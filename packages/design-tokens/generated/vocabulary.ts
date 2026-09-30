// GENERATED — DO NOT EDIT (source: vocabulary.json)
export type Tone = "neutral" | "positive" | "warning" | "danger" | "info" | "brand";
export type Term = Readonly<{ ar: string; tone: Tone }>;
export const vocabulary = {
  "availability": {
    "OPEN": {
      "ar": "مفتوح الآن",
      "tone": "positive"
    },
    "CLOSED": {
      "ar": "مغلق الآن",
      "tone": "danger"
    },
    "DUTY": {
      "ar": "مناوب الآن",
      "tone": "warning"
    },
    "TEMP_CLOSED": {
      "ar": "مغلق مؤقتاً",
      "tone": "info"
    }
  },
  "facilityStatus": {
    "DRAFT": {
      "ar": "مسودة",
      "tone": "neutral"
    },
    "SUBMITTED": {
      "ar": "قيد المراجعة",
      "tone": "info"
    },
    "ACTIVE": {
      "ar": "فعّالة",
      "tone": "positive"
    },
    "SUSPENDED": {
      "ar": "موقوفة",
      "tone": "warning"
    },
    "CLOSED": {
      "ar": "مغلقة نهائياً",
      "tone": "danger"
    },
    "REVERIFICATION_REQUIRED": {
      "ar": "تحتاج إعادة تحقق",
      "tone": "warning"
    }
  },
  "applicationStatus": {
    "DRAFT": {
      "ar": "مسودة",
      "tone": "neutral"
    },
    "SUBMITTED": {
      "ar": "قيد المراجعة",
      "tone": "info"
    },
    "APPROVED": {
      "ar": "مقبول",
      "tone": "positive"
    },
    "REJECTED": {
      "ar": "مرفوض",
      "tone": "danger"
    }
  },
  "applicationKind": {
    "INITIAL": {
      "ar": "تسجيل جديد",
      "tone": "info"
    },
    "REVERIFICATION": {
      "ar": "إعادة تحقق",
      "tone": "warning"
    }
  },
  "reportReason": {
    "WRONG_INFO": {
      "ar": "معلومات خاطئة",
      "tone": "warning"
    },
    "CLOSED_PERMANENTLY": {
      "ar": "مغلقة نهائياً",
      "tone": "danger"
    },
    "WRONG_LOCATION": {
      "ar": "الموقع خاطئ",
      "tone": "warning"
    },
    "WRONG_HOURS": {
      "ar": "أوقات الدوام خاطئة",
      "tone": "warning"
    },
    "NOT_ON_DUTY": {
      "ar": "ليست مناوبة",
      "tone": "danger"
    },
    "OTHER": {
      "ar": "أخرى",
      "tone": "neutral"
    }
  },
  "reportStatus": {
    "OPEN": {
      "ar": "مفتوح",
      "tone": "warning"
    },
    "RESOLVED": {
      "ar": "تمت المعالجة",
      "tone": "positive"
    },
    "DISMISSED": {
      "ar": "مرفوض",
      "tone": "neutral"
    }
  },
  "accountStatus": {
    "ACTIVE": {
      "ar": "فعّال",
      "tone": "positive"
    },
    "BLOCKED": {
      "ar": "محظور",
      "tone": "danger"
    }
  },
  "switch": {
    "ON": {
      "ar": "مفعّل",
      "tone": "positive"
    },
    "OFF": {
      "ar": "معطّل",
      "tone": "neutral"
    }
  },
  "trust": {
    "VERIFIED": {
      "ar": "موثّقة",
      "tone": "brand"
    }
  },
  "weekday": {
    "SATURDAY": {
      "ar": "السبت",
      "tone": "neutral"
    },
    "SUNDAY": {
      "ar": "الأحد",
      "tone": "neutral"
    },
    "MONDAY": {
      "ar": "الاثنين",
      "tone": "neutral"
    },
    "TUESDAY": {
      "ar": "الثلاثاء",
      "tone": "neutral"
    },
    "WEDNESDAY": {
      "ar": "الأربعاء",
      "tone": "neutral"
    },
    "THURSDAY": {
      "ar": "الخميس",
      "tone": "neutral"
    },
    "FRIDAY": {
      "ar": "الجمعة",
      "tone": "neutral"
    }
  },
  "contactKind": {
    "GENERAL": {
      "ar": "استفسار أو اقتراح",
      "tone": "neutral"
    },
    "OWNER": {
      "ar": "صاحب منشأة",
      "tone": "info"
    },
    "CORRECTION": {
      "ar": "تصحيح معلومة",
      "tone": "warning"
    }
  },
  "emergencyKind": {
    "AMBULANCE": {
      "ar": "إسعاف",
      "tone": "neutral"
    },
    "FIRE": {
      "ar": "إطفاء",
      "tone": "neutral"
    },
    "POLICE": {
      "ar": "شرطة",
      "tone": "neutral"
    },
    "HOSPITAL": {
      "ar": "مستشفى",
      "tone": "neutral"
    },
    "OTHER": {
      "ar": "أخرى",
      "tone": "neutral"
    }
  },
  "pageKind": {
    "PAGE": {
      "ar": "صفحة عامة",
      "tone": "neutral"
    },
    "LEGAL": {
      "ar": "قانونية",
      "tone": "info"
    },
    "FAQ": {
      "ar": "أسئلة شائعة",
      "tone": "neutral"
    }
  },
  "evidenceState": {
    "COMPLETE": {
      "ar": "الوثائق مكتملة",
      "tone": "positive"
    },
    "INCOMPLETE": {
      "ar": "وثائق ناقصة",
      "tone": "warning"
    }
  },
  "contactState": {
    "PENDING": {
      "ar": "بانتظار المعالجة",
      "tone": "warning"
    },
    "HANDLED": {
      "ar": "تمت المعالجة",
      "tone": "positive"
    }
  },
  "pageState": {
    "PUBLISHED": {
      "ar": "منشورة",
      "tone": "positive"
    },
    "UNPUBLISHED": {
      "ar": "غير منشورة",
      "tone": "neutral"
    },
    "CHANGES": {
      "ar": "تعديلات لم تُنشر",
      "tone": "warning"
    },
    "UNSAVED": {
      "ar": "تغييرات لم تُحفظ",
      "tone": "info"
    }
  },
  "numberState": {
    "PUBLIC": {
      "ar": "ظاهر للعامة",
      "tone": "positive"
    },
    "HIDDEN": {
      "ar": "مخفي",
      "tone": "neutral"
    },
    "NEEDS_CHECK": {
      "ar": "يحتاج تحققاً",
      "tone": "warning"
    }
  },
  "dutySource": {
    "ADMIN": {
      "ar": "الإدارة",
      "tone": "info"
    },
    "OWNER": {
      "ar": "المالك",
      "tone": "neutral"
    },
    "IMPORT": {
      "ar": "استيراد",
      "tone": "neutral"
    }
  }
} as const satisfies Record<string, Record<string, Term>>;
export type VocabularyGroup = keyof typeof vocabulary;
/** The label and tone for a state, falling back to the raw value so an unknown state still shows. */
export function term(group: VocabularyGroup, key: string | null | undefined): Term {
  const entries = vocabulary[group] as Record<string, Term>;
  return (key && entries[key]) || { ar: key ?? "—", tone: "neutral" };
}
