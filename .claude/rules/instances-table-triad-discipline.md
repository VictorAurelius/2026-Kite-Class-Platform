---
paths:
  - "kitehub/kitehub-platform/src/main/java/com/kitehub/platform/domain/entity/Instance.java"
  - "kitehub/kitehub-subscription/src/main/resources/db/migration/V*__*.sql"
  - "kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/repository/InstanceRepository.java"
  - "kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/service/InstanceService.java"
  - "kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/tenant/**/*.java"
---

# Instances Table Triad Discipline — entity ↔ migration ↔ caller atomic ship

**Priority:** 🟠 MANDATORY — cross-service triad governance
**Version:** 1.0.0
**Created:** 2026-06-02
**Last-Reviewed:** 2026-06-02
**Reviewer-Approver:** @nguyenvankiet (solo-dev — MINOR self-approve per `rule-change-process.md` §5; new rule với built-in enforcement (reviewer-checklist + reuse existing `entity-mapper-consistency` CI job + worked self-test trên GAP-823 originating drift) per §6.5 Enforcement Parity Mandate; no constraint loosening — extends `design-patterns.md` §3.12 generic triad rule với cross-service `instances`-table specifics; META P0 force-multiplier per `meta-gap-priority.md` §3 — fix discipline 1 lần → eliminate trust-pass class permanently cho mọi future `instances`-table change)
**Applies to:** Mọi PR thay đổi schema bảng `instances` (CREATE TABLE / ALTER TABLE in Flyway V migration), Instance JPA entity (`kitehub-platform/domain/entity/Instance.java`), Instance repository (`kitehub-subscription/repository/InstanceRepository.java`), Instance service (`kitehub-subscription/service/InstanceService.java`), HOẶC TenantSlugNormalizer normalization helper

---

## 1. The Rule

> **Mọi thay đổi schema bảng `instances` (column add / drop / rename / constraint) PHẢI ship cùng PR với: entity field update (`Instance.java`), repository method update (`InstanceRepository.java`), service wiring update (`InstanceService.java` OR documented exempt nếu pure schema migration). Mọi class helper mới (vd `TenantSlugNormalizer`) PHẢI có ≥1 production call site trong cùng PR — class shipped without caller = dead-code DRIFT.**

`instances` table là multi-tenant root entity với cross-service ownership ambiguity (V migration trong `kitehub-subscription/db/migration/`, JPA entity trong `kitehub-platform/domain/entity/`, repository + service trong `kitehub-subscription/`). Triad drift recurrence ≥3 waves (V40 Wave 77 + V12 custom-domain backfill + V24 vertical-type) — fix-discipline 1 lần loại class.

Sister rule `design-patterns.md` §3.12 codifies generic Entity-Migration-Mapper triad cho mọi entity. Rule này specializes cho `instances` table với 2 extensions: (a) cross-service ownership boundary explicit, (b) caller-existence mandate cho normalization helpers (TenantSlugNormalizer class).

---

## 2. Why this rule exists

### 2.1 GAP-823 incident — Wave 77 Bucket D drift discovery (2026-06-01)

Wave onboarding-polish-2 pre-flight state-check surfaced 4 architectural triad drift items rooted trong Wave 77 Bucket D ship pattern:

| Symbol | Expected (per V40 + GAP-535 AC) | Actual state | Drift class |
|---|---|---|---|
| `Instance.slug` field | `@Column(name="slug")` field on entity | ❌ MISSING | Entity ↛ Migration |
| `InstanceRepository.existsBySlugStartingWith()` | Repository method cho collision-recovery loop | ❌ MISSING | Repository ↛ Migration |
| `InstanceService.createInstance()` calls `TenantSlugNormalizer.normalize()` | Service wires normalizer + collision 10-retry → 409 | ❌ MISSING | Service ↛ Helper |
| `TenantSlugNormalizer` production callers | ≥1 call site trong production code | ❌ ZERO (only test invokes) | Helper ↛ Caller (dead class) |

