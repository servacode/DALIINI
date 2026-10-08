from datetime import timedelta
from typing import Any

from django.db.models import Prefetch, QuerySet
from django.db.models.functions import Coalesce, Now
from django.utils import timezone

from facilities.models import Facility, FacilityApplication, FacilityMembership
from sessions.models import UserSession

from .review import evidence_complete


def _iso(value: Any) -> Any:
    return value.isoformat() if value else None


def user_rows(queryset: Any) -> Any:
    """Add every count a console row shows, in one query rather than three per row.

    Without this a page of fifty accounts asked the database a hundred and fifty extra
    questions. Subqueries rather than joins with `annotate(Count(...))`: two joins in one
    queryset multiply each other's rows and both counts come back wrong.
    """
    from django.db.models import Count, Exists, OuterRef, Subquery
    from django.db.models.functions import Coalesce

    from accounts.models import StaffTotpDevice

    def counted(model: Any, **extra: Any) -> Any:
        return Subquery(
            model.objects.filter(user=OuterRef("pk"), **extra)
            .order_by()
            .values("user")
            .annotate(n=Count("*"))
            .values("n")[:1]
        )

    return queryset.select_related("province").prefetch_related(
        # One query for the whole page, however many accounts are on it.
        Prefetch(
            "facility_memberships",
            queryset=FacilityMembership.objects.select_related("facility")
            .prefetch_related(_first_images())
            .order_by("facility__name_ar"),
            to_attr="card_memberships",
        )
    ).annotate(
        facility_count=counted(FacilityMembership),
        # Only an operator ever sets up an authenticator, so only an operator's card needs
        # the button that clears it. Without this every card carried an action that could
        # not apply to it.
        has_two_factor=Exists(StaffTotpDevice.objects.filter(user=OuterRef("pk"))),
        # The newest heartbeat of any live session. A session's `last_seen_at` is written
        # when its refresh rotates, which is at most once per access-token lifetime, so this
        # is accurate to about a quarter of an hour and is never «right now».
        #
        # Falling back to when the session was opened, because `last_seen_at` is null until
        # the first rotation: without it a card read «5 devices» and «never signed in from
        # any device» on the same line, which is not a detail — it is the card contradicting
        # itself. Signing in is itself the session proving who it is.
        seen_at=Subquery(
            UserSession.objects.filter(
                user=OuterRef("pk"), revoked_at__isnull=True, expires_at__gt=Now()
            )
            .annotate(seen=Coalesce("last_seen_at", "created_at"))
            .order_by("-seen")
            .values("seen")[:1]
        ),
    )


# An account counts as active when its newest session rotated within this window. The access
# token lives fifteen minutes, so a person using the app refreshes at least that often; twice
# that leaves room for one missed rotation without calling someone away who is not.
ACTIVE_WITHIN = timedelta(minutes=30)


def _count(user: Any, attr: str, model: Any, **extra: Any) -> int:
    """The annotated count when the list added one, else a direct count for a single row."""
    value = getattr(user, attr, None)
    if value is not None:
        return int(value)
    return int(model.objects.filter(user=user, **extra).count())


def _seen_at(user: Any) -> Any:
    """The annotated heartbeat when the list added one, else read it for this row alone.

    `hasattr` rather than a sentinel default: the annotation is legitimately None for an
    account with no live session, and a default could not tell that apart from «the list did
    not annotate», which would cost a query per row on every page.
    """
    if hasattr(user, "seen_at"):
        return user.seen_at
    return (
        UserSession.objects.filter(
            user=user, revoked_at__isnull=True, expires_at__gt=timezone.now()
        )
        .annotate(seen=Coalesce("last_seen_at", "created_at"))
        .order_by("-seen")
        .values_list("seen", flat=True)
        .first()
    )


CARD_FACILITIES = 2


def _first_images() -> Any:
    """Every facility's photographs in order, in one query for the whole page."""
    from facilities.models import FacilityImage

    return Prefetch(
        "facility__images",
        queryset=FacilityImage.objects.order_by("sort_order", "created_at"),
        to_attr="card_images",
    )


def _image_url(facility: Any) -> str | None:
    from storage.public_media import public_media_url

    images = getattr(facility, "card_images", None)
    if images is None:
        first = facility.images.order_by("sort_order", "created_at").first()
        return public_media_url(first.storage_key) if first else None
    return public_media_url(images[0].storage_key) if images else None


def _card_facilities(user: Any) -> list[dict[str, Any]]:
    """The first few places on the account, by name — what a card shows without growing."""
    links = getattr(user, "card_memberships", None)
    if links is None:
        links = list(
            FacilityMembership.objects.filter(user=user)
            .select_related("facility")
            .prefetch_related(_first_images())
            .order_by("facility__name_ar")[:CARD_FACILITIES]
        )
    return [
        {
            "id": str(link.facility_id),
            "nameAr": link.facility.name_ar,
            "role": link.role,
            "status": link.facility.status,
            "imageUrl": _image_url(link.facility),
        }
        for link in links[:CARD_FACILITIES]
    ]


def _has_two_factor(user: Any) -> bool:
    if hasattr(user, "has_two_factor"):
        return bool(user.has_two_factor)
    from accounts.models import StaffTotpDevice

    return StaffTotpDevice.objects.filter(user=user).exists()


