# GAP-1226: TemplateRenderer zebra wrapper render cho section rỗng → dải nền lệch nhịp

**Status:** 🟢 DONE
**Priority:** 🟡 P2
**Domain:** Frontend
**Found:** 2026-06-11 (re-score — empty-section collapse)
**Affects:** `TemplateRenderer.tsx` zebra striping (`bg-muted/40` per index)

## Problem

Wrapper zebra tính striped theo index TRƯỚC khi biết component trả null (hide-when-empty) → section rỗng vẫn chiếm 1 nhịp zebra → 2 section liền kề cùng màu nền / nhịp visual lệch so kit.

## Proposed Fix

TemplateRenderer skip hoàn toàn section data-required khi slot absent (helper hasContent per sectionId) → zebra tính trên sections thật render. Sections có fallback (hero/about/timeline) giữ nguyên.

## Acceptance Criteria

- [x] Tenant thiếu data: zebra xen kẽ đúng trên các section còn lại
- [x] Unit test zebra skip

## Log

- **2026-06-11 (DONE):** `sectionHasContent` + CONTENT_REQUIRED set — bandIndex chỉ tăng khi render thật. vitest zebra strict-alternation PASS.
