# AGENTS.md — Universal Codex Handoff cho Wave 14

> **Đọc file này TRƯỚC khi làm bất kỳ task nào trên repo.** Đây là single source of truth cho Codex CLI khi thực thi Wave 14 (fix Wave 13 anomalies + DB CI hardening bundled).
>
> Codex CLI **KHÔNG** đọc `CLAUDE.md`, **KHÔNG** đọc `.claude/rules/**`, **KHÔNG** đọc `.claude/skills/**` — chỉ file `AGENTS.md` này tự load.
>
> Khi gặp ambiguity, ghi inline "Boundary call:" + reasoning + chọn 1 hướng — đừng hỏi đi hỏi lại.

---

## Section 1 — Project Overview

**Kite Platform** = 2 sản phẩm chia sẻ infrastructure:

| Sản phẩm | Mục đích | Components |
|---|---|---|
| **KiteHub** | SaaS control-plane quản lý lifecycle (trial, subscription, billing, domain, branding) | 6 service: `kitehub-admin` / `kitehub-branding` / `kitehub-email` / `kitehub-gateway` / `kitehub-platform` / `kitehub-subscription` + `kitehub-frontend` (Next.js) |
| **KiteClass** | Multi-tenant edu platform — mỗi tenant = 1 trường học | `kiteclass-core` (Spring Boot multi-tenant) + `kiteclass-frontend` (Next.js); gateway đã decommissioned per ADR-032 GAP-001 |

### Database — 2 instances cùng cluster `kite-postgres`

| DB | Mục đích | Tables | Cluster | RLS |
|---|---|---|---|---|
| `kitehub` | Control-plane | 33 tables | 4 cluster: auth (cluster 03) / subscription (cluster 04) / branding (cluster 08) / email-compliance (cluster 13) | Partial — drift Wave 13 audit |
| `kiteclass` | Multi-tenant nghiệp vụ edu | ~65 tables | 8 cluster: 01 academic-structure / 02 attendance-grading / 03 RBAC / 04 finance-invoice / 05 student-management / 06 marketing / 07 teacher / 08 system-admin | FORCED per V58/V59 — drift Wave 13 audit |

### Stack chính

- **Backend:** Java 21 + Spring Boot 3.x + Maven (`./mvnw`)
- **Frontend:** Next.js 15 (App Router) + pnpm workspace
- **Database:** PostgreSQL 16 + Flyway migrations (`**/db/migration/V*.sql`)
- **Infra:** Redis (cache/queue) + RabbitMQ (event bus) + MinIO (S3-compat object store)
- **Shared infrastructure prefix:** `kite-` (KHÔNG phải `kitehub-`). VD: `kite-postgres`, `kite-redis`, `kite-rabbitmq`, `kite-minio`, `kite-gateway` (kitehub gateway runs on shared gateway port).
- **Production:** AWS Singapore Free Tier (ap-southeast-1), 2 EC2 + RDS, Terraform IaC trong `infrastructure/terraform-aws/`.

### Current phase

**Release Lần 1 Phase 1 — P1+P2 Soft Launch** (chốt 2026-05-06). Phase 1 = Owner + Teacher MVP; Phase 2 = Parent + Student; Phase 3 = K-12 (cần legal counsel).

---

## Section 2 — Language Convention

> **Mixed language per `dev-readable-doc-language.md`:** narrative tiếng Việt, technical token English.

| Loại nội dung | Ngôn ngữ | Ví dụ |
|---|---|---|
| Narrative trong docs (PR body, gap files, runbook, audit findings) | **Tiếng Việt** | "Mục đích là sweep RLS coverage cho mọi bảng tenant-scoped" |
| Table column name, enum value, config key, HTTP code, path | **English** | `instance_id`, `PENDING`, `kite.tenant.context.enabled`, `HTTP 201`, `documents/04-quality/...` |
| Code (Java / TypeScript / SQL) + code comments | **English** | `// Filter by tenant before save` |
| Commit message | **English conventional commits** | `feat(wave-14-rls): V78 RLS sweep tenant-scoped tables` |
| Commit body trailers (`ADMIN_MERGE_OVERRIDE:`, `Co-Authored-By:`, etc.) | **English** | `ADMIN_MERGE_OVERRIDE: GAP-NNN — ...` |
| PR title | **English conventional** | `feat(wave-14-bucket-a): RLS sweep + RLS coverage CI` |

