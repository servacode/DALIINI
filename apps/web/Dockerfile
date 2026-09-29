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
# What `next build` bakes in: NEXT_PUBLIC_* values are inlined into the bundle, the API
# origins go into the CSP header, and pages with no revalidate window (/support…) render
# once, here, with the contact addresses. The platform passes a service's environment as
# build arguments, but only those declared here reach this stage. Secrets such as
# WEB_SERVER_API_KEY are deliberately absent: the server reads them at runtime.
ARG NEXT_PUBLIC_ROOT_DOMAIN
ARG NEXT_PUBLIC_API_ORIGIN
ARG NEXT_PUBLIC_PLAY_STORE_URL
ARG NEXT_PUBLIC_APP_STORE_URL
ARG NEXT_PUBLIC_ANDROID_PACKAGE
ARG PUBLIC_API_ORIGIN
ARG SUPPORT_EMAIL
ARG PRIVACY_CONTACT_EMAIL
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
