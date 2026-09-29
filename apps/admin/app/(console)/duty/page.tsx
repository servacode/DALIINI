"use client";

import Link from "next/link";
import { use, useCallback, useEffect, useState } from "react";

import { useCan } from "../../../components/admin-shell";
import { Icons } from "../../../components/icons";
import {
  ConfirmDialog,
  EmptyState,
  ErrorState,
  LoadingState,
  PageHeader,
  StatusBadge,
  TermBadge,
  Toast,
  type Tone,
  labelsFor,
  termsFor,
} from "../../../components/ui";
import { SidePanel } from "../../../components/ui/extra";
import {
  WEEK_ORDER,
  addDays,
  damascusClock,
  damascusDay,
  damascusInstant,
  endsNextDay,
  formatClock,
  formatDayLabel,
  formatDayMonth,
  formatDayNumber,
  formatMonth,
  shiftSpanOn,
  weekStart,
  weekdayOf,
} from "../../../lib/client/calendar";
import { useMutation } from "../../../lib/client/use-mutation";
import { useResource } from "../../../lib/client/use-resource";
import type { ApiErrorBody } from "../../../lib/errors/messages";

type Province = Readonly<{ id: string; nameAr: string; active: boolean; sortOrder: number }>;
type City = Readonly<{ id: string; nameAr: string; active: boolean }>;
type Category = Readonly<{ id: string; nameAr: string; specialization: string; active: boolean }>;
type Pharmacy = Readonly<{ id: string; nameAr: string; cityId: string | null }>;

type Shift = Readonly<{
  id: string;
  facilityId: string;
  facilityNameAr: string;
  cityId: string | null;
  startsAt: string;
  endsAt: string;
  createdBy: string;
  status: string;
}>;
type Day = Readonly<{ date: string; gap: boolean; shifts: readonly Shift[] }>;
type Roster = Readonly<{ provinceId: string; cityId: string | null; days: readonly Day[] }>;

/** Two full Syrian weeks, Saturday to Friday. */
const WINDOW_DAYS = 14;

const WEEKDAYS = labelsFor("weekday");
const NUMBER = new Intl.NumberFormat("ar-SY");

const SOURCE = termsFor("dutySource");

const STATUS: Record<string, { label: string; tone: Tone }> = {
  UPCOMING: { label: "قادمة", tone: "info" },
  ENDED: { label: "انتهت", tone: "neutral" },
};

/** Ready-made times, the two an operator types most. The end rolls to the next morning. */
const PRESETS = [
  { key: "night", label: "ليلية ٢٢:٠٠–٠٨:٠٠", start: "22:00", end: "08:00" },
  { key: "full", label: "٢٤ ساعة ٠٨:٠٠–٠٨:٠٠", start: "08:00", end: "08:00" },
] as const;

/**
 * The three duty refusals, said as what to do next. The backend's own sentences are
 * correct but general; here the operator is looking at one pharmacy and one time, so the
 * message can point at the fix.
 */
const DUTY_MESSAGES: Record<string, string> = {
  DUTY_NOT_SUPPORTED: "هذه المنشأة لا تُسجَّل لها مناوبات. اختر صيدلية.",
  DUTY_OVERLAP_OR_INVALID:
    "تتعارض هذه المناوبة مع مناوبة أخرى للصيدلية نفسها، أو أن وقتها غير صالح. راجع الوقت وحاول مرة أخرى.",
  DUTY_DURING_CLOSURE:
    "الصيدلية مغلقة مؤقتاً في هذا الوقت، فلا تُجدوَل لها مناوبة. اختر وقتاً آخر أو صيدلية أخرى.",
};

function dutyError(error: ApiErrorBody | null): ApiErrorBody | null {
  if (!error) return null;
  const message = DUTY_MESSAGES[error.code];
  return message ? { ...error, message } : error;
}

/** «يوم واحد», «يومان», «٣ أيام», «١١ يوماً» */
function daysCount(count: number): string {
  if (count === 1) return "يوم واحد";
  if (count === 2) return "يومان";
  if (count <= 10) return `${NUMBER.format(count)} أيام`;
  return `${NUMBER.format(count)} يوماً`;
}

