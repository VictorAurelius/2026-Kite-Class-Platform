---
title: Wave local-doable-14 — Wave 13 anomaly fix + DB CI hardening (5 bucket)
status: planned
created: 2026-06-03
updated: 2026-06-03
tag_primary: local-doable
tags_secondary: [phase-1-beta, db-fix, db-ci, schema-drift, rls]
counter: 14
waves: [local-doable-14]
gaps: []
---

# Wave local-doable-14 — Wave 13 anomaly fix + DB CI hardening

**Goal:** Đóng các anomaly DB schema mà Wave 13 cluster docs (KH 4 + KC 8) đã liệt kê, đồng thời ship 5 CI script + workflow job ngăn drift tái xảy ra. Mỗi bucket = 1 PR mang migration fix + CI script + workflow job bundled cùng PR.

**Trigger:** Wave 13 ship 12 cluster DB schema reference docs (`documents/02-architecture/database/{kitehub,kiteclass}/0X-*.md`) + audit `documents/04-quality/audits/2026-06-03-wave-13-kc-cluster-anomaly-coverage-audit.md` surface ~50 anomalies. Per `discovery-to-gap-inline-filing.md` v1.0.0 + `audit-to-gap-pipeline.md` §3, anomaly → gap → fix PR. Bundle fix + CI gate cùng wave để tránh recurrence.

**Estimated wall-clock:** ~6-8h parallel-with-dependencies (5 buckets, Bucket A → B → C → D → E recommended sequence per Bucket D risk note; A+E parallel-safe).

---

## 1. Brainstorm

**Q1 (inside-out 3-source pull per `inside-out-completeness-trigger.md`):**
- **Source 1 ROADMAP:** Wave 13 cluster docs shipped; Wave 14 candidate "DB anomaly fix + CI hardening" surfaced trong session handoff.
- **Source 2 inside-out-queue.md:** "DB anomaly cleanup" + "Schema drift CI" có trong queue per user-flagged 2026-06-03.
- **Source 3 gap-status.csv non-DONE filter phase=phase-1-beta:** Wave 13 anomalies chưa file thành gap formal (per `discovery-to-gap-inline-filing.md` v1.0.0 §6 worked self-test — Wave 13 5 cluster docs shipped với ~50 anomalies inline narrative; gap inline filing rule apply prospectively từ this wave 14 forward).

**Q2 (alternatives rejected):**
- Ship CI script standalone (Bucket E only) trước fix migration — REJECTED: CI fails on existing anomalies = noise, không có baseline clean.
- File 50 gap individual rồi mới batch fix — REJECTED: per `discovery-to-gap-inline-filing.md` §5 override mechanism — Wave 13 audit surface ≥10 same-class anomalies → batch triage wave (this wave) acceptable. Anomalies catalogued audit doc.
- Fix migration không ship CI script — REJECTED: future PR sẽ re-introduce drift.

**Q3 (risks):**
- **R1 — Bucket D type harmonize cascade conflict:** D đổi money column DECIMAL + time column TIMESTAMPTZ + enum UPPERCASE có thể conflict với entity sync Bucket B. Mitigation: ship A → B → C → D → E sequence; D ship sau B verify entity stable.
- **R2 — Bucket A RLS sweep KH payments backfill instance_id:** existing data rows có thể đã tồn tại. Mitigation: V79 backfill từ subscription FK hoặc tenant context; document fallback strategy trong migration header.
- **R3 — Bucket C audit-UUID V74 sweep data backfill:** existing BIGINT actor IDs không cast-able trực tiếp sang UUID. Mitigation: lookup users table for UUID by BIGINT id; document trong migration header + PR body.
- **R4 — Bucket E replay CI trigger sau A/B/C/D merge:** replay test V1→V_latest_after_other_buckets. Mitigation: E PR merge LAST sau A/B/C/D ship; nếu parallel, label `[depends-on: A-B-C-D]`.
- **R5 — Disjointness:** A KC RLS sweep + B entity sync + C audit-UUID + D type harmonize + E replay CI = 5 disjoint migration files + 5 CI scripts. Verified disjoint at file level; sequential merge tránh schema conflict.

**Q4 (outside-in audit deferred):** Wave 14 scope = 100% infra/CI/migration, non-user-facing. Per `outside-in-coverage-trigger.md` §4 exception row "Wave 100% internal scope (ops, refactor, tech debt)" → SKIP outside-in audit. Document trong wave plan §1 per rule mandate. Note: "Outside-in audit skipped per `outside-in-coverage-trigger.md` §4 — Wave 14 internal infra/CI scope, không có user-facing change."

