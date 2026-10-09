"use client";

import Link from "next/link";
import { useState } from "react";

import { useCan } from "../../../components/admin-shell";
import { Icon } from "../../../components/icons";
import {
  ConfirmDialog,
  EmptyState,
  ErrorState,
  LoadingState,
  PageHeader,
  Toast,
} from "../../../components/ui";
import {
  type ChecklistItem,
  Checklist,
  FilterChips,
  FormDialog,
  ItemCard,
  SearchBox,
} from "../../../components/ui/extra";
import { useMutation } from "../../../lib/client/use-mutation";
import { useResource } from "../../../lib/client/use-resource";
import { LOCALE } from "../../../lib/locale";

type Province = Readonly<{
  id: string;
  code: string;
  nameAr: string;
  nameEn: string;
  active: boolean;
  sortOrder: number;
  activeFacilityCount: number;
  cityCount: number;
  activeCityCount: number;
}>;

const SHOW = [
  { value: "", label: "الكل" },
  { value: "on", label: "المفتوحة" },
  { value: "off", label: "غير المفعّلة" },
] as const;
type Show = (typeof SHOW)[number]["value"];

/**
 * Province rollout.
 *
 * All fourteen are present from the launch seed; activation is the lever. Turning one on
 * changes what the public province list serves immediately, which is why it is confirmed
 * rather than toggled inline — and why re-running the seed will not undo it.
 */
