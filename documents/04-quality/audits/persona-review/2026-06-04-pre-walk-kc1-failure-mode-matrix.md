# Pre-Walk Failure-Mode Matrix — KC-1 Tenant Provisioning + Lifecycle + Settings

**Date:** 2026-06-04
**Flow:** KC-1 (auto-triggered từ KH-2b owner signup register-via-invite)
**Audit type:** Outside-in failure-mode matrix (3-axis: Actions × Entities × Edge cases) per `.claude/skills/quality/simulation-gap-finder/SKILL.md`
**Skill:** `simulation-gap-finder` adapted cho pre-walk (per `pre-walk-persona-simulation-mandate.md` v1.0.0)
**Coordinator:** Claude (Opus 4.7 1M)

---

## 0. Scope re-state-check (per `simulation-gap-finder` Step 0)

**KC-1 per `flow-verification-campaign.md` §3 dependency graph:**

```
KH-1.S5 register-via-invite → KH-2b (registerFromBetaInvite)
   ↓ [auto-trigger]
KC-1 = Tenant provisioning + lifecycle + settings
   ↓
KC-2 Staff invite + RBAC
```

**Empirical state-check 2026-06-04:**

| Artifact | Status | Evidence |
|---|---|---|
| `TenantProvisioningSaga` | ✅ EXISTS in `kiteclass-core/src/main/java/com/kiteclass/core/module/provisioning/TenantProvisioningSaga.java` | Saga orchestrating NOT_STARTED → INITIALIZING → GENERATING → DEPLOYED |
| `TenantCreatedEvent` | ✅ EXISTS as POJO `kiteclass-core/.../provisioning/TenantCreatedEvent.java` | Plain class, no `@RabbitListener` consumer wiring found |
| **Publisher `tenant.created`** | ❌ **MISSING** in `kitehub-subscription` | `AuthService.registerFromBetaInvite` calls `instanceService.createTrialInstance(...)` SYNCHRONOUSLY — no `outbox.enqueue("tenant.created", ...)` |
| **`@RabbitListener(queues = "tenant.created.queue")`** | ❌ **NOT FOUND** in `kiteclass-core` | Only `class.rescheduled.queue` consumers exist |
| **`TenantSettings` entity** | ❌ **DOES NOT EXIST** | Settings scattered: `Instance.contactEmail`, `Instance.organizationName`, `system_config.locale` (global, not per-tenant); no `timezone`, `fiscalYear`, `schoolType`, `address`, `logo`, `phone` columns |
| `TenantSettingsController` | ❌ **NOT FOUND** | No endpoint surfaces — campaign §3 KC-1 lists "edit tenant settings" but BE code does not exist |
| RLS policy | ⚠️ PARTIAL — `V34__enable_rls_tenant_scoped_tables.sql` enables RLS on 12 kitehub-subscription tables BUT `kitehub-subscription` runs as superuser → RLS bypassed (V34 comment line 9-16). `kiteclass-core V58` FORCE RLS. |
| Trial-once enforcement | ✅ EXISTS — `InstanceService:131` rejects 2nd trial per owner — **conflicts with "Owner owns N tenants" claim in matrix Axis 2 E3** |

**Conclusion:** KC-1 in the form described by Axis A1 (auto-trigger) is **NOT IMPLEMENTED** end-to-end. The Saga exists as orphan code; the publisher is absent; the settings entity does not exist. Walking KC-1 today would surface ≥15 P0 findings before any happy path could complete.

---

## 1. Matrix axes (re-state)

- **Axis 1 (Actions):** A1 auto-provision · A2 RLS scoping · A3 default settings apply · A4 tenant-ready email · A5 onboarding wizard · A6 edit settings · A7 lifecycle transition · A8 cross-tenant switch
- **Axis 2 (Entities):** E1 Tenant · E2 TenantSettings · E3 User-Tenant relation · E4 Audit log · E5 Async job state · E6 Email outbox · E7 RLS policy · E8 CDC events
- **Axis 3 (Edge cases):** EC1 concurrent · EC2 partial failure · EC3 retry · EC4 idempotency · EC5 timeout · EC6 permission · EC7 validation · EC8 concurrency

