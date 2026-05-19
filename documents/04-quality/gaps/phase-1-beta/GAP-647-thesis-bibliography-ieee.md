# GAP-647: Thesis bibliography — IEEE citation style + refs.md canonical

**Status:** 🟡 PARTIAL 80% — Step 1 IEEE style + Step 2 44 refs bibliography (Wave 97 + Wave 100 + Wave 100.7 Phase 2 + Phase 4) shipped + Phase 4 V1 cross-ref polish 89% inline utilization. Step 3 citation-extract skill tracked **GAP-655** (Wave 98+ thesis tooling)
**Priority:** 🔴 P0 (META — paired GAP-646)
**Domain:** Meta
**Phase:** phase-1-beta
**Found:** 2026-05-18
**Related Audits:** [thesis-defense-failure-mode-matrix](../../audits/persona-review/2026-05-18-thesis-defense-failure-mode-matrix.md)

## Current State (verified 2026-05-18)

| Piece | Path | Status |
|---|---|---|
| Citation style chosen | — | ❌ chưa chọn (IEEE / APA / Chicago) |
| Bibliography file canonical | `documents/08-thesis/references/bibliography.md` | ❌ missing |
| In-text citation pattern | per `dev-readable-doc-language.md` | ❌ no convention |
| Skill extract citation từ WebFetch | `.claude/skills/quality/citation-extract/SKILL.md` | ❌ missing |
| Existing references | `documents/08-thesis/references/*.md` | ⚠️ 5 files INTERNAL refs only (technology-stack, methodology, quality-metrics, testing-results, deployment-guide) |
| External academic refs in repo | grep `arxiv\|doi.org\|ieee.org` documents/ | ⚠️ scattered, không structured |

## Problem

VN CS thesis 2026 mandate 20-40 references mix English (academic) + Vietnamese (industry/law) cited consistently. Failure-mode aggregate P0 #5: "IEEE citations hoàn toàn vắng trong draft v3.1 — retroactive cite toàn bộ theo [1][2][3] format". Hội đồng UIT/HUST/UET đặc biệt check bibliography quality + in-text citation discipline.

Current state: zero structured bibliography, zero citation style chosen, zero skill để extract/format citations từ web sources khi research mới.

## Proposed Fix

### Step 1: Choose citation style — IEEE (recommend)

Per VN CS convention (UIT/HUST/UET preference) + computer science international norm. Format:
```
[1] A. Author, B. Co-Author, "Title of paper," Journal Name, vol. X, no. Y, pp. Z-W, Month Year.
[2] AWS, "Well-Architected Framework," 2024. [Online]. Available: https://aws.amazon.com/architecture/well-architected
```

In-text: `[1]`, `[1, 2]`, `[3]–[5]`.

### Step 2: Create `documents/08-thesis/references/bibliography.md`

Sectioned by Chapter target:
```markdown
# Bibliography — IEEE Format

## Chapter 1 — Introduction
[1] ...
[2] ...

## Chapter 2 — Theoretical Background
[3] ...

## Chapter 3 — Requirements Analysis
...

## Chapter 4-7 ...
```

Pre-populate với ~30 refs:
- 8-10 web technology docs (AWS, Spring Boot, Next.js, PostgreSQL, Anthropic API)
- 5-7 academic papers (multi-tenant SaaS architecture, microservices patterns, AI integration)
- 4-5 VN law/standard (PDPL 2023, Decree 13/2023, Decree 53/2022, Luật ANM 2018)
- 3-5 VN edu SaaS industry refs (EasyEdu, MISA EMIS, Misa AMIS)
- 3-5 books (Building Microservices Sam Newman, DDD Evans, Clean Architecture Martin)

### Step 3: Skill `quality/citation-extract`

`.claude/skills/quality/citation-extract/SKILL.md`:
- Trigger: "cite this URL", "add reference", "format citation IEEE"
- Workflow: WebFetch URL → extract metadata (title, author, year, source) → format IEEE → append to `bibliography.md` matching chapter section
- Output: append + return [N] ref number

### Step 4: Citation review checklist

Add row `output-review-mandate.md` §3:
> "Thesis citations" — every external claim in thesis MUST have `[N]` ref pointing to bibliography.md row

### Step 5: Pre-commit hook (defer Phase 2 per `incident-to-rule-pipeline.md` premature-rule guard)

Future: `scripts/check-thesis-citations.sh` — scan thesis source markdown for unsourced claims + warn missing `[N]` refs.

## Acceptance Criteria

- [ ] Citation style IEEE chosen + documented `documents/08-thesis/references/CITATION-STYLE.md` (1-page)
- [ ] `bibliography.md` exists với ≥30 refs structured per chapter
- [ ] Skill `quality/citation-extract/SKILL.md` shipped
- [ ] In-text citation pattern documented (`[N]` simple, `[N, M]` multiple, `[N]–[M]` range)
- [ ] `output-review-mandate.md` §3 thêm row "Thesis citations"
- [ ] Sample thesis draft section retroactive cite ≥10 claims với `[N]` refs

## Related

- GAP-646 thesis-docx-pipeline (bibliography section trong template)
- GAP-650 thesis-chapter-1-literature (literature review needs citations)
- `documents/08-thesis/references/` 5 internal docs (extend, không replace)

## Log

- **2026-05-19 (PARTIAL 50% → 80%):** Wave 100.7 Phase 4 V1 closure boosted bibliography quality: 44 refs IEEE compliant (Wave 97 30 + Wave 100 D 8 + Wave 100.7 Phase 2 5 + Phase 4 Bucket B 2 new − Phase 4 Foundation dedup 1 = 44 net). 89% inline cite utilization across Ch.1+2+3+4 (was 39% pre-Phase-4). `cross-ref-audit-2026-05-19.md` Round 3 confirms production-ready cho V1 thesis. Step 3 citation-extract skill (deferred GAP-655) remaining 20%.
- **2026-05-18 (created):** Filed per Release 1.5 thesis scope outside-in audit. Failure-mode aggregate P0 #5 + VN benchmark §3 Q3 examiner concern.
