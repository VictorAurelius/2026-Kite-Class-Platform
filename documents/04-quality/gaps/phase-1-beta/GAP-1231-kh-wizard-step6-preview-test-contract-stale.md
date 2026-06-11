# GAP-1231: KH wizard Step6Preview test contract stale — 7/8 fail trên main (marker `data-preview-template` + per-template bodies dropped bởi rewrite #2279/#2289)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 (gộp cụm GAP-1215 AI-chain — symptom cùng rewrite)
**Domain:** Frontend (kitehub-frontend wizard)
**Found:** 2026-06-11 (Wave ui-kits-100 Bucket G — CI `Test — KiteHub Frontend` chạy lần đầu sau nhiều PR vì path-filter; test fail SẴN trên main, không do G)
**Affects:** `kitehub-frontend/src/components/branding/wizard/__tests__/Step6Preview-template-reflect.test.tsx` (7/8 fail) + §3a guarantee "preview reflects selected template"

## Problem

Test GAP-272 §3a expect preview srcDoc chứa `data-preview-template="template-tN-*"` + per-template body ('98% học viên đạt điểm 9+', 't-bars'...). Production preview builder được rewrite ở #2279/#2289 (AI branding wizard + deploy) — marker chỉ còn TRONG test (grep production = 0 hit) → 7/8 fail trên main. CI path-filter (`kitehub-frontend/**`) khiến main không chạy test này từ sau merge → silent. Đây là symptom test-contract của cùng rewrite gây GAP-1215 (Preview-source ≠ deploy-source — WYSIWYG vỡ).

**Quarantine tạm (Wave ui-kits-100 Bucket G):** `describe.skip` + cite gap này — unblock các PR đụng kitehub-frontend không liên quan wizard. KHÔNG phải fix; un-skip là AC.

## Proposed Fix

Trong cụm AI-chain wave (GAP-1215/1021/1147 — design source: `ui_kits/ai-branding-wizard-v2/v3/` per GAP-1212): rework preview builder thống nhất preview-source = deploy-source (TemplateRenderer), re-emit marker `data-preview-template` (hoặc update test contract theo builder mới), un-skip test.

## Acceptance Criteria

- [ ] Test un-skipped + 8/8 PASS với builder mới (hoặc contract mới documented)
- [ ] §3a guarantee verify: preview srcDoc khác nhau per template (đúng tinh thần GAP-272 §3a + GAP-1215)

## Related

- GAP-1215 (WYSIWYG class — cùng rewrite), GAP-1212 (design source v3), #2279/#2289 (rewrite PRs), GAP-1220-class (CI path-filter che fail trên main)
- Discovered in: Wave ui-kits-100 Bucket G PR #2339 (CI surfaced, fail pre-existing trên main — verified local `vitest run` trên main 7/8 fail)
