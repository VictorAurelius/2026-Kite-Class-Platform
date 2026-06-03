# AGENTS.md — Universal Codex Handoff (Stable Governance Only)

> **Đọc file này TRƯỚC khi làm bất kỳ task nào trên repo.** Đây là single source of truth cho Codex CLI — chứa stable governance digest (project / language / git workflow / rules / banned patterns / reference files).
>
> **Wave-specific scope nằm ở wave plan riêng** trong `documents/03-planning/waves/wave-<date>-<tag>-<counter>-*.md`. Xem Section 5 cho con trỏ wave hiện tại.
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
- ❌ KHÔNG `Co-Authored-By:` trailer trong commit (CLAUDE.md mandate — Claude Code auto-injects this; phải bỏ đi).
- ❌ KHÔNG English narrative trong docs/PR body (vi phạm `dev-readable-doc-language.md`).
- ❌ KHÔNG Vietnamese trong table/column/enum/config identifier.

---

## Section 3 — Git Workflow Mandatory

### Branch + commit

| Item | Rule |
|---|---|
| Branch naming | Wave bucket: `wave-<tag>-<counter>-bucket-{a,b,c,d,e}-{slug}`. Generic: `fix/<short-topic>` |
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

> Codex CLI KHÔNG đọc `.claude/rules/**`. Digest dưới đây tóm gọn essence của 15 rule quan trọng nhất apply mọi wave.

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
- **Step 0:** Trước khi file gap, query `gap-status.csv` để check candidate đã tồn tại chưa.

### 5. `output-review-mandate.md` — Mọi output có review standard

- **Trigger:** Mọi artifact sinh ra trong PR (code, docs, gaps, migrations, scripts, CI workflow, audits).
- **Action:** Cite review standard cho từng output type trong PR body. VD: code = two-stage-code-review; migration = migration review checklist; docs = living-docs sync.
- **Banned:** Merge artifact mà không có review standard documented.

### 6. `ci-queue-local-runner-threshold.md` — Local CI trước khi push

- **Trigger:** PR diff fully docs-equivalent, HOẶC CI queue >5 concurrent runs, HOẶC P0 hotfix, HOẶC parallel batch ≥3 PR same wave.
- **Action:** Chạy local CI parity scripts trên worktree state TRƯỚC khi push branch. Document evidence trong PR body section `## Local CI parity`.
- **Banned:** Skip local CI "vì CI sẽ catch" — Wave rst-cascade-1 chứng minh 3/4 PRs CI fail same issue mà local đã có thể catch.

### 7. `cross-flow-bug-class-sweep.md` — Sweep sister sites sau khi fix bug

- **Trigger:** Fix 1 bug trong 1 flow.
- **Action:** Grep sister flow cho same bug class signature TRƯỚC khi flip closed. Document evidence inline PR body section `## Cross-flow sweep evidence`.
- **Banned:** Single-site fix không sweep — silent recurrence khi sister flow hit same class.
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
- **Banned:** Mention "Vercel" trong new artifact. Existing references grandfathered.

### 10. `session-currentdate-check.md` — currentDate discipline

- **Trigger:** Khi viết bất kỳ artifact có date field (rule frontmatter `Last-Reviewed`/`Created`, gap files, session logs, memory, audit reports, ADR, wave plan frontmatter, ROADMAP).
- **Action:** Đọc currentDate từ session context (system reminder "Today's date is YYYY-MM-DD") + dùng verbatim.
- **Banned:** Suy luận date từ filename, recent session log, existing frontmatter (forward-dated planning docs), `date` shell không verify TZ.

### 11. `discovery-to-gap-inline-filing.md` — Discovery → gap inline

- **Trigger:** Đang làm task non-audit (docs writing / refactor / debug / cleanup / migration / code-read / design review) mà tình cờ discover gap-worthy finding (drift / bug / anti-pattern / TODO / security risk / RLS hole / schema anomaly / dead code).
- **Action:** File gap inline trong cùng session — KHÔNG stash sang "follow-up sau". Thêm row vào `gap-status.csv` cùng PR.
- **Banned:** Discovery stuck trong narrative section docs (vd "Ghi chú anomalies" trong cluster docs) mà không file gap → silent decay.

