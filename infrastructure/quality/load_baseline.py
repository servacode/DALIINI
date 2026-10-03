#!/usr/bin/env python3
from __future__ import annotations

import argparse
import concurrent.futures
import json
import statistics
import time
import urllib.error
import urllib.parse
import urllib.request


def fetch(url: str, timeout: float) -> tuple[bool, float, int]:
    started = time.perf_counter()
    try:
        with urllib.request.urlopen(url, timeout=timeout) as response:
            response.read(256)
            ok = 200 <= response.status < 400
            status = response.status
    except urllib.error.HTTPError as exc:
        ok, status = False, exc.code
    except Exception:
        ok, status = False, 0
    return ok, (time.perf_counter() - started) * 1000, status


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--origin", required=True)
    parser.add_argument("--path", action="append", default=["/health/live/"])
    parser.add_argument("--requests", type=int, default=100)
    parser.add_argument("--concurrency", type=int, default=10)
    parser.add_argument("--timeout", type=float, default=10)
    parser.add_argument("--max-error-rate", type=float, default=0.01)
    parser.add_argument("--max-p95-ms", type=float, default=1500)
    parser.add_argument("--allow-production", action="store_true")
    args = parser.parse_args()
    parsed = urllib.parse.urlparse(args.origin)
    host = parsed.hostname or ""
    if parsed.scheme != "https":
        raise SystemExit("load baseline requires HTTPS")
    if not args.allow_production and "staging" not in host:
        raise SystemExit("refusing load run against non-staging host")
    if args.requests < 1 or args.requests > 5000 or args.concurrency < 1 or args.concurrency > 100:
        raise SystemExit("bounded limits exceeded")
    urls = [args.origin.rstrip("/") + p for p in args.path]
    work = [urls[i % len(urls)] for i in range(args.requests)]
    with concurrent.futures.ThreadPoolExecutor(max_workers=args.concurrency) as pool:
        results = list(pool.map(lambda u: fetch(u, args.timeout), work))
    latencies = sorted(r[1] for r in results)
    failures = [r for r in results if not r[0]]
    p95_index = min(len(latencies) - 1, max(0, int(len(latencies) * 0.95) - 1))
    p95 = latencies[p95_index]
    error_rate = len(failures) / len(results)
    report = {
        "requests": len(results),
        "errors": len(failures),
        "errorRate": error_rate,
        "p50Ms": statistics.median(latencies),
        "p95Ms": p95,
        "maxMs": max(latencies),
    }
    print(json.dumps(report))
    return 1 if error_rate > args.max_error_rate or p95 > args.max_p95_ms else 0


if __name__ == "__main__":
    raise SystemExit(main())
