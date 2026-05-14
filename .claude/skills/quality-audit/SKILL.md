---
name: quality-audit
description: "Dùng khi user nói 'audit', 'quality check', 'kiểm tra chất lượng', 'điểm chất lượng', 'ready to merge?', 'persona coverage', hoặc trước khi merge wave vào main. Chấm điểm 11 categories /110 điểm (10 tech + 1 persona coverage)."
user-invocable: true
---

# /quality-audit - Đánh giá chất lượng toàn diện

**Usage:** `/quality-audit [kitehub|kiteclass|all]`

**Default:** `all` (đánh giá cả KiteHub + KiteClass)

**Last Updated:** 2026-04-29 (v1.1 — added Cat 11 Persona Coverage; total scale 100 → 110)

---

## Instructions

Khi user invoke `/quality-audit`:

### Bước 1: Thu thập dữ liệu tự động

Chạy tất cả lệnh sau **song song** để thu thập metrics:

```bash
# 1. Git & PR stats
git log --oneline --since="30 days ago" | wc -l
gh pr list --state merged --limit 200 --json number --jq 'length'
gh pr list --state open --json number --jq 'length'
git branch -r | grep -v "main\|HEAD" | wc -l

# 2. CI status — PHẢI dùng script
#    Audit: dùng --status (đọc kết quả, không đợi)
#    Sau push: dùng không có flag (đợi hoàn thành)
scripts/check-ci.sh --status

# 3. Backend + Frontend tests — PHẢI dùng script
scripts/test-local.sh kiteclass all    # hoặc kitehub all
# KHÔNG chạy mvnw/vitest/eslint trực tiếp

# 4. E2E tests — dùng script có sẵn
kiteclass/scripts/test-api-e2e.sh

# 5. Docker status — dùng script có sẵn
kiteclass/scripts/dev-status.sh        # hoặc kitehub/scripts/status.sh

# 6. Code stats (grep/find OK vì chỉ đếm, không execute)
find kiteclass -name "*.java" -path "*/src/main/*" | wc -l
find kiteclass -name "*Test.java" | wc -l
find kiteclass/kiteclass-frontend/src -name "*.tsx" -o -name "*.ts" | wc -l

# 7. Security check (grep OK vì chỉ scan, không execute)
grep -r "TODO\|FIXME\|HACK\|XXX" kiteclass/*/src/main --include="*.java" | wc -l

# 8. Documentation
find documents -name "*.md" | wc -l

# 9. Monitoring
kiteclass/scripts/monitor.sh health    # hoặc kitehub/scripts/status.sh
```

**CRITICAL: KHÔNG chạy lệnh ad-hoc cho:**
- Tests → `scripts/test-local.sh`
- CI monitoring → `scripts/check-ci.sh`
- Docker → `*/scripts/dev-*.sh` hoặc `kitehub/scripts/*.sh`
- Monitoring → `*/scripts/monitor.sh`

### Bước 2: Chấm điểm 11 categories (110 điểm)

> **Per-check scoring (Wave 72b Bucket E):** Every category below uses per-check pass/fail rubric per `.claude/rules/audit-skill-rubric-quality-audit.md` §2 (5+ sub-checks per category, P0/P1 severity). Any P0 sub-check FAIL caps category total ≤ (max - 4) AND audit-level verdict = FAIL regardless of total score. The bug list (every FAIL) precedes the score table. See bound rule for primacy + concrete sub-check enumeration mirroring Wave 71c security-audit pattern.

> **Scale change 2026-04-29 (v1.1):** Cat 11 (Persona Coverage) added as 11th /10 category, raising max from 100 → 110. This preserves the relative weight of the original 10 tech categories (no rebalancing) while adding business-correctness signal that tech-only audits miss. Grade scale below adjusted proportionally. When citing a "score" in shorthand, use `X/110` form.

#### 1. E2E Functionality (10 điểm)

