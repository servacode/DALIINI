"""Contract tests that stop the schema regressing to its previous state.

Before P10 the generated document carried 84 operations with zero component schemas,
zero request bodies, zero response schemas and no security scheme, so every generated
client would have been DTO-less. These tests fail if any of that returns.
"""

import collections
import hashlib
import io
import pathlib
from collections.abc import Iterator
from typing import Any

import pytest
import yaml
from django.core.management import call_command


def _repo_root() -> pathlib.Path:
    """Locate the directory that holds the canonical contract.

    Walking up rather than counting parents keeps the tests working both in the
    repository and inside the backend container, where the tree is mounted differently.
    """
    here = pathlib.Path(__file__).resolve()
    for parent in here.parents:
        if (parent / "openapi" / "schema.yaml").exists():
            return parent
    return here.parents[min(4, len(here.parents) - 1)]


REPO_ROOT = _repo_root()
CANONICAL_SCHEMA = REPO_ROOT / "openapi" / "schema.yaml"
CANONICAL_HASH = REPO_ROOT / "openapi" / "schema.sha256"

HTTP_METHODS = {"get", "post", "put", "patch", "delete", "head", "options", "trace"}
MUTATING = {"post", "put", "patch"}

# Endpoints that legitimately take no request body.
BODYLESS_MUTATIONS = {
    ("/api/v1/account/notifications/read-all/", "post"),
    ("/api/v1/account/notifications/{notification_id}/read/", "post"),
    ("/api/v1/admin/users/{user_id}/block/", "post"),
    ("/api/v1/admin/users/{user_id}/unblock/", "post"),
    ("/api/v1/auth/logout-all/", "post"),
    ("/api/v1/owner/facilities/{facility_id}/submit/", "post"),
    ("/api/v1/owner/facilities/{facility_id}/confirm-hours/", "post"),
    ("/api/v1/account/invitations/{invitation_id}/accept/", "post"),
    ("/api/v1/account/invitations/{invitation_id}/decline/", "post"),
    ("/api/v1/owner/claims/{claim_id}/submit/", "post"),
}

# Endpoints that legitimately answer 204 with no body.
NO_CONTENT_OPERATIONS = {
    ("/api/v1/account/password/", "post"),
    ("/api/v1/account/push-token/", "put"),
    ("/api/v1/account/push-token/unregister/", "post"),
    ("/api/v1/admin/ads/{advertisement_id}/", "delete"),
    ("/api/v1/admin/content/faq/{entry_id}/", "delete"),
    ("/api/v1/admin/content/pages/{slug}/", "delete"),
    ("/api/v1/admin/duty/{shift_id}/", "delete"),
    ("/api/v1/admin/emergency-numbers/{number_id}/", "delete"),
    ("/api/v1/admin/rejection-templates/{template_id}/", "delete"),
    ("/api/v1/admin/service-tags/{service_tag_id}/", "delete"),
    ("/api/v1/admin/specialties/{specialty_id}/", "delete"),
    ("/api/v1/admin/users/{user_id}/roles/", "put"),
    ("/api/v1/auth/logout-all/", "post"),
    ("/api/v1/auth/logout/", "post"),
    ("/api/v1/auth/recovery/reset/", "post"),
    ("/api/v1/auth/sessions/{session_id}/", "delete"),
    ("/api/v1/facilities/{facility_id}/rating/", "delete"),
    ("/api/v1/owner/facilities/{facility_id}/duty/{shift_id}/", "delete"),
    ("/api/v1/owner/facilities/{facility_id}/evidence/{evidence_id}/", "delete"),
    ("/api/v1/owner/facilities/{facility_id}/images/{image_id}/", "delete"),
    ("/api/v1/owner/facilities/{facility_id}/members/{user_id}/", "delete"),
    ("/api/v1/owner/facilities/{facility_id}/temporary-closures/{closure_id}/", "delete"),
    ("/api/v1/account/invitations/{invitation_id}/decline/", "post"),
    ("/api/v1/owner/claims/{claim_id}/", "delete"),
    ("/api/v1/owner/claims/{claim_id}/evidence/{evidence_id}/", "delete"),
    ("/api/v1/owner/facilities/{facility_id}/invitations/{invitation_id}/", "delete"),
}