8 × 8 × 8 = 512 cells. Reporting **27 cells with identified failure mode** (filtered by signal — discarded ~485 irrelevant intersections).

---

## 2. Findings (27, sorted by severity)

### P0 — feature-missing / data-loss / security

#### Cell A1×E5×EC2: No `tenant.created` event publisher in kitehub-subscription
- **Combination:** auto-trigger × async-job-state × partial-failure
- **Failure mode:** `AuthService.registerFromBetaInvite:218` calls `instanceService.createTrialInstance` synchronously, NO outbox enqueue or `rabbitTemplate.convertAndSend("tenant.created", ...)`. `TenantProvisioningSaga` (kiteclass-core) thus NEVER fires. Downstream (`FrontendInstance.status`) stays NOT_STARTED forever. KC-1 step "auto-trigger from KH-2b" is non-functional.
- **Pre-walk check:** `grep -rn "tenant.created\|TenantCreatedEvent" kitehub/kitehub-subscription/src/main/java` → 0 publisher hits
- **Severity:** P0
- **Related GAP:** new — propose `GAP-NEW-kc1-tenant-created-publisher`

#### Cell A1×E1×EC2: DB provisioning failure silently swallowed (silent partial provision)
- **Combination:** auto-trigger × Tenant × partial-failure
- **Failure mode:** `InstanceService:170-176` wraps `databaseProvisioningService.provisionDatabase(saved.getId())` in `catch (Exception e) { log.error(...); /* Continue */ }`. Instance row saved with `databaseUrl="pending"`, exception swallowed. User sees "tenant ready", but DB does not exist. KC-2+ all fail with cryptic errors.
- **Pre-walk check:** `psql -c "SELECT id, subdomain, database_url FROM instances WHERE database_url='pending'"` post-walk → expect 0 rows; if >0 → silent failures
- **Severity:** P0
- **Related GAP:** new — propose `GAP-NEW-instance-db-provision-silent-swallow`

#### Cell A6×E2×EC7: TenantSettings entity does not exist — no per-tenant timezone/fiscalYear/schoolType/locale
- **Combination:** edit-settings × TenantSettings × validation
- **Failure mode:** Search `grep -rn "TenantSettings\|tenant_settings" kitehub/ kiteclass/ --include="*.java"` returns 0 hits. `system_config.locale` is GLOBAL (`ProductionSeedRunner:130`), not per-tenant. KC-1.A3 "default settings apply" + KC-1.A6 "edit settings" have no underlying entity. Walking would 404 on every settings endpoint.
- **Pre-walk check:** `find . -name "TenantSettings*.java"` → 0 hits; `grep "tenant_settings" *.sql` → 0 hits
- **Severity:** P0
- **Related GAP:** new — propose `GAP-NEW-tenant-settings-entity-missing`

#### Cell A8×E3×EC4: Trial-once limit conflicts with "Owner owns N tenants" requirement
- **Combination:** cross-tenant-switch × User-Tenant relation × idempotency
- **Failure mode:** `InstanceService:131` rejects 2nd trial per owner: `existsByOwnerIdAndTrialStartedAtIsNotNull(ownerId)` → throws `IllegalArgumentException`. But `findByOwnerIdAndDeletedFalse` returns `List<Instance>` (multi-instance ownership). Production-equivalent: Owner cannot test tenant-switch flow because they cannot create 2nd tenant. Cross-tenant E2E test infeasible.
- **Pre-walk check:** `grep -A2 "existsByOwnerIdAndTrialStartedAtIsNotNull" kitehub/kitehub-subscription/.../InstanceService.java`
- **Severity:** P0 (blocks Axis A8 entire walk)
- **Related GAP:** [GAP-532](../../gaps/phase-1-beta/GAP-532-multi-tenant-tenant-switch-flow-coverage-gap.md) (multi-tenant tenant-switch coverage gap)

