import "server-only";
import type { AdminApis } from "./client";

/**
 * Every backend call this application can make, named once.
 *
 * The browser asks the BFF for an operation by key; the BFF looks it up here and runs the
 * matching generated client method. That is the whole transport surface. No screen builds a
 * URL, serialises a query string, or names an endpoint — which is what `§2` requires and
 * what keeps the client regenerable without touching a single page.
 *
 * Splitting reads from writes is not cosmetic. The route handler serves reads over `GET`
 * and writes over `POST`, so an operation can only ever be reached by the method that suits
 * it, and a mutation cannot be triggered by a link, a prefetch or an image tag.
 */

type Params = Readonly<Record<string, string | undefined>>;
type Body = Readonly<Record<string, unknown>>;

/** Every registry entry answers with something JSON-serialisable, or nothing. */
export type ReadFn = (apis: AdminApis, params: Params) => Promise<unknown>;
export type WriteFn = (apis: AdminApis, body: Body) => Promise<unknown>;

/** Drop empty filter values so an untouched filter box does not become `?q=`. */
function filled(params: Params, keys: readonly string[]): Record<string, string> {
  const out: Record<string, string> = {};
  for (const key of keys) {
    const value = params[key]?.trim();
    if (value) out[key] = value;
  }
  return out;
}

export const READS = {
  me: (apis: AdminApis) => apis.system.adminMeRetrieve(),
  dashboard: (apis: AdminApis) => apis.system.adminDashboardRetrieve(),
  tasks: (apis: AdminApis) => apis.system.adminTasksRetrieve(),
  alerts: (apis: AdminApis) => apis.system.adminAlertsList(),
  globalSearch: (apis: AdminApis, p: Params) =>
    apis.system.adminSearchRetrieve({ q: (p.q ?? "").trim() }),
  systemStatus: (apis: AdminApis) => apis.system.adminSystemStatusRetrieve(),
  reviews: (apis: AdminApis, p: Params) =>
    apis.reviews.adminReviewsList(
      filled(p, ["kind", "status", "province", "category", "from", "to", "evidence", "cursor"]),
    ),
  review: (apis: AdminApis, p: Params) =>
    apis.reviews.adminReviewRetrieve({ applicationId: p.id! }),

  facilities: (apis: AdminApis, p: Params) =>
    apis.facilities.adminFacilitiesList(
      filled(p, [
        "status",
        "province",
        "city",
        "category",
        "q",
        "issue",
        "ordering",
        "cursor",
      ]) as never,
    ),
  facilityTimeline: (apis: AdminApis, p: Params) =>
    apis.facilities.adminFacilityTimelineRetrieve({ facilityId: p.id! }),
  facility: (apis: AdminApis, p: Params) =>
    apis.facilities.adminFacilityRetrieve({ facilityId: p.id! }),

  users: (apis: AdminApis, p: Params) =>
    apis.users.adminUsersList(filled(p, ["q", "status", "role", "cursor"])),
  user: (apis: AdminApis, p: Params) => apis.users.adminUserRetrieve({ userId: p.id! }),
  roles: (apis: AdminApis) => apis.users.adminRolesList(),

  categoryGroups: (apis: AdminApis) => apis.taxonomy.adminCategoryGroupsList(),
  categories: (apis: AdminApis) => apis.taxonomy.adminCategoriesList(),
  provinces: (apis: AdminApis) => apis.provinces.adminProvincesList(),
  verificationRequirements: (apis: AdminApis) =>
    apis.verification.adminVerificationRequirementsList(),
  ads: (apis: AdminApis) => apis.ads.adminAdsList(),
  settings: (apis: AdminApis) => apis.settings.adminSettingsList(),
  // What a mobile build must be. Its own entry rather than part of `settings`, because it is
  // a different endpoint with a different shape — and the one that can stop every phone.
  appRelease: (apis: AdminApis) => apis.settings.adminAppReleaseRetrieve({}),

  audit: (apis: AdminApis, p: Params) =>
    apis.audit.adminAuditList(
      filled(p, ["actor", "action", "resource", "requestId", "from", "to", "cursor"]),
    ),

  reports: (apis: AdminApis, p: Params) =>
    apis.reports.adminReportsList(filled(p, ["status", "facility", "cursor"])),
  provinceCities: (apis: AdminApis, p: Params) =>
    apis.provinces.adminProvinceCitiesList({ provinceId: p.id! }),

  // Operations screens
  analyticsPeriod: (apis: AdminApis, p: Params) =>
    apis.analytics.adminAnalyticsRetrieve(filled(p, ["from", "to"])),
  staffPerformance: (apis: AdminApis, p: Params) =>
    apis.analytics.adminAnalyticsStaffRetrieve(filled(p, ["from", "to"])),
  provinceReadiness: (apis: AdminApis, p: Params) =>
    apis.provinces.adminProvinceReadinessRetrieve({ provinceId: p.id ?? "" }),
  dutyRoster: (apis: AdminApis, p: Params) =>
    apis.duty.adminDutyRosterRetrieve({
      // Required upstream; an absent one is sent empty so Django answers with its own 400.
      provinceId: p.provinceId?.trim() ?? "",
      ...filled(p, ["cityId", "from", "to"]),
    }),
  contentPages: (apis: AdminApis) => apis.content.adminContentPagesList(),
  contentPage: (apis: AdminApis, p: Params) =>
    apis.content.adminContentPageRetrieve({ slug: p.slug ?? "" }),
  faqEntries: (apis: AdminApis) => apis.content.adminFaqEntriesList(),
  emergencyNumbers: (apis: AdminApis, p: Params) =>
    apis.content.adminEmergencyNumbersList(filled(p, ["provinceId"])),
  contactMessages: (apis: AdminApis, p: Params) =>
    apis.content.adminContactMessagesList(filled(p, ["status", "kind", "cursor"])),
  broadcasts: (apis: AdminApis, p: Params) =>
    apis.notifications.adminNotificationBroadcastsList(filled(p, ["cursor"])),
  rejectionTemplates: (apis: AdminApis, p: Params) =>
    apis.reviews.adminRejectionTemplatesList(p.active === "true" ? { active: true } : {}),

  // Specialties and services, one category at a time
  categorySpecialties: (apis: AdminApis, p: Params) =>
    apis.taxonomy.adminCategorySpecialtiesList({ categoryId: p.id ?? "" }),
  categoryServiceTags: (apis: AdminApis, p: Params) =>
    apis.taxonomy.adminCategoryServiceTagsList({ categoryId: p.id ?? "" }),
} as const satisfies Record<string, ReadFn>;

