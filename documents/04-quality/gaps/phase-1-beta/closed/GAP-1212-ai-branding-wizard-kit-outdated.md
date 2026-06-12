# GAP-1212: Kit `ai-branding-wizard-v2` outdated — không khớp wizard KC hiện tại + thiếu design các bước ADR-037

**Status:** 🟢 DONE
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

- [x] Kit cũ annotated superseded-scope (không xoá — archive policy) — README header ⚠️ note + legacy PRO-tier note
- [x] Design screens cho wizard KC hiện hành + bước ADR-037 (≥105/128) — 11 screens `v3/screens/*.html` (self-score 110-118/128, avg ~114)
- [x] 4-layer pointers per design-layer-coverage §2.2 — `v3/index.html` §4-layer table + per-screen HTML comment header
- [x] Cụm GAP-1134/1147/1135 implementation cite kit mới làm design source — Log line added to GAP-1134/1147/1135/1021 pointing tới `v3/`

## Related

- Cụm AI chain: GAP-1021/1108/1134/1135/1147/1160; ADR-037
- Same class: GAP-1208 (template trôi vì thiếu design spec), GAP-1212 = wizard tương tự
- Kit: `ui_kits/ai-branding-wizard-v2/README.md` (Direction C, 28 screens — archive) + `ui_kits/ai-branding-wizard-v2/v3/` (canonical KC per-tenant)

## Log

- **2026-06-11 (DONE — Wave ui-kits-100 Bucket D):** Refresh kit theo ADR-037 + wizard production. Shipped `v3/` (11 screens + index hub + styles.css): 6 bước canonical KC per-tenant (Welcome → Logo+favicon → Audience → Tone → Template → Preview per `BrandingWizard.tsx`) + bước ADR-037 (mode selector TEMPLATE/FULL_AI GAP-1147 · portrait upload GAP-1134 · banner GENERATING/FAILED/READY GAP-1135 · SSE preview/deploy GAP-1021). Tier canonical FREE/BASIC/PREMIUM/ENTERPRISE (per `PricingTier.java`), favicon affordance per GAP-1229. v2 (Direction C / KH-provisioning) annotated superseded + giữ archive. ui_kits hub card + landing parity (`check-ui-kits-landing.sh` exit 0). Token-compliant (Be Vietnam Pro, theme-kiteclass, zero hardcoded hex ngoài token), VN demo-trio (Cô Hà / Thầy Nhì), WCAG AA comment per screen, 4-layer pointers. design-only — implementation cụm AI chain ngoài wave, cite v3 làm design source.

- **2026-06-12 (v2 REDESIGN output-first — Wave branding-100 Bucket A):** Redesign v3 theo bộ bước §2.5 hội tụ 3 audit 2026-06-11 (persona / benchmark / failure-mode 23/42 vỡ). Chuyển flow 9-bước tuần tự → **output-first ≤5 bước nhập liệu**: `welcome-mode` (Welcome + Mode lên đầu + escape-ramp "Tạo ngay với mặc định") → `personality` (gộp Đối tượng + Phong cách + facts GAP-1234 progressive-disclosure) → `assets-logo`/`assets-portrait` (Logo/Portrait tùy chọn, branch theo mode) → `generate-ready` (Tạo & Live Preview THẬT: multi-variant ≥2 biến thể + quality gate /100 widget + per-resource approve + preview = deploy source) → `deploy-progress` (SSE lifecycle) → `done` (Hoàn tất + link landing tenant). Generate xảy ra NGAY sau Phong cách (output-first). States đủ (lối thoát rõ, không dead-end): `generate-generating` (GENERATING+SSE), `generate-failed` (FAILED retry/fallback/back GAP-1216), `generate-quality-fail` (gate <70 + hướng dẫn GAP-1217), `generate-quota-empty` (PREMIUM quota label trung thực GAP-1218), `deploy-failed` (recovery GAP-1108/1216). Biến thể PREMIUM FULL_AI: `welcome-mode-premium` + `assets-portrait` + `generate-ready-fullai`. Giữ ràng buộc: click-through 17 screens (GAP-1233, cả 2 nhánh walk được), facts GAP-1234 progressive-disclosure (không thêm bước chặn), 3 màn editor GAP-815 (link 2 chiều + cross-link `step1-welcome.html` → `welcome-mode.html`), tier canonical + FULL_AI PREMIUM 5/tháng + ENTERPRISE unlimited (GAP-1137). 12 step*.html cũ git rm; 14 screen output-first mới + 3 editor giữ. index.html + README §v3 + ui_kits landing card cập nhật. Token-compliant (reuse qgate/lifecycle/tpl/choice/mode-grid/sse-log/banner-stage classes; zero hardcoded hex mới ngoài variant gradient demo), WCAG AA comment + 4-layer pointer per screen, self-score avg ~114/128 (≥105 ✓; generate-ready 119 headline). Link integrity 100% + landing parity (`check-ui-kits-landing.sh` exit 0). design-only — implementation cụm AI chain ở Bucket C/D/E của wave, cite v3 làm design source.
