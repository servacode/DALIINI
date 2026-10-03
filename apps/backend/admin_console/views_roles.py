"""The console's roles and what each one may do: the role editor (DECISION-072).

Roles were seeded and could only be assigned; now whoever holds `admin.roles.manage` can
create one, rename it, choose its permissions and delete it once nobody holds it. Two rules
from `accounts.roles` bound every change: the owner role is the platform's and stays whole,
and no change may leave the platform without someone able to grant roles.
"""

import secrets
from typing import Any

from django.db import transaction
from django.db.models import Count, Q
from django.shortcuts import get_object_or_404
from drf_spectacular.utils import extend_schema
from rest_framework.exceptions import ValidationError
from rest_framework.response import Response

from accounts.authentication import AuthenticatedRequest
from accounts.models import AdminPermission, AdminRole
from accounts.roles import OWNER_ROLE_CODE, require_role_manager
from audit.services import record_audit
from core.exceptions import DomainError
from core.openapi import CONFLICT_409, NOT_FOUND_404, VALIDATION_400, protected

from .permissions import HasAdminPermission
from .schemas import (
    AdminPermissionListSerializer,
    AdminRoleCreateRequestSerializer,
    AdminRoleListSerializer,
    AdminRoleSerializer,
    AdminRoleUpdateRequestSerializer,
)
from .views import AdminView, _request_id


def _roles() -> Any:
    return AdminRole.objects.prefetch_related("permissions").annotate(
        holders=Count(
            "user_links",
            filter=Q(user_links__active=True, user_links__user__is_active=True),
            distinct=True,
        )
    )


def role_payload(role: AdminRole) -> dict[str, Any]:
    return {
        # An integer, as `AdminRoleSerializer` has always declared it.
        "id": role.id,
        "code": role.code,
        "name": role.name,
        "permissions": sorted(permission.code for permission in role.permissions.all()),
        "holderCount": getattr(role, "holders", 0),
        "locked": role.code == OWNER_ROLE_CODE,
    }


def _snapshot(role: AdminRole) -> dict[str, Any]:
    return {
        "name": role.name,
        "permissions": sorted(role.permissions.values_list("code", flat=True)),
    }


def _permissions(codes: list[str]) -> list[AdminPermission]:
    wanted = set(codes)
    found = list(AdminPermission.objects.filter(code__in=wanted))
    unknown = sorted(wanted - {permission.code for permission in found})
    if unknown:
        raise ValidationError({"permissions": f"صلاحية غير معروفة: {', '.join(unknown)}."})
    return found


def _name_taken(name: str, *, other_than: int | None = None) -> bool:
    qs = AdminRole.objects.filter(name__iexact=name)
    if other_than is not None:
        qs = qs.exclude(pk=other_than)
    return qs.exists()


def _refuse_locked(role: AdminRole) -> None:
    if role.code == OWNER_ROLE_CODE:
        raise DomainError(
            "ROLE_LOCKED",
            message="دور مدير المنصة يحمل كل الصلاحيات دائماً، ولا يُعدَّل ولا يُحذف.",
            status_code=409,
        )


class PermissionListView(AdminView):
    required_permission = "admin.roles.read"

    @extend_schema(
        operation_id="adminPermissionsList",
        tags=["Admin Users"],
        summary="Every permission a role can carry",
        description="The full catalogue, ordered by code. Labels for display belong to the client.",
        responses={200: AdminPermissionListSerializer, **protected()},
    )
    def get(self, request: AuthenticatedRequest) -> Response:
        items = AdminPermission.objects.order_by("code").values("code", "description")
        return Response({"items": list(items)})