**Banned:**
- ❌ KHÔNG `Co-Authored-By:` trailer trong commit (CLAUDE.md mandate — Claude Code auto-injects this; bỏ đi).
- ❌ KHÔNG English narrative trong docs/PR body (vi phạm `dev-readable-doc-language.md`).
- ❌ KHÔNG Vietnamese trong table/column/enum/config identifier.

---

## Section 3 — Git Workflow Mandatory

### Branch + commit

| Item | Rule |
|---|---|
| Branch naming | `wave-14-bucket-{a,b,c,d,e}-{slug}` hoặc `fix/<short-topic>` |
| Direct commit vào `main` | ❌ **CẤM** (per CLAUDE.md Wave Branch Strategy) |
| PR base | `main` |
| Merge strategy | `--squash` |
| `--admin` flag | ❌ **CẤM** (per `admin-merge-discipline.md`) — bypass branch protection silent breakage |
| `--no-verify` flag | ❌ **CẤM** (per CLAUDE.md Git Safety Protocol) — bỏ qua hook = bỏ qua governance |
| `git push --force` to `main` | ❌ **CẤM** — force flags only OK trên feature branch |
| `Co-Authored-By:` | ❌ **CẤM** trong commit message (per CLAUDE.md §"Commit Message Rules") |

### PR shape mandatory

Mỗi PR cần:

1. **Title:** conventional commits — `type(scope): subject` ≤ 70 chars
2. **Body:** xem template Section 6
3. **CI:** wait green TRƯỚC khi merge — KHÔNG `--admin` để bypass
4. **Branch cleanup:** sau merge, `git push origin :branch-name` xóa remote branch

### Recovery khi pre-commit hook fail

- ❌ **KHÔNG** `--no-verify` để bypass
- ✅ Diagnose hook output → fix root cause → re-stage → commit lại (tạo commit MỚI, đừng `--amend` vì hook fail nghĩa là commit chưa xảy ra)

---

## Section 4 — 15 Critical Rules Digest

> Codex CLI KHÔNG đọc `.claude/rules/**`. Digest dưới đây tóm gọn essence của 15 rule quan trọng nhất cho Wave 14.

### 1. `admin-merge-discipline.md` — KHÔNG `gh pr merge --admin`

- **Trigger:** Khi định merge PR.
- **Action:** Wait CI green → plain `gh pr merge <N> --squash`. Nếu CI broken (rebase+force-push), wait CI re-run hoàn tất.
- **Banned:** `--admin` flag bypass branch protection. Sau force-push, `--admin` đặc biệt nguy hiểm vì merge trước khi CI verify code rebased.
- **Exception:** Chỉ khi local verify đầy đủ vừa chạy clean trên exact rebased HEAD + cite trong PR body, HOẶC override trailer `ADMIN_MERGE_OVERRIDE: <reason>` + `ADMIN_MERGE_FOLLOWUP: GAP-NNN`.

### 2. `docs-only-pr-auto-merge.md` — Auto-merge docs PR khi CI green

- **Trigger:** PR diff fully trong `documents/**`, `.claude/rules/**`, `.claude/skills/**`, `*.md` root, `.env.*.template`.
- **Action:** Sau khi tạo PR + CI green, auto-merge squash + branch cleanup KHÔNG hỏi user "check CI?" hay "merge?".
- **Banned:** Auto-merge khi diff chạm `.github/workflows/*.yml`, code (`**/*.java`, `**/*.ts`, `**/*.tsx`), migrations, infra (`infrastructure/**`), `pom.xml`, `package.json`. Các trường hợp này dùng default "check CI → manual confirm merge".
- **Hold mechanism:** User có thể block bằng label `[do-not-auto-merge]` HOẶC PR body trailer `DOCS_AUTO_MERGE_HOLD: <reason>`.

### 3. `gap-done-discipline.md` — DONE flip discipline

- **Trigger:** Khi flip `documents/04-quality/gaps/GAP-*.md` Status `OPEN`/`PARTIAL` → `🟢 DONE`.
- **Action:** (a) Mọi `- [ ]` checkbox trong Acceptance Criteria → `- [x]` cùng diff; (b) Log entry KHÔNG chứa banned phrase (`deferred`, `out of scope`, `manual run`, `infra block`); (c) Mỗi phần deferred → file follow-up gap; (d) Update `gap-status.csv` row.
- **Banned:** Flip DONE rồi "manual capture later" — phải PARTIAL + follow-up gap.