### 12. `feature-ship-runtime-walk-mandate.md` — RST walk trước DONE

- **Trigger:** Gap user-facing feature scope (persona-attributed AC / FE page + BE endpoint pair / multi-service workflow / state machine transition / side effect ngoài DB write / multi-tenant data flow).
- **Action:** Manual RST walkthrough end-to-end trên production-equivalent stack với persona-relevant credential TRƯỚC khi flip DONE. Walk evidence (HTTP status + DB row + side effect) paste vào gap closure block.
- **Banned:** Trust audit score → DONE flip. Curl-only verify cho user-facing flow. Skip walk "vì AC simple".
- **Out-of-scope:** Internal infra changes / docs-only / dev-tool / CI workflow.

### 13. `pre-handoff-self-test-completeness.md` — Flow verify trước handoff

- **Trigger:** Khi flip gap DONE có AC liên quan flow user-facing (login, button, URL, dashboard, email link, file upload, payment redirect, tenant switch, real-time, background job).
- **Action:** Verify FLOW (entry point → auth gate → post-condition AC claims). Endpoint-level (curl 201) là cần nhưng KHÔNG đủ.
- **Banned:** "Curl trả 201, gap DONE" cho user-facing AC. Skip credential delivery to handoff.

### 14. `meta-csv-index-pattern.md` — CSV row cho mọi gap/ADR/audit

- **Trigger:** Tạo gap mới, ADR mới, audit report mới.
- **Action:** Thêm row mới vào canonical CSV cùng PR:
  - Gap → `documents/04-quality/gaps/gap-status.csv`
  - Audit → `documents/04-quality/audits/audits-index.csv`
  - Rule → `.claude/rules/rules-index.csv` (nếu wave thêm rule)
- **Schema:** Mỗi CSV có header định nghĩa column. Đọc header trước khi append để match.
- **Banned:** Tạo gap/audit file mà quên CSV row → drift.

### 15. `rule-change-process.md` — Rule semver + log + enforcement parity

- **Trigger:** Edit `.claude/rules/*.md`.
- **Action:** Bump Version (semver), update Last-Reviewed, append `## Log` entry, paired same-PR enforcement (memory + reviewer-checklist + worked self-test).
- **Banned:** Edit rule mà không bump version. Ship rule advisory không enforcement.

---

## Section 5 — Current Wave Pointer

**Active wave:** xem `documents/03-planning/waves/wave-<date>-<tag>-<counter>-*.md` cho scope chi tiết (buckets + AC + risk + workflow per bucket).

Wave hiện tại (latest): **Wave local-doable-14 — Wave 13 anomaly fix + DB CI hardening**, plan file `documents/03-planning/waves/wave-2026-06-03-14-anomaly-fix-db-ci-hardening.md`.

**Quy tắc:** Mỗi wave mới có plan file riêng. AGENTS.md chỉ chứa stable governance — KHÔNG embed wave-specific scope/bucket detail. Khi user nói "wave plan" / "scope wave" / "bucket A của wave hiện tại" → Codex CLI Read plan file đó.

Conventional naming: `wave-<YYYY-MM-DD>-<tag_primary>-<counter>-<descriptor>.md` per `wave-tag-numbering-convention.md` v1.0.0 + `docs-filename-prefix-convention.md` Tier 3.

---

## Section 6 — Generic Workflow per Bucket

### Standard sequence per bucket

