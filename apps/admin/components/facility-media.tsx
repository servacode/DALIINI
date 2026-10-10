"use client";

import { useRef, useState } from "react";

import { Icon } from "./icons";
import { ErrorState, LoadingState } from "./ui";
import { uploadFacilityImage } from "../lib/client/files";
import { useMutation } from "../lib/client/use-mutation";
import { useResource } from "../lib/client/use-resource";
import { type ApiErrorBody, messageFor } from "../lib/errors/messages";

type Hour = Readonly<{ weekday: number; opensAt: string; closesAt: string; sequence: number }>;
type Photo = Readonly<{ id: string; url: string; sortOrder: number }>;

/** One editable period: an empty pair means the day is closed. */
type Span = { opensAt: string; closesAt: string };

// Saturday first, as the week reads in Syria; the wire's weekday is Monday = 0.
const DAYS: readonly { weekday: number; label: string }[] = [
  { weekday: 5, label: "السبت" },
  { weekday: 6, label: "الأحد" },
  { weekday: 0, label: "الاثنين" },
  { weekday: 1, label: "الثلاثاء" },
  { weekday: 2, label: "الأربعاء" },
  { weekday: 3, label: "الخميس" },
  { weekday: 4, label: "الجمعة" },
];

const ACCEPTED = ["image/jpeg", "image/png", "image/webp"];

function hhmm(value: string): string {
  return value.slice(0, 5);
}

function toWeek(items: readonly Hour[]): Record<number, Span[]> {
  const week: Record<number, Span[]> = {};
  for (const day of DAYS) week[day.weekday] = [];
  for (const item of [...items].sort((a, b) => a.sequence - b.sequence)) {
    week[item.weekday]?.push({ opensAt: hhmm(item.opensAt), closesAt: hhmm(item.closesAt) });
  }
  return week;
}

/**
 * A facility's week and photos, set by the operator (DECISION-115).
 *
 * The directory opens empty and the first facilities are the platform's own: without this a
 * pharmacy the operator added could never be «مفتوح الآن» and always showed a placeholder.
 * Each day is closed, or one or two periods; a closing time before the opening one runs past
 * midnight, as the owner's own form explains. Photos go through the same checks as the owner's.
 */
export function FacilityMedia({ facilityId, canEdit }: { facilityId: string; canEdit: boolean }) {
  return (
    // Stacked: a day with two periods is wider than half the window, and side by side the
    // photos column lay over it (found in the readiness run).
    <div className="stack">
      <HoursEditor facilityId={facilityId} canEdit={canEdit} />
      <PhotosEditor facilityId={facilityId} canEdit={canEdit} />
    </div>
  );
}

function HoursEditor({ facilityId, canEdit }: { facilityId: string; canEdit: boolean }) {
  const hours = useResource<{ items: Hour[] }>("facilityHours", { id: facilityId });
  const mutation = useMutation();
  const [week, setWeek] = useState<Record<number, Span[]> | null>(null);
  const [saved, setSaved] = useState(false);

  const current = week ?? (hours.data ? toWeek(hours.data.items) : null);

  function change(weekday: number, spans: Span[]): void {
    if (!current) return;
    setSaved(false);
    setWeek({ ...current, [weekday]: spans });
  }

  async function save(): Promise<void> {
    if (!current) return;
    const items = DAYS.flatMap((day) =>
      (current[day.weekday] ?? [])
        .filter((span) => span.opensAt && span.closesAt)
        .map((span, sequence) => ({
          weekday: day.weekday,
          opensAt: span.opensAt,
          closesAt: span.closesAt,
          sequence,
        })),
    );
    const ok = await mutation.run("facilityHoursReplace", { id: facilityId, items });
    if (!ok) return;
    setSaved(true);
    setWeek(null);
    hours.reload();
  }

  return (
    <section className="settings-block" data-testid="facility-hours">
      <header>
        <strong>أوقات الدوام</strong>
        <span>يوم مغلق، أو فترة أو فترتان. إغلاق قبل الفتح يعني أن الفترة تمتد بعد منتصف الليل.</span>
      </header>
      {hours.loading ? <LoadingState /> : null}
      {hours.error ? <ErrorState error={hours.error} onRetry={hours.reload} /> : null}
      {current ? (
        <ul className="hours-editor">
          {DAYS.map((day) => {
            const spans = current[day.weekday] ?? [];
            return (
              <li key={day.weekday} className="hours-day" data-closed={spans.length === 0 || undefined}>
                <span className="hours-day-name">{day.label}</span>
                <div className="hours-spans">
                  {spans.length === 0 ? <span className="muted">مغلق</span> : null}
                  {spans.map((span, index) => (
                    <span key={index} className="hours-span">
                      <input
                        type="time"
                        dir="ltr"
                        aria-label={`${day.label}: يفتح`}
                        value={span.opensAt}
                        disabled={!canEdit}
                        data-testid={`hours-${day.weekday}-${index}-open`}
                        onChange={(event) =>
                          change(
                            day.weekday,
                            spans.map((s, i) => (i === index ? { ...s, opensAt: event.target.value } : s)),
                          )
                        }
                      />
                      <span aria-hidden="true">–</span>
                      <input
                        type="time"
                        dir="ltr"
                        aria-label={`${day.label}: يغلق`}
                        value={span.closesAt}
                        disabled={!canEdit}
                        data-testid={`hours-${day.weekday}-${index}-close`}
                        onChange={(event) =>
                          change(
                            day.weekday,
                            spans.map((s, i) => (i === index ? { ...s, closesAt: event.target.value } : s)),
                          )
                        }
                      />
                      {canEdit ? (
                        <button
                          type="button"
                          className="icon-button"
                          aria-label={`حذف فترة ${day.label}`}
                          onClick={() => change(day.weekday, spans.filter((_, i) => i !== index))}
                        >
                          <Icon name="close" width={14} height={14} />
                        </button>
                      ) : null}
                    </span>
                  ))}
                </div>
                {canEdit && spans.length < 2 ? (
                  <button
                    type="button"
                    className="button-link"
                    data-testid={`hours-${day.weekday}-add`}
                    onClick={() =>
                      change(day.weekday, [
                        ...spans,
                        spans.length === 0
                          ? { opensAt: "09:00", closesAt: "21:00" }
                          : { opensAt: "17:00", closesAt: "21:00" },
                      ])
                    }
                  >
                    {spans.length === 0 ? "فتح هذا اليوم" : "فترة ثانية"}
                  </button>
                ) : null}
              </li>
            );
          })}
        </ul>
      ) : null}
      {mutation.error ? <ErrorState error={mutation.error} /> : null}
      {canEdit && current ? (
        <div className="button-row">
          <button
            type="button"
            className="button-primary"
            disabled={mutation.pending || week === null}
            data-testid="hours-save"
            onClick={save}
          >
            {mutation.pending ? "جارٍ الحفظ…" : "حفظ الدوام"}
          </button>
          {saved ? <span className="muted">حُفظ الدوام.</span> : null}
        </div>
      ) : null}
    </section>
  );
}

