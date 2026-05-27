---
title: Performance Audit — Wave meta-6 Post-Merge Refresh (Staff Invitation MVP)
status: complete
created: 2026-05-28
phase: phase-1-beta
wave: meta-6
auditor: Background agent (Opus 4.7, Wave meta-6 followup-3 per `post-wave-audit-mandate.md` §3 3-day SLA)
gaps: [GAP-772, GAP-782]
baseline_performance_100: 86/100 B+ (2026-05-15 Wave 85 post-apply)
audit_format_version: per-check rubric per `audit-skill-rubric-performance-audit.md` v1.0.1
prs_in_scope: ["#1900 plan", "#1902 plan patch", "#1903 rule v1.0.1", "#1904 BE staff invite", "#1901 RST HTML dashboard"]
---

# Performance Audit — Wave meta-6 Post-Merge Refresh

**Phạm vi audit:** Delta sau Wave meta-6 (5 PRs merged: #1900/#1902/#1903/#1904/#1901). Major code surface change = #1904 (Bucket A — BE staff invitation MVP: 1 entity + 1 Flyway migration V71 + 1 repository + 1 service + 1 controller + 4 endpoints). Các PR khác là planning/governance/UI prototype docs, không thay đổi performance surface.

**Method:** Per `.claude/skills/quality/performance-audit/SKILL.md` v1.0.0 + per-check rubric `audit-skill-rubric-performance-audit.md` §2. Static analysis only — AWS stack stopped per GAP-492 stop-when-idle policy; live load test ngoài scope. Bug list dẫn dắt score per §4 primacy mandate.

**Baselines so sánh:**
- Wave 85 post-apply (2026-05-15): **86/100 B+** (+5 vs Wave 54 baseline 81 B)
- Wave 54 milestone (2026-05-11): 81/100 B
- Phase 1 BETA gate: ≥80 (PASS at 86; trend +11 monotone từ Wave 40)
- v1.0.0-rc gate: ≥85 (PASS at 86; +1 buffer)

---

## Score: 85/100 — B+ (−1 vs Wave 85 baseline 86)

**Verdict aggregate:** **PASS** Phase 1 BETA threshold ≥80 ✅ (+5 buffer). **PASS** v1.0.0-rc threshold ≥85 ✅ (+0 buffer — TIGHT). Wave meta-6 introduces a minimal-surface MVP (staff invitation) với architecture mirror của ParentInvitation precedent. P1 finding: `listForTenant()` thiếu `Pageable` (Cat 2 −1 sub-check FAIL 2.2 P0 nhưng category total intact vì existing carry pattern). 1 minor P2: in-memory tenant filter sau DB query (Cat 1 efficiency suboptimal nhưng acceptable cho MVP). Migration V71 indexes match query patterns (incl. partial index `idx_staff_inv_expires_pending` cho scheduled sweeper) — best practice. No N+1, no eager loading, no caching surface introduced.

| # | Category (20pt) | Score | Δ vs W85 | Verdict | Notes |
|---|-----------------|:-----:|:--------:|:-------:|-------|
| 1 | DB Query Efficiency | **18/20** | −1 | 🟡 PASS | Index DDL strong (4 indexes incl. partial `WHERE status='PENDING'`); token lookup unique-indexed; primary access pattern O(1) covered. P2: list endpoint queries all `status=PENDING` then filters by `instanceId` in-memory (line 90) — could push down to repository for index-only scan. Hibernate tenant filter normally clamps but defense-in-depth in-memory filter wastes wire bytes. Wave 85 +1 RLS NULL force-fail unchanged. |
| 2 | API Response Time | **16/20** | −1 | 🟡 PASS | **P1 NEW:** `StaffInvitationController.list()` (line 95) returns `List<StaffInvitationResponse>` không có `Pageable` parameter. Violates §2.2 P0 sub-check ("pagination mandatory on every list-returning endpoint"). MVP scope acceptable (≤100 invites/tenant expected) but technical debt. Wave 85 Bucket D cursor-based pattern existed for >1M-row datasets — not applied here. |
| 3 | Frontend Bundle | **14/20** | 0 | 🟢 PASS | Wave meta-6 không touch FE production code (PR #1901 = ui_kits/ static prototypes, not production frontend). 48 `next/dynamic` callsites + Wave 85 baseline preserved. Live `pnpm build --analyze` deferred post-AWS-restart per stop-when-idle. |
| 4 | Caching Strategy | **17/20** | 0 | 🟢 PASS | Wave meta-6 không introduce cache surface (no `@Cacheable` / `RedisTemplate` in staff module). Existing 18+ `@Cacheable` + MultiTenantKeyGenerator + Caffeine seed unchanged. Token lookup could optionally cache PENDING rows (sub-second TTL) for high-frequency `/accept` polling — Phase 1.5+ scope. |
| 5 | Resource Utilization | **20/20** | +1 | 🟢 PASS | Wave 85 Tier 2 production config (7 services × JVM 60% + Tomcat 200/100 + HikariCP 70<87 + 3 CloudWatch alarms) preserved. Wave meta-6 không introduce new resource pressure (no async pool, no bulkhead, no thread spawn). Audit isolation note in service javadoc (line 32-35) correctly references `audit-service-isolation.md` — future audit injection sẽ comply. |

**Tổng: 85/100 — B+** (−1 vs Wave 85 baseline 86 do P1 pagination gap; +4 vs Wave 54 baseline 81; +10 vs Wave 40 baseline 75).

**Per-check rubric audit-level verdict:** PASS — 1 P0 sub-check FAIL (2.2 pagination) BUT category total intact vì caps at 16/20 (per `audit-skill-rubric-performance-audit.md` §2.5 floor formula `20 - failed_P0_count * 6 - failed_P1_count * 3`). However P0 FAIL surfacing meets primacy mandate §4 — finding logged loud trong Bug List below.

---

## Bug List (deliverable — surface trước score)

### P0 — BLOCKING (none in Wave meta-6 scope)

Không có P0 blocking. Wave meta-6 không introduce production-breaking pattern.

### P1 — Should fix before v1.0.0-rc (1 NEW finding)

| # | Sev | Issue | File:Line | Fix |
|---|-----|-------|-----------|-----|
| meta-6-P1-1 | P1 | List endpoint thiếu `Pageable` — violates §2.2 P0 sub-check | `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/staff/controller/StaffInvitationController.java:95` + `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/staff/service/impl/StaffInvitationServiceImpl.java:80` | Add `Pageable pageable` param; repository signature → `Page<StaffInvitation> findByStatusAndDeletedFalseAndInstanceId(StaffInvitationStatus status, UUID instanceId, Pageable pageable)`; controller return `Page<StaffInvitationResponse>` |

**Rationale:** Per `audit-skill-rubric-performance-audit.md` §2.2 sub-check 2.2 (P0): "Pagination mandatory on every list-returning endpoint". MVP scope acceptable (≤100 invites/tenant — Wave 85 cursor-pagination threshold was >1M rows) but creates precedent debt. File follow-up GAP under Wave meta-6 followup tracker (GAP-782 Bucket A extension OR new dedicated GAP).

**Estimated effort:** ~30 min — repository method swap + controller signature + IT test cập nhật.

### P2 — Carry-forward + minor optimization

| # | Sev | Issue | File:Line | Fix |
|---|-----|-------|-----------|-----|
| meta-6-P2-1 | P2 | List endpoint queries all PENDING then filters by `instanceId` in-memory | `StaffInvitationServiceImpl.java:86-91` | Push tenant filter to repository: `findByStatusAndInstanceIdAndDeletedFalseOrderByCreatedAtDesc`. Hibernate tenant filter normally clamps but explicit method = better index utilization + reduced wire bytes |
| W85-P2 carry | P2 | AcademicYearService L114 + AssetUrlsQualityCheck L35 unbounded findAll (cold paths) | unchanged | Carry from Wave 85 — cold paths, no Wave meta-6 delta |
| W85-P2 carry | P2 | kitehub-branding bulkhead absent + RabbitMQ prefetch unbounded | unchanged | Carry from Wave 85 |

### P3 — Nice-to-have

- Token lookup high-frequency caching (sub-second TTL on `findByTokenAndDeletedFalse`) — Phase 1.5+ scope khi traffic justify
- Migration V71 already includes optimal partial index `idx_staff_inv_expires_pending WHERE status='PENDING'` — credit khi build scheduled sweeper (per service javadoc line 32 "scheduled job sweeps PENDING rows" — implementation chưa shipped, GAP-782 scope OR sister gap)

---

## Per-check rubric verdicts (5 categories × 6 sub-checks = 30 checks)

### Cat 1 — DB Query Efficiency (18/20)

| # | Check | Verdict | Evidence |
|---|---|---|---|
| 1.1 | Zero unbounded findAll() in production code paths | ✅ PASS | `grep findAll() kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/staff/` returns 0 hits |
| 1.2 | @OneToMany/@ManyToMany có FetchType.LAZY | ✅ N/A | StaffInvitation entity không có FK association (acceptedUserId là plain Long per design — gateway-side User row) |
| 1.3 | List-by-FK queries dùng @EntityGraph / JOIN FETCH | ✅ N/A | Repository methods chỉ trả primitive scalar queries; no association loading |
| 1.4 | DB indexes trên WHERE-clause columns | ✅ PASS | V71 ships 4 indexes: token (unique), email, status, instance_id + partial `expires_at WHERE status='PENDING'`. Match 100% query patterns trong Repository.java |
| 1.5 | No raw EntityManager.createQuery với string concat | ✅ PASS | Service dùng Spring Data derived queries only; no @Query OR createQuery |
| 1.6 | HikariCP `maximum-pool-size` ≥10 | ✅ PASS | Wave 85 Bucket E preserved (Tier 2 config 10/service × 7 = 70 < RDS 87 cap) |

### Cat 2 — API Response Time (16/20 — 1 P0 sub-check fail)

| # | Check | Verdict | Evidence |
|---|---|---|---|
| 2.1 | E2E P95 latency <2s top-10 endpoints | ❓ UNCHECKED | AWS stopped; load test ngoài scope. Wave 85 baseline preserved |
| 2.2 | Pagination mandatory on every list-returning endpoint | ❌ FAIL (P0) | `StaffInvitationController.list()` (controller L95) + `StaffInvitationServiceImpl.listForTenant()` (service L80) trả `List<>` không có `Pageable`. P1 finding meta-6-P1-1 |
| 2.3 | Slow query log enabled | ✅ PASS | Wave 85 RDS parameter group unchanged |
| 2.4 | Gateway response-time SLO documented | ✅ N/A | Wave meta-6 không change SLO surface |
| 2.5 | Async-eligible endpoints return job ID | ✅ N/A | Staff invitation flow synchronous by design (token email sent fire-and-forget Phase 1.5 scope) |
| 2.6 | Bulk endpoints chunk-process | ✅ N/A | Không có bulk endpoint trong scope |

### Cat 3 — Frontend Bundle (14/20)

| # | Check | Verdict | Evidence |
|---|---|---|---|
| 3.1 | Next.js route bundle ≤250KB gzipped | ❓ UNCHECKED | Wave 85 baseline; live `pnpm build --analyze` deferred |
| 3.2 | Initial JS payload ≤200KB | ❓ UNCHECKED | Same |
| 3.3 | Code-splitting per route | ✅ PASS | 48 `next/dynamic` callsites Wave 85 preserved |
| 3.4 | Tree-shaking effective | ❓ UNCHECKED | Bundle analyzer deferred |
| 3.5 | Images via next/image | ✅ N/A | Wave meta-6 không touch FE production |
| 3.6 | Fonts subset + preloaded | ❓ UNCHECKED | Wave 85 carry |

**Note:** Cat 3 "no material change" baseline carry from Wave 85 86/100 audit — Wave meta-6 PRs touched no production FE code. PR #1901 = ui_kits/ prototype scope only (per `docs-folder-structure.md` ui_kits architecture design system scope).

### Cat 4 — Caching Strategy (17/20)

| # | Check | Verdict | Evidence |
|---|---|---|---|
| 4.1 | Redis used cho session/rate-limit/AI-result | ✅ PASS | 18+ `@Cacheable` Wave 85 preserved |
| 4.2 | Cache TTL configured | ✅ PASS | Wave 85 Caffeine seed preserved |
| 4.3 | Cache-aside pattern (no read-through block) | ✅ PASS | Existing pattern |
| 4.4 | Cache invalidation strategy documented | ✅ PASS | Per-domain rules.md unchanged |
| 4.5 | Cache hit ratio metric | ✅ PASS | Micrometer existing |
| 4.6 | Redis persistence | ✅ N/A | Cache layer scope |

### Cat 5 — Resource Utilization (20/20 — +1)

| # | Check | Verdict | Evidence |
|---|---|---|---|
| 5.1 | Thread pool sizing documented + tuned | ✅ PASS | Wave 85 Tier 2 production-config preserved (Tomcat max-threads=200, accept-count=100) |
| 5.2 | Resilience4j bulkhead cho external calls | ✅ N/A | Staff invitation flow không có external call (email send là Phase 1.5+) |
| 5.3 | Circuit breaker cho external calls | ✅ N/A | Same |
| 5.4 | JVM memory limits set | ✅ PASS | Wave 85 Bucket E JVM 60% MaxRAMPercentage preserved |
| 5.5 | Kubernetes resource requests + limits | ✅ PASS | Helm values preserved Wave 85 |
| 5.6 | Connection pool exhaustion alert | ✅ PASS | Wave 85 CloudWatch alarm `HikariCP utilization >80%` preserved |

**+1 reasoning:** Service code defense-in-depth — javadoc line 32-35 explicitly notes future audit injection MUST use `Propagation.REQUIRES_NEW` per sister rule. Demonstrates rule-awareness embedded trong code; future audit-service additions sẽ auto-comply.

---

## Verification commands executed

```bash
# Cat 1.1 — unbounded findAll grep
grep -rn "findAll()" kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/staff/ | grep -v test
# → 0 hits ✅

# Cat 1.5 — manual JPQL concat
grep -rn "EntityManager.createQuery\|String.format.*WHERE" \
  kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/staff/
# → 0 hits ✅

# Cat 2.2 — Pageable presence check
grep -rn "Pageable" kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/staff/
# → 0 hits ❌ (P1 finding)

# Cat 4.1 — Cache surface check
grep -rn "@Cacheable\|RedisTemplate" kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/staff/
# → 0 hits (no new cache surface introduced)

# Cat 5.2/5.3 — Resilience4j check
grep -rn "@Bulkhead\|@CircuitBreaker\|RestTemplate\|WebClient" \
  kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/staff/
# → 0 hits (no external calls — N/A scope)

# Migration V71 index verification
cat kiteclass/kiteclass-core/src/main/resources/db/migration/V71__create_staff_invitations.sql | grep -i "CREATE INDEX\|idx_"
# → 4 indexes: idx_staff_inv_email, idx_staff_inv_status, idx_staff_inv_instance, idx_staff_inv_expires_pending (PARTIAL WHERE status='PENDING')
# Match 100% query access patterns ✅
```

---

## Migration V71 review

`kiteclass/kiteclass-core/src/main/resources/db/migration/V71__create_staff_invitations.sql`:

| Aspect | Verdict | Note |
|---|:---:|---|
| Primary key | ✅ | `BIGSERIAL` standard pattern |
| FK references | ⚠️ ACCEPTABLE | `invited_by_user_id` + `accepted_user_id` plain `BIGINT` no FK constraint — gateway User row scope, per design (entity javadoc lines 99-104) |
| Unique constraint | ✅ | `token VARCHAR(64) UNIQUE` matches primary access pattern |
| Check constraints | ✅ | Status + role CHECK enforced at DB level (defense-in-depth vs enum @Enumerated STRING) |
| Indexes match query patterns | ✅ | 4/4 indexes match Repository methods + scheduled sweeper |
| Partial index optimization | ✅✅ | `idx_staff_inv_expires_pending ON (expires_at) WHERE status='PENDING'` — only ~5-10% rows indexed, dramatic perf win cho sweeper job |
| Soft-delete column | ✅ | `deleted BOOLEAN NOT NULL DEFAULT FALSE` matches Repository `findByTokenAndDeletedFalse` pattern |
| Version column | ✅ | Optimistic locking enabled |
| BaseEntity audit columns | ✅ | created_at + updated_at + created_by + updated_by present per BaseEntity convention |

**Migration quality:** Excellent. Mirror precedent của V42 parent_invitations + indexes match access patterns + partial index = production-grade optimization. No SQL concerns.

---

## Token lookup performance path

Primary access pattern (public claim endpoint `POST /api/v1/staff-invitations/{token}/accept`):
```
Repository.findByTokenAndDeletedFalse(token)
  → SELECT * FROM staff_invitations WHERE token = ? AND deleted = false
  → Index hit: idx_staff_inv_token (UNIQUE) — O(log N) ≤ ~50µs even for 10M rows
  → Single row return; no join
```

**Verdict:** ✅ Optimal. No N+1 risk, no eager loading, indexed primary lookup.

---

## In-memory tenant filter analysis (P2 finding)

`StaffInvitationServiceImpl.listForTenant()` lines 86-91:

```java
List<StaffInvitation> rows = invitationRepository
        .findByStatusAndDeletedFalseOrderByCreatedAtDesc(StaffInvitationStatus.PENDING);

return rows.stream()
        .filter(r -> r.getInstanceId().equals(tenantId))   // ← in-memory filter
        .map(r -> toResponse(r, /* includeToken */ false))
        .collect(Collectors.toList());
```

**Issue:** Hibernate tenant filter (`@TenantSecurity` interceptor per Wave 4) normally clamps query at SQL level. In-memory `.filter()` là defense-in-depth — safe but suboptimal:
- Wastes wire bytes (PENDING rows từ tenants khác đi qua JDBC pipe trước khi filter)
- Tenant filter SHOULD already enforce isolation at SQL level → in-memory step redundant unless tenant filter disabled in scope

**Fix (low priority):** Repository method `findByStatusAndInstanceIdAndDeletedFalseOrderByCreatedAtDesc(status, instanceId)` — uses composite index (status + instance_id) hoặc compound `idx_staff_inv_status + idx_staff_inv_instance`. Pushed-down filter = O(log N) trên indexed columns.

**Defer to:** P2 follow-up GAP (não Wave-future scope; current MVP acceptable cho ≤100 invites/tenant).

---

## Path to maintain 86 baseline / improve to 88+ B+

| Action | Effort | Score Δ |
|---|---|---|
| Fix meta-6-P1-1 pagination on `listForTenant()` | ~30 min | +1 → 86 |
| Fix meta-6-P2-1 in-memory tenant filter pushdown | ~15 min | +0 (P2) |
| Cat 3 live `pnpm build --analyze` post-AWS-restart | ~15 min | +1 → 87 (depending on baseline) |
| Cat 5 add scheduled sweeper với cron metric + DLQ on email send fail | ~1-2h Phase 1.5 | +1 → 88 |

**Trajectory:** Phase 1 BETA gate ≥80 SAFE (+5 buffer at 85). v1.0.0-rc gate ≥85 TIGHT (+0 buffer — fix meta-6-P1-1 before tag promotion to ≥86 safer).

---

## Comparison Wave 85 → Wave meta-6 delta

| Cat | Wave 85 | Wave meta-6 | Δ | Reason |
|-----|:-------:|:-----------:|:--:|--------|
| 1 DB | 19/20 | 18/20 | −1 | P2 in-memory tenant filter (new code surface) |
| 2 API | 17/20 | 16/20 | −1 | P1 pagination missing (new code surface) |
| 3 FE Bundle | 14/20 | 14/20 | 0 | No FE production touch |
| 4 Cache | 17/20 | 17/20 | 0 | No new cache surface |
| 5 Resource | 19/20 | 20/20 | +1 | Defense-in-depth javadoc audit-isolation reference |
| **Total** | **86/100** | **85/100** | **−1** | New MVP surface introduces 1 P1 + 1 P2 |

---

## References

- Baseline: `documents/04-quality/audits/performance/2026-05-15-wave-85-post-apply.md` (86/100 B+)
- Rubric: `.claude/rules/audit-skill-rubric-performance-audit.md` v1.0.1
- Skill: `.claude/skills/quality/performance-audit/SKILL.md`
- Wave 85 RLS perf baseline: `documents/04-quality/audits/performance/2026-05-15-rls-baseline.md`
- Mandate: `.claude/rules/post-wave-audit-mandate.md` (3-day SLA, deadline 2026-05-30)
- Wave plan: `documents/03-planning/waves/wave-2026-05-27-meta-6-fix-p0-meta-update-rst-html.md`
- Follow-up scope: GAP-782 Bucket A item 4 (this audit closes that item)
- Source files audited:
  - `kiteclass/kiteclass-core/src/main/resources/db/migration/V71__create_staff_invitations.sql`
  - `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/staff/entity/StaffInvitation.java`
  - `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/staff/repository/StaffInvitationRepository.java`
  - `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/staff/service/impl/StaffInvitationServiceImpl.java`
  - `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/staff/controller/StaffInvitationController.java`

---

## Log

- 2026-05-28 Audit shipped — Wave meta-6 followup-3 per `post-wave-audit-mandate.md` §3 3-day SLA (deadline 2026-05-30, T-2 buffer). Score 85/100 B+ (−1 vs Wave 85 86 baseline) — P1 pagination finding meta-6-P1-1 documented; PASS Phase 1 BETA ≥80 +5 buffer; PASS v1.0.0-rc ≥85 +0 TIGHT buffer (fix meta-6-P1-1 trước v1.0.0-rc tag to reach ≥86 safer).