| Tiêu chí | Điểm | Check |
|----------|------|-------|
| E2E API tests pass 100% | 4 | `test-api-e2e.sh` results |
| E2E pass ngay lần đầu (no cold start issue) | 2 | Chạy 1 lần duy nhất |
| Critical flows hoạt động: Register→Login→Dashboard→Instance | 2 | Manual hoặc E2E |
| AI features hoạt động (không chỉ mock) | 2 | Check AI provider config |

#### 2. Security (10 điểm)

| Tiêu chí | Điểm | Check |
|----------|------|-------|
| Authentication: JWT + email verify + captcha | 3 | Code review |
| Rate limiting hoạt động | 2 | Gateway config |
| Không có secrets hardcode trong code | 2 | `grep -r` sensitive patterns |
| CORS configured đúng | 1 | Gateway CORS config |
| Input validation trên tất cả endpoints | 2 | DTO annotations |

#### 3. Backend Tests (10 điểm)

| Tiêu chí | Điểm | Check |
|----------|------|-------|
| Tất cả modules build + test pass (0 errors) | 4 | `mvnw test` |
| 0 skipped tests | 2 | Check skipped count |
| Test coverage >70% | 2 | Jacoco report nếu có |
| Integration tests cho critical paths | 2 | Check *IT.java files |

#### 4. Frontend Tests (10 điểm)

| Tiêu chí | Điểm | Check |
|----------|------|-------|
| KiteClass FE: vitest pass, <10% skipped | 3 | `vitest run` results |
| KiteHub FE: build pass (all pages) | 2 | `next build` |
| Component tests cho critical pages | 3 | Count test files |
| E2E browser tests (Playwright/Cypress) | 2 | Check e2e/ folder |

#### 5. CI/CD (10 điểm)

| Tiêu chí | Điểm | Check |
|----------|------|-------|
| Tất cả CI workflows green trên main | 4 | `gh run list` |
| 0 stale branches | 2 | `git branch -r` count |
| 0 open PRs không hoạt động | 2 | `gh pr list --state open` |
| CI history sạch (không spam failed runs) | 2 | `gh run list` count |

#### 6. UI/UX (10 điểm)

| Tiêu chí | Điểm | Check |
|----------|------|-------|
| Tất cả pages có consistent design system | 3 | Check gradient headers, shadow-soft |
| Theme system hoạt động (đổi màu visual) | 2 | Test ?primary=FF0000 URL |
| Responsive (mobile-friendly) | 2 | Check breakpoints |
| Onboarding/guidance cho new users | 2 | Wizard, checklist, tooltips |
| Accessibility (a11y basics) | 1 | aria-labels, semantic HTML |

#### 7. DevOps/Infrastructure (10 điểm)

| Tiêu chí | Điểm | Check |
|----------|------|-------|
| Tất cả Docker containers healthy | 3 | `docker compose ps` |
| Production deployment plan có (Terraform) | 2 | Check terraform/ folder |
| Backup strategy documented | 2 | Check docs |
| Monitoring/alerting | 2 | Check dashboard/alerts |
| Secrets management documented | 1 | SECRET-MANAGEMENT.md |

#### 8. Documentation (10 điểm)

> **Business docs at `documents/01-business/` are SOURCE OF TRUTH. Score 0 for this category if no business docs exist for implemented domains.**

| Tiêu chí | Điểm | Check |
|----------|------|-------|
| Business docs exist for all implemented domains | 3 | Check `documents/01-business/` has doc for each service with business logic |
| Business docs match code - config keys, rules | 2 | Cross-check config keys in code vs business doc Config section |
| Architecture + guides + README up-to-date | 3 | Check planning/ docs, vietnamese/ folder, README, CLAUDE.md |
| Plans up to date with completion tracking | 2 | Check ✅/⬜ in plans |

#### 9. Code Quality (10 điểm)

