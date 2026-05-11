# GAP-101: Docs Folder README Standardization

**Status:** 🟢 DONE (2026-04-18, PR #347)
**Priority:** 🟢 P3
**Domain:** Documentation governance
**Found:** 2026-04-18 (post PR #345 planning docs restructure)
**Affects:** `documents/00-brd`, `documents/02-architecture`, `documents/05-guides`, `documents/07-archived`

## Problem

PR #345 ra rule `planning-docs-structure.md` bắt buộc `documents/03-planning/README.md` có: purpose, directory map, how-to-add, archive policy. Rule chỉ apply cho `03-planning/`.

Kiểm tra 9 top-level folders khác trong `documents/`:

| Folder | README | Files | Trạng thái |
|--------|:------:|:-----:|------------|
| `00-brd` | ❌ | 1 | Thiếu — chỉ có `personas-catalog.md`, không có navigation |
| `01-business` | ✅ | 2+tree | OK |
| `02-architecture` | ❌ | 10 flat + `adr/` rỗng | Thiếu — 10 docs phẳng, không group |
| `03-planning` | ✅ | — | Vừa restructure |
| `04-quality` | ✅ | 4 subdirs | OK |
| `05-guides` | ❌ | 5 + 2 subdirs | Thiếu — operational docs không có index |
| `06-diagrams` | ✅ | 3 subdirs | OK |
| `07-archived` | ❌ | 8 subdirs | Thiếu — historical docs, navigation khó |
| `08-thesis` | ✅ | — | OK |

Hệ quả:
- Contributor mới không biết file nên đặt ở folder nào (00-brd vs 01-business cho business docs; 02-architecture vs 05-guides cho deploy strategy)
- Scattered deploy-related docs hiện ở 6 vị trí khác nhau (infrastructure/, implementation/, 05-guides/, operations/runbooks/, vietnamese/, thesis/references/)
- Không có archival policy rõ ràng cho folders ngoài `03-planning`

## Root Cause

Rule `planning-docs-structure.md` được tạo riêng cho 03-planning lúc restructure, chưa generalize. Historical folders (pre-2026-04) được tạo ad-hoc mà không có template README.

## Proposed Fix

### Option A: 1 PR — README cho 4 folders thiếu (recommended)
Tạo README.md cho mỗi folder theo template chuẩn:
- Purpose (1 đoạn)
- Directory map (table: path → purpose → typical files)
- File placement rules (cái gì thuộc đây vs folder khác)
- How to add / archive policy
- Links tới key docs trong folder

### Option B: Extend rule → `docs-folder-structure.md` (generic)
Tạo `.claude/rules/docs-folder-structure.md` — generic version của `planning-docs-structure.md` áp dụng cho toàn `documents/`. Rule § mới:
- Mọi top-level folder phải có `README.md`
- README chứa directory map + file placement rules + archive policy
- Frontmatter cho `.md` chỉ required ở `03-planning/` (giữ nguyên)
- Pre-merge PR review check README updated nếu thêm subdir

### Recommendation: Làm cả 2 — GAP-101 = Option A, viết rule mới trong cùng PR

## Acceptance Criteria

- [x] README.md tạo cho `00-brd`, `02-architecture`, `05-guides`, `07-archived`
- [x] Mỗi README có: purpose + directory map + file placement + archive policy
- [x] `.claude/rules/docs-folder-structure.md` (generic) được tạo, reference từ `planning-docs-structure.md`
- [x] CLAUDE.md cập nhật link tới rule mới
- [x] 2 stacked PRs: #346 (rule + gap files) → #347 (4 READMEs)

## Dependencies

- None. Standalone task.

## Related

- PR #345 — `planning-docs-structure.md` (source pattern)
- GAP-102 — `05-guides` completion (sẽ consume README từ GAP-101)
- GAP-103 — Deploy philosophy single-source (sẽ mention trong `02-architecture/README.md`)