/** «مناوبة واحدة», «مناوبتان», «٣ مناوبات», «١١ مناوبة» */
function shiftsCount(count: number): string {
  if (count === 1) return "مناوبة واحدة";
  if (count === 2) return "مناوبتان";
  if (count <= 10) return `${NUMBER.format(count)} مناوبات`;
  return `${NUMBER.format(count)} مناوبة`;
}

/** «من ٢٢:٠٠ السبت، ٢٦ أيلول إلى ٠٨:٠٠ الأحد، ٢٧ أيلول» */
function fullRange(startsAt: string, endsAt: string): string {
  const startDay = damascusDay(startsAt);
  const endDay = damascusDay(endsAt);
  const start = `${formatClock(startsAt)} ${formatDayLabel(startDay)}`;
  const end = endDay === startDay ? formatClock(endsAt) : `${formatClock(endsAt)} ${formatDayLabel(endDay)}`;
  return `من ${start} إلى ${end}`;
}

/**
 * The duty roster of one province (or one of its cities), two weeks at a time.
 *
 * Pharmacists enter their own duty shifts in the app; this is where an operator sees the
 * result and repairs it. A day nobody covers is flagged by the same rule as the DUTY_GAP
 * alert. Adding, moving or cancelling a shift here goes through the same validation as the
 * owner's own entry, is recorded as the administration's, is audited, and tells the
 * pharmacy's owners — which is why each of those actions says so before it runs.
 */
