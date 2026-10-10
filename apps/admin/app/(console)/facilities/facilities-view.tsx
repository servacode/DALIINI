"use client";

import Link from "next/link";
import { useState } from "react";

import { useCan } from "../../../components/admin-shell";
import { ExportButton } from "../../../components/export-button";
import { type EditableFacility, FacilityForm } from "../../../components/facility-form";
import { FacilityMedia } from "../../../components/facility-media";
import { Icon, type IconName } from "../../../components/icons";
import {
  ConfirmDialog,
  EmptyState,
  ErrorState,
  LoadingState,
  Pagination,
  StatusBadge,
  type Tone,
  Toast,
  formatDateTime,
} from "../../../components/ui";
import {
  FilterChips,
  FormDialog,
  PageHero,
  ProfileCard,
  SearchBox,
  relativeTime,
} from "../../../components/ui/extra";
import { useCursorPage } from "../../../lib/client/use-cursor-page";
import { useLookups } from "../../../lib/client/use-lookups";
import { useMutation } from "../../../lib/client/use-mutation";
import { useResource } from "../../../lib/client/use-resource";
import { useUrlFilters } from "../../../lib/client/use-url-filters";
import { QUALITY_ISSUES, STATUS } from "./terms";

type FacilityRow = Readonly<{
  id: string;
  nameAr: string;
  status: string;
  provinceId: string;
  categoryId: string;
  categoryNameAr: string;
  provinceNameAr: string;
  cityNameAr: string | null;
  categoryIconKey: string;
  phone: string | null;
  whatsapp: string | null;
  addressAr: string | null;
  location: { latitude: number; longitude: number } | null;
  ownerName: string | null;
  ownerPhone: string | null;
  qualityScore: number;
  qualityIssues: readonly string[];
  imageUrl: string | null;
  updatedAt: string | null;
  createdAt: string | null;
}>;

type TimelineEvent = Readonly<{
  at: string;
  kind: string;
  titleAr: string;
  actorName: string | null;
}>;

/** Which facilities the list shows: one choice, so one row of chips. */
type Show = "" | "ACTIVE" | "SUBMITTED" | "DRAFT" | "SUSPENDED" | "CLOSED";

const SHOW: readonly { value: Show; label: string }[] = [
  { value: "", label: "الكل" },
  { value: "ACTIVE", label: "فعّالة" },
  { value: "SUBMITTED", label: "قيد المراجعة" },
  { value: "DRAFT", label: "مسودة" },
  { value: "SUSPENDED", label: "موقوفة" },
  { value: "CLOSED", label: "مغلقة" },
];

/** The icon in the band's corner says the state before its word is read. */
const STATUS_ICON: Record<string, IconName> = {
  ACTIVE: "checkCircle",
  SUBMITTED: "clock",
  DRAFT: "edit",
  SUSPENDED: "lock",
  CLOSED: "close",
  REVERIFICATION_REQUIRED: "refresh",
};

/** The category's own drawing where the icon set has one; a building otherwise. */
const CATEGORY_ICONS = new Set<IconName>(["pharmacy", "clinic", "lab", "emergency", "home", "building"]);

const TIMELINE_TONE: Record<string, Tone> = {
  APPLICATION_SUBMITTED: "info",
  APPLICATION_APPROVED: "positive",
  APPLICATION_REJECTED: "danger",
  REPORT_CREATED: "warning",
  REPORT_RESOLVED: "positive",
  REPORT_DISMISSED: "neutral",
  DUTY_SUMMARY: "brand",
  AUDIT: "neutral",
};

type Decision = "suspend" | "reactivate" | "close";

const DECISIONS: Record<
  Decision,
  { operation: string; title: string; body: string; label: string; destructive?: boolean; reason?: boolean }
