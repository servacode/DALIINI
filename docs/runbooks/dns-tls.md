# DNS and TLS Plan

Until the owner supplies the actual `ROOT_DOMAIN` (EXT-001), do not invent one.

Production names, all served by Caddy on the production server (DECISION-080):
- `<ROOT_DOMAIN>` → the public site; the site's own links, its sitemap and the app's App Links
  use this bare name.
- `www.<ROOT_DOMAIN>` → a permanent redirect to `<ROOT_DOMAIN>`.
- `api.<ROOT_DOMAIN>` → the API, its realtime socket and its health probes.
- `admin.<ROOT_DOMAIN>` → the operators' console.

Staging uses `staging.<ROOT_DOMAIN>` with the same prefixes.

At cutover:
1. In Cloudflare, point the four names at the server; proxied is fine. SSL/TLS mode
   **Full (strict)**, and **Always Use HTTPS** off (renewals answer over plain HTTP; Caddy
   redirects everything else).
2. Fill `ROOT_DOMAIN` and `ACME_EMAIL` in the server's env file and start the stack; Caddy
   issues and renews the certificates itself.
3. The origins and hosts the backend accepts are derived from `ROOT_DOMAIN` in
   `infrastructure/production/compose.yml`, exactly, never with wildcards.
4. HSTS is sent by Caddy (and the API) once HTTPS works end to end; check it before any public
   link goes out.
5. Then update the legal pages, the store listing and the App Links host
   (`DIRECTORY_APP_LINK_HOST`).

Details and the first-issuance fallback: `infrastructure/production/README.md`, "DNS and TLS".