#### Cell A2×E7×EC6: RLS bypassed on kitehub-subscription tables (superuser runtime)
- **Combination:** RLS scoping × RLS policy × permission
- **Failure mode:** `V34__enable_rls_tenant_scoped_tables.sql:9-16` documents that policies created BUT `ENABLE ROW LEVEL SECURITY` NOT FORCED — kitehub-subscription runs as superuser → all RLS policies bypassed. Cross-tenant data leak surface present. `kiteclass-core V58` is FORCE; kitehub-subscription is not.
- **Pre-walk check:** `psql -c "SELECT relname, relforcerowsecurity FROM pg_class WHERE relname IN ('instances','staff_invitations','onboarding_progress')"` — `relforcerowsecurity=false` → not enforced
- **Severity:** P0 (OWASP A01 risk)
- **Related GAP:** [GAP-466](../../gaps/phase-1-beta/GAP-466-multi-tenant-postgres-rls.md) (RLS defense-in-depth, PARTIAL 90%)

#### Cell A4×E6×EC2: No "Tenant ready" email — `sendTenantReadyEmail` does not exist
- **Combination:** tenant-ready email × email outbox × partial-failure
- **Failure mode:** `EmailServiceClient` exposes `sendBetaInviteEmail` + `sendInviteStaffEmail` only. No `sendTenantReady`. After `registerFromBetaInvite` success, user receives JWT + redirected to dashboard but NO confirmation email with onboarding link / SLA / support contact. Recovery path absent if user closes tab.
- **Pre-walk check:** `grep -n "sendTenantReady\|tenant.ready" kitehub/kitehub-subscription/src/main/java -r` → 0 hits
- **Severity:** P0 (UX recovery; trust signal)
- **Related GAP:** new — propose `GAP-NEW-tenant-ready-email`

#### Cell A1×E4×EC2: Tenant provisioning has no audit log row
- **Combination:** auto-provision × audit log × partial-failure
- **Failure mode:** `AuthService.registerFromBetaInvite` logs via `log.info` only. No row written to `admin_audit_logs` or domain audit table for `TENANT_PROVISIONED` event. PDPL Art 11 + Wave 85 immutable admin audit work does NOT cover tenant creation. Cannot answer "when was tenant X provisioned, by which beta-invite, IP, fingerprint" post-incident.
- **Pre-walk check:** `psql -c "SELECT * FROM admin_audit_logs WHERE event_type LIKE '%TENANT%' LIMIT 1"` → expect rows; 0 → audit gap
- **Severity:** P0 (PDPL compliance)
- **Related GAP:** new — propose `GAP-NEW-tenant-provisioning-audit-log`

### P1 — significant correctness / UX

#### Cell A1×E1×EC1: Concurrent signup same subdomain — race window between `existsBySubdomain` + `save`
- **Combination:** auto-provision × Tenant × concurrent
- **Failure mode:** `AuthService.registerFromBetaInvite:230` checks `existsBySubdomainAndDeletedFalse(subdomain)` then saves. No `SELECT FOR UPDATE` or DB unique constraint enforcement guarantee. 2 concurrent calls with same subdomain → both pass check → 2nd save throws unique-constraint violation 500. Should map to friendlier 409.
- **Pre-walk check:** `grep -n "unique\|UNIQUE" kitehub/kitehub-subscription/src/main/resources/db/migration/V1__create_instances_table.sql` — verify unique constraint exists; then verify service handles `DataIntegrityViolationException` → 409 not 500
- **Severity:** P1
- **Related GAP:** new — propose `GAP-NEW-subdomain-concurrent-race`

