from __future__ import annotations

import hashlib
import sys
from pathlib import Path


def main() -> int:
    schema = Path(sys.argv[1])
    output = Path(sys.argv[2])
    digest = hashlib.sha256(schema.read_bytes()).hexdigest()
    # Write bytes rather than text. Path.write_text translates "\n" to "\r\n" on Windows,
    # so a digest file produced on a developer machine would differ from the one CI
    # produces and the drift gate would fail for a reason unrelated to the contract.
    output.write_bytes(f"{digest}  {schema.name}\n".encode())
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
