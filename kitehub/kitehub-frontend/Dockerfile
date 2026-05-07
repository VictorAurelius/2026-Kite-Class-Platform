# Build context: repo root (per ADR-024 pnpm workspace).
# Dockerfile path: kitehub/kitehub-frontend/Dockerfile
FROM node:20-alpine AS base
# Pin pnpm@9 — pnpm@11 requires Node 22.5+ (node:sqlite builtin); we run Node 20.
RUN corepack enable && corepack prepare pnpm@9 --activate

FROM base AS deps
RUN apk add --no-cache libc6-compat
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

FROM node:20-alpine AS runner
WORKDIR /app
ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1

RUN addgroup --system --gid 1001 nodejs && adduser --system --uid 1001 nextjs

# Next standalone with monorepo: outputFileTracingRoot=repo root means
# the standalone bundle preserves workspace structure under /app.
COPY --from=builder --chown=nextjs:nodejs /app/kitehub/kitehub-frontend/.next/standalone ./
COPY --from=builder --chown=nextjs:nodejs /app/kitehub/kitehub-frontend/.next/static ./kitehub/kitehub-frontend/.next/static
COPY --from=builder /app/kitehub/kitehub-frontend/public ./kitehub/kitehub-frontend/public

RUN rm -rf /var/cache/apk/* /tmp/*

USER nextjs
EXPOSE 3001
ENV PORT=3001
ENV HOSTNAME="0.0.0.0"

HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD node -e "require('http').get('http://localhost:3001/api/health', (r) => {process.exit(r.statusCode === 200 ? 0 : 1)})"

CMD ["node", "kitehub/kitehub-frontend/server.js"]