#### Cell A1×E1×EC4: registerFromBetaInvite has NO idempotency key — double-submit creates orphan
- **Combination:** auto-provision × Tenant × idempotency
- **Failure mode:** Beta signup POST not idempotent (no `Idempotency-Key` header check). User double-clicks submit → 2 attempts; 1st succeeds (tenant created), 2nd fails on `existsByEmail`/`existsBySubdomain` (already taken by 1st). User confused. GAP-536 history shows POST /tenants had this fixed; beta-signup variant likely lacks same protection.
- **Pre-walk check:** `grep -n "Idempotency\|@IdempotencyKey" kitehub/kitehub-subscription/.../service/AuthService.java`
- **Severity:** P1
- **Related GAP:** [GAP-536](../../gaps/phase-1-beta/) (POST /tenants idempotency — but check whether beta-signup path covered)

#### Cell A3×E2×EC7: Default settings hardcoded `instance.databaseUrl="pending"` rather than per-tenant config
- **Combination:** default-settings × TenantSettings × validation
- **Failure mode:** `InstanceService:160` sets `databaseUrl="pending"` as PLACEHOLDER. If `provisionDatabase` swallows exception (P0 above), this string remains. Onboarding wizard / KC-2 reads `databaseUrl` → gets literal "pending" → connection 500. No validation that `databaseUrl != "pending"` post-provision.
- **Pre-walk check:** `psql -c "SELECT id FROM instances WHERE database_url='pending'"` post-walk → expect 0
- **Severity:** P1
- **Related GAP:** see also P0 silent-swallow finding

#### Cell A6×E2×EC8: 2 staff edit settings same time — last-write-wins, no optimistic locking
- **Combination:** edit-settings × TenantSettings × concurrency
- **Failure mode:** TenantSettings entity missing (P0 above); when added, JPA default `save` overwrites. No `@Version` optimistic lock → silent loss. Realistic for owner + 1 admin editing school address simultaneously.
- **Pre-walk check:** post-implementation, `grep "@Version" kitehub/.../TenantSettings.java` — expect present
- **Severity:** P1
- **Related GAP:** new — propose `GAP-NEW-tenant-settings-optimistic-lock`

#### Cell A7×E1×EC6: Lifecycle transition TRIAL→SUSPENDED has no role guard
- **Combination:** lifecycle × Tenant × permission
- **Failure mode:** `SubscriptionRenewalService:177` flips `instance.setStatus(InstanceStatus.SUSPENDED)` — caller authentication not enforced at service level (only at controller). If a non-admin endpoint indirectly calls this (e.g., webhook callback), suspension bypass possible.
- **Pre-walk check:** `grep -B5 -A10 "setStatus.SUSPENDED" kitehub/kitehub-subscription/.../service/` — verify every caller path has admin/owner guard at controller layer
- **Severity:** P1
- **Related GAP:** new — propose `GAP-NEW-lifecycle-transition-role-guard`

#### Cell A5×E3×EC5: Onboarding wizard async — no SLA / step timeout
- **Combination:** onboarding wizard × User-Tenant × timeout
- **Failure mode:** Onboarding wizard reads `OnboardingProgress.steps`. No timeout if user takes >24h. Trial clock starts at instance creation → user might lose days. Plus no resumption email after N hours idle.
- **Pre-walk check:** `psql -c "SELECT id, created_at, completed_steps FROM onboarding_progress WHERE created_at < NOW() - INTERVAL '2 days' AND completion_pct < 100"` — expect 0 stuck rows
- **Severity:** P1
- **Related GAP:** [GAP-531](../../gaps/phase-1-beta/GAP-531-tenant-init-handoff-post-admin-approve.md) (tenant init handoff, PARTIAL 45%)