/**
 * Turn the schedule's ISO strings into `Date` objects for the generated client.
 *
 * The generated serialisers call `.toISOString()` on date-time fields, so a JSON string
 * from the browser would throw inside the client and surface as a 502. Only keys that are
 * present are touched: an absent key must stay absent, because an update is partial, and an
 * explicit `null` clears the schedule on purpose.
 */
function withDates(body: Body, keys: readonly string[]): Record<string, unknown> {
  const out: Record<string, unknown> = { ...body };
  for (const key of keys) {
    if (!(key in body)) continue;
    const value = body[key];
    if (value === null || value === undefined || value === "") {
      out[key] = null;
      continue;
    }
    const parsed = new Date(String(value));
    out[key] = Number.isNaN(parsed.getTime()) ? value : parsed;
  }
  return out;
}

const SCHEDULE = ["startsAt", "endsAt"] as const;

export const WRITES = {
  reviewApprove: (apis: AdminApis, b: Body) =>
    apis.reviews.adminReviewApprove({
      applicationId: String(b.id),
      adminDecisionRequest: { reason: String(b.reason ?? "") },
    }),
  reviewReject: (apis: AdminApis, b: Body) =>
    apis.reviews.adminReviewReject({
      applicationId: String(b.id),
      adminDecisionRequest: { reason: String(b.reason ?? "") },
    }),

  // The record an operator adds or corrects. `location` arrives as `{latitude, longitude}` or
  // null, and the id of an update travels beside the fields rather than among them.
  facilityCreate: (apis: AdminApis, b: Body) =>
    apis.facilities.adminFacilityCreate({ adminFacilityCreate: b as never }),
  facilityUpdate: (apis: AdminApis, b: Body) => {
    const { id, ...fields } = b;
    return apis.facilities.adminFacilityUpdate({
      facilityId: String(id),
      patchedAdminFacilityWrite: fields as never,
    });
  },

  facilitySuspend: (apis: AdminApis, b: Body) =>
    apis.facilities.adminFacilitySuspend({
      facilityId: String(b.id),
      adminDecisionRequest: { reason: String(b.reason ?? "") },
    }),
  facilityReactivate: (apis: AdminApis, b: Body) =>
    apis.facilities.adminFacilityReactivate({
      facilityId: String(b.id),
      adminDecisionRequest: { reason: String(b.reason ?? "") },
    }),
  facilityClose: (apis: AdminApis, b: Body) =>
    apis.facilities.adminFacilityClose({
      facilityId: String(b.id),
      adminDecisionRequest: { reason: String(b.reason ?? "") },
    }),

  userBlock: (apis: AdminApis, b: Body) => apis.users.adminUserBlock({ userId: String(b.id) }),
  userUnblock: (apis: AdminApis, b: Body) =>
    apis.users.adminUserUnblock({ userId: String(b.id) }),
  userRoles: (apis: AdminApis, b: Body) =>
    apis.users.adminUserRolesReplace({
      userId: String(b.id),
      // `AdminRole` is keyed by an integer; the screen holds the ids as checkbox strings.
      adminUserRolesRequest: { roleIds: ((b.roleIds as unknown[] | undefined) ?? []).map(Number) },
    }),

  categoryGroupCreate: (apis: AdminApis, b: Body) =>
    apis.taxonomy.adminCategoryGroupCreate({ adminCategoryGroupRequest: b }),
  categoryGroupUpdate: (apis: AdminApis, b: Body) =>
    apis.taxonomy.adminCategoryGroupUpdate({
      groupId: String(b.id),
      adminCategoryGroupRequest: b,
    }),
  categoryCreate: (apis: AdminApis, b: Body) =>
    apis.taxonomy.adminCategoryCreate({ adminCategoryCreateRequest: b as never }),
  categoryUpdate: (apis: AdminApis, b: Body) =>
    apis.taxonomy.adminCategoryUpdate({
      categoryId: String(b.id),
      adminCategoryUpdateRequest: b,
    }),
  categoryCapabilities: (apis: AdminApis, b: Body) =>
    apis.taxonomy.adminCategoryCapabilitiesReplace({
      categoryId: String(b.id),
      adminCapabilitiesRequest: b,
    }),
  categoryProvince: (apis: AdminApis, b: Body) =>
    apis.taxonomy.adminCategoryProvinceReplace({
      categoryId: String(b.id),
      adminCategoryProvinceRequest: b as never,
    }),

  provinceUpdate: (apis: AdminApis, b: Body) =>
    apis.provinces.adminProvinceUpdate({
      provinceId: String(b.id),
      adminProvinceUpdateRequest: b,
    }),

  verificationCreate: (apis: AdminApis, b: Body) =>
    apis.verification.adminVerificationRequirementCreate({
      adminVerificationRequirementRequest: b as never,
    }),
  verificationUpdate: (apis: AdminApis, b: Body) =>
    apis.verification.adminVerificationRequirementUpdate({
      requirementId: Number(b.id),
      adminVerificationRequirementUpdateRequest: b,
    }),

  adCreate: (apis: AdminApis, b: Body) =>
    apis.ads.adminAdCreate({ adminAdvertisementRequest: withDates(b, SCHEDULE) as never }),
  adUpdate: (apis: AdminApis, b: Body) =>
    apis.ads.adminAdUpdate({
      advertisementId: String(b.id),
      adminAdvertisementUpdateRequest: withDates(b, SCHEDULE),
    }),
  adDelete: (apis: AdminApis, b: Body) =>
    apis.ads.adminAdDelete({ advertisementId: String(b.id) }),

  reportResolve: (apis: AdminApis, b: Body) =>
    apis.reports.adminReportResolve({
      reportId: String(b.id),
      adminReportDecisionRequest: { note: String(b.note ?? "") },
    }),
  reportDismiss: (apis: AdminApis, b: Body) =>
    apis.reports.adminReportDismiss({
      reportId: String(b.id),
      adminReportDecisionRequest: { note: String(b.note ?? "") },
    }),
  // Up to a hundred at once, in one transaction. The response says what happened to each id,
  // because some may have been decided by somebody else between the list and the button.
  appReleaseUpdate: (apis: AdminApis, b: Body) =>
    apis.settings.adminAppReleaseUpdate({
      adminAppReleaseRequest: {
        minimumVersionCode: Number(b.minimumVersionCode ?? 0),
        latestVersionCode: Number(b.latestVersionCode ?? 0),
        storeUrl: String(b.storeUrl ?? ""),
        noticeAr: String(b.noticeAr ?? ""),
      },
    }),
  reportsBulkDecide: (apis: AdminApis, b: Body) =>
    apis.reports.adminReportsBulkDecide({
      adminReportBulkRequest: {
        ids: (b.ids as string[]) ?? [],
        action: b.action as "resolve" | "dismiss",
        note: String(b.note ?? ""),
      },
    }),
  cityUpdate: (apis: AdminApis, b: Body) =>
    apis.provinces.adminProvinceCityUpdate({
      provinceId: String(b.provinceId),
      cityId: String(b.id),
      adminCityUpdateRequest: { active: Boolean(b.active) },
    }),

  settingWrite: (apis: AdminApis, b: Body) =>
    apis.settings.adminSettingWrite({ adminSettingWriteRequest: b as never }),

  // Operations screens
  dutyShiftCreate: (apis: AdminApis, b: Body) =>
    apis.duty.adminDutyShiftCreate({
      adminDutyShiftCreateRequest: withDates(
        { facilityId: String(b.facilityId ?? ""), ...sent(b, SCHEDULE) },
        SCHEDULE,
      ) as never,
    }),
  dutyShiftUpdate: (apis: AdminApis, b: Body) =>
    apis.duty.adminDutyShiftUpdate({
      shiftId: String(b.id),
      patchedAdminDutyShiftUpdateRequest: withDates(sent(b, SCHEDULE), SCHEDULE),
    }),
  dutyShiftDelete: (apis: AdminApis, b: Body) =>
    apis.duty.adminDutyShiftDelete({ shiftId: String(b.id) }),

  contentPageCreate: (apis: AdminApis, b: Body) =>
    apis.content.adminContentPageCreate({
      adminContentPageCreateRequest: sent(b, [
        "slug",
        "kind",
        "titleAr",
        "bodyAr",
        "published",
      ]) as never,
    }),
  contentPageUpdate: (apis: AdminApis, b: Body) =>
    apis.content.adminContentPageUpdate({
      slug: String(b.slug ?? ""),
      adminContentPageUpdateRequest: sent(b, ["kind", "titleAr", "bodyAr", "published"]),
    }),
  contentPageDelete: (apis: AdminApis, b: Body) =>
    apis.content.adminContentPageDelete({ slug: String(b.slug ?? "") }),

  faqCreate: (apis: AdminApis, b: Body) =>
    apis.content.adminFaqEntryCreate({ adminFaqEntryRequest: sent(b, FAQ_FIELDS) as never }),
  faqUpdate: (apis: AdminApis, b: Body) =>
    apis.content.adminFaqEntryUpdate({
      entryId: String(b.id),
      adminFaqEntryRequest: sent(b, FAQ_FIELDS) as never,
    }),
  faqDelete: (apis: AdminApis, b: Body) =>
    apis.content.adminFaqEntryDelete({ entryId: String(b.id) }),

  emergencyNumberCreate: (apis: AdminApis, b: Body) =>
    apis.content.adminEmergencyNumberCreate({
      adminEmergencyNumberRequest: sent(b, NUMBER_FIELDS) as never,
    }),
  emergencyNumberUpdate: (apis: AdminApis, b: Body) =>
    apis.content.adminEmergencyNumberUpdate({
      numberId: String(b.id),
      adminEmergencyNumberRequest: sent(b, NUMBER_FIELDS) as never,
    }),
  emergencyNumberDelete: (apis: AdminApis, b: Body) =>
    apis.content.adminEmergencyNumberDelete({ numberId: String(b.id) }),

  contactMessageHandle: (apis: AdminApis, b: Body) =>
    apis.content.adminContactMessageHandle({
      messageId: String(b.id),
      adminContactHandleRequest: { note: String(b.note ?? "") },
    }),

  broadcastSend: (apis: AdminApis, b: Body) =>
    apis.notifications.adminNotificationBroadcast({
      adminBroadcastRequest: {
        titleAr: String(b.titleAr ?? ""),
        bodyAr: String(b.bodyAr ?? ""),
        // Passed through untouched: an unknown audience is refused upstream, never widened
        // here to "everyone".
        audience: String(b.audience ?? "") as never,
        provinceId: b.provinceId ? String(b.provinceId) : null,
      },
    }),

  rejectionTemplateCreate: (apis: AdminApis, b: Body) =>
    apis.reviews.adminRejectionTemplateCreate({
      adminRejectionTemplateRequest: sent(b, TEMPLATE_FIELDS) as never,
    }),
  rejectionTemplateUpdate: (apis: AdminApis, b: Body) =>
    apis.reviews.adminRejectionTemplateUpdate({
      templateId: String(b.id),
      adminRejectionTemplateRequest: sent(b, TEMPLATE_FIELDS) as never,
    }),
  rejectionTemplateDelete: (apis: AdminApis, b: Body) =>
    apis.reviews.adminRejectionTemplateDelete({ templateId: String(b.id) }),

  // Specialties and services. Their keys are integers; the category travels as `categoryId`.
  specialtyCreate: (apis: AdminApis, b: Body) =>
    apis.taxonomy.adminCategorySpecialtyCreate({
      categoryId: String(b.categoryId ?? ""),
      adminSpecialtyCreateRequest: sent(b, [...TAG_FIELDS, "scope"]) as never,
    }),
  specialtyUpdate: (apis: AdminApis, b: Body) =>
    apis.taxonomy.adminSpecialtyUpdate({
      specialtyId: Number(b.id),
      adminTagUpdateRequest: sent(b, TAG_FIELDS),
    }),
  specialtyDelete: (apis: AdminApis, b: Body) =>
    apis.taxonomy.adminSpecialtyDelete({ specialtyId: Number(b.id) }),
  serviceTagCreate: (apis: AdminApis, b: Body) =>
    apis.taxonomy.adminCategoryServiceTagCreate({
      categoryId: String(b.categoryId ?? ""),
      adminServiceTagCreateRequest: sent(b, TAG_FIELDS) as never,
    }),
  serviceTagUpdate: (apis: AdminApis, b: Body) =>
    apis.taxonomy.adminServiceTagUpdate({
      serviceTagId: Number(b.id),
      adminTagUpdateRequest: sent(b, TAG_FIELDS),
    }),
  serviceTagDelete: (apis: AdminApis, b: Body) =>
    apis.taxonomy.adminServiceTagDelete({ serviceTagId: Number(b.id) }),
} as const satisfies Record<string, WriteFn>;

