# GAP-1147: Step 7 thiếu action tạo banner FULL_AI — mode selector chỉ set state

**Status:** 🟡 PARTIAL — action+gate shipped PR #2289; real AI render Phase 2 (GAP-1135)
**Priority:** 🟡 P2
**Domain:** Frontend + Backend
**Found:** 2026-06-10 (Wizard Step 7 G2 browser-walk — PR #2289)
**Affects:** wizard Step 7 `GenerationModeSelector` · deploy-mode pipeline

## Problem

G2 feedback #3: "cần ấn nút tạo lại hoặc design chuẩn để thay banner template thành full AI".

Hiện trạng sau enhancement wave-wizard-step7:
- `GenerationModeSelector` (GAP-1142) cho chọn TEMPLATE/FULL_AI nhưng **chỉ set state** — không có action thực thi render FULL_AI.
- Live banner preview (GAP-1143) **luôn TEMPLATE** (cố ý — không đốt quota khi xem).
- Nút "Tạo lại" (regenerate) đang 400 (GAP-1145) + 409 trên mock job.
- Deploy honoring mode = **deferred** (Phase-1 deploy = mock provisioning, không chạy `AIBrandingProcessor`).

→ User chọn "AI cao cấp" nhưng không có cách nào thực sự tạo banner FULL_AI để xem/dùng.

## Proposed Fix

Option A (preview FULL_AI on-demand): thêm nút **"Tạo bằng AI cao cấp (tốn 1 lượt)"** → endpoint FULL_AI preview, gate `GenerationMode.forTier` + `FullAiQuotaService.canUseFullAi` (PREMIUM cap / ENTERPRISE ∞), CircuitBreaker fallback TEMPLATE. Trả bannerUrl FULL_AI → đè preview.
Option B (deploy honoring): wire mode end-to-end qua entity→message→`AIBrandingProcessor:96` (`resolveExplicit(mode, tier)`) + real provisioning (Phase 2).

Khuyến nghị: Option A cho Phase 1 (chỉ preview, chưa cần real deploy).

## Acceptance Criteria

- [ ] PREMIUM/ENTERPRISE: chọn FULL_AI + bấm tạo → banner FULL_AI render + hiển thị; quota PREMIUM giảm 1.
- [ ] FREE/BASIC: action FULL_AI khoá (đã có ở selector) — verify không bypass.
- [ ] Hết quota PREMIUM → fallback TEMPLATE + thông báo.

## Fix (PR #2289, 2026-06-10) — Option A (preview on-demand)

- BE `previewBanner` endpoint nhận `mode` (DTO) + header `X-Subscription-Tier`. Khi `mode=FULL_AI`: gate **server-side** = `GenerationMode.forTier(tier)==FULL_AI` (eligibility) + `FullAiQuotaService.canUseFullAi` (PREMIUM cap) → `recordFullAiUsage` (trừ quota) + trả `mode:FULL_AI`; ngược lại fallback `TEMPLATE` + `fallbackReason` (`TIER_NOT_ELIGIBLE` / `QUOTA_EXHAUSTED`). Gate enforce ở BE → FE giả mạo không bypass được.
- FE `useBannerPreview` gửi `mode` + tier header; `Step6Preview` thêm nút "Tạo bằng AI cao cấp (tốn 1 lượt)" (chỉ tier eligible khi FULL_AI selected) + toast theo mode/fallbackReason.
- BE 3 gate tests: PREMIUM+quota→FULL_AI+record; FREE→fallback TIER_NOT_ELIGIBLE (no record); PREMIUM exhausted→fallback QUOTA_EXHAUSTED.
- **AC2** (FREE/BASIC khoá) + **AC3** (hết quota → fallback + thông báo) ✅. **AC1** action+quota ✅ nhưng **render = TEMPLATE mock** vì AI image-gen thật là Phase 2 → còn PARTIAL.
- **Pending:** real GPT image-gen wiring = **GAP-1135** (Phase 2). G2 walk verify quota giảm + fallback.

## Related

- Discovered in: PR #2289 (wave-wizard-step7 G2 walk 2026-06-10)
- GAP-1142 (mode selector) · GAP-1143 (live preview) · GAP-1145 (regenerate 400) · GAP-1137 (FULL_AI tier-gate) · GAP-1135 (real AI image-gen, Phase 2)

## Log

- **2026-06-11:** Design source: `ui_kits/ai-branding-wizard-v2/v3/screens/step3-mode.html` (GAP-1212 DONE 2026-06-11, Wave ui-kits-100 Bucket D) — mode selector TEMPLATE/FULL_AI + tier-gate + PREMIUM quota meter làm 基本設計 layer cho action FULL_AI.
