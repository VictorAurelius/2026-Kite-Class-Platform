# Audit → Gap → Fix Pipeline

**Priority:** 🟠 MANDATORY — audit findings governance
**Version:** 1.0.0
**Created:** 2026-04-16
**Last-Reviewed:** 2026-04-29
**Reviewer-Approver:** @nguyenvankiet (starter-kit upstream maintainer)
**Applies to:** Every audit run (UI /128, Quality /100, Security /100, Performance /100, API Contract /100, Ops Readiness /100, Business Logic /100) and the gap files / fix PRs they produce

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

Step 2 only guards against **duplicate GAP files**. It does NOT detect when a gap proposes work already shipped as code. A gap filed against already-existing implementation wastes reviewer time and gets rewritten later.

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
| **project** | Quyết định ảnh hưởng roadmap | "Dark mode chưa implement, cần thêm vào wave" |

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
| UI Review /128 | GAP-XXX-{app}-{screen}-{issue} | <example: GAP-XXX-app-capture-mock-auth> |
| Quality Audit /100 | GAP-XXX-{category}-{issue} | <example: GAP-XXX-business-correctness> |
| Security Audit /100 | GAP-XXX-{owasp/category}-{issue} | <example: GAP-XXX-svg-xss-protection> |
| Performance Audit /100 | GAP-XXX-{area}-{issue} | <example: GAP-XXX-n-plus-one-queries> |
| API Contract /100 | GAP-XXX-{service}-{issue} | <example: GAP-XXX-endpoint-undocumented> |
| Ops Readiness /100 | GAP-XXX-{area}-{issue} | <example: GAP-XXX-missing-health-probes> |

---

## 5. Integration

- **CLAUDE.md** references this rule
- **Audit skills** output issue list → trigger this pipeline
- **gap-to-pr-converter** skill consumes gap files → generates PR
- **wave-completion-check** verifies all gaps in wave are DONE

---

## 6. Log

- **2026-04-29** (v1.0.0 upstream import): Imported into starter-kit v2.3.0 from project source. Local project remains source of truth; upstream version may diverge as starter-kit evolves separately.
- **2026-04-28** (v1.0.0 backfill): Frontmatter backfill — added Last-Reviewed + Reviewer-Approver + Applies-to fields; reformatted existing Version `1.0` → `1.0.0` (semver three-part canonical). No content change.
- 2026-04-20 — Added **Step 2.5 State-Check Against Current Codebase** after gaps were filed without code-state verification; both required follow-up rewrite. Step 2.5 is BẮT BUỘC alongside Step 2 — dedupe alone is insufficient.
- 2026-04-16 — Rule created after UI audit session produced 5 gaps; user requested formalization.
