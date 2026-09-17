# DNS and TLS Plan

Until the owner supplies an actual `ROOT_DOMAIN`, do not invent one. Provider hostnames may be used for staging only.

Production canonical names:
- `www.<ROOT_DOMAIN>` → public web
- `api.<ROOT_DOMAIN>` → Django ASGI
- `admin.<ROOT_DOMAIN>` → admin

At cutover: configure Cloudflare DNS, attach provider custom domains, require TLS, set exact `ALLOWED_HOSTS`, `CORS_ALLOWED_ORIGINS` and `CSRF_TRUSTED_ORIGINS`, verify HSTS only after HTTPS works end-to-end, then update legal/store/app-link URLs. Wildcards are forbidden for production origins/hosts.
