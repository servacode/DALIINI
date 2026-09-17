#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--ledger", default="artifacts/release-blockers.json")
    args = parser.parse_args()
    path = ROOT / args.ledger
    if not path.exists():
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text('{"issues": []}\n', encoding="utf-8")
    data = json.loads(path.read_text(encoding="utf-8"))
    issues = data.get("issues", [])
    open_critical = [
        issue
        for issue in issues
        if issue.get("status") != "CLOSED" and issue.get("severity") in {"P0", "P1"}
    ]
    print(json.dumps({"openP0P1": len(open_critical), "issues": open_critical}, ensure_ascii=False))
    return 1 if open_critical else 0


if __name__ == "__main__":
    raise SystemExit(main())
