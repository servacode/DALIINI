"""Invitations by phone number: sent by an owner, answered by the invitee (`invitations`)."""

from __future__ import annotations

from typing import Any
from uuid import UUID

from django.shortcuts import get_object_or_404
from drf_spectacular.utils import extend_schema
from rest_framework import serializers
from rest_framework.exceptions import NotFound
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from accounts.authentication import AuthenticatedRequest
from accounts.phone import normalize_syrian_phone
from core.openapi import CONFLICT_409, NOT_FOUND_404, VALIDATION_400, protected
from core.throttles import UserDefaultThrottle, UserOrIpThrottle

from .invitations import (
    accept,
    decline,
    invitation_payload,
    invite,
    received_by,
    received_payload,
    revoke,
)
from .models import FacilityInvitation, FacilityMembership
from .permissions import require_facility_owner
from .views import _owned_facilities, _request_id

STATUSES = ["PENDING", "ACCEPTED", "DECLINED", "REVOKED", "EXPIRED"]


class InviteThrottle(UserOrIpThrottle):
    scope = "owner_invite"
    only_writes = True


class InvitationRequestSerializer(serializers.Serializer[Any]):
    phone = serializers.CharField(
        max_length=20, help_text="Syrian mobile, 09XXXXXXXX or +9639XXXXXXXX."
    )
    role = serializers.ChoiceField(
        choices=FacilityMembership.Role.choices, default=FacilityMembership.Role.MANAGER
    )

    def validate_phone(self, value: str) -> str:
        try:
            return normalize_syrian_phone(value)
        except ValueError as exc:
            raise serializers.ValidationError("Enter a valid Syrian mobile number.") from exc


class InvitationSerializer(serializers.Serializer[Any]):
    id = serializers.UUIDField()
    phone = serializers.CharField()
    role = serializers.ChoiceField(choices=FacilityMembership.Role.choices)
    status = serializers.ChoiceField(
        choices=[(value, value) for value in STATUSES],
        help_text="EXPIRED is a PENDING invitation past `expiresAt`.",
    )
    createdAt = serializers.DateTimeField()
    expiresAt = serializers.DateTimeField()
    respondedAt = serializers.DateTimeField(allow_null=True)


class InvitationListSerializer(serializers.Serializer[Any]):
    items = InvitationSerializer(many=True)


class ReceivedFacilitySerializer(serializers.Serializer[Any]):
    id = serializers.UUIDField()
    nameAr = serializers.CharField()
    categoryNameAr = serializers.CharField()
    provinceNameAr = serializers.CharField()


class ReceivedInvitationSerializer(serializers.Serializer[Any]):
    id = serializers.UUIDField()
    role = serializers.ChoiceField(choices=FacilityMembership.Role.choices)
    facility = ReceivedFacilitySerializer()
    invitedByName = serializers.CharField(allow_null=True)
    createdAt = serializers.DateTimeField()
    expiresAt = serializers.DateTimeField()


class ReceivedInvitationListSerializer(serializers.Serializer[Any]):
    items = ReceivedInvitationSerializer(many=True)


class AcceptedSerializer(serializers.Serializer[Any]):
    facilityId = serializers.UUIDField()
    role = serializers.ChoiceField(choices=FacilityMembership.Role.choices)