### 4. `audit-to-gap-pipeline.md` — Audit → Gap → Memory → Fix PR

- **Trigger:** Khi chạy audit (UI / Quality / Security / Performance / API Contract / Ops Readiness / Business Logic), hoặc draft wave plan, hoặc PR decision-doc (gap closure config-shaped, ADR, runbook đổi domain/email/brand/env-var/region).
- **Action:** Mọi audit issue PHẢI đi qua pipeline: Issue → Duplicate Check (`grep` existing gaps) → Gap File → optional Memory entry → Fix PR. Mỗi audit run = 1 row mới trong `audits-index.csv`. Mỗi gap mới = 1 row mới trong `gap-status.csv`.
- **Banned:** Fix trực tiếp từ audit report không qua gap. Tạo gap duplicate. Skip CSV row.
- **Step 0 (Wave 14 specific):** Trước khi file gap, query `gap-status.csv` để check candidate đã tồn tại chưa.

### 5. `output-review-mandate.md` — Mọi output có review standard

- **Trigger:** Mọi artifact sinh ra trong PR (code, docs, gaps, migrations, scripts, CI workflow, audits).
- **Action:** Cite review standard cho từng output type trong PR body. VD: code = two-stage-code-review; migration = migration review checklist; docs = living-docs sync.
- **Banned:** Merge artifact mà không có review standard documented.

### 6. `ci-queue-local-runner-threshold.md` — Local CI trước khi push

- **Trigger:** PR diff fully docs-equivalent, HOẶC CI queue >5 concurrent runs, HOẶC P0 hotfix, HOẶC parallel batch ≥3 PR same wave.
- **Action:** Chạy local CI parity scripts trên worktree state TRƯỚC khi push branch. Document evidence trong PR body section `## Local CI parity`.
- **Banned:** Skip local CI "vì CI sẽ catch" — Wave rst-cascade-1 chứng minh 3/4 PRs CI fail same issue mà local đã có thể catch.
- **Wave 14 mandatory local scripts (per bucket):**
  - Bucket A: `bash scripts/check-rls-coverage.sh`
  - Bucket B: `bash scripts/check-schema-drift.sh`
  - Bucket C: `bash scripts/check-audit-col-uniformity.sh`
  - Bucket D: `bash scripts/check-type-consistency.sh`
  - Bucket E: `bash scripts/check-migration-replay.sh` (hoặc invoke job từ `quality-db.yml` qua `act` nếu available)

### 7. `cross-flow-bug-class-sweep.md` — Sweep sister sites sau khi fix bug

- **Trigger:** Fix 1 bug trong 1 flow.
- **Action:** Grep sister flow cho same bug class signature TRƯỚC khi flip closed. Document evidence inline PR body section `## Cross-flow sweep evidence`.
- **Banned:** Single-site fix không sweep — silent recurrence khi sister flow hit same class. VD Wave 14 Bucket A: nếu sửa 1 bảng RLS, MUST grep `pg_policies` + `instance_id` columns toàn DB.
- **Format:** Table mỗi site `FIX / DEFER / EXEMPT` + 1-line rationale.

### 8. `dev-readable-doc-language.md` — VN narrative + EN identifier

- **Trigger:** Mọi dev-readable artifact (gap files, runbook, planning, audit, business docs, arch docs, end-user docs).
- **Action:** Narrative content tiếng Việt. Technical identifier (table/column/enum/HTTP/config key/path) giữ English.
- **Banned:** English narrative trong gap/PR body/runbook. Vietnamese trong identifier (vd KHÔNG đặt tên cột `ngay_tao`).
- **Code-switch:** OK trong cùng câu — "Thêm cột `instance_id` để filter theo tenant".

### 9. `no-vercel-references.md` — Vercel decommissioned

- **Trigger:** Mọi artifact mới (PR, docs, planning, code addition) từ 2026-05-17 trở đi.
- **Action:** KHÔNG đề xuất Vercel as architecture choice. KHÔNG add code path consuming Vercel SDK. KHÔNG reference `kitehub.vercel.app` URL trong end-user docs. KHÔNG document Vercel as production hosting.
- **Production hosting:** AWS EC2 self-host per Wave 82 pivot + Wave 88 decommission.
- **Banned:** Mention "Vercel" trong new artifact (Wave 14 docs/CI/migrations). Existing references grandfathered.