> = {
  suspend: {
    operation: "facilitySuspend",
    title: "إيقاف المنشأة",
    body: "تختفي من النتائج العامة فوراً، ويمكن إعادة تفعيلها لاحقاً. اكتب السبب: يُحفظ في سجل التدقيق.",
    label: "تأكيد الإيقاف",
    destructive: true,
    reason: true,
  },
  reactivate: {
    operation: "facilityReactivate",
    title: "إعادة تفعيل المنشأة",
    body: "تعود للظهور في النتائج العامة.",
    label: "تأكيد التفعيل",
  },
  close: {
    operation: "facilityClose",
    title: "إغلاق المنشأة نهائياً",
    body: "إجراء تشغيلي كبير لا يُتراجع عنه من هنا. اكتب السبب: يُحفظ في سجل التدقيق.",
    label: "تأكيد الإغلاق",
    destructive: true,
    reason: true,
  },
};

/**
 * Every facility, each one a card that holds all of itself (DECISION-109), on the page the
 * accounts already use: an emerald hero, a toolbar that filters as it changes, cards three
 * across.
 *
 * Adding a facility and correcting one happen in a window over the list, and where it is, is
 * chosen with a pin on the platform's own map. What the old detail page held is on the card or
 * one press away from it: the decisions (suspend, reactivate, close), the facility's history in
 * a window, and the map. `/facilities/<id>` still resolves, to this list with that card outlined.
 */
