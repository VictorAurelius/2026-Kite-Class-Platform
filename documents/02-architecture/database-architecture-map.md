---
title: Database Architecture Map — Entity catalog, FK graph, RLS coverage, migration history
audience: mixed
status: active
created: 2026-05-19
last-reviewed: 2026-05-19
waves: [99b]
gaps: [GAP-672]
scope: Consolidated DB architecture report cho KiteHub + KiteClass — entity catalog, FK graph, migration history index, tenant_id propagation, sizing baseline, Postgres-specific type inventory
related:
  - documents/02-architecture/multi-tenant-architecture.md
  - documents/02-architecture/adr/ADR-001-k12-data-model.md
  - documents/04-quality/gaps/phase-1-beta/closed/GAP-466-rls-impl.md
---

# Database Architecture Map

**TL;DR:** Tài liệu này consolidate toàn bộ database architecture cho KiteHub Platform — 91 tables phân chia giữa `kitehub-subscription` (32 tables, control-plane) và `kiteclass-core` (59 tables, multi-tenant domain) — kèm FK graph, RLS coverage (51/91 tables = 56%), Flyway migration history (114 V-files), tenant_id propagation map, sizing baseline Phase 1 BETA, và Postgres-specific type inventory (15 JSONB columns + 6 Testcontainers IT). Sister-doc của [`multi-tenant-architecture.md`](multi-tenant-architecture.md) — file này KHÔNG duplicate RLS narrative, chỉ extend §3 với entity-level map.

**Audience:** Backend dev (debug "table X có RLS chưa?"), SRE (capacity planning), tech lead (compliance review).

---

## Section 1 — Entity Catalog

91 tables tổng cộng, phân chia theo owner service:

### 1.1 `kitehub-subscription` (32 tables — control-plane / shared infrastructure)

| # | Table | tenant column | RLS enabled? | Phase 1 BETA row estimate |
|:--|---|:---:|:---:|---:|
| 1 | `instances` | (root tenant table) | ❌ | ~5-20 |
| 2 | `subscriptions` | `instance_id` | ✅ (non-forced) | ~5-20 |
| 3 | `payments` | `instance_id` (via subscriptions) | ❌ | ~10-100 |
| 4 | `branding_jobs` | `instance_id` | ✅ (non-forced) | ~50-500 |
| 5 | `branding_templates` | (shared catalog) | ❌ | ~20-50 (seed) |
| 6 | `email_logs` | `instance_id` | ✅ (non-forced) | ~1k-10k |
| 7 | `email_sent_log` | `instance_id` | ✅ (non-forced) | ~1k-10k |
| 8 | `instance_contact_email` (via V7 alter) | inline `instances` | ❌ | (subset) |
| 9 | `branding_outbox` | `instance_id` | ❌ (outbox pattern) | ~100-1k |
| 10 | `users` | TBD (control-plane shared) | ❌ | ~20-100 |
| 11 | `email_verification` (V10 inline) | via `users` | ❌ | (subset) |
| 12 | `custom_domain` (V12 inline) | via `instances` | ❌ | (subset) |
| 13 | `ai_usage_log` | `instance_id` | ✅ (non-forced) | ~500-5k |
| 14 | `backup_records` | `instance_id` | ✅ (non-forced) | ~50-200 |
| 15 | `migration_outbox` | `instance_id` | ✅ (non-forced) | ~100-1k |
| 16 | `branding_lifecycle_events` | `instance_id` | ✅ (non-forced) | ~100-1k |
| 17 | `branding_instance_state` | `instance_id` | ✅ (non-forced) | ~5-20 |
| 18 | `branding_regenerate_usage` | `instance_id` | ✅ (non-forced) | ~50-500 |
| 19 | `migration_idempotency_key` | `instance_id` | ✅ (non-forced) | ~100-1k |
| 20 | `consent_record` | `tenant_id` | ✅ (non-forced) | ~50-200 |
| 21 | `dsar_ticket` | TBD | ❌ | ~0-10 |
| 22 | `system_config` | (admin seed) | ❌ | ~20-50 |
| 23 | `beta_access_request` | (signup queue) | ❌ | ~5-50 |
| 24 | `notification_preferences` | per-user | ❌ | ~20-100 |
| 25 | `idempotency_keys` | per-request | ❌ | ~1k-10k |
| 26 | `oauth_attempts` | per-user | ❌ | ~10-100 |
| 27 | `onboarding_progress` | `tenant_id` | ❌ | ~5-20 |
| 28 | `feedback_submissions` | TBD | ❌ | ~10-50 |
| 29 | `staff_invitations` | per-instance | ❌ | ~10-100 |
| 30 | `staff_invitation_audit_log` | `tenant_id` | ❌ (audit immutable) | ~50-500 |
| 31 | `login_audit_log` | TBD | ❌ (audit immutable) | ~1k-10k |
| 32 | `admin_audit_log(s)` | (admin scope) | ✅ (immutable per V50) | ~100-1k |

