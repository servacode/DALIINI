from django.core.exceptions import ValidationError as DjangoValidationError
from django.db import connection
from django.db.models import Avg, Count, Q
from django.http import FileResponse
from django.utils import timezone
from drf_spectacular.types import OpenApiTypes
from drf_spectacular.utils import OpenApiResponse, extend_schema, extend_schema_view
from rest_framework.exceptions import ValidationError
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from accounts.models import AdminRole, User
from analytics.models import ProductAnalyticsEvent
from audit.models import AuditEvent
from audit.services import record_audit
from content_services.models import Advertisement
from content_services.services import delete_advertisement, save_advertisement
from core.openapi import NOT_FOUND_404, VALIDATION_400, protected
from directory.models import (
    Category,
    CategoryCapabilities,
    CategoryGroup,
    CategoryProvince,
    VerificationRequirement,
)
from facilities.models import Facility, FacilityApplication, VerificationEvidence
from locations.models import Province
from platform_settings.models import PlatformSetting
from storage.backends import PrivateS3Storage

from .permissions import HasAdminPermission
from .schemas import (
    AdminAdvertisementListSerializer,
    AdminAdvertisementRequestSerializer,
    AdminAnalyticsSerializer,
    AdminAuditListSerializer,
    AdminCapabilitiesRequestSerializer,
    AdminCapabilitiesSerializer,
    AdminCategoryGroupListSerializer,
    AdminCategoryListSerializer,
    AdminCategoryProvinceRequestSerializer,
    AdminDashboardSerializer,
    AdminDecisionRequestSerializer,
    AdminFacilityListSerializer,
    AdminFacilitySerializer,
    AdminIdSerializer,
    AdminProvinceListSerializer,
    AdminProvinceUpdatedSerializer,
    AdminProvinceUpdateRequestSerializer,
    AdminRoleListSerializer,
    AdminSettingListSerializer,
    AdminSettingWriteRequestSerializer,
    AdminSettingWrittenSerializer,
    AdminSystemStatusSerializer,
    AdminUserDetailSerializer,
    AdminUserListSerializer,
    AdminUserRolesRequestSerializer,
    AdminUserSerializer,
    AdminVerificationRequirementListSerializer,
    AdminVerificationRequirementRequestSerializer,
)
from .schemas import AdminApplicationDetailSerializer as AppDetail
from .schemas import AdminApplicationListSerializer as AppList
from .schemas import AdminApplicationSerializer as App
from .serializers import application_payload, facility_payload, user_payload
from .services import decide_application, replace_user_roles, set_user_blocked, transition_facility


def _request_id(request):
    return getattr(request, "request_id", "")


def _validation_error(exc):
    if hasattr(exc, "message_dict"):
        return ValidationError(exc.message_dict)
    return ValidationError(getattr(exc, "messages", [str(exc)]))


class AdminView(APIView):
    permission_classes = [IsAuthenticated, HasAdminPermission]


class DashboardView(AdminView):
    required_permission = "admin.dashboard.read"

    @extend_schema(
        operation_id="adminDashboardRetrieve",
        tags=["Admin System"],
        summary="Operational counters for the review desk",
        responses={200: AdminDashboardSerializer, **protected()},
    )
    def get(self, request):
        return Response(
            {
                "pendingReviews": FacilityApplication.objects.filter(
                    status=FacilityApplication.Status.SUBMITTED
                ).count(),
                "reverification": Facility.objects.filter(
                    status=Facility.Status.REVERIFICATION_REQUIRED
                ).count(),
                "facilitiesByStatus": list(
                    Facility.objects.values("status").annotate(count=Count("id"))
                ),
                "activeUsers": User.objects.filter(is_active=True).count(),
                "recentActions": list(
                    AuditEvent.objects.order_by("-created_at")
                    .values("action", "target_type", "target_id", "created_at")[:10]
                ),
            }
        )


