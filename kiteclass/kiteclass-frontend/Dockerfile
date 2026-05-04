# Build context: repo root (per ADR-024 pnpm workspace).
# Dockerfile path: kiteclass/kiteclass-frontend/Dockerfile
FROM node:20-alpine AS base
RUN corepack enable && corepack prepare pnpm@latest --activate

FROM base AS deps
RUN apk add --no-cache libc6-compat
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
RUN pnpm --filter kiteclass-frontend build

FROM node:20-alpine AS runner
WORKDIR /app
ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1

RUN addgroup --system --gid 1001 nodejs && adduser --system --uid 1001 nextjs

# Next standalone with monorepo: outputFileTracingRoot=repo root means
# the standalone bundle preserves workspace structure under /app.
COPY --from=builder --chown=nextjs:nodejs /app/kiteclass/kiteclass-frontend/.next/standalone ./
COPY --from=builder --chown=nextjs:nodejs /app/kiteclass/kiteclass-frontend/.next/static ./kiteclass/kiteclass-frontend/.next/static
COPY --from=builder /app/kiteclass/kiteclass-frontend/public ./kiteclass/kiteclass-frontend/public

RUN rm -rf /var/cache/apk/* /tmp/*

USER nextjs
EXPOSE 3000
ENV PORT=3000
ENV HOSTNAME="0.0.0.0"

HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD node -e "require('http').get('http://localhost:3000', (r) => {process.exit(r.statusCode === 200 ? 0 : 1)})"

CMD ["node", "kiteclass/kiteclass-frontend/server.js"]
