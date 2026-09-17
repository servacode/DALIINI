FROM node:24-bookworm-slim AS build

ENV PNPM_HOME=/pnpm
ENV PATH=$PNPM_HOME:$PATH
RUN corepack enable && corepack prepare pnpm@10.17.1 --activate
WORKDIR /workspace

COPY package.json pnpm-workspace.yaml ./
COPY apps/admin/package.json apps/admin/package.json
COPY packages/design-tokens/package.json packages/design-tokens/package.json
COPY packages/api-typescript/package.json packages/api-typescript/package.json
RUN pnpm install --no-frozen-lockfile

COPY apps/admin apps/admin
COPY packages/design-tokens packages/design-tokens
COPY packages/api-typescript packages/api-typescript
RUN pnpm --filter @servacode/admin build

FROM node:24-bookworm-slim AS runtime
ENV NODE_ENV=production
ENV PNPM_HOME=/pnpm
ENV PATH=$PNPM_HOME:$PATH
RUN corepack enable && corepack prepare pnpm@10.17.1 --activate
WORKDIR /workspace
COPY --from=build /workspace /workspace
EXPOSE 10000
CMD ["sh", "-c", "pnpm --filter @servacode/admin start -- -p ${PORT:-10000}"]
