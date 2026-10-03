"""The review queue: applications, their decisions, and the private evidence behind them."""

import mimetypes
from pathlib import PurePosixPath
from typing import Any
from uuid import UUID

from django.core.exceptions import ObjectDoesNotExist
from django.core.exceptions import ValidationError as DjangoValidationError
from django.db.models.functions import Coalesce
from django.http import FileResponse
from django.shortcuts import get_object_or_404
from drf_spectacular.types import OpenApiTypes
from drf_spectacular.utils import (
    OpenApiResponse,
    extend_schema,
    extend_schema_view,
)
from rest_framework.exceptions import NotFound, ValidationError
from rest_framework.response import Response

from accounts.authentication import AuthenticatedRequest
from audit.models import AuditEvent
from audit.services import record_audit
from core.openapi import NOT_FOUND_404, VALIDATION_400, protected
from core.pagination import QueryOrderedCursorPage, page_parameters
from facilities.changes import review_snapshots
from facilities.models import FacilityApplication, VerificationEvidence
from storage.backends import PrivateS3Storage
from storage.public_media import public_media_url

from .review import find_duplicates, missing_evidence, previous_snapshot, with_evidence_state
from .schemas import AdminApplicationDetailSerializer as AppDetail
from .schemas import AdminApplicationListSerializer as AppList
from .schemas import AdminApplicationSerializer as App
from .schemas import (
    AdminAuditTrailEntrySerializer,
    AdminReviewDecisionRequestSerializer,
)
from .serializers import (
    application_payload,
    facility_payload,
    location_payload,
    with_application_names,
)
from .services import (
    decide_application,
)
from .views import (
    AdminView,
    _filter,
    _page,
    _parse_bound,
    _request_id,
    _validation_error,
    logger,
)


class ApplicationListView(AdminView):
    required_permission = "admin.reviews.read"

    @extend_schema(
        operation_id="adminReviewsList",
        tags=["Admin Reviews"],
        summary="List facility applications awaiting or past review",
        description=(
            "Newest submission first, in cursor pages. Every filter is optional and combines "
            "with the rest. A draft that was never submitted sorts by when it was started."
        ),
        parameters=[
            *page_parameters(QueryOrderedCursorPage),
            _filter("kind", "Application kind, for example REGISTRATION or REVERIFICATION."),
            _filter("status", "Application status, for example SUBMITTED or APPROVED."),
            _filter("province", "Province id of the facility the application belongs to."),
            _filter("category", "Category id of the facility the application belongs to."),
            _filter(
                "from",
                "Submitted on or after this day (YYYY-MM-DD, Damascus) or this ISO datetime.",
            ),
            _filter(
                "to",
                "Submitted on or before this day (YYYY-MM-DD, Damascus) or before this datetime.",
            ),
            _filter(
                "evidence",
                "`complete` or `incomplete`: whether every required document is uploaded.",
            ),
        ],
        responses={200: AppList, 400: VALIDATION_400, **protected()},
    )
    def get(self, request: AuthenticatedRequest) -> Response:
        qs = (
            with_evidence_state(with_application_names(FacilityApplication.objects.all()))
            # A cursor needs a value on every row, and a draft has no submission time yet.
            .annotate(sort_at=Coalesce("submitted_at", "created_at"))
            .order_by("-sort_at", "-id")
        )
        for field, param in (("kind", "kind"), ("status", "status")):
            if value := request.query_params.get(param):
                qs = qs.filter(**{field: value})
        if value := request.query_params.get("province"):
            qs = qs.filter(facility__province_id=value)
        if value := request.query_params.get("category"):
            qs = qs.filter(facility__category_id=value)
        if value := request.query_params.get("from"):
            qs = qs.filter(submitted_at__gte=_parse_bound(value, "from", end=False))
        if value := request.query_params.get("to"):
            qs = qs.filter(submitted_at__lt=_parse_bound(value, "to", end=True))
        evidence = request.query_params.get("evidence")
        if evidence == "complete":
            qs = qs.filter(~missing_evidence())
        elif evidence == "incomplete":
            qs = qs.filter(missing_evidence())
        elif evidence:
            raise ValidationError({"evidence": "Expected `complete` or `incomplete`."})
        return _page(request, qs, application_payload)


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
    def get(self, request: AuthenticatedRequest, application_id: UUID) -> Response:
        item = get_object_or_404(
            with_application_names(FacilityApplication.objects.all()), pk=application_id
        )
        facility = item.facility
        waiting_change = (
            item.kind == FacilityApplication.Kind.CHANGE
            and item.status == FacilityApplication.Status.SUBMITTED
        )
        snapshot: dict[str, Any]
        previous: dict[str, Any] | None
        if waiting_change:
            snapshot, previous = review_snapshots(item)
        else:
            snapshot, previous = item.snapshot, previous_snapshot(item)
        # A claim is decided on the claimant's own documents; any other application on the
        # facility's, which never include a pending claimant's.
        evidence = (
            item.claim_evidence.select_related("requirement")
            if item.kind == FacilityApplication.Kind.CLAIM
            else facility.evidence.filter(application__isnull=True).select_related("requirement")
        )
        images = list(facility.images.order_by("sort_order", "created_at"))
        return Response(
            {
                **application_payload(item),
                "facility": facility_payload(facility),
                "snapshot": snapshot,
                "previous": previous,
                "proposedFields": sorted(item.proposed_changes or {}),
                "revision": item.revision,
                "location": location_payload(facility),
                "duplicates": find_duplicates(facility),
                "publicImageIds": [str(image.id) for image in images],
                "publicImages": [
                    {"id": str(image.id), "url": public_media_url(image.storage_key)}
                    for image in images
                ],
                "evidence": [
                    {
                        "id": str(row.id),
                        "requirementId": row.requirement_id,
                        "labelAr": row.requirement.label_ar,
                    }
                    for row in evidence
                ],
                "audit": AdminAuditTrailEntrySerializer(
                    AuditEvent.objects.filter(target_id=str(item.id))
                    .order_by("-created_at")
                    .values("action", "request_id", "created_at")[:50],
                    many=True,
                ).data,
            }
        )


