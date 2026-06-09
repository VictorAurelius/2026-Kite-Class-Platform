# GAP-1117: Wire bước render banner thật (template-compose 3 lớp) + cấu hình AI provider API KEY (free template + full-AI ChatGPT-5.5)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Mixed
**Found:** 2026-06-10 (discuss wizard 6-bước với user — design critique câu 3 + user-flagged dev config API KEY)
**Affects:** `kitehub-branding` generation pipeline (`ResourceRoutingService` / `AIClient` strategy / `BrandingJobService` / banner-compose) + `kitehub-frontend` wizard Step5/Step6 + dev env config (provider API keys)

## Problem

Bước 5 (`TemplateStep`) cho chọn template, NHƯNG **bước render banner thật** (compose 3 lớp: text + chân dung + icon theo template đã chọn) **chưa wire vào generation** của wizard:

- Phase 1 generation hiện **mock** (per memory `project_ai_branding_generation_model`: "code hiện=mock, no real DNS/AI").
- Banner compose deterministic (HTML→WebP qua `compose-sky-demo-banner.mjs` / Playwright) hiện chỉ dùng cho demo/thesis, **chưa nối** vào flow wizard runtime.
- Thiếu cả 2 chế độ render mà `ResourceCategory` định nghĩa: **TEMPLATE** (free, template-compose / local Ollama) và **FULL_AI** (`ResourceCategory.FULL_AI`).

User mental model đúng hơn: audience → user-type → chân dung → **render banner (template hoặc full-AI)** → preview.

## Dev config prerequisite (user-flagged 2026-06-10)

Để render THẬT (bỏ mock), dev PHẢI cấu hình **API KEY cho 2 provider** (per `AIClient` Strategy + ADR-037-ai-branding-generation-stack):

| Chế độ | Provider | Cấu hình |
|---|---|---|
| **AI template (free)** | Local Ollama / template-compose (deterministic) | Local Ollama endpoint (no external key) HOẶC template-only path (no key) |
| **Full AI** | **OpenAI ChatGPT-5.5** (user-specified) | `OPENAI_API_KEY` env / Secrets Manager (`kitehub/<env>/openai-api-key`); model id = ChatGPT-5.5 |

Tier-gate: full-AI chỉ mở cho **ENTERPRISE Advanced Mode** (per `ai-branding-guidelines.md` §2.4); FREE/BASIC/PREMIUM = template-first (§1 ResourceCategory). Per `ai-branding-guidelines.md` §2.5 input cap vẫn áp dụng mọi callsite.

## Proposed Fix

1. **Wire render thật:** `ResourceRoutingService.classify()` → TEMPLATE path gọi banner-compose thật (logo + portrait [[GAP-1116]] + icon theo template) thay vì mock; surface kết quả vào Step6 preview ([[GAP-1118]]).
2. **AIClient strategy switch:** template (free) vs full-AI (ChatGPT-5.5) theo tier (`X-Subscription-Tier` per ADR-039); full-AI gated ENTERPRISE.
3. **Provider config:** cấu hình `OPENAI_API_KEY` (full-AI) + local Ollama/template (free); document trong deploy runbook + Secrets Manager. **Lưu ý dev: đây là prerequisite — không có key thì full-AI fallback template.**
4. Quality gate (per §5) chạy trước DEPLOY.

## Acceptance Criteria

- [ ] Render banner THẬT cho TEMPLATE path (không mock) — compose logo+portrait+icon theo template
- [ ] AI provider keys configured: template-free (Ollama/local) + full-AI ChatGPT-5.5 (`OPENAI_API_KEY`); documented trong deploy runbook
- [ ] Full-AI gated ENTERPRISE Advanced Mode; FREE/BASIC/PREMIUM = template-first; input-cap §2.5 enforced
- [ ] Render result surface vào Step6 preview ([[GAP-1118]])
- [ ] Quality gate (§5) trước DEPLOY; CircuitBreaker fallback template khi provider fail
- [ ] IT / migration test checklist §11.4 (AI behavior change)

## Related

- Discovered in: discuss wizard 6-bước 2026-06-10 (user critique câu 3 + dev API KEY config flag)
- Depends: [[GAP-1116]] (portrait asset đầu vào)
- Feeds: [[GAP-1118]] (preview render result)
- Cluster: [[GAP-1115]] user-type axis
- Design: `ADR-037-ai-branding-generation-stack.md`, `ai-branding-guidelines.md` §1 (ResourceCategory) / §2.4 (Enterprise full-prompt) / §2.5 (input cap) / §5 (quality gate) / §11.4 (migration test)
- Generation model: memory `project_ai_branding_generation_model` (Phase 1 = TEMPLATE-first by design, code=mock)

## Log

- **2026-06-10:** Filed từ discuss wizard với user — bước render banner (template/full-AI) chưa wire vào flow, generation hiện mock. User-flagged: dev phải cấu hình API KEY (AI template free + full-AI ChatGPT-5.5). Per `discovery-to-gap-inline-filing.md`. GAP-ID từ block reserve 1115-1118.