class ApplicationListView(AdminView):
    required_permission = "admin.reviews.read"

    @extend_schema(
        operation_id="adminReviewsList",
        tags=["Admin Reviews"],
        summary="List facility applications awaiting or past review",
        description="Capped at 200 rows.",
        responses={200: AppList, **protected()},
    )
    def get(self, request):
        qs = FacilityApplication.objects.select_related("facility").order_by("-submitted_at")
        for field, param in (("kind", "kind"), ("status", "status")):
            if value := request.query_params.get(param):
                qs = qs.filter(**{field: value})
        if value := request.query_params.get("province"):
            qs = qs.filter(facility__province_id=value)
        if value := request.query_params.get("category"):
            qs = qs.filter(facility__category_id=value)
        return Response({"items": [application_payload(item) for item in qs[:200]]})


class ApplicationDetailView(AdminView):
    required_permission = "admin.reviews.read"

    @extend_schema(
        operation_id="adminReviewRetrieve",
        tags=["Admin Reviews"],
        summary="Retrieve one application with its review context",
        description=(
            "Evidence is referenced by identifier only; content is fetched separately "
            "through the audited evidence endpoint."
        ),
        responses={200: AppDetail, **protected(), 404: NOT_FOUND_404},
    )
    def get(self, request, application_id):
        item = FacilityApplication.objects.select_related(
            "facility__category", "facility__province"
        ).get(pk=application_id)
        facility = item.facility
        evidence = facility.evidence.select_related("requirement").all()
        return Response(
            {
                **application_payload(item),
                "facility": facility_payload(facility),
                "snapshot": item.snapshot,
                "publicImageIds": [
                    str(value)
                    for value in facility.images.values_list("id", flat=True)
                ],
                "evidence": [
                    {
                        "id": str(row.id),
                        "requirementId": str(row.requirement_id),
                        "labelAr": row.requirement.label_ar,
                    }
                    for row in evidence
                ],
                "audit": list(
                    AuditEvent.objects.filter(target_id=str(item.id))
                    .order_by("-created_at")
                    .values("action", "request_id", "created_at")[:50]
                ),
            }
        )


class ApplicationDecisionView(AdminView):
    required_permission = "admin.reviews.decide"
    approve = False

    @extend_schema(
        operation_id="adminReviewDecide",
        tags=["Admin Reviews"],
        summary="Decide an application",
        request=AdminDecisionRequestSerializer,
        responses={200: App, 400: VALIDATION_400, **protected(), 404: NOT_FOUND_404},
    )
    def post(self, request, application_id):
        try:
            item = decide_application(
                request=request,
                application_id=application_id,
                approve=self.approve,
                reason=str(request.data.get("reason", "")),
            )
        except DjangoValidationError as exc:
            raise _validation_error(exc) from exc
        return Response(application_payload(item))


@extend_schema_view(
    post=extend_schema(
        operation_id="adminReviewApprove",
        tags=["Admin Reviews"],
        summary="Approve an application",
        description=(
            "Runs in one transaction: the application and the facility lifecycle are "
            "locked, the current requirements are re-checked, the change is audited and "
            "the realtime event is emitted only after commit."
        ),
        request=AdminDecisionRequestSerializer,
        responses={200: App, 400: VALIDATION_400, **protected(), 404: NOT_FOUND_404},
    )
)
class ApplicationApproveView(ApplicationDecisionView):
    approve = True


@extend_schema_view(
    post=extend_schema(
        operation_id="adminReviewReject",
        tags=["Admin Reviews"],
        summary="Reject an application",
        description="A reason is recorded in the audit trail; nothing is silently deleted.",
        request=AdminDecisionRequestSerializer,
        responses={200: App, 400: VALIDATION_400, **protected(), 404: NOT_FOUND_404},
    )
)
class ApplicationRejectView(ApplicationDecisionView):
    approve = False