PUBLIC_PREFIXES = ("/api/v1/public/",)
PUBLIC_EXTRA = {
    ("/api/v1/auth/login/", "post"),
    ("/api/v1/auth/refresh/", "post"),
    ("/api/v1/auth/register/start/", "post"),
    ("/api/v1/auth/register/verify/", "post"),
    ("/api/v1/auth/register/complete/", "post"),
    ("/api/v1/auth/recovery/start/", "post"),
    ("/api/v1/auth/recovery/verify/", "post"),
    ("/api/v1/auth/recovery/reset/", "post"),
    ("/api/v1/analytics/events/", "post"),
}

# Names that must never surface in the public contract.
FORBIDDEN_SCHEMA_TOKENS = [
    "otp_digest",
    "otpDigest",
    "refresh_digest",
    "refreshDigest",
    "previous_refresh_digest",
    "previousRefreshDigest",
    "password_hash",
    "passwordHash",
    "storage_key",
    "storageKey",
    "token_ciphertext",
    "tokenCiphertext",
    "identity_digest",
    "identityDigest",
    "SECRET_KEY",
    "S3_SECRET_ACCESS_KEY",
]


def generate_schema() -> str:
    buffer = io.StringIO()
    call_command("spectacular", "--format", "openapi", stdout=buffer)
    return buffer.getvalue()


@pytest.fixture(scope="module")
def schema() -> Any:
    return yaml.safe_load(generate_schema())


def operations(schema: dict[str, Any]) -> Iterator[tuple[str, str, dict[str, Any]]]:
    for path, item in schema["paths"].items():
        for method, operation in item.items():
            if method in HTTP_METHODS:
                yield path, method, operation


def test_schema_generates_and_has_paths(schema: dict[str, Any]) -> None:
    assert schema["paths"], "the schema carries no paths"
    assert len(list(operations(schema))) >= 80


def test_component_schemas_exist(schema: dict[str, Any]) -> None:
    schemas = schema.get("components", {}).get("schemas", {})
    assert len(schemas) >= 100, (
        "the contract must carry real component schemas; a generated client built from a "
        f"document with {len(schemas)} of them would have no DTOs"
    )


def test_operation_ids_are_unique_and_named(schema: dict[str, Any]) -> None:
    ids = [operation.get("operationId") for _, _, operation in operations(schema)]
    assert all(ids), "every operation needs an explicit operationId"
    duplicates = {name: count for name, count in collections.Counter(ids).items() if count > 1}
    assert not duplicates, f"duplicate operationIds: {duplicates}"
    suffixed = [name for name in ids if name and name[-1].isdigit() and name[-2] == "_"]
    assert not suffixed, (
        f"numeral-suffixed operationIds indicate an unresolved collision: {suffixed}"
    )


def test_mutations_carry_a_request_schema(schema: dict[str, Any]) -> None:
    missing = [
        (path, method)
        for path, method, operation in operations(schema)
        if method in MUTATING
        and not operation.get("requestBody")
        and (path, method) not in BODYLESS_MUTATIONS
    ]
    assert not missing, f"mutations without a request schema: {missing}"


def test_successful_responses_carry_a_schema(schema: dict[str, Any]) -> None:
    missing: list[tuple[str, str]] = []
    for path, method, operation in operations(schema):
        if (path, method) in NO_CONTENT_OPERATIONS:
            continue
        described = False
        for code, response in (operation.get("responses") or {}).items():
            if str(code).startswith("2") and any(
                media.get("schema") for media in (response.get("content") or {}).values()
            ):
                described = True
        if not described:
            missing.append((path, method))
    assert not missing, f"successful responses without a schema: {missing}"


def _request_components(
    schema: dict[str, Any],
) -> Iterator[tuple[str, str, str, dict[str, Any]]]:
    """Yield (operationId, media type, component name, component) for every request body."""
    components = schema["components"]["schemas"]
    for _, _, operation in operations(schema):
        content = (operation.get("requestBody") or {}).get("content") or {}
        for media_type, media in content.items():
            body = media.get("schema") or {}
            ref = body.get("$ref") or (body.get("items") or {}).get("$ref")
            if ref:
                name = ref.rsplit("/", 1)[-1]
                yield operation["operationId"], media_type, name, components[name]


def test_upload_parts_are_binary(schema: dict[str, Any]) -> None:
    """A file part must be bytes on the wire (INT-048).

    Described as `format: uri`, the Kotlin and Swift generators typed the part as a URI and
    sent its text instead of the file, so no mobile client could upload anything.
    """
    wrong = [
        (operation_id, name, field)
        for operation_id, media_type, name, component in _request_components(schema)
        if media_type == "multipart/form-data"
        for field, spec in (component.get("properties") or {}).items()
        if spec.get("format") == "uri"
    ]
    assert not wrong, f"multipart parts described as a URI rather than binary: {wrong}"