**Root cause Layer 1:** Wave 77 Bucket D shipped V40 migration + normalizer class but NOT entity field, repository method, service wiring. Original GAP-535 AC explicit mandated wiring; Wave meta-7 audit-catalog bulk flip GAP-535 → DONE based on "class file + migration file exists" heuristic without verifying wiring AC.

**Root cause Layer 2:** Cross-service ownership ambiguity. V40 migration lives in `kitehub-subscription/db/migration/` (PR author edits subscription module) while Instance entity lives in `kitehub-platform/domain/entity/` (separate Maven module). Triad reviewers naturally scope to module diff — cross-module drift escapes per-PR review.

**Root cause Layer 3:** Trust-pass anti-pattern recurrence ≥7 per `feedback_audit_of_trust_pass.md` memory — audit suite + Mockito tests + IT tests all PASS despite missing wiring. No RST walk per `feature-ship-runtime-walk-mandate.md` §3 caught miss at original ship.

### 2.2 Force-multiplier rationale per `meta-gap-priority.md` §3

`instances` table = multi-tenant root + cross-service ownership = highest-leverage entity trong system. 1 chuẩn triad discipline → mọi future column add (vertical-type, custom-domain, slug, future fields) auto-comply prospectively → eliminate trust-pass class permanently cho `instances`-table change.

---

## 3. Required artifacts per PR class

### 3.1 Schema change PR (V migration `ALTER TABLE instances`)

PR PHẢI include ALL bốn artifacts:

| Artifact | Path | Verify |
|---|---|---|
| **V migration** | `kitehub/kitehub-subscription/src/main/resources/db/migration/V[0-9]+__*.sql` | `ALTER TABLE instances` OR `CREATE TABLE instances` SQL present |
| **Entity field update** | `kitehub/kitehub-platform/src/main/java/com/kitehub/platform/domain/entity/Instance.java` | New `@Column(name="<snake_case>")` matches migration column name + nullable matches `NULL`/`NOT NULL` |
| **Repository method (if queryable)** | `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/repository/InstanceRepository.java` | Method named `findBy<Field>`, `existsBy<Field>`, etc. if column queryable |
| **Service wiring (if business field)** | `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/service/InstanceService.java` | Service reads/writes field via `instance.set<Field>(...)` HOẶC documented exempt nếu pure infrastructure column (vd audit timestamp via @PrePersist) |

Exempt cases per §4 override mechanism.

### 3.2 Normalization helper class PR (vd TenantSlugNormalizer)

Khi PR ships new helper class trong `kitehub-subscription/tenant/**` OR similar pattern (utility for transforming instance fields):

| Artifact | Verify |
|---|---|
| **Helper class** | New `*.java` file under `tenant/` package |
| **Production call site ≥1** | `grep -rln "<HelperClassName>\b" kitehub/kitehub-subscription/src/main/java` returns ≥1 file outside test sources |
| **Service wiring documented** | Service method's javadoc cites helper usage OR new field's `@Column` javadoc references normalization rule |

Banned: class shipped với ONLY test invocation (dead-class pattern per GAP-823 Layer 1).

### 3.3 Cross-module ownership note

Triad spans 2 Maven modules:
- **`kitehub-platform`** owns `Instance.java` entity (per existing convention — platform domain enum + entity layer)
- **`kitehub-subscription`** owns V migration + `InstanceRepository.java` + `InstanceService.java` + tenant helpers

PR description PHẢI cite both modules in diff scope khi triad change touches both sides:

```markdown
## Diff scope (per instances-table-triad-discipline.md §3.3)
- kitehub-platform: Instance.java entity field added
- kitehub-subscription: V<N>__<topic>.sql + InstanceRepository.java + InstanceService.java
```

---

## 4. Banned shortcuts + override mechanism