| Tiêu chí | Điểm | Check |
|----------|------|-------|
| 0 TODO/FIXME/HACK trong production code | 1 | `grep -r` |
| 0 IDE warnings (TypeScript + Java) | 1 | User reports |
| Consistent coding style (ESLint, Checkstyle) | 1 | Pre-commit hooks |
| No dead code / unused imports | 1 | Linter results |
| Spring Boot latest patch version | 1 | Check pom.xml |
| **Design patterns applied correctly** (no anti-patterns) | 3 | `.claude/rules/design-patterns.md` §3 |
| Pattern choice documented (javadoc) | 1 | Spot check key services |
| No God Services (>500 lines / >15 methods) | 1 | `find -size +20k *Service.java` |

**Design patterns check:**
- No God Service trong kitehub-branding, core services
- Status transitions via State Pattern (not switch)
- External APIs wrapped by Adapter
- Events via Outbox (check `outbox_events` table + publisher)
- External calls have Circuit Breaker + fallback
- No primitive obsession (value objects for structured types)

Reference: `documents/02-architecture/ai-branding-design-patterns.md` + `.claude/rules/design-patterns.md`

**Document Generation Quality check (Wave 5+ — when `kiteclass-core/module/document/**` changes):**
- Vietnamese diacritics (Đ, ễ, ă, ô, ơ, ư) round-trip through every format — verified by `PdfGeneratorTest.vietnamese_diacritics_round_trip_through_pdf_text_layer` + `DocxGeneratorTest.vietnamese_diacritics_preserved_in_contract_text` + sample-emitter outputs
- VND currency formatted with `vi-VN` locale (`.` thousand separator, no decimals) — `BR-DOC-PDF-003`, `BR-DOC-DOCX-006`
- Branding-aware outputs: PDF branded header, XLSX header-row fill, DOCX title run all derive from `branding.primaryColor` / `branding.logoUrl` / `branding.displayName` injected by `DocumentBrandingAssembler` (Sub-PR 5.5). Renderers MUST fall back gracefully when keys are absent (`BR-DOC-016`)
- WCAG contrast: when `branding.primaryColor` paints a fill (XLSX header), foreground text switches to white. PDF + DOCX use color on text and rely on tenant choosing readable colors — flag dark backgrounds for review.
- Filename in `Content-Disposition` uses RFC-5987 UTF-8 encoding so VN diacritics in filenames survive (`BR-DOC-014`)
- Cross-format consistency proven by `DocumentBrandingIntegrationTest` — when adding a 4th format (PPT/Wave 6), extend that test before merging.

#### 10. Project Management (10 điểm)

| Tiêu chí | Điểm | Check |
|----------|------|-------|
| Tất cả plans có completion status | 3 | Review plan docs |
| PRs follow Superpowers methodology | 3 | Check recent PR descriptions |
| Commit messages clean + meaningful | 2 | `git log` review |
| Issues/gaps tracked và prioritized | 2 | Check gap reports |

#### 11. Persona Coverage (10 điểm) — added 2026-04-29 (v1.1)

> **Source of truth:** latest reports under `documents/00-brd/persona-reviews/` produced via the `persona-based-business-review` skill quarterly cadence. **GAP-152** ships the first round of Tier 1 review reports — until those land, this category scores `5/10` (mid-baseline) marked `data pending` per the data-pending policy below.

> **Data-pending policy:** if no review report exists for ≥1 Tier 1 persona, score Cat 11 = `5/10` and annotate "data pending GAP-152". Do NOT score 0 (no data ≠ no coverage); do NOT score 10 (cannot claim full coverage without evidence). This 5/10 baseline holds steady until GAP-152 ships first reports.

| Tiêu chí | Điểm | Check |
|----------|------|-------|
| Tier 1 personas (Solo Teacher, Tutoring Center, Medium Center, K-12 School) all have a review report ≤90 days old | 3 | `ls -lt documents/00-brd/persona-reviews/*.md` + match each Tier 1 persona name |
| Each Tier 1 review's Coverage Analysis table has zero 🔴 critical (blocking-launch) gaps | 3 | Read each report's verdict + critical-gap section |
| Quarterly cadence respected: latest review ≤current quarter (per `persona-based-business-review.md` §Quarterly Review Cadence) | 2 | Compare report date vs current EOQ window |
| `documents/00-brd/personas-catalog.md` has fresh `next_review` field (not overdue) | 1 | grep `next_review:` frontmatter; compare to today |
| Audit-driven persona gaps (filed via `audit-to-gap-pipeline.md`) are tracked in ROADMAP, not stale | 1 | grep recent persona-* gaps in ROADMAP |

