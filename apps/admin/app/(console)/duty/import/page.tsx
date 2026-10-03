"use client";

import { useEffect, useState } from "react";

import {
  type Column,
  ConfirmDialog,
  DataTable,
  EmptyState,
  ErrorState,
  FormSection,
  LoadingState,
  PageHeader,
  Panel,
  StatusBadge,
  Toast,
  formatDateTime,
} from "../../../../components/ui";
import { uploadDutyRoster } from "../../../../lib/client/files";
import { useMutation } from "../../../../lib/client/use-mutation";
import { useResource } from "../../../../lib/client/use-resource";
import { type ApiErrorBody, messageFor } from "../../../../lib/errors/messages";

type Named = Readonly<{ id: string; nameAr: string }>;
type Category = Readonly<{ id: string; nameAr: string; specialization: string }>;

type ReportRow = Readonly<{
  line: number;
  facilityId: string | null;
  facilityNameAr: string | null;
  startsAt: string | null;
  endsAt: string | null;
  outcome: "CREATED" | "UPDATED" | "UNCHANGED" | null;
  problems: readonly string[];
}>;

type Report = Readonly<{
  applied: boolean;
  rows: readonly ReportRow[];
  errorCount: number;
  created: number;
  unchanged: number;
}>;

type Rotation = Readonly<{
  id: string;
  name: string;
  provinceId: string;
  facilityIds: readonly string[];
  startsAt: string;
  endsAt: string;
  perDay: number;
  anchorDate: string;
}>;

const OUTCOME: Record<string, { label: string; tone: "positive" | "neutral" | "info" }> = {
  CREATED: { label: "جديدة", tone: "positive" },
  UPDATED: { label: "معدّلة", tone: "info" },
  UNCHANGED: { label: "موجودة", tone: "neutral" },
};

const SAMPLE = "الصيدلية,التاريخ,من,إلى\nصيدلية الأمل,2026-11-01,20:00,08:00\n";

function downloadSample(): void {
  const blob = new Blob(["﻿", SAMPLE], { type: "text/csv;charset=utf-8" });
  const link = document.createElement("a");
  link.href = URL.createObjectURL(blob);
  link.download = "duty-roster-sample.csv";
  link.click();
  URL.revokeObjectURL(link.href);
}

/** The row-by-row answer of a preview or an apply: what each line would do, or why not. */
function RosterReport({ report }: { report: Report }) {
  const columns: readonly Column<ReportRow>[] = [
    { key: "line", header: "السطر", width: "1%", render: (row) => <span className="tabular">{row.line}</span> },
    { key: "pharmacy", header: "الصيدلية", render: (row) => row.facilityNameAr ?? <span className="muted">—</span> },
    { key: "starts", header: "من", ltr: true, render: (row) => formatDateTime(row.startsAt) },
    { key: "ends", header: "إلى", ltr: true, render: (row) => formatDateTime(row.endsAt) },
    {
      key: "state",
      header: "النتيجة",
      render: (row) =>
        row.problems.length > 0 ? (
          <span className="field-error">{row.problems.join(" ")}</span>
        ) : row.outcome ? (
          <StatusBadge tone={OUTCOME[row.outcome]?.tone ?? "neutral"}>
            {OUTCOME[row.outcome]?.label ?? row.outcome}
          </StatusBadge>
        ) : null,
    },
  ];
  return (
    <div className="stack" data-testid="roster-report">
      <p className={report.errorCount > 0 ? "field-error" : "notice"} role="status">
        {report.errorCount > 0
          ? `${report.errorCount} سطر فيه مشكلة. صحّحها في الملف وأعد المعاينة؛ لا يُكتب شيء قبل ذلك.`
          : report.applied
            ? `كُتب الجدول: ${report.created} وردية جديدة، و${report.unchanged} كانت موجودة.`
            : `الجدول سليم: ${report.created} وردية جديدة، و${report.unchanged} موجودة أصلاً.`}
      </p>
      <DataTable caption="نتيجة المعاينة" columns={columns} rows={report.rows} rowKey={(row) => String(row.line)} />
    </div>
  );
}