#### Cell A8×E7×EC6: Tenant switch — JWT not re-issued, tenantId claim stale
- **Combination:** cross-tenant-switch × RLS policy × permission
- **Failure mode:** Owner with 2 tenants (after P0 trial-once fix): switching from A→B needs new JWT with `tenantId=B`. Current `TokenService:93` returns first instance from `findByOwnerIdAndDeletedFalse(...).stream().findFirst()` at login. No re-issue endpoint. Frontend stores tenantId in sessionStorage (jwt-storage.ts:39) but BE JWT claim still A → RLS uses A → cross-tenant leak OR data invisibility.
- **Pre-walk check:** Decode JWT after switch → verify `tenantId` claim updated
- **Severity:** P1
- **Related GAP:** GAP-532 (multi-tenant tenant-switch)

#### Cell A1×E8×EC2: No CDC event published to KiteClass downstream
- **Combination:** auto-provision × CDC events × partial-failure
- **Failure mode:** `kiteclass-core` needs to know tenant existence to provision per-tenant resources (DB schema, MinIO bucket). No outbox → no broker message → kiteclass-core unaware. `TenantProvisioningSaga` exists but never invoked (no @RabbitListener queue match).
- **Pre-walk check:** `grep -rn "@RabbitListener.*tenant" kiteclass/kiteclass-core/src/main/java` → 0 hits
- **Severity:** P1
- **Related GAP:** same as P0 publisher gap

#### Cell A4×E6×EC3: Tenant-ready email retry — no DLQ visibility for failed tenant-bootstrap emails
- **Combination:** tenant-ready email × email outbox × retry
- **Failure mode:** After P0 email is added, retry path unclear. EmailServiceClient uses subscription outbox + RabbitTemplate; if 3 retries exhausted, DLQ-handling not surfaced. User does not receive welcome email; no admin alert.
- **Pre-walk check:** `psql -c "SELECT COUNT(*) FROM outbox_events WHERE topic='tenant.ready.email' AND status='FAILED'"` post-walk → expect 0
- **Severity:** P1

#### Cell A6×E2×EC7: Validation — organization name `@Size(min=2, max=200)` but Vietnamese diacritic = 1 char but 2 bytes
- **Combination:** edit-settings × TenantSettings × validation
- **Failure mode:** `CreateInstanceRequest:34` `@Size(min=2, max=200)` operates on `String.length()` (char count). VARCHAR(200) DB column. If user inputs all-diacritic VN name (e.g., "Trường THPT Hà Nội Quốc gia Hồng Bàng"), char count OK but UTF-8 bytes may overflow if DB column is `VARCHAR(200 bytes)` not `VARCHAR(200 chars)`. Postgres `VARCHAR(n)` is character-based but worth verifying.
- **Pre-walk check:** `psql -c "INSERT INTO instances (organization_name) VALUES (repeat('Hồng', 50))"` → verify accepts 200-char VN name
- **Severity:** P2 (likely fine but verify)

#### Cell A1×E1×EC7: subdomain validation `[a-z0-9-]+` rejects Vietnamese — owners with "trường-mầm-non" must transliterate
- **Combination:** auto-provision × Tenant × validation
- **Failure mode:** `CreateInstanceRequest:30` `@Pattern("^[a-z0-9-]+$")` — DNS-safe, correct. But UX: owner enters "Trường Mầm Non Hồng" → app must auto-transliterate via `TenantSlugNormalizer`. Verify it does + collision recovery works (GAP-535 DONE).
- **Pre-walk check:** Walk slug-normalize: input "Trường Mầm Non" → expect "truong-mam-non" or similar
- **Severity:** P2 (covered by GAP-535 DONE)

#### Cell A7×E1×EC3: SUSPENDED→DELETED transition has no grace period / un-suspend retry
- **Combination:** lifecycle × Tenant × retry
- **Failure mode:** `InstanceService:523` flips to DELETED. No documented soft-delete grace window. Once DELETED, restoration path unclear. Subscription renewal can move ACTIVE→SUSPENDED→ACTIVE (SubscriptionRenewalService:124) but SUSPENDED→DELETED is one-way.
- **Pre-walk check:** `grep -A10 "InstanceStatus.DELETED" kitehub/kitehub-subscription/.../service/InstanceService.java`
- **Severity:** P2
- **Related GAP:** [GAP-201](../../gaps/) (off-boarding runbook, PARTIAL 50%)