class EvidenceContentView(AdminView):
    required_permission = "admin.evidence.read"

    @extend_schema(
        operation_id="adminEvidenceContentRetrieve",
        tags=["Admin Reviews"],
        summary="Stream one piece of private verification evidence",
        responses={
            200: OpenApiResponse(
                response=OpenApiTypes.BINARY,
                description=(
                    "Evidence bytes. Served with Cache-Control private, no-store and "
                    "X-Content-Type-Options nosniff, and every access is audited."
                ),
            ),
            **protected(),
            404: NOT_FOUND_404,
        },
    )
    def get(self, request, evidence_id):
        evidence = VerificationEvidence.objects.get(pk=evidence_id)
        record_audit(
            actor=request.user,
            action="verification_evidence.viewed",
            target=evidence,
            metadata={"facilityId": str(evidence.facility_id)},
            request_id=_request_id(request),
        )
        stream = PrivateS3Storage().open(evidence.storage_key, "rb")
        response = FileResponse(stream, content_type="application/octet-stream")
        response["Cache-Control"] = "private, no-store"
        response["X-Content-Type-Options"] = "nosniff"
        return response


class FacilityListView(AdminView):
    required_permission = "admin.facilities.read"

    @extend_schema(
        operation_id="adminFacilitiesList",
        tags=["Admin Facilities"],
        summary="List facilities for operations",
        description="Capped at 250 rows.",
        responses={200: AdminFacilityListSerializer, **protected()},
    )
    def get(self, request):
        qs = Facility.objects.select_related("category", "province").order_by("-updated_at")
        if value := request.query_params.get("status"):
            qs = qs.filter(status=value)
        if value := request.query_params.get("province"):
            qs = qs.filter(province_id=value)
        if value := request.query_params.get("category"):
            qs = qs.filter(category_id=value)
        if value := request.query_params.get("q"):
            qs = qs.filter(Q(name_ar__icontains=value) | Q(name_en__icontains=value))
        return Response({"items": [facility_payload(item) for item in qs[:250]]})


class FacilityDetailView(AdminView):
    required_permission = "admin.facilities.read"

    @extend_schema(
        operation_id="adminFacilityRetrieve",
        tags=["Admin Facilities"],
        summary="Retrieve one facility",
        responses={200: AdminFacilitySerializer, **protected(), 404: NOT_FOUND_404},
    )
    def get(self, request, facility_id):
        return Response(facility_payload(Facility.objects.get(pk=facility_id)))


class FacilityTransitionView(AdminView):
    required_permission = "admin.facilities.manage"
    target_status = ""

    @extend_schema(
        operation_id="adminFacilityTransition",
        tags=["Admin Facilities"],
        summary="Move a facility to a lifecycle state",
        request=AdminDecisionRequestSerializer,
        responses={
            200: AdminFacilitySerializer,
            400: VALIDATION_400,
            **protected(),
            404: NOT_FOUND_404,
        },
    )
    def post(self, request, facility_id):
        try:
            facility = transition_facility(
                request=request,
                facility_id=facility_id,
                target_status=self.target_status,
                reason=str(request.data.get("reason", "")),
            )
        except DjangoValidationError as exc:
            raise _validation_error(exc) from exc
        return Response(facility_payload(facility))


@extend_schema_view(
    post=extend_schema(
        operation_id="adminFacilitySuspend",
        tags=["Admin Facilities"],
        summary="Suspend a facility",
        description="A suspended facility leaves public discovery and cannot self-reactivate.",
        request=AdminDecisionRequestSerializer,
        responses={
            200: AdminFacilitySerializer,
            400: VALIDATION_400,
            **protected(),
            404: NOT_FOUND_404,
        },
    )
)
class FacilitySuspendView(FacilityTransitionView):
    target_status = Facility.Status.SUSPENDED


@extend_schema_view(
    post=extend_schema(
        operation_id="adminFacilityReactivate",
        tags=["Admin Facilities"],
        summary="Reactivate a suspended facility",
        request=AdminDecisionRequestSerializer,
        responses={
            200: AdminFacilitySerializer,
            400: VALIDATION_400,
            **protected(),
            404: NOT_FOUND_404,
        },
    )
)
class FacilityReactivateView(FacilityTransitionView):
    target_status = Facility.Status.ACTIVE