### 10. `session-currentdate-check.md` — currentDate discipline

- **Trigger:** Khi viết bất kỳ artifact có date field (rule frontmatter `Last-Reviewed`/`Created`, gap files, session logs, memory, audit reports, ADR, wave plan frontmatter, ROADMAP).
- **Action:** Đọc currentDate từ session context (system reminder "Today's date is YYYY-MM-DD") + dùng verbatim. **Today's date là `2026-06-03`** cho session này.
- **Banned:** Suy luận date từ filename, recent session log, existing frontmatter (forward-dated planning docs), `date` shell không verify TZ.

### 11. `discovery-to-gap-inline-filing.md` — Discovery → gap inline

- **Trigger:** Đang làm task non-audit (docs writing / refactor / debug / cleanup / migration / code-read / design review) mà tình cờ discover gap-worthy finding (drift / bug / anti-pattern / TODO / security risk / RLS hole / schema anomaly / dead code).
- **Action:** File gap inline trong cùng session — KHÔNG stash sang "follow-up sau". Thêm row vào `gap-status.csv` cùng PR.
- **Banned:** Discovery stuck trong narrative section docs (vd "Ghi chú anomalies" trong cluster docs) mà không file gap → silent decay.
- **Wave 14 specific:** Nếu trong khi viết migration phát hiện thêm anomaly chưa được audit Wave 13 list → file gap mới inline cùng PR Bucket tương ứng.

### 12. `feature-ship-runtime-walk-mandate.md` — RST walk trước DONE

- **Trigger:** Gap user-facing feature scope (persona-attributed AC / FE page + BE endpoint pair / multi-service workflow / state machine transition / side effect ngoài DB write / multi-tenant data flow).
- **Action:** Manual RST walkthrough end-to-end trên production-equivalent stack với persona-relevant credential TRƯỚC khi flip DONE. Walk evidence (HTTP status + DB row + side effect) paste vào gap closure block.
- **Banned:** Trust audit score → DONE flip. Curl-only verify cho user-facing flow. Skip walk "vì AC simple".
- **Wave 14 N/A:** Wave 14 = infra/CI/migration scope, không phải user-facing feature. Skip rule này (out-of-scope per rule §2 "Internal infra changes").

### 13. `pre-handoff-self-test-completeness.md` — Flow verify trước handoff

- **Trigger:** Khi flip gap DONE có AC liên quan flow user-facing (login, button, URL, dashboard, email link, file upload, payment redirect, tenant switch, real-time, background job).
- **Action:** Verify FLOW (entry point → auth gate → post-condition AC claims). Endpoint-level (curl 201) là cần nhưng KHÔNG đủ.
- **Banned:** "Curl trả 201, gap DONE" cho user-facing AC. Skip credential delivery to handoff.
- **Wave 14 partial:** Bucket E (migration replay CI) cần verify CI workflow chạy clean end-to-end trên test PR — đó là "flow" cấp CI infra.

### 14. `meta-csv-index-pattern.md` — CSV row cho mọi gap/ADR/audit

- **Trigger:** Tạo gap mới, ADR mới, audit report mới.
- **Action:** Thêm row mới vào canonical CSV cùng PR:
  - Gap → `documents/04-quality/gaps/gap-status.csv`
  - Audit → `documents/04-quality/audits/audits-index.csv`
  - Rule → `.claude/rules/rules-index.csv` (nếu Wave 14 thêm rule)
- **Schema:** Mỗi CSV có header định nghĩa column. Đọc header trước khi append để match.
- **Banned:** Tạo gap/audit file mà quên CSV row → drift.

### 15. `rule-change-process.md` — Rule semver + log + enforcement parity

- **Trigger:** Edit `.claude/rules/*.md` (Wave 14 có thể không cần edit rule nào — mostly infra).
- **Action:** Bump Version (semver), update Last-Reviewed, append `## Log` entry, paired same-PR enforcement (memory + reviewer-checklist + worked self-test).
- **Banned:** Edit rule mà không bump version. Ship rule advisory không enforcement.
- **Wave 14 likely N/A:** Bucket A-E chỉ là infra/CI/migration. Nếu phát sinh rule change inline (vd thêm `db-rls-coverage.md` rule), apply process.

---

## Section 5 — Wave 14 Scope

Wave 14 = **fix Wave 13 anomalies + DB CI hardening, bundled per bucket**. Mỗi bucket = 1 PR ship migration fix + CI script + workflow job paired same PR.