def user_payload(user: Any) -> Any:
    seen_at = _seen_at(user)
    return {
        "id": str(user.id),
        "name": user.name,
        "phone": user.phone,
        "active": user.is_active,
        "provinceId": str(user.province_id) if user.province_id else None,
        # The name beside the id. The console shows accounts to a person, and a UUID tells
        # them nothing; without it every row would have to fetch the province list to
        # translate one field.
        "provinceName": user.province.name_ar if user.province_id else None,
        # Not whether the number was proved: registration proves it before the account
        # exists, so it is true of every account the app opened. The only accounts it is
        # false for are the ones this console opened, and «has not signed in yet» — which
        # `lastLoginAt` already says — tells an operator that more plainly.
        "lastLoginAt": _iso(user.last_login),
        "lastSeenAt": _iso(seen_at),
        # Said as «recently active», never as «online». Nothing in this system knows whether
        # an app is open; what it knows is when a session last proved itself.
        "recentlyActive": bool(seen_at and timezone.now() - seen_at <= ACTIVE_WITHIN),
        "facilityCount": _count(user, "facility_count", FacilityMembership),
        "facilities": _card_facilities(user),
        "hasTwoFactor": _has_two_factor(user),
        "createdAt": _iso(user.created_at),
        "updatedAt": _iso(user.updated_at),
    }


def user_facilities_payload(user: Any) -> list[dict[str, Any]]:
    """The places this account owns or helps run.

    An operator about to block an account needs to know what hangs off it. Three active
    pharmacies behind a name is the difference between a block and a phone call, and the
    console had no way to learn it: the facility list shows its owner, and nothing showed
    the reverse.
    """
    links = (
        FacilityMembership.objects.filter(user=user)
        .select_related("facility")
        .prefetch_related(_first_images())
        .order_by("facility__name_ar")
    )
    return [
        {
            "id": str(link.facility_id),
            "nameAr": link.facility.name_ar,
            "role": link.role,
            "status": link.facility.status,
            "imageUrl": _image_url(link.facility),
        }
        for link in links
    ]


def _owner_prefetch(prefix: str = "") -> Any:
    return Prefetch(
        f"{prefix}memberships",
        queryset=FacilityMembership.objects.filter(role=FacilityMembership.Role.OWNER)
        .select_related("user")
        .order_by("created_at"),
        to_attr="owner_links",
    )


def with_facility_names(queryset: QuerySet[Facility]) -> QuerySet[Facility]:
    """Everything `facility_payload` reads, fetched in a fixed number of queries."""
    return queryset.select_related("category", "province").prefetch_related(_owner_prefetch())


def with_application_names(
    queryset: QuerySet[FacilityApplication],
) -> QuerySet[FacilityApplication]:
    return queryset.select_related(
        "facility__category", "facility__province", "applicant"
    ).prefetch_related(
        _owner_prefetch("facility__")
    )


def _owner(facility: Any) -> Any:
    links = getattr(facility, "owner_links", None)
    if links is None:
        links = list(
            facility.memberships.filter(role=FacilityMembership.Role.OWNER)
            .select_related("user")
            .order_by("created_at")[:1]
        )
    return links[0].user if links else None


def _names(facility: Any) -> Any:
    owner = _owner(facility)
    return {
        "categoryNameAr": facility.category.name_ar,
        "provinceNameAr": facility.province.name_ar,
        "ownerName": owner.name if owner else None,
        "ownerPhone": owner.phone if owner else None,
    }


def location_payload(facility: Any) -> Any:
    if not facility.location:
        return None
    return {"latitude": facility.location.y, "longitude": facility.location.x}


def facility_payload(facility: Any) -> Any:
    return {
        "id": str(facility.id),
        "nameAr": facility.name_ar,
        "nameEn": facility.name_en or None,
        "categoryId": str(facility.category_id),
        "provinceId": str(facility.province_id),
        "cityId": str(facility.city_id) if facility.city_id else None,
        "status": facility.status,
        "location": location_payload(facility),
        "updatedAt": _iso(facility.updated_at),
        **_names(facility),
    }


def facility_detail_payload(facility: Any) -> Any:
    """One facility as the console reads and edits it: the row, its quality, every detail."""
    from .quality import quality_payload

    return {
        **facility_payload(facility),
        **quality_payload(facility),
        "neighborhoodId": str(facility.neighborhood_id) if facility.neighborhood_id else None,
        "descriptionAr": facility.description_ar or None,
        "descriptionEn": facility.description_en or None,
        "phone": facility.phone or None,
        "whatsapp": facility.whatsapp or None,
        "addressAr": facility.address_ar or None,
        "addressEn": facility.address_en or None,
        "specialtyIds": sorted(facility.specialty_links.values_list("specialty_id", flat=True)),
        "serviceTagIds": sorted(
            facility.service_links.values_list("service_tag_id", flat=True)
        ),
        "ownerCount": facility.memberships.filter(role=FacilityMembership.Role.OWNER).count(),
        "activatedAt": _iso(facility.activated_at),
        "lastVerifiedAt": _iso(facility.last_verified_at),
        "createdAt": _iso(facility.created_at),
    }


def application_payload(application: Any) -> Any:
    facility = application.facility
    return {
        "id": str(application.id),
        "facilityId": str(application.facility_id),
        "facilityNameAr": facility.name_ar,
        "kind": application.kind,
        "status": application.status,
        "provinceId": str(facility.province_id),
        "categoryId": str(facility.category_id),
        "submittedAt": _iso(application.submitted_at),
        "reviewedAt": _iso(application.reviewed_at),
        "rejectionReason": application.rejection_reason or None,
        "evidenceComplete": evidence_complete(application),
        **_names(facility),
        # A claim comes from somebody who is not a member yet; nobody else's does.
        "applicantName": application.applicant.name if application.applicant_id else None,
        "applicantPhone": application.applicant.phone if application.applicant_id else None,
    }