@extend_schema_view(
    post=extend_schema(
        operation_id="adminFacilityClose",
        tags=["Admin Facilities"],
        summary="Close a facility",
        request=AdminDecisionRequestSerializer,
        responses={
            200: AdminFacilitySerializer,
            400: VALIDATION_400,
            **protected(),
            404: NOT_FOUND_404,
        },
    )
)
class FacilityCloseView(FacilityTransitionView):
    target_status = Facility.Status.CLOSED


class UserListView(AdminView):
    required_permission = "admin.users.read"

    @extend_schema(
        operation_id="adminUsersList",
        tags=["Admin Users"],
        summary="Search user accounts",
        description="Password hashes and session secret material are never returned.",
        responses={200: AdminUserListSerializer, **protected()},
    )
    def get(self, request):
        qs = User.objects.order_by("-created_at")
        if value := request.query_params.get("q"):
            qs = qs.filter(Q(name__icontains=value) | Q(phone__icontains=value))
        if value := request.query_params.get("status"):
            qs = qs.filter(is_active=value.lower() == "active")
        return Response({"items": [user_payload(item) for item in qs[:250]]})


class UserDetailView(AdminView):
    required_permission = "admin.users.read"

    @extend_schema(
        operation_id="adminUserRetrieve",
        tags=["Admin Users"],
        summary="Retrieve one user with the roles assigned",
        responses={200: AdminUserDetailSerializer, **protected(), 404: NOT_FOUND_404},
    )
    def get(self, request, user_id):
        user = User.objects.get(pk=user_id)
        payload = user_payload(user)
        payload["roleIds"] = [
            str(value)
            for value in user.admin_role_links.filter(active=True).values_list("role_id", flat=True)
        ]
        return Response(payload)


class UserBlockView(AdminView):
    required_permission = "admin.users.manage"
    blocked = True

    @extend_schema(
        operation_id="adminUserBlock",
        tags=["Admin Users"],
        summary="Block a user account",
        description="Blocking also revokes every active refresh session of that user.",
        request=None,
        responses={200: AdminUserSerializer, **protected(), 404: NOT_FOUND_404},
    )
    def post(self, request, user_id):
        user = User.objects.get(pk=user_id)
        updated = set_user_blocked(
            request=request,
            user=user,
            blocked=self.blocked,
        )
        return Response(user_payload(updated))


@extend_schema_view(
    post=extend_schema(
        operation_id="adminUserUnblock",
        tags=["Admin Users"],
        summary="Unblock a user account",
        request=None,
        responses={200: AdminUserSerializer, **protected(), 404: NOT_FOUND_404},
    )
)
class UserUnblockView(UserBlockView):
    blocked = False


class RoleListView(AdminView):
    required_permission = "admin.roles.read"

    @extend_schema(
        operation_id="adminRolesList",
        tags=["Admin Users"],
        summary="List admin roles and their permission codes",
        responses={200: AdminRoleListSerializer, **protected()},
    )
    def get(self, request):
        items = AdminRole.objects.prefetch_related("permissions").order_by("name")
        return Response(
            {
                "items": [
                    {
                        "id": str(role.id),
                        "code": role.code,
                        "name": role.name,
                        "permissions": list(role.permissions.values_list("code", flat=True)),
                    }
                    for role in items
                ]
            }
        )


class UserRolesView(AdminView):
    required_permission = "admin.roles.manage"

    @extend_schema(
        operation_id="adminUserRolesReplace",
        tags=["Admin Users"],
        summary="Replace the admin roles of a user",
        description=(
            "Authorization is always re-checked server-side; the Admin UI only hides "
            "actions as a convenience."
        ),
        request=AdminUserRolesRequestSerializer,
        responses={204: None, 400: VALIDATION_400, **protected(), 404: NOT_FOUND_404},
    )
    def put(self, request, user_id):
        role_ids = request.data.get("roleIds", [])
        if not isinstance(role_ids, list):
            raise ValidationError({"roleIds": "Must be a list."})
        if AdminRole.objects.filter(id__in=role_ids).count() != len(set(role_ids)):
            raise ValidationError({"roleIds": "Unknown role."})
        user = User.objects.get(pk=user_id)
        replace_user_roles(request=request, user=user, role_ids=role_ids)
        return Response(status=204)


