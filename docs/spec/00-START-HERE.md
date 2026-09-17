# Serva Code Directory Platform V3 — START HERE

هذه الحزمة هي مرجع مستقل وكامل لبناء المشروع من الصفر حتى Staging وProduction والنشر على Google Play ثم iOS لاحقًا.

## قاعدة التنفيذ

أي مطور أو وكيل برمجي مثل Claude يجب أن:

1. يقرأ **كل الملفات في هذه الحزمة** قبل كتابة الكود.
2. يعتبر `01-MASTER-SPECIFICATION.md` المرجع الأعلى.
3. يقرأ `02-BASELINE-DECISIONS.md` حتى لا يسأل عن قرارات حُسمت.
4. ينشئ المشروع من الصفر وفق `26-REPOSITORY-STRUCTURE.md`.
5. ينفذ بالتسلسل وفق `24-IMPLEMENTATION-ROADMAP.md`.
6. بعد نجاح Gate ينتقل تلقائيًا إلى المرحلة التالية دون انتظار "التالي".
7. لا يستخدم React Native أو Flutter.
8. لا يختلق بيانات أو APIs أو صلاحيات غير موجودة في المواصفات.
9. لا يطبع أو يسجل الأسرار.
10. لا يعلن نجاح مرحلة بلا دليل تشغيل واختبارات.

## الهدف التقني النهائي

```text
Backend              Python + Django + DRF + GeoDjango
Database             PostgreSQL + PostGIS
Cache/Realtime       Redis + Django Channels
Async jobs           Celery
Admin Web            Next.js + React + TypeScript
Public Web           Next.js (legal/support/account deletion/landing)
Android              Kotlin + Jetpack Compose
iOS                  Swift + SwiftUI
Maps                 MapLibre Native
Routing              RoutingProvider → OSRM initially
Geocoding            GeocodingProvider
API Contracts        OpenAPI generated clients
Object storage       S3-compatible
DNS/CDN              Cloudflare recommended baseline
Staging/Production   Managed paid hosting, Render baseline
Monitoring           Sentry + structured server logs
```

## القرارات التي لا يجب إعادة فتحها أثناء التنفيذ

- المنتج category-driven وليس Pharmacy hardcoded.
- الحساب generic ويمكنه امتلاك عدة منشآت.
- public browsing لا يحتاج تسجيل دخول.
- rating/owner actions تحتاج login.
- PostGIS مصدر المسافة والـgeo.
- REST مصدر الحقيقة.
- WebSocket للأحداث/invalidation.
- Verification Evidence خاص ولا يظهر للعامة.
- Django Admin ليس لوحة التشغيل.
- Android Native Kotlin.
- iOS Native Swift.
- Design System موحد.
- العربية RTL هي اللغة الأساسية.
- Raqqa + Pharmacy + Duty هو launch baseline الأول.
- لا fake production data.
- لا demo map services في Production.

## عناصر خارجية لا يمكن اختراعها

الوثائق لا تطلب قرارات منتج إضافية، لكن بعض الأمور تحتاج ملكية/credentials حقيقية عند لحظة الربط:

- اسم النطاق الذي تم شراؤه.
- صلاحيات DNS/Cloudflare.
- Play Console account access.
- Firebase/APNs credentials.
- Production OTP provider credentials.
- Object storage keys.
- Sentry/monitoring credentials.
- Production secrets.
- Android upload keystore secret.

يجب على الوكيل تنفيذ كل ما يمكن محليًا وStaging تلقائيًا، ووضع هذه القيم خلف environment variables. عدم وجود credential خارجي **ليس سببًا لإيقاف بناء المشروع أو سؤال المستخدم عن المعمارية**.

## ترتيب القراءة المقترح

```text
01 Master Specification
02 Baseline Decisions
03 Product Specification
04 Operating Cycles
05 System Architecture
06 Data Model
07 Backend
08 API Contract
09 Admin
10 Design System
11 Android
12 iOS
13 Maps/Geo
14 Realtime/Offline/Push
15 Security/Privacy
16 Ads/Analytics/Audit
17 Testing/QA
18 Infrastructure
19 Domain/DNS/TLS
20 Google Play Release
21 App Store Release
22 Operations
23 Autonomous Claude Execution
24 Roadmap/Gates
25 Environment Contract
26 Repository Structure
27 Legacy/Migration
28 External Prerequisites
```

## حالة الوثائق

هذه الحزمة صممت لتكون **Implementation Baseline**.  
أي تغيير جوهري بعد اعتمادها يمر عبر ADR/Change Request بدل تعديلات صامتة.