**Scoring guidance:**
- 9-10/10: All Tier 1 reports current, zero critical persona gaps, cadence on track
- 7-8/10: All reports exist but ≥1 has open critical gap, OR cadence slipping by ≤1 quarter
- 5/10: **Data pending** (GAP-152 not yet shipped) — default neutral score
- 3-4/10: Reports exist for some personas only, OR cadence overdue ≥2 quarters
- 1-2/10: Reports exist but multiple critical gaps, no remediation in flight
- 0/10: ALL personas reviewed and overall verdict shows feature completeness <30% for ≥1 Tier 1 persona (system actively unusable for that persona at scale)

**Reference:** `documents/00-brd/personas-catalog.md` (canonical Tier 1 list), `.claude/skills/quality/persona-based-business-review.md` (review methodology + cadence), `.claude/rules/meta-gap-priority.md` §3 (business-logic tier rationale).

### Bước 3: Output Report

```markdown
# Quality Audit Report: [KiteHub/KiteClass/All]

**Ngày:** [date]
**Người đánh giá:** Claude Code
**Version:** [latest commit hash]

---

## Overall Score

| # | Category | Score | Max | Grade |
|---|----------|-------|-----|-------|
| 1 | E2E Functionality | X | 10 | ✅/⚠️/❌ |
| 2 | Security | X | 10 | ✅/⚠️/❌ |
| 3 | Backend Tests | X | 10 | ✅/⚠️/❌ |
| 4 | Frontend Tests | X | 10 | ✅/⚠️/❌ |
| 5 | CI/CD | X | 10 | ✅/⚠️/❌ |
| 6 | UI/UX | X | 10 | ✅/⚠️/❌ |
| 7 | DevOps/Infra | X | 10 | ✅/⚠️/❌ |
| 8 | Documentation | X | 10 | ✅/⚠️/❌ |
| 9 | Code Quality | X | 10 | ✅/⚠️/❌ |
| 10 | Project Management | X | 10 | ✅/⚠️/❌ |
| 11 | Persona Coverage | X | 10 | ✅/⚠️/❌ |
| **Total** | | **X** | **110** | **Grade** |

### Grade Scale (rebased for /110 — proportional to the prior /100 thresholds)

- 105-110: A+ (Production Excellence)
- 99-104: A (Production Ready)
- 94-98: B+ (Near Production)
- 88-93: B (Good, needs polish)
- 77-87: C (Acceptable, significant gaps)
- <77: D (Major work needed)

**Conversion note:** the new thresholds are the prior /100 thresholds × 1.1 (rounded). E.g., A was 90/100 = 0.90; now A starts at 99/110 = 0.90. Comparing audits across the v1.0 → v1.1 boundary: convert old `X/100` to equivalent `(X × 1.1)/110` before delta — OR add a "data pending" 5 to the old score for direct comparison (assumes Cat 11 baseline neutral).

---

## Detailed Findings

### ✅ Strengths (8+/10)
[List categories scoring 8+ with evidence]

### ⚠️ Needs Improvement (5-7/10)
[List categories with specific gaps]

### ❌ Critical Issues (<5/10)
[List categories with blockers]

---

## Improvement Roadmap

### Quick Wins (1-2 hours each)
[Items that give most score per effort]

### Medium Effort (0.5-1 day)
[Important improvements]

### Major Effort (2+ days)
[Strategic improvements]

---

## Comparison with Previous Audit

| Category | Previous | Current | Change |
|----------|----------|---------|--------|
| ... | ... | ... | +X/-X |

(Nếu có audit trước, so sánh. Nếu không, ghi "First audit")

---

## Action Items

| Priority | Item | Estimated Score | Effort |
|----------|------|-----------------|--------|
| 🔴 P0 | ... | +X | ... |
| 🟠 P1 | ... | +X | ... |
| 🟡 P2 | ... | +X | ... |

---

## Next Audit Recommended

Đề xuất chạy `/quality-audit` lại sau khi hoàn thành Phase [X] của improvement plan.
```

