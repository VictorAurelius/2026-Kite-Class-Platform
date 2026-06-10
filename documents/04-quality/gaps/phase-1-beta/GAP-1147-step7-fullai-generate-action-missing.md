# GAP-1147: Step 7 thiếu action tạo banner FULL_AI — mode selector chỉ set state

**Status:** 🔵 OPEN
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

## Related

- Discovered in: PR #2289 (wave-wizard-step7 G2 walk 2026-06-10)
- GAP-1142 (mode selector) · GAP-1143 (live preview) · GAP-1145 (regenerate 400) · GAP-1137 (FULL_AI tier-gate)