class TaxonomyView(AdminView):
    required_permission = "admin.taxonomy.read"
    model = None

    @extend_schema(
        operation_id="adminTaxonomyList",
        tags=["Admin Taxonomy"],
        summary="List taxonomy rows",
        responses={200: AdminCategoryListSerializer, **protected()},
    )
    def get(self, request):
        if self.model is CategoryGroup:
            items = self.model.objects.order_by("sort_order", "name_ar").values(
                "id", "code", "name_ar", "name_en", "active", "sort_order"
            )
        else:
            items = self.model.objects.order_by("sort_order", "name_ar").values(
                "id",
                "group_id",
                "code",
                "slug",
                "name_ar",
                "name_en",
                "icon_key",
                "specialization",
                "active",
                "sort_order",
            )
        return Response({"items": list(items)})


@extend_schema_view(
    get=extend_schema(
        operation_id="adminCategoryGroupsList",
        tags=["Admin Taxonomy"],
        summary="List category groups",
        responses={200: AdminCategoryGroupListSerializer, **protected()},
    )
)
class CategoryGroupListView(TaxonomyView):
    model = CategoryGroup


@extend_schema_view(
    get=extend_schema(
        operation_id="adminCategoriesList",
        tags=["Admin Taxonomy"],
        summary="List categories",
        responses={200: AdminCategoryListSerializer, **protected()},
    )
)
class CategoryListView(TaxonomyView):
    model = Category


class CategoryCapabilitiesView(AdminView):
    required_permission = "admin.taxonomy.manage"

    @extend_schema(
        operation_id="adminCategoryCapabilitiesReplace",
        tags=["Admin Taxonomy"],
        summary="Set the capability flags of a category",
        description="Duty can only be enabled for an approved specialization.",
        request=AdminCapabilitiesRequestSerializer,
        responses={
            200: AdminCapabilitiesSerializer,
            400: VALIDATION_400,
            **protected(),
            404: NOT_FOUND_404,
        },
    )
    def put(self, request, category_id):
        category = Category.objects.get(pk=category_id)
        capabilities, _ = CategoryCapabilities.objects.get_or_create(category=category)
        before = {
            field.name: getattr(capabilities, field.name)
            for field in capabilities._meta.fields
            if field.name.startswith("supports_")
        }
        for field in before:
            if field in request.data:
                setattr(capabilities, field, bool(request.data[field]))
        try:
            capabilities.full_clean()
            capabilities.save()
        except DjangoValidationError as exc:
            raise _validation_error(exc) from exc
        after = {field: getattr(capabilities, field) for field in before}
        record_audit(
            actor=request.user,
            action="category.capabilities.updated",
            target=category,
            before_snapshot=before,
            after_snapshot=after,
            request_id=_request_id(request),
        )
        return Response(after)


class ProvinceListView(AdminView):
    required_permission = "admin.provinces.read"

    @extend_schema(
        operation_id="adminProvincesList",
        tags=["Admin Provinces"],
        summary="List every province",
        responses={200: AdminProvinceListSerializer, **protected()},
    )
    def get(self, request):
        return Response(
            {
                "items": list(
                    Province.objects.order_by("sort_order", "name_ar").values(
                        "id", "code", "name_ar", "name_en", "active", "sort_order"
                    )
                )
            }
        )


class ProvinceDetailView(AdminView):
    required_permission = "admin.provinces.manage"

    @extend_schema(
        operation_id="adminProvinceUpdate",
        tags=["Admin Provinces"],
        summary="Activate a province or change its order",
        request=AdminProvinceUpdateRequestSerializer,
        responses={200: AdminProvinceUpdatedSerializer, **protected(), 404: NOT_FOUND_404},
    )
    def put(self, request, province_id):
        province = Province.objects.get(pk=province_id)
        before = {"active": province.active, "sortOrder": province.sort_order}
        if "active" in request.data:
            province.active = bool(request.data["active"])
        if "sortOrder" in request.data:
            province.sort_order = int(request.data["sortOrder"])
        province.save(update_fields=["active", "sort_order"])
        record_audit(
            actor=request.user,
            action="province.updated",
            target=province,
            before_snapshot=before,
            after_snapshot={"active": province.active, "sortOrder": province.sort_order},
            request_id=_request_id(request),
        )
        return Response({"active": province.active, "sortOrder": province.sort_order})