---

## 2. Task Breakdown

| Bucket | Gap(s) | Owner | Effort | Disjoint? |
|--------|--------|-------|--------|-----------|
| A | RLS sweep V78 KC + V79 backfill KH instance_id + RLS coverage CI | bg-agent Opus | ~90min | ✅ migrations RLS-specific + check-rls-coverage.sh |
| B | Entity sync (Payment/Invoice/BrandingJob/Lead/ContactMessage/ClassSession/Course/Classes/Teacher) + Live Schema Drift CI | bg-agent Opus | ~120min | ✅ entity files KC core + KH branding + check-schema-drift.sh |
| C | Audit-UUID V80 sweep KC + KH BaseEntity UUID refactor + Audit-col uniformity CI | bg-agent Opus | ~120min | ✅ migrations audit-col + BaseEntity + check-audit-col-uniformity.sh |
| D | Type harmonize V81 (money DECIMAL + time TIMESTAMPTZ + enum UPPERCASE) + Type consistency CI | bg-agent Opus | ~90min | ✅ migrations type-cast + check-type-consistency.sh |
| E | Migration replay CI standalone + workflow job db-migration-replay | bg-agent Opus | ~60min | ✅ check-migration-replay.sh + quality-db.yml job (workflow chia sẻ với A/B/C/D) |

**Disjoint check:** Migration file names không trùng (V78 + V79 + V80 + V81); CI script names không trùng; workflow file `.github/workflows/quality-db.yml` shared nhưng job names khác (`db-rls-coverage` + `db-schema-drift` + `db-audit-col-uniformity` + `db-type-consistency` + `db-migration-replay`).

**Cross-layer check per `contract-first-for-cross-layer.md` §2:** NO bucket touches FE. Wave 14 = pure infra/CI/migration. Skip Bucket 0 Foundation.

**Sequence:** Recommended A → B → C → D → E. A+E parallel-safe (E chỉ verify replay, không change schema). B+C parallel risk MEDIUM (entity vs audit-col separable). D phải SAU A+B+C để tránh type cascade conflict.

---

## 3. Scope

**Stake tier per `wave-pack-planner/SKILL.md` §Step 4.6:** MEDIUM-HIGH (infra hardening + production-impacting migrations) → **Opus 4.7** mandatory per `agent-model-opus-default.md`.
**Cross-layer?** NO → skip Bucket 0 Foundation.

**Workflow file mới:** `.github/workflows/quality-db.yml` chứa 5 job tương ứng 5 bucket. Trigger: `pull_request` paths `**/db/migration/V*.sql`, `**/entity/**/*.java`, `kitehub/*/src/main/resources/db/**`, `kiteclass/*/src/main/resources/db/**`.

---

### Bucket A — P0 RLS sweep + RLS coverage CI

**Branch:** `wave-14-bucket-a-rls-sweep`

#### Anomalies fix

| # | DB | Bảng | Vấn đề | Action |
|---|---|---|---|---|
| A1 | KC | `payment_records` (V69) | Thiếu RLS DB-level | ENABLE RLS FORCE + tenant policy |
| A2 | KC | `payment_idempotency_keys` (V61) | Thiếu RLS | ENABLE RLS FORCE + tenant policy |
| A3 | KH | `payments` (V3) | Thiếu `instance_id` → V34 RLS list skip | Backfill `instance_id` column + RLS policy |
| A4 | KH | `branding_outbox` | No `instance_id` | Backfill + RLS |
| A5 | KH | `branding_templates` | System-wide nhưng thiếu RLS | RLS với policy public-read |
| A6 | KC | `landing_pages` (V75) | Tạo sau V58/V59 RLS sweep | ENABLE RLS FORCE |
| A7 | KC | `idempotency_keys` (V66) | Tạo sau V58/V59 | ENABLE RLS FORCE |
| A8 | KC | `teacher_courses` | RLS skip | ENABLE RLS FORCE |
| A9 | KC | `attendance_period` (V50) + `vettings` (V52) | Cần verify RLS state | Inspect + apply nếu missing |
| A10 | KH | Email/admin cluster | 8/13 tables thiếu RLS (cluster 13 audit) | RLS sweep cho mỗi bảng tenant-scoped |

