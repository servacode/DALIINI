"""Reading OpenStreetMap's own words for a place, and deciding which of ours it is.

The platform's geography was a fixture: five rectangles laid over Raqqa so that a position would
resolve to something on a device. This is what replaces it — the administrative boundaries and the
named quarters that OpenStreetMap contributors have drawn for Syria, which are the real thing.

Nothing here touches a database or a geometry. It is the part that has to be right about names, and
names are where this goes wrong: the same governorate is written `محافظة الرقة` in the extract and
`الرقة` in this platform, `إدلب` with a hamza and `ادلب` without, `حماة` with a ta marbuta and
`حماه` with a ha. So a name is matched on its skeleton — no prefix, no diacritics, one spelling of
each letter that has several — and never on the string a contributor happened to type.

The tagging this reads, in OpenStreetMap's terms for Syria:

* `boundary=administrative` with `admin_level=4` is a governorate (محافظة); there are fourteen and
  they are this platform's provinces.
* `admin_level=5` is a district (منطقة). Sixty-seven of them, and they are what this platform calls
  a city: the name a Syrian gives the area — الرقة, تل أبيض, الطبقة — and the unit a reader is
  actually in.
* `admin_level=6` is a subdistrict (ناحية). Not imported: it sits between the two and would answer
  "ناحية مركز الرقة" where a reader expects "الرقة".
* `admin_level=10`, and `place` in `neighbourhood`, `suburb` or `quarter`, are quarters of a city
  (حي). Damascus's are drawn as boundaries, Aleppo's and Raqqa's as places; both are the same thing
  to a reader, so both are imported as neighbourhoods.
"""

from __future__ import annotations

import re
import unicodedata
import uuid
from typing import Any

#: The namespace every imported row's primary key is derived in, so that a second import updates
#: the same rows rather than making a second set. It is this app's own, fixed once.
NAMESPACE = uuid.UUID("f0c0f2f6-6b9a-5c1e-9b5b-4a0d1d1c9a10")

#: What OpenStreetMap calls each thing this platform keeps.
PROVINCE_LEVEL = "4"
CITY_LEVEL = "5"
NEIGHBOURHOOD_LEVEL = "10"
NEIGHBOURHOOD_PLACES = frozenset({"neighbourhood", "suburb", "quarter"})

#: Words that say what a place is rather than name it. A governorate's boundary is named
#: "محافظة الرقة"; this platform's province is named "الرقة", and the reader reads the second.
_KINDS = ("محافظة", "منطقة", "ناحية", "مدينة", "بلدة", "قرية")

_TATWEEL = "ـ"
_DIACRITICS = re.compile(r"[ً-ٰٟۖ-ۜ۟-۪ۨ-ۭ]")
_LETTER_VARIANTS = str.maketrans(
    {
        "أ": "ا",
        "إ": "ا",
        "آ": "ا",
        "ٱ": "ا",
        "ة": "ه",
        "ى": "ي",
        "ئ": "ي",
        "ؤ": "و",
        "ٶ": "و",
    }
)
#: Marks that carry direction rather than sound. One district in the extract is written with a
#: left-to-right mark inside it, which is invisible and would defeat every comparison.
_INVISIBLE = re.compile(r"[​-‏‪-‮⁦-⁩]")


def normalise_arabic(name: str | None) -> str:
    """A name reduced to what it sounds like, for comparing two spellings of the same place."""
    if not name:
        return ""
    text = unicodedata.normalize("NFKC", name)
    text = _INVISIBLE.sub("", text)
    text = _DIACRITICS.sub("", text)
    text = text.replace(_TATWEEL, "")
    text = text.translate(_LETTER_VARIANTS)
    text = re.sub(r"[^\w\s]", " ", text, flags=re.UNICODE)
    return re.sub(r"\s+", " ", text).strip().lower()


def strip_kind(name: str | None) -> str:
    """The place's own name, without the word that says what kind of place it is."""
    if not name:
        return ""
    text = _INVISIBLE.sub("", unicodedata.normalize("NFKC", name)).strip()
    for kind in _KINDS:
        if text.startswith(f"{kind} "):
            return text[len(kind) :].strip()
    return text


def match_key(name: str | None) -> str:
    """How a province in the extract is recognised as a province in this platform."""
    return normalise_arabic(strip_kind(name))


def english_name(other_tags: str | None) -> str | None:
    """`name:en` out of the field GDAL puts every tag it has no column for.

    The field is a string of `"key"=>"value"` pairs. A name with a quote in it would break this
    reading, so a value that looks truncated is dropped rather than half-imported.
    """
    if not other_tags:
        return None
    found = re.search(r'"name:en"=>"((?:[^"\\]|\\.)*)"', other_tags)
    if not found:
        return None
    value = found.group(1).replace('\\"', '"').strip()
    return value or None


def is_province(properties: dict[str, Any]) -> bool:
    if properties.get("boundary") != "administrative":
        return False
    return properties.get("admin_level") == PROVINCE_LEVEL


def is_city(properties: dict[str, Any]) -> bool:
    if properties.get("boundary") != "administrative":
        return False
    return properties.get("admin_level") == CITY_LEVEL


def is_neighbourhood(properties: dict[str, Any]) -> bool:
    """A quarter of a city, however the contributor chose to say so."""
    if properties.get("boundary") == "administrative":
        return properties.get("admin_level") == NEIGHBOURHOOD_LEVEL
    return properties.get("place") in NEIGHBOURHOOD_PLACES


def osm_identity(properties: dict[str, Any]) -> str | None:
    """What makes an imported row that row, across re-runs of the import.

    OpenStreetMap's own id: a relation's, or the closed way's when the feature is one. A feature
    with neither is skipped rather than given a key derived from its name, because two quarters of
    Raqqa are both called البعث and a name is not an identity.
    """
    relation = properties.get("osm_id")
    way = properties.get("osm_way_id")
    if relation:
        return f"relation:{relation}"
    if way:
        return f"way:{way}"
    return None


def row_id(kind: str, identity: str) -> uuid.UUID:
    """The primary key for an imported place, derived from what it is in OpenStreetMap."""
    return uuid.uuid5(NAMESPACE, f"osm:{kind}:{identity}")


def city_code(identity: str) -> str:
    """A city's code, which must be unique within its province and mean something to a reader.

    It names its source: `osm-relation-1234567` says where to look when a boundary is wrong, which
    a slug invented here would not.
    """
    return f"osm-{identity.replace(':', '-')}"
