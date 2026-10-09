"""Category groups, categories, what each can do and where, and what it must prove."""

from typing import Any
from uuid import UUID

from django.core.exceptions import ValidationError as DjangoValidationError
from django.db.models import Count, Prefetch
from django.shortcuts import get_object_or_404
from drf_spectacular.utils import (
    extend_schema,
    extend_schema_view,
)
from rest_framework.exceptions import ValidationError
from rest_framework.response import Response

from accounts.authentication import AuthenticatedRequest
from audit.services import record_audit
from core.openapi import NOT_FOUND_404, VALIDATION_400, protected
from directory.models import (
    Category,
    CategoryCapabilities,
    CategoryGroup,
    CategoryProvince,
    VerificationRequirement,
)
from directory.services import (
    create_category,
    create_category_group,
    create_verification_requirement,
    update_category,
    update_category_group,
    update_verification_requirement,
)

from .permissions import HasAdminPermission
from .schemas import (
    AdminCapabilitiesRequestSerializer,
    AdminCapabilitiesSerializer,
    AdminCategoryCardListSerializer,
    AdminCategoryCardSerializer,
    AdminCategoryCreateRequestSerializer,
    AdminCategoryGroupListSerializer,
    AdminCategoryGroupRequestSerializer,
    AdminCategoryGroupSerializer,
    AdminCategoryListSerializer,
    AdminCategoryProvinceRequestSerializer,
    AdminCategorySerializer,
    AdminCategoryUpdateRequestSerializer,
    AdminIdSerializer,
    AdminVerificationRequirementListSerializer,
    AdminVerificationRequirementRequestSerializer,
    AdminVerificationRequirementSerializer,
    AdminVerificationRequirementUpdateRequestSerializer,
)
from .views import (
    AdminView,
    _request_id,
    _validation_error,
)


