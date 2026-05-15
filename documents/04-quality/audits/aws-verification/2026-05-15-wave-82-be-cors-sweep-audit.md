---
title: Wave 82 Bucket B — BE CORS_ALLOWED_ORIGINS pre-DNS-flip sweep audit
status: complete
created: 2026-05-15
phase: wave-82-fe-self-host
wave: 82
bucket: B
gaps: [GAP-568]
---

# AWS Verification Report — Wave 82 BE CORS sweep pre-DNS-flip

## Scope

Wave 82 Bucket B (GAP-568, P0 BLOCKING) yêu cầu sweep CORS_ALLOWED_ORIGINS trên 7 BE services trước khi DNS flip Bucket D cutover `https://kitehub.me` từ Vercel sang EC2 self-host. Mục tiêu audit:

- Inventory hiện trạng CORS config per service (yaml default + production override)
- Identify services cần update allowlist khi origin chính (production FE) thay đổi domain
- Xác định service NÀO delegate cho gateway và service NÀO có config riêng
- Recommend pending action cho user (config update + deploy strategy)

Audit này read-only per `agent-aws-access.md` §2 Tier 1 — không mutate production config. User decide final value và apply qua terraform/compose update.

## Commands run (Tier 1 read-only per `agent-aws-access.md` §2.1)

```bash
# Grep CORS config across all service application yamls
grep -rn "CORS_ALLOWED_ORIGINS\|allowedOrigins\|allowed-origins\|cors:" \
  kitehub/*/src/main/resources/application*.yml \
  kiteclass/*/src/main/resources/application*.yml

# Grep CORS env override in production compose
grep -n "CORS\|cors\|allowed-origin\|allowedOrigin" docker-compose.production.yml

# Grep Java CORS configurations (WebMvcConfigurer / CorsFilter / @CrossOrigin)
grep -rln "CorsConfig\|CorsFilter\|@CrossOrigin\|cors()" \
  kitehub/ kiteclass/ --include="*.java"

# Inspect each Java CORS config and per-service application.yml CORS block
```

## Findings

### Table — current CORS allowlist state per service

| # | Service | YAML default location | YAML default value | Production env override (docker-compose.production.yml) | Java CORS config | Wave 82 required additions |
|---|---|---|---|---|---|---|
| 1 | **kitehub-gateway** | `kitehub/kitehub-gateway/src/main/resources/application.yml:11` | `${CORS_ALLOWED_ORIGINS:http://localhost:3001,http://localhost:3000,http://kitehub-frontend:3001,http://kiteclass-frontend:3000}` (Spring Cloud Gateway `globalcors.corsConfigurations['[/**]'].allowedOrigins`) | ✅ **L216:** `CORS_ALLOWED_ORIGINS: "https://kitehub.me,https://www.kitehub.me,https://kitehub-victoraurelius-projects.vercel.app"` | — (gateway-level CORS via yaml) | **✅ Đã có `https://kitehub.me`** — không cần thay đổi nếu DNS flip giữ apex `kitehub.me`. Loại bỏ Vercel domain sau ≥7 ngày cutover stable (defer Bucket E). |
| 2 | **kitehub-subscription** | (no yaml CORS block) | — | — (no env in compose) | `WebMvcConfig.java` chỉ register `AdminApiKeyInterceptor`, KHÔNG có CORS filter | **✅ Delegate to kitehub-gateway** — không cần update. Service nằm sau gateway trong cluster network, không expose trực tiếp ra browser. |
| 3 | **kitehub-branding** | (no yaml CORS block) | — | — (no env in compose) | (no CORS config Java file found) | **✅ Delegate to kitehub-gateway** — same as subscription. |
| 4 | **kitehub-email** | (no yaml CORS block) | — | — (no env in compose) | (no CORS config Java file found) | **✅ Delegate to kitehub-gateway** — same as subscription. |
| 5 | **kitehub-admin** | (no yaml CORS block) | — | — (no env in compose) | (no CORS config Java file found) | **✅ Delegate to kitehub-gateway** — same as subscription. |
| 6 | **kiteclass-gateway** | `kiteclass/kiteclass-gateway/src/main/resources/application.yml:107,111` (comments only — "CORS handled by Nginx now"); `SecurityConfig.java:52` `@Value("${cors.allowed-origins:http://localhost:3000,http://127.0.0.1:3000,http://localhost:8090,http://127.0.0.1:8090}")` | `cors.allowed-origins` env-driven Java config; default = localhost only | ❌ **MISSING** — không có `CORS_ALLOWED_ORIGINS` hoặc `cors.allowed-origins` env trong compose `kiteclass-gateway` (service deferred to Phase 7 per compose L7 comment) | `SecurityConfig.java:78-107` CorsConfigurationSource + CorsWebFilter (HIGHEST_PRECEDENCE) | **⚠️ NEEDS UPDATE khi Phase 7 KC stack bring-up** — hiện KC stack chưa deploy production (Phase 7 polish), nhưng khi enable cần add `CORS_ALLOWED_ORIGINS=https://kitehub.me,https://kiteclass.kitehub.me` (hoặc origin tương ứng). |
| 7 | **kiteclass-core** | `WebMvcConfig.java` chỉ register `TenantFilterInterceptor`, KHÔNG có CORS filter | — | — | TenantFilter only, no CORS | **✅ Delegate to kiteclass-gateway** — same pattern as kitehub services delegate to kitehub-gateway. |