#### Files

- `kiteclass/kiteclass-core/src/main/resources/db/migration/V78__rls_sweep.sql` (NEW)
- `kitehub/<service>/src/main/resources/db/migration/Vxx__rls_sweep_kh.sql` (NEW)
- `kitehub/<service>/src/main/resources/db/migration/V79__payments_kh_add_instance_id.sql` (NEW — backfill trước RLS policy enable)
- `scripts/check-rls-coverage.sh` (NEW)
- `.github/workflows/quality-db.yml` job `db-rls-coverage` (NEW)

#### Acceptance Criteria

- [ ] V78 KC + tương đương KH — `ENABLE ROW LEVEL SECURITY FORCE` + tenant policy cho mọi bảng tenant-scoped thiếu RLS
- [ ] V79 backfill `instance_id` ship TRƯỚC khi RLS policy enable (KH payments + branding_outbox)
- [ ] `scripts/check-rls-coverage.sh`: Testcontainer postgres 16 + Flyway apply V1→Vn + query `pg_policies` mọi schema/table + assert mọi table có column `instance_id` PHẢI có matching policy `USING (instance_id = current_setting('app.current_tenant')::uuid)` + exit non-zero nếu miss
- [ ] CI job `db-rls-coverage` trong `.github/workflows/quality-db.yml`
- [ ] Local pre-flight: `bash scripts/check-rls-coverage.sh` chạy được trên dev machine (yêu cầu Docker)
- [ ] Sweep evidence inline PR body per `cross-flow-bug-class-sweep.md` §3: grep `pg_policies` toàn schema + table-by-table FIX/DEFER/EXEMPT verdict

**Boundary call:** Nếu KC dùng dynamic `do $$ for r in select ... loop` pattern (V73 style) — ưu tiên pattern đó để future-proof. Nếu impossible vì policy-per-table cần custom column reference — viết explicit per-table block + document trong migration header.

---

### Bucket B — P1 entity sync + Live Schema Drift CI

**Branch:** `wave-14-bucket-b-entity-sync`

#### Anomalies fix

| # | Service | Entity | Vấn đề |
|---|---|---|---|
| B1 | `kiteclass-core` | `Payment` | Drift 12+ cột so V3 migration |
| B2 | `kiteclass-core` | `Invoice` | Thiếu `deleted` + `enrollment_id` mà BaseEntity yêu cầu |
| B3 | `kitehub-branding` | `BrandingJob` | Drift 7 cột so V4 |
| B4 | `kiteclass-core` | `Lead` + `ContactMessage` (marketing) | `@Table` declare nhưng KHÔNG có migration |
| B5 | `kiteclass-core` | `ClassSession` / `Course` / `Classes` | Drift entity vs migration |
| B6 | `kiteclass-core` | `Teacher` | Drift V1 + V27 cumulative |

#### Files

- Entity sync per service match migration final state (xem 2 hướng resolve drift dưới)
- New migration cho missing `Lead` + `ContactMessage` tables (B4)
- `scripts/check-schema-drift.sh` (NEW)
- `.github/workflows/quality-db.yml` job `db-schema-drift` (extend)

#### Acceptance Criteria

- [ ] Sync entity per service. 2 hướng resolve drift chọn per-case:
  - **(a) Entity is correct → migration backfill** — viết migration mới DB match entity (preferred khi entity reflect business logic mới)
  - **(b) Migration is correct → entity fix** — sửa entity field/annotation match DB (preferred khi entity vô tình thêm field unused)
- [ ] New migration cho `Lead` + `ContactMessage` tables (B4 ghost entity)
- [ ] `scripts/check-schema-drift.sh`: Testcontainer postgres 16 + Flyway apply + boot Spring Boot test profile với `spring.jpa.hibernate.ddl-auto=validate` + capture boot fail (`SchemaManagementException`) → exit non-zero + loop qua 3 service `kitehub-subscription` / `kitehub-branding` / `kiteclass-core`
- [ ] CI job `db-schema-drift` trong `quality-db.yml`
- [ ] Local pre-flight script chạy được
- [ ] Cross-flow sweep per `cross-flow-bug-class-sweep.md` §3: grep mọi `@Entity` class repo-wide → list service nào chưa được verify → file gap inline nếu phát sinh thêm drift ngoài Wave 13 list (per `discovery-to-gap-inline-filing.md`)

