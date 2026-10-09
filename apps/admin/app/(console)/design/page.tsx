"use client";

import { iconPaths, illustrationPaths } from "@servacode/design-tokens/icons";
import { vocabulary } from "@servacode/design-tokens/vocabulary";
import { useState } from "react";

import { Icon, type IconName, Illustration, type IllustrationName } from "../../../components/icons";
import {
  ConfirmDialog,
  DataTable,
  EmptyState,
  ErrorState,
  KeyValueList,
  LoadingState,
  PageHeader,
  Panel,
  StatCard,
  TermBadge,
  Toast,
} from "../../../components/ui";
import {
  CharCount,
  Checklist,
  NotificationPreview,
  Segmented,
  SidePanel,
  SlidePreview,
  Trend,
} from "../../../components/ui/extra";

const SWATCHES: readonly (readonly [string, string])[] = [
  ["--sd-semantic-action-primary", "الفعل الأساسي"],
  ["--sd-semantic-surface-canvas", "خلفية الصفحة"],
  ["--sd-semantic-surface-default", "سطح البطاقة"],
  ["--sd-semantic-surface-subtle", "سطح هادئ"],
  ["--sd-semantic-surface-brand-soft", "سطح العلامة"],
  ["--sd-semantic-surface-bar", "الشريط"],
  ["--sd-semantic-content-primary", "نص أساسي"],
  ["--sd-semantic-content-secondary", "نص ثانوي"],
  ["--sd-semantic-stroke-default", "حدود"],
  ["--sd-semantic-feedback-success", "نجاح"],
  ["--sd-semantic-feedback-warning", "تحذير"],
  ["--sd-semantic-feedback-danger", "خطر"],
  ["--sd-semantic-feedback-info", "معلومة"],
];

const TYPE: readonly (readonly [string, string, number, number])[] = [
  ["عنوان الصفحة", "لوحة المتابعة", 24, 700],
  ["عنوان قسم", "آخر الإجراءات", 16, 700],
  ["نص أساسي", "تظهر المنشأة للعامة بعد قبول الطلب.", 14, 400],
  ["نص صغير", "آخر تحديث قبل 3 أيام", 12, 400],
  ["رقم بارز", "1,248", 30, 800],
];

const GROUP_TITLES: Record<string, string> = {
  availability: "حالة الفتح",
  facilityStatus: "حالة المنشأة",
  applicationStatus: "حالة الطلب",
  applicationKind: "نوع الطلب",
  reportReason: "سبب البلاغ",
  reportStatus: "حالة البلاغ",
  accountStatus: "حالة الحساب",
  switch: "مفتاح",
  trust: "الثقة",
  weekday: "أيام الأسبوع",
  contactKind: "نوع رسالة التواصل",
  emergencyKind: "نوع رقم الطوارئ",
  pageKind: "نوع الصفحة",
  dutySource: "مصدر المناوبة",
  evidenceState: "الوثائق المطلوبة",
  contactState: "رسالة التواصل",
  pageState: "حالة الصفحة",
  numberState: "رقم الطوارئ",
};

/**
 * The console's own design reference.
 *
 * Every colour, type size, control, state word, icon and illustration the platform uses,
 * rendered live from the shared design package — so what is shown here is what the screens
 * are built from, in the current theme. It changes nothing and calls no endpoint.
 */
