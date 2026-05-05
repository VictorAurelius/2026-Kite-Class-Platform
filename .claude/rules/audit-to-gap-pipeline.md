# Audit → Gap → Fix Pipeline

**Priority:** 🟠 MANDATORY — audit findings governance
**Version:** 1.2.0
**Created:** 2026-04-16
**Last-Reviewed:** 2026-05-05
**Reviewer-Approver:** @nguyenvankiet (solo-dev — MINOR self-approve per `rule-change-process.md` §5; new §2.6 Wave-Plan Pre-Flight Protocol paired same-PR with `session-docs-check` Rule 16 detector + 3-fixture self-test + wave-plan template per §6.5 Enforcement Parity Mandate; closes GAP-356 5th-recurrence escalation)
**Applies to:** Every audit run (UI /128, Quality /100, Security /100, Performance /100, API Contract /100, Ops Readiness /100, Business Logic /100), every wave plan drafting, and the gap files / fix PRs they produce

---

## 1. The Rule

> **Mọi issue từ audit PHẢI đi qua pipeline: Issue → Gap Check → Gap File → Memory → Fix PR**
> Không fix trực tiếp từ audit report. Không tạo gap duplicate. Không fix không có thứ tự.

---

## 2. Pipeline Steps

### Step 1: Issue Discovery (trong audit)

Audit output issue list. Mỗi issue có:
- ID (ví dụ: H-1, K-3)
- Severity (P0/P1/P2/P3)
- Screen/location
- Description

### Step 2: Duplicate Check (BẮT BUỘC trước tạo gap)

```bash
# Search existing gaps cho keyword liên quan
grep -rl "dark.mode\|404\|i18n\|mock" documents/04-quality/gaps/ | head -10
```

3 outcomes:
- **Exact duplicate** → link issue tới gap hiện tại, KHÔNG tạo mới
- **Related but different scope** → tạo gap mới, ghi "Related: GAP-XXX" 
- **Completely new** → tạo gap mới

### Step 2.5: State-Check Against Current Codebase (BẮT BUỘC trước tạo gap)

Step 2 only guards against **duplicate GAP files**. It does NOT detect when a gap proposes work already shipped as code. A gap filed against already-existing implementation wastes reviewer time and gets rewritten later (see 2026-04-20 GAP-190 / GAP-197 incident).

**Run code-state check before Step 3** — grep the actual paths the gap would touch:

```bash
# Frontend gap → check app routes + components
find {service}/src/app -type d -name "{topic}*"
grep -rl "{keyword}" {service}/src --include="*.tsx" --include="*.ts"

# Backend gap → check controllers + services + migrations
grep -rl "{keyword}" {service}/src/main/java --include="*.java"
ls {service}/src/main/resources/db/migration/ | grep -i "{topic}"

# Infra/CI gap → check workflows + scripts + hooks
ls -la .husky/ .github/workflows/
grep -l "{tool}" .github/workflows/*.yml

# Docs/runbook gap → check existing docs
find documents/05-guides documents/01-business -iname "*{topic}*"
```

Expected outcomes + how to proceed:

| Code state | Gap status to file | AC framing |
|-----------|-------------------|-----------|
| Nothing exists | 🔵 OPEN | Build-from-scratch |
| Partial implementation | 🟡 PARTIAL | Must include `## Current State (verified YYYY-MM-DD)` table listing what exists + what's missing; AC narrows to the delta |
| Fully implemented | SKIP filing — the gap is already DONE | Close the underlying concern by updating docs / existing gap, not by filing a new one |

**If filing 🟡 PARTIAL**, the gap file MUST contain:
- A `## Current State (verified YYYY-MM-DD)` section with file paths + line counts (or symbol names) as evidence
- A Log entry: "Scope revised after state-check. Found: ..."

**Anti-pattern detected 2026-04-20:** GAP-190 (SEO) and GAP-197 (attendance calendar) were filed without state-check; both had substantial implementations already (sitemap/robots/OG/JsonLd/blog MDX; enhanced-attendance-calendar 315 LOC PR 3.8.1). Both required follow-up rewrite PRs. Rule added to prevent recurrence.

**Recurrence 2026-05-04 (4th time):** GAP-345 K-12 LEGAL trio state-check audit + Wave 18b1 Bucket D found Wave 2 inline-fetch FE skeleton at `(dashboard)/parent/page.tsx` (159 LOC) that GAP-345 missed. Root cause: `head` truncation on grep + insufficient `find` depth. Hardened rule below.

