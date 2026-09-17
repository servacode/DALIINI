#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import os
import ssl
import urllib.error
import urllib.parse
import urllib.request


class Client:
    def __init__(self, origin: str):
        self.origin = origin.rstrip("/")
        self.access = ""
        self.context = ssl.create_default_context()

    def request(
        self,
        method: str,
        path: str,
        body=None,
        *,
        token=True,
        expected=(200, 201, 202, 204),
    ):
        headers = {"Content-Type": "application/json", "X-E2E-Test": "directory-p20"}
        if token and self.access:
            headers["Authorization"] = f"Bearer {self.access}"
        data = None if body is None else json.dumps(body).encode()
        request = urllib.request.Request(
            self.origin + path,
            data=data,
            method=method,
            headers=headers,
        )
        try:
            with urllib.request.urlopen(request, context=self.context, timeout=20) as response:
                raw = response.read()
                payload = json.loads(raw) if raw else None
                if response.status not in expected:
                    raise RuntimeError(f"{method} {path}: unexpected {response.status}")
                return payload
        except urllib.error.HTTPError as exc:
            detail = exc.read().decode("utf-8", errors="replace")[:1000]
            raise RuntimeError(f"{method} {path}: HTTP {exc.code}: {detail}") from exc


def required_env(name: str) -> str:
    value = os.getenv(name, "").strip()
    if not value:
        raise RuntimeError(f"missing required environment variable: {name}")
    return value


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--origin", default=os.getenv("STAGING_API_ORIGIN", ""))
    parser.add_argument("--preflight-only", action="store_true")
    args = parser.parse_args()
    origin = args.origin.rstrip("/")
    if not origin:
        raise RuntimeError("STAGING_API_ORIGIN is required")
    host = urllib.parse.urlparse(origin).hostname or ""
    if not origin.startswith("https://") or ("staging" not in host and "onrender.com" not in host):
        raise RuntimeError("golden path refuses non-HTTPS/non-staging origin")
    client = Client(origin)
    live = client.request("GET", "/health/live/", token=False)
    ready = client.request("GET", "/health/ready/", token=False)
    print(json.dumps({"stage": "preflight", "live": live, "ready": ready}))
    if args.preflight_only:
        return 0

    owner_phone = required_env("E2E_OWNER_PHONE")
    owner_password = required_env("E2E_OWNER_PASSWORD")
    otp_code = required_env("E2E_OTP_CODE")
    admin_phone = required_env("E2E_ADMIN_PHONE")
    admin_password = required_env("E2E_ADMIN_PASSWORD")

    provinces = client.request("GET", "/api/v1/public/provinces/", token=False)["items"]
    if not provinces:
        raise RuntimeError("no active province")
    province_id = provinces[0]["id"]
    categories = client.request(
        "GET", f"/api/v1/public/provinces/{province_id}/categories/", token=False
    )["items"]
    if not categories:
        raise RuntimeError("no public category")
    category_id = categories[0]["id"]

    challenge = client.request(
        "POST",
        "/api/v1/auth/register/start/",
        {"displayName": "P20 Owner", "phone": owner_phone, "provinceId": province_id},
        token=False,
    )
    challenge_id = challenge["challengeId"]
    client.request(
        "POST",
        "/api/v1/auth/register/verify/",
        {"challengeId": challenge_id, "code": otp_code},
        token=False,
    )
    session = client.request(
        "POST",
        "/api/v1/auth/register/complete/",
        {"challengeId": challenge_id, "password": owner_password, "platform": "E2E"},
        token=False,
    )
    client.access = session["accessToken"]
    facility = client.request(
        "POST",
        "/api/v1/owner/facilities/",
        {"provinceId": province_id, "categoryId": category_id, "nameAr": "اختبار P20"},
    )
    facility_id = facility["id"]
    client.request(
        "PUT",
        f"/api/v1/owner/facilities/{facility_id}/location/",
        {"latitude": 35.95, "longitude": 39.01},
    )
    submitted = client.request("POST", f"/api/v1/owner/facilities/{facility_id}/submit/", {})

    admin = client.request(
        "POST",
        "/api/v1/auth/login/",
        {"phone": admin_phone, "password": admin_password, "platform": "E2E_ADMIN"},
        token=False,
    )
    client.access = admin["accessToken"]
    client.request(
        "POST",
        f"/api/v1/admin/applications/{submitted['applicationId']}/approve/",
        {"reason": "P20 automated golden path"},
    )

    public = client.request(
        "GET",
        f"/api/v1/public/facilities/{facility_id}/",
        token=False,
    )
    if public.get("id") != facility_id:
        raise RuntimeError("approved facility not visible publicly")

    print(
        json.dumps(
            {
                "status": "PASS",
                "facilityId": facility_id,
                "applicationId": submitted["applicationId"],
            }
        )
    )
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except Exception as exc:
        print(json.dumps({"status": "FAIL", "error": str(exc)}))
        raise SystemExit(1)
