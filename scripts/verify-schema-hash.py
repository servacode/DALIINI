"""Does openapi/schema.sha256 describe openapi/schema.yaml?

A separate file rather than a flag on write-openapi-hash.py, because the two intentions are
opposite: one records what the schema is, this one refuses when the record has gone stale.
"""

import hashlib
import pathlib
import sys

schema = pathlib.Path("openapi/schema.yaml")
record = pathlib.Path("openapi/schema.sha256")
if not schema.exists() or not record.exists():
    print("openapi/schema.yaml or openapi/schema.sha256 is missing")
    sys.exit(1)

recorded = record.read_text(encoding="utf-8").split()[0]
actual = hashlib.sha256(schema.read_bytes()).hexdigest()
if recorded != actual:
    print(f"recorded {recorded[:16]}… but the file hashes to {actual[:16]}…")
    sys.exit(1)
