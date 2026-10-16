#!/usr/bin/env bash
# Prepare a fresh Ubuntu 24.04 server for the stack, once (README.md, "The server" and "First
# start"). Run as root; `.github/workflows/server-setup.yml` runs it over SSH. Running it again
# changes nothing that is already right, and never touches a filled env file.
#
#   bootstrap-server.sh <deploy-public-key> <root-domain> <ops-email> <support-email> <privacy-email>
#
# It installs Docker from Docker's own repository, the firewall and unattended upgrades; makes
# the `deploy` user that GitHub deploys as; clones the repository; and writes
# /srv/daliini/production.env with every random value generated here, on the server, so none of
# them ever leaves it. What only the owner can supply (the storage keys, Firebase) stays <...>.
set -euo pipefail

deploy_key="${1:?deploy public key}"
domain="${2:?root domain}"
ops_email="${3:?ops email}"
support_email="${4:?support email}"
privacy_email="${5:?privacy email}"
repo_url="https://github.com/servacode/DALIINI.git"
log() { printf '%s [bootstrap] %s\n' "$(date -u +%H:%M:%S)" "$*"; }

export DEBIAN_FRONTEND=noninteractive

log "packages"
apt-get update -q
apt-get install -y -q ca-certificates curl git python3 ufw unattended-upgrades openssl >/dev/null

if ! command -v docker >/dev/null 2>&1; then
  log "docker, from docker.com"
  install -m 0755 -d /etc/apt/keyrings
  curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
  chmod a+r /etc/apt/keyrings/docker.asc
  codename="$(. /etc/os-release && echo "$VERSION_CODENAME")"
  echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc]" \
    "https://download.docker.com/linux/ubuntu $codename stable" >/etc/apt/sources.list.d/docker.list
  apt-get update -q
  apt-get install -y -q docker-ce docker-ce-cli containerd.io docker-buildx-plugin \
    docker-compose-plugin >/dev/null
fi
systemctl enable --now docker >/dev/null

# Four gigabytes hold the stack, but the monthly map build runs beside it.
if ! swapon --show | grep -q /swapfile; then
  log "swap, 2 GB"
  fallocate -l 2G /swapfile
  chmod 600 /swapfile
  mkswap /swapfile >/dev/null
  swapon /swapfile
  grep -q '^/swapfile ' /etc/fstab || echo '/swapfile none swap sw 0 0' >>/etc/fstab
fi

log "the deploy user"
id deploy >/dev/null 2>&1 || useradd --create-home --shell /bin/bash deploy
usermod -aG docker deploy
install -d -m 700 -o deploy -g deploy /home/deploy/.ssh
touch /home/deploy/.ssh/authorized_keys
grep -qxF "$deploy_key" /home/deploy/.ssh/authorized_keys ||
  echo "$deploy_key" >>/home/deploy/.ssh/authorized_keys
chown deploy:deploy /home/deploy/.ssh/authorized_keys
chmod 600 /home/deploy/.ssh/authorized_keys

log "ssh by key only"
cat >/etc/ssh/sshd_config.d/10-daliini.conf <<'EOF'
PasswordAuthentication no
KbdInteractiveAuthentication no
PermitRootLogin prohibit-password
EOF
systemctl reload ssh

log "firewall: 22, 80 and 443"
ufw allow OpenSSH >/dev/null
ufw allow 80,443/tcp >/dev/null
ufw allow 443/udp >/dev/null
ufw --force enable >/dev/null

log "unattended security upgrades"
dpkg-reconfigure -f noninteractive unattended-upgrades >/dev/null

log "the repository"
install -d -o deploy -g deploy /srv/daliini /srv/daliini/map /srv/daliini/evidence
if [ ! -d /srv/daliini/repo/.git ]; then
  sudo -u deploy git clone --quiet "$repo_url" /srv/daliini/repo
fi

env_file=/srv/daliini/production.env
if [ ! -f "$env_file" ]; then
  log "the env file, its random values made here"
  sudo -u deploy python3 - "$env_file" "$domain" "$ops_email" "$support_email" "$privacy_email" <<'PY'
import os, secrets, sys

path, domain, ops, support, privacy = sys.argv[1:]
source = open("/srv/daliini/repo/infrastructure/production/production.env.example").read()
lines = []
for line in source.splitlines():
    if line.endswith("=<random>"):
        line = line[: -len("<random>")] + secrets.token_urlsafe(48)
    line = line.replace("<ROOT_DOMAIN>", domain)
    if line.startswith("ACME_EMAIL="):
        line = "ACME_EMAIL=" + ops
    elif line.startswith("SUPPORT_EMAIL="):
        line = "SUPPORT_EMAIL=" + support
    elif line.startswith("PRIVACY_CONTACT_EMAIL="):
        line = "PRIVACY_CONTACT_EMAIL=" + privacy
    elif line.startswith("IMAGE_REGISTRY="):
        line = "IMAGE_REGISTRY=ghcr.io/servacode/"
    lines.append(line)
descriptor = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
with os.fdopen(descriptor, "w") as handle:
    handle.write("\n".join(lines) + "\n")
PY
fi

missing="$(grep -o '<[A-Z_]*>' "$env_file" | grep -v GIT_SHA | sort -u | tr '\n' ' ' || true)"
log "done. Still to fill in $env_file: ${missing:-nothing}"
log "this server's key:"
ssh-keygen -lf /etc/ssh/ssh_host_ed25519_key.pub
