# Public web image. Build context is the monorepo root (render.yaml: dockerContext: .).
#
# deps    -> installs exactly what pnpm-lock.yaml pins (--frozen-lockfile fails on drift)
# build   -> `next build` with output: "standalone" (apps/web/next.config.ts)
# runtime -> only the traced standalone server, run as the unprivileged `node` user
FROM node:24-bookworm-slim AS deps
ENV PNPM_HOME=/pnpm
ENV PATH=$PNPM_HOME:$PATH
RUN corepack enable && corepack prepare pnpm@10.17.1 --activate
WORKDIR /workspace
COPY package.json pnpm-workspace.yaml pnpm-lock.yaml ./
COPY apps/web/package.json apps/web/package.json
COPY packages/design-tokens/package.json packages/design-tokens/package.json
COPY packages/api-typescript/package.json packages/api-typescript/package.json
RUN pnpm install --frozen-lockfile --filter @servacode/public-web...

FROM deps AS build
COPY apps/web apps/web
COPY packages/design-tokens packages/design-tokens
COPY packages/api-typescript packages/api-typescript
ENV NEXT_TELEMETRY_DISABLED=1
RUN pnpm --filter @servacode/public-web build

FROM node:24-bookworm-slim AS runtime
ENV NODE_ENV=production \
    NEXT_TELEMETRY_DISABLED=1 \
    HOSTNAME=0.0.0.0 \
    PORT=10000
WORKDIR /app
COPY --from=build --chown=node:node /workspace/apps/web/.next/standalone ./
COPY --from=build --chown=node:node /workspace/apps/web/.next/static ./apps/web/.next/static
USER node
EXPOSE 10000
# Render probes its own healthCheckPath; this covers plain `docker run` / compose.
HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
  CMD node -e "fetch('http://127.0.0.1:'+(process.env.PORT||10000)+'/').then(r=>process.exit(r.ok?0:1)).catch(()=>process.exit(1))"
# The standalone server reads PORT and HOSTNAME from the environment.
CMD ["node", "apps/web/server.js"]
