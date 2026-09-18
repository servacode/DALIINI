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
  systemStatus: (apis: AdminApis) => apis.system.adminSystemStatusRetrieve(),
  analytics: (apis: AdminApis) => apis.analytics.adminAnalyticsRetrieve(),

  reviews: (apis: AdminApis, p: Params) =>
    apis.reviews.adminReviewsList(filled(p, ["kind", "status", "province", "category"])),
  review: (apis: AdminApis, p: Params) =>
    apis.reviews.adminReviewRetrieve({ applicationId: p.id! }),

  facilities: (apis: AdminApis, p: Params) =>
    apis.facilities.adminFacilitiesList(filled(p, ["status", "province", "category", "q"])),
  facility: (apis: AdminApis, p: Params) =>
    apis.facilities.adminFacilityRetrieve({ facilityId: p.id! }),

  users: (apis: AdminApis, p: Params) =>
    apis.users.adminUsersList(filled(p, ["q", "status"])),
  user: (apis: AdminApis, p: Params) => apis.users.adminUserRetrieve({ userId: p.id! }),
  roles: (apis: AdminApis) => apis.users.adminRolesList(),

  categoryGroups: (apis: AdminApis) => apis.taxonomy.adminCategoryGroupsList(),
  categories: (apis: AdminApis) => apis.taxonomy.adminCategoriesList(),
  provinces: (apis: AdminApis) => apis.provinces.adminProvincesList(),
  verificationRequirements: (apis: AdminApis) =>
    apis.verification.adminVerificationRequirementsList(),
  ads: (apis: AdminApis) => apis.ads.adminAdsList(),
  settings: (apis: AdminApis) => apis.settings.adminSettingsList(),

  audit: (apis: AdminApis, p: Params) =>
    apis.audit.adminAuditList(filled(p, ["actor", "action", "resource", "requestId"])),
} as const satisfies Record<string, ReadFn>;

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
      adminUserRolesRequest: { roleIds: (b.roleIds as string[]) ?? [] },
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
    apis.ads.adminAdCreate({ adminAdvertisementRequest: b as never }),
  adUpdate: (apis: AdminApis, b: Body) =>
    apis.ads.adminAdUpdate({
      advertisementId: String(b.id),
      adminAdvertisementUpdateRequest: b,
    }),
  adDelete: (apis: AdminApis, b: Body) =>
    apis.ads.adminAdDelete({ advertisementId: String(b.id) }),

  settingWrite: (apis: AdminApis, b: Body) =>
    apis.settings.adminSettingWrite({ adminSettingWriteRequest: b as never }),
} as const satisfies Record<string, WriteFn>;

export type ReadOperation = keyof typeof READS;
export type WriteOperation = keyof typeof WRITES;

export function isReadOperation(name: string): name is ReadOperation {
  return Object.hasOwn(READS, name);
}

export function isWriteOperation(name: string): name is WriteOperation {
  return Object.hasOwn(WRITES, name);
}