function ImportPanel({ provinceId }: { provinceId: string }) {
  const [file, setFile] = useState<File | null>(null);
  const [report, setReport] = useState<Report | null>(null);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<ApiErrorBody | null>(null);
  const [toast, setToast] = useState<string | null>(null);

  async function send(apply: boolean): Promise<void> {
    if (!file || !provinceId) return;
    setPending(true);
    setError(null);
    const result = await uploadDutyRoster<Report>(file, provinceId, apply);
    setPending(false);
    if (!result.ok) {
      setError(result.error);
      return;
    }
    setReport(result.data);
    if (result.data.applied) setToast("كُتب الجدول وأُبلغ مالكو الصيدليات.");
  }

  return (
    <FormSection
      title="استيراد جدول من ملف"
      description="ملف CSV أو Excel بأعمدة: الصيدلية، التاريخ، من، إلى. تُعاين الأسطر كلها قبل الكتابة، ويُكتب الجدول كله أو لا شيء."
    >
      <label className="field">
        <span>الملف</span>
        <input
          type="file"
          accept=".csv,.xlsx"
          data-testid="roster-file"
          onChange={(event) => {
            setFile(event.target.files?.[0] ?? null);
            setReport(null);
          }}
        />
      </label>
      <div className="button-row">
        <button type="button" className="button-ghost" onClick={downloadSample}>
          تنزيل نموذج
        </button>
        <button
          type="button"
          className="button-primary"
          disabled={!file || !provinceId || pending}
          data-testid="roster-preview"
          onClick={() => void send(false)}
        >
          {pending ? "جارٍ القراءة…" : "معاينة"}
        </button>
        {report && !report.applied && report.errorCount === 0 && report.created > 0 ? (
          <button
            type="button"
            className="button-primary"
            disabled={pending}
            data-testid="roster-apply"
            onClick={() => void send(true)}
          >
            تطبيق الجدول
          </button>
        ) : null}
      </div>
      {error ? (
        <p className="field-error" role="alert">
          {messageFor(error)}
        </p>
      ) : null}
      {report ? (
        <div style={{ gridColumn: "1 / -1" }}>
          <RosterReport report={report} />
        </div>
      ) : null}
      <Toast message={toast} onDismiss={() => setToast(null)} />
    </FormSection>
  );
}