class ApplicationDecisionView(AdminView):
    required_permission = "admin.reviews.decide"
    approve = False

    @extend_schema(
        operation_id="adminReviewDecide",
        tags=["Admin Reviews"],
        summary="Decide an application",
        request=AdminReviewDecisionRequestSerializer,
        responses={200: App, 400: VALIDATION_400, **protected(), 404: NOT_FOUND_404},
    )
    def post(self, request: AuthenticatedRequest, application_id: UUID) -> Response:
        try:
            payload = AdminReviewDecisionRequestSerializer(data=request.data)
            payload.is_valid(raise_exception=True)
            item = decide_application(
                request=request,
                application_id=application_id,
                approve=self.approve,
                reason=str(payload.validated_data.get("reason", "")),
                revision=payload.validated_data.get("revision"),
            )
        except ObjectDoesNotExist as exc:
            # The service locks the row itself; an id that does not exist is the
            # caller's 404, not a server fault.
            raise NotFound() from exc
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
        request=AdminReviewDecisionRequestSerializer,
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
        request=AdminReviewDecisionRequestSerializer,
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
    def get(self, request: AuthenticatedRequest, evidence_id: UUID) -> FileResponse:
        evidence = get_object_or_404(VerificationEvidence, pk=evidence_id)
        record_audit(
            actor=request.user,
            action="verification_evidence.viewed",
            target=evidence,
            metadata={"facilityId": str(evidence.facility_id)},
            request_id=_request_id(request),
        )
        logger.info(
            "evidence.accessed",
            extra={
                "evidence_id": str(evidence.pk),
                "facility_id": str(evidence.facility_id),
                "actor_id": str(request.user.pk),
            },
        )
        try:
            stream = PrivateS3Storage().open(evidence.storage_key, "rb")
        except Exception:
            logger.exception("evidence.open_failed", extra={"evidence_id": str(evidence.pk)})
            raise
        # Evidence is re-encoded to JPEG on upload, and the stored name says so. Serving it as
        # octet-stream told the operator's browser nothing about what it received (INT-066).
        content_type = mimetypes.guess_type(evidence.storage_key)[0] or "application/octet-stream"
        # FileResponse would otherwise name the download after the stream, and an S3 file is
        # named by its object key: the file name must say nothing about where it is stored.
        suffix = PurePosixPath(evidence.storage_key).suffix
        response = FileResponse(
            stream, content_type=content_type, filename=f"evidence-{evidence.pk}{suffix}"
        )
        response["Cache-Control"] = "private, no-store"
        response["X-Content-Type-Options"] = "nosniff"
        return response
