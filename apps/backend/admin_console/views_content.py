"""Admin editing of the platform's own words: pages, FAQ, emergency numbers, contact inbox.

Reads need admin.content.read, writes admin.content.manage (re-checked in the handler).
Every write is audited.
"""

from __future__ import annotations

from typing import Any

from django.core.exceptions import ValidationError as DjangoValidationError
from django.shortcuts import get_object_or_404
from django.utils import timezone
from drf_spectacular.utils import OpenApiParameter, extend_schema
from rest_framework.exceptions import NotFound, ValidationError
from rest_framework.response import Response

from audit.services import record_audit
from content_services.models import ContactMessage, EmergencyNumber, FaqEntry, LegalDocument
from content_services.pages import (
    all_pages,
    create_page,
    delete_page,
    key_for,
    page_payload,
    page_rows,
    update_page,
)
from core.openapi import CONFLICT_409, NOT_FOUND_404, VALIDATION_400, protected
from core.pagination import CursorPage
from locations.models import Province

from .schemas_smart import (
    AdminContactHandleRequestSerializer,
    AdminContactMessagePageSerializer,
    AdminContactMessageSerializer,
    AdminContentPageCreateRequestSerializer,
    AdminContentPageListSerializer,
    AdminContentPageSerializer,
    AdminContentPageUpdateRequestSerializer,
    AdminEmergencyNumberListSerializer,
    AdminEmergencyNumberRequestSerializer,
    AdminEmergencyNumberSerializer,
    AdminFaqEntryListSerializer,
    AdminFaqEntryRequestSerializer,
    AdminFaqEntrySerializer,
)
from .views import AdminView
from .views_smart import require_permission

READ = "admin.content.read"
MANAGE = "admin.content.manage"


def _request_id(request: Any) -> str:
    return getattr(request, "request_id", "")


def _django_error(exc: DjangoValidationError) -> ValidationError:
    if hasattr(exc, "message_dict"):
        return ValidationError(exc.message_dict)
    return ValidationError({"nonFieldErrors": exc.messages})


# ------------------------------------------------------------------------------ pages


class ContentPageListView(AdminView):
    required_permission = READ

    @extend_schema(
        operation_id="adminContentPagesList",
        tags=["Admin Content"],
        summary="List content pages, including the built-in legal pages",
        responses={200: AdminContentPageListSerializer, **protected()},
    )
    def get(self, request: Any) -> Response:
        return Response({"items": all_pages()})

    @extend_schema(
        operation_id="adminContentPageCreate",
        tags=["Admin Content"],
        summary="Create a content page",
        description="409 CONTENT_PAGE_EXISTS when the slug is taken (case-insensitive).",
        request=AdminContentPageCreateRequestSerializer,
        responses={
            201: AdminContentPageSerializer,
            400: VALIDATION_400,
            **protected(),
            409: CONFLICT_409,
        },
    )
    def post(self, request: Any) -> Response:
        require_permission(self, request, MANAGE)
        payload = AdminContentPageCreateRequestSerializer(data=request.data)
        payload.is_valid(raise_exception=True)
        try:
            rows = create_page(
                actor=request.user, data=payload.validated_data, request_id=_request_id(request)
            )
        except DjangoValidationError as exc:
            raise _django_error(exc) from exc
        return Response(page_payload(rows), status=201)


class ContentPageDetailView(AdminView):
    required_permission = READ

    def _key(self, slug: str) -> str:
        return key_for(slug)

    @extend_schema(
        operation_id="adminContentPageRetrieve",
        tags=["Admin Content"],
        summary="Retrieve a content page with its newest words",
        responses={200: AdminContentPageSerializer, **protected(), 404: NOT_FOUND_404},
    )
    def get(self, request: Any, slug: str) -> Response:
        rows = page_rows(self._key(slug))
        if not rows:
            raise NotFound()
        return Response(page_payload(rows))

    @extend_schema(
        operation_id="adminContentPageUpdate",
        tags=["Admin Content"],
        summary="Edit, publish or unpublish a content page",
        description=(
            "Changing the words of a page that was ever published writes a new version and "
            "keeps the old one as history; until `published: true` is sent the live version "
            "stays as it was (`hasUnpublishedChanges`). `published: false` takes the page "
            "offline. Omitted fields keep their value."
        ),
        request=AdminContentPageUpdateRequestSerializer,
        responses={
            200: AdminContentPageSerializer,
            400: VALIDATION_400,
            **protected(),
            404: NOT_FOUND_404,
        },
    )
    def put(self, request: Any, slug: str) -> Response:
        require_permission(self, request, MANAGE)
        payload = AdminContentPageUpdateRequestSerializer(data=request.data, partial=True)
        payload.is_valid(raise_exception=True)
        try:
            rows = update_page(
                actor=request.user,
                key=self._key(slug),
                data=dict(payload.validated_data),
                request_id=_request_id(request),
            )
        except LegalDocument.DoesNotExist as exc:
            raise NotFound() from exc
        except DjangoValidationError as exc:
            raise _django_error(exc) from exc
        return Response(page_payload(rows))

    @extend_schema(
        operation_id="adminContentPageDelete",
        tags=["Admin Content"],
        summary="Delete a content page and all its versions",
        description="409 CONTENT_PAGE_BUILT_IN for the six built-in pages: unpublish them.",
        responses={204: None, **protected(), 404: NOT_FOUND_404, 409: CONFLICT_409},
    )
    def delete(self, request: Any, slug: str) -> Response:
        require_permission(self, request, MANAGE)
        try:
            delete_page(actor=request.user, key=self._key(slug), request_id=_request_id(request))
        except LegalDocument.DoesNotExist as exc:
            raise NotFound() from exc
        return Response(status=204)


