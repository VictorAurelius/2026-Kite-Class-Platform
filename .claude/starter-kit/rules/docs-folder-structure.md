# Docs Folder Structure — Generic Rule for `documents/`

**Priority:** 🟠 MANDATORY — governance for all top-level folders trong `documents/`
**Version:** 1.0.0
**Created:** 2026-04-18
**Last-Reviewed:** 2026-04-29
**Reviewer-Approver:** @nguyenvankiet (starter-kit upstream maintainer)
**Applies to:** Every top-level folder under `documents/` (e.g. 00-brd, 01-business, 02-architecture, 03-planning, 04-quality, 05-guides, 06-diagrams, 07-archived, và future folders)

---

## 1. Purpose

Generalizes the pattern from `planning-docs-structure.md` (a specialized rule for one folder) to ALL of `documents/`. Ensures navigability, consistent structure, và clear ownership cho mọi documentation folder.

**Note:** Specialized rules (e.g. for `03-planning/` if you adopt one) take precedence where they overlap.

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
| `03-planning/` | `planning-docs-structure.md` (if adopted) | Frontmatter required, wave/plan taxonomy |
| `01-business/` | (project CLAUDE.md §Business Logic Documents) | 3-file structure per domain |
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

- **`planning-docs-structure.md`** (if project adopts one) — specialized rule cho 03-planning; OVERRIDES nơi xung đột
- **`output-review-mandate.md`** — mandate review standards; README phải có "review process" nếu folder produce outputs
- **`audit-to-gap-pipeline.md`** — `04-quality/gaps/` follows specialized template; README link to this rule

---

## 9. Log

- **2026-04-29** (v1.0.0 upstream import): Imported into starter-kit v2.3.0 from project source. Local project remains source of truth; upstream version may diverge as starter-kit evolves separately.
- **2026-04-28** (v1.0.0 backfill): Frontmatter backfill — added Version + Last-Reviewed + Reviewer-Approver fields. No content change.
- **2026-04-18:** Rule created after planning docs restructure. Generalizes pattern từ 03-planning sang toàn `documents/`.
