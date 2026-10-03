# The production server

Everything the platform runs, on one VPS, behind Caddy (DECISION-080). Staging is the same
stack with its own env file, project name and domain.

| What | Where |
|---|---|
| The stack | `compose.yml` (API, worker, beat, migrations, site, console, WhatsApp bot, PostgreSQL/PostGIS, Redis, Caddy, the backup tool) |
| TLS and the four names | `Caddyfile`: `<ROOT>`, `www.<ROOT>` → `<ROOT>`, `api.<ROOT>`, `admin.<ROOT>` |
| Every setting | `production.env.example`, copied to `/srv/daliini/production.env` on the server |
| Nightly backup | `systemd/daliini-backup.{service,timer}` |
| What CI holds it to | `apps/backend/core/tests/test_production_stack.py`, `infrastructure/scripts/qualify-production-stack.py` |

Not on the server, on purpose: photographs and verification documents live in an S3 service
(Cloudflare R2 or similar), and the backups in a second bucket with its own key. A lost disk
loses neither.

## The server

- Debian 12 or Ubuntu 24.04, 4 GB of memory and 2 vCPU to start, 60 GB of disk. Docker Engine
  with the compose plugin (`docs.docker.com/engine/install`).
- A user for deploys in the `docker` group; SSH by key only; the firewall open on 22, 80 and
  443 only (`ufw allow OpenSSH && ufw allow 80,443/tcp && ufw allow 443/udp && ufw enable`).
- Unattended security upgrades on (`apt install unattended-upgrades`).

## DNS and TLS (Cloudflare)

1. Point `<ROOT>`, `www`, `api` and `admin` at the server (A, and AAAA if it has IPv6).
2. Proxied (orange cloud) is fine. SSL/TLS mode: **Full (strict)**. Leave **Always Use HTTPS**
   off: certificate renewals answer a challenge over plain HTTP, and Caddy redirects every other
   plain request itself.
3. Caddy obtains and renews the certificates on its first start. If the first issuance fails
   behind the proxy, switch the four records to DNS only (grey cloud), start Caddy, and turn the
   proxy back on once the certificates exist.

Caddy believes Cloudflare's `CF-Connecting-IP` only from Cloudflare's own ranges, listed in the
Caddyfile. Cloudflare publishes them at `https://www.cloudflare.com/ips`; check them a few times
a year and update the list.

## First start

```sh
sudo mkdir -p /srv/daliini && sudo chown "$USER" /srv/daliini
git clone https://github.com/servacode/DALIINI.git /srv/daliini/repo
cp /srv/daliini/repo/infrastructure/production/production.env.example /srv/daliini/production.env
chmod 600 /srv/daliini/production.env   # then fill in every <...>

cd /srv/daliini/repo/infrastructure/production
docker compose --env-file /srv/daliini/production.env up -d --build
docker compose --env-file /srv/daliini/production.env ps
```

`migrate` runs `check --deploy` and the migrations, then exits; the API starts only after it
succeeded. Then:

```sh
# The first operator (DECISION-072). --create makes the account and asks for its password.
docker compose --env-file /srv/daliini/production.env exec api \
  uv run python manage.py grant_operator +9639XXXXXXXX --create --name "<name>"
# Pair the WhatsApp number that sends sign-in codes (OTP_PROVIDER=whatsapp_bot).
docker compose --env-file /srv/daliini/production.env logs -f whatsapp-bot
```

Check from outside: `https://api.<ROOT>/health/ready/` answers `{"status": "ready", ...}`,
`https://<ROOT>/` is the site, `https://admin.<ROOT>/login` the console.

## The nightly backup

```sh
sudo cp systemd/daliini-backup.service systemd/daliini-backup.timer /etc/systemd/system/
sudo systemctl daemon-reload && sudo systemctl enable --now daliini-backup.timer
sudo systemctl start daliini-backup.service   # one now, to see it work
```

Each run reports to the console's «حالة النظام» page. Restoring, and the monthly drill:
`infrastructure/BACKUP-RESTORE.md`.

## Deploying a new version

`docs/runbooks/deploy.md`: pull the commit, `up -d --build`, check health, and how to go back.

## Staging

The same file under another project name, env file and domain (`staging.<ROOT>` and its
`api.`, `admin.` and `www.` names):

```sh
docker compose -p daliini-staging --env-file /srv/daliini/staging.env up -d --build
```

with `ENVIRONMENT=staging` (the settings module that keeps the schema readable for operators)
and its own secrets, database password and buckets. On the same server, give one of the two
stacks other published ports, or put staging on its own small server: two Caddys cannot both
hold 80 and 443.
