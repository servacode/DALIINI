# Security and Privacy Specification

## Threat model

Protect against:
- account takeover.
- OTP brute force.
- phone enumeration.
- token theft.
- refresh reuse.
- IDOR.
- admin privilege escalation.
- evidence leakage.
- upload attacks.
- SQL injection.
- XSS.
- CSRF.
- SSRF.
- rate abuse.
- secret leakage.
- location privacy abuse.
- backup leakage.
- dependency compromise.

## Passwords

- Argon2.
- central password policy.
- never log.
- no password delivery over WhatsApp/SMS.

## OTP

- random six-digit baseline.
- short expiry.
- HMAC/digest at rest.
- max attempts.
- start/verify throttles.
- challenge consumption.
- provider abstraction.

## Sessions

- short access.
- rotating opaque refresh.
- server stores digest.
- previous digest short concurrency grace.
- post-grace reuse = compromise/revoke.
- session list/revoke.
- password reset/change revokes sessions.
- user block revokes/denies.

## Mobile storage

Android:
- Keystore-backed.

iOS:
- Keychain.

## Admin

- HttpOnly refresh cookie.
- Secure.
- SameSite.
- origin/CSRF protection.
- no refresh in browser storage.

## Authorization

Every owner facility access:
```text
facility membership check
```

Every admin mutation:
```text
permission check
```

Do not rely on UUID secrecy.

## Evidence privacy

- private object storage namespace.
- no public URL.
- evidence view requires permission.
- evidence view audit.
- `Cache-Control: private, no-store` where streamed.

## Upload security

- extension not trusted.
- MIME header not trusted.
- decode.
- max byte/pixel.
- re-encode.
- strip metadata.
- random storage key.

## Location privacy

Core:
- foreground only.
- coordinates used for current query.
- avoid long-term history.
- analytics must not include raw precise coordinates unless explicit approved purpose.

## PII inventory

Likely:
- name.
- phone.
- profile image.
- owner evidence.
- location input/current coordinates.
- facility ownership relationship.
- device/session metadata.

Document retention and purpose.

## Account deletion

Google Play requires apps offering account creation to offer account deletion:
- in app.
- external web resource.

Deletion policy must address owned facilities and records required for fraud/security/audit.

## Security headers

Web:
- CSP.
- HSTS.
- X-Content-Type-Options.
- Referrer-Policy.
- frame restrictions.

API:
- strict CORS.
- proxy trust config.
- TLS only.

## Secrets

Never commit:
- signing keys.
- JWT/access secrets.
- refresh HMAC secrets.
- recovery HMAC.
- DB URL.
- R2 keys.
- FCM/APNs keys.
- Sentry auth tokens.

## Security gates

Before release:
```text
Critical 0
P0 0
P1 0
```

Test:
- IDOR.
- privilege escalation.
- session abuse.
- upload abuse.
- evidence exposure.
- throttling.
- CORS/CSRF.
- XSS.
- injection.
- SSRF.
- dependency scan.
- secret scan.
