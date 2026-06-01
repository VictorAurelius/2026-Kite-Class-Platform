# GAP-536: POST /tenants idempotency key — prevent double-submit orphan tenants

**Status:** 🟡 PARTIAL — Wave 77 Bucket D backend foundation shipped (entity + service + repo + cleanup job + V41 + 7 tests); Wave onboarding-polish-2 Bucket C shipped HandlerInterceptor + CachingFilter + WebMvcConfig wire + 3 IT scenarios; FE submit-button debounce + live verify post-AWS-restore deferred
**Priority:** 🔴 P0 — BLOCKING Phase 1 BETA invite (P1 Solo teacher slow 3G common in VN)
**Domain:** Backend
**Found:** 2026-05-14 (Wave 77 — outside-in audit: failure-mode matrix F3)
**Affects:** P1 Solo teacher (mobile-first, slow 3G) — accidental double-tap → 2 tenant rows orphan billing
**Phase:** phase-1-beta

## Current State (verified 2026-05-14)

| Piece | File / Path | Status |
|-------|-------------|--------|
| Tenant create endpoint | `kitehub/kitehub-subscription/src/main/java/**/tenant/TenantController.java` | 🟡 verify-at-spawn |
| Idempotency-Key header support | (anywhere) | ❌ likely missing |
| `idempotency_keys` table | DB schema | ❌ missing |
| FE submit-button debounce | `kitehub-frontend/src/...signup form` | 🟡 verify-at-spawn — may exist partial |

## Problem

Wave 77 outside-in audit (2026-05-14) — failure-mode matrix F3 (P0): **P1 Solo teacher trên 3G chậm nhấn "Tạo trường" 2 lần (form không disable button + network slow)** → 2 POST /tenants requests → 2 tenant rows tạo trong DB race condition → 1 orphan tenant (no admin assigned, no billing).

Recovery cost: admin manual cleanup; user confusion ("tại sao 2 trường?"). Per VN connectivity context: 3G + mobile-first phổ biến cho gia sư cá nhân.

Standard fix: `Idempotency-Key` header (Stripe pattern) — request với same key returns cached response.

## Proposed Fix

1. **Schema:**
   - `V{N+1}__idempotency_keys.sql` — new table:
     ```sql
     CREATE TABLE idempotency_keys (
       key VARCHAR(128) PRIMARY KEY,
       endpoint VARCHAR(64) NOT NULL,
       request_hash VARCHAR(64) NOT NULL,
       response_status INT NOT NULL,
       response_body TEXT NOT NULL,
       created_at TIMESTAMP NOT NULL DEFAULT NOW(),
       INDEX idx_created_at (created_at)
     );
     ```
   - TTL: 24h cleanup job (or scheduled task)
2. **Middleware / Interceptor:**
   - Before POST `/tenants` handler:
     - Read `Idempotency-Key` header (required for mutation endpoints)
     - If key exists in `idempotency_keys` table → return cached `response_status` + `response_body`
     - If key missing → reject 400 `idempotency_key_required` (or auto-generate for forward-compat)
   - After handler success → cache response
3. **FE update:**
   - Frontend generates UUID v4 idempotency key per submit-attempt
   - Header `Idempotency-Key: <uuid>` on POST /tenants
   - Submit button disabled after 1st click (defense-in-depth — even if key works)
4. **Tests:**
   - 1st POST với key X → 201
   - 2nd POST với key X (same body) → 201 (same response, no new row)
   - 2nd POST với key X (DIFFERENT body) → 422 idempotency conflict

## Acceptance Criteria

- [x] DB migration V41 adds generic `idempotency_keys` table (PK key, endpoint, request_hash, response_status, response_body, created_at, expires_at) + indexes (expires_at + endpoint/created_at) — Flyway checksum-immutable per GAP-493 retro
- [x] `IdempotencyKey` entity + `IdempotencyKeyRepository` + `IdempotencyService` shipped: `findValidReplay(key, endpoint, hash)` returns cached row OR throws 422 on hash mismatch OR empty on miss/expired; `cacheResponse(key, endpoint, hash, status, body)` persists with 24h TTL; race-safe via `DataIntegrityViolationException` swallow
- [x] SHA-256 request hashing (`IdempotencyService.hashRequest`) — null-safe, deterministic
- [x] `IdempotencyConflictException` (422-class) — distinct from RuntimeException so controller advice can map to 422
- [x] `IdempotencyCleanupJob` `@Scheduled(cron = "0 0 4 * * *")` — daily 04:00 deletes expired rows; `@EnableScheduling` already wired in `KitehubSubscriptionApplication`
- [x] Unit tests cover: hash determinism, cache miss, cache hit + matching hash, expired-row-as-miss, hash mismatch → 422, persist success, persist-race-swallowed (7 tests pass)
- [x] **HandlerInterceptor wiring** into POST `/api/platform/instances` — `IdempotencyHandlerInterceptor` + `IdempotencyCachingFilter` shipped Wave onboarding-polish-2 Bucket C. Interceptor reads `Idempotency-Key` header, replays cached response qua `X-Idempotency-Replay: true` header, persists response trong `afterCompletion`. Path pattern khớp EXACT `/api/platform/instances` (POST), không cover sub-paths như `/extend-trial` (deliberate — chỉ tenant-create scope per F3 outside-in finding). 3 IT scenarios viết: same-key+same-body replay / same-key+different-body 422 / no-header proceeds normally.
- [ ] **FE submit-button debounce** + UUID v4 idempotency-key generation — frontend work, not backend scope
- [ ] **Live verify post-deploy:** 2 sequential POSTs same Idempotency-Key → 1 DB row + replay; same key different body → 422 — deferred next AWS-restore session per `feature-ship-runtime-walk-mandate.md` §5 `FEATURE_SHIP_WALK_DEFER`: AWS stack stopped (no production-equivalent env reachable từ current session)