# -------------------------------------------------------------------------------- faq


def _faq_payload(entry: FaqEntry) -> dict[str, Any]:
    return {
        "id": str(entry.pk),
        "questionAr": entry.question_ar,
        "answerAr": entry.answer_ar,
        "sortOrder": entry.sort_order,
        "published": entry.published,
        "updatedAt": entry.updated_at.isoformat(),
    }


FAQ_FIELDS = {
    "questionAr": "question_ar",
    "answerAr": "answer_ar",
    "sortOrder": "sort_order",
    "published": "published",
}


def _faq_snapshot(entry: FaqEntry) -> dict[str, Any]:
    return {wire: getattr(entry, column) for wire, column in FAQ_FIELDS.items()}


class FaqEntryListView(AdminView):
    required_permission = READ

    @extend_schema(
        operation_id="adminFaqEntriesList",
        tags=["Admin Content"],
        summary="List FAQ entries, published or not",
        responses={200: AdminFaqEntryListSerializer, **protected()},
    )
    def get(self, request: Any) -> Response:
        rows = FaqEntry.objects.order_by("sort_order", "created_at")
        return Response({"items": [_faq_payload(row) for row in rows]})

    @extend_schema(
        operation_id="adminFaqEntryCreate",
        tags=["Admin Content"],
        summary="Add a FAQ entry",
        request=AdminFaqEntryRequestSerializer,
        responses={201: AdminFaqEntrySerializer, 400: VALIDATION_400, **protected()},
    )
    def post(self, request: Any) -> Response:
        require_permission(self, request, MANAGE)
        payload = AdminFaqEntryRequestSerializer(data=request.data)
        payload.is_valid(raise_exception=True)
        entry = FaqEntry.objects.create(
            **{column: payload.validated_data[wire] for wire, column in FAQ_FIELDS.items()}
        )
        record_audit(
            actor=request.user,
            action="faq_entry.created",
            target=entry,
            after_snapshot=_faq_snapshot(entry),
            request_id=_request_id(request),
        )
        return Response(_faq_payload(entry), status=201)


class FaqEntryDetailView(AdminView):
    required_permission = MANAGE

    @extend_schema(
        operation_id="adminFaqEntryUpdate",
        tags=["Admin Content"],
        summary="Edit, reorder, publish or unpublish a FAQ entry",
        description="Omitted fields keep their value.",
        request=AdminFaqEntryRequestSerializer,
        responses={
            200: AdminFaqEntrySerializer,
            400: VALIDATION_400,
            **protected(),
            404: NOT_FOUND_404,
        },
    )
    def put(self, request: Any, entry_id: Any) -> Response:
        entry = get_object_or_404(FaqEntry, pk=entry_id)
        payload = AdminFaqEntryRequestSerializer(data=request.data, partial=True)
        payload.is_valid(raise_exception=True)
        before = _faq_snapshot(entry)
        for wire, column in FAQ_FIELDS.items():
            if wire in request.data and wire in payload.validated_data:
                setattr(entry, column, payload.validated_data[wire])
        entry.save()
        record_audit(
            actor=request.user,
            action="faq_entry.updated",
            target=entry,
            before_snapshot=before,
            after_snapshot=_faq_snapshot(entry),
            request_id=_request_id(request),
        )
        return Response(_faq_payload(entry))

    @extend_schema(
        operation_id="adminFaqEntryDelete",
        tags=["Admin Content"],
        summary="Delete a FAQ entry",
        responses={204: None, **protected(), 404: NOT_FOUND_404},
    )
    def delete(self, request: Any, entry_id: Any) -> Response:
        entry = get_object_or_404(FaqEntry, pk=entry_id)
        record_audit(
            actor=request.user,
            action="faq_entry.deleted",
            target=entry,
            before_snapshot=_faq_snapshot(entry),
            request_id=_request_id(request),
        )
        entry.delete()
        return Response(status=204)