Phụ trợ entities (V37-V51 add columns/indexes inline): `recovery_codes`, `impersonation_audit_log`, account_lockout columns (trên `users`).

### 1.2 `kiteclass-core` (59 tables — multi-tenant domain)

Mọi tenant-scoped tables dùng column `instance_id` (alias semantic của `tenant_id` — per [multi-tenant §1](multi-tenant-architecture.md)). V58 ENABLE + FORCE RLS trên 51 tables.

| # | Table | RLS enabled? | Phase 1 BETA row estimate |
|:--|---|:---:|---:|
| 1 | `academic_years` | ✅ FORCED | ~5-20 per tenant |
| 2 | `assignments` | ✅ FORCED | ~100-1k per tenant |
| 3 | `attendance` | ✅ FORCED | ~10k-100k per tenant |
| 4 | `attendance_period` | ✅ FORCED | ~100-500 per tenant |
| 5 | `audit_log` | ✅ FORCED | ~1k-10k per tenant |
| 6 | `badges` | ✅ FORCED | ~10-50 per tenant |
| 7 | `branding` | ✅ FORCED | ~1-5 per tenant |
| 8 | `branding_resources` | ✅ FORCED | ~10-50 per tenant |
| 9 | `branding_versions` | ✅ FORCED | ~5-20 per tenant |
| 10 | `child_protection_audit_log` | ✅ FORCED | ~10-100 per tenant |
| 11 | `class_schedule_slots` | ✅ FORCED | ~100-500 per tenant |
| 12 | `classes` | ✅ FORCED | ~10-100 per tenant |
| 13 | `courses` | ✅ FORCED | ~20-100 per tenant |
| 14 | `curricula` | ✅ FORCED | ~5-20 per tenant |
| 15 | `deletion_requests` | ✅ FORCED | ~0-10 per tenant |
| 16 | `dmca_takedown_requests` | ✅ FORCED | ~0-10 per tenant |
| 17 | `enrollments` | ✅ FORCED | ~100-1k per tenant |
| 18 | `frontend_instances` | ✅ FORCED | ~1-3 per tenant |
| 19 | `grades` | ✅ FORCED | ~1k-10k per tenant |
| 20 | `grading_scales` | ✅ FORCED | ~5-20 per tenant |
| 21 | `holidays` | ✅ FORCED | ~20-50 per tenant |
| 22 | `homeroom_classes` | ✅ FORCED | ~10-50 per tenant |
| 23 | `incidents` | ✅ FORCED | ~10-100 per tenant |
| 24 | `invoices` | ✅ FORCED | ~100-1k per tenant |
| 25 | `moderation_queue` | ✅ FORCED | ~10-100 per tenant |
| 26 | `outbox_events` | ✅ FORCED | ~100-1k per tenant |
| 27 | `parent_complaint_queue` | ✅ FORCED | ~5-50 per tenant |
| 28 | `parent_invitations` | ✅ FORCED | ~50-200 per tenant |
| 29 | `parent_read_audit_log` | ✅ FORCED | ~500-5k per tenant |
| 30 | `parent_student_links` | ✅ FORCED | ~100-500 per tenant |
| 31 | `parents` | ✅ FORCED | ~100-500 per tenant |
| 32 | `payments` (kc-core scope) | ✅ FORCED | ~100-1k per tenant |
| 33 | `payroll_configs` | ✅ FORCED | ~1-5 per tenant |
| 34 | `payroll_periods` | ✅ FORCED | ~10-50 per tenant |
| 35 | `permissions` | ✅ FORCED | ~20-50 per tenant |
| 36 | `point_rules` | ✅ FORCED | ~10-30 per tenant |
| 37 | `quality_reports` | ✅ FORCED | ~10-100 per tenant |
| 38 | `rebrand_approvals` | ✅ FORCED | ~5-20 per tenant |
| 39 | `reward_redemptions` | ✅ FORCED | ~50-500 per tenant |
| 40 | `rewards` | ✅ FORCED | ~10-50 per tenant |
| 41 | `roles` | ✅ FORCED | ~10-20 per tenant |
| 42 | `semesters` | ✅ FORCED | ~5-20 per tenant |
| 43 | `student_bulk_import_jobs` | ✅ FORCED | ~10-50 per tenant |
| 44 | `student_points` | ✅ FORCED | ~1k-10k per tenant |
| 45 | `students` | ✅ FORCED | ~100-1k per tenant |
| 46 | `subject_grades` | ✅ FORCED | ~500-5k per tenant |
| 47 | `subject_sections` | ✅ FORCED | ~10-50 per tenant |
| 48 | `submissions` | ✅ FORCED | ~500-5k per tenant |
| 49 | `teachers` | ✅ FORCED | ~10-100 per tenant |
| 50 | `user_roles` | ✅ FORCED | ~10-100 per tenant |
| 51 | `vettings` | ✅ FORCED | ~10-50 per tenant |
| 52 | `admin_audit_logs` (V60) | ✅ NULL force-fail | ~100-1k per tenant |
| 53 | `class_schedules` | ⚠️ Not in V58 list (verify) | ~10-50 per tenant |
| 54 | `class_sessions` | ⚠️ Not in V58 list (verify) | ~500-5k per tenant |
| 55 | `course_prerequisites` | ⚠️ Not in V58 list (verify) | ~20-100 per tenant |
| 56 | `invoice_items` | ⚠️ FK to invoices (cascade tenant) | ~500-5k per tenant |
| 57 | `role_permissions` | ⚠️ M2M join | ~50-200 per tenant |
| 58 | `student_badges` | ⚠️ M2M join | ~100-1k per tenant |
| 59 | `teacher_courses` | ⚠️ M2M join | ~50-500 per tenant |