**Boundary call:** GAP-743 đã có "Entity ↔ Migration ↔ Mapper triad drift" rule (static AST check). Rule mới (dynamic boot-validate) là stronger — không deprecate static check, complement. Nếu PR conflict, cite GAP-743 + new check as 2 lớp defense.

---

### Bucket C — P1 audit-UUID V74 sweep + Audit-col uniformity CI

**Branch:** `wave-14-bucket-c-audit-uuid-sweep`

#### Anomalies fix

| # | DB | Cluster | Cột bỏ sót | Type expected | Type actual |
|---|---|---|---|---|---|
| C1 | KC | 04 (finance-invoice) | `received_by`, `payer_id`, `recorded_by`, `user_id` | UUID | mixed (V73 bỏ sót) |
| C2 | KC | 02 (attendance-grading) | `assigned_by` | UUID | BIGINT |
| C3 | KC | 08 (system-admin) | `rebrand_approvals` 3 cột actor | UUID | mixed |
| C4 | KC | 07 (teacher) | 6 cột actor | UUID | BIGINT |
| C5 | KH | BaseEntity | `createdBy` / `updatedBy` | UUID (match KC) | String — drift cross-DB |
| C6 | KH | 04 | 4 kiểu drift (UUID / BIGINT / VARCHAR(100)) cho actor columns | UUID | mixed |

#### Files

- `kiteclass/kiteclass-core/src/main/resources/db/migration/V80__audit_uuid_sweep_v74.sql` (NEW — dynamic loop)
- KH BaseEntity refactor `String` → `UUID` (services using BaseEntity: `kitehub-platform` / `kitehub-subscription` / `kitehub-admin` / `kitehub-branding` / `kitehub-email`)
- KH migration Vxx_KH cho DB column type change
- `scripts/check-audit-col-uniformity.sh` (NEW)
- `.github/workflows/quality-db.yml` job `db-audit-col-uniformity` (extend)

#### Acceptance Criteria

- [ ] V80 KC dynamic loop cover các cột actor V73 bỏ sót. Pattern:

  ```sql
  DO $$
  DECLARE
    r record;
  BEGIN
    FOR r IN
      SELECT table_schema, table_name, column_name
      FROM information_schema.columns
      WHERE column_name LIKE '%_by'
        AND data_type != 'uuid'
        AND table_schema = current_schema()
    LOOP
      EXECUTE format('ALTER TABLE %I.%I ALTER COLUMN %I TYPE uuid USING %I::uuid',
                     r.table_schema, r.table_name, r.column_name, r.column_name);
    END LOOP;
  END $$;
  ```

  **Boundary call:** Nếu existing data có BIGINT actor IDs không cast-able → migration cần data backfill strategy (vd lookup users table for UUID by BIGINT id). Document trong migration header comment + PR body.

- [ ] KH BaseEntity refactor `String` → `UUID` + migration tương ứng cho DB column type change
- [ ] `scripts/check-audit-col-uniformity.sh`: Testcontainer postgres + Flyway apply + query `information_schema.columns` cho mọi cột match `*_by` pattern + assert mọi cột PHẢI là `uuid` type + exit non-zero nếu drift
- [ ] CI job `db-audit-col-uniformity` trong `quality-db.yml`
- [ ] Local pre-flight script chạy được

---

### Bucket D — P2 type harmonize + Type consistency CI

**Branch:** `wave-14-bucket-d-type-harmonize`

#### Anomalies fix

| # | DB | Cluster | Vấn đề |
|---|---|---|---|
| D1 | KC | 04 (finance-invoice) | DECIMAL(12,2) vs NUMERIC(19,2) vs DECIMAL(15,2) trộn cho money columns |
| D2 | KH | 04 | BIGINT đồng (cent) vs DECIMAL — inconsistent money representation |
| D3 | Both | 04, 07, 08 | TIMESTAMP vs TIMESTAMPTZ trộn — timezone bug class |
| D4 | KC | 04 | Enum lowercase (`invoices.status = 'pending'`) vs UPPERCASE (entity `Status.PENDING`) drift |
| D5 | KC | 04 | CHECK constraint USD inert (`CHECK (currency IN ('VND', 'USD'))` nhưng business chỉ dùng VND) |

#### Files

- `kiteclass/kiteclass-core/src/main/resources/db/migration/V81__type_harmonize.sql` (NEW)
- KH tương đương Vxx_KH (NEW)
- `scripts/check-type-consistency.sh` (NEW)
- `.github/workflows/quality-db.yml` job `db-type-consistency` (extend)