/** Pick pharmacies by searching, in the order they take their turn. */
function RotationEditor({
  provinceId,
  onSaved,
}: {
  provinceId: string;
  onSaved: () => void;
}) {
  const mutation = useMutation();
  const categories = useResource<{ items: Category[] }>("categories");
  const pharmacyCategory =
    (categories.data?.items ?? []).find((item) => item.specialization === "PHARMACY")?.id ?? "";
  const [text, setText] = useState("");
  const [search, setSearch] = useState("");
  useEffect(() => {
    const timer = window.setTimeout(() => setSearch(text.trim()), 300);
    return () => window.clearTimeout(timer);
  }, [text]);
  const found = useResource<{ items: Named[] }>(
    "facilities",
    { province: provinceId, category: pharmacyCategory, status: "ACTIVE", q: search },
    { enabled: Boolean(provinceId && pharmacyCategory) },
  );
  const [chosen, setChosen] = useState<Named[]>([]);
  const [draft, setDraft] = useState({
    name: "",
    startsAt: "20:00",
    endsAt: "08:00",
    perDay: "1",
    anchorDate: new Date().toISOString().slice(0, 10),
  });

  function move(index: number, by: number): void {
    setChosen((current) => {
      const next = [...current];
      const [item] = next.splice(index, 1);
      next.splice(Math.max(0, Math.min(next.length, index + by)), 0, item!);
      return next;
    });
  }

  async function save(): Promise<void> {
    const ok = await mutation.run("dutyRotationCreate", {
      name: draft.name.trim(),
      provinceId,
      facilityIds: chosen.map((item) => item.id),
      startsAt: draft.startsAt,
      endsAt: draft.endsAt,
      perDay: Number(draft.perDay) || 1,
      anchorDate: draft.anchorDate,
    });
    if (!ok) return;
    setChosen([]);
    setDraft({ ...draft, name: "" });
    onSaved();
  }

  return (
    <FormSection
      title="قالب دوري جديد"
      description="الصيدليات بالترتيب الذي تتناوب به. في «يوم البداية» تناوب الأولى، وفي اليوم التالي التي بعدها، ثم يعود الدور من أول القائمة."
      footer={
        <button
          type="button"
          className="button-primary"
          disabled={mutation.pending || !draft.name.trim() || chosen.length === 0}
          data-testid="rotation-save"
          onClick={() => void save()}
        >
          {mutation.pending ? "جارٍ الحفظ…" : "حفظ القالب"}
        </button>
      }
    >
      <label className="field">
        <span>اسم القالب</span>
        <input
          value={draft.name}
          data-testid="rotation-name"
          onChange={(event) => setDraft({ ...draft, name: event.target.value })}
        />
      </label>
      <label className="field">
        <span>تبدأ المناوبة</span>
        <input type="time" dir="ltr" value={draft.startsAt} onChange={(event) => setDraft({ ...draft, startsAt: event.target.value })} />
      </label>
      <label className="field">
        <span>تنتهي</span>
        <input type="time" dir="ltr" value={draft.endsAt} onChange={(event) => setDraft({ ...draft, endsAt: event.target.value })} />
      </label>
      <label className="field">
        <span>صيدليات كل ليلة</span>
        <input type="number" min={1} max={10} dir="ltr" value={draft.perDay} onChange={(event) => setDraft({ ...draft, perDay: event.target.value })} />
      </label>
      <label className="field">
        <span>يوم البداية</span>
        <input type="date" dir="ltr" value={draft.anchorDate} onChange={(event) => setDraft({ ...draft, anchorDate: event.target.value })} />
      </label>
      <label className="field">
        <span>أضف صيدلية</span>
        <input
          type="search"
          value={text}
          placeholder="ابحث باسم الصيدلية"
          data-testid="rotation-search"
          onChange={(event) => setText(event.target.value)}
        />
      </label>
      <div style={{ gridColumn: "1 / -1" }} className="stack">
        {found.loading ? <LoadingState label="جارٍ البحث…" /> : null}
        <div className="button-row" style={{ flexWrap: "wrap" }}>
          {(found.data?.items ?? [])
            .filter((item) => !chosen.some((picked) => picked.id === item.id))
            .slice(0, 12)
            .map((item) => (
              <button
                key={item.id}
                type="button"
                className="button-ghost"
                data-testid={`rotation-add-${item.id}`}
                onClick={() => setChosen([...chosen, item])}
              >
                + {item.nameAr}
              </button>
            ))}
        </div>
        {chosen.length > 0 ? (
          <ol className="rotation-order" data-testid="rotation-order">
            {chosen.map((item, index) => (
              <li key={item.id}>
                <span>{item.nameAr}</span>
                <span className="button-row">
                  <button type="button" className="button-ghost" disabled={index === 0} onClick={() => move(index, -1)} aria-label="أعلى">
                    ↑
                  </button>
                  <button type="button" className="button-ghost" disabled={index === chosen.length - 1} onClick={() => move(index, 1)} aria-label="أسفل">
                    ↓
                  </button>
                  <button type="button" className="button-ghost" onClick={() => setChosen(chosen.filter((picked) => picked.id !== item.id))} aria-label="إزالة">
                    ×
                  </button>
                </span>
              </li>
            ))}
          </ol>
        ) : (
          <span className="muted">لم تُضف صيدليات بعد.</span>
        )}
      </div>
      {mutation.error ? (
        <p className="field-error" role="alert" style={{ gridColumn: "1 / -1" }}>
          {messageFor(mutation.error)}
        </p>
      ) : null}
    </FormSection>
  );
}