### Bước 4: Lưu kết quả

- Save report to `documents/04-quality/audits/quality/quality-audit-[date].md`
- Update `kitehub-quality-100-plan.md` nếu phát hiện gaps mới
- So sánh với audit trước nếu có

---

## Context Management (CRITICAL)

Quality-audit là skill tốn context nhất (60-100K+ tokens). **Compaction giữa audit = scores sai.** Tuân thủ:

### Staged Execution — 3 Phases

**Phase 1: Automated metrics** (run in parent, ~15K tokens)
- Git stats, CI status, Docker status, code counts
- Test execution (via scripts — output có thể lớn, LUÔN `| tail -30`)
- KHÔNG đọc source files trong phase này

**Phase 2: Parallel deep-dive** (delegate to subagents, ~20K each)
- Agent 1: Categories 1-3 (E2E, Security, Backend Tests) — chạy tests + scan
- Agent 2: Categories 4-6 (Frontend Tests, CI/CD, UI/UX) — build + check design
- Agent 3: Categories 7-10 (DevOps, Docs, Code Quality, PM) — config + doc review
- Mỗi agent trả JSON summary: `{category, score, evidence: [1-line each], issues: []}`

**Phase 3: Aggregate** (parent, ~5K tokens)
- Collect subagent results → compile report → compare with previous audit

### Output Limiting Rules

| Command | Limit |
|---------|-------|
| `mvnw test` | `\| tail -30` (chỉ summary) |
| `vitest run` | `\| tail -20` |
| `npm run build` | `\| tail -25` (route table only) |
| `gh run list` | `--limit 10` |
| `git log` | `--oneline -20` |
| `grep -r` | `\| head -20` per pattern |
| `find ... \| wc -l` | OK (single number) |

### Subagent Return Format

```json
{
  "categories": [
    {"id": 1, "name": "E2E Functionality", "score": 8, "max": 10, "evidence": ["E2E pass 12/12", "cold start OK"], "issues": []}
  ]
}
```

Parent KHÔNG cần re-verify — trust subagent evidence. Chỉ sanity-check nếu score bất thường (0 hoặc 10).

## Rules

- LUÔN chạy tests thật (không đoán)
- LUÔN giao tiếp tiếng Việt
- Chấm điểm dựa trên evidence (test output, code check), không dựa trên cảm tính
- Nếu E2E fail lần 1 do cold start, chạy lần 2 nhưng GHI NHẬN cold start issue (-2 điểm)
- Nếu không thể chạy test (Docker down, etc.), ghi 0 điểm cho category đó + note lý do
- So sánh với quality plan nếu có (`kitehub-quality-100-plan.md`)
- **KHÔNG chạy tất cả trong 1 context** — PHẢI dùng staged execution + subagents

### CRITICAL: CI phải hoàn thành trước khi chấm điểm

**KHÔNG BAO GIỜ** kết luận CI/CD score hoặc Backend Tests score khi CI còn đang chạy (`in_progress`).

**Quy trình bắt buộc:**
1. Thu thập data → kiểm tra `gh run list` status
2. Nếu có run `in_progress` liên quan đến target (branch/PR):
   - **PHẢI** dùng `scripts/check-ci.sh --status` để kiểm tra — KHÔNG dùng lệnh CI trực tiếp
   - Nếu có runs `in_progress` → dùng `scripts/check-ci.sh` (wait mode) để đợi
   - **PHẢI** báo user: "CI đang chạy, đợi kết quả trước khi chấm điểm CI/CD"
   - **KHÔNG ĐƯỢC** giả định pass/fail