export default function DutyPage({
  searchParams,
}: {
  searchParams: Promise<Record<string, string | string[] | undefined>>;
}) {
  const query = use(searchParams);
  const requested = typeof query.province === "string" ? query.province : "";
  const canManage = useCan("admin.duty.manage");
  const [today] = useState(() => damascusDay(new Date()));
  const [start, setStart] = useState(() => weekStart(damascusDay(new Date())));
  const [chosenProvince, setChosenProvince] = useState(requested);
  const [cityId, setCityId] = useState("");
  const [selected, setSelected] = useState<string | null>(null);

  const provinces = useResource<{ items: Province[] }>("provinces");
  const provinceItems = [...(provinces.data?.items ?? [])].sort((a, b) => a.sortOrder - b.sortOrder);
  const provinceId =
    chosenProvince || (provinceItems.find((item) => item.active) ?? provinceItems[0])?.id || "";
  const province = provinceItems.find((item) => item.id === provinceId);

  const cities = useResource<{ items: City[] }>(
    "provinceCities",
    { id: provinceId },
    { enabled: Boolean(provinceId) },
  );
  const cityItems = cities.data?.items ?? [];
  const city = cityItems.find((item) => item.id === cityId);

  const end = addDays(start, WINDOW_DAYS - 1);
  const roster = useResource<Roster>(
    "dutyRoster",
    { provinceId, cityId, from: start, to: end },
    { enabled: Boolean(provinceId) },
  );

  // The last roster shown stays on screen while the next one loads, dimmed, so moving a week
  // or saving a shift does not flash the whole calendar back to a spinner.
  const [shown, setShown] = useState<Roster | null>(null);
  if (roster.data && roster.data !== shown) setShown(roster.data);
  const refreshing = roster.loading && shown !== null;

  const days = shown?.days.map((day) => ({ ...day, date: day.date.slice(0, 10) })) ?? [];
  const upcomingGaps = days.filter((day) => day.gap && day.date >= today).length;
  const selectedDay = days.find((day) => day.date === selected) ?? null;

  function changeProvince(id: string): void {
    setChosenProvince(id);
    setCityId("");
    setSelected(null);
    setShown(null);
  }

  return (
    <div className="stack">
      <PageHeader
        title="جدول المناوبات"
        description="من يناوب في كل يوم، والأيام التي لا تغطيها أي صيدلية."
      />

      {provinces.loading ? <LoadingState /> : null}
      {provinces.error ? <ErrorState error={provinces.error} onRetry={provinces.reload} /> : null}
      {provinces.data && provinceItems.length === 0 ? (
        <EmptyState title="لا محافظات بعد" />
      ) : null}

      {provinceId ? (
        <div className="filter-bar duty-toolbar" data-testid="duty-toolbar">
          <label className="filter-field">
            <span>المحافظة</span>
            <select
              value={provinceId}
              data-testid="duty-province"
              onChange={(event) => changeProvince(event.target.value)}
            >
              {provinceItems.map((item) => (
                <option key={item.id} value={item.id}>
                  {item.active ? item.nameAr : `${item.nameAr} (غير مفعّلة)`}
                </option>
              ))}
            </select>
          </label>
          {cityItems.length > 0 ? (
            <label className="filter-field">
              <span>المدينة</span>
              <select
                value={cityId}
                data-testid="duty-city"
                onChange={(event) => {
                  setCityId(event.target.value);
                  setSelected(null);
                }}
              >
                <option value="">كل المدن</option>
                {cityItems.map((item) => (
                  <option key={item.id} value={item.id}>
                    {item.nameAr}
                  </option>
                ))}
              </select>
            </label>
          ) : null}
          <div className="filter-actions">
            <button
              type="button"
              className="button-ghost icon-back"
              data-testid="duty-previous"
              onClick={() => setStart(addDays(start, -WINDOW_DAYS))}
            >
              <Icons.chevron />
              الأسبوعان السابقان
            </button>
            <button
              type="button"
              className="button-ghost"
              data-testid="duty-this-week"
              disabled={start === weekStart(today)}
              onClick={() => setStart(weekStart(today))}
            >
              هذا الأسبوع
            </button>
            <button
              type="button"
              className="button-ghost"
              data-testid="duty-next"
              onClick={() => setStart(addDays(start, WINDOW_DAYS))}
            >
              الأسبوعان التاليان
              <Icons.chevron />
            </button>
          </div>
        </div>
      ) : null}

      {roster.error ? <ErrorState error={roster.error} onRetry={roster.reload} /> : null}
      {provinceId && !shown && roster.loading ? <LoadingState label="جارٍ تحميل الجدول…" /> : null}

      {shown && !roster.error ? (
        <section className="panel duty-panel" aria-busy={refreshing || undefined} data-testid="duty-roster">
          <header className="duty-head">
            <div>
              <h2>
                {formatDayMonth(days[0]?.date ?? start)} –{" "}
                {formatDayMonth(days.at(-1)?.date ?? end)}
              </h2>
              <p className="muted">
                {province?.nameAr}
                {city ? ` · ${city.nameAr}` : cityItems.length > 0 ? " · كل المدن" : ""}
              </p>
            </div>
            <ul className="duty-legend" aria-label="دليل الألوان">
              <li>
                <span className="duty-swatch" data-kind="gap" aria-hidden="true" />
                يوم بلا مناوبة
              </li>
              <li>
                <span className="duty-swatch" data-kind="live" aria-hidden="true" />
                مناوبة جارية الآن
              </li>
              <li>
                <span className="duty-swatch" data-kind="today" aria-hidden="true" />
                اليوم
              </li>
            </ul>
          </header>

          {upcomingGaps > 0 ? (
            <p className="duty-summary" data-tone="danger" role="status" data-testid="duty-gaps">
              <Icons.alert />
              {`${daysCount(upcomingGaps)} بلا أي صيدلية مناوبة في هذه الفترة. اختر اليوم لإضافة مناوبة له.`}
            </p>
          ) : days.some((day) => day.date >= today) ? (
            <p className="duty-summary" data-tone="positive" role="status">
              <Icons.checkCircle />
              كل الأيام القادمة في هذه الفترة فيها صيدلية مناوبة.
            </p>
          ) : null}

          <div className="duty-calendar">
            <div className="duty-weekdays" aria-hidden="true">
              {WEEK_ORDER.map((key) => (
                <span key={key}>{WEEKDAYS[key]}</span>
              ))}
            </div>
            <ol className="duty-grid" aria-label="أيام الجدول">
              {days.map((day) => (
                <li key={day.date}>
                  <DayCell day={day} today={today} onOpen={setSelected} />
                </li>
              ))}
            </ol>
          </div>
          {refreshing ? (
            <p className="muted duty-refreshing" role="status">
              جارٍ التحديث…
            </p>
          ) : null}
        </section>
      ) : null}

      {selectedDay && provinceId ? (
        <DayPanel
          key={`${provinceId}-${selectedDay.date}`}
          day={selectedDay}
          today={today}
          provinceId={provinceId}
          cityId={cityId}
          cities={cityItems}
          canManage={canManage}
          onClose={() => setSelected(null)}
          onChanged={roster.reload}
        />
      ) : null}
    </div>
  );
}

