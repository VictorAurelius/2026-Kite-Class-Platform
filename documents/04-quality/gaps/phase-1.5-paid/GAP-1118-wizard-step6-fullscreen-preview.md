# GAP-1118: Step6 preview full-screen + access các quyết định trước (audience/tone/template/logo)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Frontend
**Found:** 2026-06-10 (discuss wizard 6-bước với user — design critique câu 4)
**Affects:** `kitehub-frontend` `Step6Preview.tsx` (+ reuse `TemplateFullscreen.tsx` pattern)

## Problem

Preview hiện ở `Step6Preview` chỉ là **1 khung nhỏ** (in-page). User muốn preview là **full-screen live preview** (iframe landing thật) + **panel hiện/sửa các quyết định trước** (audience / tone / template / logo / portrait đã chọn) ngay trong màn preview.

Khả thi + đúng intent:
- Pattern full-screen đã có sẵn: `TemplateFullscreen.tsx` (fullscreen cho bước Template 5).
- `ai-branding-guidelines.md` §4.2 mandate "Show live preview (iframe)".

## Proposed Fix

1. `Step6Preview` thêm **full-screen mode** (reuse `TemplateFullscreen` pattern) — iframe render landing thật với theme + assets đã generate ([[GAP-1117]]).
2. **Side panel** trong full-screen: summary các bước đã quyết (audience/tone/template/logo/portrait) + cho phép quick-edit (jump về step tương ứng) — vẫn giữ per-resource approve (§4.2).
3. Responsive (desktop full-screen; mobile collapse panel).

## Acceptance Criteria

- [ ] Step6 có full-screen preview mode (toggle), reuse `TemplateFullscreen` pattern
- [ ] iframe live preview landing thật (theme + assets) per §4.2
- [ ] Side panel hiện các quyết định trước + quick-edit/jump-to-step
- [ ] Per-resource approve giữ nguyên (§4.2); responsive
- [ ] Build xanh; browser G2 walk verify

## Related

- Discovered in: discuss wizard 6-bước 2026-06-10 (user critique câu 4)
- Reuse pattern: `TemplateFullscreen.tsx` (fullscreen template step)
- Depends: [[GAP-1117]] (render result để preview)
- Cluster: [[GAP-1115]] / [[GAP-1116]]
- Design: `ai-branding-guidelines.md` §4.2 (preview-before-commit, live iframe), screenshot `design-system/screenshots/ai-branding-wizard-step6.png`

## Log

- **2026-06-10:** Filed từ discuss wizard với user — preview khung nhỏ → muốn full-screen + access prior decisions. Pattern fullscreen đã có (`TemplateFullscreen`). Per `discovery-to-gap-inline-filing.md`. GAP-ID từ block reserve 1115-1118.