export default function DesignPage() {
  const [dialog, setDialog] = useState(false);
  const [toast, setToast] = useState<string | null>(null);
  const [checked, setChecked] = useState(true);
  const [sheet, setSheet] = useState(false);
  const [audience, setAudience] = useState<"ALL" | "OWNERS">("ALL");
  const [note, setNote] = useState("اتصلت بالرقم وتأكدت منه.");

  return (
    <div className="stack">
      <PageHeader
        eyebrow="مرجع داخلي"
        title="نظام التصميم"
        description="الألوان والخطوط والعناصر والحالات المشتركة بين التطبيق والموقع ولوحة الإدارة. غيّر المظهر من الشريط العلوي لرؤية الوضع الداكن."
      />

      <Panel title="الألوان" description="من مصدر واحد لكل المنصات، وتتبدّل تلقائياً في الوضع الداكن.">
        <div className="swatch-grid">
          {SWATCHES.map(([token, label]) => (
            <div key={token} className="swatch">
              <span className="swatch-chip" style={{ background: `var(${token})` }} />
              <strong>{label}</strong>
              <code>{token.replace("--sd-semantic-", "")}</code>
            </div>
          ))}
        </div>
      </Panel>

      <Panel
        title="الخطوط"
        description="Alexandria للعناوين والأرقام الكبيرة، وIBM Plex Sans Arabic لكل ما يُقرأ."
      >
        <KeyValueList
          items={TYPE.map(([label, sample, size, weight]) => ({
            label: `${label} · ${size}`,
            value: <span style={{ fontSize: size, fontWeight: weight }}>{sample}</span>,
          }))}
        />
      </Panel>

      <div className="grid-2">
        <Panel title="الأزرار">
          <div className="button-row">
            <button type="button" className="button-primary">
              حفظ
            </button>
            <button type="button" className="button-ghost">
              إلغاء
            </button>
            <button type="button" className="button-danger">
              حذف
            </button>
            <button type="button" className="button-ghost" data-tone="danger">
              رفض
            </button>
          </div>
          <div className="button-row">
            <button type="button" className="button-primary" disabled>
              معطّل
            </button>
            <button type="button" className="button-primary" aria-busy="true">
              جارٍ التنفيذ…
            </button>
            <button type="button" className="button-ghost">
              <Icon name="plus" />
              مع أيقونة
            </button>
          </div>
        </Panel>

        <Panel title="الحقول">
          <label className="field">
            <span>اسم المنشأة</span>
            <input defaultValue="صيدلية الشفاء" />
          </label>
          <label className="field">
            <span>رقم الهاتف</span>
            <input dir="ltr" defaultValue="09" aria-invalid="true" />
            <span className="field-error">رقم الهاتف غير مكتمل.</span>
          </label>
          <label className="field">
            <span>التصنيف</span>
            <select defaultValue="p">
              <option value="p">صيدليات</option>
              <option value="c">عيادات</option>
            </select>
          </label>
          <label className="switch-row">
            <span>ظاهر للعامة</span>
            <input type="checkbox" checked={checked} onChange={(e) => setChecked(e.target.checked)} />
          </label>
          <label className="field">
            <span>حقل معطّل</span>
            <input disabled defaultValue="لا يمكن تعديله" />
          </label>
        </Panel>
      </div>

      <Panel title="كلمات الحالات" description="القاموس الموحّد: كل حالة بكلمة واحدة ولون واحد في كل مكان.">
        <KeyValueList
          items={Object.keys(vocabulary).map((group) => ({
            label: GROUP_TITLES[group] ?? group,
            value: (
              <div className="button-row">
                {Object.keys(vocabulary[group as keyof typeof vocabulary]).map((key) => (
                  <TermBadge key={key} group={group as keyof typeof vocabulary} value={key} />
                ))}
              </div>
            ),
          }))}
        />
      </Panel>

      <div className="kpi-grid">
        <StatCard label="طلبات بانتظار المراجعة" value={12} icon="inbox" tone="info" hint="فتح القائمة" href="/reviews" />
        <StatCard label="بلاغات مفتوحة" value={3} icon="flag" tone="warning" />
        <StatCard label="مناوبون الآن" value={7} icon="moon" />
      </div>

      <Panel title="جدول" flush>
        <DataTable
          caption="مثال"
          rows={[
            { id: "1", name: "صيدلية الشفاء", status: "ACTIVE", state: "DUTY" },
            { id: "2", name: "صيدلية النور", status: "SUBMITTED", state: "OPEN" },
            { id: "3", name: "صيدلية الأمل", status: "SUSPENDED", state: "CLOSED" },
          ]}
          rowKey={(row) => row.id}
          columns={[
            { key: "name", header: "المنشأة", render: (row) => <strong>{row.name}</strong> },
            { key: "status", header: "الحالة", render: (row) => <TermBadge group="facilityStatus" value={row.status} /> },
            { key: "state", header: "الآن", render: (row) => <TermBadge group="availability" value={row.state} /> },
          ]}
        />
      </Panel>

      <div className="grid-2">
        <Panel title="الحالات">
          <LoadingState />
          <ErrorState
            error={{ code: "NETWORK_UNAVAILABLE", message: "", details: {}, requestId: "9f1c2a0e" }}
            onRetry={() => undefined}
          />
          <p className="notice">ملاحظة إرشادية تشرح أثر الإجراء قبل تنفيذه.</p>
        </Panel>
        <Panel title="صفحة فارغة">
          <EmptyState
            title="لا بلاغات"
            hint="لا يوجد ما ينتظر المعالجة."
            action={
              <button type="button" className="button-ghost">
                تحديث
              </button>
            }
          />
        </Panel>
      </div>

      <Panel title="النوافذ والتنبيهات">
        <div className="button-row">
          <button type="button" className="button-primary" onClick={() => setDialog(true)}>
            فتح نافذة تأكيد
          </button>
          <button type="button" className="button-ghost" onClick={() => setToast("تم حفظ التغييرات.")}>
            إظهار تنبيه
          </button>
        </div>
      </Panel>

      <Panel
        title="عناصر التشغيل"
        description="مقارنة بالفترة السابقة، وعدّاد الأحرف، والاختيار المقسّم، وقائمة الشروط، واللوحة الجانبية، والمعاينات."
      >
        <div className="kpi-grid">
          <StatCard label="عمليات البحث" value="12,840" icon="search" trend={<Trend current={12840} previous={10450} />} />
          <StatCard
            label="بحث بلا نتائج"
            value="912"
            icon="inbox"
            tone="warning"
            trend={<Trend current={912} previous={780} lowerIsBetter />}
          />
          <StatCard label="طلبات الاتجاهات" value="4,120" icon="directions" trend={<Trend current={4120} previous={0} />} />
        </div>
        <div className="grid-2">
          <div className="stack">
            <Segmented
              label="إلى من"
              name="design-audience"
              value={audience}
              options={[
                { value: "ALL", label: "الجميع", hint: "كل الحسابات الفعّالة" },
                { value: "OWNERS", label: "أصحاب المنشآت", hint: "المالكون والمديرون" },
              ]}
              onChange={setAudience}
            />
            <label className="field">
              <span className="field-label-row">
                ملاحظة
                <CharCount value={note} max={40} />
              </span>
              <input value={note} onChange={(event) => setNote(event.target.value)} />
            </label>
            <Checklist
              items={[
                { key: "ok", ok: true, title: "المحافظة مفعّلة", detail: "ظاهرة للعامة." },
                {
                  key: "missing",
                  ok: false,
                  title: "صيدلية مناوبة كل يوم",
                  detail: "يومان بلا مناوبة خلال الأسبوعين القادمين.",
                },
              ]}
            />
            <div>
              <button type="button" className="button-ghost" onClick={() => setSheet(true)}>
                فتح لوحة جانبية
              </button>
            </div>
          </div>
          <div className="stack">
            <SlidePreview src={null} title="حملة التلقيح الوطنية" emptyLabel="اختر صورة لتظهر هنا" />
            <NotificationPreview title="صيدليات مناوبة جديدة" body="أضفنا صيدليات مناوبة في حيّك." />
          </div>
        </div>
      </Panel>

      <Panel title="الرسومات" description="للحالات الفارغة وانقطاع الاتصال والأخطاء والصيانة.">
        <div className="icon-grid illustration-grid">
          {(Object.keys(illustrationPaths) as IllustrationName[]).map((name) => (
            <figure key={name}>
              <Illustration name={name} size={88} />
              <figcaption>
                <code>{name}</code>
              </figcaption>
            </figure>
          ))}
        </div>
      </Panel>

      <Panel title="الأيقونات" description={`${Object.keys(iconPaths).length} أيقونة مشتركة. الأيقونات الاتجاهية تنعكس تلقائياً.`}>
        <div className="icon-grid">
          {(Object.keys(iconPaths) as IconName[]).map((name) => (
            <figure key={name}>
              <Icon name={name} width={24} height={24} />
              <figcaption>
                <code>{name}</code>
              </figcaption>
            </figure>
          ))}
        </div>
      </Panel>

      <ConfirmDialog
        open={dialog}
        title="إيقاف المنشأة"
        body="ستختفي المنشأة من الواجهات العامة حتى إعادة تفعيلها."
        confirmLabel="تأكيد الإيقاف"
        destructive
        onConfirm={() => setDialog(false)}
        onCancel={() => setDialog(false)}
      />
      <SidePanel open={sheet} title="لوحة جانبية" description="للعمل على عنصر واحد دون مغادرة الصفحة." onClose={() => setSheet(false)}>
        <p className="notice">تُغلق بزر الإغلاق أو بمفتاح Escape أو بالنقر خارجها.</p>
      </SidePanel>
      <Toast message={toast} onDismiss={() => setToast(null)} />
    </div>
  );
}