class CategoryProvinceView(AdminView):
    required_permission = "admin.taxonomy.manage"

    @extend_schema(
        operation_id="adminCategoryProvinceReplace",
        tags=["Admin Taxonomy"],
        summary="Set the per-province switches of a category",
        description="Public visibility and owner onboarding are independent switches.",
        request=AdminCategoryProvinceRequestSerializer,
        responses={200: AdminIdSerializer, 400: VALIDATION_400, **protected(), 404: NOT_FOUND_404},
    )
    def put(self, request, category_id):
        province_id = request.data.get("provinceId")
        if not province_id:
            raise ValidationError({"provinceId": "Required."})
        row, _ = CategoryProvince.objects.get_or_create(
            category_id=category_id,
            province_id=province_id,
        )
        before = {
            "publicEnabled": row.public_enabled,
            "ownerRegistrationEnabled": row.owner_registration_enabled,
        }
        row.public_enabled = bool(request.data.get("publicEnabled", row.public_enabled))
        row.owner_registration_enabled = bool(
            request.data.get("ownerRegistrationEnabled", row.owner_registration_enabled)
        )
        row.sort_order = int(request.data.get("sortOrder", row.sort_order))
        row.save()
        record_audit(
            actor=request.user,
            action="category.province.updated",
            target=row,
            before_snapshot=before,
            after_snapshot={
                "publicEnabled": row.public_enabled,
                "ownerRegistrationEnabled": row.owner_registration_enabled,
            },
            request_id=_request_id(request),
        )
        return Response({"id": row.pk})


class VerificationRequirementListView(AdminView):
    required_permission = "admin.verification.read"

    @extend_schema(
        operation_id="adminVerificationRequirementsList",
        tags=["Admin Verification"],
        summary="List verification requirements",
        responses={200: AdminVerificationRequirementListSerializer, **protected()},
    )
    def get(self, request):
        qs = VerificationRequirement.objects.order_by("category_id", "sort_order")
        return Response(
            {
                "items": list(
                    qs.values(
                        "id",
                        "category_id",
                        "label_ar",
                        "label_en",
                        "required",
                        "active",
                        "min_files",
                        "max_files",
                        "sort_order",
                    )
                )
            }
        )

    @extend_schema(
        operation_id="adminVerificationRequirementCreate",
        tags=["Admin Verification"],
        summary="Create a verification requirement",
        description="Requires the manage permission, which is re-checked inside the handler.",
        request=AdminVerificationRequirementRequestSerializer,
        responses={201: AdminIdSerializer, 400: VALIDATION_400, **protected()},
    )
    def post(self, request):
        self.required_permission = "admin.verification.manage"
        if not HasAdminPermission().has_permission(request, self):
            self.permission_denied(request)
        row = VerificationRequirement(
            category_id=request.data.get("categoryId"),
            label_ar=str(request.data.get("labelAr", "")).strip(),
            label_en=str(request.data.get("labelEn", "")).strip(),
            instructions_ar=str(request.data.get("instructionsAr", "")).strip(),
            instructions_en=str(request.data.get("instructionsEn", "")).strip(),
            required=bool(request.data.get("required", True)),
            active=bool(request.data.get("active", True)),
            min_files=int(request.data.get("minFiles", 1)),
            max_files=int(request.data.get("maxFiles", 1)),
            sort_order=int(request.data.get("sortOrder", 0)),
        )
        try:
            row.full_clean()
            row.save()
        except DjangoValidationError as exc:
            raise _validation_error(exc) from exc
        record_audit(
            actor=request.user,
            action="verification_requirement.created",
            target=row,
            after_snapshot={"categoryId": str(row.category_id), "required": row.required},
            request_id=_request_id(request),
        )
        return Response({"id": row.pk}, status=201)