### 1.3 RLS Coverage Summary

- **Total tables:** 91 (32 kh-sub + 59 kc-core)
- **RLS enabled:** 51 (12 kh-sub non-forced + 39 kc-core forced — per V58 + V34)
  - kh-sub: 12 (`subscriptions`, `branding_jobs`, etc. — non-forced vì control-plane service không propagate `TenantContext`)
  - kc-core: 39 (FORCED — per V58 lists 51 candidates, ~39 actual deploy after `IF NOT EXISTS` skip)
- **Auto-excluded (intentional):** ~30
  - `instances` (root tenant table — no parent)
  - M2M join tables (cascade via FK)
  - Shared catalogs (`branding_templates`, `system_config`)
  - Audit immutable (`*_audit_log` — separate immutability policy)
  - Per-user/per-request (`idempotency_keys`, `oauth_attempts`)
- **RLS Coverage %:** 56% (51/91); 89% nếu loại trừ auto-excluded scope (51/57 tenant-scoped tables)

---

## Section 2 — FK Graph (Mermaid erDiagram)

Sample 25 high-value entities + relationships (KHÔNG exhaustive — full graph 91 tables would overflow render). Auto-gen full FK graph từ Flyway parser tracked Wave 100+ follow-up (xem §7).

```mermaid
erDiagram
    instances ||--o{ subscriptions : "has"
    instances ||--o{ branding_jobs : "owns"
    instances ||--o{ email_logs : "sends"
    instances ||--o{ users : "scopes"
    instances ||--o{ frontend_instances : "deploys"

    subscriptions ||--o{ payments : "billed_via"
    users ||--o{ login_audit_log : "audited_in"
    users ||--o{ oauth_attempts : "attempts"
    users ||--o{ user_roles : "assigned"
    user_roles }o--|| roles : "binds"
    roles ||--o{ role_permissions : "grants"
    role_permissions }o--|| permissions : "ref"

    students ||--o{ enrollments : "enrolled_in"
    students ||--o{ attendance : "tracked_in"
    students ||--o{ grades : "scored_in"
    students ||--o{ submissions : "submitted_by"
    students ||--o{ student_badges : "earns"
    students ||--o{ parent_student_links : "linked_to"
    parents ||--o{ parent_student_links : "links_to"

    classes ||--o{ enrollments : "contains"
    classes ||--o{ attendance : "scheduled_in"
    classes ||--o{ homeroom_classes : "managed_via"
    classes ||--o{ class_sessions : "sessions"
    classes ||--o{ class_schedule_slots : "scheduled"

    courses ||--o{ assignments : "has"
    courses ||--o{ teacher_courses : "taught_by"
    courses ||--o{ course_prerequisites : "requires"
    teachers ||--o{ teacher_courses : "teaches"

    assignments ||--o{ submissions : "receives"
    subject_sections ||--o{ subject_grades : "grades"

    invoices ||--o{ invoice_items : "items"
    invoices ||--o{ payments : "paid_by"

    academic_years ||--o{ semesters : "contains"
    semesters ||--o{ classes : "spans"
```

