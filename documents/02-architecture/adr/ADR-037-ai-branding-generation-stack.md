# ADR-037: AI Branding Generation Stack — Free-Tier LLM (text/HTML) + GPT 5.5 (banner), Image-Gen Stacks Deferred

**Status:** ACCEPTED
**Date:** 2026-06-01
**Deciders:** @nguyenvankiet (solo-dev, acting CTO + Product Owner)
**Reviewers:** N/A (solo-dev mode per CLAUDE.md decision context locked 2026-05-06)
**Related Gap(s):** GAP-003 (AI branding pipeline); GAP-810 (banner image assets — PARTIAL, template-composer demo); GAP-827 (landing input safety — AI-gen text must sanitize + prompt-constrain); GAP-828 (landing conversion scope)
**Related Rule(s):** `.claude/rules/ai-branding-guidelines.md` (STATIC/TEMPLATE/FULL_AI taxonomy); `.claude/rules/thesis-as-future-state-mandate.md` (thesis AI-branding claims = Phase 1.5 goal)
**Related ADR(s):** ADR-026 (Defer Ollama/FULL_AI self-host to Phase 2) — this ADR picks the Phase 1 generation route ADR-026 left open; ADR-010 (content moderation)
**Supersedes:** implicit "image-gen stack" exploration (MiniMax / Stable Diffusion / DALL-E / Midjourney / Flux etc.) listed in early planning — all DEFERRED

---

## Context

ADR-026 đã defer Ollama self-host FULL_AI inference sang Phase 2 (Phase 1 BETA = template-only). Câu hỏi còn mở: **Phase 1 AI branding tạo nội dung (text/HTML copy + banner image) bằng route nào?** Early planning từng liệt kê nhiều image-gen stack (MiniMax, Stable Diffusion, DALL-E, Midjourney, Flux, ComfyUI, Replicate). Session handoff 2026-05-29/30 (GAP-810) chứng minh template-composer (HTML→PNG via Playwright) tốt hơn AI image-gen cho asset có chữ Việt (text crisp, deterministic).

Quyết định 2026-06-01 chốt route Phase 1:

| Output | Route chốt | Lý do |
|---|---|---|
| **Text + HTML copy** (heroTitle/subtitle/about/programs draft) | **Free-tier LLM** (cloud free quota) | Đủ cho draft copy ngắn; $0 recurring; không cần self-host GPU (ADR-026 đã defer Ollama) |
| **Banner image** | **GPT 5.5** (image gen) | Chất lượng cao, 1 banner/tenant ít lượt → cost kiểm soát; vượt template-composer cho ảnh nền phức tạp |
| **Other image-gen stacks** (MiniMax/SD/DALL-E/Midjourney/Flux/ComfyUI/Replicate) | **DEFERRED** | Không cần — GPT 5.5 cover banner; tránh multi-vendor sprawl |

## Decision

1. **Text/HTML:** free-tier LLM cho draft copy. AI prompt PHẢI constraint (per GAP-827 + GAP-828): length cap (heroTitle ≤45 ký tự...), plain-text (no Markdown), **cấm bịa số liệu/testimonial giả** (Luật Quảng cáo VN + ADR-010 moderation).
2. **Banner:** GPT 5.5 image gen, 1 banner active/tenant (multi-banner carousel = Phase 1.5 GAP-826).
3. **Image-gen stacks khác:** DEFERRED — không adopt Phase 1; re-evaluate Phase 2 nếu volume/cost đổi.
4. AI-gen output đi qua **sanitize-on-write** (GAP-827) — AI text không được bypass safety.

## Consequences

**Positive:** $0-low recurring (free-tier + GPT pay-per-banner ít lượt); không multi-vendor sprawl; template-composer (GAP-810) vẫn là fallback cho banner chữ-Việt nếu GPT cost/quality không đạt.

**Negative / track:**
- **PDPL data-flow:** text + banner prompt gửi ra cloud (free-tier LLM + OpenAI) → data rời VN. ADR-026 đã flag. Tenant content (tên trung tâm, GV) là low-sensitivity nhưng phải ghi trong privacy policy + cân nhắc opt-in. **Follow-up: PDPL review AI data-flow.**
- **Free-tier quota limit:** rate/quota cap → cần fallback (template-composer) khi hết quota.
- **GPT 5.5 banner cost:** monitor cost/banner; cap regen quota per tenant (GAP-005 fair-scheduling).

## Doc-sync (§2.7 — decision lands → sweep stale refs)

Decision này supersede image-gen-stack exploration. Stale refs cần reconcile (sweep 2026-06-01 thấy ~30 doc hits, nhiều false-positive). Canonical AI-branding docs cần update prospectively (KHÔNG mass-edit ad-hoc — update khi chạm):
- `.claude/rules/ai-branding-guidelines.md` — taxonomy + route reference → cite ADR-037
- GAP-003 / GAP-810 — link ADR-037 làm route-of-record
- Thesis `chapter-1-ai-techniques.md` — per `thesis-as-future-state-mandate.md`: thesis AI claim = Phase 1.5 goal; KHÔNG downgrade wording, ADR-037 là delivery commitment
- Wave plans cũ liệt kê image-gen stack → grandfathered (historical); rule applies prospectively

Sweep stale image-gen-stack refs = opportunistic (khi chạm doc) per `no-vercel-references.md` precedent (prospective sweep, historical grandfathered). Không block.
