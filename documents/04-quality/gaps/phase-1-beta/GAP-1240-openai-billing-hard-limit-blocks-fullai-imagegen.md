# GAP-1240: OpenAI billing hard limit chặn FULL_AI image-gen thật — cần user nạp credit

**Status:** 🟣 PENDING (external — user action: nạp billing OpenAI project)
**Priority:** 🟠 P1
**Domain:** DevOps
**Found:** 2026-06-12 (G1 browser walk wave branding-100 — Bug #6 root-cause chain)
**Affects:** FULL_AI banner generation (wave branding-100 gate "luồng full-ai e2e thật")

## Problem

G1 walk FULL_AI: click "Tạo bằng AI cao cấp" → OpenAI trả lỗi 2 lớp (verify bằng curl
key thật, không in key):

1. `dall-e-3` → 400 `"The model 'dall-e-3' does not exist"` — OpenAI deprecated cho
   project keys mới → **fixed**: model default `gpt-image-1` + size `1536x1024` +
   parse `b64_json` → persist MinIO (PR wave/branding-100-g1-fixes).
2. `gpt-image-1` → 400 `"Billing hard limit has been reached"` — project của key
   `kitehub/production/openai-api-key` (SM, tạo 2026-06-09) **hết hạn mức billing**.

Lớp 2 là external blocker: code đúng nhưng không thể sinh ảnh thật cho tới khi user
nâng billing limit / nạp credit OpenAI project.

**Bug đi kèm đã fix cùng PR (GAP-1218 class):** `ResilientAIClient.generateImage`
fallback nuốt lỗi → trả `placehold.co` → controller tưởng thành công → **trừ quota +
toast "đã trừ 1 lượt"** trên banner placeholder. Fixed: `generateImageStrict` (no
fallback) cho FULL_AI path → GENERATION_FAILED + KHÔNG trừ quota.

## Acceptance Criteria

- [ ] User nạp billing / nâng hard limit OpenAI project của key SM
- [ ] Re-walk FULL_AI: click generate → ảnh gpt-image-1 thật (MinIO URL, không placehold.co) + quota trừ đúng 1
- [ ] Sad path verify: khi provider lỗi → toast GENERATION_FAILED + quota KHÔNG trừ

## Related

- Discovered in: G1 walk wave branding-100 2026-06-12
- GAP-1218 (FULL_AI quota consumer-trust guard — strict path fix cùng PR)
- GAP-1135 (FULL_AI image-gen wire thật #2362)
