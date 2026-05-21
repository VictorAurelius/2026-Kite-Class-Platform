# GAP-699: kite-gateway compose service block thiếu JWT_SECRET + ENCRYPTION_MASTER_KEY passthrough

**Status:** 🟢 DONE (2026-05-21 — Wave 102.9 Bucket E, branch `feature/gap-699-gateway-jwt-secret-passthrough`)
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

- [x] `kitehub/docker-compose.kitehub.yml` `kite-gateway:` service block thêm `JWT_SECRET` env passthrough (matching subscription + admin pattern với `:?required` fail-fast) — diff shipped 2026-05-21 line 564-566
- [x] `kite-gateway` container env-resolve thành công: `docker exec kite-gateway env | grep JWT_SECRET` returns `JWT_SECRET=<SET>` + `ENCRYPTION_MASTER_KEY=<SET>` ✓
- [x] Gateway no longer crashes với `IllegalStateException: JWT_SECRET (or jwt.secret) is required`: `docker logs kite-gateway | grep -c "JWT_SECRET.*is required"` returns `0` (post-fix) vs N (pre-fix crash loop) ✓
- [x] `ENCRYPTION_MASTER_KEY` env passthrough also shipped (matching subscription + admin pattern; gateway secrets propagation per Wave 89 Bucket A GAP-604 scope)
- [ ] ~~`curl POST /api/auth/login` qua gateway port 9000 trả HTTP 200 + JWT~~ — DEFERRED: local stack có Postgres password mismatch (kite-postgres volume từ prior session vs new `.env` POSTGRES_PASSWORD). Separate infra regression filed follow-up. JWT_SECRET wiring (GAP-699 primary scope) verified DONE qua env-resolve check above; full E2E gateway→subscription→approve action depends on Postgres auth fix (orthogonal scope to JWT_SECRET passthrough). Wave 102.9.5 deferred per `pre-handoff-self-test-completeness.md` §5.4 override.

## Related

- Rule: `audit-to-gap-pipeline.md` §2.7 Decision-Doc Code-Sync (decision Wave 89 Bucket A "gateway needs JWT_SECRET" landed BE code-side but compose passthrough miss = stale ref)
- Wave 102.8 Bucket D Log (GAP-518/519) — code-side verified
- Wave 102.8.1 audit `documents/04-quality/audits/local-stack/2026-05-21-wave-102-8-1-browser-walk-verify.md` Finding #1
- GAP-518 §AC line "Approve/reject buttons fire correct endpoint" — separate scope per AC text, this gap unblocks
- GAP-695 self-test readiness — Tier-3 (full UI interactive) blocked by gateway path; Tier-2 (curl + chunk verify) DONE Wave 102.8.1

## Log

- **2026-05-21 (Wave 102.8.1)** — Gap created. Triggered bởi browser walk verify session phát hiện gateway container restart loop với `IllegalStateException: JWT_SECRET (or jwt.secret) is required`. Investigation: compose `kite-gateway` block không có `JWT_SECRET` env passthrough trong khi subscription + admin có. Application config `application.yml:685` `jwt.secret: ${JWT_SECRET:}` default empty → `JwtAuthenticationGatewayFilter.java:59` constructor throws. Wave 102.8.1 audit artifact §Finding #1 cite full evidence. Status OPEN; fix small (2-line compose edit + verify); Wave 102.9 candidate.
- **2026-05-21 (Wave 102.9 Bucket E)** — DONE. Shipped 2-line `kitehub/docker-compose.kitehub.yml` edit (sau `KITECLASS_CORE_URL` line 563): thêm `ENCRYPTION_MASTER_KEY` + `JWT_SECRET` env passthrough với `:?required` fail-fast pattern matching subscription (line 363-364) + admin (line 515-516). Verify evidence: (1) `git diff` shows 3-line addition (comment + 2 env vars) trong `kite-gateway:` block; (2) `docker compose up -d --no-deps --force-recreate kite-gateway` recreated container với new env passthrough; (3) `docker exec kite-gateway env | grep -E "JWT_SECRET\|ENCRYPTION_MASTER_KEY"` returns BOTH `=<SET>` (vars wired); (4) `docker logs kite-gateway | grep -c "JWT_SECRET.*is required"` returns `0` (zero crashes on JWT_SECRET — primary fix verified). E2E POST `/api/auth/login` qua port 9000 DEFERRED: orthogonal Postgres password mismatch trong kite-postgres volume blocks DB auth (separate infra issue — old volume password vs new `.env`). Per `pre-handoff-self-test-completeness.md` §5.4: JWT_SECRET wiring scope (this gap) verified DONE; full chain test deferred to next session sau Postgres volume reset OR follow-up infra gap. PR #1699 (merged 2026-05-21, branch `feature/gap-699-gateway-jwt-secret-passthrough`). Unblocks Wave 102.9 A/B/C/D batch.