**Nguồn anomaly:** 12 cluster DB schema reference docs ở `documents/02-architecture/database/{kitehub,kiteclass}/0X-*.md` + audit `documents/04-quality/audits/2026-06-03-wave-13-kc-cluster-anomaly-coverage-audit.md`.

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

#### Acceptance Criteria

- [ ] New Flyway migration `V78__rls_sweep.sql` (KC) + tương đương KH (`Vxx__rls_sweep.sql` next-number trong kitehub schema) — `ENABLE ROW LEVEL SECURITY FORCE` + tenant policy cho mọi bảng tenant-scoped thiếu RLS
- [ ] Migration riêng backfill `instance_id`: `V79__payments_kh_add_instance_id.sql` (cho KH payments + branding_outbox) — trước khi RLS policy enable
- [ ] CI script `scripts/check-rls-coverage.sh`:
  - Spin Testcontainer postgres 16
  - Flyway apply V1→Vn cho cả KH và KC schema
  - Query `pg_policies` cho mọi schema/table
  - Check: mọi table có column `instance_id` PHẢI có matching policy `USING (instance_id = current_setting('app.current_tenant')::uuid)`
  - Exit non-zero nếu miss
- [ ] CI job `db-rls-coverage` trong `.github/workflows/quality-db.yml`
- [ ] Local pre-flight: `bash scripts/check-rls-coverage.sh` chạy được trên dev machine (yêu cầu Docker)
- [ ] Gap rows updated: flip RLS-related gap rows trong `gap-status.csv` OPEN → DONE
- [ ] Sweep evidence inline PR body: grep `pg_policies` toàn schema + table-by-table FIX/DEFER/EXEMPT verdict

**Boundary call:** Nếu KC dùng dynamic `do $$ for r in select ... loop` style như V73 sweep — ưu tiên pattern đó để future-proof. Nếu impossible vì policy-per-table cần custom column reference — viết explicit per-table block.

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

#### Acceptance Criteria

- [ ] Sync entity per service match migration final state. 2 hướng resolve drift, chọn per-case:
  - **(a) Entity is correct → migration backfill** — viết migration mới để DB match entity (preferred khi entity reflect business logic mới)
  - **(b) Migration is correct → entity fix** — sửa entity field/annotation match DB (preferred khi entity vô tình thêm field unused)
- [ ] New migration cho missing `Lead` + `ContactMessage` tables (B4) — đây là drift kiểu `@Table` ghost: entity tồn tại nhưng DB chưa có
- [ ] CI script `scripts/check-schema-drift.sh`:
  - Spin Testcontainer postgres 16
  - Flyway apply
  - Boot Spring Boot test profile với `spring.jpa.hibernate.ddl-auto=validate`
  - Capture boot fail (Hibernate `SchemaManagementException`) → exit non-zero
  - Loop qua 3 service: `kitehub-subscription` / `kitehub-branding` / `kiteclass-core`
- [ ] CI job `db-schema-drift` trong `quality-db.yml`
- [ ] Local pre-flight script chạy được
- [ ] Cross-flow sweep: grep mọi `@Entity` class repo-wide → list service nào chưa được verify → file gap inline nếu phát sinh thêm drift ngoài Wave 13 list

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

#### Acceptance Criteria

- [ ] New migration `V80__audit_uuid_sweep_v74.sql` (KC) — mở rộng V73 dynamic loop cover các cột actor V73 bỏ sót. Pattern:
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
- [ ] KH BaseEntity refactor `String` → `UUID` (C5) — riêng KH service `kitehub-platform` / `kitehub-subscription` / `kitehub-admin` / `kitehub-branding` / `kitehub-email` sử dụng BaseEntity. Migration tương ứng (Vxx_KH) để DB column type change.
- [ ] CI script `scripts/check-audit-col-uniformity.sh`:
  - Spin Testcontainer postgres + Flyway apply
  - Query `information_schema.columns` cho mọi cột match `*_by` pattern
  - Assert mọi cột PHẢI là `uuid` type
  - Exit non-zero nếu drift
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

#### Acceptance Criteria

- [ ] Type harmonize migration `V81__type_harmonize.sql` (KC) + tương đương KH:
  - Money columns: DECIMAL(19,2) chuẩn
  - Time columns: TIMESTAMPTZ chuẩn
  - Enum CHECK: UPPERCASE align entity values
  - Remove CHECK USD inert HOẶC keep nhưng document business decision
