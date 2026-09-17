from __future__ import annotations

import hashlib
import sys
from pathlib import Path


def main() -> int:
    schema = Path(sys.argv[1])
    output = Path(sys.argv[2])
    digest = hashlib.sha256(schema.read_bytes()).hexdigest()
    output.write_text(f"{digest}  {schema.name}\n")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
