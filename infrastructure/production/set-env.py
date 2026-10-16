#!/usr/bin/env python3
"""Write NAME=value lines read from stdin into a server's env file, in place (DECISION-121).

    ... | set-env.py /srv/daliini/production.env

The deploy workflow pipes in the settings only the owner can supply, kept as the GitHub
environment's secrets, so they reach the server without passing through anyone's machine. A name
already in the file has its line replaced; a new one is appended. Nothing is printed but names.
"""

from __future__ import annotations

import os
import sys
from pathlib import Path


def main() -> int:
    path = Path(sys.argv[1])
    wanted: dict[str, str] = {}
    for raw in sys.stdin.read().splitlines():
        name, sep, value = raw.partition("=")
        if sep and name.strip():
            wanted[name.strip()] = value
    if not wanted:
        return 0
    lines = path.read_text(encoding="utf-8").splitlines()
    done: set[str] = set()
    for index, line in enumerate(lines):
        name = line.partition("=")[0]
        if name in wanted and not line.startswith("#"):
            lines[index] = f"{name}={wanted[name]}"
            done.add(name)
    lines += [f"{name}={value}" for name, value in wanted.items() if name not in done]
    temporary = path.with_suffix(".tmp")
    descriptor = os.open(temporary, os.O_WRONLY | os.O_CREAT | os.O_TRUNC, 0o600)
    with os.fdopen(descriptor, "w", encoding="utf-8") as handle:
        handle.write("\n".join(lines) + "\n")
    temporary.replace(path)
    print("settings written:", " ".join(sorted(wanted)))
    return 0


if __name__ == "__main__":
    sys.exit(main())
