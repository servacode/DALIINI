# The WhatsApp bot that delivers registration codes.
#
# Build context is this directory. The image carries no session: /app/auth is a volume,
# because the paired credentials must survive a restart and a redeploy — and because they are
# the account itself, they must never be baked into a layer that gets pushed to a registry.
#
# Two stages, for one reason: Baileys pulls its libsignal dependency straight from GitHub, so
# installing needs a git binary that the running service has no use for.
FROM node:24-bookworm-slim AS deps

RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates git \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
# Versions in package.json are exact, so this resolves the same tree every build even though
# the service deliberately carries no lockfile and is not part of the pnpm workspace: an
# unofficial WhatsApp library has no business in the website's dependency tree.
COPY package.json ./
RUN npm install --omit=dev --no-audit --no-fund


FROM node:24-bookworm-slim

RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY package.json ./
COPY src ./src

# node's own user, and an auth directory it owns: the process writes credentials there on
# every reconnect.
RUN mkdir -p /app/auth && chown -R node:node /app
USER node
VOLUME ["/app/auth"]

# WHATSAPP_AUTH_DIR is deliberately unset: it defaults to ./auth, and WORKDIR makes that the
# volume declared above. Set it only to mount the session somewhere else.
ENV PORT=8085
EXPOSE 8085

# Liveness and readiness in one: /health answers 503 while the session is down, which is
# exactly when nothing should be routed here.
HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
  CMD node -e "fetch('http://127.0.0.1:'+(process.env.PORT||8085)+'/health').then(r=>process.exit(r.ok?0:1)).catch(()=>process.exit(1))"

CMD ["node", "src/server.js"]