function DayCell({
  day,
  today,
  onOpen,
}: {
  day: Day;
  today: string;
  onOpen: (date: string) => void;
}) {
  const past = day.date < today;
  const isToday = day.date === today;
  const gap = day.gap && !past;
  const visible = day.shifts.slice(0, 3);
  const more = day.shifts.length - visible.length;

  return (
    <button
      type="button"
      className="duty-day"
      data-gap={gap || undefined}
      data-past={past || undefined}
      data-today={isToday || undefined}
      data-testid={`duty-day-${day.date}`}
      onClick={() => onOpen(day.date)}
    >
      <span className="duty-date">
        <span className="sr-only">{formatDayLabel(day.date)}</span>
        <span className="duty-weekday" aria-hidden="true">
          {WEEKDAYS[WEEK_ORDER[(weekdayOf(day.date) + 1) % 7]!]}
        </span>
        <strong aria-hidden="true">{formatDayNumber(day.date)}</strong>
        <span aria-hidden="true">{formatMonth(day.date)}</span>
        {isToday ? <span className="duty-today">اليوم</span> : null}
      </span>
      {gap ? (
        <span className="duty-gap">
          <Icons.alert />
          لا توجد مناوبة
        </span>
      ) : null}
      {past && day.shifts.length === 0 ? <span className="duty-none">لم تُسجَّل مناوبة</span> : null}
      {visible.length > 0 ? (
        <span className="duty-chips">
          {visible.map((shift) => (
            <span
              key={shift.id}
              className="duty-chip"
              data-live={shift.status === "ONGOING" || undefined}
            >
              <span className="duty-chip-name">{shift.facilityNameAr}</span>
              <span className="duty-chip-meta">
                <span className="duty-chip-time">
                  {shiftSpanOn(day.date, shift.startsAt, shift.endsAt)}
                </span>
                <span className="duty-source" data-source={shift.createdBy}>
                  {SOURCE[shift.createdBy]?.label ?? shift.createdBy}
                </span>
              </span>
            </span>
          ))}
          {more > 0 ? <span className="duty-more">{`و${NUMBER.format(more)} غيرها`}</span> : null}
        </span>
      ) : null}
    </button>
  );
}

type Draft = Readonly<{
  shiftId: string | null;
  pharmacy: Pharmacy | null;
  startDay: string;
  startTime: string;
  endTime: string;
}>;

/**
 * One day: its shifts, and the form that adds one or moves one.
 *
 * Keyed by day, so opening another day starts from a clean form rather than carrying a
 * half-chosen pharmacy across.
 */
