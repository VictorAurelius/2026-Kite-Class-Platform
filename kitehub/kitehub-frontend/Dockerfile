# Build context: repo root (per ADR-024 pnpm workspace).
# Dockerfile path: kitehub/kitehub-frontend/Dockerfile
# GAP-442: Node 22 LTS on Debian trixie-slim — replaces alpine 3.23 to clear gnutls
# CVE-2026-33845 + the 11 npm-in-base CVE pile (tar / minimatch / cross-spawn / glob).
# pnpm@9 supports Node 18-22+; pin retained for behavior parity with Node 20-alpine baseline.
FROM node:22-trixie-slim AS base
RUN corepack enable && corepack prepare pnpm@9 --activate

FROM base AS deps
# libc6-compat is alpine-specific (musl→glibc shim); not needed on Debian trixie (glibc native).
WORKDIR /app
COPY pnpm-workspace.yaml package.json pnpm-lock.yaml ./
COPY packages/shared-ui ./packages/shared-ui
COPY kitehub/kitehub-frontend/package.json ./kitehub/kitehub-frontend/
RUN pnpm install --frozen-lockfile --filter kitehub-frontend...

FROM base AS builder
WORKDIR /app
COPY --from=deps /app /app
COPY kitehub/kitehub-frontend ./kitehub/kitehub-frontend

# Build-time arguments for Next.js public environment variables
# Safe defaults for local development - override in docker-compose for production
ARG NEXT_PUBLIC_API_URL=http://localhost:9000
ARG NEXT_PUBLIC_KITECLASS_URL_PATTERN=http://localhost:3000?tenant={subdomain}
ENV NEXT_PUBLIC_API_URL=$NEXT_PUBLIC_API_URL
ENV NEXT_PUBLIC_KITECLASS_URL_PATTERN=$NEXT_PUBLIC_KITECLASS_URL_PATTERN
ENV NEXT_TELEMETRY_DISABLED=1

RUN pnpm --filter kitehub-frontend build

FROM node:22-trixie-slim AS runner
WORKDIR /app
ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1

# CVE patch — Debian trixie packages stale in upstream node:22-trixie-slim digest.
# Closes CodeQL HIGH alerts: CVE-2026-29111 (libudev1/libsystemd0) + CVE-2026-4878 (libcap2).
# Re-evaluate at next Dockerfile review whether upstream image has rebuilt with patched bases.
RUN apt-get update \
 && apt-get upgrade -y libudev1 libsystemd0 libcap2 \
 && apt-get clean

RUN groupadd --system --gid 1001 nodejs && useradd --system --uid 1001 --gid nodejs --no-create-home --shell /usr/sbin/nologin nextjs

# Next standalone with monorepo: outputFileTracingRoot=repo root means
# the standalone bundle preserves workspace structure under /app.
COPY --from=builder --chown=nextjs:nodejs /app/kitehub/kitehub-frontend/.next/standalone ./
COPY --from=builder --chown=nextjs:nodejs /app/kitehub/kitehub-frontend/.next/static ./kitehub/kitehub-frontend/.next/static
COPY --from=builder /app/kitehub/kitehub-frontend/public ./kitehub/kitehub-frontend/public

RUN rm -rf /var/lib/apt/lists/* /tmp/*

USER nextjs
EXPOSE 3001
ENV PORT=3001
ENV HOSTNAME="0.0.0.0"

HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD node -e "require('http').get('http://localhost:3001/api/health', (r) => {process.exit(r.statusCode === 200 ? 0 : 1)})"

CMD ["node", "kitehub/kitehub-frontend/server.js"]
