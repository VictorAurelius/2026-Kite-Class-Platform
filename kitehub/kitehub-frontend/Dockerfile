FROM node:20-alpine AS base
RUN corepack enable && corepack prepare pnpm@latest --activate

FROM base AS deps
# Install libc6-compat for Alpine compatibility
RUN apk add --no-cache libc6-compat
WORKDIR /app
COPY package.json pnpm-lock.yaml* ./
RUN pnpm install --frozen-lockfile 2>/dev/null || pnpm install

FROM base AS builder
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .

# Build-time arguments for Next.js public environment variables
# Safe defaults for local development - override in docker-compose for production
ARG NEXT_PUBLIC_API_URL=http://localhost:9000
ARG NEXT_PUBLIC_KITECLASS_URL_PATTERN=http://localhost:3000?tenant={subdomain}

# Expose as environment variables for Next.js build
ENV NEXT_PUBLIC_API_URL=$NEXT_PUBLIC_API_URL
ENV NEXT_PUBLIC_KITECLASS_URL_PATTERN=$NEXT_PUBLIC_KITECLASS_URL_PATTERN

# Disable Next.js telemetry during build
ENV NEXT_TELEMETRY_DISABLED=1

RUN pnpm build

# Use minimal node image for runner (no pnpm/corepack needed)
FROM node:20-alpine AS runner
WORKDIR /app
ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1

# Create non-root user
RUN addgroup --system --gid 1001 nodejs && adduser --system --uid 1001 nextjs

# Copy only necessary files from builder
COPY --from=builder /app/public ./public
COPY --from=builder --chown=nextjs:nodejs /app/.next/standalone ./
COPY --from=builder --chown=nextjs:nodejs /app/.next/static ./.next/static

# Clean up unnecessary files
RUN rm -rf /var/cache/apk/* /tmp/*

USER nextjs
EXPOSE 3001
ENV PORT=3001
ENV HOSTNAME="0.0.0.0"

# Health check using Node.js built-in http module (no wget needed)
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD node -e "require('http').get('http://localhost:3001/api/health', (r) => {process.exit(r.statusCode === 200 ? 0 : 1)})"

CMD ["node", "server.js"]
