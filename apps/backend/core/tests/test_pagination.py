"""INT-035: cursor pages are `{items, nextCursor, hasMore}` and the cursor is opaque.

The five cases the contract has to survive are a first page, a middle page, a last page, a
mangled cursor, and walking the whole collection without losing or repeating a row.
"""

from typing import Any

import pytest
from rest_framework.test import APIClient

from directory.models import CategoryProvince
from facilities.models import Facility

PAGE = 4
TOTAL = 11  # three pages at PAGE=4: 4, 4, 3.

ENVELOPE_KEYS = {"items", "nextCursor", "hasMore"}


@pytest.fixture
def listing(facility: Facility) -> Facility:
    """A category visible in its province plus TOTAL active facilities in it."""
    CategoryProvince.objects.update_or_create(
        category=facility.category,
        province=facility.province,
        defaults={"public_enabled": True},
    )
    facility.name_ar = "منشأة 00"
    facility.save(update_fields=["name_ar"])
    Facility.objects.bulk_create(
        Facility(
            category=facility.category,
            province=facility.province,
            name_ar=f"منشأة {index:02d}",
            status=Facility.Status.ACTIVE,
        )
        for index in range(1, TOTAL)
    )
    return facility


def _get(client: APIClient, facility: Facility, **params: Any) -> dict[str, Any]:
    query = {
        "provinceId": str(facility.province_id),
        "categoryId": str(facility.category_id),
        "limit": PAGE,
        **params,
    }
    response = client.get("/api/v1/public/facilities/", query)
    assert response.status_code == 200, response.content
    body: dict[str, Any] = response.json()
    return body


@pytest.mark.django_db
def test_first_page_carries_the_contract_envelope(listing: Facility) -> None:
    body = _get(APIClient(), listing)

    assert set(body) == ENVELOPE_KEYS
    assert len(body["items"]) == PAGE
    assert body["hasMore"] is True
    assert body["nextCursor"]


@pytest.mark.django_db
def test_the_cursor_is_a_token_and_not_a_url(listing: Facility) -> None:
    cursor = _get(APIClient(), listing)["nextCursor"]

    assert "://" not in cursor
    assert "testserver" not in cursor
    assert "/api/" not in cursor
    assert "limit" not in cursor
    assert "provinceId" not in cursor


@pytest.mark.django_db
def test_middle_page_still_reports_more(listing: Facility) -> None:
    client = APIClient()
    first = _get(client, listing)

    middle = _get(client, listing, cursor=first["nextCursor"])

    assert len(middle["items"]) == PAGE
    assert middle["hasMore"] is True
    assert middle["nextCursor"] != first["nextCursor"]


@pytest.mark.django_db
def test_last_page_closes_the_cursor(listing: Facility) -> None:
    client = APIClient()
    body = _get(client, listing)
    body = _get(client, listing, cursor=body["nextCursor"])

    last = _get(client, listing, cursor=body["nextCursor"])

    assert len(last["items"]) == TOTAL - 2 * PAGE
    assert last["hasMore"] is False
    assert last["nextCursor"] is None


@pytest.mark.django_db
def test_walking_every_page_yields_each_row_exactly_once(listing: Facility) -> None:
    client = APIClient()
    seen: list[str] = []
    cursor: str | None = None
    for _ in range(10):
        body = _get(client, listing, **({"cursor": cursor} if cursor else {}))
        seen.extend(item["id"] for item in body["items"])
        cursor = body["nextCursor"]
        if not cursor:
            break

    assert len(seen) == TOTAL
    assert len(set(seen)) == TOTAL
    names = list(Facility.objects.order_by("name_ar", "id").values_list("id", flat=True))
    assert seen == [str(value) for value in names]


@pytest.mark.django_db
def test_a_mangled_cursor_is_a_field_error_not_a_missing_collection(listing: Facility) -> None:
    response = APIClient().get(
        "/api/v1/public/facilities/",
        {
            "provinceId": str(listing.province_id),
            "categoryId": str(listing.category_id),
            "cursor": "not-a-real-cursor",
        },
    )

    assert response.status_code == 400
    body = response.json()
    assert body["code"] == "VALIDATION_ERROR"
    assert "cursor" in body["details"]
