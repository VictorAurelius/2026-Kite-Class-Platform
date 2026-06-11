# GAP-1135: Wire render banner thật — TEMPLATE (HTML+Gemini→Playwright) + FULL_AI (GPT-5.5 image) + cấu hình provider key (per ADR-037 amendment 2026-06-10)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Mixed
**Found:** 2026-06-10 (discuss wizard 6-bước — design critique câu 3 + user-flagged provider + ADR-037 amendment)
**Affects:** `kitehub-branding` generation pipeline (`AIBrandingProcessor` mock / `AIClient` strategy / `ResourceRoutingService` / banner-compose runtime) + `kitehub-frontend` wizard Step5/Step6 + dev env config (provider keys)

## Problem

Bước 5 (`TemplateStep`) cho chọn template, NHƯNG **bước render banner thật chưa wire vào generation**:

- `AIBrandingProcessor.processJob()` hiện = **MOCK 100%**: mọi asset (hero/profile/facebookCover/youtubeBanner/ogImage) đều `= message.getLogoUrl()` + `simulateProcessing()`. **Không gọi `aiClient` nào.**
- `AIClient` interface CÓ `generateText()`/`generateImage()`/`analyzeLogo()` nhưng chỉ có `OllamaClient` + `OpenAIClient` (**không có GeminiClient**).
- Banner compose deterministic (`kiteclass-frontend/scripts/compose-sky-demo-banner.mjs`, HTML→Playwright→WebP, GAP-810) hiện chỉ là demo-seed script, **chưa wire vào kitehub-branding runtime**.

## Quyết định provider (ADR-037 amendment 2026-06-10 — user-pinned)

| Output | Mode | Route | Dev key |
|---|---|---|---|
| **Text/HTML copy** (heroTitle/about/programs) | free | **Gemini free-tier** | `GEMINI_API_KEY` |
| **Banner — TEMPLATE** (default, FREE) | free | **HTML template (không cố định) + Gemini render nội dung → Playwright chụp WebP** — chữ Việt nét, $0 | `GEMINI_API_KEY` |
| **Banner — FULL_AI** (ENTERPRISE) | paid | **GPT-5.5 image-gen** render cả ảnh, prompt chuẩn mẫu thesis | `OPENAI_API_KEY` |

Tier-gate: FULL_AI (GPT-5.5) chỉ cho **ENTERPRISE Advanced Mode** (`ai-branding-guidelines.md` §2.4); FREE/BASIC/PREMIUM = TEMPLATE (HTML+Gemini→Playwright). Input-cap §2.5 mọi callsite. **Dev lưu ý: chưa set key thì fallback copy template tĩnh / banner template không-Gemini.**

## Proposed Fix

1. **Thêm `GeminiClient`** implement `AIClient` (text gen cho copy + drive HTML template content) — strategy switch theo `ai.provider` + tier.
2. **Wire HTML-template-compose runtime:** port `compose-sky-demo-banner.mjs` → BE render path (Playwright headless trong service OR sidecar), nhận logo + chân dung ([[GAP-1134]]) + icon chủ đề + màu brand + copy (Gemini) → WebP. Thay mock trong `AIBrandingProcessor`.
3. **FULL_AI path:** `OpenAIClient.generateImage` (GPT-5.5) cho ENTERPRISE — prompt builder chuẩn mẫu thesis; CircuitBreaker fallback → TEMPLATE khi fail/hết quota.
4. **Provider config:** `GEMINI_API_KEY` + `OPENAI_API_KEY` → env/Secrets Manager; document deploy runbook.
5. Render result surface vào Step6 preview ([[GAP-1136]]); quality gate §5 trước DEPLOY.

## Acceptance Criteria

- [ ] `GeminiClient` (AIClient strategy) — text/copy gen Gemini free-tier; `GEMINI_API_KEY` config
- [ ] TEMPLATE banner render THẬT: HTML template + Gemini copy → Playwright→WebP (thay mock `AIBrandingProcessor`); chữ Việt nét; logo+chân dung+icon
- [ ] FULL_AI banner: GPT-5.5 image-gen gated ENTERPRISE; prompt chuẩn thesis; CircuitBreaker fallback TEMPLATE
- [ ] Provider keys configured + documented (`GEMINI_API_KEY` + `OPENAI_API_KEY`); chưa có key → fallback graceful
- [ ] Render result surface Step6 preview ([[GAP-1136]]); quality gate §5; input-cap §2.5
- [ ] IT / migration test checklist §11.4 (AI behavior change)

## Related

- Discovered in: discuss wizard 6-bước 2026-06-10 (câu 3 + provider decision)
- **Design pinned:** `ADR-037-ai-branding-generation-stack.md` §Amendment 2026-06-10
- Depends: [[GAP-1134]] (portrait asset đầu vào) ; Feeds: [[GAP-1136]] (preview)
- Cluster: [[GAP-1133]] user-type axis
- Banner reference: `kiteclass-frontend/scripts/compose-sky-demo-banner.mjs` (GAP-810), thesis banner 3-lớp
- Guidelines: `ai-branding-guidelines.md` §1 ResourceCategory / §2.4 Enterprise / §2.5 input-cap / §5 quality gate / §11.4 migration test
- Generation model: memory `project_ai_branding_generation_model` (Phase 1 mock)

## Log

- **2026-06-11:** Design source: `ui_kits/ai-branding-wizard-v2/v3/screens/step8-banner-{generating,failed,ready}.html` (GAP-1212 DONE 2026-06-11, Wave ui-kits-100 Bucket D) — 3 trạng thái banner GENERATING/FAILED/READY + SSE log làm 基本設計 layer.
- **2026-06-10 (PARTIAL 85%):** Tier-propagation + provider provisioning + setup runbook shipped (wave/branding-fix-2026-06-10). `BrandingJobService.createJob` overload + `BrandingJobController` đọc `X-Subscription-Tier` (ADR-039) → `BrandingJobMessage.tier` → FULL_AI reachable cho PREMIUM/ENTERPRISE (2 test tier-propagation + verify message.getTier). `scripts/fetch-secrets.sh` thêm `gemini-api-key`/`openai-api-key` optional fetch → `/etc/kite/.env` (`AI_PROVIDER`/`GEMINI_API_KEY`/`OPENAI_API_KEY`; empty → MOCK, không fail boot). `application.yml` `ai.gemini.api-key=${GEMINI_API_KEY:}` + `openai.api-key`. **Hướng dẫn cụ thể hoàn thiện:** `documents/05-guides/deploy/ai-branding-provider-setup-runbook.md` (§2 keys local+prod, §3 Playwright BannerRenderer @Primary, §6 verify). Surefire 318 green. **Remaining (dev/infra action, KHÔNG phải code):** (1) set `GEMINI_API_KEY` + `OPENAI_API_KEY` per runbook §2; (2) wire `BannerRenderer` @Primary (Playwright sidecar/JVM) cho HTML→WebP rasterise per runbook §3. Tới đó banner image = logo/placeholder; TEMPLATE copy = MOCK Vietnamese sample.
- **2026-06-10:** Filed + refined per ADR-037 amendment: TEMPLATE = HTML+Gemini→Playwright (free default), FULL_AI = GPT-5.5 image (Enterprise), text-LLM = Gemini free-tier. Cần thêm GeminiClient + wire compose runtime. Per `discovery-to-gap-inline-filing.md`. GAP-ID block reserve 1115-1118.
