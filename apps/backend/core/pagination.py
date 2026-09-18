"""One cursor pagination envelope for the whole API.

`08-API-CONTRACT.md` specifies `{items, nextCursor, hasMore}`. DRF's `CursorPagination`
emits `{next, previous, results}` instead, where `next` is an absolute URL — that was
INT-035.

The URL is the reason this class exists rather than a per-view rename. `next` carries the
scheme, host, path and every query parameter the caller sent, so the response reflects
internal routing back at the client, a client that persists a page token persists a full
URL, and a host rewritten by a proxy leaks into stored state. Only the opaque cursor token
crosses the boundary here; the client sends it back as `?cursor=`.
"""

from typing import Any
from urllib.parse import parse_qs, urlparse

from rest_framework.exceptions import NotFound, ValidationError
from rest_framework.pagination import CursorPagination
from rest_framework.response import Response


class CursorPage(CursorPagination):
    """The project's cursor pagination. Subclasses set `ordering` and nothing else.

    `ordering` must end in a unique column. A cursor encodes a position in the ordering,
    so a non-unique ordering lets rows that compare equal shift between pages and a row is
    then served twice or skipped.
    """

    page_size = 30
    page_size_query_param = "limit"
    max_page_size = 100

    def get_paginated_response(self, data: Any) -> Response:
        return Response(self.get_paginated_payload(data))

    def get_paginated_payload(self, data: Any) -> dict[str, Any]:
        """Build the envelope body, for views that compose it into a larger response."""
        next_cursor = self._token(super().get_next_link())
        return {
            "items": data,
            "nextCursor": next_cursor,
            "hasMore": next_cursor is not None,
        }

    @staticmethod
    def _token(link: str | None) -> str | None:
        """Reduce DRF's absolute next-page URL to the opaque cursor value alone."""
        if not link:
            return None
        values = parse_qs(urlparse(link).query).get("cursor")
        return values[0] if values else None

    def decode_cursor(self, request: Any) -> Any:
        """Report a malformed cursor as a field error rather than a missing resource.

        DRF raises `NotFound` here, which tells the caller the collection does not exist
        when in fact one query parameter was mangled. The cursor is client input, so it is
        reported like any other invalid input.
        """
        try:
            return super().decode_cursor(request)
        except NotFound as exc:
            raise ValidationError(
                {"cursor": ["The cursor is not valid. Request the first page again."]}
            ) from exc

    def get_paginated_response_schema(self, schema: dict[str, Any]) -> dict[str, Any]:
        return {
            "type": "object",
            "required": ["items", "nextCursor", "hasMore"],
            "properties": {
                "items": schema,
                "nextCursor": {
                    "type": "string",
                    "nullable": True,
                    "description": (
                        "Opaque token for the next page, or null on the last page. Send it "
                        "back unchanged as the `cursor` query parameter."
                    ),
                },
                "hasMore": {
                    "type": "boolean",
                    "description": "True when `nextCursor` is set.",
                },
            },
        }