def test_request_bodies_do_not_require_server_assigned_fields(schema: dict[str, Any]) -> None:
    """A client cannot be made to supply a value only the server assigns (INT-049).

    Duty shifts and temporary closures reused their response component as the request, so
    the read-only `id` was required and a generated Kotlin client could not build a request
    without inventing one.
    """
    wrong = [
        (operation_id, name, field)
        for operation_id, _, name, component in _request_components(schema)
        for field in component.get("required") or []
        if (component.get("properties") or {}).get(field, {}).get("readOnly")
    ]
    assert not wrong, f"request bodies requiring a read-only field: {sorted(set(wrong))}"


def test_every_enum_has_a_value(schema: dict[str, Any]) -> None:
    """An enum of nothing but null does not compile in the Kotlin client (INT-052).

    drf-spectacular described nullable choice fields as `oneOf: [<Enum>, NullEnum]`, and
    the generator rendered NullEnum as an enum class with no entries.
    """
    empty = [
        name
        for name, component in schema["components"]["schemas"].items()
        if "enum" in component and not [value for value in component["enum"] if value is not None]
    ]
    assert not empty, f"enum components without a non-null value: {empty}"


def test_bearer_security_scheme_is_declared(schema: dict[str, Any]) -> None:
    schemes = schema.get("components", {}).get("securitySchemes", {})
    assert "bearerAccessToken" in schemes, "the access-token scheme is missing"
    scheme = schemes["bearerAccessToken"]
    assert scheme["type"] == "http"
    assert scheme["scheme"] == "bearer"


def test_public_operations_do_not_require_authentication(schema: dict[str, Any]) -> None:
    offenders: list[tuple[str, ...]] = []
    for path, method, operation in operations(schema):
        public = path.startswith(PUBLIC_PREFIXES) or (path, method) in PUBLIC_EXTRA
        if not public:
            continue
        security = operation.get("security")
        if security and all(requirement for requirement in security):
            offenders.append((path, method, security))
    assert not offenders, f"public operations must stay reachable without a token: {offenders}"


def test_protected_operations_require_authentication(schema: dict[str, Any]) -> None:
    offenders: list[tuple[str, ...]] = []
    for path, method, operation in operations(schema):
        protected = path.startswith(("/api/v1/admin/", "/api/v1/owner/", "/api/v1/account/"))
        if not protected:
            continue
        security = operation.get("security") or []
        if not any("bearerAccessToken" in requirement for requirement in security):
            offenders.append((path, method))
        if any(requirement == {} for requirement in security):
            offenders.append((path, method, "authentication is optional"))
    assert not offenders, f"protected operations must require the access token: {offenders}"


def test_schema_does_not_leak_internal_fields() -> None:
    document = generate_schema()
    leaked = [token for token in FORBIDDEN_SCHEMA_TOKENS if token in document]
    assert not leaked, f"the contract exposes internal or secret field names: {leaked}"


def test_committed_schema_matches_the_source() -> None:
    assert CANONICAL_SCHEMA.exists(), (
        f"no openapi/schema.yaml above {pathlib.Path(__file__).resolve()}: either it is not "
        "committed, or these tests are running somewhere the repository root is not mounted — "
        "the backend container mounts apps/backend alone, and openapi/ sits above it"
    )
    generated = generate_schema()
    committed = CANONICAL_SCHEMA.read_text(encoding="utf-8")
    assert generated == committed, (
        "openapi/schema.yaml is stale; run ./scripts/generate-openapi.sh and commit the result"
    )


def test_committed_hash_matches_the_committed_schema() -> None:
    assert CANONICAL_HASH.exists(), (
        f"no openapi/schema.sha256 above {pathlib.Path(__file__).resolve()}: see the message on "
        "test_committed_schema_matches_the_source — a missing mount reads as drift otherwise"
    )
    recorded = CANONICAL_HASH.read_text(encoding="utf-8").split()[0]
    actual = hashlib.sha256(CANONICAL_SCHEMA.read_bytes()).hexdigest()
    assert recorded == actual, "openapi/schema.sha256 does not describe openapi/schema.yaml"


def test_generation_is_deterministic() -> None:
    assert generate_schema() == generate_schema(), (
        "two generations differ, so the drift gate would fail at random"
    )