```bash
# Bước 1: branch từ main
git checkout main
git pull --ff-only origin main
git checkout -b <branch-name-from-wave-plan>

# Bước 2: implement per wave plan §3 Scope cho bucket tương ứng
# - Migration / code / docs files
# - CI script (nếu wave include CI hardening)
# - Workflow job (nếu wave include CI hardening)

# Bước 3: local pre-flight per ci-queue-local-runner-threshold.md
# Tùy bucket scope:
bash scripts/check-<topic>.sh   # nếu bucket include CI script
cd kitehub/<service> && ./mvnw verify -P strict-warnings   # Java
cd kiteclass/kiteclass-core && ./mvnw verify -P strict-warnings   # KC core
pnpm -F <pkg> test --run && pnpm -F <pkg> build && pnpm -F <pkg> lint   # FE

# Bước 4: commit
git add <files>
git commit -m "feat(<scope>): <short subject in English>

<VN narrative body in 1-2 đoạn, mô tả mục đích + scope>"
# NO --no-verify. NO Co-Authored-By.

# Bước 5: push
git push -u origin <branch-name>

# Bước 6: tạo PR
gh pr create --base main --title "feat(<scope>): <subject>" --body "$(cat <<'EOF'
## Mục đích

<1 đoạn VN narrative — vì sao bucket này, fix anomaly hoặc deliver scope nào>

## Scope

- File migration / code / docs: <list>
- File CI script / workflow (nếu có): <list>
- File entity sync (nếu có): <list>

## Acceptance Criteria

- [ ] <AC từ wave plan §3 cho bucket tương ứng>
- [ ] ...

## Local CI parity (per ci-queue-local-runner-threshold.md)

- [ ] `bash scripts/check-<topic>.sh` passed local (nếu có)
- [ ] `./mvnw verify -P strict-warnings` passed (nếu touch Java)
- [ ] `pnpm build` passed (nếu touch FE)

## Cross-flow sweep evidence (per cross-flow-bug-class-sweep.md, nếu bucket là bug-fix)

<grep evidence + FIX/DEFER/EXEMPT table>

## Anomalies fixed (per discovery-to-gap-inline-filing.md)

- GAP-NNN ... (link gap files đã file inline)
- Discovery inline: nếu phát sinh thêm anomaly khi implement → file gap mới + thêm row vào gap-status.csv cùng PR

## Risk + production deploy notes

<VD type change — freeze window steps>
EOF
)"

# Bước 7: watch CI
gh pr checks <PR_NUMBER> --watch

# Bước 8: merge khi green
gh pr merge <PR_NUMBER> --squash
# KHÔNG --admin. Wait CI thực sự green.

# Bước 9: cleanup
git push origin :<branch-name>
git checkout main
git pull --ff-only origin main

# Bước 10: post-merge sync
# - Update gap-status.csv: flip rows OPEN → DONE per gap-done-discipline.md
# - Append entry vào wave plan §7 Log
# - Nếu là bucket cuối cùng → run wave closure checklist trong wave plan §5
```

### Bucket ordering guidance

Xem wave plan §3 Scope (cột "Spawn order") + §2 Task Breakdown (cột "Disjoint?") cho thứ tự ship recommended + parallel-safe matrix per wave.

---

## Section 7 — Banned Patterns Quick Reference