- [ ] CI script `scripts/check-type-consistency.sh`:
  - Query `information_schema.columns` post-migration
  - Assert money cols (heuristic: column name match `*amount*`, `*price*`, `*total*`, `*paid*`, `*due*`, `*tax*`, `*fee*`) là DECIMAL(19,2)
  - Assert time cols (`*_at`, `*_date`, `*_time`) là TIMESTAMPTZ
  - Exit non-zero nếu drift
- [ ] CI job `db-type-consistency` trong `quality-db.yml`
- [ ] **RISK NOTE inline PR body:** Type change có thể break running queries / application code đang cast type explicit. Bucket D RECOMMENDED ship SAU Bucket A+B+C để tránh cascade conflict. Document production deploy steps trong PR body — cụ thể: (1) freeze writes window 5 min, (2) run migration, (3) verify entity boot-validate pass, (4) unfreeze.
- [ ] Local pre-flight script chạy được
- [ ] Cross-flow sweep: grep code paths đang cast money/time explicit — verify post-migration không break

**Boundary call:** Nếu Bucket A+B+C chưa merge, Bucket D ship trước = risk cascade conflict (vd Bucket B đổi entity field type → conflict với D type change). Order ship: A → B → C → D → E. Nếu user/CI yêu cầu parallel, document risk trong PR body + label `[depends-on: bucket-a-b-c]`.

---

### Bucket E — Migration replay CI (standalone)

**Branch:** `wave-14-bucket-e-migration-replay`

#### Anomalies cover

| # | Vấn đề | Action |
|---|---|---|
| E1 | KC cluster 08: V17/V18/V19/V20 no-op stub | Verify replay clean; nếu stub gây Flyway warning, add comment migration |
| E2 | General | Ensure mọi migration replay-able from scratch (catch env-dependent migration sớm — VD migration ref runtime data) |

#### Acceptance Criteria

- [ ] New CI workflow `quality-db.yml` (nếu chưa exist từ Bucket A — nhiều khả năng Bucket A đã tạo, Bucket E chỉ thêm job)
- [ ] Job `db-migration-replay`:
  - Spin Testcontainer postgres 16
  - Run `cd kitehub && ./mvnw flyway:migrate -P flyway-baseline` (hoặc tương đương) từ V1 → V_latest cho KH schema
  - Run tương tự KC schema
  - Check exit 0 + count migrations applied == expected (parse `flyway:info` output)
- [ ] Trigger trên PR touch `**/db/migration/**` hoặc `**/V*.sql`
- [ ] Document trong PR body section `## Local CI parity` per `ci-queue-local-runner-threshold.md`:
  - Run `bash scripts/check-migration-replay.sh` local trước push
- [ ] Bucket E STANDALONE — ship parallel với A/B/C/D OK (chỉ verify replay, không thay đổi schema). Nhưng Boundary call: nếu A/B/C/D commit migration cùng wave → CI job replay PR Bucket E phải merge SAU vì replay sẽ test V1→V_latest_after_other_buckets.

---

## Section 6 — Workflow per Bucket

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
gh pr create --base main --title "feat(wave-14-bucket-{X}): <subject>" --body "$(cat <<'EOF'
## Mục đích

<1 đoạn VN narrative — vì sao bucket này, fix anomaly nào>

## Scope

- File migration: <list>
- File CI script: <list>
- File workflow: `.github/workflows/quality-db.yml` (job `<job-name>`)
- File entity sync (nếu có): <list>

## Acceptance Criteria