#### Acceptance Criteria

- [ ] V81 type harmonize KC + tương đương KH:
  - Money columns: DECIMAL(19,2) chuẩn
  - Time columns: TIMESTAMPTZ chuẩn
  - Enum CHECK: UPPERCASE align entity values
  - Remove CHECK USD inert HOẶC keep nhưng document business decision
- [ ] `scripts/check-type-consistency.sh`: query `information_schema.columns` post-migration + assert money cols (heuristic: column name match `*amount*`, `*price*`, `*total*`, `*paid*`, `*due*`, `*tax*`, `*fee*`) là DECIMAL(19,2) + assert time cols (`*_at`, `*_date`, `*_time`) là TIMESTAMPTZ + exit non-zero nếu drift
- [ ] CI job `db-type-consistency` trong `quality-db.yml`
- [ ] **RISK NOTE inline PR body:** Type change có thể break running queries / application code đang cast type explicit. Bucket D RECOMMENDED ship SAU Bucket A+B+C để tránh cascade conflict. Document production deploy steps trong PR body — cụ thể: (1) freeze writes window 5 min, (2) run migration, (3) verify entity boot-validate pass, (4) unfreeze.
- [ ] Local pre-flight script chạy được
- [ ] Cross-flow sweep per `cross-flow-bug-class-sweep.md` §3: grep code paths đang cast money/time explicit — verify post-migration không break

**Boundary call:** Nếu Bucket A+B+C chưa merge, Bucket D ship trước = risk cascade conflict (vd Bucket B đổi entity field type → conflict với D type change). Order ship: A → B → C → D → E. Nếu user/CI yêu cầu parallel, document risk trong PR body + label `[depends-on: bucket-a-b-c]`.

---

### Bucket E — Migration replay CI (standalone)

**Branch:** `wave-14-bucket-e-migration-replay`

#### Anomalies cover

| # | Vấn đề | Action |
|---|---|---|
| E1 | KC cluster 08: V17/V18/V19/V20 no-op stub | Verify replay clean; nếu stub gây Flyway warning, add comment migration |
| E2 | General | Ensure mọi migration replay-able from scratch (catch env-dependent migration sớm — VD migration ref runtime data) |

#### Files

- `scripts/check-migration-replay.sh` (NEW)
- `.github/workflows/quality-db.yml` job `db-migration-replay` (extend hoặc create nếu Bucket A chưa tạo file)

#### Acceptance Criteria

- [ ] Workflow `quality-db.yml` job `db-migration-replay`:
  - Spin Testcontainer postgres 16
  - Run `cd kitehub && ./mvnw flyway:migrate -P flyway-baseline` (hoặc tương đương) từ V1 → V_latest cho KH schema
  - Run tương tự KC schema
  - Check exit 0 + count migrations applied == expected (parse `flyway:info` output)
- [ ] Trigger trên PR touch `**/db/migration/**` hoặc `**/V*.sql`
- [ ] Document trong PR body section `## Local CI parity` per `ci-queue-local-runner-threshold.md`:
  - Run `bash scripts/check-migration-replay.sh` local trước push
- [ ] Bucket E STANDALONE — ship parallel với A/B/C/D OK (chỉ verify replay, không thay đổi schema). Nhưng Boundary call: nếu A/B/C/D commit migration cùng wave → CI job replay PR Bucket E phải merge SAU vì replay sẽ test V1→V_latest_after_other_buckets.

---

## 4. Workflow per Bucket

### Standard sequence per bucket