| ❌ Banned | ✅ Required | Rule reference |
|---|---|---|
| `gh pr merge --admin` | Wait CI green, plain `gh pr merge <N> --squash` | `admin-merge-discipline.md` |
| `git commit --no-verify` | Diagnose hook fail, fix root cause, re-stage, new commit | CLAUDE.md Git Safety Protocol |
| `git push --force` to `main` | Force flags only on feature branches | CLAUDE.md Git Safety Protocol |
| English narrative trong docs/PR body/gap | VN narrative + EN identifier | `dev-readable-doc-language.md` |
| Vietnamese trong table/column/enum/config identifier | English identifier (`instance_id`, `PENDING`, etc.) | `dev-readable-doc-language.md` |
| Ship feature DONE không RST walk | Walk user flow trước flip DONE (N/A cho infra/CI wave) | `feature-ship-runtime-walk-mandate.md` |
| Skip local CI "vì CI sẽ catch" | Local pre-flight per bucket | `ci-queue-local-runner-threshold.md` |
| File audit/gap không CSV row | Thêm row vào `gap-status.csv` / `audits-index.csv` cùng PR | `meta-csv-index-pattern.md` |
| Discovery stuck trong narrative không file gap | File gap inline cùng session | `discovery-to-gap-inline-filing.md` |
| Mention "Vercel" trong new artifact | Vercel decommissioned per Wave 88 | `no-vercel-references.md` |
| `Co-Authored-By:` trong commit message | KHÔNG dùng (Claude Code auto-injects — bỏ đi) | CLAUDE.md §Commit Message Rules |
| Suy luận date từ filename/log | Đọc `# currentDate` từ session context | `session-currentdate-check.md` |
| Flip DONE rồi "manual capture later" | PARTIAL + follow-up gap | `gap-done-discipline.md` |
| Edit `.claude/rules/*.md` không bump Version | Bump semver + update Last-Reviewed + append Log + ship enforcement same PR | `rule-change-process.md` |
| Single-site bug fix không sweep sister | Grep sister flow + inline FIX/DEFER/EXEMPT table | `cross-flow-bug-class-sweep.md` |
| Fix trực tiếp từ audit report không qua gap | Pipeline: Audit → Duplicate check → Gap → Memory → Fix PR | `audit-to-gap-pipeline.md` |
| Auto-merge PR diff chạm code/workflow/migration | Auto-merge ONLY docs-only scope | `docs-only-pr-auto-merge.md` |
| Direct commit `main` (bypass PR) | Tất cả qua PR + squash merge | CLAUDE.md Wave Branch Strategy |

---

## Section 8 — Reference Files Codex Should Read When Needed

Codex CLI nên Read các file dưới đây khi cần detail context cho task hiện tại:

### Wave context (per wave)

- **Wave plan file:** `documents/03-planning/waves/wave-<date>-<tag>-<counter>-*.md` (xem Section 5 cho con trỏ wave hiện tại)
- **Wave audit (nếu có):** `documents/04-quality/audits/<date>-wave-<N>-*.md`
- **Gap inventory wave backfill:** `documents/04-quality/gaps/phase-1-beta/GAP-*.md` (search by keyword khi cần)

### Schema / cluster reference (cho DB-related work)

- KiteHub (4 cluster): `documents/02-architecture/database/kitehub/03-auth-cluster.md`, `04-subscription-cluster.md`, `08-branding-cluster.md`, `13-email-compliance-cluster.md`
- KiteClass (8 cluster): `documents/02-architecture/database/kiteclass/01-academic-structure-cluster.md` qua `08-system-admin-cluster.md`

### Canonical CSV (thêm row khi tạo gap/audit/rule mới)

- `documents/04-quality/gaps/gap-status.csv` — header dòng 1, append row cuối khi tạo gap mới
- `documents/04-quality/audits/audits-index.csv` — append row khi run audit
- `.claude/rules/rules-index.csv` — append row nếu wave thêm rule mới

### Existing CI workflow reference (pattern để follow khi viết workflow mới)

- `.github/workflows/quality-code.yml` — pattern cho job naming + matrix
- `.github/workflows/quality-docs.yml` — pattern cho docs-only PR fast path
- `.github/workflows/quality-rules-skills.yml` — pattern cho rule/skill validation
- `.github/workflows/quality-infra.yml` — pattern cho infra/terraform check

### Migration reference (pattern để follow khi viết migration mới)

- KiteClass migrations: `kiteclass/kiteclass-core/src/main/resources/db/migration/V*.sql` — đặc biệt V58/V59 (RLS sweep pattern), V73 (audit-UUID sweep pattern)
- KiteHub migrations: `kitehub/<service>/src/main/resources/db/migration/V*.sql` per service

### Project doc index

- `documents/03-planning/roadmap/release-1-plan-2026.md` — Phase 1 BETA scope
- `documents/03-planning/waves/` — wave plan history (each wave = 1 file)
- `documents/03-planning/inside-out-queue.md` — user-flagged inside-out items beyond ROADMAP
- `README.md` — repo root + folder layout

---

## Appendix — Quick Codex Self-Check Before Push

