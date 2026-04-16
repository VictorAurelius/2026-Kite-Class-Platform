# GAP-089: Post-Deploy Smoke Test Suite

**Status:** 🔵 OPEN
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

- [ ] Smoke test script exists covering ≥5 critical paths
- [ ] Runs in <60s
- [ ] Integrated into deploy pipeline (auto-run post-deploy)
- [ ] Failed smoke test triggers alert
