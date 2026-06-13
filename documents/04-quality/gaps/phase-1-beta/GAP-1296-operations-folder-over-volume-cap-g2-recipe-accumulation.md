# GAP-1296: `documents/05-guides/operations/` vượt volume cap (70/50) — g2-recipe tích lũy

**Status:** 🔵 OPEN
**Priority:** 🟢 P3
**Domain:** Docs
**Found:** 2026-06-14 (khi viết 4 G2 recipe RBAC+LMS — `docs/rbac-lms-g2-recipes`)
**Affects:** `documents/05-guides/operations/` (time-bound artifact class, cap 50 per `docs-folder-volume-budget.md` §2)

## Problem

Folder `documents/05-guides/operations/` hiện có **70 file `.md` active** (sau khi thêm 4 G2 recipe wave RBAC-LMS), vượt cap 50 của class "time-bound artifact" (per `docs-folder-volume-budget.md` §2). Nguyên nhân chính: **~30+ file `YYYY-MM-DD-g2-recipe-*.md`** tích lũy từ Flow Verification Campaign (KC-1..KC-12, KH-3..KH-10, landing-100, demo-seed, branding-100, RBAC+LMS).

Đây là tình trạng **pre-existing over-cap** (66 file trước khi thêm 4 recipe lần này). 4 recipe mới BẮT BUỘC nằm trong folder này per `g2-handoff-md-mandate.md` §4 (filename convention `documents/05-guides/operations/YYYY-MM-DD-g2-recipe-<flow>.md`) — KHÔNG thể đặt nơi khác mà không vi phạm mandate kia.

Discovered per `discovery-to-gap-inline-filing.md` §1 (phát hiện trong non-audit work = viết docs).

## Proposed Fix

Sub-split G2 recipe theo semantic dimension (per `docs-folder-volume-budget.md` §4.2) — vd subdir `operations/g2-recipes/` HOẶC archive recipe của flow đã `✅ THÔNG (production)`. Cần reconcile với `g2-handoff-md-mandate.md` §4 filename convention (có thể cần cập nhật mandate cho subdir path). Recipe của flow đã verified production → archive `operations/archived/` hoặc `closed/`.

## Acceptance Criteria

- [ ] `operations/*.md` active count ≤ 50 (archive recipe flow đã THÔNG production HOẶC sub-split semantic)
- [ ] Cập nhật `g2-handoff-md-mandate.md` §4 nếu chuyển sang subdir path (giữ 2 rule không mâu thuẫn)
- [ ] Verify `bash scripts/check-docs-folder-volume.sh` PASS (hoặc WARN→OK) sau split/archive

## Related

- Discovered in: PR `docs/rbac-lms-g2-recipes` (4 recipe RBAC+LMS G2)
- Rule: `docs-folder-volume-budget.md` §2 cap + §4 trigger flow + §6.2 override
- Rule: `g2-handoff-md-mandate.md` §4 filename convention (path constraint)
- Override applied this PR: `DOCS_VOLUME_OVERRIDE: documents/05-guides/operations 70/50 — g2-recipe mandated path per g2-handoff-md-mandate §4; archive/split deferred GAP-1296`
