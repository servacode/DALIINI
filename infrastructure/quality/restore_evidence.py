#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
from datetime import datetime
from pathlib import Path

REQUIRED = {
    "backupId",
    "sourceEnvironment",
    "restoreTarget",
    "startedAt",
    "completedAt",
    "databaseConsistencyPass",
    "objectStorageConsistencyPass",
    "smokePass",
    "achievedRpoMinutes",
    "achievedRtoMinutes",
}


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("evidence")
    parser.add_argument("--max-rpo-minutes", type=float, default=60)
    parser.add_argument("--max-rto-minutes", type=float, default=120)
    args = parser.parse_args()
    data = json.loads(Path(args.evidence).read_text(encoding="utf-8"))
    missing = sorted(REQUIRED - set(data))
    errors = []
    if missing:
        errors.append("missing:" + ",".join(missing))
    for key in ("startedAt", "completedAt"):
        if key in data:
            datetime.fromisoformat(data[key].replace("Z", "+00:00"))
    if data.get("restoreTarget") == data.get("sourceEnvironment"):
        errors.append("restore target must be disposable and separate from source")
    for key in ("databaseConsistencyPass", "objectStorageConsistencyPass", "smokePass"):
        if data.get(key) is not True:
            errors.append(f"{key}=false")
    if float(data.get("achievedRpoMinutes", 10**9)) > args.max_rpo_minutes:
        errors.append("RPO exceeded")
    if float(data.get("achievedRtoMinutes", 10**9)) > args.max_rto_minutes:
        errors.append("RTO exceeded")
    print(json.dumps({"status": "FAIL" if errors else "PASS", "errors": errors}))
    return 1 if errors else 0


if __name__ == "__main__":
    raise SystemExit(main())
