"""Readable link words for a facility: `/f/{id}/صيدلية-الأمل` (DECISION-067).

The id stays the address — a facility renamed keeps every link that was ever shared — and the
words after it are for the people and search engines reading the link. They are the facility's
Arabic name as written (letters and digits of any script), with everything else turned into
single hyphens, so the slug of «صيدلية الأمل (الفرع ٢)» is `صيدلية-الأمل-الفرع-٢`.
"""

from __future__ import annotations

import re
import unicodedata

LONGEST = 80
_NOT_WORD = re.compile(r"[^\w]+", re.UNICODE)
# Arabic marks that read as nothing in a link: tashkeel and the tatweel.
_MARKS = re.compile("[ً-ٰٟـ]")


def facility_slug(name: str) -> str:
    text = unicodedata.normalize("NFKC", name or "")
    text = _MARKS.sub("", text).lower()
    slug = _NOT_WORD.sub("-", text).replace("_", "-").strip("-")
    slug = re.sub(r"-{2,}", "-", slug)
    if len(slug) > LONGEST:
        slug = slug[:LONGEST].rsplit("-", 1)[0] or slug[:LONGEST]
    return slug
