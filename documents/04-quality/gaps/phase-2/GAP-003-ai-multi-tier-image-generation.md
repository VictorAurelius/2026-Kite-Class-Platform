# GAP-003: Multi-tier image generation strategy

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** AI / Backend
**Detected:** 2026-04-14
**Related Docs:**
- `documents/03-planning/implementation/ai-local-implementation-plan.md`

## Problem

Design hiện tại chỉ plan 1 model cho image generation (Stable Diffusion XL). Không có strategy multi-tier cho use cases khác nhau: quick preview vs high-quality render. Tất cả user đều chờ cùng latency.

## Context

- SDXL full: ~5 phút CPU, ~30s GPU (best quality)
- SDXL Turbo / LCM LoRA: ~5-10s CPU, ~2s GPU (70-80% quality)
- Different user tiers có expectations khác nhau:
  - FREE/BASIC: chờ lâu OK nếu chất lượng OK
  - PREMIUM/ENTERPRISE: cần nhanh + chất lượng cao

## Evidence

- `ai-local-implementation-plan.md` table line 59-65: chỉ list 1 model SD per feature
- PR-AI-2 scope không mention multi-tier

## Proposed Fix

**PR-AI-6: Multi-tier Image Generation**

| Tier | Model | Latency CPU | Latency GPU | Quality | Availability |
|------|-------|-------------|-------------|---------|--------------|
| **Fast** | SDXL Turbo / LCM | ~5-10s | ~1-2s | 75% | Tất cả users |
| **Standard** | SDXL base | ~2-3 min | ~15-30s | 85% | BASIC+ |
| **Premium** | SDXL + refiner | ~5 min | ~30-60s | 95% | PREMIUM+ |

**Routing logic:**
```java
public Tier selectTier(User user, ImageRequest req) {
  if (req.previewMode) return Tier.FAST;
  if (user.tier == ENTERPRISE || user.tier == PREMIUM) return Tier.PREMIUM;
  if (user.tier == BASIC) return Tier.STANDARD;
  return Tier.FAST;
}
```

**Combine với GAP-002 (async pipeline):**
- Fast tier: có thể sync (5-10s OK)
- Standard/Premium: async via queue

## Acceptance Criteria

- [ ] 3 model tiers loaded trong Ollama/ComfyUI
- [ ] Tier routing dựa trên user subscription + request type
- [ ] Latency benchmark documented
- [ ] Quality comparison screenshots 3 tiers
- [ ] Cost analysis updated

## Dependencies

- Blocked by **GAP-002** (async pipeline) nếu muốn Standard/Premium tier UX tốt
- Related to PR-AI-2 (base image generation — phải done trước)

## Log

- 2026-04-14 — Phát hiện khi review AI design
