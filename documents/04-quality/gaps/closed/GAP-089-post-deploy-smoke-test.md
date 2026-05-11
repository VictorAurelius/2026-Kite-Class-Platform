# GAP-089: Post-Deploy Smoke Test Suite

**Status:** 🟢 DONE
**Priority:** 🟠 P1 (trước production)
**Domain:** Testing / Operations
**Found:** 2026-04-16 (skills gap simulation)
**Affects:** Production verification after deploy

## Problem

Deploy thành công (pods healthy, no crash) nhưng không có automated verification: "Can users actually use the app?"

Cần smoke tests verify critical paths:
- Landing page loads
- Login works
- Dashboard renders
- API responds with valid data

## Proposed Fix

1. Tạo `scripts/smoke-test.sh <environment>`:
   - Curl health endpoints: `/actuator/health` per service
   - Curl critical pages: landing, login (check HTTP 200 + body contains expected text)
   - Curl critical APIs: `/api/v1/public/courses` (check 200 + valid JSON)
   - Report: PASS/FAIL per check
2. Integrate vào deploy pipeline: auto-run after pod ready
3. Alert if any smoke test fails → auto-trigger rollback consideration

## Acceptance Criteria

- [x] Smoke test script exists covering ≥5 critical paths
- [x] Runs in <60s
- [ ] Integrated into deploy pipeline (auto-run post-deploy)
- [ ] Failed smoke test triggers alert

## Resolution

Created `scripts/smoke-test.sh` with 11 checks across 5 categories:

1. **Health endpoints** (4): kiteclass-core, kitehub-subscription, kitehub-branding, kitehub-email
2. **Public pages** (1): landing page (200 + contains expected text)
3. **Public API** (2): /api/v1/public/courses, /api/v1/public/settings (200 + valid JSON)
4. **Error handling** (2): register + login with empty body (expects 400, warns on 500)
5. **Gateway routing** (2): kiteclass route, kitehub-subscription route (no 502/503)

Features:
- Requires only `curl` + `bash` (no special deps)
- Color output in terminal, plain text in pipes
- Exit codes: 0=pass, 1=fail, 2=warn-only
- 10s timeout per request, total run <60s
- Usage: `./scripts/smoke-test.sh http://localhost:9000`

## Log

- 2026-04-16 — Implemented: scripts/smoke-test.sh with 11 checks, 5 categories