export type ReadOperation = keyof typeof READS;
export type WriteOperation = keyof typeof WRITES;

export function isReadOperation(name: string): name is ReadOperation {
  return Object.hasOwn(READS, name);
}

export function isWriteOperation(name: string): name is WriteOperation {
  return Object.hasOwn(WRITES, name);
}

// Operations screens
//
// Declared after the registries on purpose, so the entries above stay an append-only block;
// they are only read when an operation runs, long after this module has finished loading.

/**
 * Copy only the keys the browser actually sent.
 *
 * The content, FAQ, emergency-number and template updates are partial upstream ("omitted
 * fields keep their value"), so a key the screen did not send must stay absent rather than
 * arrive as `undefined` or a default that would overwrite what is stored.
 */
function sent(body: Body, keys: readonly string[]): Record<string, unknown> {
  const out: Record<string, unknown> = {};
  for (const key of keys) {
    if (Object.hasOwn(body, key)) out[key] = body[key];
  }
  return out;
}

const FAQ_FIELDS = ["questionAr", "answerAr", "sortOrder", "published"] as const;
const NUMBER_FIELDS = [
  "provinceId",
  "labelAr",
  "phone",
  "kind",
  "sortOrder",
  "active",
  "adminNote",
] as const;
const TEMPLATE_FIELDS = ["titleAr", "bodyAr", "active", "sortOrder"] as const;
/** What a specialty or a service carries besides its scope, which is set once on creation. */
const TAG_FIELDS = ["nameAr", "nameEn", "active", "sortOrder"] as const;