export default function ProvincesPage() {
  const provinces = useResource<{ items: Province[] }>("provinces");
  const mutation = useMutation();
  const canManage = useCan("admin.provinces.manage");

  const [pending, setPending] = useState<Province | null>(null);
  const [citiesOf, setCitiesOf] = useState<Province | null>(null);
  const [readinessOf, setReadinessOf] = useState<Province | null>(null);
  const [toast, setToast] = useState<string | null>(null);
  const [q, setQ] = useState("");
  const [show, setShow] = useState<Show>("");

  async function submit(): Promise<void> {
    if (!pending) return;
    const ok = await mutation.run("provinceUpdate", {
      id: pending.id,
      active: !pending.active,
    });
    if (!ok) return;
    setToast(pending.active ? "تم تعطيل المحافظة." : "تم تفعيل المحافظة.");
    setPending(null);
    provinces.reload();
  }

  const all = provinces.data?.items ?? [];
  const needle = q.trim();
  const shown = all.filter((province) => {
    if (needle && !province.nameAr.includes(needle) && !province.code.includes(needle)) {
      return false;
    }
    return show === "on" ? province.active : show === "off" ? !province.active : true;
  });
  const activeCount = all.filter((province) => province.active).length;

  return (
    <div className="stack">
      <PageHeader
        eyebrow="الدليل"
        title="المحافظات"
        description={
          provinces.data
            ? `التوسّع الجغرافي للمنصة: ${NUMBER.format(activeCount)} من ${NUMBER.format(all.length)} محافظة مفتوحة للعامة.`
            : "التحكم في التوسّع الجغرافي للمنصة."
        }
      />

      <div className="live-toolbar">
        <SearchBox value={q} onChange={setQ} placeholder="ابحث باسم محافظة" delay={0} />
        <FilterChips label="عرض" value={show} options={SHOW} onChange={setShow} />
      </div>

      {provinces.loading ? <LoadingState /> : null}
      {provinces.error ? (
        <ErrorState error={provinces.error} onRetry={provinces.reload} />
      ) : null}

      {provinces.data && shown.length === 0 ? (
        <EmptyState title="لا محافظة تطابق هذا العرض." illustration="noResults" />
      ) : null}

      {shown.length > 0 ? (
        <ul className="profile-grid" data-testid="provinces">
          {shown.map((province, index) => (
            <ItemCard
              key={province.id}
              index={index}
              icon="mapPin"
              glyph={province.nameAr.replace(/^ال/, "").slice(0, 1)}
              tone={province.active ? "brand" : "neutral"}
              muted={!province.active}
              title={province.nameAr}
              subtitle={province.nameEn || province.code}
              state={
                province.active
                  ? { label: "مفتوحة للعامة", tone: "positive" }
                  : { label: "غير مفعّلة" }
              }
              facts={[
                { label: "منشآت فعّالة", value: NUMBER.format(province.activeFacilityCount) },
                {
                  label: "المدن المفعّلة",
                  value: `${NUMBER.format(province.activeCityCount)} من ${NUMBER.format(province.cityCount)}`,
                },
                { label: "الترتيب", value: NUMBER.format(province.sortOrder) },
              ]}
              testId={`province-${province.code}`}
              actions={
                <>
                  <button
                    type="button"
                    className="profile-act-main"
                    data-testid={`readiness-${province.code}`}
                    onClick={() => setReadinessOf(province)}
                  >
                    <Icon name="checkCircle" width={16} height={16} />
                    الجاهزية
                  </button>
                  <button
                    type="button"
                    className="profile-act-quiet"
                    data-testid={`cities-${province.code}`}
                    onClick={() => setCitiesOf(province)}
                  >
                    المدن
                  </button>
                  {canManage ? (
                    <button
                      type="button"
                      className={province.active ? "profile-act-danger" : "profile-act-quiet"}
                      data-testid={`toggle-province-${province.code}`}
                      onClick={() => {
                        mutation.reset();
                        setPending(province);
                      }}
                    >
                      {province.active ? "تعطيل" : "تفعيل"}
                    </button>
                  ) : null}
                </>
              }
            />
          ))}
        </ul>
      ) : null}

      <FormDialog
        open={citiesOf !== null}
        icon="mapPin"
        title={citiesOf ? `مدن ${citiesOf.nameAr}` : ""}
        description="مدينة معطّلة لا تظهر للعامة حتى في محافظة مفتوحة."
        onClose={() => {
          setCitiesOf(null);
          provinces.reload();
        }}
        testId="cities-panel"
        footer={
          <button
            type="button"
            className="button-ghost"
            onClick={() => {
              setCitiesOf(null);
              provinces.reload();
            }}
          >
            إغلاق
          </button>
        }
      >
        {citiesOf ? <CitiesList key={citiesOf.id} province={citiesOf} canManage={canManage} /> : null}
      </FormDialog>

      <FormDialog
        open={readinessOf !== null}
        wide
        icon="checkCircle"
        title={readinessOf ? `جاهزية ${readinessOf.nameAr} للإطلاق` : ""}
        description="ما يلزم قبل أن تُفتح المحافظة للعامة، مفحوصاً الآن."
        onClose={() => setReadinessOf(null)}
        testId="readiness-panel"
        footer={
          <button type="button" className="button-ghost" onClick={() => setReadinessOf(null)}>
            إغلاق
          </button>
        }
      >
        {readinessOf ? <ReadinessChecklist key={readinessOf.id} province={readinessOf} /> : null}
      </FormDialog>

      <ConfirmDialog
        open={pending !== null}
        title={pending?.active ? "تعطيل المحافظة" : "تفعيل المحافظة"}
        body={
          pending?.active
            ? "ستختفي المحافظة وكل ما فيها من الواجهات العامة."
            : "ستظهر المحافظة للعامة فوراً، وستُتاح تصنيفاتها المفعّلة فيها."
        }
        confirmLabel="تأكيد"
        destructive={pending?.active}
        pending={mutation.pending}
        error={mutation.error}
        onConfirm={submit}
        onCancel={() => setPending(null)}
      />

      <Toast message={toast} onDismiss={() => setToast(null)} />
    </div>
  );
}

type City = Readonly<{ id: string; code: string; nameAr: string; nameEn: string; active: boolean }>;

/**
 * The cities inside one province, with their own activation switch.
 *
 * A city switch narrows what the public sees inside an active province; it cannot open a
 * city in a province that is itself off. Each toggle is audited by the backend.
 */
function CitiesList({ province, canManage }: { province: Province; canManage: boolean }) {
  const cities = useResource<{ items: City[] }>("provinceCities", { id: province.id });
  const mutation = useMutation();

  async function toggle(city: City): Promise<void> {
    const ok = await mutation.run("cityUpdate", {
      provinceId: province.id,
      id: city.id,
      active: !city.active,
    });
    if (ok) cities.reload();
  }

  return (
    <div className="stack">
      {cities.loading ? <LoadingState /> : null}
      {cities.error ? <ErrorState error={cities.error} onRetry={cities.reload} /> : null}
      {mutation.error ? <ErrorState error={mutation.error} /> : null}
      {cities.data && cities.data.items.length === 0 ? (
        <EmptyState title="لا مدن مسجّلة لهذه المحافظة" />
      ) : null}
      {cities.data && cities.data.items.length > 0 ? (
        <ul className="plain-list city-list">
          {cities.data.items.map((city) => (
            <li key={city.id} className="plain-row" data-off={!city.active || undefined}>
              <span className="city-name">
                <Icon name="mapPin" width={16} height={16} />
                <strong>{city.nameAr}</strong>
                <span className="muted">{city.active ? "مفعّلة" : "معطّلة"}</span>
              </span>
              {canManage ? (
                <button
                  type="button"
                  className={city.active ? "button-ghost" : "button-primary"}
                  disabled={mutation.pending}
                  data-testid={`toggle-city-${city.code}`}
                  onClick={() => toggle(city)}
                >
                  {city.active ? "تعطيل" : "تفعيل"}
                </button>
              ) : null}
            </li>
          ))}
        </ul>
      ) : null}
    </div>
  );
}