3. Chỉ sau khi CI completed → mới chấm điểm categories: CI/CD, Backend Tests, E2E
4. Nếu user yêu cầu audit gấp → ghi rõ "CI/CD: PENDING (chưa có kết quả)" thay vì đoán điểm

**Lý do:** Audit trước đã kết luận CI pass trong khi thực tế CI đang chạy. Điều này làm sai lệch kết quả đánh giá và có thể dẫn đến merge PR lỗi.

### Specialized Audits (for deeper analysis)

Quality-audit cho cái nhìn tổng quan /110. Để đánh giá sâu hơn từng domain, dùng:

| Audit | Skill | Khi nào |
|-------|-------|---------|
| Business Logic | `/business-logic-audit` | Code ↔ rules.md sync |
| Security | `/security-audit` | Deep security assessment |
| Performance | `/performance-audit` | Bottleneck + baseline |
| API Contract | `/api-contract-audit` | Endpoint ↔ docs sync |
| Ops Readiness | `/ops-readiness-audit` | Production deploy readiness |
| UI/UX | `/ui-review` | Per-screen visual /128 |
| Persona Coverage | `/persona-based-business-review` | Quarterly role-play per Tier 1 persona — feeds Cat 11 score |
| Document Generation | (sample inspection — no dedicated skill yet) | Open `documents/04-quality/samples/*` after Wave 5 Sub-PR 5.6; spot-check VN diacritics, VND format, branding application across PDF/XLSX/DOCX outputs. |

Thêm section "Specialized Audit Scores" vào report nếu có kết quả từ audit chuyên sâu.

---

## Gotchas

- **Self-audit overstates 15-20 pts vs specialist** — per memory `feedback_audit_calibration.md`, do not trust standalone score; always compute delta vs prior baseline, not absolute number
- **Audit grep scope must include -core submodules** — search `kiteclass-core/` + sub-modules explicitly; missing them produces false-positive issue counts (GAP-107 incident — see `feedback_audit_grep_scope.md`)
- **Targeted re-audit only after fix** — full re-audit only on release / major refactor; per `feedback_targeted_audit.md` re-score only categories affected by the fix
- **First-run baseline is NOT a regression** — when scoring a never-audited category (e.g., ops-readiness, performance pre-2026-04-19), low score is honest baseline, not drift
- **Always use `scripts/check-ci.sh --status`** (read mode) for audit data; `gh pr checks` field `state` returns null and breaks polling — see `feedback_monitor_gh_pr_checks_no_state_field.md`
- **Cat 11 (Persona Coverage) defaults to 5/10 until GAP-152 ships** — do NOT score 0 (no data ≠ no coverage) or 10 (cannot claim full coverage without evidence). Once GAP-152 lands first 4 Tier 1 reports, Cat 11 becomes data-driven per the rubric in §11
- **Comparing v1.0 (/100) vs v1.1 (/110) audits** — convert old `X/100` to `(X × 1.1)/110` before delta, OR add neutral `5` to old score's category sum. Don't compare raw integers across the scale change

---

## Log

- **2026-04-29** (v1.1): Added Cat 11 Persona Coverage /10. Total scale moved 100 → 110 (proportional rebasing — original 10 categories untouched). Grade scale rebased ×1.1. References GAP-152 as data source; defaults to 5/10 baseline ("data pending") until first reports ship. Closes GAP-050 framework AC #6 ("quality-audit /100 adds persona coverage category referencing GAP-152 output"). Reviewer: @nguyenvankiet (solo-dev — paired with `pre-flight-check.md` Layer 4 + `persona-based-business-review.md` §Quarterly Review Cadence in same PR per `rule-change-process.md` §6.5 Enforcement Parity Mandate).
- **2026-03 — 2026-04** (v1.0): Skill at 10 categories /100. Various calibration entries captured in memory (see `feedback_audit_calibration.md`).