### Hardened state-check protocol (post-2026-05-04 incident)

State-check **MUST NOT use `| head`** on `grep -rl` / `find` commands. Truncation hides existing implementations.

| ❌ Banned pattern | ✅ Required pattern |
|------------------|---------------------|
| `grep -rl "X" path/ \| head` | `grep -rl "X" path/` (read full output) |
| `find path/ -name "X*" \| head -10` | `find path/ -name "X*"` then count + sample |
| Single grep on entry name | Multiple greps: file name + class name + JSX selector + i18n key |
| Skipping `documents/` searches | Include `documents/04-quality/gaps/` to find prior gap files |

**Mandatory cross-checks:**
- For frontend gaps: `find {service}/src/app -type d` (full tree) + `grep -rl "{keyword}" {service}/src --include="*.tsx" --include="*.ts"` (no head)
- For backend gaps: `ls {service}/src/main/java/com/.../module/` (full module tree) + `grep -rl "Class.*{Topic}\|interface {Topic}" path/`
- For data layer: `ls {service}/src/main/resources/db/migration/` (FULL list, look for related V-prefixes) + `grep -l "{table_name}\|create table {topic}" db/migration/`
- For docs: `find documents -iname "*{topic}*"` (no head)

**Self-test if uncertain:** if the gap claims "fully greenfield" or "missing entirely," the agent MUST list the exact grep + find commands run AND the OUTPUT counts (e.g., "0 files found" or "3 files found, sampled, none match scope"). Inline these in `## Current State` section.

**Tracked recurrences:**
- 2026-04-20: GAP-190 (SEO), GAP-197 (attendance calendar) → PR #396 rewrite
- 2026-05-04 (3rd): GAP-345 audit revising GAP-321/322/323 → PR #757
- 2026-05-04 (4th): GAP-345 itself missed Wave 2 FE parent skeleton → Wave 18b1 Bucket D agent caught + flagged in PR #766. Hardened protocol added this section.
- 2026-05-04 (5th): Wave 18b3 plan §3 Bucket C referenced 3 absent symbols (`Incident.visibilityScope`, `BR-CHILD-PROTECT-005`, `Notification` entity) — agent caught at execution time + filed 3 sub-gaps. **5th recurrence escalation triggered → GAP-356 filed → this v1.2.0 rule extension below adds wave-plan pre-flight state-check (§2.6).**

