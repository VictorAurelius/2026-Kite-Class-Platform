# GAP-1245: Multi-variant pick chưa wire vào deploy palette (BE) — non-base variant preview ≠ deploy

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Mixed
**Found:** 2026-06-12 (branding-100 Bucket D — Agent D, discover khi implement GAP-1212 multi-variant)
**Affects:** `kitehub-frontend/.../wizard/{Step6Preview.tsx, paletteVariants.ts}` (FE) + deploy/approve pipeline (BE — kitehub-branding)

## Problem

GAP-1212 multi-variant pick (3 biến thể palette) shipped như **preview affordance**: chọn variant → đổi theme + banner trong bản xem trước (`/preview` iframe). NHƯNG deploy/approve (`useApproveBrandingJob`) vẫn dùng palette BE-resolved của job (= variant A, base). Nếu user chọn variant B/C rồi triển khai → landing thật áp variant A, KHÔNG phải variant đã preview → tái phát WYSIWYG drift cho non-base variant.

Variant A (default + selected ban đầu) = deploy-faithful nên drift CHỈ xảy ra khi user chủ động đổi sang B/C. Bucket C (deploy chain thật) đã đóng wave này nên không đụng BE pipeline sâu — defer.

## Proposed Fix

Wire selected variant palette qua approve/deploy: hoặc (a) BE `approve` nhận `paletteOverride {primary,secondary,accent}` → áp vào branding theme trước DEPLOYED; hoặc (b) FE chỉ cho phép pick variant TRƯỚC create-job + regenerate job với palette đã chọn (job-resolved palette = variant chọn). Chọn cách rẻ khi mở Bucket C-tiếp.

## Acceptance Criteria

- [ ] Chọn variant B/C → landing deploy áp đúng palette variant đã preview (preview == deploy cho mọi variant, không chỉ base)

## Related

- GAP-1215 (preview-source = deploy-source, cụm cha), GAP-1212 (kit multi-variant), GAP-1213/GAP-1217 (deploy chain thật — Bucket C)
- Discovered in: branding-100 Bucket D (Agent D) — `## Discoveries filed`