class RoleListView(AdminView):
    required_permission = "admin.roles.read"

    @extend_schema(
        operation_id="adminRolesList",
        tags=["Admin Users"],
        summary="List admin roles, their permission codes and how many hold each",
        responses={200: AdminRoleListSerializer, **protected()},
    )
    def get(self, request: AuthenticatedRequest) -> Response:
        return Response({"items": [role_payload(role) for role in _roles().order_by("name")]})

    @extend_schema(
        operation_id="adminRoleCreate",
        tags=["Admin Users"],
        summary="Create a role with the permissions it carries",
        description="Requires `admin.roles.manage`, which is re-checked inside the handler.",
        request=AdminRoleCreateRequestSerializer,
        responses={201: AdminRoleSerializer, 400: VALIDATION_400, **protected()},
    )
    def post(self, request: AuthenticatedRequest) -> Response:
        self.required_permission = "admin.roles.manage"
        if not HasAdminPermission().has_permission(request, self):
            self.permission_denied(request)
        payload = AdminRoleCreateRequestSerializer(data=request.data)
        payload.is_valid(raise_exception=True)
        name = payload.validated_data["name"].strip()
        if not name:
            raise ValidationError({"name": "اكتب اسم الدور."})
        if _name_taken(name):
            raise ValidationError({"name": "يوجد دور آخر بهذا الاسم."})
        code = payload.validated_data.get("code") or f"role-{secrets.token_hex(4)}"
        if AdminRole.objects.filter(code=code).exists():
            raise ValidationError({"code": "يوجد دور آخر بهذا الرمز."})
        permissions = _permissions(payload.validated_data["permissions"])
        with transaction.atomic():
            role = AdminRole.objects.create(code=code, name=name)
            role.permissions.set(permissions)
            record_audit(
                actor=request.user,
                action="admin_role.created",
                target=role,
                after_snapshot=_snapshot(role),
                request_id=_request_id(request),
            )
        return Response(role_payload(_roles().get(pk=role.pk)), status=201)


class RoleDetailView(AdminView):
    required_permission = "admin.roles.manage"

    @extend_schema(
        operation_id="adminRoleUpdate",
        tags=["Admin Users"],
        summary="Rename a role or change the permissions it carries",
        description=(
            "Omitted fields keep their value. Takes effect for every holder on their next "
            "request. Refused (409) for the owner role, and when it would leave nobody able "
            "to grant roles."
        ),
        request=AdminRoleUpdateRequestSerializer,
        responses={
            200: AdminRoleSerializer,
            400: VALIDATION_400,
            **protected(),
            404: NOT_FOUND_404,
            409: CONFLICT_409,
        },
    )
    def patch(self, request: AuthenticatedRequest, role_id: int) -> Response:
        payload = AdminRoleUpdateRequestSerializer(data=request.data)
        payload.is_valid(raise_exception=True)
        with transaction.atomic():
            role = get_object_or_404(AdminRole.objects.select_for_update(), pk=role_id)
            _refuse_locked(role)
            before = _snapshot(role)
            if "name" in payload.validated_data:
                name = payload.validated_data["name"].strip()
                if not name:
                    raise ValidationError({"name": "اكتب اسم الدور."})
                if _name_taken(name, other_than=role.pk):
                    raise ValidationError({"name": "يوجد دور آخر بهذا الاسم."})
                role.name = name
                role.save(update_fields=["name"])
            if "permissions" in payload.validated_data:
                role.permissions.set(_permissions(payload.validated_data["permissions"]))
                require_role_manager()
            after = _snapshot(role)
            if after != before:
                record_audit(
                    actor=request.user,
                    action="admin_role.updated",
                    target=role,
                    before_snapshot=before,
                    after_snapshot=after,
                    request_id=_request_id(request),
                )
        return Response(role_payload(_roles().get(pk=role.pk)))

    @extend_schema(
        operation_id="adminRoleDelete",
        tags=["Admin Users"],
        summary="Delete a role nobody holds",
        description=(
            "Refused (409) while any account holds the role, blocked accounts included, so a "
            "role is never taken from somebody as a side effect; and for the owner role."
        ),
        responses={204: None, **protected(), 404: NOT_FOUND_404, 409: CONFLICT_409},
    )
    def delete(self, request: AuthenticatedRequest, role_id: int) -> Response:
        with transaction.atomic():
            role = get_object_or_404(AdminRole.objects.select_for_update(), pk=role_id)
            _refuse_locked(role)
            if role.user_links.filter(active=True).exists():
                raise DomainError(
                    "ROLE_IN_USE",
                    message="لا يُحذف دور يحمله أحد. انزعه من حامليه أولاً.",
                    status_code=409,
                )
            record_audit(
                actor=request.user,
                action="admin_role.deleted",
                target=role,
                before_snapshot={**_snapshot(role), "code": role.code},
                request_id=_request_id(request),
            )
            role.delete()
        return Response(status=204)
