# Admin console image. Build context is the monorepo root (render.yaml: dockerContext: .).
# Runtime behaviour is unchanged (`next start` via pnpm); the hardening is a frozen
# lockfile install, a non-root runtime user and a container HEALTHCHECK.
FROM node:26-bookworm-slim AS build

ENV PNPM_HOME=/pnpm
ENV PATH=$PNPM_HOME:$PATH
RUN corepack enable && corepack prepare pnpm@10.17.1 --activate
WORKDIR /workspace

COPY package.json pnpm-workspace.yaml pnpm-lock.yaml ./
COPY apps/admin/package.json apps/admin/package.json
COPY packages/design-tokens/package.json packages/design-tokens/package.json
COPY packages/api-typescript/package.json packages/api-typescript/package.json
RUN pnpm install --frozen-lockfile --filter @servacode/admin...

COPY apps/admin apps/admin
COPY packages/design-tokens packages/design-tokens
COPY packages/api-typescript packages/api-typescript
ENV NEXT_TELEMETRY_DISABLED=1
RUN pnpm --filter @servacode/admin build

FROM node:26-bookworm-slim AS runtime
ENV NODE_ENV=production \
    NEXT_TELEMETRY_DISABLED=1
# pnpm is activated for root at build time; COREPACK_HOME makes it readable by `node`.
ENV PNPM_HOME=/pnpm \
    COREPACK_HOME=/corepack
ENV PATH=$PNPM_HOME:$PATH
RUN mkdir -p /corepack && corepack enable && corepack prepare pnpm@10.17.1 --activate \
    && chown -R node:node /corepack
WORKDIR /workspace
COPY --from=build --chown=node:node /workspace /workspace
USER node
EXPOSE 10000
# /login renders without a session, so it is the cheapest page that proves the app is up.
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD node -e "fetch('http://127.0.0.1:'+(process.env.PORT||10000)+'/login').then(r=>process.exit(r.status<500?0:1)).catch(()=>process.exit(1))"
CMD ["sh", "-c", "pnpm --filter @servacode/admin start -p ${PORT:-10000}"]