class AdvertisementListView(AdminView):
    required_permission = "admin.ads.read"

    @extend_schema(
        operation_id="adminAdsList",
        tags=["Admin Ads"],
        summary="List advertisements",
        responses={200: AdminAdvertisementListSerializer, **protected()},
    )
    def get(self, request):
        return Response(
            {
                "items": list(
                    Advertisement.objects.order_by("sort_order", "-updated_at").values(
                        "id",
                        "title_ar",
                        "target_scope",
                        "enabled",
                        "starts_at",
                        "ends_at",
                        "sort_order",
                        "slide_duration_ms",
                    )
                )
            }
        )

    @extend_schema(
        operation_id="adminAdCreate",
        tags=["Admin Ads"],
        summary="Create an advertisement",
        description=(
            "Requires the manage permission, which is re-checked inside the handler. "
            "Action payloads are validated per action type."
        ),
        request=AdminAdvertisementRequestSerializer,
        responses={201: AdminIdSerializer, 400: VALIDATION_400, **protected()},
    )
    def post(self, request):
        self.required_permission = "admin.ads.manage"
        if not HasAdminPermission().has_permission(request, self):
            self.permission_denied(request)
        ad = Advertisement(
            image_key=str(request.data.get("imageKey", "")),
            title_ar=str(request.data.get("titleAr", "")),
            title_en=str(request.data.get("titleEn", "")),
            subtitle_ar=str(request.data.get("subtitleAr", "")),
            subtitle_en=str(request.data.get("subtitleEn", "")),
            action_type=request.data.get("actionType", Advertisement.ActionType.NONE),
            action_payload=request.data.get("actionPayload", {}),
            target_scope=request.data.get("targetScope", Advertisement.TargetScope.GLOBAL),
            province_id=request.data.get("provinceId"),
            category_id=request.data.get("categoryId"),
            enabled=bool(request.data.get("enabled", False)),
            sort_order=int(request.data.get("sortOrder", 0)),
            slide_duration_ms=int(request.data.get("slideDurationMs", 5000)),
        )
        try:
            save_advertisement(actor=request.user, advertisement=ad)
        except DjangoValidationError as exc:
            raise _validation_error(exc) from exc
        return Response({"id": ad.pk}, status=201)


class AdvertisementDetailView(AdminView):
    required_permission = "admin.ads.manage"

    @extend_schema(
        operation_id="adminAdDelete",
        tags=["Admin Ads"],
        summary="Delete an advertisement",
        responses={204: None, **protected(), 404: NOT_FOUND_404},
    )
    def delete(self, request, advertisement_id):
        ad = Advertisement.objects.get(pk=advertisement_id)
        delete_advertisement(actor=request.user, advertisement=ad)
        return Response(status=204)


class AuditListView(AdminView):
    required_permission = "admin.audit.read"

    @extend_schema(
        operation_id="adminAuditList",
        tags=["Admin Audit"],
        summary="Search the audit trail",
        description="Capped at 250 rows. Snapshots and metadata are stored redacted.",
        responses={200: AdminAuditListSerializer, **protected()},
    )
    def get(self, request):
        qs = AuditEvent.objects.order_by("-created_at")
        if value := request.query_params.get("actor"):
            qs = qs.filter(actor_id=value)
        if value := request.query_params.get("action"):
            qs = qs.filter(action__icontains=value)
        if value := request.query_params.get("resource"):
            qs = qs.filter(Q(target_type__icontains=value) | Q(target_id=value))
        if value := request.query_params.get("requestId"):
            qs = qs.filter(request_id=value)
        return Response(
            {
                "items": list(
                    qs.values(
                        "id",
                        "actor_id",
                        "action",
                        "target_type",
                        "target_id",
                        "request_id",
                        "metadata",
                        "created_at",
                    )[:250]
                )
            }
        )