export function FacilitiesView({ mapStyleUrl }: { mapStyleUrl: string }) {
  const [filters, setFilters] = useUrlFilters({
    q: "",
    status: "",
    province: "",
    category: "",
    issue: "",
    ordering: "",
    id: "",
    edit: "",
    add: "",
  });
  const { add, edit, ...listFilters } = filters;
  const facilities = useCursorPage<FacilityRow>("facilities", listFilters);
  const lookups = useLookups();
  const canEdit = useCan("admin.facilities.edit");
  const canManage = useCan("admin.facilities.manage");
  const mutation = useMutation();

  const [decision, setDecision] = useState<{ kind: Decision; row: FacilityRow } | null>(null);
  const [reason, setReason] = useState("");
  const [history, setHistory] = useState<FacilityRow | null>(null);
  const [media, setMedia] = useState<FacilityRow | null>(null);
  const [owning, setOwning] = useState<FacilityRow | null>(null);
  const [toast, setToast] = useState<string | null>(null);
  const linked = filters.id || null;

  function openEdit(id: string): void {
    setFilters({ ...filters, edit: id, add: "" });
  }
  function closeForm(): void {
    setFilters({ ...filters, edit: "", add: "" });
  }

  return (
    <div className="stack">
      <PageHero
        eyebrow="الدليل"
        title="المنشآت"
        description="كل منشأة في الدليل ببطاقتها: صورتها، وحالتها، وكيف يُوصَل إليها، ومن يملكها، وما ينقص بياناتها."
        action={
          <span className="button-row">
            {canEdit ? (
              <button
                type="button"
                className="page-hero-action"
                data-testid="facility-new"
                onClick={() => setFilters({ ...filters, add: "1", edit: "" })}
              >
                <Icon name="plus" />
                منشأة جديدة
              </button>
            ) : null}
          </span>
        }
      />

      <div className="live-toolbar" data-testid="facilities-toolbar">
        <SearchBox
          value={filters.q}
          placeholder="ابحث باسم المنشأة"
          testId="facilities-search"
          onChange={(q) => setFilters({ ...filters, q, id: "" })}
        />
        <FilterChips
          label="الحالة"
          value={(filters.status || "") as Show}
          options={SHOW}
          testId="facilities-status"
          onChange={(status) => setFilters({ ...filters, status, id: "" })}
        />
        <select
          className="toolbar-select"
          aria-label="المحافظة"
          value={filters.province}
          onChange={(event) => setFilters({ ...filters, province: event.target.value, id: "" })}
        >
          <option value="">كل المحافظات</option>
          {(lookups.provinceFilter.options ?? []).map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
        <select
          className="toolbar-select"
          aria-label="التصنيف"
          value={filters.category}
          onChange={(event) => setFilters({ ...filters, category: event.target.value, id: "" })}
        >
          <option value="">كل التصنيفات</option>
          {(lookups.categoryFilter.options ?? []).map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
        <select
          className="toolbar-select"
          aria-label="ما ينقص البيانات"
          value={filters.issue}
          onChange={(event) => setFilters({ ...filters, issue: event.target.value, id: "" })}
        >
          <option value="">بيانات كاملة وناقصة</option>
          {Object.entries(QUALITY_ISSUES).map(([value, label]) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </select>
        <select
          className="toolbar-select"
          aria-label="الترتيب"
          value={filters.ordering}
          onChange={(event) => setFilters({ ...filters, ordering: event.target.value })}
        >
          <option value="">الأحدث تحديثاً</option>
          <option value="qualityScore">الأضعف بيانات أولاً</option>
          <option value="-qualityScore">الأكمل بيانات أولاً</option>
          <option value="updatedAt">الأقدم تحديثاً</option>
        </select>
        {/* The file holds what the filters hold, without the page's limit. */}
        <ExportButton name="facilities" params={listFilters} />
      </div>

      {linked ? (
        <p>
          <button type="button" className="button-link" onClick={() => setFilters({ ...filters, id: "" })}>
            عرض كل المنشآت
          </button>
        </p>
      ) : null}

      {facilities.loading ? <LoadingState /> : null}
      {facilities.error ? <ErrorState error={facilities.error} onRetry={facilities.reload} /> : null}
      {facilities.data && facilities.data.items.length === 0 ? (
        <EmptyState
          title="لا منشأة تطابق البحث."
          hint="جرّب اسماً آخر، أو اختر «الكل»."
          illustration="noResults"
        />
      ) : null}

      {facilities.data && facilities.data.items.length > 0 ? (
        <ul className="profile-grid" data-testid="facilities">
          {facilities.data.items.map((row, index) => (
            <FacilityCard
              key={row.id}
              row={row}
              index={index}
              focused={linked === row.id}
              canEdit={canEdit}
              canManage={canManage}
              onEdit={() => openEdit(row.id)}
              onDecide={(kind) => {
                mutation.reset();
                setReason("");
                setDecision({ kind, row });
              }}
              onHistory={() => setHistory(row)}
              onMedia={() => setMedia(row)}
              onOwner={() => setOwning(row)}
            />
          ))}
        </ul>
      ) : null}

      {facilities.pagination ? <Pagination {...facilities.pagination} /> : null}

      <FormDialog
        open={add === "1" || Boolean(edit)}
        wide
        icon={edit ? "edit" : "building"}
        title={edit ? "تعديل المنشأة" : "منشأة جديدة"}
        description={
          edit
            ? "تعديل اللوحة لا يعيد المنشأة إلى المراجعة."
            : "تظهر فوراً في الدليل أو تُحفظ مسودة، كما تختار."
        }
        onClose={closeForm}
        testId="facility-dialog"
      >
        {edit ? (
          <EditFacility
            id={edit}
            mapStyleUrl={mapStyleUrl}
            canManage={canManage}
            onSaved={() => {
              closeForm();
              setToast("حُفظت التعديلات وسُجّلت في سجل التدقيق.");
              facilities.reload();
            }}
            onCancel={closeForm}
            onClose={(row) => {
              closeForm();
              setReason("");
              mutation.reset();
              setDecision({ kind: "close", row });
            }}
          />
        ) : add === "1" ? (
          <FacilityForm
            facility={null}
            mapStyleUrl={mapStyleUrl}
            onCancel={closeForm}
            onSaved={() => {
              closeForm();
              setToast("أُضيفت المنشأة. أضف دوامها وصورها من زر الساعة على بطاقتها.");
              facilities.reload();
            }}
          />
        ) : null}
      </FormDialog>

      <FormDialog
        open={media !== null}
        wide
        icon="clock"
        title={media ? `دوام ${media.nameAr} وصورها` : ""}
        description="ما يراه الناس: متى تفتح، وكيف تبدو."
        onClose={() => {
          setMedia(null);
          facilities.reload();
        }}
        testId="facility-media"
        footer={
          <button
            type="button"
            className="button-primary"
            onClick={() => {
              setMedia(null);
              facilities.reload();
            }}
          >
            إغلاق
          </button>
        }
      >
        {media ? <FacilityMedia facilityId={media.id} canEdit={canEdit} /> : null}
      </FormDialog>

      {owning ? (
        <OwnerTransferDialog
          facility={owning}
          onDone={(message) => {
            setOwning(null);
            setToast(message);
            facilities.reload();
          }}
          onCancel={() => setOwning(null)}
        />
      ) : null}

      <FormDialog
        open={history !== null}
        icon="history"
        title={history?.nameAr ?? "السجل"}
        description="كل ما حدث لهذه المنشأة، من التسجيل حتى اليوم."
        onClose={() => setHistory(null)}
        testId="facility-history"
        footer={
          <button type="button" className="button-primary" onClick={() => setHistory(null)}>
            إغلاق
          </button>
        }
      >
        {history ? <FacilityTimeline facilityId={history.id} /> : null}
      </FormDialog>

      <ConfirmDialog
        open={decision !== null}
        title={decision ? DECISIONS[decision.kind].title : ""}
        body={decision ? `${decision.row.nameAr} — ${DECISIONS[decision.kind].body}` : undefined}
        confirmLabel={decision ? DECISIONS[decision.kind].label : ""}
        destructive={decision ? DECISIONS[decision.kind].destructive : false}
        pending={mutation.pending}
        error={mutation.error}
        onCancel={() => setDecision(null)}
        onConfirm={async () => {
          if (!decision) return;
          const meta = DECISIONS[decision.kind];
          if (meta.reason && !reason.trim()) return;
          if (await mutation.run(meta.operation, { id: decision.row.id, reason: reason.trim() })) {
            setDecision(null);
            setToast("نُفّذ القرار وسُجّل في سجل التدقيق.");
            facilities.reload();
          }
        }}
      >
        {decision && DECISIONS[decision.kind].reason ? (
          <label className="field">
            <span>السبب</span>
            <textarea
              rows={3}
              value={reason}
              data-testid="decision-reason"
              onChange={(event) => setReason(event.target.value)}
            />
          </label>
        ) : null}
      </ConfirmDialog>

      <Toast message={toast} onDismiss={() => setToast(null)} />
    </div>
  );
}

function FacilityCard({
  row,
  index,
  focused,
  canEdit,
  canManage,
  onEdit,
  onDecide,
  onHistory,
  onMedia,
  onOwner,
}: {
  row: FacilityRow;
  index: number;
  focused: boolean;
  canEdit: boolean;
  canManage: boolean;
  onEdit: () => void;
  onDecide: (kind: Decision) => void;
  onHistory: () => void;
  onMedia: () => void;
  onOwner: () => void;
}) {
  const status = STATUS[row.status];
  const kind =
    row.status === "ACTIVE" ? "owner" : row.status === "SUSPENDED" || row.status === "CLOSED" ? "blocked" : "plain";
  const icon = CATEGORY_ICONS.has(row.categoryIconKey as IconName)
    ? (row.categoryIconKey as IconName)
    : "building";
  const quality = row.qualityScore;
  const missing = row.qualityIssues.map((code) => QUALITY_ISSUES[code] ?? code);
  const place = row.cityNameAr ? `${row.cityNameAr}، ${row.provinceNameAr}` : row.provinceNameAr;

  return (
    <ProfileCard
      testId={`facility-${row.id}`}
      index={index}
      focused={focused}
      kind={kind}
      cover={row.imageUrl}
      tag={{ label: status?.label ?? row.status, icon: STATUS_ICON[row.status] ?? "info" }}
      mark={<Icon name={icon} />}
      name={row.nameAr}
      tiles={[
        { icon: "phone", value: row.phone ?? "—", label: "الهاتف", ltr: Boolean(row.phone) },
        { icon: "mapPin", tone: "gold", value: place, label: row.addressAr ?? "المكان" },
      ]}
      pills={[
        { label: row.categoryNameAr, icon: "tag" },
        {
          label: `اكتمال البيانات ${quality}%`,
          tone: quality >= 80 ? "positive" : quality >= 50 ? "warning" : "danger",
        },
      ]}
      places={{
        icon: "user",
        title: "المالك",
        subtitle: missing.length ? `ينقصها: ${missing.join("، ")}` : "بياناتها مكتملة",
        empty: "بلا مالك مسجّل — أضافتها اللوحة",
        items: row.ownerName
          ? [
              {
                key: "owner",
                label: row.ownerName,
                aside: row.ownerPhone ?? undefined,
                icon: "user",
              },
            ]
          : [],
      }}
      dates={[
        {
          icon: "calendar",
          label: "آخر تحديث",
          value: row.updatedAt ? relativeTime(row.updatedAt) : "—",
        },
        {
          icon: "mapPin",
          tone: "info",
          label: "على الخريطة",
          value: row.location ? "محدّد" : "غير محدّد",
        },
      ]}
      actions={
        <>
          {canEdit ? (
            <button type="button" className="profile-act-main" data-testid={`edit-${row.id}`} onClick={onEdit}>
              <Icon name="edit" width={16} height={16} />
              تعديل
            </button>
          ) : null}
          {canManage && row.status === "ACTIVE" ? (
            <button
              type="button"
              className="profile-act-danger"
              data-testid={`suspend-${row.id}`}
              onClick={() => onDecide("suspend")}
            >
              <Icon name="lock" width={15} height={15} />
              إيقاف
            </button>
          ) : null}
          {canManage && row.status === "SUSPENDED" ? (
            <button
              type="button"
              className="profile-act-danger"
              data-testid={`reactivate-${row.id}`}
              onClick={() => onDecide("reactivate")}
            >
              <Icon name="refresh" width={15} height={15} />
              تفعيل
            </button>
          ) : null}
          <button
            type="button"
            className="profile-act-icon"
            title="الدوام والصور"
            aria-label={`دوام ${row.nameAr} وصورها`}
            data-testid={`media-${row.id}`}
            onClick={onMedia}
          >
            <Icon name="clock" width={16} height={16} />
          </button>
          {canManage && row.status !== "CLOSED" ? (
            <button
              type="button"
              className="profile-act-icon"
              title="نقل الملكية"
              aria-label={`نقل ملكية ${row.nameAr}`}
              data-testid={`owner-${row.id}`}
              onClick={onOwner}
            >
              <Icon name="user" width={16} height={16} />
            </button>
          ) : null}
          <button
            type="button"
            className="profile-act-icon"
            title="السجل"
            aria-label={`سجل ${row.nameAr}`}
            data-testid={`history-${row.id}`}
            onClick={onHistory}
          >
            <Icon name="history" width={16} height={16} />
          </button>
          {row.location ? (
            <Link
              className="profile-act-icon profile-act-link"
              title="على الخريطة"
              aria-label={`${row.nameAr} على الخريطة`}
              href={`/facilities/map?q=${encodeURIComponent(row.nameAr)}`}
            >
              <Icon name="map" width={16} height={16} />
            </Link>
          ) : null}
        </>
      }
    />
  );
}

/** The edit window's contents: the full facility, read when the window opens. */
function EditFacility({
  id,
  mapStyleUrl,
  canManage,
  onSaved,
  onCancel,
  onClose,
}: {
  id: string;
  mapStyleUrl: string;
  canManage: boolean;
  onSaved: () => void;
  onCancel: () => void;
  onClose: (row: FacilityRow) => void;
}) {
  const facility = useResource<EditableFacility & FacilityRow>("facility", { id });
  if (facility.loading) return <LoadingState />;
  if (facility.error) return <ErrorState error={facility.error} onRetry={facility.reload} />;
  if (!facility.data) return null;
  const data = facility.data;
  return (
    <>
      {canManage && data.status !== "CLOSED" ? (
        <div className="form-danger-zone">
          <p>
            إغلاق المنشأة نهائياً يخفيها من الدليل ولا يُتراجع عنه من هنا. للإخفاء المؤقت استعمل
            «إيقاف» من بطاقتها.
          </p>
          <button
            type="button"
            className="button-ghost button-danger-quiet"
            data-testid="facility-close"
            onClick={() => onClose(data)}
          >
            إغلاق نهائي
          </button>
        </div>
      ) : null}
      <FacilityForm
        facility={data}
        mapStyleUrl={mapStyleUrl}
        onSaved={onSaved}
        onCancel={onCancel}
      />
    </>
  );
}

/** Everything that happened to one facility, newest first. */
function FacilityTimeline({ facilityId }: { facilityId: string }) {
  const timeline = useResource<{ items: TimelineEvent[] }>("facilityTimeline", { id: facilityId });
  if (timeline.loading) return <LoadingState />;
  if (timeline.error) return <ErrorState error={timeline.error} onRetry={timeline.reload} />;
  const items = timeline.data?.items ?? [];
  if (items.length === 0) return <span className="muted">لا أحداث مسجّلة بعد.</span>;
  return (
    <ol className="timeline" data-testid="facility-timeline">
      {items.map((event, index) => (
        <li key={`${event.at}-${index}`}>
          <time dateTime={event.at} className="cell-ltr">
            {formatDateTime(event.at)}
          </time>
          <StatusBadge tone={TIMELINE_TONE[event.kind] ?? "neutral"}>{event.titleAr}</StatusBadge>
          {event.actorName ? <span className="muted">بواسطة {event.actorName}</span> : null}
        </li>
      ))}
    </ol>
  );
}

/**
 * «نقل الملكية»: the facility to another account, on the word of both sides (DECISION-119).
 * Nothing could change an owner before — a sold pharmacy could only be closed.
 */
function OwnerTransferDialog({
  facility,
  onDone,
  onCancel,
}: {
  facility: FacilityRow;
  onDone: (message: string) => void;
  onCancel: () => void;
}) {
  const mutation = useMutation();
  const [phone, setPhone] = useState("");
  const [keep, setKeep] = useState(false);
  return (
    <FormDialog
      open
      icon="user"
      title={`نقل ملكية ${facility.nameAr}`}
      description="تحقق من المالك الحالي والجديد قبل النقل. يجب أن يكون للمالك الجديد حساب في التطبيق."
      onClose={onCancel}
      locked={mutation.pending}
      testId="owner-transfer"
      footer={
        <>
          <button
            type="button"
            className="button-primary"
            data-testid="owner-transfer-confirm"
            disabled={mutation.pending || !phone.trim()}
            onClick={async () => {
              const done = await mutation.runFor<{ ownerName: string }>(
                "facilityOwnerTransfer",
                { id: facility.id, phone: phone.trim(), keepPreviousAsManager: keep },
              );
              if (done) {
                onDone(`نُقلت ملكية ${facility.nameAr} إلى ${done.ownerName}، وأُبلغ الطرفان.`);
              }
            }}
          >
            {mutation.pending ? "جارٍ النقل…" : "نقل الملكية"}
          </button>
          <button
            type="button"
            className="button-ghost"
            disabled={mutation.pending}
            onClick={onCancel}
          >
            إلغاء
          </button>
        </>
      }
    >
      <label className="field">
        <span>رقم جوال المالك الجديد</span>
        <input
          type="tel"
          dir="ltr"
          inputMode="tel"
          placeholder="09XXXXXXXX"
          data-testid="owner-transfer-phone"
          value={phone}
          onChange={(event) => setPhone(event.target.value)}
        />
      </label>
      <label className="switch-row">
        <span>إبقاء المالك الحالي مديراً فيها</span>
        <input
          type="checkbox"
          data-testid="owner-transfer-keep"
          checked={keep}
          onChange={(event) => setKeep(event.target.checked)}
        />
      </label>
      {mutation.error ? <ErrorState error={mutation.error} /> : null}
    </FormDialog>
  );
}