type Readiness = Readonly<{
  provinceId: string;
  provinceNameAr: string;
  ready: boolean;
  minActiveFacilities: number;
  items: readonly { code: string; ok: boolean; detailAr: string }[];
}>;

const READINESS: Record<string, { title: string; fix?: (province: Province) => { href: string; label: string } }> = {
  PROVINCE_ACTIVE: { title: "المحافظة مفعّلة" },
  CATEGORY_PUBLIC: {
    title: "تصنيف واحد على الأقل ظاهر للعامة",
    fix: () => ({ href: "/taxonomy/categories", label: "فتح التصنيفات" }),
  },
  MIN_ACTIVE_FACILITIES: {
    title: "عدد كافٍ من المنشآت الفعّالة",
    fix: () => ({ href: "/reviews", label: "فتح طلبات المراجعة" }),
  },
  DUTY_COVERAGE: {
    title: "صيدلية مناوبة في كل يوم من الأسبوعين القادمين",
    fix: (province) => ({
      href: `/duty?province=${encodeURIComponent(province.id)}`,
      label: "فتح جدول المناوبات",
    }),
  },
  EMERGENCY_NUMBERS: {
    title: "رقم طوارئ واحد على الأقل",
    fix: () => ({ href: "/content/emergency", label: "فتح أرقام الطوارئ" }),
  },
};

const NUMBER = new Intl.NumberFormat(LOCALE);

/**
 * The launch checklist for one province, computed by the backend on each open: whether it
 * is active, has a public category, enough active facilities, a pharmacy on duty every day
 * of the next two weeks, and an emergency number. Each missing item says why and links to
 * the screen where it is fixed.
 */
function ReadinessChecklist({ province }: { province: Province }) {
  const readiness = useResource<Readiness>("provinceReadiness", { id: province.id });
  const data = readiness.data;
  const missing = data ? data.items.filter((item) => !item.ok).length : 0;
  const items: ChecklistItem[] = (data?.items ?? []).map((item) => {
    const meta = READINESS[item.code];
    const fix = !item.ok ? meta?.fix?.(province) : undefined;
    return {
      key: item.code,
      ok: item.ok,
      title: meta?.title ?? item.code,
      detail: item.detailAr,
      action: fix ? (
        <Link href={fix.href} className="button-ghost">
          {fix.label}
        </Link>
      ) : undefined,
    };
  });

  return (
    <div className="stack readiness">
      {readiness.loading ? <LoadingState label="جارٍ الفحص…" /> : null}
      {readiness.error ? <ErrorState error={readiness.error} onRetry={readiness.reload} /> : null}
      {data ? (
        <>
          <div
            className="readiness-verdict"
            data-ready={data.ready}
            role="status"
            data-testid="readiness-verdict"
          >
            <strong>{data.ready ? "جاهزة للإطلاق" : "غير جاهزة بعد"}</strong>
            <span>
              {data.ready
                ? "كل الشروط متحققة. يمكن فتح المحافظة للعامة."
                : `ينقصها ${NUMBER.format(missing)} من ${NUMBER.format(data.items.length)} شروط.`}
            </span>
          </div>
          <Checklist items={items} />
          <p className="field-hint">
            {`الحد الأدنى للمنشآت الفعّالة ${NUMBER.format(data.minActiveFacilities)}، ويُضبط من الإعدادات.`}
          </p>
          <div>
            <button type="button" className="button-ghost" onClick={readiness.reload}>
              إعادة الفحص
            </button>
          </div>
        </>
      ) : null}
    </div>
  );
}
