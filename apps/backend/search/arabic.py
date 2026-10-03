"""Arabic text as people type it, matched against Arabic text as owners wrote it.

The same name is written several ways, and all of them are correct to the person typing:
«صيدلية» and «صيدليه», «أحمد» and «احمد», «مستشفى» and «مستشفي», with or without
vowel marks, with or without a stretched letter. A search that compares raw characters finds
only the spelling the owner happened to choose, which is a search that fails on the first try.

Both sides are folded to one form before they are compared: the term here, in Python, and the
stored column in PostgreSQL, by `directory_normalize_ar`, which migration
`search/0001_arabic_normalize` creates from the same two tables below. A test holds the two
implementations to the same answers.
"""

from __future__ import annotations

import re
from typing import Any

from django.db.models import CharField, TextField
from django.db.models.lookups import Contains

#: Letters folded into the one form a search compares: every alef carrying a hamza or madda
#: into a bare alef, ta marbuta into ha, alef maqsura into ya, the hamza seats into their
#: carriers, and the Persian keyboard's ya and kaf into the Arabic letters they look like.
FOLD_FROM = "أإآٱةىؤئیک"
FOLD_TO = "ااااهيوييك"

#: Removed outright: tatweel, the harakat and tanween, shadda, sukun and the dagger alef.
STRIPPED = "ـً-ٰٟ"

_FOLD = str.maketrans(FOLD_FROM, FOLD_TO)
_STRIP = re.compile(f"[{STRIPPED}]")
_SPACES = re.compile(r"\s+")

#: The database function both sides agree on. Its body lives in the migration.
SQL_FUNCTION = "directory_normalize_ar"


def normalize_arabic(text: str) -> str:
    """Fold a string to the form searches compare: lower case, one letter per sound."""
    folded = _STRIP.sub("", text.lower().translate(_FOLD))
    return _SPACES.sub(" ", folded).strip()


class ArabicContains(Contains):
    """`field__ar_contains=term`: a substring match that ignores spelling variants.

    It is `contains` with both sides folded first. The column goes through the database
    function, and the term through `normalize_arabic` before it is escaped for LIKE, so a `%`
    or `_` typed by a person is still a literal character.
    """

    lookup_name = "ar_contains"

    def get_prep_lookup(self) -> Any:
        if isinstance(self.rhs, str):
            self.rhs = normalize_arabic(self.rhs)
        return super().get_prep_lookup()

    def process_lhs(
        self, compiler: Any, connection: Any, lhs: Any = None
    ) -> tuple[str, list[Any]]:
        sql, params = super().process_lhs(compiler, connection, lhs)
        return f"{SQL_FUNCTION}({sql})", list(params)

    def get_rhs_op(self, connection: Any, rhs: str) -> str:
        return str(connection.operators["contains"] % rhs)


def register() -> None:
    CharField.register_lookup(ArabicContains)
    TextField.register_lookup(ArabicContains)