```bash
# Bước 1: branch từ main
git checkout main
git pull --ff-only origin main
git checkout -b wave-14-bucket-{X}-{slug}

# Bước 2: implement
# - Migration file: kitehub/<service>/src/main/resources/db/migration/Vxx__<topic>.sql
#                   kiteclass/kiteclass-core/src/main/resources/db/migration/Vxx__<topic>.sql
# - CI script: scripts/check-{topic}.sh
# - Workflow job: thêm job vào .github/workflows/quality-db.yml

# Bước 3: local pre-flight
bash scripts/check-{topic}.sh
# Nếu touch Java entity:
cd kitehub/<service> && ./mvnw verify -P strict-warnings
# Nếu touch kiteclass-core:
cd kiteclass/kiteclass-core && ./mvnw verify -P strict-warnings

# Bước 4: commit
git add <files>
git commit -m "feat(wave-14-bucket-{X}): <short subject in English>

<VN narrative body in 1-2 đoạn, mô tả mục đích + scope>"
# NO --no-verify. NO Co-Authored-By.

# Bước 5: push
git push -u origin wave-14-bucket-{X}-{slug}

# Bước 6: tạo PR
gh pr create --base main --title "feat(wave-14-bucket-{X}): <subject>" --body "..."

# Bước 7: watch CI
gh pr checks <PR_NUMBER> --watch

# Bước 8: merge khi green
gh pr merge <PR_NUMBER> --squash
# KHÔNG --admin. Wait CI thực sự green.

# Bước 9: cleanup
git push origin :wave-14-bucket-{X}-{slug}
git checkout main
git pull --ff-only origin main

# Bước 10: post-merge sync
# - Update gap-status.csv: flip rows OPEN → DONE per gap-done-discipline.md
# - Append entry vào documents/03-planning/waves/wave-2026-06-03-14-anomaly-fix-db-ci-hardening.md Log
```

### PR body template per bucket

```markdown
## Mục đích

<1 đoạn VN narrative — vì sao bucket này, fix anomaly nào>

## Scope

- File migration: <list>
- File CI script: <list>
- File workflow: `.github/workflows/quality-db.yml` (job `<job-name>`)
- File entity sync (nếu có): <list>

## Acceptance Criteria

- [ ] <AC từ wave plan §3 cho bucket tương ứng>
- [ ] ...

## Local CI parity (per ci-queue-local-runner-threshold.md)

- [ ] `bash scripts/check-{topic}.sh` passed local
- [ ] `./mvnw verify -P strict-warnings` passed (nếu touch Java)

## Cross-flow sweep evidence (per cross-flow-bug-class-sweep.md, nếu Bucket A/B/C)

<grep evidence + FIX/DEFER/EXEMPT table>

## Anomalies fixed (per discovery-to-gap-inline-filing.md)

- GAP-NNN ... (link gap files Wave 13 backfill nếu đã tồn tại)
- Discovery inline (nếu phát sinh thêm anomaly khi viết migration): file gap mới + thêm row vào gap-status.csv cùng PR

## Risk + production deploy notes

<VD Bucket D type change — freeze window steps>
```

### Boundary call — bucket ordering

**Recommended order:** A → B → C → D → E (per RISK NOTE Section 3 Bucket D + Q3 R1).

**Parallel-safe:**
- A + E parallel OK (E chỉ verify replay, không change schema)
- B + C parallel risk MEDIUM (B đổi entity, C đổi audit col — sister but separable)
- D phải SAU A+B+C

**Codex CLI autonomy:** Nếu CI queue empty + local pre-flight pass cho ≥2 bucket cùng lúc, OK ship parallel với label `[depends-on:]` documented.

---

## 5. Closure Checklist

Khi 5 bucket A-E đã ship + CI green + merge:

- [ ] Update wave plan file frontmatter `status: planned → complete` + `completed_at: YYYY-MM-DD` + Log section append timeline
- [ ] Update `documents/04-quality/gaps/ROADMAP.md` `## 🎯 Current Status Snapshot` section — add entry "Wave 14 closed YYYY-MM-DD — DB CI hardening + Wave 13 anomaly sweep (5 bucket)"
- [ ] Append entry vào `.claude/skills/quality/wave-pack-planner/data/wave-history.jsonl` — JSONL format:

  ```json
  {"wave": "local-doable-14", "tag_primary": "local-doable", "tags_secondary": ["phase-1-beta", "db-fix", "db-ci"], "counter": 14, "date": "YYYY-MM-DD", "theme": "Wave 13 anomaly fix + DB CI hardening", "buckets": ["A-rls-sweep", "B-entity-sync", "C-audit-uuid", "D-type-harmonize", "E-migration-replay"], "prs": [<list>], "anomalies_resolved": <count>, "ci_jobs_added": 5}
  ```

