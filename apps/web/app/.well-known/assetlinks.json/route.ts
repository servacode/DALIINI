import { androidPackage } from "../../../lib/config";

/*
 * /.well-known/assetlinks.json — the Digital Asset Links statement that lets
 * Android verify the Daliini app's App Links, so the site's URLs (the facility
 * pages, «افتح في التطبيق») open in the app when it is installed.
 *
 * Built from NEXT_PUBLIC_ANDROID_PACKAGE (inlined at build time) and
 * ANDROID_CERT_SHA256: the SHA-256 fingerprints of the certificates the app is
 * signed with, comma-separated (the Play App Signing key, and the upload key if
 * directly installed builds should verify too), read at request time. Either
 * one missing or invalid answers 404, so nothing half-configured is published.
 */
export const dynamic = "force-dynamic";

const FINGERPRINT = /^(?:[0-9A-F]{2}:){31}[0-9A-F]{2}$/;

/* "aa:bb:…" or 64 bare hex digits, as the upper-case colon form Android expects; null when neither. */
function fingerprint(raw: string): string | null {
  const value = raw.trim().toUpperCase();
  if (FINGERPRINT.test(value)) return value;
  if (/^[0-9A-F]{64}$/.test(value)) return value.match(/../g)!.join(":");
  return null;
}

export function GET() {
  const pkg = androidPackage();
  const fingerprints = [
    ...new Set(
      (process.env.ANDROID_CERT_SHA256 ?? "")
        .split(",")
        .map(fingerprint)
        .filter((f): f is string => f !== null),
    ),
  ];
  if (!pkg || fingerprints.length === 0) {
    return new Response("Not Found", { status: 404, headers: { "Content-Type": "text/plain; charset=utf-8" } });
  }
  return Response.json(
    [
      {
        relation: ["delegate_permission/common.handle_all_urls"],
        target: { namespace: "android_app", package_name: pkg, sha256_cert_fingerprints: fingerprints },
      },
    ],
    { headers: { "Cache-Control": "public, max-age=3600" } },
  );
}
