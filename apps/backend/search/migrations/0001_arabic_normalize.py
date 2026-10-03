"""The database half of `search.arabic.normalize_arabic`.

The letter tables are copied here rather than imported, so that this migration means the same
thing forever; `search/tests/test_arabic_search.py` fails if the two ever disagree.
"""

from django.db import migrations

FOLD_FROM = "أإآٱةىؤئیک"
FOLD_TO = "ااااهيوييك"
STRIPPED = "ـً-ٰٟ"

CREATE = f"""
CREATE OR REPLACE FUNCTION directory_normalize_ar(value text) RETURNS text
LANGUAGE sql IMMUTABLE STRICT PARALLEL SAFE AS $$
    SELECT btrim(regexp_replace(
        regexp_replace(translate(lower(value), '{FOLD_FROM}', '{FOLD_TO}'), '[{STRIPPED}]', '', 'g'),
        '\\s+', ' ', 'g'
    ))
$$;
"""

DROP = "DROP FUNCTION IF EXISTS directory_normalize_ar(text);"


class Migration(migrations.Migration):
    initial = True
    dependencies: list[tuple[str, str]] = []

    operations = [migrations.RunSQL(CREATE, DROP)]