function DayPanel({
  day,
  today,
  provinceId,
  cityId,
  cities,
  canManage,
  onClose,
  onChanged,
}: {
  day: Day;
  today: string;
  provinceId: string;
  cityId: string;
  cities: readonly City[];
  canManage: boolean;
  onClose: () => void;
  onChanged: () => void;
}) {
  const mutation = useMutation();
  const past = day.date < today;
  const blank: Draft = {
    shiftId: null,
    pharmacy: null,
    startDay: day.date,
    startTime: PRESETS[0].start,
    endTime: PRESETS[0].end,
  };
  const [draft, setDraft] = useState<Draft>(blank);
  const [cancelling, setCancelling] = useState<Shift | null>(null);
  const [problem, setProblem] = useState<string | null>(null);
  const [toast, setToast] = useState<string | null>(null);
  const dismissToast = useCallback(() => setToast(null), []);

  const editing = draft.shiftId !== null;
  const endDay = endsNextDay(draft.startTime, draft.endTime) ? addDays(draft.startDay, 1) : draft.startDay;
  const canAdd = canManage && !past;

  function edit(shift: Shift): void {
    mutation.reset();
    setProblem(null);
    setDraft({
      shiftId: shift.id,
      pharmacy: { id: shift.facilityId, nameAr: shift.facilityNameAr, cityId: shift.cityId },
      startDay: damascusDay(shift.startsAt),
      startTime: damascusClock(shift.startsAt),
      endTime: damascusClock(shift.endsAt),
    });
  }

  async function save(): Promise<void> {
    if (!draft.pharmacy) {
      setProblem("اختر الصيدلية المناوبة.");
      return;
    }
    if (!draft.startTime || !draft.endTime || !draft.startDay) {
      setProblem("حدّد وقت البداية ووقت النهاية.");
      return;
    }
    setProblem(null);
    const startsAt = damascusInstant(draft.startDay, draft.startTime);
    const endsAt = damascusInstant(endDay, draft.endTime);
    const ok = editing
      ? await mutation.run("dutyShiftUpdate", { id: draft.shiftId, startsAt, endsAt })
      : await mutation.run("dutyShiftCreate", { facilityId: draft.pharmacy.id, startsAt, endsAt });
    if (!ok) return;
    setToast(
      editing
        ? "عُدّل وقت المناوبة، وأُبلغ أصحاب الصيدلية."
        : "أُضيفت المناوبة، وأُبلغ أصحاب الصيدلية.",
    );
    setDraft(blank);
    onChanged();
  }

  async function cancelShift(): Promise<void> {
    if (!cancelling) return;
    const ok = await mutation.run("dutyShiftDelete", { id: cancelling.id });
    if (!ok) return;
    setCancelling(null);
    if (draft.shiftId === cancelling.id) setDraft(blank);
    setToast("أُلغيت المناوبة، وأُبلغ أصحاب الصيدلية.");
    onChanged();
  }

  const preset = PRESETS.find((item) => item.start === draft.startTime && item.end === draft.endTime);

  return (
    <>
      <SidePanel
        open
        title={formatDayLabel(day.date)}
        description={
          day.gap && !past
            ? "لا توجد صيدلية مناوبة في هذا اليوم."
            : day.shifts.length > 0
              ? `${shiftsCount(day.shifts.length)} في هذا اليوم.`
              : "لم تُسجَّل مناوبة في هذا اليوم."
        }
        onClose={onClose}
        locked={cancelling !== null || mutation.pending}
        testId="duty-day-panel"
      >
        {past ? (
          <p className="notice">يوم مضى: تُعرض مناوباته للاطلاع، ولا تُضاف إليه مناوبات جديدة.</p>
        ) : null}

        {day.shifts.length > 0 ? (
          <ul className="shift-list" data-testid="duty-shifts">
            {day.shifts.map((shift) => (
              <li
                key={shift.id}
                className="shift-item"
                data-selected={draft.shiftId === shift.id || undefined}
              >
                <div className="shift-main">
                  <Link href={`/facilities/${shift.facilityId}`}>{shift.facilityNameAr}</Link>
                  <span className="muted">{fullRange(shift.startsAt, shift.endsAt)}</span>
                  <span className="button-row">
                    <StatusBadge tone={SOURCE[shift.createdBy]?.tone ?? "neutral"}>
                      {`أضافها ${SOURCE[shift.createdBy]?.label ?? shift.createdBy}`}
                    </StatusBadge>
                    {shift.status === "ONGOING" ? (
                      <TermBadge group="availability" value="DUTY" />
                    ) : (
                      <StatusBadge tone={STATUS[shift.status]?.tone ?? "neutral"}>
                        {STATUS[shift.status]?.label ?? shift.status}
                      </StatusBadge>
                    )}
                  </span>
                </div>
                {canManage && shift.status !== "ENDED" ? (
                  <div className="button-row">
                    <button
                      type="button"
                      className="button-ghost"
                      data-testid={`duty-edit-${shift.id}`}
                      onClick={() => edit(shift)}
                    >
                      تعديل الوقت
                    </button>
                    <button
                      type="button"
                      className="button-ghost"
                      data-tone="danger"
                      data-testid={`duty-cancel-${shift.id}`}
                      onClick={() => {
                        mutation.reset();
                        setCancelling(shift);
                      }}
                    >
                      إلغاء المناوبة
                    </button>
                  </div>
                ) : null}
              </li>
            ))}
          </ul>
        ) : null}

        {canAdd || editing ? (
          <section className="shift-form" data-testid="duty-form">
            <h3>{editing ? "تعديل وقت المناوبة" : "إضافة مناوبة"}</h3>

            {editing ? (
              <p className="shift-fixed">
                <span className="muted">الصيدلية</span>
                <strong>{draft.pharmacy?.nameAr}</strong>
              </p>
            ) : (
              <PharmacyPicker
                provinceId={provinceId}
                cityId={cityId}
                cities={cities}
                value={draft.pharmacy}
                onChange={(pharmacy) => {
                  setProblem(null);
                  setDraft({ ...draft, pharmacy });
                }}
              />
            )}

            <div className="preset-row" role="group" aria-label="أوقات جاهزة">
              {PRESETS.map((item) => (
                <button
                  key={item.key}
                  type="button"
                  className="button-ghost"
                  aria-pressed={preset?.key === item.key}
                  data-testid={`duty-preset-${item.key}`}
                  onClick={() => setDraft({ ...draft, startTime: item.start, endTime: item.end })}
                >
                  {item.label}
                </button>
              ))}
            </div>

            <div className="shift-times">
              {editing ? (
                <label className="field">
                  <span>يوم البداية</span>
                  <input
                    type="date"
                    dir="ltr"
                    value={draft.startDay}
                    data-testid="duty-start-day"
                    onChange={(event) => setDraft({ ...draft, startDay: event.target.value })}
                  />
                </label>
              ) : null}
              <label className="field">
                <span>من الساعة</span>
                <input
                  type="time"
                  dir="ltr"
                  value={draft.startTime}
                  data-testid="duty-start"
                  onChange={(event) => setDraft({ ...draft, startTime: event.target.value })}
                />
              </label>
              <label className="field">
                <span>إلى الساعة</span>
                <input
                  type="time"
                  dir="ltr"
                  value={draft.endTime}
                  data-testid="duty-end"
                  onChange={(event) => setDraft({ ...draft, endTime: event.target.value })}
                />
              </label>
            </div>
            {draft.startDay && draft.startTime && draft.endTime ? (
              <p className="field-hint" data-testid="duty-range">
                {`${fullRange(
                  damascusInstant(draft.startDay, draft.startTime),
                  damascusInstant(endDay, draft.endTime),
                )}${endDay !== draft.startDay ? " (تنتهي في اليوم التالي)" : ""}.`}
              </p>
            ) : null}

            {problem ? <p className="form-error">{problem}</p> : null}
            {mutation.error && !cancelling ? <ErrorState error={dutyError(mutation.error)} /> : null}

            <div className="button-row">
              <button
                type="button"
                className="button-primary"
                disabled={mutation.pending}
                aria-busy={mutation.pending || undefined}
                data-testid="duty-save"
                onClick={save}
              >
                {mutation.pending ? "جارٍ الحفظ…" : editing ? "حفظ الوقت" : "إضافة المناوبة"}
              </button>
              {editing ? (
                <button
                  type="button"
                  className="button-ghost"
                  disabled={mutation.pending}
                  onClick={() => {
                    mutation.reset();
                    setProblem(null);
                    setDraft(blank);
                  }}
                >
                  تراجع عن التعديل
                </button>
              ) : null}
            </div>
            <p className="field-hint">يصل إشعار بكل إضافة أو تعديل أو إلغاء إلى أصحاب الصيدلية.</p>
          </section>
        ) : null}
      </SidePanel>

      <ConfirmDialog
        open={cancelling !== null}
        title="إلغاء المناوبة"
        body={
          cancelling
            ? `تُحذف مناوبة ${cancelling.facilityNameAr} ${fullRange(cancelling.startsAt, cancelling.endsAt)} من الجدول، ويصل إشعار بذلك إلى أصحاب الصيدلية.`
            : undefined
        }
        confirmLabel="تأكيد إلغاء المناوبة"
        destructive
        pending={mutation.pending}
        error={dutyError(mutation.error)}
        onConfirm={cancelShift}
        onCancel={() => setCancelling(null)}
      />
      <Toast message={toast} onDismiss={dismissToast} />
    </>
  );
}