### Architecture verdict — single-gateway CORS pattern

KH stack: 4 backend services (subscription/branding/email/admin) → kitehub-gateway → browser. **CORS chỉ enforce tại gateway.** Các service downstream KHÔNG có CORS filter Java và KHÔNG có CORS env trong compose — đó là design intentional, không phải gap.

KC stack (deferred Phase 7): kiteclass-core → kiteclass-gateway → browser. **CORS enforce tại kiteclass-gateway** (Java `SecurityConfig` với `CorsWebFilter`). Hiện KC stack chưa deploy → no immediate Wave 82 blocker; nhưng khi Phase 7 bring-up cần add env override.

### Verdict per service

| Service | Wave 82 status | Action required pre-DNS-flip |
|---|---|---|
| kitehub-gateway | ✅ **READY** (allowlist đã có `https://kitehub.me`) | Verify deployed image picked up new env (Wave 81 Bucket F secrets-cross-svc-sync fail-fast pattern). Run preflight curl per §Pending action. |
| kitehub-subscription | ✅ **READY** (delegate to gateway) | No-op |
| kitehub-branding | ✅ **READY** (delegate to gateway) | No-op |
| kitehub-email | ✅ **READY** (delegate to gateway) | No-op |
| kitehub-admin | ✅ **READY** (delegate to gateway) | No-op |
| kiteclass-gateway | ⚠️ **DEFERRED Phase 7** (KC stack not deployed production) | Defer; tracking note in §Pending action. Phase 7 cần add `CORS_ALLOWED_ORIGINS` env. |
| kiteclass-core | ✅ **READY** (delegate to gateway, deferred Phase 7) | Defer with kiteclass-gateway |

**Summary:** **1/7 services** có active CORS allowlist (kitehub-gateway) và **đã include `https://kitehub.me`**. **4/7 services** (subscription/branding/email/admin) delegate to gateway → no per-service config needed. **2/7 services** (kiteclass-*) deferred Phase 7.

Wave 82 Bucket D DNS flip cutover `Vercel → EC2 self-host` giữ apex `kitehub.me` không đổi → **gateway allowlist hiện tại đã handle đúng origin**. Bucket B GAP-568 yêu cầu sweep + preflight verify, không yêu cầu thêm domain mới (vì DNS flip = swap target chứ không swap domain).

### Risk classification

- **P0 risk surfaced:** Không. Allowlist gateway đã có `kitehub.me`.
- **Hidden risk:** Service deploy chưa nhận env mới (Wave 81 Bucket F precedent — secrets-cross-svc-sync gate). Mitigation: chạy preflight verify per §Pending action TRƯỚC khi user trigger DNS flip Bucket D.
- **Forward risk:** Khi remove Vercel domain post-cutover (≥7 ngày stable), phải edit compose env + redeploy gateway. Track defer Bucket E hoặc Wave 83 cleanup.

## Prior actions verified (per `audit-to-gap-pipeline.md` §2.8)

| Action | When | Reference |
|---|---|---|
| GAP-507 — CORS production origins setup (added `kitehub.me` + Vercel cohort to gateway env) | Wave 71 (pre-Wave-82) | `docker-compose.production.yml:214-216` comment cites GAP-507 |
| `production-env-config-registry.md` v1.1.0 §11 — 3 audit scripts shipped Wave 71 Bucket E | 2026-05-13 | `scripts/audit-env-coverage.sh` + sister scripts; this audit complements với CORS-specific preflight |
| Wave 81 Bucket F — fail-fast env var sweep precedent | 2026-05-14 | Same cross-service consistency pattern này audit follow |
| `concurrent-production-mutation-ops.md` v1.0.0 — BE deploy + DNS flip serialize | 2026-05-12 | Reinforced cho Bucket B → Bucket D ordering |

## Pending (this op — user action)