### P2 — UX / observability / hygiene

#### Cell A3×E2×EC7: No fiscalYear / schoolType / academicYearStart default
- **Combination:** default-settings × TenantSettings × validation
- **Failure mode:** Even if TenantSettings entity added, no defaults per Vietnam edu context: fiscal year (Sep 1 - Jun 30 for K-12), school type (mầm non / tiểu học / THCS / THPT). KC-3 (academic year setup) downstream will fail because parent context missing.
- **Pre-walk check:** Post-implementation
- **Severity:** P2

#### Cell A5×E1×EC5: First-login redirect to /admin vs /dashboard ambiguous for OWNER role
- **Combination:** onboarding wizard × Tenant × timeout
- **Failure mode:** Wave flow-kh2 G2 walk surfaced this — `OWNER` role redirect default not consistent; depending on frontend buildtime, owner may land /admin (no permission) instead of /dashboard.
- **Pre-walk check:** Walk owner first-login → verify post-login URL == `/dashboard` not `/admin` or `/login`
- **Severity:** P2

#### Cell A4×E6×EC1: Tenant ready email + invite-staff email — concurrent send within 1s race
- **Combination:** tenant-ready email × email outbox × concurrent
- **Failure mode:** Owner signup completes → tenant-ready email queued → owner immediately invites staff → invite-staff email queued. Both go through subscription outbox. Order matters (tenant-ready BEFORE invite-staff). Outbox FIFO not guaranteed across topics.
- **Pre-walk check:** Walk + observe MailHog: tenant-ready arrives before invite-staff
- **Severity:** P2

#### Cell A6×E4×EC6: Owner edits settings of tenant they don't own — no per-row guard
- **Combination:** edit-settings × audit log × permission
- **Failure mode:** Even with role=OWNER, must verify `tenant.ownerId == auth.userId`. If endpoint just checks role without row scope, owner can edit ANY tenant via tenantId path param.
- **Pre-walk check:** POST settings with tenantId=other-tenant → expect 403
- **Severity:** P1 (security) — but no endpoint exists yet, so currently inert

#### Cell A1×E5×EC5: Provisioning saga `DEPLOYED` step has no SLA / dead-job sweep
- **Combination:** auto-provision × async job × timeout
- **Failure mode:** `TenantProvisioningSaga` AnalyzerService + PlannerService + PlanExecutor (AI branding). If AI provider hangs (Ollama down), saga stuck in GENERATING. No sweep job → tenant unusable indefinitely.
- **Pre-walk check:** `psql -c "SELECT id, status, updated_at FROM frontend_instances WHERE status NOT IN ('DEPLOYED', 'FAILED') AND updated_at < NOW() - INTERVAL '10 minutes'"` → expect 0
- **Severity:** P2

#### Cell A8×E4×EC6: No audit log for tenant-switch action
- **Combination:** cross-tenant-switch × audit log × permission
- **Failure mode:** Owner switching tenants — security-sensitive action — has no audit row. Cannot detect lateral-movement-after-compromise scenario.
- **Pre-walk check:** Walk tenant switch → `psql -c "SELECT * FROM admin_audit_logs WHERE event_type='TENANT_SWITCH' ORDER BY created_at DESC LIMIT 1"` → expect row
- **Severity:** P2

#### Cell A2×E7×EC1: app.current_tenant_id GUC reset between requests — connection pool may leak
- **Combination:** RLS scoping × RLS policy × concurrent
- **Failure mode:** Per `audit-service-isolation.md` family + GUC reset risk: if HikariCP connection returned to pool with stale `app.current_tenant_id`, next request on that connection (different tenant) may see cross-tenant data BEFORE filter applied. Wave 85 Bucket B fix verified for Postgres NULL force-fail — verify for kitehub-subscription too.
- **Pre-walk check:** Multi-tenant concurrent request test verifying GUC reset before each query
- **Severity:** P2 (covered by GAP-466 PARTIAL)
- **Related GAP:** GAP-466 (RLS defense-in-depth)

