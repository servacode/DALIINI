"""Accounts and the console roles they hold: lists, details, blocking, role grants."""

from uuid import UUID

from django.db.models import Q
from django.shortcuts import get_object_or_404
from drf_spectacular.utils import (
    extend_schema,
    extend_schema_view,
)
from rest_framework.exceptions import ValidationError
from rest_framework.response import Response

from accounts.authentication import AuthenticatedRequest
from accounts.models import AdminRole, User, UserAdminRole
from core.openapi import NOT_FOUND_404, VALIDATION_400, protected
from core.pagination import QueryOrderedCursorPage, page_parameters

from .schemas import (
    AdminUserDetailSerializer,
    AdminUserListSerializer,
    AdminUserRolesRequestSerializer,
    AdminUserSerializer,
)
from .serializers import (
    user_payload,
)
from .services import (
    replace_user_roles,
    set_user_blocked,
)
from .views import (
    AdminView,
    _filter,
    _page,
)

# Wire ordering -> columns, each ending in the primary key so ties stay stable across pages.
USER_ORDERINGS: dict[str, tuple[str, ...]] = {
    "createdAt": ("created_at", "id"),
    "-createdAt": ("-created_at", "-id"),
    "name": ("name", "id"),
    "-name": ("-name", "-id"),
}


class UserListView(AdminView):
    required_permission = "admin.users.read"

    @extend_schema(
        operation_id="adminUsersList",
        tags=["Admin Users"],
        summary="Search user accounts",
        description=(
            "Password hashes and session secret material are never returned. Newest first "
            "unless `ordering` says otherwise, in cursor pages. Every filter is optional."
        ),
        parameters=[
            *page_parameters(QueryOrderedCursorPage),
            _filter("q", "Free text matched against the account name and phone number."),
            _filter(
                "status",
                "`active` keeps active accounts; any other value keeps blocked accounts.",
            ),
            _filter(
                "role",
                "Admin role id or code; keeps accounts holding that role actively. The value "
                "`any` keeps every operator, `none` every non-operator.",
            ),
            _filter("ordering", "createdAt, -createdAt (the default), name or -name."),
        ],
        responses={200: AdminUserListSerializer, **protected()},
    )
    def get(self, request: AuthenticatedRequest) -> Response:
        ordering = request.query_params.get("ordering") or "-createdAt"
        if ordering not in USER_ORDERINGS:
            raise ValidationError({"ordering": f"Use one of {', '.join(USER_ORDERINGS)}."})
        qs = User.objects.order_by(*USER_ORDERINGS[ordering])
        if value := request.query_params.get("q"):
            qs = qs.filter(Q(name__icontains=value) | Q(phone__icontains=value))
        if value := request.query_params.get("status"):
            qs = qs.filter(is_active=value.lower() == "active")
        if value := request.query_params.get("role"):
            operators = UserAdminRole.objects.filter(active=True)
            if value == "none":
                qs = qs.exclude(pk__in=operators.values("user_id"))
            else:
                if value != "any":
                    by = Q(role__code=value)
                    if value.isdigit():
                        by |= Q(role_id=int(value))
                    operators = operators.filter(by)
                qs = qs.filter(pk__in=operators.values("user_id"))
        return _page(request, qs, user_payload)


class UserDetailView(AdminView):
    required_permission = "admin.users.read"

    @extend_schema(
        operation_id="adminUserRetrieve",
        tags=["Admin Users"],
        summary="Retrieve one user with the roles assigned",
        responses={200: AdminUserDetailSerializer, **protected(), 404: NOT_FOUND_404},
    )
    def get(self, request: AuthenticatedRequest, user_id: UUID) -> Response:
        user = get_object_or_404(User, pk=user_id)
        payload = user_payload(user)
        # Integers, as `AdminRole` is keyed and as the role list declares its ids.
        payload["roleIds"] = list(
            user.admin_role_links.filter(active=True).values_list("role_id", flat=True)
        )
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
    def post(self, request: AuthenticatedRequest, user_id: UUID) -> Response:
        user = get_object_or_404(User, pk=user_id)
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
    def put(self, request: AuthenticatedRequest, user_id: UUID) -> Response:
        role_ids = request.data.get("roleIds", [])
        if not isinstance(role_ids, list):
            raise ValidationError({"roleIds": "Must be a list."})
        if AdminRole.objects.filter(id__in=role_ids).count() != len(set(role_ids)):
            raise ValidationError({"roleIds": "Unknown role."})
        user = get_object_or_404(User, pk=user_id)
        replace_user_roles(request=request, user=user, role_ids=role_ids)
        return Response(status=204)
