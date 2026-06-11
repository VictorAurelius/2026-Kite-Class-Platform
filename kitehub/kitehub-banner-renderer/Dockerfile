# kitehub-banner-renderer — HTML→WebP rasterise sidecar (GAP-1135).
#
# Base image ships Node 20 + Chromium + all OS deps Playwright needs, so the
# branding JVM image stays light (this is the deferred "where does the browser
# live" decision, resolved as a sidecar). Pin the base tag to the same Playwright
# version as package.json so the bundled browser matches the npm client.
FROM mcr.microsoft.com/playwright:v1.49.1-noble

WORKDIR /app

# Install only the production deps. The base image already has the browsers, so
# skip the post-install browser download (saves ~150MB + build time).
ENV PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1
COPY package.json ./
RUN npm install --omit=dev --no-audit --no-fund

COPY server.mjs ./

ENV NODE_ENV=production
ENV PORT=3000
EXPOSE 3000

# Non-root user provided by the Playwright base image.
USER pwuser

HEALTHCHECK --interval=15s --timeout=4s --start-period=10s --retries=5 \
  CMD node -e "require('http').get('http://localhost:3000/health',r=>process.exit(r.statusCode===200?0:1)).on('error',()=>process.exit(1))"

CMD ["node", "server.mjs"]
