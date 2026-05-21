# GAP-699: kite-gateway compose service block thiếu JWT_SECRET + ENCRYPTION_MASTER_KEY passthrough

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 (blocks full FE→BE chain via gateway port 9000; subscription direct port 8081 unaffected)
**Domain:** DevOps
**Found:** 2026-05-21 (Wave 102.8.1 browser walk verify — `documents/04-quality/audits/local-stack/2026-05-21-wave-102-8-1-browser-walk-verify.md` Finding #1)
**Affects:** Mọi local stack up profile chạm `kite-gateway` (`branding-only`, `branding-only-no-ai`, `beta-funnel`, `kc-only`, `full`). Production deploy KHÔNG affected (ECS task definition + Helm chart truyền env riêng).

## Problem

`kitehub/docker-compose.kitehub.yml` `kite-gateway:` service block thiếu `JWT_SECRET` env passthrough (và có khả năng cả `ENCRYPTION_MASTER_KEY` nếu gateway code dùng). Gateway code `kitehub/kitehub-gateway/src/main/java/com/kitehub/gateway/filter/JwtAuthenticationGatewayFilter.java:59` throws `IllegalStateException: JWT_SECRET (or jwt.secret) is required for kitehub-gateway. Must match the JWT_SECRET configured in kitehub-subscription so issued tokens can be validated.` khi `jwt.secret` empty.

Application config `kitehub/kitehub-gateway/src/main/resources/application.yml:685`: `jwt.secret: ${JWT_SECRET:}` (default empty).

Compose comparison:
- `kitehub-subscription` (line 364): `JWT_SECRET: ${JWT_SECRET:?JWT_SECRET is required - run ./scripts/setup.sh}` ✅
- `kitehub-admin` (line 516): `JWT_SECRET: ${JWT_SECRET:?...}` ✅
- `kite-gateway` block: **MISSING JWT_SECRET** ❌

Container restart loop log:
```
Caused by: java.lang.IllegalStateException: JWT_SECRET (or jwt.secret) is required for kitehub-gateway.
Must match the JWT_SECRET configured in kitehub-subscription so issued tokens can be validated.
	at com.kitehub.gateway.filter.JwtAuthenticationGatewayFilter.<init>(JwtAuthenticationGatewayFilter.java:59)
```

## Root Cause

Hypothesis: gateway service `JwtAuthenticationGatewayFilter` được introduce sau commit setup compose ban đầu (Wave 89 Bucket A per application.yml comment line 679 "GAP-604 (Wave 89 Bucket A): JWT signing secret cho JwtAuthenticationGatewayFilter"). Compose `kite-gateway` env block không được sync khi GAP-604 ship — coverage gap trong `audit-to-gap-pipeline.md` §2.7 decision-doc code-sync (decision: gateway needs JWT_SECRET; code-sync trên compose pattern bị miss).

## Proposed Fix

Thêm 2 dòng vào `kitehub/docker-compose.kitehub.yml` `kite-gateway:` service block, sau dòng `ADMIN_SERVICE_URL: http://kitehub-admin:8080` (line ~xxx):

```yaml
      # Security secrets (shared với subscription + admin per Wave 89 Bucket A GAP-604)
      ENCRYPTION_MASTER_KEY: ${ENCRYPTION_MASTER_KEY:?ENCRYPTION_MASTER_KEY is required - run ./scripts/setup.sh}
      JWT_SECRET: ${JWT_SECRET:?JWT_SECRET is required - run ./scripts/setup.sh}
```

**Verify khi fix:**
1. `docker restart kite-gateway`
2. `bash kitehub/scripts/wait-for-healthy.sh` — gateway tới healthy state
3. `curl -s -X POST http://localhost:9000/api/auth/login -d '{"email":"admin@kitehub.com","password":"Admin@KiteHub123"}'` → HTTP 200 + JWT
4. Re-run Wave 102.8.1 §2.4 item (g) approve action via gateway port 9000:
   ```bash
   JWT=$(curl ... port 9000)
   PENDING_ID=$(curl -H "Authorization: Bearer $JWT" http://localhost:9000/api/v1/admin/beta-requests | jq -r '.[0].id')
   curl -i -X POST -H "Authorization: Bearer $JWT" http://localhost:9000/api/v1/admin/beta-requests/${PENDING_ID}/approve
   ```
   Expected: HTTP 200 + approval mutation visible trong DB

## Acceptance Criteria

- [ ] `kitehub/docker-compose.kitehub.yml` `kite-gateway:` service block thêm `JWT_SECRET` env passthrough (matching subscription + admin pattern with `:?required` fail-fast)
- [ ] `kite-gateway` container starts healthy với current `.env` (post `setup.sh` generated values)
- [ ] `curl POST /api/auth/login` qua gateway port 9000 trả HTTP 200 + JWT (E2E gateway→subscription)
- [ ] `curl POST /api/v1/admin/beta-requests/{id}/approve` qua gateway port 9000 với Bearer JWT trả HTTP 200 (closes Wave 102.8.1 item (g) PARTIAL)
- [ ] Consider: `ENCRYPTION_MASTER_KEY` env passthrough also — verify gateway code references; add nếu cần

## Related

- Rule: `audit-to-gap-pipeline.md` §2.7 Decision-Doc Code-Sync (decision Wave 89 Bucket A "gateway needs JWT_SECRET" landed BE code-side but compose passthrough miss = stale ref)
- Wave 102.8 Bucket D Log (GAP-518/519) — code-side verified
- Wave 102.8.1 audit `documents/04-quality/audits/local-stack/2026-05-21-wave-102-8-1-browser-walk-verify.md` Finding #1
- GAP-518 §AC line "Approve/reject buttons fire correct endpoint" — separate scope per AC text, this gap unblocks
- GAP-695 self-test readiness — Tier-3 (full UI interactive) blocked by gateway path; Tier-2 (curl + chunk verify) DONE Wave 102.8.1

## Log

- **2026-05-21 (Wave 102.8.1)** — Gap created. Triggered bởi browser walk verify session phát hiện gateway container restart loop với `IllegalStateException: JWT_SECRET (or jwt.secret) is required`. Investigation: compose `kite-gateway` block không có `JWT_SECRET` env passthrough trong khi subscription + admin có. Application config `application.yml:685` `jwt.secret: ${JWT_SECRET:}` default empty → `JwtAuthenticationGatewayFilter.java:59` constructor throws. Wave 102.8.1 audit artifact §Finding #1 cite full evidence. Status OPEN; fix small (2-line compose edit + verify); Wave 102.9 candidate.