If 6th recurrence detected, escalate to meta-rule audit (this rule's enforcement is failing despite both gap-filing protocol §2.5 AND wave-plan protocol §2.6 below).

### Step 2.6: Wave-Plan Pre-Flight State-Check (BẮT BUỘC trước merge wave plan PR)

**Why this exists:** Wave plans are higher-leverage than individual gaps (3-15× gap leverage — 1 plan governs 3-5 buckets × multiple days each). When a plan references absent entities/rules/migrations, the cost cascades: agents read plan as ground truth, execute against absent schema, recover at execution time (best case Wave 18b3 — agents caught it; worst case stalls until reviewer notices). Per `feedback_wave_plan_through_pr.md` wave plans merge BEFORE agent spawn — so the plan PR is the LAST checkpoint to catch absent-symbol references.

**Trigger:** any new file added under `documents/03-planning/waves/wave-*.md`.

**Required content in the wave plan:** a `## State-Check Evidence` section demonstrating that every code-symbol-shaped reference in §3 Scope has been verified present in the codebase. Use the table format from `documents/03-planning/waves/_TEMPLATE.md`.

**Symbols requiring verification:**

| Symbol type | Pattern (in backticks) | Required grep |
|-------------|------------------------|--------------|
| Java class / entity field | `` `ClassName.fieldName` `` or `` `ClassName.METHOD` `` | `grep -rn "ClassName" {service}/src/main/java` (no `\| head`) |
| Business rule ID | `` `BR-DOMAIN-NNN` `` | `grep -rn "BR-DOMAIN-NNN" documents/01-business/` |
| Flyway migration | `` `V[0-9]+__name.sql` `` | `ls {service}/src/main/resources/db/migration/V[0-9]+__name.sql` |
| Frontend component | `` `<ComponentName>` `` or `` `useHookName` `` | `grep -rn "ComponentName\|useHookName" {service}/src --include="*.tsx" --include="*.ts"` |
| Config key | `` `kite.foo.bar` `` | `grep -rn "kite.foo.bar" {service}/src/main/resources` |

**Forward-looking references (allowed exception):** symbols intentionally absent because the wave WILL CREATE them are allowed IF the State-Check Evidence row marks Verdict as `🆕 to-be-created` AND the Bucket explicitly owns the creation. Symbols referenced as if existing but absent → FAIL.

**Banned shortcuts (mirroring §2.5):**
- `| head` truncation on `grep -rl` / `find` commands
- Skipping verification "because the agent will check at execution time" — the whole point is to catch absent symbols BEFORE agent spawn
- Aspirational references ("we'll filter by `Incident.visibilityScope`") without a 🆕 to-be-created flag

**If pre-flight fails:** revise plan §3 Scope to either (a) drop the absent symbol, (b) reframe the bucket scope, or (c) flag the symbol as 🆕 to-be-created with explicit creation owner. Plan PR does not merge until State-Check Evidence section shows ✅/🆕 verdict for every symbol.

**Reference template:** `documents/03-planning/waves/_TEMPLATE.md` §State-Check Evidence.

**Detector:** `session-docs-check` Rule 16 (`scripts/check-docs.sh`) — fires on new wave plan files in diff; FAIL when symbol-shaped references in `## Scope` / `### Bucket` sections lack a corresponding `## State-Check Evidence` row OR the row's grep evidence is absent.

### Step 3: Gap File Creation

Format chuẩn cho gap từ audit:

```markdown
# GAP-XXX: [Title]

**Status:** 🔵 OPEN
**Priority:** 🔴 P0 / 🟠 P1 / 🟡 P2 / 🟢 P3
**Domain:** [Frontend / Backend / DevOps / ...]
**Found:** [date] ([audit type] audit)
**Affects:** [scope — pages, services, users]

## Problem
[Mô tả issue từ audit, kèm evidence: scores, screenshots, file sizes]

## Root Cause
[Phân tích nguyên nhân, hoặc "Cần investigate"]

## Proposed Fix
[Steps cụ thể]

## Acceptance Criteria
- [ ] [Measurable criteria]

## Related
- [Link tới audit report]
- [Link tới gaps liên quan]
- [Link tới existing fix attempts]
```

### Step 4: Memory Update

Sau khi tạo gaps, save memories cho:

| Memory type | Khi nào | Ví dụ |
|-------------|---------|-------|
| **feedback** | Pattern lặp lại cần tránh | "Port 3000 bị chiếm → luôn verify trước capture" |
| **project** | Quyết định ảnh hưởng roadmap | "Dark mode KiteHub chưa implement, cần thêm vào wave" |

**KHÔNG save memory cho:**
- Issue details (đã có trong gap file)
- Fix steps (sẽ có trong PR)
- Scores (đã có trong audit report)

### Step 5: Update ROADMAP (BẮT BUỘC)

Sau khi tạo gap files, PHẢI update `documents/04-quality/gaps/ROADMAP.md`:

1. **Assign epic** — gap thuộc epic nào? Tạo epic mới nếu cần.
2. **Assign sprint** — gap nên fix trong sprint nào? Dựa trên priority + dependencies.
3. **Update counts** — tổng số gaps trong epic heading.
4. **Update dependency graph** — nếu gap mới block hoặc bị block bởi gap khác.

**KHÔNG được tạo gap mà không update ROADMAP.** Gap không có trong ROADMAP = gap bị quên.

### Step 6: Fix Priority & Ordering

**Meta-boost first:** trước khi áp dụng thứ tự dưới, apply `meta-gap-priority.md` — gaps về skills/rules/workflow luôn đi trước feature gaps cùng P-level. Xem `.claude/rules/meta-gap-priority.md` §3 cho priority matrix đầy đủ.

Sau khi meta-boost áp dụng, fix gaps theo thứ tự:

```
1. P0 blockers (chặn audit/deploy/CI) — meta gaps trước feature gaps
2. P0 → P1 có dependency chain (fix A trước mới fix B được)
3. P1 independent (fix song song) — meta gaps trước feature gaps
4. P2 batch (gom nhiều P2 vào 1 PR)
5. P3 opportunistic (fix khi đụng file liên quan)
```

**Dependency rules:**
- Capture-tool bugs fix TRƯỚC content bugs (vì cần re-capture sau fix)
- Mock data gaps fix TRƯỚC UI scoring gaps (vì scores phụ thuộc content)
- Infrastructure fix TRƯỚC feature fix

---

## 3. Anti-Patterns

| ❌ Don't | ✅ Do |
|---------|------|
| Fix issue trực tiếp trong audit session | Tạo gap file → fix trong PR riêng |
| Tạo gap mà không check duplicate | `grep` existing gaps trước |
| Fix P2 trước P0 | Respect priority + dependency order |
| Tạo 1 gap cho 5 issues khác nhau | 1 gap = 1 issue rõ ràng |
| Gom tất cả fixes vào 1 PR khổng lồ | Group by domain/priority, max 3-5 gaps per PR |
| Save mọi issue detail vào memory | Memory = patterns + decisions, gaps = details |

---

## 4. Mapping Audit Types → Gap Naming

| Audit | Gap prefix suggestion | Example |
|-------|----------------------|---------|
| UI Review /128 | GAP-XXX-{app}-{screen}-{issue} | GAP-076-kitehub-capture-mock-auth |
| Quality Audit /100 | GAP-XXX-{category}-{issue} | GAP-049-business-correctness |
| Security Audit /100 | GAP-XXX-{owasp/category}-{issue} | GAP-041-svg-xss-protection |
| Performance Audit /100 | GAP-XXX-{area}-{issue} | GAP-XXX-n-plus-one-queries |
| API Contract /100 | GAP-XXX-{service}-{issue} | GAP-XXX-endpoint-undocumented |
| Ops Readiness /100 | GAP-XXX-{area}-{issue} | GAP-XXX-missing-health-probes |

---

## 5. Integration

- **CLAUDE.md** references this rule
- **Audit skills** output issue list → trigger this pipeline
- **gap-to-pr-converter** skill consumes gap files → generates PR
- **wave-completion-check** verifies all gaps in wave are DONE

---

## 6. Log

- **2026-05-05** (v1.2.0): MINOR — added §2.6 Wave-Plan Pre-Flight State-Check Protocol extending state-check from gap-filing to wave-plan drafting. Triggered by 5th GAP-190/197 head-truncation recurrence (Wave 18b3 plan §3 Bucket C referenced 3 absent symbols `Incident.visibilityScope` + `BR-CHILD-PROTECT-005` + `Notification` entity; agent caught at execution time + filed 3 sub-gaps GAP-321b.1-trio). Per self-mandated 5th-recurrence escalation clause: file gap on rule itself (GAP-356 filed 2026-05-05) → ship rule extension. Paired same-PR per `rule-change-process.md` §6.5 with: `session-docs-check` Rule 16 detector + 3-fixture self-test (good-flip / bad-absent-symbol / forward-flag-allowed) + `documents/03-planning/waves/_TEMPLATE.md` State-Check Evidence section + `feedback_wave_plan_state_check.md` memory + cross-link updates. Recurrence list updated inline (5th entry). Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per §5 — adds new constraint covering previously-uncovered higher-leverage artifact, no constraint loosening for prior work).
- **2026-05-04** (v1.1.0): MINOR — added "Hardened state-check protocol" subsection to Step 2.5 banning `| head` truncation on grep/find during state-check. Triggered by 4th recurrence: GAP-345 K-12 LEGAL audit itself missed Wave 2 inline-fetch FE skeleton (159 LOC) at `(dashboard)/parent/page.tsx`; Wave 18b1 Bucket D agent caught at execution time + flagged in PR #766. Per `rule-change-process.md` §5 MINOR self-approve solo-dev — adds enforcement detail to existing rule, no constraint loosening. Recurrence list now tracked inline; 5th recurrence escalates to gap on this rule.
- **2026-04-28** (v1.0.0 backfill): Frontmatter backfill per GAP-249 — added Last-Reviewed + Reviewer-Approver + Applies-to fields; reformatted existing Version `1.0` → `1.0.0` (semver three-part canonical). No content change. Solo-dev PATCH self-approve per `rule-change-process.md` §5.
- 2026-04-20 — Added **Step 2.5 State-Check Against Current Codebase** after GAP-190 (SEO) + GAP-197 (attendance calendar) were filed without code-state verification; both required follow-up rewrite (PR #396). User feedback: "gaps phải dựa trên tình trạng của hệ thống hiện tại". Step 2.5 is BẮT BUỘC alongside Step 2 — dedupe alone is insufficient.
- 2026-04-16 — Rule created after UI audit session produced 5 gaps; user requested formalization