# ------------------------------------------------------------------ emergency numbers


def _number_payload(number: EmergencyNumber) -> dict[str, Any]:
    return {
        "id": str(number.pk),
        "provinceId": str(number.province_id) if number.province_id else None,
        "provinceNameAr": number.province.name_ar if number.province else None,
        "labelAr": number.label_ar,
        "phone": number.phone,
        "kind": number.kind,
        "sortOrder": number.sort_order,
        "active": number.active,
        "adminNote": number.admin_note,
        "updatedAt": number.updated_at.isoformat(),
    }


NUMBER_FIELDS = {
    "labelAr": "label_ar",
    "phone": "phone",
    "kind": "kind",
    "sortOrder": "sort_order",
    "active": "active",
    "adminNote": "admin_note",
}


def _number_snapshot(number: EmergencyNumber) -> dict[str, Any]:
    snapshot = {wire: getattr(number, column) for wire, column in NUMBER_FIELDS.items()}
    snapshot["provinceId"] = str(number.province_id) if number.province_id else None
    return snapshot


def _apply_province(number: EmergencyNumber, data: dict[str, Any]) -> None:
    if "provinceId" not in data:
        return
    province_id = data["provinceId"]
    if province_id is None:
        number.province = None
        return
    province = Province.objects.filter(pk=province_id).first()
    if province is None:
        raise ValidationError({"provinceId": ["Unknown province."]})
    number.province = province


class EmergencyNumberListView(AdminView):
    required_permission = READ

    @extend_schema(
        operation_id="adminEmergencyNumbersList",
        tags=["Admin Content"],
        summary="List emergency numbers, national and provincial, active or not",
        parameters=[
            OpenApiParameter(
                "provinceId",
                str,
                OpenApiParameter.QUERY,
                required=False,
                description="Keep this province's numbers; `national` keeps national ones.",
            )
        ],
        responses={200: AdminEmergencyNumberListSerializer, **protected()},
    )
    def get(self, request: Any) -> Response:
        rows = EmergencyNumber.objects.select_related("province").order_by(
            "province__sort_order", "sort_order", "label_ar"
        )
        if value := request.query_params.get("provinceId"):
            rows = (
                rows.filter(province__isnull=True)
                if value == "national"
                else rows.filter(province_id=value)
            )
        return Response({"items": [_number_payload(row) for row in rows]})

    @extend_schema(
        operation_id="adminEmergencyNumberCreate",
        tags=["Admin Content"],
        summary="Add an emergency number",
        description="No `provinceId` (or null) makes it national.",
        request=AdminEmergencyNumberRequestSerializer,
        responses={201: AdminEmergencyNumberSerializer, 400: VALIDATION_400, **protected()},
    )
    def post(self, request: Any) -> Response:
        require_permission(self, request, MANAGE)
        payload = AdminEmergencyNumberRequestSerializer(data=request.data)
        payload.is_valid(raise_exception=True)
        data = payload.validated_data
        number = EmergencyNumber(
            **{column: data[wire] for wire, column in NUMBER_FIELDS.items() if wire in data}
        )
        _apply_province(number, data)
        try:
            number.full_clean()
        except DjangoValidationError as exc:
            raise _django_error(exc) from exc
        number.save()
        record_audit(
            actor=request.user,
            action="emergency_number.created",
            target=number,
            after_snapshot=_number_snapshot(number),
            request_id=_request_id(request),
        )
        return Response(_number_payload(number), status=201)