- [ ] <AC từ Section 5 cho bucket tương ứng>
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
EOF
)"

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
# - Append entry vào documents/03-planning/waves/wave-14-*.md Log
```

### Boundary call — bucket ordering

**Recommended order:** A → B → C → D → E (per RISK NOTE Section 5).

**Parallel-safe:**
- A + E parallel OK (E chỉ verify replay, không change schema)
- B + C parallel risk MEDIUM (B đổi entity, C đổi audit col — sister but separable)
- D phải SAU A+B+C

**Codex CLI autonomy:** Nếu CI queue empty + local pre-flight pass cho ≥2 bucket cùng lúc, OK ship parallel với label `[depends-on:]` documented.

---

## Section 7 — Banned Patterns Quick Reference

| ❌ Banned | ✅ Required | Rule reference |
|---|---|---|
| `gh pr merge --admin` | Wait CI green, plain `gh pr merge <N> --squash` | `admin-merge-discipline.md` |
| `git commit --no-verify` | Diagnose hook fail, fix root cause, re-stage, new commit | CLAUDE.md Git Safety Protocol |
| `git push --force` to `main` | Force flags only on feature branches | CLAUDE.md Git Safety Protocol |
| English narrative trong docs/PR body/gap | VN narrative + EN identifier | `dev-readable-doc-language.md` |
| Vietnamese trong table/column/enum/config identifier | English identifier (`instance_id`, `PENDING`, etc.) | `dev-readable-doc-language.md` |
| Ship feature DONE không RST walk | Walk user flow trước flip DONE (rule N/A cho Wave 14 infra) | `feature-ship-runtime-walk-mandate.md` |
| Skip local CI "vì CI sẽ catch" | Local pre-flight per bucket per Section 6 | `ci-queue-local-runner-threshold.md` |
| File audit/gap không CSV row | Thêm row vào `gap-status.csv` / `audits-index.csv` cùng PR | `meta-csv-index-pattern.md` |
| Discovery stuck trong narrative không file gap | File gap inline cùng session | `discovery-to-gap-inline-filing.md` |
| Mention "Vercel" trong new artifact | Vercel decommissioned per Wave 88 | `no-vercel-references.md` |
| `Co-Authored-By:` trong commit message | KHÔNG dùng (Claude Code auto-injects — bỏ đi) | CLAUDE.md §Commit Message Rules |
| Suy luận date từ filename/log | Đọc `# currentDate` từ session context (today `2026-06-03`) | `session-currentdate-check.md` |
| Flip DONE rồi "manual capture later" | PARTIAL + follow-up gap | `gap-done-discipline.md` |
| Edit `.claude/rules/*.md` không bump Version | Bump semver + update Last-Reviewed + append Log + ship enforcement same PR | `rule-change-process.md` |
| Single-site bug fix không sweep sister | Grep sister flow + inline FIX/DEFER/EXEMPT table | `cross-flow-bug-class-sweep.md` |
| Fix trực tiếp từ audit report không qua gap | Pipeline: Audit → Duplicate check → Gap → Memory → Fix PR | `audit-to-gap-pipeline.md` |
| Auto-merge PR diff chạm code/workflow/migration | Auto-merge ONLY docs-only scope | `docs-only-pr-auto-merge.md` |
| Direct commit `main` (bypass PR) | Tất cả qua PR + squash merge | CLAUDE.md Wave Branch Strategy |

---

## Section 8 — Reference Files Codex Should Read When Needed

Codex CLI nên Read các file dưới đây khi cần detail context cho task hiện tại:

### Wave 13 anomaly source

- **12 cluster DB schema reference docs:**
  - KiteHub (4 cluster): `documents/02-architecture/database/kitehub/03-auth-cluster.md`, `04-subscription-cluster.md`, `08-branding-cluster.md`, `13-email-compliance-cluster.md`
  - KiteClass (8 cluster): `documents/02-architecture/database/kiteclass/01-academic-structure-cluster.md` qua `08-system-admin-cluster.md`
- **Wave 13 audit:** `documents/04-quality/audits/2026-06-03-wave-13-kc-cluster-anomaly-coverage-audit.md`
- **Gap inventory Wave 13 backfill:** `documents/04-quality/gaps/phase-1-beta/GAP-*.md` (search by cluster keyword khi cần)

### Canonical CSV (thêm row khi tạo gap/audit/rule mới)

- `documents/04-quality/gaps/gap-status.csv` — header dòng 1, append row cuối khi tạo gap mới
- `documents/04-quality/audits/audits-index.csv` — append row khi run audit
- `.claude/rules/rules-index.csv` — append row nếu Wave 14 thêm rule mới (ít có khả năng)

### Existing CI workflow reference (pattern để follow khi viết `quality-db.yml`)

- `.github/workflows/quality-code.yml` — pattern cho job naming + matrix
- `.github/workflows/quality-docs.yml` — pattern cho docs-only PR fast path
- `.github/workflows/quality-rules-skills.yml` — pattern cho rule/skill validation
- `.github/workflows/quality-infra.yml` — pattern cho infra/terraform check

### Migration reference (pattern để follow khi viết V78/V79/V80/V81)