## Related

- **Sibling Wave 77 outside-in:** GAP-533, GAP-534, GAP-535
- **Related:** GAP-535 (slug normalize — same controller surface)
- **Wave plan:** `documents/03-planning/waves/wave-2026-05-14-77-beta-invite-launch-foundation.md` Bucket D
- **Outside-in audit source:** Wave 77 failure-mode matrix F3 (2026-05-14)

## Log

- **2026-06-01** (Wave onboarding-polish-2 Bucket C — CI fix): Fix regression — `IdempotencyHandlerInterceptor` hard constructor dep trên `@Service IdempotencyService` làm vỡ TOÀN BỘ `@WebMvcTest` controller slices của subscription (70 errors: `NoSuchBeanDefinitionException` vì `@Service` không có trong web slice; `WebMvcConfig` auto-load mọi `HandlerInterceptor` trong slice). Switch sang `ObjectProvider<IdempotencyService>` (resolve lazily at request time) — production behavior identical, web slice degrade idempotency thành no-op. Cross-flow sweep (per `cross-flow-bug-class-sweep.md`): 2 interceptor còn lại (`AdminApiKeyInterceptor`, `MagicLinkCacheControlInterceptor`) dep-free → no sister site. Dọn unused `ModelAndView` import. Local verify: 31 web-slice tests PASS (BetaAccessController + OnboardingProgressController) + strict-warnings compile clean.
- **2026-06-01** (Wave onboarding-polish-2 Bucket C): IdempotencyHandlerInterceptor shipped — `IdempotencyHandlerInterceptor` + `IdempotencyCachingFilter` (wrap request/response body cho hashing) + `WebMvcConfig` wire trên POST `/api/platform/instances` (EXACT path, no sub-paths). 3 IT scenarios viết: (1) same-key+same-body replay 1 instance + `X-Idempotency-Replay: true` header; (2) same-key+different-body → 422 `idempotency_conflict`; (3) no-header proceeds normally, no cache row. IT execution local blocked by pre-existing Spring `set_config()` test infra issue (8 IT classes total fail trên H2 — pre-existing on main HEAD, confirmed via `InstanceProvisioningIT` baseline test; same exception class as GAP-735 / `admin-merge-discipline.md` precedent). 7 existing Mockito unit tests `IdempotencyServiceTest` PASS — no regression. CI canonical with Postgres infrastructure runs all 3 IT scenarios. completion_pct 65→80. Live verify defer next AWS-restore session per `feature-ship-runtime-walk-mandate.md` §5 LOCAL_SMOKE_SKIP override (AWS stack stopped).
- **2026-05-14** — Wave 77 Bucket D shipped backend foundation: `V41__idempotency_keys.sql` (generic per-endpoint table, distinct from V20 migration-specific 10-min TTL) + `IdempotencyKey` entity + `IdempotencyKeyRepository` (find + deleteExpired @Modifying) + `IdempotencyService` (find/cache + SHA-256 hash + race-safe save) + `IdempotencyConflictException` (422 mapping marker) + `IdempotencyCleanupJob` (daily 04:00 cron). 7 unit tests pass. Status → PARTIAL: backend infrastructure DONE; HandlerInterceptor wiring into POST `/api/platform/instances` + FE submit-button debounce + live verify deferred per scope split (foundation vs per-endpoint opt-in vs FE work — 3 distinct concerns, only the foundation belongs to this gap's security/correctness core).
- **2026-05-14** — Initial write-up. Wave 77 outside-in failure-mode matrix F3 surfaced. Stub in wave plan PR; full execution → Bucket D.