| ❌ Don't | ✅ Do |
|---|---|
| Ship V migration only, defer entity update | Triad atomic same PR OR mark gap PARTIAL với explicit follow-up |
| Ship helper class "for future wiring" | Wire ≥1 production caller same PR; otherwise YAGNI delete |
| Trust "audit + tests pass" cho DONE flip without RST walk | Per `feature-ship-runtime-walk-mandate.md` §3 — RST walk evidence required |
| Bulk audit-catalog flip GAP → DONE based on file-exists heuristic | Verify ALL Acceptance Criteria checkboxes ticked per `gap-done-discipline.md` §2 |
| Cross-module triad: split into 2 sequential PRs | Same PR — atomic ship; reviewer reviews both module diffs |
| Skip repository method "vì controller chỉ cần entity" | Repository method explicit when column queryable; controller uses repository |

### 4.1 Override trailer

Genuine exception (vd pure infrastructure column like `audit_created_at` via `@PrePersist`, NO repository/service touch needed):

```
git commit -m "...
INSTANCES_TRIAD_PARTIAL: V<N>__<topic>.sql — <reason — e.g. 'audit timestamp via @PrePersist, no business read/write'>
INSTANCES_TRIAD_FOLLOWUP: <gap link OR 'N/A — infrastructure only'>"
```

Trailer logged. Pattern frequency >10%/quarter triggers meta-review (likely §3.1 exempt list mis-defined).

---

## 5. Enforcement (per `rule-change-process.md` §6.5 Enforcement Parity Mandate)

### 5.1 Reviewer-checklist (active now)

Pre-merge review cho PR touching paths trong rule frontmatter:

- [ ] PR diff includes V migration `ALTER TABLE instances` OR `CREATE TABLE instances`?
- [ ] Nếu CÓ → Entity field update trong `Instance.java` same PR?
- [ ] Nếu column queryable → Repository method trong `InstanceRepository.java` same PR?
- [ ] Nếu business field → Service wiring trong `InstanceService.java` same PR? (HOẶC override trailer `INSTANCES_TRIAD_PARTIAL` cho infrastructure column)
- [ ] PR ships new helper class trong `tenant/**`?
- [ ] Nếu CÓ → grep production callers returns ≥1 file outside test sources?
- [ ] Cross-module diff (platform + subscription) — both module changes cited trong PR body §Diff scope per §3.3?

### 5.2 Reuse existing CI job `entity-mapper-consistency`

`scripts/check-entity-mapper-consistency.sh` (CI job `entity-mapper-consistency` trong `quality-code.yml`, WARN-mode v1 per `design-patterns.md` §3.12) heuristically scans entity fields → migration columns (camelCase → snake_case mapping). Rule này extends matrix với additional check: helper-class-without-caller pattern.

Future enhancement: extend script với check `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/tenant/*.java` classes — grep production callers, WARN if zero. Defer per `incident-to-rule-pipeline.md` §3.1 tightened legitimate-deferral conditions (recurrence count 1 today; revisit when recurrence ≥2 post-rule).

### 5.3 Memory auto-load (deferred per `incident-to-rule-pipeline.md` §3.1)

Memory entry `feedback_instances_table_triad_discipline.md` could remind tại session start trước khi modify Instance entity / V migration / InstanceService. Defer ≥7 ngày per premature-rule guard:
- **Detector complexity:** Trivial extension of existing `check-entity-mapper-consistency.sh` (helper-caller grep ~20 LOC)
- **Recurrence count:** 1 today (GAP-823 originating); revisit khi recurrence ≥2
- **Decision:** Reviewer-checklist §5.1 + worked self-test §6 + path-scoped auto-load đủ cho v1.0.0

### 5.4 Cross-rule integration

- **`design-patterns.md` §3.12 + §4 PR Review Checklist** — generic Entity-Migration-Mapper triad row applies; rule này specializes cho `instances` cross-service case
- **`gap-done-discipline.md` §2** — AC verification required trước DONE flip; rule này provides AC verification mechanism for `instances`-table gaps
- **`feature-ship-runtime-walk-mandate.md` §3** — RST walk required for user-facing features; tenant signup flow touches `instances` table → walk evidence mandatory before DONE flip

---

## 6. Self-test — GAP-823 originating incident