**Top FK targets** (most-referenced entities):

| Rank | Target table | Inbound FK count |
|:---:|---|:---:|
| 1 | `students` | 11 |
| 2 | `classes` | 6 |
| 3 | `instances` | 5 |
| 4 | `users` | 4 |
| 5 | `courses` | 4 |
| 6 | `roles` | 3 |
| 7 | `parents` | 3 |
| 8 | `academic_years` | 3 |

`students` là central entity của KiteClass domain — hầu hết FE/BE feature paths đi qua students; tối ưu index trên `(instance_id, id)` critical cho query perf.

---

## Section 3 — Migration History Index

Tổng **114 Flyway V-files** active (54 kh-sub + 60 kc-core), 0 trong các non-DB services (kitehub-platform, branding, email, admin, base, gateway — đều dùng kh-subscription DB hoặc stateless).

| Service | V-file count | Latest V | Breaking changes |
|---|:---:|:---:|:---:|
| `kitehub-subscription` | 54 | V54 (admin_audit_log enrichment) | 4 |
| `kiteclass-core` | 60 | V60 (admin_audit_logs RLS NULL force-fail) | 1 |
| `kitehub-platform` | 0 | — | 0 |
| `kitehub-branding` | 0 | — | 0 |
| `kitehub-email` | 0 | — | 0 |
| `kitehub-admin` | 0 | — | 0 |
| `kitehub-base` | 0 | — | 0 |
| `kitehub-gateway` | 0 | — | 0 |
| **Total** | **114** | — | **5** |

### 3.1 Breaking change migrations (require data migration / forward-only)

| V-file | Service | Change type | Data migration risk |
|---|---|---|---|
| `V15__alter_branding_templates_theme_config_to_text.sql` | kh-sub | ALTER COLUMN TYPE | Low — text expansion |
| `V22__generalize_migration_outbox_to_subscription_outbox.sql` | kh-sub | RENAME TO | Medium — rename touches references |
| `V42__login_audit_fingerprint_varchar.sql` | kh-sub | ALTER COLUMN TYPE | Low — inet → varchar(45) |
| `V46__align_audit_columns_to_bigint.sql` | kh-sub | ALTER COLUMN TYPE | Medium — int → bigint width |
| `V52__login_audit_ip_varchar.sql` | kh-sub | ALTER COLUMN TYPE | Low — same pattern V42 |