#### Cell A6×E4×EC2: Tenant settings change — audit log not written on partial failure
- **Combination:** edit-settings × audit log × partial-failure
- **Failure mode:** Per `audit-service-isolation.md` v1.0.0: if audit service uses default `@Transactional` propagation, audit failure rolls back the settings UPDATE. Need REQUIRES_NEW.
- **Pre-walk check:** Post-implementation, `grep "@Transactional.Propagation.REQUIRES_NEW" kitehub/.../TenantSettingsAuditService.java`
- **Severity:** P2

#### Cell A1×E1×EC7: instance.databaseUrl placeholder "pending" — no length validation if real URL > VARCHAR(255)
- **Combination:** auto-provision × Tenant × validation
- **Failure mode:** AWS RDS endpoint URLs can be ~100 chars; with SSL params + parameters can exceed 255. Verify `Instance.databaseUrl` column type.
- **Pre-walk check:** `\d instances` → check `database_url` column type ≥ VARCHAR(512) or TEXT
- **Severity:** P3

---

## 3. Summary

| Severity | Count |
|---|---:|
| P0 | 7 |
| P1 | 10 |
| P2 | 9 |
| P3 | 1 |
| **Total** | **27** |

**Root causes (by frequency):**
1. **Feature-missing class** (4 P0): no `tenant.created` publisher, no TenantSettings entity, no sendTenantReady email, no tenant-provision audit log
2. **Silent failure class** (2 P0): DB provision swallowed exception, trial-once limit blocks multi-tenant ownership
3. **Security class** (1 P0 + 3 P1): RLS bypass on kitehub-subscription, lifecycle role-guard, tenant-switch JWT staleness, settings permission per-row
4. **Concurrency/race class** (3 P1): subdomain race, idempotency, optimistic locking
5. **Observability class** (multiple P2): no audit for switch / settings change / DEPLOYED step timeout

**Walk readiness verdict for KC-1:** ❌ NOT READY. Recommend **deferring KC-1 walk** until ≥4 P0 fixed:
1. Add `tenant.created` outbox publisher in `AuthService.registerFromBetaInvite` + paired `@RabbitListener` consumer in kiteclass-core
2. Either remove silent-swallow on `databaseProvisioningService` OR document acceptable "pending" state with explicit retry path
3. Decision required: drop trial-once limit OR re-state campaign Axis A8 "Owner owns N tenants" as Phase 2 scope
4. Define `TenantSettings` entity OR re-scope KC-1 to "tenant lifecycle only, settings deferred"

**Recommended path forward:**
- File 7 new GAPs (1 per P0 above) per `discovery-to-gap-inline-filing.md` §3
- KC-1 wave plan must scope-down to ≤A1 + A7 minimum (skip A3/A4/A6/A8 until P0 closed)
- Tenant-switch (A8) → entirely defer to Phase 2 per GAP-532 unless trial-once decision flips

---

## 4. References

- Skill: `.claude/skills/quality/simulation-gap-finder/SKILL.md`
- Rule: `.claude/rules/pre-walk-persona-simulation-mandate.md` v1.0.0
- Rule: `.claude/rules/discovery-to-gap-inline-filing.md` v1.0.0
- Flow campaign: `documents/03-planning/roadmap/flow-verification-campaign.md` §3 KC-1
- Existing GAPs cited: GAP-466 (RLS), GAP-531 (init handoff), GAP-532 (tenant-switch), GAP-535 (slug normalize), GAP-536 (idempotency), GAP-704 (JWT tenantId claim)
- Source code anchors: `kitehub-subscription/src/main/java/com/kitehub/subscription/service/AuthService.java:218-269`, `InstanceService.java:123-180`, `kiteclass-core/.../provisioning/TenantProvisioningSaga.java:49-75`
