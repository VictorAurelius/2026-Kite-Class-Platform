---
audience: dev
---

# GAP-776 — Gateway circuit-breaker 503 fallback cold-start (auth + admin)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-05-27 (Wave 106 RST Mảng B3 + D3 probe)
**Affects:** First user auth login + first admin instances fetch after backend restart
**Phase:** phase-1-beta

## Problem

Reproduce:
1. `POST /api/auth/login` (cold — sau khi `kitehub-subscription` restart): HTTP **503** với HTML body "Dịch vụ tạm ngưng — Auth service hiện không khả dụng. Vui lòng thử lại sau vài phút."
2. Retry 2-5 giây sau: HTTP **200** + JWT response normal.

Gateway logs:
```
WARN  c.k.g.controller.FallbackController - Circuit breaker triggered for auth service
Caused by: java.net.ConnectException: Connection refused
```

Same pattern reproduces với `GET /api/v1/admin/instances` (D3 luồng):
- Cold start: 503
- Retry: 200

## Root Cause

Gateway có Resilience4j circuit breaker fallback wired (good defensive practice). NHƯNG:
- Cold-start window (subscription container vừa restart) → connection refused → circuit open
- Fallback returns 503 + Vietnamese HTML "Dịch vụ tạm ngưng" (correct UX for sustained outage)
- Circuit half-opens after backoff, retries → close → 200

User-visible cost:
- First-of-session login lần đầu sau backend restart sẽ thấy "Dịch vụ tạm ngưng" → confused, có thể bỏ flow
- Real production scenario: deploy / health-check restart sẽ trigger này cho 1-2 user
- Retry logic FE-side chưa có

## Proposed Fix

Option A — FE retry layer: `kitehub-frontend/src/lib/api-client.ts` thêm exponential backoff retry (3 attempts × 2s) cho 503 responses → user không thấy "Dịch vụ tạm ngưng" thoáng qua.

Option B — Gateway tune circuit breaker: tăng `waitDurationInOpenState` 1s → 3s, `slidingWindowSize` to reduce flap. Less responsive nhưng less false-positive.

Option C — Both A + B: defensive depth.

## Acceptance Criteria

- [ ] Cold-start login probe: lần đầu 200 (no 503 flap) OR FE invisibly retries
- [ ] Gateway logs: circuit breaker trigger count ≤1 per restart event
- [ ] RST re-walk B3 + D3: no "Dịch vụ tạm ngưng" toast visible

## Related

- Wave 106 RST B3 + D3 evidence (this gap §Problem timestamps)
- Gateway code: `kitehub-gateway/.../FallbackController.java`
- Sister incident: 2026-05-26 cred-rotate cycle (per `pre-flight-aws-lifecycle-check.md` §6) — different symptom, same retry-resilience class
