from __future__ import annotations

import ast
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
FACILITIES = ROOT / "facilities"


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def read(relative: str) -> str:
    return (ROOT / relative).read_text()


def check_owner_routes() -> None:
    urls = read("facilities/urls.py")
    for fragment in (
        "owner/config/",
        "owner/facilities/",
        "submit/",
        "location/",
        "images/",
        "evidence/",
        "members/",
    ):
        require(fragment in urls, f"owner route missing: {fragment}")
    root_urls = read("directory_backend/urls.py")
    require('include("facilities.urls")' in root_urls, "facilities owner routes not mounted")


def check_idor_and_membership() -> None:
    views = read("facilities/views.py")
    permissions = read("facilities/permissions.py")
    require("memberships__user=user" in views, "owner queryset is not membership scoped")
    require("require_facility_member" in views, "facility member guard missing")
    require("require_facility_owner" in views, "facility owner guard missing")
    require("FacilityMembership.objects.select_for_update()" in views, "member mutation lock missing")
    require("LAST_OWNER_PROTECTED" in views, "last-owner protection missing")
    require("FacilityMembership.objects.filter" in permissions, "membership permission query missing")
    require("facility=facility" in permissions and "user=user" in permissions, "membership scope missing")


def check_submission_policy() -> None:
    services = read("facilities/services.py")
    require(services.count("validate_owner_registration(") >= 3, "create/submit policy recheck missing")
    require("select_for_update()" in services, "facility submission/update lock missing")
    require("_required_evidence_complete" in services, "current evidence policy check missing")
    require("FacilityApplication.Status.SUBMITTED" in services, "submitted application guard missing")
    migration = read("facilities/migrations/0003_owner_media_integrity.py")
    require("uniq_submitted_application_per_facility_kind" in migration, "submitted uniqueness missing")
    require("condition=" in migration and "SUBMITTED" in migration, "partial submitted constraint missing")


def check_media_security() -> None:
    media = read("facilities/media.py")
    presenters = read("facilities/presenters.py")
    views = read("facilities/views.py")
    require("PrivateS3Storage" in media, "private evidence storage missing")
    require("PublicS3Storage" in media, "public image storage missing")
    require("Image.open" in media, "image decode validation missing")
    require("image.size" in media, "pixel/dimension preflight missing")
    require("image.load()" in media, "image decode missing")
    require("format=\"JPEG\"" in media, "safe image re-encode missing")
    require("uuid4" in media, "random object key missing")
    require("storage_key" not in presenters, "presenter leaks raw storage key")
    require('"storageKey"' not in views, "owner API leaks raw storage key")
    evidence_block = views[views.index("class OwnerFacilityEvidenceView"):]
    require("storage.url" not in evidence_block, "private evidence URL must not be returned")


def check_location_integrity() -> None:
    services = read("facilities/services.py")
    require('if "cityId" in data and city is None' in services, "city-clear neighborhood reset missing")
    require("City.objects.get" in services and "province=facility.province" in services, "city scope check missing")
    require("Neighborhood.objects.get" in services and "city=city" in services, "neighborhood scope check missing")


def check_python_integrity() -> None:
    files = [
        *FACILITIES.rglob("*.py"),
        ROOT / "directory_backend/urls.py",
    ]
    for path in files:
        ast.parse(path.read_text(), filename=str(path))
        if "migrations" not in path.parts:
            for line_no, line in enumerate(path.read_text().splitlines(), 1):
                require(len(line) <= 100, f"line >100: {path.relative_to(ROOT)}:{line_no}")


def main() -> int:
    checks = (
        check_owner_routes,
        check_idor_and_membership,
        check_submission_policy,
        check_media_security,
        check_location_integrity,
        check_python_integrity,
    )
    for check in checks:
        check()
        print(f"PASS {check.__name__}")
    print("PASS owner backend source qualification")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except AssertionError as exc:
        print(f"FAIL {exc}", file=sys.stderr)
        raise SystemExit(1)