/**
 * Choose the pharmacy: active facilities of the province's pharmacy category, searched by
 * name on the server (the list is capped at 250) and narrowed to the city on screen.
 *
 * Pharmacies are the categories whose specialization is PHARMACY. If the taxonomy cannot be
 * read, every active facility of the province is offered and the backend's own refusal
 * (DUTY_NOT_SUPPORTED) covers a wrong pick.
 */
function PharmacyPicker({
  provinceId,
  cityId,
  cities,
  value,
  onChange,
}: {
  provinceId: string;
  cityId: string;
  cities: readonly City[];
  value: Pharmacy | null;
  onChange: (pharmacy: Pharmacy) => void;
}) {
  const categories = useResource<{ items: Category[] }>("categories");
  const pharmacyCategories = (categories.data?.items ?? []).filter(
    (item) => item.specialization === "PHARMACY",
  );
  const [categoryChoice, setCategoryChoice] = useState("");
  const categoryId = categoryChoice || pharmacyCategories[0]?.id || "";

  const [text, setText] = useState("");
  const [search, setSearch] = useState("");
  useEffect(() => {
    const timer = window.setTimeout(() => setSearch(text.trim()), 300);
    return () => window.clearTimeout(timer);
  }, [text]);

  const facilities = useResource<{ items: Pharmacy[] }>(
    "facilities",
    { province: provinceId, category: categoryId, status: "ACTIVE", q: search },
    { enabled: !categories.loading },
  );
  const items = (facilities.data?.items ?? []).filter((item) => !cityId || item.cityId === cityId);
  const cityName = (id: string | null) => cities.find((item) => item.id === id)?.nameAr;

  return (
    <div className="picker" data-testid="pharmacy-picker">
      <div className="picker-search">
        <label className="field">
          <span>الصيدلية</span>
          <input
            type="search"
            value={text}
            placeholder="ابحث باسم الصيدلية"
            data-testid="pharmacy-search"
            onChange={(event) => setText(event.target.value)}
          />
        </label>
        {pharmacyCategories.length > 1 ? (
          <label className="field">
            <span>التصنيف</span>
            <select
              value={categoryId}
              onChange={(event) => setCategoryChoice(event.target.value)}
            >
              {pharmacyCategories.map((item) => (
                <option key={item.id} value={item.id}>
                  {item.nameAr}
                </option>
              ))}
            </select>
          </label>
        ) : null}
      </div>
      {categories.data && pharmacyCategories.length === 0 ? (
        <p className="field-hint">لا يوجد تصنيف صيدليات؛ تظهر كل المنشآت الفعّالة في المحافظة.</p>
      ) : null}

      {value ? (
        <p className="picker-chosen" data-testid="pharmacy-chosen">
          <Icons.checkCircle />
          <span>
            المختارة: <strong>{value.nameAr}</strong>
          </span>
        </p>
      ) : null}

      {facilities.loading ? <LoadingState label="جارٍ البحث…" /> : null}
      {facilities.error ? <ErrorState error={facilities.error} onRetry={facilities.reload} /> : null}
      {facilities.data ? (
        items.length === 0 ? (
          <p className="muted picker-empty">
            {search ? "لا صيدلية فعّالة بهذا الاسم هنا." : "لا صيدليات فعّالة هنا بعد."}
          </p>
        ) : (
          <fieldset className="picker-results">
            <legend className="sr-only">نتائج البحث</legend>
            {items.map((item) => (
              <label key={item.id} className="picker-option">
                <input
                  type="radio"
                  name="duty-pharmacy"
                  checked={value?.id === item.id}
                  data-testid={`pharmacy-${item.id}`}
                  onChange={() => onChange(item)}
                />
                <span>{item.nameAr}</span>
                {cityName(item.cityId) ? <small className="muted">{cityName(item.cityId)}</small> : null}
              </label>
            ))}
          </fieldset>
        )
      ) : null}
      {(facilities.data?.items.length ?? 0) >= 250 ? (
        <p className="field-hint">تظهر أول ٢٥٠ نتيجة. اكتب جزءاً من الاسم لتضييق البحث.</p>
      ) : null}
    </div>
  );
}