Trước khi `git push` cho mỗi bucket PR, mental checklist:

1. **Pwd:** Tôi đang ở worktree root (relative path Read/Write/Edit)?
2. **Branch:** Branch name match wave plan convention?
3. **Local CI:** `bash scripts/check-<topic>.sh` đã chạy clean local (nếu bucket include CI script)?
4. **Java verify:** Nếu touch Java, `./mvnw verify -P strict-warnings` đã chạy clean?
5. **FE build:** Nếu touch FE, `pnpm build` đã chạy clean (per `fe-build-local-verify.md`)?
6. **Commit message:** Conventional + VN narrative body + **NO `Co-Authored-By`**?
7. **CSV row:** Gap row (nếu file gap mới) đã thêm vào `gap-status.csv`?
8. **PR body template:** Section 6 template structure (Mục đích / Scope / AC / Local CI parity / Cross-flow sweep / Anomalies fixed / Risk)?
9. **Banned patterns:** Đã check Section 7 table?
10. **currentDate:** Date trong frontmatter / log entry = today (từ `# currentDate` session context)?
11. **No-Vercel:** Không mention Vercel trong artifact mới?

Sau merge: chạy wave closure checklist trong wave plan §5 (hoặc §7 nếu format khác).

---

## Appendix B — `.codex/` Mirror + Skill Bridge

Repo có symlink `.codex → .claude` (full mirror). Codex CLI tự đọc `AGENTS.md` này. Để truy cập deeper context khi cần:

| Khi user nói... | Codex action |
|---|---|
| "start session" / "session status" / "tình trạng" | Run `bash .codex/skills/workflow/start-session/scripts/collect-state.sh` + Read `.codex/skills/workflow/start-session/SKILL.md` |
| "audit" / "quality check" / "kiểm tra chất lượng" | Read `.codex/skills/quality-audit/SKILL.md` + follow 4-step process |
| "check pr" / "review pr N" | Read `.codex/skills/workflow/check-pr/SKILL.md` |
| "fix pr N" | Read `.codex/skills/workflow/fix-pr/SKILL.md` |
| "wave plan" / "tạo wave" | Read `.codex/skills/quality/wave-pack-planner/SKILL.md` |
| "repo status" / "repo health" | Run `bash scripts/repo-status.sh --json` |
| "security audit" | Read `.codex/skills/quality/security-audit/SKILL.md` |
| Cần rule cụ thể (vd "admin merge", "docs auto merge") | Read `.codex/rules/<rule-name>.md` |
| Cần CLAUDE.md full content | Read `CLAUDE.md` (root) — broad project context, KHÔNG auto-load cho Codex |
| Cần wave plan hiện tại detail | Read plan file ở Section 5 con trỏ (vd `documents/03-planning/waves/wave-2026-06-03-14-anomaly-fix-db-ci-hardening.md`) |

**Note về limitation:**
- `.codex/hooks/**/*.py` — Python hooks tied to Claude harness (PreToolUse/PostToolUse events) → KHÔNG chạy được với Codex CLI. Ignore.
- `.codex/settings.json` — Claude permission schema → KHÔNG dùng được. Codex có config riêng.
- `.codex/rules/**` có `paths:` YAML frontmatter — Claude auto-load via path-scope, Codex KHÔNG. Codex chỉ Read khi user invoke hoặc khi AGENTS.md instruct.
- Skill `description` field cho slash command — Claude-specific. Codex dùng AGENTS.md table trên thay thế.

**Single source of truth:** Mọi update vào `.claude/rules/` hoặc `.claude/skills/` đều auto-propagate sang `.codex/` qua symlink. KHÔNG duplicate maintenance.

---

**File version:** v1.2.0 — 2026-06-03 — slimmed to stable-only; Wave-specific content moved to wave plan files (`documents/03-planning/waves/wave-<date>-<tag>-<counter>-*.md`).
**Maintainer:** Wave coordinator (Claude / Codex hybrid handoff).
**Re-read trigger:** Start of every new session OR when reference rule/workflow conventions.
