# Build context: repo root (per ADR-024 pnpm workspace).
# Dockerfile path: kiteclass/kiteclass-frontend/Dockerfile
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
COPY kiteclass/kiteclass-frontend/package.json ./kiteclass/kiteclass-frontend/
RUN pnpm install --frozen-lockfile --filter kiteclass-frontend...

FROM base AS builder
WORKDIR /app
COPY --from=deps /app /app
COPY kiteclass/kiteclass-frontend ./kiteclass/kiteclass-frontend
ENV NEXT_TELEMETRY_DISABLED=1
# Next.js inlines NEXT_PUBLIC_* at build time. Without explicit ARG/ENV,
# api-client.ts falls back to 'http://localhost:8080' default literal and
# browser cannot reach gateway. Surfaced Wave 105 RST UI walk 2026-05-23
# GAP-724. ARG default = local dev URL; production builds override via
# --build-arg NEXT_PUBLIC_API_URL=https://api.kitehub.me
ARG NEXT_PUBLIC_API_URL=http://localhost:9000
ENV NEXT_PUBLIC_API_URL=${NEXT_PUBLIC_API_URL}
# NEXT_PUBLIC_KITEHUB_URL — base KiteHub SaaS URL (branding wizard cross-app link
# in (dashboard)/branding/wizard/page.tsx). Without this baked, code falls back to
# http://localhost:3001 → dead link in production. Prod value = https://kitehub.me.
ARG NEXT_PUBLIC_KITEHUB_URL=http://localhost:3001
ENV NEXT_PUBLIC_KITEHUB_URL=${NEXT_PUBLIC_KITEHUB_URL}
RUN pnpm --filter kiteclass-frontend build

FROM node:22-trixie-slim AS runner
WORKDIR /app
ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1

# CVE patch — Debian trixie packages stale in upstream node:22-trixie-slim digest.
# Closes CodeQL HIGH alerts: CVE-2026-29111 (libudev1/libsystemd0) + CVE-2026-4878 (libcap2).
# Re-evaluate at next Dockerfile review whether upstream image has rebuilt with patched bases.
RUN apt-get update \
 && apt-get install -y --only-upgrade --no-install-recommends libudev1 libsystemd0 libcap2 \
 && apt-get clean \
 && rm -rf /var/lib/apt/lists/*

RUN groupadd --system --gid 1001 nodejs && useradd --system --uid 1001 --gid nodejs --no-create-home --shell /usr/sbin/nologin nextjs

# Next standalone with monorepo: outputFileTracingRoot=repo root means
# the standalone bundle preserves workspace structure under /app.
COPY --from=builder --chown=nextjs:nodejs /app/kiteclass/kiteclass-frontend/.next/standalone ./
COPY --from=builder --chown=nextjs:nodejs /app/kiteclass/kiteclass-frontend/.next/static ./kiteclass/kiteclass-frontend/.next/static
COPY --from=builder /app/kiteclass/kiteclass-frontend/public ./kiteclass/kiteclass-frontend/public

RUN rm -rf /var/lib/apt/lists/* /tmp/*

USER nextjs
EXPOSE 3000
ENV PORT=3000
ENV HOSTNAME="0.0.0.0"

HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD node -e "require('http').get('http://localhost:3000', (r) => {process.exit(r.statusCode === 200 ? 0 : 1)})"

CMD ["node", "kiteclass/kiteclass-frontend/server.js"]
