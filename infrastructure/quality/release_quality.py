#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import os
import shutil
import subprocess
import sys
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


def run(name: str, command: list[str], *, required=True) -> dict:
    proc = subprocess.run(command, cwd=ROOT, text=True, capture_output=True)
    result = {
        "name": name,
        "command": command,
        "status": "PASS" if proc.returncode == 0 else ("FAIL" if required else "UNAVAILABLE"),
        "returnCode": proc.returncode,
        "stdout": proc.stdout[-4000:],
        "stderr": proc.stderr[-4000:],
    }
    return result


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--require-connected", action="store_true")
    parser.add_argument("--evidence", default="artifacts/evidence/quality/p20-local.json")
    args = parser.parse_args()
    results = []
    results.append(
        run("source-security", [sys.executable, "infrastructure/quality/source_security.py"])
    )
    results.append(
        run("release-blockers", [sys.executable, "infrastructure/quality/release_blockers.py"])
    )
    results.append(run("governance", ["node", "scripts/check-governance.mjs"]))
    results.append(
        run("android-foundation", [sys.executable, "apps/android/scripts/qualify-source.py"])
    )
    results.append(
        run("android-public", [sys.executable, "apps/android/scripts/qualify-public-source.py"])
    )
    results.append(
        run("android-owner", [sys.executable, "apps/android/scripts/qualify-owner-source.py"])
    )
    results.append(
        run(
            "android-navigation",
            [sys.executable, "apps/android/scripts/qualify-navigation-source.py"],
        )
    )
    results.append(
        run(
            "android-live-data",
            [sys.executable, "apps/android/scripts/qualify-live-data-source.py"],
        )
    )
    results.append(
        run(
            "production-stack",
            [sys.executable, "infrastructure/scripts/qualify-production-stack.py"],
        )
    )
    results.append(run("git-whitespace", ["git", "diff", "--check"]))

    connected_requirements = {
        "backend-runtime": shutil.which("docker") is not None and bool(os.getenv("DATABASE_URL")),
        "admin-runtime": shutil.which("pnpm") is not None,
        "android-runtime": (
            shutil.which("gradle") is not None or (ROOT / "apps/android/gradlew").exists()
        ),
        "staging-golden-path": bool(os.getenv("STAGING_API_ORIGIN")),
        "restore-drill": bool(os.getenv("P20_RESTORE_EVIDENCE")),
    }
    for name, available in connected_requirements.items():
        results.append({
            "name": name,
            "status": (
                "PENDING"
                if available
                else ("FAIL" if args.require_connected else "UNAVAILABLE")
            ),
            "reason": (
                "connected execution must be run separately"
                if available
                else "required runtime/input unavailable"
            ),
        })

    failed = [r for r in results if r["status"] == "FAIL"]
    report = {
        "gate": "P20 RELEASE QUALITY PASS",
        "timestamp": datetime.now(timezone.utc).isoformat(),
        "commit": subprocess.check_output(
            ["git", "rev-parse", "HEAD"], cwd=ROOT, text=True
        ).strip(),
        "mode": "require-connected" if args.require_connected else "local-source",
        "overall": "FAIL" if failed else "SOURCE_QUALIFIED",
        "results": results,
    }
    target = ROOT / args.evidence
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(
        json.dumps(
            {
                "overall": report["overall"],
                "failed": [r["name"] for r in failed],
                "evidence": str(target.relative_to(ROOT)),
            }
        )
    )
    return 1 if failed else 0


if __name__ == "__main__":
    raise SystemExit(main())