class TaxonomyView(AdminView):
    required_permission = "admin.taxonomy.read"
    # CategoryGroup or Category, set by each subclass.
    model: Any = None

    @extend_schema(
        operation_id="adminTaxonomyList",
        tags=["Admin Taxonomy"],
        summary="List taxonomy rows",
        responses={200: AdminCategoryListSerializer, **protected()},
    )
    def get(self, request: AuthenticatedRequest) -> Response:
        serializer: type[AdminCategoryGroupSerializer] | type[AdminCategorySerializer]
        if self.model is CategoryGroup:
            rows = self.model.objects.order_by("sort_order", "name_ar").values(
                "id", "code", "name_ar", "name_en", "icon_key", "active", "sort_order"
            )
            serializer = AdminCategoryGroupSerializer
        else:
            rows = self.model.objects.order_by("sort_order", "name_ar").values(
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
            serializer = AdminCategorySerializer
        return Response({"items": serializer(rows, many=True).data})


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


# The flags a category card shows, in the wire's names (the request serializer's own).
CAPABILITY_FIELDS = {
    str(field.source): name
    for name, field in AdminCapabilitiesRequestSerializer().fields.items()
}


class CategoryListView(AdminView):
    required_permission = "admin.taxonomy.read"

    @extend_schema(
        operation_id="adminCategoriesList",
        tags=["Admin Taxonomy"],
        summary="List categories",
        description=(
            "Each category with its capability flags, its province switches and how many "
            "facilities it holds: everything its card and settings window show, in one query "
            "count whatever the number of categories."
        ),
        responses={200: AdminCategoryCardListSerializer, **protected()},
    )
    def get(self, request: AuthenticatedRequest) -> Response:
        rows = (
            Category.objects.select_related("capabilities")
            .prefetch_related(
                Prefetch(
                    "province_switches",
                    queryset=CategoryProvince.objects.select_related("province").order_by(
                        "province__sort_order", "province__name_ar"
                    ),
                )
            )
            .annotate(facility_count=Count("facilities"))
            .order_by("sort_order", "name_ar")
        )
        items = []
        for category in rows:
            flags = getattr(category, "capabilities", None)
            items.append(
                {
                    "id": category.pk,
                    "group_id": category.group_id,
                    "code": category.code,
                    "slug": category.slug,
                    "name_ar": category.name_ar,
                    "name_en": category.name_en,
                    "icon_key": category.icon_key,
                    "specialization": category.specialization,
                    "active": category.active,
                    "sort_order": category.sort_order,
                    "capabilities": {
                        wire: bool(getattr(flags, column)) if flags else False
                        for column, wire in CAPABILITY_FIELDS.items()
                    },
                    "switches": [
                        {
                            "provinceId": switch.province_id,
                            "provinceNameAr": switch.province.name_ar,
                            "publicEnabled": switch.public_enabled,
                            "ownerRegistrationEnabled": switch.owner_registration_enabled,
                        }
                        for switch in category.province_switches.all()
                    ],
                    "facilityCount": category.facility_count,
                }
            )
        return Response({"items": AdminCategoryCardSerializer(items, many=True).data})


class CategoryGroupCreateView(AdminView):
    """Cycle J begins here: a group has to exist before a category can join it."""

    required_permission = "admin.taxonomy.manage"

    @extend_schema(
        operation_id="adminCategoryGroupCreate",
        tags=["Admin Taxonomy"],
        summary="Create a category group",
        request=AdminCategoryGroupRequestSerializer,
        responses={201: AdminIdSerializer, 400: VALIDATION_400, **protected()},
    )
    def post(self, request: AuthenticatedRequest) -> Response:
        payload = AdminCategoryGroupRequestSerializer(data=request.data)
        payload.is_valid(raise_exception=True)
        if not payload.validated_data.get("code") or not payload.validated_data.get("nameAr"):
            raise ValidationError({"code": ["Required."], "nameAr": ["Required."]})
        try:
            group = create_category_group(request=request, data=payload.validated_data)
        except DjangoValidationError as exc:
            raise _validation_error(exc) from exc
        return Response({"id": str(group.pk)}, status=201)


class CategoryGroupDetailView(AdminView):
    required_permission = "admin.taxonomy.manage"

    @extend_schema(
        operation_id="adminCategoryGroupUpdate",
        tags=["Admin Taxonomy"],
        summary="Rename, reorder or deactivate a category group",
        description="The group code is immutable; sending a different one is refused.",
        request=AdminCategoryGroupRequestSerializer,
        responses={
            200: AdminCategoryGroupSerializer,
            400: VALIDATION_400,
            **protected(),
            404: NOT_FOUND_404,
        },
    )
    def put(self, request: AuthenticatedRequest, group_id: UUID) -> Response:
        payload = AdminCategoryGroupRequestSerializer(data=request.data, partial=True)
        payload.is_valid(raise_exception=True)
        try:
            group = update_category_group(
                request=request, group_id=group_id, data=payload.validated_data
            )
        except DjangoValidationError as exc:
            raise _validation_error(exc) from exc
        return Response(AdminCategoryGroupSerializer(group).data)


class CategoryCreateView(AdminView):
    required_permission = "admin.taxonomy.manage"

    @extend_schema(
        operation_id="adminCategoryCreate",
        tags=["Admin Taxonomy"],
        summary="Create a category",
        description=(
            "`code` and `slug` are fixed at creation and cannot be changed afterwards. A "
            "new category is invisible everywhere until its per-province switches are "
            "turned on, whatever `active` says."
        ),
        request=AdminCategoryCreateRequestSerializer,
        responses={201: AdminIdSerializer, 400: VALIDATION_400, **protected()},
    )
    def post(self, request: AuthenticatedRequest) -> Response:
        payload = AdminCategoryCreateRequestSerializer(data=request.data)
        payload.is_valid(raise_exception=True)
        try:
            category = create_category(request=request, data=payload.validated_data)
        except DjangoValidationError as exc:
            raise _validation_error(exc) from exc
        return Response({"id": str(category.pk)}, status=201)


class CategoryDetailView(AdminView):
    required_permission = "admin.taxonomy.manage"

    @extend_schema(
        operation_id="adminCategoryUpdate",
        tags=["Admin Taxonomy"],
        summary="Rename, move, reorder or deactivate a category",
        description=(
            "`code` and `slug` are immutable and are not accepted. Changing the "
            "specialization re-validates the capability set, so a category that carries "
            "duty cannot be moved off PHARMACY while it does."
        ),
        request=AdminCategoryUpdateRequestSerializer,
        responses={
            200: AdminCategorySerializer,
            400: VALIDATION_400,
            **protected(),
            404: NOT_FOUND_404,
        },
    )
    def put(self, request: AuthenticatedRequest, category_id: UUID) -> Response:
        payload = AdminCategoryUpdateRequestSerializer(data=request.data, partial=True)
        payload.is_valid(raise_exception=True)
        data = dict(payload.validated_data)
        # The serializer drops `code` and `slug`, so an attempt to change them would be
        # silently ignored. 09-ADMIN-NEXTJS.md asks for the opposite: refuse, visibly.
        for field in ("code", "slug"):
            if field in request.data:
                data[field] = request.data[field]
        try:
            category = update_category(request=request, category_id=category_id, data=data)
        except DjangoValidationError as exc:
            raise _validation_error(exc) from exc
        return Response(AdminCategorySerializer(category).data)


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
    def put(self, request: AuthenticatedRequest, category_id: UUID) -> Response:
        category = get_object_or_404(Category, pk=category_id)
        capabilities, _ = CategoryCapabilities.objects.get_or_create(category=category)
        payload = AdminCapabilitiesRequestSerializer(data=request.data, partial=True)
        payload.is_valid(raise_exception=True)

        # INT-039: the wire is camelCase and the columns are not. The serializer's `source`
        # mapping is the translation, so `validated_data` already carries column names.
        columns = [
            field.name for field in capabilities._meta.fields if field.name.startswith("supports_")
        ]
        before = {name: getattr(capabilities, name) for name in columns}
        for name, value in payload.validated_data.items():
            setattr(capabilities, name, bool(value))
        try:
            capabilities.full_clean()
            capabilities.save()
        except DjangoValidationError as exc:
            raise _validation_error(exc) from exc
        record_audit(
            actor=request.user,
            action="category.capabilities.updated",
            target=category,
            before_snapshot=before,
            after_snapshot={name: getattr(capabilities, name) for name in columns},
            request_id=_request_id(request),
        )
        return Response(AdminCapabilitiesSerializer(capabilities).data)


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
    def put(self, request: AuthenticatedRequest, category_id: UUID) -> Response:
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
    def get(self, request: AuthenticatedRequest) -> Response:
        qs = VerificationRequirement.objects.order_by("category_id", "sort_order")
        return Response(
            {
                "items": AdminVerificationRequirementSerializer(
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
                    ),
                    many=True,
                ).data
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
    def post(self, request: AuthenticatedRequest) -> Response:
        self.required_permission = "admin.verification.manage"
        if not HasAdminPermission().has_permission(request, self):
            self.permission_denied(request)
        payload = AdminVerificationRequirementRequestSerializer(data=request.data)
        payload.is_valid(raise_exception=True)
        try:
            requirement = create_verification_requirement(
                request=request, data=payload.validated_data
            )
        except DjangoValidationError as exc:
            raise _validation_error(exc) from exc
        return Response({"id": str(requirement.pk)}, status=201)


class VerificationRequirementDetailView(AdminView):
    required_permission = "admin.verification.manage"

    @extend_schema(
        operation_id="adminVerificationRequirementUpdate",
        tags=["Admin Verification"],
        summary="Edit a verification requirement, or retire it",
        description=(
            "The owning category cannot change: evidence already submitted points at a "
            "(facility, requirement) pair. Retirement is `active = false`; there is no "
            "delete, because evidence references the row."
        ),
        request=AdminVerificationRequirementUpdateRequestSerializer,
        responses={
            200: AdminVerificationRequirementSerializer,
            400: VALIDATION_400,
            **protected(),
            404: NOT_FOUND_404,
        },
    )
    def put(self, request: AuthenticatedRequest, requirement_id: int) -> Response:
        payload = AdminVerificationRequirementUpdateRequestSerializer(
            data=request.data, partial=True
        )
        payload.is_valid(raise_exception=True)
        data = dict(payload.validated_data)
        if "categoryId" in request.data:
            data["categoryId"] = request.data["categoryId"]
        try:
            requirement = update_verification_requirement(
                request=request, requirement_id=requirement_id, data=data
            )
        except DjangoValidationError as exc:
            raise _validation_error(exc) from exc
        return Response(AdminVerificationRequirementSerializer(requirement).data)