**Pattern observation:** Breaking changes cluster trên audit log columns (V42/V46/V52) — root cause là Postgres INET type không match Hibernate varchar binding natively. Mitigation pattern: switch sang VARCHAR(45) cho IPv4-mapped-IPv6 max length. Xem [§6 Postgres-Specific Type Inventory](#section-6--postgres-specific-type-inventory) cho IT coverage gap.

### 3.2 Recent significant migrations (last 10 per service)

**kh-subscription V45-V54 (Wave 56-92):**
- V45-V48: Staff invitations + RBAC seed + impersonation audit
- V49: Staff invitation audit log
- V50: RLS admin bypass NULL force-fail (admin_audit_logs immutability)
- V51-V52: OAuth attempts + login audit IP type fix
- V53: Beta request abort cleanup index
- V54: Admin audit log enrichment (5 cols + composite index)

**kc-core V51-V60 (Wave 56-92):**
- V57: (unknown — verify)
- V58: ENABLE + FORCE RLS trên 51 tables (GAP-466 Phase 1)
- V59: RLS admin bypass + NULL force-fail
- V60: admin_audit_logs (multi-tenant audit immutable)

---

## Section 4 — Tenant_id Propagation Map

Extends [`multi-tenant-architecture.md` §3](multi-tenant-architecture.md) — KHÔNG duplicate RLS narrative đầy đủ. Section này focus trên **column naming patterns** + **RLS policy snippet** per cluster.

### 4.1 Column naming conventions

| Convention | Used in | Tables |
|---|---|---|
| `instance_id UUID NOT NULL` | kh-sub (11 tables) + kc-core (51 tables FORCED + ~8 M2M cascade) | 70 tables |
| `tenant_id UUID NOT NULL` | kh-sub semantic alias (3 tables) | 3 tables (`consent_record`, `onboarding_progress`, `staff_invitation_audit_log`) |
| (no tenant column) | Control-plane shared catalogs + audit logs | ~17 tables |

`tenant_id` = `instance_id` semantically per [multi-tenant §1](multi-tenant-architecture.md). Column alias drift là technical debt — GAP candidate Wave 100+ cho unification (rename `tenant_id` → `instance_id` HOẶC standardize new tables on `tenant_id`).

### 4.2 RLS policy snippets per cluster

**Cluster A — `instance_id`-keyed (kh-sub non-forced, 11 tables):**

```sql
ALTER TABLE <table> ENABLE ROW LEVEL SECURITY;
-- NB: NO `FORCE ROW LEVEL SECURITY` cho kh-sub
CREATE POLICY tenant_isolation ON <table>
  USING (instance_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid)
  WITH CHECK (instance_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid);
```

**Cluster B — `instance_id`-keyed FORCED (kc-core, 51 tables):**

```sql
ALTER TABLE <table> ENABLE ROW LEVEL SECURITY;
ALTER TABLE <table> FORCE ROW LEVEL SECURITY;  -- table owner cũng bị filter
CREATE POLICY tenant_isolation ON <table>
  USING (instance_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid)
  WITH CHECK (instance_id = NULLIF(current_setting('app.current_tenant_id', true), '')::uuid);
```

**Cluster C — `tenant_id`-keyed (kh-sub non-forced, 1 table `consent_record`):**

Identical pattern with `tenant_id` column reference.

**Cluster D — admin_audit_logs (immutable per V50 + V60):**

```sql
ALTER TABLE admin_audit_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE admin_audit_logs FORCE ROW LEVEL SECURITY;
CREATE POLICY admin_audit_select ON admin_audit_logs FOR SELECT USING (true);
CREATE POLICY admin_audit_insert ON admin_audit_logs FOR INSERT WITH CHECK (true);
CREATE POLICY admin_audit_no_update ON admin_audit_logs FOR UPDATE USING (false) WITH CHECK (false);
CREATE POLICY admin_audit_no_delete ON admin_audit_logs FOR DELETE USING (false);
```

Audit immutability (no UPDATE/DELETE) là PDPL Art 11 compliance — tampering prevention.

### 4.3 NULL force-fail pattern (Wave 85)

`NULLIF(current_setting('app.current_tenant_id', true), '')::uuid` — nếu GUC unset hoặc empty string, expression evaluates NULL → policy predicate evaluates NULL → row invisible (default-deny preserved). Đây là defense-in-depth Layer 4 chống lại bug code quên `SET LOCAL app.current_tenant_id` trong transaction.

Per [multi-tenant §Defense-in-depth](multi-tenant-architecture.md) — Layer 1 (gateway JWT) + Layer 2 (@PreAuthorize) + Layer 3 (TenantContext propagate) + Layer 4 (RLS NULL force-fail) + Layer 5 (FK column NOT NULL).

---

## Section 5 — DB Sizing Baseline (Phase 1 BETA → Phase 2 trajectory)

**Phase 1 BETA assumptions:**
- 5-10 beta tenants (per ROADMAP §🎯)
- ~50-100 students per tenant average
- ~5-10 classes per tenant
- ~2-3 month active history before Phase 2 cutover

### 5.1 Top-10 row count drivers

| Rank | Table | Rows per tenant | Total 10-tenant estimate | Growth rate |
|:---:|---|---:|---:|:---:|
| 1 | `attendance` | ~10k-100k | ~100k-1M | High (daily inserts) |
| 2 | `grades` | ~1k-10k | ~10k-100k | Medium (weekly) |
| 3 | `subject_grades` | ~500-5k | ~5k-50k | Medium |
| 4 | `submissions` | ~500-5k | ~5k-50k | Medium |
| 5 | `student_points` | ~1k-10k | ~10k-100k | High (event-driven) |
| 6 | `parent_read_audit_log` | ~500-5k | ~5k-50k | High (read event) |
| 7 | `email_logs` | ~1k-10k | ~10k-100k (kh-sub scope) | Medium |
| 8 | `outbox_events` | ~100-1k | ~1k-10k | Medium (retention 7-30d) |
| 9 | `audit_log` | ~1k-10k | ~10k-100k | High (immutable) |
| 10 | `idempotency_keys` | ~1k-10k | ~10k-100k (kh-sub scope) | High (TTL 24h) |

**Total Phase 1 BETA DB size estimate:** ~50-200 MB (50% growth/quarter trajectory)
**Phase 2 trajectory (50-200 tenants):** ~5-20 GB — bắt đầu cần read-replica + partition strategy cho top-3 hot tables.

### 5.2 Index hot-path coverage

Critical indexes shipped per migration history (V31, V46, V53, V54):
- `branding_jobs(instance_id, status)` — V31
- `audit columns BIGINT` — V46
- `beta_request abort cleanup` — V53
- `admin_audit_log composite (instance_id, occurred_at DESC)` — V54

Gap: `students(instance_id, id)` composite index — verify Wave 100+ (top FK target, hot path cho mọi feature query).

---

## Section 6 — Postgres-Specific Type Inventory

Per [`postgres-specific-type-testcontainers.md`](../../.claude/rules/postgres-specific-type-testcontainers.md) — Postgres-specific types KHÔNG match H2 + Mockito tests; cần Testcontainers IT cho fidelity.

### 6.1 Type inventory

| Type | Usage count | Tables / Columns | Testcontainers IT covered? |
|---|:---:|---|:---:|
| `jsonb` | 15 | `outbox_events.payload_json`, `audit_log.before_state` + `after_state`, `branding.metadata`, `moderation_queue.payload`, `submissions.snapshot_json`, `moderation_queue.flagged_keywords`, `notification_preferences.notification_preferences`, `quality_reports.issues`, `class_schedule_slots.recurrence_rule`, `students.parental_consent`, etc. | ⚠️ Partial (6 IT) |
| `uuid` | ~100+ | Mọi `instance_id`, `tenant_id`, primary keys | ✅ Built-in |
| `inet` | 0 (migrated away V42+V52) | (none active) | N/A — migrated → VARCHAR(45) |
| `bytea` | TBD | Recovery codes likely | TBD |
| `tsvector` | 0 | (none) | N/A |
| `citext` | 0 | (none) | N/A |
| `hstore` | 0 | (none) | N/A |
| `interval` | TBD | Subscription billing periods possibly | TBD |

### 6.2 Testcontainers IT coverage

**Total Testcontainers @Testcontainers IT classes:** 6 (4 kh-sub + 2 kc-core — verify)

**Coverage gap analysis:**
- 15 JSONB columns spread across ~10 entities
- 6 IT cover ~3-4 JSONB hot paths (admin_audit_log, branding, outbox)
- **Estimated coverage:** ~30-40% (3-4 of ~10 JSONB-using entities)
- **Recommendation:** Add IT cho `moderation_queue`, `submissions.snapshot_json`, `students.parental_consent`, `class_schedule_slots.recurrence_rule` (high-value PDPL/business logic surfaces)

### 6.3 INET → VARCHAR migration lessons

V42 (kh-sub fingerprint) + V52 (kh-sub IP) đã migrate `INET` → `VARCHAR(45)` do Hibernate binding mismatch (SQLState 42804 cast varchar→inet). Pattern lesson:
- Postgres INET là semantically correct cho IP storage
- Hibernate native binding requires `@JdbcTypeCode(SqlTypes.INET)` (Hibernate 6.2+) hoặc custom converter
- Trade-off: VARCHAR(45) accept-friendly with Hibernate; loses Postgres INET indexing/containment query benefit
- Verdict: For Phase 1 BETA, VARCHAR(45) acceptable; Phase 2 reconsider khi cần CIDR-range query

---

## Section 7 — Auto-gen Follow-up (Wave 100+ scope)

Per outside-in Benchmark agent recommendation (wave plan §1 Brainstorm Q3) — FK graph hand-maintain rủi ro drift sau ~3-6 months. Backstage-pattern auto-gen tooling exists:

**Proposal: GAP-XXX — Auto-gen DB architecture map from Flyway parser**

Concept:
- Parse Flyway V*.sql via SQL AST library (vd `sqlparse` Python, JSqlParser Java)
- Extract: CREATE TABLE → table list; FOREIGN KEY → edge list; @Column columnDefinition → type inventory
- Emit Mermaid `erDiagram` block + tables for Sections 1, 2, 3, 6
- Pre-commit hook re-run khi `db/migration/V*.sql` change
- Output drift CI check: `documents/02-architecture/database-architecture-map.md` regenerated section matches generated baseline

**Defer to Wave 100+:** Phase 1 BETA scope không cần auto-gen; baseline hand-write này đủ cho 5-10 tenant scale. Re-eval khi (a) 3rd manual refresh in 90 days (drift cost > automation cost), (b) >5 services có own DB migrations, (c) Backstage adoption decision.

---

## Section 8 — Related Documents

- **Sister architecture docs:**
  - [`multi-tenant-architecture.md`](multi-tenant-architecture.md) — Tenant isolation defense-in-depth 5 layers (source of truth cho RLS narrative)
  - [`kitehub-architecture.md`](kitehub-architecture.md) — kitehub-subscription + service catalog context
  - [`kiteclass-architecture.md`](kiteclass-architecture.md) — kiteclass-core domain context

- **ADRs:**
  - [`ADR-001-k12-data-model.md`](adr/ADR-001-k12-data-model.md) — k12 entity design rationale
  - [`ADR-002-academic-year-structure.md`](adr/ADR-002-academic-year-structure.md)
  - [`ADR-003-role-hierarchy.md`](adr/ADR-003-role-hierarchy.md)
  - [`ADR-004-instance-lifecycle.md`](adr/ADR-004-instance-lifecycle.md)

- **Closed gaps:**
  - GAP-466 — RLS implementation Phase 1 (V58 + V34 ship) — `documents/04-quality/gaps/closed/`
  - GAP-432 — RLS boundary test coverage (Wave 91+)
  - GAP-600 — JSONB Testcontainers IT prod-equiv

- **Wave plans:**
  - [Wave 99B plan](../03-planning/waves/wave-2026-05-19-99b-architecture-docs-sweep-expansion.md) — original scope cho this doc
  - Wave 56 — V58 RLS Phase 1 ship
  - Wave 85 — NULL force-fail hardening
  - Wave 92 — admin_audit_log enrichment (V54)

- **Rules:**
  - [`postgres-specific-type-testcontainers.md`](../../.claude/rules/postgres-specific-type-testcontainers.md) — IT mandate
  - [`pre-mutation-state-check.md`](../../.claude/rules/pre-mutation-state-check.md) — schema migration discipline

---

## Section 9 — Log

- **2026-05-19** (v1.0.0): Initial database architecture map created per Wave 99B Bucket B3 (GAP-672). Consolidates entity catalog (91 tables) + FK graph (top 25 sample, full graph deferred Wave 100+ auto-gen) + migration history index (114 V-files) + tenant_id propagation map + sizing baseline + Postgres-specific type inventory (15 JSONB + 6 Testcontainers IT). Sister-doc của [`multi-tenant-architecture.md`](multi-tenant-architecture.md) — extends §3 với entity-level map. Per `outside-in-coverage-trigger.md` v1.1.0 §3 + `diagram-format-selection.md` Mermaid default mandate. Reviewer: @nguyenvankiet (Wave 99B B3 agent worktree isolation).