- KiteClass migrations: `kiteclass/kiteclass-core/src/main/resources/db/migration/V*.sql` — đặc biệt V58/V59 (RLS sweep pattern), V73 (audit-UUID sweep pattern)
- KiteHub migrations: `kitehub/<service>/src/main/resources/db/migration/V*.sql` per service

### Project doc index

- `documents/03-planning/roadmap/release-1-plan-2026.md` — Phase 1 BETA scope
- `documents/03-planning/waves/` — wave plan history reference
- `README.md` — repo root + folder layout

---

## Section 9 — Wave 14 Closure Checklist

Khi 5 bucket A-E đã ship + CI green + merge:

- [ ] Update `documents/03-planning/waves/wave-14-*.md` plan file (tạo mới nếu chưa có) + Log section append timeline
- [ ] Update `documents/04-quality/gaps/ROADMAP.md` `## 🎯 Current Status Snapshot` section — add entry "Wave 14 closed YYYY-MM-DD — DB CI hardening + Wave 13 anomaly sweep (5 bucket)"
- [ ] Append entry vào `.claude/skills/quality/wave-pack-planner/data/wave-history.jsonl` — JSONL format:
  ```json
  {"wave": "14", "date": "YYYY-MM-DD", "scope": "Wave 13 anomaly fix + DB CI hardening", "buckets": ["A-rls-sweep", "B-entity-sync", "C-audit-uuid", "D-type-harmonize", "E-migration-replay"], "prs": [<list>], "anomalies_resolved": <count>, "ci_jobs_added": 5}
  ```
- [ ] Re-run gap inventory: flip Wave 13 anomaly gap rows trong `gap-status.csv` OPEN → DONE per `gap-done-discipline.md` §2 (AC checked + no banned phrase + follow-up nếu PARTIAL)
- [ ] Move gap files DONE → `documents/04-quality/gaps/phase-1-beta/closed/` per `gap-folder-organization.md` v2.0.0 phase-only design (gap file path mirrors phase, `closed/` subdir = one-way archive)
- [ ] Create session-handoff doc: `documents/03-planning/session-handoffs/YYYY-MM-DD-wave-14-closure.md` — narrative VN, scope shipped, pickup state cho next session, link 5 PR
- [ ] **Nếu schema drift CI (Bucket B) surface new findings** khi run trên existing PRs → file gap mới inline per `discovery-to-gap-inline-filing.md`. KHÔNG stash "fix Wave 15".
- [ ] **Audit `audits-index.csv` row added:** Khi tổng kết Wave 14, optionally run mini Quality Audit refresh trên DB cluster docs — file row mới `AUDIT-YYYY-MM-DD-wave-14-closure-db-anomaly` với evidence "0 anomaly remaining" hoặc list residual nếu có defer.
- [ ] Verify production smoke: nếu deploy AWS-side cần (KH RDS có data, KC dev RDS empty), follow `pre-handoff-self-test-completeness.md` §3.4 admin-flow checklist nếu touch user-facing — Wave 14 mostly N/A cho production smoke vì pure schema/CI scope.

---

## Appendix — Quick Codex Self-Check Before Push

Trước khi `git push` cho mỗi bucket PR, mental checklist:

1. **Pwd:** Tôi đang ở worktree root (relative path Read/Write/Edit)?
2. **Branch:** Branch name match `wave-14-bucket-{X}-{slug}`?
3. **Local CI:** `bash scripts/check-<topic>.sh` đã chạy clean local?
4. **Java verify:** Nếu touch Java, `./mvnw verify -P strict-warnings` đã chạy clean?
5. **Commit message:** Conventional + VN narrative body + **NO `Co-Authored-By`**?
6. **CSV row:** Gap row (nếu file gap mới) đã thêm vào `gap-status.csv`?
7. **PR body template:** Section 6 template structure (Mục đích / Scope / AC / Local CI parity / Cross-flow sweep / Anomalies fixed / Risk)?
8. **Banned patterns:** Đã check Section 7 table?
9. **currentDate:** Date trong frontmatter / log entry = `2026-06-03` (hôm nay)?
10. **No-Vercel:** Không mention Vercel trong artifact mới?

Sau merge: chạy Section 9 closure checklist.

---

**File version:** v1.0.0 — created 2026-06-03 cho Wave 14 handoff.
**Maintainer:** Wave coordinator (Claude / Codex hybrid handoff).
**Re-read trigger:** Start of every Wave 14 bucket session.
