# GAP-1212: Kit `ai-branding-wizard-v2` outdated — không khớp wizard KC hiện tại + thiếu design các bước ADR-037

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Frontend / Design System
**Found:** 2026-06-11 (user-flagged "ui kits có design cho các bước ai branding không? có vẻ outdated" — investigation confirm)
**Affects:** `ui_kits/ai-branding-wizard-v2/` (28 screens) vs `kiteclass-frontend/src/components/branding/wizard/`

## Problem

Kit vs wizard thật lệch 2 trục:

1. **Sai surface**: kit Direction C (Round 2, Wave 1.7) design cho **KH provisioning wizard** (step1 slug/tenant input, lifecycle DEPLOYED/FAILED, ENTERPRISE advanced mode, quality gate /100 ở step 6). Wizard production hiện hành là **KC per-tenant branding wizard** (`BrandingWizard.tsx` state machine: Welcome → Logo → Audience → Tone → Template → Preview) — bộ bước khác hẳn, không slug/tenant input.
2. **Thiếu các bước mới ADR-037**: mode selector FULL_AI (GAP-1147 "Step 7"), bước upload chân dung (GAP-1134), banner generate HTML+Gemini / GPT-5.5 image (GAP-1135), SSE preview/deploy (GAP-1021) — **0 screen design** cho các bước này. Engineering đang build không có 基本設計 (per design-layer-coverage §2 — layer 2 ❌ cho scope wizard mới).

Đúng class "design source stale" — code tiến hoá (ADR-037) nhưng kit đứng ở Direction C cũ → port/parity check vô nghĩa, agents implement bằng judgment (nguyên nhân cùng loại với GAP-1208 personal template trôi).

## Proposed Fix

Refresh kit TRƯỚC khi fix cụm AI-chain (GAP-1021/1134/1135/1147/1160 — design đi trước implementation):
1. Annotate kit hiện tại header "⚠️ Direction C / KH-provisioning — superseded một phần bởi ADR-037" (giữ làm archive reference).
2. Kit mới (hoặc v3 section trong kit): KC branding wizard 6 bước hiện hành + các bước ADR-037 (mode selector, portrait upload, banner generate states GENERATING/FAILED/READY, preview SSE) — rubric /128 per chuẩn.
3. Gộp vào wave fix cụm AI-branding chain (campaign KH-6/KC-10).

## Acceptance Criteria

- [ ] Kit cũ annotated superseded-scope (không xoá — archive policy)
- [ ] Design screens cho wizard KC hiện hành + bước ADR-037 (≥105/128)
- [ ] 4-layer pointers per design-layer-coverage §2.2
- [ ] Cụm GAP-1134/1147/1135 implementation cite kit mới làm design source

## Related

- Cụm AI chain: GAP-1021/1108/1134/1135/1147/1160; ADR-037
- Same class: GAP-1208 (template trôi vì thiếu design spec), GAP-1212 = wizard tương tự
- Kit: `ui_kits/ai-branding-wizard-v2/README.md` (Direction C, 28 screens)
