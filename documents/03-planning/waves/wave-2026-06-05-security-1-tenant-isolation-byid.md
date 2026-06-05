---
title: Wave security-1 — Cross-tenant by-id read leak fix (GAP-983)
status: complete
created: 2026-06-05
updated: 2026-06-05
waves: [security-1]
tag_primary: security
tags_secondary: [multi-tenant, isolation, owasp-a01, kiteclass-core]
counter: 1
date_launch: 2026-06-05
gaps: [GAP-983, GAP-746, GAP-749, GAP-362]
---

# Wave security-1 — Cross-tenant by-id read leak fix (GAP-983)

**Goal:** Đóng P0 cross-tenant by-id read leak (GAP-983) platform-wide trong kiteclass-core — tenant đọc được data tenant khác qua GET-by-id (OWASP A01 IDOR). Fix có IT proof (Testcontainers) + live re-walk KC-3 isolation 404, KHÔNG ship half-fix.

**Trigger:** Wave flow-kc3 KC-3 G1 walk 2026-06-05 surfaced LIVE leak (tenant khanh-phapluat đọc sky-education classes/14 + 27 sessions + teachers/10 → HTTP 200). Root cause đã investigated. Blocks KC-3 campaign-THÔNG + beta launch (PDPL personal data exposure across tenants).

**Campaign context:** Project đang Flow Verification Campaign sub-mode. GAP-983 = P0 security/data-isolation → qualifies override "pick-gap-to-fix pause" (PDPL hard angle + blocks campaign THÔNG cho mọi KC flow vì cùng leak class). Execute khi self-hosted runner ONLINE (cần full kiteclass-core IT suite validate).

## 1. Brainstorm

**Root cause (đã investigated Wave flow-kc3 — xem GAP-983 §Root-cause investigation):**
- `spring.jpa.open-in-view: false` (OSIV OFF, `application.yml:70`) + method `@Transactional` mở Hibernate session riêng mà `TenantFilterInterceptor.preHandle` (enable filter qua `entityManagerProvider.getIfAvailable()` + `setParameter`) KHÔNG reach.
- → Method `@Transactional` read (vd `ClassServiceImpl.getClass`) leak; method KHÔNG `@Transactional` (course/teacher/student getById) được filter áp dụng.
- `@Filter` applier thiếu trên **58 entity** extends BaseEntity (chỉ Lead/LandingPage/ContactMessage có) — leak platform-wide.
- Secondary: khi filter trả empty → `.orElseThrow(EntityNotFoundException)` map ra **500 không phải 404** (cần verify handler + filter param-not-set trên non-interceptor session).

**Fix layers (4):**
1. **Filter enablement reliable trên transaction-bound session** (CORE FIX) — chuyển/bổ sung enable filter để mọi `@Transactional` read method có filter + param set. Candidate design (spike Bucket 0): (a) `TransactionSynchronizationManager` register synchronization enable filter on session at txn begin; (b) Hibernate `Integrator` / `SessionFactoryBuilder` filter-on-open; (c) AOP `@Around` trên `@Transactional` set filter trên current `EntityManager`. Chọn design ít-blast + testable nhất.
2. **`@Filter` applier sweep** trên entity thiếu (GAP-749) — re-declare `@Filter(name="tenantFilter", condition="instance_id = :tenantId")` matching Lead pattern. **CẨN TRỌNG:** loại trừ entity cross-tenant by-design (OutboxEvent / AuditLog / Role / Permission / UserRole / ModerationQueue / Retention / DeletionRequest / platform-scope) — per-entity judgment, KHÔNG blanket 58.
3. **Exception→404 mapping** — filter trả empty → 404 (not 500/200); verify GlobalExceptionHandler + filter param luôn set.
4. **Defense-in-depth (optional/defer):** RLS FORCE trên bảng nhạy cảm trong kiteclass_shared (hiện OFF) — lớp 2 nếu app filter miss.

**Outside-in audit:** SKIP per `outside-in-coverage-trigger.md` §4 row "Gap fix cụ thể đã có root cause" — đây là security bug fix có root cause xác định, không phải user-facing feature brainstorm.

**Blocker:** self-hosted CI runner OFFLINE → full kiteclass-core IT suite (Testcontainers) không validate được trong session. Execute wave này CẦN runner online (per G1 gate IT proof). GAP-612 AWS suspension không block (fix local-validated).

## 2. Task Breakdown

| Bucket | Scope | Owner | Effort |
|---|---|---|---|
| 0 (Spike) | Design + spike filter-enablement-on-txn-session (3 candidate, pick 1) + verify trên 1 entity (Class) Testcontainers IT proof before broad sweep | Coordinator/agent | ~1-2h |
| A | CORE: implement chosen filter-enablement mechanism + unit/IT cho `@Transactional` read path isolation | agent | ~2-3h |
| B | `@Filter` applier sweep entity (GAP-749) với per-entity cross-tenant-exclusion review + matrix doc | agent | ~2-3h |
| C | Exception→404 mapping + filter param-not-set fix + IT (cross-tenant → 404 not 500) | agent | ~1h |
| D | Re-enable + de-flake GAP-362 `TenantIsolationIT` + extend by-id cases (course/class/teacher/session × @Transactional + non-txn) + full regression | agent | ~2h |
| E (defer) | RLS FORCE defense-in-depth migration + GUC wiring | defer Phase 1.5 | varies |

## 3. Scope

