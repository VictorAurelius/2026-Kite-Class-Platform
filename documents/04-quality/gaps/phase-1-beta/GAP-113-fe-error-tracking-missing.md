# GAP-113: Frontend Error Tracking Missing

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Frontend / Monitoring
**Found:** 2026-04-19 (ops-readiness audit — baseline)
**Affects:** kiteclass-frontend, kitehub-frontend

## Problem

Không có service error tracking (Sentry/Rollbar/Bugsnag) cho 2 Next.js frontends. Lỗi runtime chỉ stuck trong browser console → team không biết user gặp lỗi.

Evidence:
- `grep -rn "sentry\|rollbar\|bugsnag" kiteclass/kiteclass-frontend kitehub/kitehub-frontend` → 0 hits trong source
- `package.json` không có `@sentry/nextjs`

## Root Cause

Feature chưa triển khai. Dev tập trung vào functional flow, chưa có observability path.

## Proposed Fix

1. Chọn provider: Sentry (recommended — OSS-available, free tier) hoặc OpenTelemetry → backend trace store
2. Install `@sentry/nextjs` vào cả 2 frontends
3. Config DSN qua env var `NEXT_PUBLIC_SENTRY_DSN`
4. Enable: auto-error capture, session replay (low sample), performance monitoring
5. Source map upload trong CI build
6. Scrub PII (email, names) trước khi gửi

## Acceptance Criteria

- [ ] Sentry SDK wired vào kiteclass-frontend + kitehub-frontend
- [ ] Source maps uploaded trong CI
- [ ] PII scrubbing rule active (beforeSend hook)
- [ ] Sentry project dashboard cấu hình
- [ ] Test error trigger → hiển thị trong Sentry với correct release tag

## Related

- Audit: `documents/04-quality/audits/ops/ops-readiness-audit-2026-04-19.md` §4

## Log

- 2026-04-19 — Discovered in ops-readiness baseline audit