Apply rule retroactively to Wave 77 Bucket D PR (V40 ship moment):

### 6.1 Scope check (§3.1)

Wave 77 Bucket D PR diff scope (hypothetical retroactive review):
- ✅ V40 migration `ALTER TABLE instances ADD COLUMN slug VARCHAR(120)` — present
- ❌ Entity field `Instance.slug` — MISSING (drift)
- ❌ Repository `existsBySlugStartingWith()` — MISSING (drift)
- ❌ Service wiring `InstanceService.createInstance()` calls `TenantSlugNormalizer.normalize()` — MISSING (drift)
- ✅ `TenantSlugNormalizer` class — present
- ❌ Production caller cho `TenantSlugNormalizer` — ZERO (dead-class)

### 6.2 Cross-flow sweep evidence (per `cross-flow-bug-class-sweep.md` §3)

**Bug class signature:** `instances` table schema change without entity/repository/service triad ship.

**Grep command run:**

```bash
grep -ln "ALTER TABLE instances\|CREATE TABLE instances" \
  kitehub/kitehub-subscription/src/main/resources/db/migration/V*.sql
```

**Sites found + verdict (8 migrations modifying instances schema):**

| # | Migration | Verdict | Reason |
|---|---|---|---|
| 1 | `V1__create_instances_table.sql` | EXEMPT | Initial CREATE — Wave 1 baseline, entity created alongside |
| 2 | `V7__add_instance_contact_email.sql` | EXEMPT | Pre-rule grandfathered; verify follow-up GAP if drift surfaces |
| 3 | `V12__add_custom_domain.sql` | EXEMPT | Pre-rule grandfathered |
| 4 | `V17__add_purge_tracking.sql` | EXEMPT | Infrastructure column (@PrePersist class) — `INSTANCES_TRIAD_PARTIAL` exempt cho audit timestamp |
| 5 | `V18__add_notification_preferences.sql` | EXEMPT | Pre-rule grandfathered; FK to notification_preference table (separate entity) |
| 6 | `V19__add_migration_phase_column.sql` | EXEMPT | Pre-rule grandfathered |
| 7 | `V24__add_instance_vertical_type.sql` | EXEMPT | Pre-rule grandfathered |
| 8 | `V40__tenant_slug_normalize.sql` | **FIX (tracked GAP-823)** | DRIFT confirmed — triad incomplete; entity+repository+service missing |

**Decision:**
- Sites FIXED this PR: 0 (rule shipped same PR; fix scoped to Phase 1 of GAP-823 Wave meta-9 candidate)
- Sites DEFERRED: 1 (V40 — GAP-823 Phase 1 tracking)
- Sites EXEMPT: 7 (pre-rule grandfathered per §1 rule scope "applies prospectively")

### 6.3 Counterfactual

Without rule applied at Wave 77 Bucket D PR review:
- V40 + normalizer class shipped → audit suite PASS → GAP-535 marked PARTIAL (correct) → Wave meta-7 catalog flip → DONE (premature)
- Wave onboarding-polish-2 state-check 2026-06-01 surfaces triad drift → file GAP-823 META P0 → ~5h fix work (Phase 1: ADR + entity + repo + service + IT; Phase 2: META detector)

With rule applied:
- Reviewer-checklist §5.1 fires at PR review: "Entity field update same PR? No → REJECT"
- Wave 77 author files PR PARTIAL: V40 + normalizer + entity + repository + service all atomic OR explicit `INSTANCES_TRIAD_PARTIAL` trailer với follow-up gap
- GAP-535 closure honest (only flipped DONE when all 4 AC checked + RST walk passes)
- GAP-823 eliminated upstream — ~5h saved + audit trust preserved

**Verdict:** Rule fires correctly trên GAP-823 originating drift. Self-test PASS ✅.

---

## 7. Relationship to other rules