Full §3 expansion tại session start khi pick wave (per stub convention). Skeleton:
- **BE (kiteclass-core):** `config/TenantFilterInterceptor.java` + new filter-enablement component (Bucket A) + `common/entity/BaseEntity.java` (`@FilterDef`/`@Filter` reference) + 58 entity `@Filter` sweep (Bucket B, minus exclusions) + `GlobalExceptionHandler` (Bucket C) + `application.yml` (OSIV decision).
- **Test:** `integration/TenantIsolationIT.java` (extend by-id cases) + new `*PostgresIT` per module + Bucket A mechanism unit test.
- **Exclusion review (Bucket B):** entity cross-tenant by-design — list + rationale doc trong wave artifact.
- **Verify target:** re-walk KC-3 chain isolation (tenant khanh-phapluat GET sky's course/class/teacher/session → 404).
- **Dependency:** self-hosted runner ONLINE (IT suite). Ties GAP-746 (Enrollment/Invoice same class) + GAP-749 (repo sweep) + GAP-362 (test re-enable).

## 4. State-Check Evidence

Verified 2026-06-05 (Wave flow-kc3 investigation — đã empirical, không phải hypothesis):

| Symbol | Verify command | Verdict |
|---|---|---|
| OSIV off | `grep open-in-view kiteclass-core/src/main/resources/application.yml` | ✅ `open-in-view: false` (line 70) |
| `TenantFilterInterceptor` enable + setParameter | read `config/TenantFilterInterceptor.java:84-92` | ✅ enables on `getIfAvailable()` session + `setParameter` |
| `getClass` @Transactional → leak | read `ClassServiceImpl.java:200-202` + live test | ✅ `@Transactional(readOnly=true)` → cross-tenant GET 200 LEAK |
| `getCourseById`/`getTeacherById`/`getStudentById` no @Transactional → filtered | read service methods + live test | ✅ no `@Transactional` → filter applies |
| 58 entity thiếu `@Filter` | `grep -rln "extends BaseEntity" + grep -L "@Filter("` | ✅ 58 entities (3 marketing có) |
| `TenantIsolationIT.shouldIsolateCourseDataBetweenTenants` test gap | read test:146-170 | ✅ test CHỈ LIST endpoint, không test findById → coverage gap để lọt bug |
| Live leak proof | curl khanh-phapluat GET sky classes/14 | ✅ HTTP 200 (documented GAP-983) |

**Fix attempt v1 reverted:** `@Filter` 4 entity alone PARTIAL (getClass @Transactional vẫn leak) → confirms Bucket A (filter-enablement) là core, không chỉ Bucket B (@Filter sweep).

## 5. Verification Gates

| Gate | Owner | Criteria | Status |
|---|---|---|---|
| G1 — IT proof | Claude + runner | Testcontainers IT: cross-tenant GET-by-id → **404** cho course/class/teacher/session trên CẢ `@Transactional` + non-txn paths; own-tenant → 200; list isolation no regression; full kiteclass-core IT suite PASS (no regression from filter-enablement change) | ⬜ |
| G2 — live re-walk | Claude + User | Rebuild kiteclass-core + re-walk KC-3: tenant khanh-phapluat GET sky course/class/teacher/session → 404; sky owner own-access → 200 | ⬜ |
| G3 — production parity | Claude + User | Production (post GAP-612 restore): multi-tenant isolation verified + RLS decision (Bucket E) | ⬜ |

## 6. Agent Spawn Pattern

- Bucket 0 spike FIRST (sequential — design decision gates Bucket A/B/C).
- Sau spike: Bucket A (core) + B (sweep) + C (exception) có thể parallel (disjoint files) per `feedback_parallel_agent_strategy.md`; Bucket D (test) sau A/B/C merge.
- Opus agents BACKGROUND per `agent-model-opus-default.md` + `agent-background-spawn-default.md`.
- Bucket B exclusion review cần judgment — agent prompt PHẢI list per-entity decision + rationale (không blanket).

## 7. Closure Protocol

1. Flip `gap-status.csv`: GAP-983 DONE (after G1+G2 PASS) + GAP-746/GAP-749/GAP-362 reconcile (DONE/PARTIAL) + git mv → `phase-1-beta/closed/`.
2. ROADMAP §🎯 Current Status Snapshot — Wave security-1 closure entry.
3. Scope-Completeness Reconciliation table per `wave-closure-scope-completeness.md` §3 (Bucket E defer documented).
4. `bash scripts/prune-merged-worktrees.sh --yes` per `post-wave-cleanup.md`.
5. Frontmatter `status: draft → complete`.
6. Campaign §4: KC-3 (+ mọi KC flow blocked cùng leak class) unblock → re-verdict.
7. wave-history.jsonl append (new tag format per `wave-tag-numbering-convention.md` §2.5).
8. Post-wave security audit per `post-wave-audit-mandate.md` (security /100 refresh — isolation fix là security-critical).

## 8. Log

- **2026-06-05 (plan ship):** Filed sau Wave flow-kc3 KC-3 G1 walk surfaced P0 GAP-983 + root-cause investigated (OSIV off + @Transactional filter bypass + 58-entity @Filter gap). First `wave-security-N` tag (counter 1) per `wave-tag-numbering-convention.md`. Fix attempt v1 (@Filter 4 entity) reverted per `release-fix-retry-budget.md` §3 — confirms filter-enablement (Bucket A) là core fix, không chỉ @Filter sweep. Outside-in SKIP (root-cause fix per `outside-in-coverage-trigger.md` §4). EXECUTE BLOCKED until self-hosted runner ONLINE (G1 IT proof mandate). Ties GAP-746 (Enrollment/Invoice) + GAP-749 (repo sweep) + GAP-362 (test re-enable + by-id coverage). Full §3 scope + Bucket B exclusion matrix happens tại session start khi pick.
