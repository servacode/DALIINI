# Backup runner for the Render cron job (render.yaml: directory-v3-staging-db-backup).
# Build context is the monorepo root. postgres:17 ships a pg_dump that matches the
# database's major version (postgresMajorVersion: "17"); awscli handles the upload.
FROM postgres:17-bookworm

RUN apt-get update \
    && apt-get install -y --no-install-recommends awscli ca-certificates \
    && rm -rf /var/lib/apt/lists/*

COPY scripts/db-backup.sh scripts/db-restore.sh /usr/local/bin/
RUN chmod 0755 /usr/local/bin/db-backup.sh /usr/local/bin/db-restore.sh

# The postgres image already defines an unprivileged `postgres` user.
USER postgres
WORKDIR /tmp
ENTRYPOINT []
CMD ["/usr/local/bin/db-backup.sh"]