- [ ] Per `wave-closure-scope-completeness.md` §3 Scope-Completeness Reconciliation table — categorize 5 bucket items + bất kỳ scope-pending item: ✅ DONE / 🟡 PARTIAL (gap link) / ❌ NOT-IMPLEMENTED (follow-up gap link OR rationale)
- [ ] Re-run gap inventory: flip Wave 13 anomaly gap rows trong `gap-status.csv` OPEN → DONE per `gap-done-discipline.md` §2 (AC checked + no banned phrase + follow-up nếu PARTIAL)
- [ ] Move gap files DONE → `documents/04-quality/gaps/phase-1-beta/closed/` per `gap-folder-organization.md` v2.0.0 phase-only design
- [ ] Create session-handoff doc: `documents/03-planning/session-handoffs/YYYY-MM-DD-wave-14-closure.md` — narrative VN, scope shipped, pickup state cho next session, link 5 PR
- [ ] **Nếu schema drift CI (Bucket B) surface new findings** khi run trên existing PRs → file gap mới inline per `discovery-to-gap-inline-filing.md`. KHÔNG stash "fix Wave 15".
- [ ] **Audit `audits-index.csv` row added:** Khi tổng kết Wave 14, optionally run mini Quality Audit refresh trên DB cluster docs — file row mới `AUDIT-YYYY-MM-DD-wave-14-closure-db-anomaly` với evidence "0 anomaly remaining" hoặc list residual nếu có defer.
- [ ] Verify production smoke: Wave 14 mostly N/A cho production smoke vì pure schema/CI scope (per `feature-ship-runtime-walk-mandate.md` §2 out-of-scope "Internal infra changes"). Nếu deploy AWS-side cần (KH RDS có data, KC dev RDS empty), follow `pre-handoff-self-test-completeness.md` §3.4 admin-flow checklist nếu touch user-facing.
- [ ] Run `bash scripts/prune-merged-worktrees.sh --yes` per `post-wave-cleanup.md` (after all bucket PRs merged, before drafting closure PR)

---

## 6. References

- **Source AGENTS.md (slimmed v1.2.0):** `AGENTS.md` — stable governance digest cho Codex CLI handoff
- **Source Wave 13 anomaly:**
  - KiteHub cluster docs (4): `documents/02-architecture/database/kitehub/03-auth-cluster.md`, `04-subscription-cluster.md`, `08-branding-cluster.md`, `13-email-compliance-cluster.md`
  - KiteClass cluster docs (8): `documents/02-architecture/database/kiteclass/01-academic-structure-cluster.md` qua `08-system-admin-cluster.md`
- **Wave 13 audit:** `documents/04-quality/audits/2026-06-03-wave-13-kc-cluster-anomaly-coverage-audit.md`
- **Canonical CSV (append row khi tạo gap/audit mới):**
  - `documents/04-quality/gaps/gap-status.csv`
  - `documents/04-quality/audits/audits-index.csv`
- **Existing CI workflow reference:** `.github/workflows/quality-code.yml` + `quality-docs.yml` + `quality-rules-skills.yml` + `quality-infra.yml`
- **Migration reference:**
  - KiteClass: `kiteclass/kiteclass-core/src/main/resources/db/migration/V*.sql` (đặc biệt V58/V59 RLS sweep pattern, V73 audit-UUID sweep pattern)
  - KiteHub: `kitehub/<service>/src/main/resources/db/migration/V*.sql` per service
- **Rules apply:**
  - `discovery-to-gap-inline-filing.md` v1.0.0 — anomaly inline filing
  - `audit-to-gap-pipeline.md` — Audit → Gap → Memory → Fix PR
  - `cross-flow-bug-class-sweep.md` v1.0.1 — sister site sweep evidence
  - `ci-queue-local-runner-threshold.md` v1.0.0 — local CI pre-flight
  - `admin-merge-discipline.md` v1.0.3 — NO `--admin` merge
  - `gap-done-discipline.md` — DONE flip discipline
  - `wave-closure-scope-completeness.md` v1.0.1 — closure reconciliation
  - `outside-in-coverage-trigger.md` v1.1.0 — §4 internal-scope exception applied
  - `agent-model-opus-default.md` v1.0.0 — Opus 4.7 spawn mandate
  - `wave-tag-numbering-convention.md` v1.0.0 — wave naming `wave-local-doable-14`

---

## 7. Log

- **2026-06-03** (planned): Plan created — extracted Wave 14 specific scope từ AGENTS.md v1.1.0 (which over-loaded stable + volatile content). Paired same PR với AGENTS.md slim v1.2.0 (stable-only) + cSpell allowlist additions. Outside-in audit skipped per `outside-in-coverage-trigger.md` §4 row "Wave 100% internal scope".