function RotationRow({ rotation, onDeleted }: { rotation: Rotation; onDeleted: () => void }) {
  const mutation = useMutation();
  const today = new Date().toISOString().slice(0, 10);
  const [range, setRange] = useState({ fromDate: today, toDate: today });
  const [report, setReport] = useState<Report | null>(null);
  const [deleting, setDeleting] = useState(false);

  async function generate(apply: boolean): Promise<void> {
    const answer = await mutation.runFor<Report>("dutyRotationGenerate", {
      id: rotation.id,
      ...range,
      apply,
    });
    if (answer) setReport(answer);
  }

  return (
    <Panel
      title={rotation.name}
      description={`${rotation.facilityIds.length} صيدلية · ${rotation.perDay} كل ليلة · من ${rotation.startsAt} إلى ${rotation.endsAt}`}
      actions={
        <button type="button" className="button-ghost" onClick={() => setDeleting(true)}>
          حذف القالب
        </button>
      }
    >
      <div className="form-grid">
        <label className="field">
          <span>من يوم</span>
          <input type="date" dir="ltr" value={range.fromDate} onChange={(event) => setRange({ ...range, fromDate: event.target.value })} />
        </label>
        <label className="field">
          <span>إلى يوم</span>
          <input type="date" dir="ltr" value={range.toDate} onChange={(event) => setRange({ ...range, toDate: event.target.value })} />
        </label>
        <div className="button-row" style={{ alignSelf: "end" }}>
          <button type="button" className="button-ghost" disabled={mutation.pending} data-testid={`rotation-preview-${rotation.id}`} onClick={() => void generate(false)}>
            معاينة الفترة
          </button>
          {report && !report.applied && report.errorCount === 0 && report.created > 0 ? (
            <button type="button" className="button-primary" disabled={mutation.pending} data-testid={`rotation-apply-${rotation.id}`} onClick={() => void generate(true)}>
              تطبيق
            </button>
          ) : null}
        </div>
      </div>
      {mutation.error ? <p className="field-error">{messageFor(mutation.error)}</p> : null}
      {report ? <RosterReport report={report} /> : null}
      <ConfirmDialog
        open={deleting}
        title="حذف القالب"
        body="تبقى الورديات التي ولّدها؛ يُحذف القالب وحده."
        confirmLabel="حذف"
        destructive
        pending={mutation.pending}
        error={mutation.error}
        onConfirm={async () => {
          if (await mutation.run("dutyRotationDelete", { id: rotation.id })) {
            setDeleting(false);
            onDeleted();
          }
        }}
        onCancel={() => setDeleting(false)}
      />
    </Panel>
  );
}

/**
 * Duty in bulk: a roster from the syndicate's spreadsheet, and rotations that generate months.
 *
 * Both are previewed first, row by row against the province's pharmacies and the shifts
 * already stored, and written all at once or not at all.
 */
export default function DutyImportPage() {
  const provinces = useResource<{ items: Named[] }>("provinces");
  const [provinceChoice, setProvinceChoice] = useState("");
  const provinceId = provinceChoice || provinces.data?.items[0]?.id || "";
  const rotations = useResource<{ items: Rotation[] }>(
    "dutyRotations",
    { provinceId },
    { enabled: Boolean(provinceId) },
  );

  return (
    <div className="stack">
      <PageHeader
        back={{ href: "/duty", label: "جدول المناوبات" }}
        title="الاستيراد والقوالب"
        description="جدول المناوبات دفعة واحدة: من ملف النقابة، أو من قالب دوري يولّد الأشهر."
        actions={
          <label className="field field-inline">
            <span>المحافظة</span>
            <select value={provinceId} data-testid="import-province" onChange={(event) => setProvinceChoice(event.target.value)}>
              {(provinces.data?.items ?? []).map((item) => (
                <option key={item.id} value={item.id}>
                  {item.nameAr}
                </option>
              ))}
            </select>
          </label>
        }
      />
      {provinces.error ? <ErrorState error={provinces.error} onRetry={provinces.reload} /> : null}
      {provinceId ? (
        <>
          <ImportPanel provinceId={provinceId} />
          <RotationEditor provinceId={provinceId} onSaved={rotations.reload} />
          {rotations.loading ? <LoadingState /> : null}
          {rotations.data && rotations.data.items.length === 0 ? (
            <EmptyState title="لا قوالب دورية لهذه المحافظة بعد" hint="أنشئ قالباً أعلاه ثم ولّد منه الأشهر." />
          ) : null}
          {(rotations.data?.items ?? []).map((item) => (
            <RotationRow key={item.id} rotation={item} onDeleted={rotations.reload} />
          ))}
        </>
      ) : null}
    </div>
  );
}
