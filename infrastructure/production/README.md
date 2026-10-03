# The production server

Everything the platform runs, on one VPS, behind Caddy (DECISION-080). Staging is the same
stack with its own env file, project name and domain.

| What | Where |
|---|---|
| The stack | `compose.yml` (API, worker, beat, migrations, site, console, WhatsApp bot, PostgreSQL/PostGIS, Redis, Caddy, the backup tool) |
| TLS and the four names | `Caddyfile`: `<ROOT>`, `www.<ROOT>` → `<ROOT>`, `api.<ROOT>`, `admin.<ROOT>` |
| Every setting | `production.env.example`, copied to `/srv/daliini/production.env` on the server |
| Hourly backup, monthly restore drill | `systemd/`, `restore-drill.sh` |
| Deploying a commit | `deploy.sh`, run by `.github/workflows/deploy.yml` over SSH |
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

If the repository is made private, clone it over SSH with a read-only deploy key instead (GitHub →
the repository's Settings → Deploy keys; `git clone git@github.com:servacode/DALIINI.git`).
Deploys fetch through the same remote.

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

## Backups

```sh
sudo cp systemd/daliini-backup.service systemd/daliini-backup.timer /etc/systemd/system/
sudo systemctl daemon-reload && sudo systemctl enable --now daliini-backup.timer
sudo systemctl start daliini-backup.service   # one now, to see it work
```

Each run reports to the console's «حالة النظام» page. The restore drill runs on the first Sunday
of each month: it restores the newest backup into a throwaway database, checks it, and writes
its evidence to `/srv/daliini/evidence/` (DECISION-081). Install its timer too, and run it once
by hand before trusting the server:

```sh
sudo cp systemd/daliini-restore-drill.service systemd/daliini-restore-drill.timer /etc/systemd/system/
sudo systemctl daemon-reload && sudo systemctl enable --now daliini-restore-drill.timer
sudo systemctl start daliini-restore-drill.service && journalctl -u daliini-restore-drill -n 20
```

Restoring by hand: `infrastructure/BACKUP-RESTORE.md`.

## Deploying from GitHub

Releases are images on GHCR, and a deploy is `deploy.sh` run over SSH (DECISION-081).
`docs/runbooks/deploy.md` has the sequence. Once, for each environment:

1. **GitHub → Settings → Environments → `production`** (and `staging`):
   - *Variables*: `ROOT_DOMAIN`, `MEDIA_ORIGIN`, `SUPPORT_EMAIL`, `PRIVACY_CONTACT_EMAIL`, and
     if used `MAP_STYLE_URL`, `MAP_ORIGINS`, `PLAY_STORE_URL`, `APP_STORE_URL` and
     `APP_DOWNLOAD_URL`. The site's image is built with them.
   - *Secrets*: `DEPLOY_HOST`, `DEPLOY_USER`, `DEPLOY_SSH_KEY` (the private half of a key
     made for deploys only: `ssh-keygen -t ed25519 -f daliini-deploy -N ''`, with its `.pub`
     in the deploy user's `~/.ssh/authorized_keys`), and `DEPLOY_KNOWN_HOSTS`
     (`ssh-keyscan -t ed25519 <server>`, compared by hand with
     `ssh-keygen -lf /etc/ssh/ssh_host_ed25519_key.pub` on the server).
   - *Required reviewers* for `production`, if a second person should approve each deploy.
2. **On the server**, as the deploy user, let Docker pull the images. Use a GitHub token with
   only `read:packages`, or make the packages public:
   `echo <token> | docker login ghcr.io -u <github-user> --password-stdin`.
3. In `/srv/daliini/production.env`, set `IMAGE_REGISTRY=ghcr.io/servacode/`.

Without GitHub, the same deploy runs by hand on the server, and builds the images there when
`IMAGE_REGISTRY` is empty:

```sh
/srv/daliini/repo/infrastructure/production/deploy.sh /srv/daliini/production.env <commit>
```

## Staging

The same file under another env file and domain (`staging.<ROOT>` and its `api.`, `admin.` and
`www.` names). `/srv/daliini/staging.env` says `ENVIRONMENT=staging` (the settings module that
keeps the schema readable for operators) and `COMPOSE_PROJECT_NAME=daliini-staging`, and has
its own secrets, database password and buckets:

```sh
docker compose --env-file /srv/daliini/staging.env up -d --build
```

On the same server, give one of the two stacks other published ports, or put staging on its own
small server: two Caddys cannot both hold 80 and 443.
