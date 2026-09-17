# Production Domain, DNS and TLS

## Important

The actual root domain is an external property that must exist in a registrar account. It is impossible to safely invent ownership.

Use environment variable:
```text
ROOT_DOMAIN
```

Once available, production canonical hostnames are:

```text
www.<ROOT_DOMAIN>
api.<ROOT_DOMAIN>
admin.<ROOT_DOMAIN>
```

Optional:
```text
cdn.<ROOT_DOMAIN>
status.<ROOT_DOMAIN>
```

## DNS baseline

Cloudflare recommended.

Records depend on Render custom-domain instructions.

Typical:
```text
www    CNAME → Render public-web hostname
api    CNAME → Render API hostname
admin  CNAME → Render admin hostname
```

Do not copy placeholder targets without verifying provider.

## TLS

- HTTPS only.
- redirect HTTP.
- HSTS after verifying all subdomains.
- certificate auto-managed by Cloudflare/Render baseline.
- no mixed content.

## Backend configuration

```text
ALLOWED_HOSTS=api.<ROOT_DOMAIN>
CSRF_TRUSTED_ORIGINS=https://admin.<ROOT_DOMAIN>,https://www.<ROOT_DOMAIN>
CORS_ALLOWED_ORIGINS=https://admin.<ROOT_DOMAIN>,https://www.<ROOT_DOMAIN>
```

Mobile is native and does not require browser CORS, but API TLS must be valid.

## Public web required URLs

```text
https://www.<ROOT_DOMAIN>/privacy
https://www.<ROOT_DOMAIN>/terms
https://www.<ROOT_DOMAIN>/support
https://www.<ROOT_DOMAIN>/delete-account
```

These links are used by store listings and support.

## App Links

After domain freeze:
Android:
- Digital Asset Links.

iOS:
- apple-app-site-association.

Use:
```text
https://www.<ROOT_DOMAIN>/facility/<id>
```
or approved public deep-link structure.

## DNS cutover checklist

- verify ownership.
- lower TTL before cutover if moving.
- add custom domains to Render.
- issue cert.
- test API.
- test Admin cookie.
- test public legal pages.
- test deep link association.
- then production mobile config.