- **`design-patterns.md`** §3.12 Entity-Migration-Mapper Triad Drift — sister rule (generic scope). Rule này specializes cho `instances` cross-service case + adds caller-existence mandate cho normalization helpers.
- **`gap-done-discipline.md`** §2 — AC verification before DONE flip; rule này provides AC verification mechanism cho `instances`-table gaps.
- **`feature-ship-runtime-walk-mandate.md`** §3 — RST walk before DONE flip cho user-facing features; tenant signup touches `instances` → walk evidence mandatory.
- **`cross-flow-bug-class-sweep.md`** §3 — sister-flow sweep after bug fix; rule này extends sweep direction tới same-table schema-history audit.
- **`audit-to-gap-pipeline.md`** §2.8 — fix-time state-check; rule này provides state-check criteria cho `instances`-table fix PRs.
- **`meta-gap-priority.md`** §3 — META P0 force-multiplier; fix discipline 1 lần → mọi future `instances` change auto-comply.
- **`incident-to-rule-pipeline.md`** — rule này = direct output GAP-823 Wave onboarding-polish-2 state-check 2026-06-01 applied through 5-stage pipeline.
- **`rule-change-process.md`** §6.5 Enforcement Parity Mandate — rule + reviewer-checklist + reuse existing CI job + worked self-test §6 paired same PR (Wave local-doable-8 Bucket A).
- **`context-budget-mandate.md`** §3.2 — path-scoped `paths:` frontmatter (5 specific paths) per §3.1 narrow scope; rule loads only khi PR touches Instance entity / V migration / InstanceRepository / InstanceService / tenant helpers.

---

## 8. Log

- **2026-06-02 (v1.0.0):** Rule created in response to GAP-823 META P0 (filed 2026-06-01 Wave onboarding-polish-2-execute state-check). Triggered by 4-item architectural triad drift discovery rooted trong Wave 77 Bucket D ship pattern (V40 migration + TenantSlugNormalizer class shipped without entity/repository/service/caller wiring). Per `incident-to-rule-pipeline.md` 5-stage applied: Detect ✓ (state-check 2026-06-01 surfaced triad drift TRƯỚC khi commit code Bucket B fix) → Classify ✓ (existing rule `design-patterns.md` §3.12 covers generic Entity-Migration-Mapper triad nhưng KHÔNG specialize cho cross-service `instances`-table ownership boundary + KHÔNG mandate helper-class-caller-existence) → Rule+Enforce ✓ (this file + reviewer-checklist §5.1 + reuse existing CI job `entity-mapper-consistency` + worked self-test §6 on GAP-823 originating incident + cross-flow sweep evidence §6.2 + paired same-PR Wave local-doable-8 Bucket A per `rule-change-process.md` §6.5 Enforcement Parity Mandate) → Self-Test ✓ (§6 worked example trên 8-migration retroactive sweep — rule fires correctly cho V40 drift + correctly exempts pre-rule grandfathered V1/V7/V12/V17/V18/V19/V24) → Retro Log ✓ (this entry). META P0 force-multiplier per `meta-gap-priority.md` §3 — fix triad discipline 1 lần → mọi future `instances`-table schema change auto-comply prospectively → eliminate trust-pass anti-pattern recurrence ≥7 class permanently cho `instances` scope. Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5 — new constraint codifying previously-uncovered cross-service triad discipline; no constraint loosening; existing 7 pre-rule migrations grandfathered per §6.2 sweep verdict; rule applies prospectively từ this PR forward 2026-06-02). Atomic-unique-bar §5.1 check passed: ✅ atomic (single concept: `instances`-table triad atomic ship) + ✅ unique (sister `design-patterns.md` §3.12 covers generic scope, this specializes cross-service + helper-caller mandate) + ✅ widely applicable (every `instances`-schema change + every tenant helper class) + ✅ body discipline §1 ≤2 "and" conjunctions. Memory auto-load §5.3 + CI detector extension §5.2 deferred per `incident-to-rule-pipeline.md` §3.1 tightened conditions (recurrence count 1 today; revisit khi recurrence ≥2 post-rule); reviewer-checklist + path-scoped auto-load + worked self-test §6 sufficient cho v1.0.0.
