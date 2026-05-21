---
paths:
  - "documents/**/README.md"
---

# Docs Folder Structure — Generic Rule for `documents/`

**Priority:** 🟠 MANDATORY — governance for all top-level folders trong `documents/`
**Version:** 1.0.2
**Created:** 2026-04-18
**Last-Reviewed:** 2026-05-21
**Reviewer-Approver:** @nguyenvankiet (solo-dev — v1.0.2 PATCH self-approve per `rule-change-process.md` §5; narrow `paths:` xuống chỉ `documents/**/README.md` (drop overly-broad `documents/**/*.md`) — rule §2 scope là folder README structure, không phải mọi document read. Wave 102.7.5 meta cleanup giảm wave-plan-read auto-load bundle. No constraint change; §2 review process unchanged. v1.0.1 (kept): paths added Wave 73 Bucket A3. v1.0.0 (giữ): solo-dev backfill per `rule-change-process.md` §3)
**Applies to:** Every top-level folder under `documents/` (00-brd, 01-business, 02-architecture, 03-planning, 04-quality, 05-guides, 06-diagrams, 07-archived, 08-thesis, và future folders)

---

## 1. Purpose

Generalizes pattern từ `planning-docs-structure.md` (03-planning only) ra toàn bộ `documents/`. Ensures navigability, consistent structure, và clear ownership cho mọi documentation folder.

**KHÔNG thay thế** `planning-docs-structure.md` — rule đó vẫn là source of truth cho 03-planning (frontmatter, archival rules, specific layout). File này áp dụng CHUNG cho mọi folder.

---

## 2. The Rule

> **Every top-level folder under `documents/` PHẢI có `README.md` với 4 sections:**
> 1. Purpose (1 đoạn)
> 2. Directory map (table: path → purpose → typical files)
> 3. File placement rules (cái gì thuộc đây vs folder khác)
> 4. Archive/retention policy (khi nào move đi đâu)

---

## 3. README Template

```markdown
# {NN-folder-name} — {Short Purpose}

**Rules:** [`.claude/rules/docs-folder-structure.md`](../../.claude/rules/docs-folder-structure.md)

{1 paragraph — purpose of this folder, audience, relationship with sibling folders}

---

## Directory Map

| Path | Purpose | Typical files |
|------|---------|---------------|
| `README.md` | This index | 1 |
| `{subdir}/` | {purpose} | {examples} |

---

## File Placement Rules

- ✅ **Belongs here:** {criteria}
- ❌ **Does NOT belong here:** {what belongs in sibling folder, with link}
- Naming: `{pattern}`

---

## Archive Policy

Move to `documents/07-archived/{folder-name}-YYYY/` khi:
- {Condition 1}
- {Condition 2}
- Doc >180 days old AND no recent reference

---

## Key Documents

- [{title}]({path}) — {1-line description}
```

---

## 4. Per-Folder Specializations

Một số folder có rule đặc biệt — README của chúng phải reference rule file chuyên biệt:

| Folder | Extra Rule File | Why |
|--------|-----------------|-----|
| `03-planning/` | `planning-docs-structure.md` | Frontmatter required, wave/plan taxonomy |
| `01-business/` | (CLAUDE.md §Business Logic Documents 3-Layer) | 3-file structure per domain |
| `02-architecture/adr/` | (MADR template in README) | ADR naming + format |
| `04-quality/gaps/` | `audit-to-gap-pipeline.md` | Gap file template, priority order |
| `04-quality/audits/` | `output-review-mandate.md` §3 | Audit report standards |

Folders không có rule chuyên biệt chỉ cần README theo template §3.

---

## 5. Ownership Matrix

| Folder | Owner | Reviewer |
|--------|-------|----------|
| `00-brd` | PM / Business Lead | Tech Lead |
| `01-business` | Dev + PM | PR reviewer |
| `02-architecture` | Architect | Tech Lead |
| `02-architecture/adr/` | Architect | Team consensus |
| `03-planning` | Wave lead | Tech Lead |
| `04-quality` | QA / Auditor | Lead auditor |
| `05-guides` | SRE / DevOps | Ops lead |
| `06-diagrams` | Architect | Tech Lead |
| `07-archived` | Anyone (append-only) | — |
| `08-thesis` | Thesis author | Advisor |

---

## 6. Anti-Patterns

| ❌ Don't | ✅ Do |
|---------|------|
| Tạo folder mới trong `documents/` mà không có README | Template từ §3 trước khi commit file đầu tiên |
| Copy docs giữa folders khi không rõ thuộc đâu | Dùng `file placement rules` trong README để quyết |
| Để folder rỗng mà không có stub README | README giải thích "planned content" + timeline |
| Mix concerns (vd. deploy docs ở cả `03-planning/infrastructure` và `05-guides/operations`) | 1 folder = 1 concern, cross-reference từ README |
| Archive docs nhưng không update README của folder gốc | Remove link từ README khi archive |

---

## 7. Enforcement

- **Pre-merge PR review:** reviewer check README updated nếu PR thêm/xóa subdir hoặc file notable
- **Pre-commit hook (future):** warn nếu top-level folder trong `documents/` không có README
- **Quarterly doc audit:** verify tất cả folders có README + README chưa stale

---

## 8. Relationship to Other Rules

- **`planning-docs-structure.md`** — specific rule cho 03-planning; OVERRIDES nơi xung đột
- **`output-review-mandate.md`** — mandate review standards; README phải có "review process" nếu folder produce outputs
- **`audit-to-gap-pipeline.md`** — `04-quality/gaps/` follows specialized template; README link to this rule

---

## 9. Log

- **2026-05-21** (v1.0.2): Wave 102.7.5 meta cleanup — narrow `paths:` xuống `documents/**/README.md` only (drop `documents/**/*.md`). Rule §2 scope = folder README structure; broad pattern fired trên mọi document read (wave plan, gap file, audit, business doc) without rule relevance — measured cost: contributed ~14% context auto-load on single wave-plan read. Sync `rules-index.csv` path_trigger column. No constraint change; review process unchanged. Reviewer: @nguyenvankiet (solo-dev PATCH self-approve per `rule-change-process.md` §5 — path-scope narrowing per precedent v1.0.1 + `context-budget-mandate.md` §3.1 broad-glob anti-pattern).
- **2026-05-14** (v1.0.1): Wave 73 Bucket A3 — thêm `paths:` frontmatter (`documents/**/README.md`, `documents/**/*.md`) cho Anthropic native deferred-loading. Path-scope MANDATORY rule giúp tiết kiệm token context khi session không động chạm `documents/`. Không đổi constraint, scope rule giữ nguyên. Sync `rules-index.csv` path_trigger column. Reviewer: @nguyenvankiet (solo-dev PATCH self-approve per `rule-change-process.md` §5).
- **2026-04-28** (v1.0.0 backfill): Frontmatter backfill per GAP-249 — added Version + Last-Reviewed + Reviewer-Approver fields. No content change. Solo-dev PATCH self-approve per `rule-change-process.md` §5.
- **2026-04-18:** Rule created (GAP-101) after planning docs restructure. Generalizes pattern từ 03-planning sang toàn documents/.