class EmergencyNumberDetailView(AdminView):
    required_permission = MANAGE

    @extend_schema(
        operation_id="adminEmergencyNumberUpdate",
        tags=["Admin Content"],
        summary="Edit, move, reorder or deactivate an emergency number",
        description="Omitted fields keep their value; `provinceId: null` makes it national.",
        request=AdminEmergencyNumberRequestSerializer,
        responses={
            200: AdminEmergencyNumberSerializer,
            400: VALIDATION_400,
            **protected(),
            404: NOT_FOUND_404,
        },
    )
    def put(self, request: Any, number_id: Any) -> Response:
        number = get_object_or_404(EmergencyNumber.objects.select_related("province"), pk=number_id)
        payload = AdminEmergencyNumberRequestSerializer(data=request.data, partial=True)
        payload.is_valid(raise_exception=True)
        data = payload.validated_data
        before = _number_snapshot(number)
        for wire, column in NUMBER_FIELDS.items():
            if wire in request.data and wire in data:
                setattr(number, column, data[wire])
        _apply_province(number, data)
        try:
            number.full_clean()
        except DjangoValidationError as exc:
            raise _django_error(exc) from exc
        number.save()
        record_audit(
            actor=request.user,
            action="emergency_number.updated",
            target=number,
            before_snapshot=before,
            after_snapshot=_number_snapshot(number),
            request_id=_request_id(request),
        )
        return Response(_number_payload(number))

    @extend_schema(
        operation_id="adminEmergencyNumberDelete",
        tags=["Admin Content"],
        summary="Delete an emergency number",
        responses={204: None, **protected(), 404: NOT_FOUND_404},
    )
    def delete(self, request: Any, number_id: Any) -> Response:
        number = get_object_or_404(EmergencyNumber, pk=number_id)
        record_audit(
            actor=request.user,
            action="emergency_number.deleted",
            target=number,
            before_snapshot=_number_snapshot(number),
            request_id=_request_id(request),
        )
        number.delete()
        return Response(status=204)


# ------------------------------------------------------------------- contact messages


def _contact_payload(message: ContactMessage) -> dict[str, Any]:
    return {
        "id": str(message.pk),
        "name": message.name,
        "phone": message.phone or None,
        "message": message.message,
        "kind": message.kind,
        "userId": str(message.user_id) if message.user_id else None,
        "handled": message.handled_at is not None,
        "handledAt": message.handled_at.isoformat() if message.handled_at else None,
        "handledById": str(message.handled_by_id) if message.handled_by_id else None,
        "createdAt": message.created_at.isoformat(),
    }


class ContactCursorPage(CursorPage):
    ordering = ("-created_at", "id")


class ContactMessageListView(AdminView):
    required_permission = READ

    @extend_schema(
        operation_id="adminContactMessagesList",
        tags=["Admin Content"],
        summary="The contact inbox, newest first",
        parameters=[
            OpenApiParameter(
                "status",
                str,
                OpenApiParameter.QUERY,
                required=False,
                enum=["open", "handled"],
                description="`open` keeps unhandled messages, `handled` the rest.",
            ),
            OpenApiParameter(
                "kind",
                str,
                OpenApiParameter.QUERY,
                required=False,
                enum=ContactMessage.Kind.values,
            ),
            OpenApiParameter("cursor", str, OpenApiParameter.QUERY, required=False),
            OpenApiParameter("limit", int, OpenApiParameter.QUERY, required=False),
        ],
        responses={200: AdminContactMessagePageSerializer, 400: VALIDATION_400, **protected()},
    )
    def get(self, request: Any) -> Response:
        rows = ContactMessage.objects.order_by("-created_at", "id")
        status = request.query_params.get("status")
        if status == "open":
            rows = rows.filter(handled_at__isnull=True)
        elif status == "handled":
            rows = rows.filter(handled_at__isnull=False)
        elif status:
            raise ValidationError({"status": ["Use open or handled."]})
        if kind := request.query_params.get("kind"):
            rows = rows.filter(kind=kind.upper())
        paginator = ContactCursorPage()
        page = paginator.paginate_queryset(rows, request, view=self) or []
        return Response(paginator.get_paginated_payload([_contact_payload(m) for m in page]))


class ContactMessageHandleView(AdminView):
    required_permission = MANAGE

    @extend_schema(
        operation_id="adminContactMessageHandle",
        tags=["Admin Content"],
        summary="Mark a contact message handled",
        description="Idempotent: a handled message keeps who handled it and when.",
        request=AdminContactHandleRequestSerializer,
        responses={
            200: AdminContactMessageSerializer,
            400: VALIDATION_400,
            **protected(),
            404: NOT_FOUND_404,
        },
    )
    def post(self, request: Any, message_id: Any) -> Response:
        payload = AdminContactHandleRequestSerializer(data=request.data)
        payload.is_valid(raise_exception=True)
        message = get_object_or_404(ContactMessage, pk=message_id)
        if message.handled_at is None:
            message.handled_at = timezone.now()
            message.handled_by = request.user
            message.save(update_fields=["handled_at", "handled_by"])
            record_audit(
                actor=request.user,
                action="contact_message.handled",
                target=message,
                metadata={"note": payload.validated_data.get("note", "").strip()},
                request_id=_request_id(request),
            )
        return Response(_contact_payload(message))