class AnalyticsView(AdminView):
    required_permission = "admin.analytics.read"

    @extend_schema(
        operation_id="adminAnalyticsRetrieve",
        tags=["Admin Analytics"],
        summary="Operational KPIs",
        responses={200: AdminAnalyticsSerializer, **protected()},
    )
    def get(self, request):
        event_counts = list(
            ProductAnalyticsEvent.objects.values("name")
            .annotate(count=Count("id"))
            .order_by("name")
        )
        return Response(
            {
                "activeFacilities": Facility.objects.filter(status=Facility.Status.ACTIVE).count(),
                "pendingReviews": FacilityApplication.objects.filter(
                    status=FacilityApplication.Status.SUBMITTED
                ).count(),
                "ratingAverage": Facility.objects.aggregate(value=Avg("ratings__stars"))["value"],
                "events": event_counts,
            }
        )


class SettingsView(AdminView):
    required_permission = "admin.settings.read"

    @extend_schema(
        operation_id="adminSettingsList",
        tags=["Admin Settings"],
        summary="List typed platform settings",
        responses={200: AdminSettingListSerializer, **protected()},
    )
    def get(self, request):
        return Response(
            {
                "items": list(
                    PlatformSetting.objects.order_by("key").values(
                        "key", "value_type", "value", "updated_at"
                    )
                )
            }
        )

    @extend_schema(
        operation_id="adminSettingWrite",
        tags=["Admin Settings"],
        summary="Create or update a typed platform setting",
        description="Requires the manage permission, which is re-checked inside the handler.",
        request=AdminSettingWriteRequestSerializer,
        responses={200: AdminSettingWrittenSerializer, 400: VALIDATION_400, **protected()},
    )
    def put(self, request):
        self.required_permission = "admin.settings.manage"
        if not HasAdminPermission().has_permission(request, self):
            self.permission_denied(request)
        key = str(request.data.get("key", "")).strip()
        if not key:
            raise ValidationError({"key": "Required."})
        setting, _ = PlatformSetting.objects.get_or_create(
            key=key,
            defaults={"value_type": request.data.get("type", "JSON"), "value": None},
        )
        before = {"type": setting.value_type, "value": setting.value}
        setting.value_type = request.data.get("type", setting.value_type)
        setting.value = request.data.get("value")
        setting.updated_by = request.user
        try:
            setting.full_clean()
            setting.save()
        except DjangoValidationError as exc:
            raise _validation_error(exc) from exc
        record_audit(
            actor=request.user,
            action="platform_setting.updated",
            target=setting,
            before_snapshot=before,
            after_snapshot={"type": setting.value_type, "value": setting.value},
            request_id=_request_id(request),
        )
        return Response({"key": setting.key, "type": setting.value_type, "value": setting.value})


class SystemStatusView(AdminView):
    required_permission = "admin.system.read"

    @extend_schema(
        operation_id="adminSystemStatusRetrieve",
        tags=["Admin System"],
        summary="Runtime and configuration status",
        description=(
            "Reports only whether each dependency is configured. No secret, connection "
            "string or credential is returned."
        ),
        responses={200: AdminSystemStatusSerializer, **protected()},
    )
    def get(self, request):
        database = "unavailable"
        try:
            with connection.cursor() as cursor:
                cursor.execute("SELECT 1")
                cursor.fetchone()
            database = "ok"
        except Exception:
            database = "unavailable"
        from django.conf import settings

        return Response(
            {
                "apiVersion": "1.0.0",
                "environment": getattr(settings, "ENVIRONMENT_NAME", "unknown"),
                "database": database,
                "redis": "configured" if getattr(settings, "REDIS_URL", "") else "unconfigured",
                "celery": (
                    "configured"
                    if getattr(settings, "CELERY_BROKER_URL", "")
                    else "unconfigured"
                ),
                "storage": (
                    "configured"
                    if getattr(settings, "S3_ENDPOINT_URL", "")
                    else "unconfigured"
                ),
                "schemaHash": getattr(settings, "OPENAPI_SCHEMA_HASH", "unavailable"),
                "checkedAt": timezone.now().isoformat(),
            }
        )