class OwnerFacilityInvitationsView(APIView):
    permission_classes = [IsAuthenticated]
    throttle_classes = [InviteThrottle, UserDefaultThrottle]

    @extend_schema(
        operation_id="ownerFacilityInvitationsList",
        tags=["Owner"],
        summary="Invitations sent for a facility",
        description="Owners only. Newest first.",
        responses={200: InvitationListSerializer, **protected(), 404: NOT_FOUND_404},
    )
    def get(self, request: AuthenticatedRequest, facility_id: UUID) -> Response:
        facility = get_object_or_404(_owned_facilities(request.user), pk=facility_id)
        require_facility_owner(request.user, facility)
        rows = facility.invitations.order_by("-created_at")[:100]
        return Response({"items": [invitation_payload(row) for row in rows]})

    @extend_schema(
        operation_id="ownerFacilityInvitationCreate",
        tags=["Owner"],
        summary="Invite someone to help run a facility, by phone number",
        description=(
            "Owners only. The answer is the same whether or not the number has an account, "
            "so this cannot be used to find out who is registered. A person with an account "
            "is notified at once; anyone else finds the invitation when they sign up with "
            "that number. It lasts seven days; inviting the same number again renews it. "
            "409 ALREADY_MEMBER when the number belongs to a member already."
        ),
        request=InvitationRequestSerializer,
        responses={
            201: InvitationSerializer,
            400: VALIDATION_400,
            **protected(),
            404: NOT_FOUND_404,
            409: CONFLICT_409,
        },
    )
    def post(self, request: AuthenticatedRequest, facility_id: UUID) -> Response:
        facility = get_object_or_404(_owned_facilities(request.user), pk=facility_id)
        require_facility_owner(request.user, facility)
        payload = InvitationRequestSerializer(data=request.data)
        payload.is_valid(raise_exception=True)
        invitation = invite(
            actor=request.user,
            facility=facility,
            phone=payload.validated_data["phone"],
            role=payload.validated_data["role"],
            request_id=_request_id(request),
        )
        return Response(invitation_payload(invitation), status=201)


class OwnerFacilityInvitationRevokeView(APIView):
    permission_classes = [IsAuthenticated]

    @extend_schema(
        operation_id="ownerFacilityInvitationRevoke",
        tags=["Owner"],
        summary="Withdraw an invitation that has not been answered",
        responses={204: None, **protected(), 404: NOT_FOUND_404, 409: CONFLICT_409},
    )
    def delete(
        self, request: AuthenticatedRequest, facility_id: UUID, invitation_id: UUID
    ) -> Response:
        facility = get_object_or_404(_owned_facilities(request.user), pk=facility_id)
        require_facility_owner(request.user, facility)
        try:
            revoke(
                actor=request.user,
                facility=facility,
                invitation_id=invitation_id,
                request_id=_request_id(request),
            )
        except FacilityInvitation.DoesNotExist as exc:
            raise NotFound() from exc
        return Response(status=204)


class AccountInvitationsView(APIView):
    permission_classes = [IsAuthenticated]

    @extend_schema(
        operation_id="accountInvitationsList",
        tags=["Account"],
        summary="Invitations waiting for this account's phone number",
        responses={200: ReceivedInvitationListSerializer, **protected()},
    )
    def get(self, request: AuthenticatedRequest) -> Response:
        rows = (
            received_by(request.user)
            .select_related("facility__category", "facility__province", "invited_by")
            .order_by("-created_at")
        )
        return Response({"items": [received_payload(row) for row in rows]})


class AccountInvitationAcceptView(APIView):
    permission_classes = [IsAuthenticated]

    @extend_schema(
        operation_id="accountInvitationAccept",
        tags=["Account"],
        summary="Join the facility an invitation is for",
        description=(
            "Only the account whose phone number was invited can accept; any other caller "
            "gets 404. An invitation to own raises a manager to owner and never lowers anyone. "
            "409 INVITATION_EXPIRED or INVITATION_CLOSED when it can no longer be accepted."
        ),
        request=None,
        responses={200: AcceptedSerializer, **protected(), 404: NOT_FOUND_404, 409: CONFLICT_409},
    )
    def post(self, request: AuthenticatedRequest, invitation_id: UUID) -> Response:
        try:
            member = accept(
                user=request.user, invitation_id=invitation_id, request_id=_request_id(request)
            )
        except FacilityInvitation.DoesNotExist as exc:
            raise NotFound() from exc
        return Response({"facilityId": str(member.facility_id), "role": member.role})


class AccountInvitationDeclineView(APIView):
    permission_classes = [IsAuthenticated]

    @extend_schema(
        operation_id="accountInvitationDecline",
        tags=["Account"],
        summary="Decline an invitation",
        request=None,
        responses={204: None, **protected(), 404: NOT_FOUND_404, 409: CONFLICT_409},
    )
    def post(self, request: AuthenticatedRequest, invitation_id: UUID) -> Response:
        try:
            decline(user=request.user, invitation_id=invitation_id, request_id=_request_id(request))
        except FacilityInvitation.DoesNotExist as exc:
            raise NotFound() from exc
        return Response(status=204)