function PhotosEditor({ facilityId, canEdit }: { facilityId: string; canEdit: boolean }) {
  const photos = useResource<{ items: Photo[] }>("facilityImages", { id: facilityId });
  const mutation = useMutation();
  const [uploading, setUploading] = useState(false);
  const [problem, setProblem] = useState<string | null>(null);
  const picker = useRef<HTMLInputElement>(null);

  async function pick(file: File): Promise<void> {
    setProblem(null);
    if (!ACCEPTED.includes(file.type)) {
      setProblem("الصورة يجب أن تكون بصيغة JPEG أو PNG أو WebP.");
      return;
    }
    if (file.size > 2 * 1024 * 1024) {
      setProblem("حجم الصورة أكبر من 2 ميغابايت. صغّرها ثم أعد المحاولة.");
      return;
    }
    setUploading(true);
    const result = await uploadFacilityImage(facilityId, file);
    setUploading(false);
    if (!result.ok) {
      setProblem(messageFor(result.error as ApiErrorBody));
      return;
    }
    photos.reload();
  }

  async function remove(photo: Photo): Promise<void> {
    const ok = await mutation.run("facilityImageDelete", { facilityId, imageId: photo.id });
    if (ok) photos.reload();
  }

  const items = [...(photos.data?.items ?? [])].sort((a, b) => a.sortOrder - b.sortOrder);
  return (
    <section className="settings-block" data-testid="facility-photos">
      <header>
        <strong>الصور</strong>
        <span>الأولى تظهر على بطاقة المنشأة في التطبيق والموقع.</span>
      </header>
      {photos.loading ? <LoadingState /> : null}
      {photos.error ? <ErrorState error={photos.error} onRetry={photos.reload} /> : null}
      {photos.data && items.length === 0 ? <p className="muted">لا صور بعد.</p> : null}
      {items.length > 0 ? (
        <ul className="photo-grid">
          {items.map((photo, index) => (
            <li key={photo.id}>
              {/* eslint-disable-next-line @next/next/no-img-element -- public media, no loader */}
              <img src={photo.url} alt="" loading="lazy" />
              {index === 0 ? <span className="photo-badge">على البطاقة</span> : null}
              {canEdit ? (
                <button
                  type="button"
                  className="photo-remove"
                  aria-label="حذف الصورة"
                  disabled={mutation.pending}
                  data-testid={`photo-delete-${photo.id}`}
                  onClick={() => remove(photo)}
                >
                  <Icon name="trash" width={14} height={14} />
                </button>
              ) : null}
            </li>
          ))}
        </ul>
      ) : null}
      {problem ? (
        <span className="field-error" role="alert">
          {problem}
        </span>
      ) : null}
      {mutation.error ? <ErrorState error={mutation.error} /> : null}
      {canEdit ? (
        <>
          <input
            ref={picker}
            type="file"
            accept={ACCEPTED.join(",")}
            hidden
            data-testid="photo-file"
            onChange={(event) => {
              const file = event.target.files?.[0];
              event.target.value = "";
              if (file) void pick(file);
            }}
          />
          <div className="button-row">
            <button
              type="button"
              className="button-ghost"
              disabled={uploading}
              data-testid="photo-add"
              onClick={() => picker.current?.click()}
            >
              <Icon name="upload" width={16} height={16} />
              {uploading ? "جارٍ الرفع…" : "إضافة صورة"}
            </button>
          </div>
        </>
      ) : null}
    </section>
  );
}
