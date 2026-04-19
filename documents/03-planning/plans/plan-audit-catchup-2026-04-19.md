---
title: Audit Catch-up Plan (2026-04-19) — 5 audit baselines
status: active
created: 2026-04-19
updated: 2026-04-19
gaps: [GAP-104-candidate, GAP-105-candidate]
---

# Audit Catch-up Plan — 5 audits, 5 sessions

**Mục đích:** Part A của governance turnaround (C + B đã ship PR #362). Chạy 5 audit skills lần đầu hoặc refresh sau ~27 ngày drift để tạo baseline cho hook enforcement đã active.

**Lý do split nhiều session:** 5 audits × 30-60 phút mỗi cái = 3-5 giờ tổng. Session dài → context drift. Best practice: 1 audit/session (hoặc ≤2 nếu audit ngắn).

---

## Usage — Cách invoke session mới

### Phương án 1: Reference command (đơn giản nhất)

Paste prompt ngắn gọn này vào session mới:

```
Đọc documents/03-planning/plans/plan-audit-catchup-2026-04-19.md và
thực hiện Audit {N} (N = 1..5). Dừng khi PR được merge. Tuân thủ toàn
bộ constraints + out-of-scope trong plan.
```

### Phương án 2: Paste trực tiếp audit prompt

Copy nguyên Section 3.{N} (ví dụ Section 3.1 cho business-logic) xuống session mới — self-contained, không cần đọc file plan.

---

## 1. Context tổng quan (session mới đọc trước khi làm)

**Dự án:** KiteClass + KiteHub SaaS giáo dục, repo `2026-Kite-Class-Platform`.

**Trạng thái governance (2026-04-19):**
- PR #358 ship `meta-gap-priority.md` MASTER RULE (skills/rules/workflow ưu tiên cao nhất)
- PR #362 ship `post-wave-audit-mandate.md` MASTER RULE + hardened `audit-gate.py` (warn → block)
- Hook giờ block PR non-docs-only missing required audits, trừ khi có `AUDIT_OVERRIDE:` trong PR body
- ROADMAP: 48/103 gaps CLOSED, 6 GA blockers còn (GAP-047 → 046 → 016 → 011 → 014 → 005)

**Audit drift gần đây:**
| Audit | Latest | Age | Status |
|-------|--------|:---:|:------:|
| business-logic /100 | 2026-03-23 | 27 ngày | 🔴 STALE |
| ops-readiness /100 | Chưa từng | ∞ | 🔴 VIOLATION |
| performance /100 | Chưa từng | ∞ | 🔴 VIOLATION |
| ui-review /128 | 2026-04-11 | 8 ngày | 🔴 STALE |
| quality-audit /100 | 2026-04-14 | 5 ngày | 🟡 borderline |
| security /100 | 2026-04-17 | 2 ngày | ✅ fresh (skip) |
| api-contract /100 | 2026-04-17 | 2 ngày | ✅ fresh (skip) |

**Post-wave PRs chưa được audit (merged #332-361):**
- Wave 1 #332 (bulk import), Wave 2b #337 (parent portal), Wave 3b #341 (async pipeline), Wave 4b #343 (branding propagation)
- Features #338, #353 (GAP-100), #354 (GAP-098), #355 (GAP-099)

---

## 2. Global rules áp dụng mọi audit

Mỗi session PHẢI tuân thủ:

### 2.1 Workflow bắt buộc
1. Đọc skill file tương ứng
2. Đọc reference rules:
   - `.claude/rules/audit-to-gap-pipeline.md` (issue → gap flow)
   - `.claude/rules/post-wave-audit-mandate.md` (cadence rule)
   - `.claude/rules/meta-gap-priority.md` (priority ordering)
   - `.claude/rules/output-review-mandate.md` (master review rule)
3. Đọc scope-specific files (list riêng per audit ở §3)
4. Chạy audit theo skill
5. Output deliverables (list riêng per audit)
6. Create PR theo Git Workflow — branch `feature/audit-{type}-catchup`, never commit main
7. Merge PR khi clean + mergeable

### 2.2 Constraints chung
- **Tiếng Việt** cho mọi communication (CLAUDE.md §Communication Language)
- **Superpowers methodology** cho PR (CLAUDE.md §Superpowers)
- **KHÔNG Co-Authored-By** trong commit (CLAUDE.md §Commit Message Rules)
- **KHÔNG commit trực tiếp main** — luôn qua PR
- **Thorough > fast** — đây là BASELINE cho hook enforcement
- **AUDIT_OVERRIDE** trong PR body nếu cần bypass hook (hiếm khi cần vì audit report CHÍNH LÀ output của session này)

### 2.3 Out-of-scope chung (không làm trong audit session)
- Fix issues found — audit chỉ identify + tạo gap file
- Fix theo `audit-to-gap-pipeline.md` ở PR riêng sau
- Re-audit lần 2 — chỉ 1 baseline per audit, session kế tiếp làm audit khác

### 2.4 Gap creation rules (per `audit-to-gap-pipeline.md` §3)
- Check duplicate bằng grep trước khi tạo gap mới
- Naming: GAP-XXX-{short-descriptive-name}.md
- Frontmatter: Status, Priority, Domain, Found, Affects
- Body: Problem, Root Cause, Proposed Fix, Acceptance Criteria, Related

Gap số hiện tại tối đa: **GAP-103** (check `ls documents/04-quality/gaps/` trước khi assign số mới).

---

## 3. Audit prompts

### 3.1 Audit 1: business-logic /100

**Lý do ưu tiên:** 27 ngày stale, highest risk rule-code drift sau Waves 2-4.

**Skill:** `.claude/skills/quality/business-logic-audit/SKILL.md`

**Scope source — đọc trước audit:**
- `documents/01-business/**/rules.md` (toàn bộ domains)
- `documents/01-business/**/use-cases.md` (optional, cross-ref)
- Recent code changes qua git log:
  ```bash
  git log --since=2026-03-23 --oneline --name-only -- "*.java" "*.yml" "*.yaml" | head -200
  ```

**Focus areas (Waves 2-4 + post-wave):**
- Wave 2 data model (academic year, multi-subject, role hierarchy) — are rules in `01-business/academic-year/rules.md`, `multi-subject/rules.md`, `role/rules.md` matching code?
- Wave 3 AI branding (lifecycle, resource classification, wizard) — match với `ai-branding/rules.md`?
- Wave 4 security (moderation, DMCA, deletion) — rules ở `security/rules.md`?
- Post-wave: bulk import (GAP-051), parent portal (GAP-052a), async pipeline, branding propagation
- Config keys: application.yml ↔ rules.md references

**Deliverables:**
1. Report: `documents/04-quality/audits/business/business-logic-audit-2026-04-19.md`
   - Score /100 per rubric trong skill
   - Domains covered (list rules.md checked)
   - Top 10 violations với code ref + rule ref + severity
   - Rule-code drift ranking
   - Recommendation cho fix order
2. Gap files cho violations P0/P1:
   - Check duplicate với `grep -rl "keyword" documents/04-quality/gaps/` trước
   - Tối đa 5-7 gap files, không tạo gap cho P2/P3 minor drift
3. ROADMAP.md Progress Log entry:
   ```markdown
   ### Audit catch-up 2026-04-19 — business-logic baseline
   - Score: XX/100
   - New gaps: GAP-104 to GAP-108 (N total)
   - Report: documents/04-quality/audits/business/business-logic-audit-2026-04-19.md
   ```
4. PR: branch `feature/audit-business-logic-catchup`, title `audit(business): business-logic baseline 2026-04-19`

**Out-of-scope:** fix violations; full rewrite rules.md; create domains not yet documented.

---

### 3.2 Audit 2: ops-readiness /100

**Lý do ưu tiên:** CHƯA TỪNG chạy — violation per `output-review-mandate.md`. Hook giờ block infra PRs cần audit này.

**Skill:** `.claude/skills/quality/ops-readiness-audit/SKILL.md`

**Scope source:**
- `infrastructure/` (helm, k8s, terraform-aws, terraform-oracle)
- `docker-compose.*.yml`, `Dockerfile*`
- `kitehub/docker-compose.kitehub.yml` (canonical)
- `kitehub/scripts/*.sh`, `scripts/*.sh`
- `documents/05-guides/` (operational runbooks — mới ship PR #350, #352)
- `.github/workflows/*.yml` (CI/CD)
- Monitoring: check if Grafana/Prometheus/alerting configured

**Focus areas (baseline — chưa từng audit):**
- **Monitoring:** metrics endpoints, dashboards, alerts — exist? configured?
- **Logging:** structured? retention? PII scrubbing? (gap: output-review-mandate.md flagged logs as VIOLATION)
- **Health probes:** /actuator/health in all services? K8s liveness/readiness?
- **Deploy gates:** go/no-go checklist? rollback procedure per service? (GAP-087, 088 DONE but verify)
- **Incident response:** runbook exists? (GAP-086 DONE but verify in practice)
- **Secrets management:** env vars, vault, k8s secrets?
- **Capacity planning:** resource limits, autoscaling configured?

**Deliverables:**
1. Report: `documents/04-quality/audits/ops/ops-readiness-audit-2026-04-19.md`
   - Score /100 (expect 40-60 baseline — first audit of this category)
   - Sections per skill rubric
   - Critical gaps (missing monitoring, health probes, etc.)
2. Gap files — likely 8-12 new gaps cho baseline violations
3. Update `.claude/rules/output-review-mandate.md` Section 4 table: ops-readiness từ VIOLATION → BASELINE_CAPTURED
4. ROADMAP entry
5. PR: `feature/audit-ops-readiness-catchup`

**Out-of-scope:** implement monitoring; setup Grafana; write runbooks; fix ops gaps.

---

### 3.3 Audit 3: performance /100

**Lý do:** CHƯA TỪNG chạy — violation. Hook chưa trigger vì pattern performance khó detect auto, nhưng rule post-wave-audit-mandate yêu cầu.

**Skill:** `.claude/skills/quality/performance-audit/SKILL.md`

**Scope source:**
- Database: migrations V*.sql, entity JPA annotations (@Index, @Query), N+1 patterns
- API handlers: Controller → Service → Repository chain
- Frontend bundle: `kiteclass-frontend/next.config.js`, package.json deps, build stats
- Cache: Redis usage, @Cacheable annotations
- Async: RabbitMQ consumer patterns (Wave 3b shipped async pipeline)
- Queries: look for SELECT * without LIMIT, missing JOIN indexes

**Focus areas:**
- **Database performance:** migrations có index? query patterns có N+1? lazy vs eager loading?
- **API latency:** endpoints có async/sync mix? timeouts?
- **Frontend:** bundle size, code splitting, image optimization
- **Cache strategy:** hit rate? TTL? stampede protection (GAP-043 OPEN)?
- **Async workloads:** GAP-002 async pipeline (shipped) — throughput? queue depth?

**Deliverables:**
1. Report: `documents/04-quality/audits/performance/performance-audit-2026-04-19.md`
   - Score /100 (expect 50-70 baseline)
   - DB section, API section, FE section, Cache section
   - Top 10 performance risks
2. Gap files P0/P1 — likely 5-8 new gaps
3. Update `.claude/rules/output-review-mandate.md` Section 4: performance PLANNED → BASELINE_CAPTURED
4. ROADMAP entry
5. PR: `feature/audit-performance-catchup`

**Out-of-scope:** load testing; profiling real prod data; optimize queries.

**Gotcha:** nếu thư mục `documents/04-quality/audits/performance/` chưa tồn tại, tạo + add `.gitkeep`.

---

### 3.4 Audit 4: ui-review /128

**Lý do:** 8 ngày stale. Waves 3b/4b shipped frontend changes.

**Skill:** `.claude/skills/quality/ui-review/SKILL.md`

**Scope source:**
- `kiteclass-frontend/` (Next.js admin/marketing)
- `kitehub-frontend/` (nếu có, check)
- Previous audit: `documents/04-quality/audits/ui/ui-audit-issues-2026-04-11.md`
- Capture script: `kiteclass/kiteclass-frontend/scripts/capture-screenshots.ts`

**Focus areas (post-Wave 4b changes):**
- Branding propagation (GAP-021, 032, 037 Wave 4) — FE theo tenant theme?
- Bulk import UI (GAP-051 Wave 1)
- Parent portal signup/invitation flow (GAP-052a Wave 2b)
- Error pages, auth flows, branded routes
- Previous issues: GAP-076 (mock auth), GAP-077 (error overlay), GAP-078 (dark mode), GAP-079 (i18n) — verify fixed

**Pre-flight (MANDATORY per skill):**
- Node 20, ports 4700/4701 free
- Playwright installed
- Correct app targeted

**Deliverables:**
1. Report: `documents/04-quality/audits/ui/ui-review-2026-04-19.md`
   - Scores /128 per-screen (KHÔNG average — per memory `feedback_ui_audit_setup.md`)
   - Before/after nếu có fix
   - New issues identified
2. Screenshots committed? NO — gitignored per memory `feedback_screenshot_mock_data.md`
3. Gap files cho P0/P1 UI issues
4. ROADMAP entry
5. PR: `feature/audit-ui-review-catchup`

**Out-of-scope:** fix UI issues; rewrite CSS; redesign screens.

**Critical memory notes (đọc trước audit):**
- MSW mock data required (không để error states)
- Port 3000 có thể bị chiếm → verify
- Per-screen scoring, không average
- Score what you SEE (external auditor view), không self-score theo code

---

### 3.5 Audit 5: quality-audit /100 refresh

**Lý do:** 5 ngày borderline — proactive refresh after 4 audits ở trên.

**Skill:** `.claude/skills/quality-audit/SKILL.md`

**Scope source:**
- Previous audit: `documents/04-quality/audits/quality/quality-audit-2026-04-14.md` (95/100 A+)
- Incorporate findings từ 4 audits trước (Audit 1-4) — đã run, scores đã có
- Recent merges từ 2026-04-14 đến nay
- 10 categories trong skill: E2E Functionality, Security, Backend Tests, Frontend Tests, CI/CD, UI/UX, DevOps/Infra, Documentation, Code Quality, Project Management

**Deliverables:**
1. Report: `documents/04-quality/audits/quality/quality-audit-2026-04-19.md`
   - Score /100, grade (A+, A, B, C, D)
   - Per-category score vs 2026-04-14 baseline
   - Deltas explained
2. Cross-reference với Audits 1-4 findings
3. ROADMAP entry finalizing governance turnaround
4. PR: `feature/audit-quality-refresh`

**Out-of-scope:** gaps creation (already done in Audits 1-4); fix issues.

---

## 4. Execution strategy — thứ tự + parallel

### 4.1 Thứ tự đề xuất

1. **Audit 1 (business-logic)** — urgent (27 ngày stale, highest drift risk)
2. **Audit 2 (ops-readiness)** — baseline first-ever, unblocks hook infra-PR enforcement
3. **Audit 3 (performance)** — baseline first-ever
4. **Audit 4 (ui-review)** — stale, depends on capture tool working
5. **Audit 5 (quality-audit)** — last vì cần findings từ 1-4 để refresh

### 4.2 Parallel opportunities

- Audit 1, 2, 3 ĐỘC LẬP → có thể chạy parallel (3 sessions riêng HOẶC 3 agents với worktree isolation)
- Audit 4 độc lập với 1-3 → parallel OK
- Audit 5 DEPENDS vào 1-4 → chạy cuối

### 4.3 Multi-session vs multi-agent

**Multi-session (recommended cho thorough audits):**
- Mở 2-3 terminals, mỗi cái `claude` riêng
- Mỗi session tự dùng 1 context window đầy đủ
- User control mỗi session
- Rủi ro: git conflict nếu 2 session edit cùng file → dùng git worktree mỗi session riêng branch

**Multi-agent (recommended cho quick audits):**
- 1 parent session spawn Agent tool với `isolation: worktree`
- Parent tiếp tục làm việc khác trong khi agents chạy
- Results return về parent → parent synthesize
- Rủi ro: agent context < full session, audit có thể ít thorough hơn

**Cho audit catch-up này:** khuyến nghị **multi-session** cho Audit 1-3 (thorough cần thiết cho baseline), **multi-agent** OK cho Audit 4-5.

### 4.4 Setup multi-session an toàn

```bash
# Session 1 (terminal 1): business-logic
cd ~/projects/2026-Kite-Class-Platform
git worktree add ../kite-audit-business feature/audit-business-logic-catchup
cd ../kite-audit-business
claude  # → paste Audit 1 prompt

# Session 2 (terminal 2): ops-readiness
cd ~/projects/2026-Kite-Class-Platform
git worktree add ../kite-audit-ops feature/audit-ops-readiness-catchup
cd ../kite-audit-ops
claude  # → paste Audit 2 prompt

# Session 3 (terminal 3): performance
cd ~/projects/2026-Kite-Class-Platform
git worktree add ../kite-audit-perf feature/audit-performance-catchup
cd ../kite-audit-perf
claude  # → paste Audit 3 prompt

# Sau khi merge xong, cleanup worktrees:
git worktree remove ../kite-audit-business
git worktree list  # verify
```

Mỗi worktree là checkout độc lập của repo, cùng `.git/` chia sẻ → branches, hooks, config sync; working files tách biệt → không conflict.

---

## 5. Deliverables tổng (sau khi 5 audits hoàn tất)

- [ ] 5 audit report files trong `documents/04-quality/audits/{category}/`
- [ ] ~25-40 gap files mới (tổng từ 5 audits)
- [ ] ROADMAP.md Progress Log có 5 entries
- [ ] `.claude/rules/output-review-mandate.md` Section 4 table updated:
  - business-logic: stale → CURRENT
  - ops-readiness: VIOLATION → BASELINE_CAPTURED
  - performance: PLANNED → BASELINE_CAPTURED
- [ ] 5 PRs merged
- [ ] Consolidation section trong ROADMAP: "Governance turnaround 2026-04-19 COMPLETE"

---

## 6. After all 5 complete — consolidation step

Session cuối (hoặc extension của Audit 5) làm:

1. Sort gaps mới theo `meta-gap-priority.md`
2. Identify top 5 actionable trong 2 tuần tới
3. Add tới ROADMAP Current Status Snapshot (nếu có GA-blocker mới surface)
4. Mark turnaround DONE trong `.claude/rules/post-wave-audit-mandate.md` §10 Log
5. Create summary PR nếu cần cross-audit synthesis

---

## 7. Log

- **2026-04-19:** Plan tạo sau governance turnaround PR #362 ship rule + hook. Part A (5 audits) split thành 5 sessions/agents theo best practice multi-session workflow.
