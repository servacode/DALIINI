# Serva Code Directory Platform V3 — Master Specification

**Document ID:** SCDP-V3-MASTER  
**Status:** IMPLEMENTATION BASELINE DRAFT  
**Owner:** Serva Code  
**Primary language:** Arabic RTL  
**Architecture:** Native mobile + Django platform  
**Launch:** Raqqa first, Syria-wide architecture

---

## 1. الرؤية

إنشاء منصة دليل سورية حديثة وموثوقة لاكتشاف المنشآت والخدمات حسب المحافظة والتصنيف والموقع، مع خرائط Native، ترتيب الأقرب، حالات الفتح والمناوبة، وإدارة احترافية لانضمام أصحاب المنشآت ومراجعتها.

المنصة ليست تطبيق صيدليات فقط. الصيدلية هي أول تصنيف تشغيلي. البنية نفسها يجب أن تستوعب لاحقًا العيادات، التمريض، المختبرات، المستلزمات الطبية، ثم تصنيفات خدمية وتجارية بدون إعادة كتابة التطبيق عند كل تصنيف عادي.

---

## 2. المنتجات التي تتكون منها المنصة

```text
1. Backend Platform
2. Custom Admin Web
3. Public/Owner Android App
4. Public/Owner iOS App
5. Minimal Public Web
```

### Backend Platform
مصدر الحقيقة للقواعد والبيانات والصلاحيات والـgeo والـsearch والـrealtime.

### Admin
لوحة تشغيل للموظفين، وليست مجرد CRUD.

### Android
Kotlin + Compose Native.

### iOS
Swift + SwiftUI Native.

### Public Web
صفحات عامة مطلوبة للتشغيل والنشر:
- landing.
- privacy.
- terms.
- support.
- account deletion request.

---

## 3. إطلاق V3 الأول

- المحافظة الفعالة: الرقة.
- التصنيف العام الفعال: الصيدليات.
- owner onboarding للصيدليات: فعال.
- pharmacy duty: فعال.
- كل المحافظات السورية موجودة بالقاعدة.
- المحافظات الأخرى: غير مفعلة.
- التصنيفات الأخرى: قابلة للإنشاء أو seed ولكن لا تظهر حتى تفعيلها.
- لا حاجة لحساب للتصفح العام.
- الحساب مطلوب للتقييم وإدارة المنشآت.

---

## 4. المستخدمون

### Anonymous
يستطيع:
- الرئيسية.
- التصنيفات.
- البحث.
- الخريطة.
- التفاصيل.
- الاتصال.
- الاتجاهات.
- المناوبات.

### Registered User
يضيف:
- الملف الشخصي.
- التقييم.
- الجلسات.
- بدء طلب إضافة منشأة.

### Facility Owner/Manager
يضيف:
- إنشاء/متابعة draft.
- إدارة منشآته.
- الصور.
- ساعات العمل.
- المناوبات.
- إعادة التحقق.
- المديرين ضمن السياسة.

### Admin Operator
حسب Permission Matrix.

---

## 5. تصنيف المنتج

الـtaxonomy ديناميكية:

```text
DirectoryCategoryGroup
DirectoryCategory
DirectoryCategoryProvince
CategoryCapabilities
VerificationRequirement
```

لا يُستخدم اسم التصنيف في if/else داخل Android/Admin لتمييز السلوك العادي.

### Technical specialization
يُستخدم فقط عند وجود منطق متخصص:

```text
GENERIC
PHARMACY
MEDICAL_CLINIC
NURSING_CENTER
```

---

## 6. التفعيل حسب المحافظة

Public visibility:

```text
province active
AND group active
AND category active
AND public_enabled for province
```

Owner onboarding:

```text
province active
AND group active
AND category active
AND owner_registration_enabled for province
```

المفتاحان مستقلان.

---

## 7. Capability-driven UX

أمثلة:

```text
Pharmacy:
  hours=true
  duty=true
  specialty=false
  service=false

Medical Clinic:
  hours=true
  duty=false
  specialty=true

Nursing Center:
  hours=true
  duty=false
  service=true

Generic:
  hours=true
  photos=true
  ratings=true
  duty=false
  specialty=false
  service=false
```

كل كلاينت يعرض الفلاتر والحقول بناءً على capabilities من API.

---

## 8. نطاق Core

Core يشمل:

- accounts.
- OTP/recovery abstraction.
- sessions.
- provinces/cities/neighborhoods.
- categories/groups/capabilities.
- facilities.
- ownership.
- onboarding.
- verification evidence.
- public images.
- business hours.
- temporary closures.
- pharmacy duty.
- public discovery.
- search.
- geo.
- ratings.
- admin.
- ads.
- realtime invalidation.
- push.
- offline caching.
- analytics.
- audit.
- backup/restore.
- monitoring.
- Android.
- iOS.

---

## 9. خارج Core

لا يدخل دون Change Request:

- e-commerce.
- cart.
- payments.
- delivery.
- medical records.
- consultation.
- prescriptions.
- appointment booking.
- insurance.
- chat.
- social feed.
- text reviews.
- background tracking.

---

## 10. مبادئ المعمارية

- Modular Monolith وليس Microservices.
- Django هو API/domain platform.
- PostgreSQL/PostGIS هو data/geo truth.
- Redis مساعد وليس source of truth.
- WebSocket event/invalidation layer.
- Celery للـasync غير المعاملاتي.
- OpenAPI contract.
- generated clients.
- native mobile clients.
- custom Admin.
- platform-neutral design tokens.
- provider abstractions للخدمات الخارجية.
- no demo endpoints in Production.
- no fake data.

---

## 11. دورة جودة Serva Code

```text
Requirement
→ UX/Architecture
→ Plan
→ Implement
→ Unit tests
→ Integration
→ Connected qualification
→ Device/Browser QA
→ Staging
→ Evidence
→ Close
```

---

## 12. Definition of Done العامة

الميزة لا تنتهي قبل وجود:

- spec.
- implementation.
- loading state.
- empty state.
- error state.
- offline/permission state عند الحاجة.
- security review حسب المخاطر.
- tests.
- accessibility.
- analytics إن كانت مطلوبة.
- documentation.
- evidence.
- no P0/P1.

---

## 13. الهدف النهائي

الإطلاق الإنتاجي يجب أن يملك:

```text
api.<ROOT_DOMAIN>
admin.<ROOT_DOMAIN>
www.<ROOT_DOMAIN>
```

مع:
- TLS.
- paid production compute.
- managed PostgreSQL/PostGIS.
- Redis.
- Celery worker.
- private object storage.
- monitoring.
- backups.
- restore proof.
- signed Android AAB.
- Play policy completion.
- privacy/account deletion URLs.
- staged production rollout.