| Action | Owner | Notes |
|---|---|---|
| Verify CORS sweep findings | User review | Đọc audit này; confirm Verdict per service |
| Run `bash scripts/sweep-be-cors-origins.sh --audit` | User / coordinator | Generate machine-readable inventory output để cross-check audit table |
| Run `bash scripts/sweep-be-cors-origins.sh --preflight https://kitehub.me` từ máy ngoài AWS VPC | User | TRƯỚC khi DNS flip Bucket D — verify gateway accept new origin với preflight OPTIONS |
| (Optional) `bash scripts/sweep-be-cors-origins.sh --suggest` | User | Emit docker-compose env diff snippet nếu cần thay đổi (hiện không cần — allowlist OK) |
| Track Phase 7 KC stack CORS env wire | Future wave | Khi enable kiteclass-* services production, add `CORS_ALLOWED_ORIGINS` env trong compose KC service blocks |
| Track remove Vercel domain post-cutover ≥7 days stable | Wave 83+ | Edit `docker-compose.production.yml:216` để bỏ `https://kitehub-victoraurelius-projects.vercel.app` |

**Concurrent-mutation-ops compliance:** Bucket B (this audit) MUST complete BEFORE Bucket D DNS flip trigger per `concurrent-production-mutation-ops.md` v1.0.0 §3.1 + GAP-568 Proposed Fix Bước 3. Serialize order: audit → preflight verify → (no config change needed) → DNS flip.

## Recommendations

1. **APPROVE Bucket D DNS flip readiness từ CORS perspective.** Gateway allowlist hiện tại đã include `https://kitehub.me` (production target apex). Vercel domain giữ trong allowlist cho rolling cutover window.
2. **MANDATORY preflight verify** trước khi user trigger DNS flip: chạy `bash scripts/sweep-be-cors-origins.sh --preflight https://kitehub.me` từ máy ngoài AWS VPC (không phải EC2 inside VPC — preflight verification phải mô phỏng browser thực) và confirm tất cả 6+ endpoints trả `HTTP 200/204` + `access-control-allow-origin: https://kitehub.me`.
3. **Defer KC stack CORS** sang Phase 7 tracking note; không block Wave 82 Phase 1 KH cutover.
4. **File follow-up gap (Wave 83+ cleanup)** để remove `kitehub-victoraurelius-projects.vercel.app` từ allowlist sau ≥7 ngày DNS flip stable (per `concurrent-production-mutation-ops.md` §3.1 post-cutover step).
5. **Per `pre-handoff-self-test-completeness.md` §2.2 anonymous flow:** sau Bucket D DNS flip, browser-test 1 anonymous POST `/api/v1/auth/request-beta-access` từ `https://kitehub.me` để confirm flow live (curl preflight verify là necessary nhưng không sufficient).

## References

- Gap: `documents/04-quality/gaps/GAP-568-wave-82-be-cors-allowlist-sweep-pre-dns-flip.md`
- Wave plan: `documents/03-planning/waves/wave-2026-05-15-82-fe-self-host.md` §3 Bucket B
- Sister gaps: GAP-565 (F6 SG), GAP-566 (F7 RAM), GAP-567 (F10 cert)
- ADR: ADR-031 (Phase 1 FE self-host strategy — referenced by GAP-568)
- Rules: `production-env-config-registry.md` §2 + §11, `pre-mutation-state-check.md` §3, `concurrent-production-mutation-ops.md` §3.1, `pre-launch-infra-hardening-checklist.md` §2.2 (CORS pre-launch P0 check), `pre-launch-owasp-rest-hardening-checklist.md` §2.1 (A01 broken access control per-resource authz — out of scope ở đây nhưng cross-reference)
- Wave precedent: Wave 81 Bucket F fail-fast env var sweep (4 secrets cross-service)
- Wave precedent: Wave 71 GAP-507 production CORS origins setup (added `kitehub.me` first)
- Compose: `docker-compose.production.yml:204-237` (kitehub-gateway service block); L7 comment "KC stack deferred to Phase 7 polish"
- Java: `kiteclass/kiteclass-gateway/src/main/java/com/kiteclass/gateway/config/SecurityConfig.java:52-107`
- Helper script (this PR): `scripts/sweep-be-cors-origins.sh`

## Per `pre-handoff-self-test-completeness.md` §2.2 anonymous flow checklist (post-Bucket-D readiness)

Pre-Bucket-D verify checklist user MUST execute trước khi flip DNS:

- [ ] `bash scripts/sweep-be-cors-origins.sh --audit` output matches §Findings table trong audit này
- [ ] `bash scripts/sweep-be-cors-origins.sh --preflight https://kitehub.me` exit 0 (no FAIL endpoints)
- [ ] `bash scripts/sweep-be-cors-origins.sh --preflight https://www.kitehub.me` exit 0 (cover www subdomain)
- [ ] Confirm gateway image currently deployed reflects compose L216 env value (verify via `aws ssm send-command` hoặc EC2 docker exec — out of scope cho audit này, thuộc Bucket B execution)

Khi 4 checkboxes ✅ → Bucket D DNS flip ready (CORS perspective).
