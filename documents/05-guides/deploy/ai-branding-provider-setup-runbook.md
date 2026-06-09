---
title: AI Branding Provider Setup Runbook — bật generation thật (GAP-1117)
audience: dev
created: 2026-06-10
scope: Hoàn thiện GAP-1117 — provider keys (Gemini + GPT-5.5) + Playwright rasterise + tier propagation
references:
  - documents/02-architecture/adr/ADR-037-ai-branding-generation-stack.md
  - .claude/rules/ai-branding-guidelines.md
  - documents/01-business/kitehub/subscription-billing/rules.md
  - documents/04-quality/gaps/phase-1.5-paid/GAP-1117-wizard-banner-render-step-ai-provider-keys.md
---

# AI Branding Provider Setup Runbook (GAP-1117)

## 1. Mục tiêu + trạng thái hiện tại

Code generation thật đã ship (wave/branding-fix-2026-06-10): `GeminiClient` + `AIBrandingProcessor` (thay mock) + `BannerHtmlComposer` + tier-gate `GenerationMode.forTier` + `FullAiQuotaService` + cost metric `ai.fullai.call`. **Chưa bật vì 2 thứ cần dev cấu hình + 1 đã wire sẵn:**

| Mảng | Trạng thái | Việc cần làm |
|---|---|---|
| **Provider keys** | ⏳ chưa set → MOCK mode | §2 — set `GEMINI_API_KEY` + `OPENAI_API_KEY` |
| **Playwright rasterise** (HTML→WebP) | ⏳ `StubBannerRenderer` trả null → logo/placeholder | §3 — wire `BannerRenderer` thật (@Primary) |
| **Tier propagation** (FULL_AI reachable) | ✅ DONE this PR | `X-Subscription-Tier` → `message.tier` → routing |

> **Không set key vẫn an toàn:** Gemini/OpenAI client chạy MOCK (copy tiếng Việt mẫu + banner = logo/placeholder). Pipeline KHÔNG bao giờ crash — chỉ "không có AI thật". Đây là đúng Phase 1 BETA design.

---

## 2. Provider keys

### 2.1 Lấy key

| Provider | Dùng cho | Lấy ở đâu | Chi phí |
|---|---|---|---|
| **Gemini** (`GEMINI_API_KEY`) | TEMPLATE: copy heroTitle/about/programs + nội dung HTML banner (mọi tier) | [Google AI Studio](https://aistudio.google.com/apikey) → "Create API key" | **Free-tier** (đủ Phase 1) |
| **OpenAI** (`OPENAI_API_KEY`) | FULL_AI: GPT-5.5 image-gen banner (PREMIUM + ENTERPRISE) | [platform.openai.com](https://platform.openai.com/api-keys) → "Create new secret key" | **Pay-per-image** (cap qua `FullAiQuotaService`) |

### 2.2 Local dev

`kitehub-branding` đọc 3 env: `AI_PROVIDER` (`${AI_PROVIDER:openai}`), `GEMINI_API_KEY` (`${GEMINI_API_KEY:}`), `OPENAI_API_KEY` (`${OPENAI_API_KEY:sk-mock-key-for-local-testing}`).

Export trước khi up stack (canonical compose `kitehub/docker-compose.kitehub.yml`):

```bash
export AI_PROVIDER=gemini
export GEMINI_API_KEY="AIza...<your-key>"
export OPENAI_API_KEY="sk-...<your-key>"   # bỏ qua nếu chỉ test TEMPLATE
bash kitehub/scripts/rebuild.sh kitehub-branding   # rebuild để nhận env mới
```

> Chỉ test TEMPLATE (Gemini free): set `AI_PROVIDER=gemini` + `GEMINI_API_KEY`, để `OPENAI_API_KEY` trống → FULL_AI tự fallback TEMPLATE.

### 2.3 Production (AWS Secrets Manager)

`scripts/fetch-secrets.sh` đã wire sẵn (PR này) — fetch 2 secret optional + ghi vào `/etc/kite/.env`:

```bash
# Tạo secret (1 lần, plain string):
aws secretsmanager create-secret --name kitehub/production/gemini-api-key \
  --secret-string "AIza...<key>" --region ap-southeast-1
aws secretsmanager create-secret --name kitehub/production/openai-api-key \
  --secret-string "sk-...<key>" --region ap-southeast-1

# IAM grant: wildcard kitehub/production/* đã cover (per pre-launch-secrets-hardening-checklist §2.3).
# Deploy: fetch-secrets.sh chạy lúc boot → /etc/kite/.env có AI_PROVIDER/GEMINI_API_KEY/OPENAI_API_KEY.
```

Empty secret → `fetch-secrets.sh` log INFO + service chạy MOCK (không fail boot).

---

## 3. Playwright rasterise (HTML → WebP banner)

`BannerHtmlComposer` đã tạo HTML banner 3-lớp (gradient + logo + headline+copy + chân dung + icon chủ đề + CTA) — **deterministic, chữ Việt nét**. Nhưng `StubBannerRenderer` (`render()` → `null`) chưa rasterise → banner image fallback về logo/placeholder. HTML vẫn được persist (`assets.bannerHtml`) để inspect.

### 3.1 Bật rasterise: implement `BannerRenderer` @Primary

Interface `BannerRenderer.render(BannerComposition, UUID) → imageUrl`. 2 lựa chọn (deployment decision):

**Option A — Playwright sidecar (Node microservice) [khuyến nghị]:**
- Reuse `kiteclass-frontend/scripts/compose-sky-demo-banner.mjs` (HTML→headless Chromium→WebP, GAP-810) làm base.
- Dựng service nhỏ: nhận `{html, dims}` → trả WebP bytes → upload MinIO/S3 → trả URL.
- Implement `PlaywrightSidecarBannerRenderer implements BannerRenderer` với `@Primary` + `@ConditionalOnProperty(ai.banner.renderer=playwright)`; POST HTML sang sidecar.

**Option B — JVM Playwright (microsoft/playwright-java):**
- Add dependency `com.microsoft.playwright:playwright` vào `kitehub-branding/pom.xml`.
- Implement `@Primary BannerRenderer`: `Playwright.create() → chromium.launch() → page.setContent(html) → page.screenshot(WebP) → upload S3`.
- Nặng container (Chromium binary ~300MB) — cân nhắc memory cap Free Tier.

Drop bất kỳ impl nào với `@Primary` → activate, **không đụng generation pipeline** (clean seam per `BannerRenderer` javadoc).

> Cho tới khi wire: banner image = logo/placeholder (graceful). TEMPLATE copy (Gemini) vẫn thật khi có `GEMINI_API_KEY`.

---

## 4. Tier propagation (✅ DONE — đã wire PR này)

Để FULL_AI thực sự fire cho PREMIUM/ENTERPRISE:
- `BrandingJobController` đọc `@RequestHeader X-Subscription-Tier` (gateway inject per ADR-039) → `createJob(..., tier)` → `BrandingJobMessage.tier` → `AIBrandingProcessor` route.
- Gateway PHẢI gắn `X-Subscription-Tier` từ subscription tier của tenant (đã có per ADR-039 propagation). Verify gateway forward header tới `/api/platform/branding`.
- Wizard MOCK path (`createWizardJob`) cố tình KHÔNG chạy processor (Phase 1 mock provisioning) — FULL_AI fire ở heavy `createJob` path.

---

## 5. Tier × AI-mode matrix (canonical SUB-22)

| Tier | TEMPLATE (Gemini, $0) | FULL_AI (GPT-5.5, có phí) | regen/ngày |
|---|:---:|:---:|:---:|
| FREE | ✅ | ❌ | 3 |
| BASIC | ✅ | ❌ | 10 |
| PREMIUM | ✅ | ✅ **5 banner/tháng** (`ai.rate-limit.fullai-premium-per-month`) | 30 |
| ENTERPRISE | ✅ | ✅ ∞ | ∞ |

Hết quota FULL_AI (PREMIUM) → fallback TEMPLATE. Tune qua env `AI_FULLAI_PREMIUM_PER_MONTH`.

---

## 6. Verify (sau khi set keys)

```bash
# 1. Confirm env tới container
docker exec kitehub-branding env | grep -E "AI_PROVIDER|GEMINI_API_KEY|OPENAI_API_KEY"

# 2. Trigger generate (heavy path) với tier header
curl -X POST "http://localhost:9000/api/platform/branding" \
  -H "X-Instance-Id: <uuid>" -H "X-Tenant-Id: <tenant>" \
  -H "X-Subscription-Tier: PREMIUM" \
  --data-urlencode "organizationName=Trung tâm Anh ngữ Sky Education" \
  --data-urlencode "logoUrl=http://localhost:9100/...logo.png"

# 3. Grep logs — phân biệt MOCK vs real
docker logs kitehub-branding 2>&1 | grep -E "FULL_AI banner generated|TEMPLATE fallback|MOCK|provider=gemini"

# 4. Cost metric (FULL_AI calls per tier)
curl -s http://localhost:8083/actuator/metrics/ai.fullai.call | jq
```

PASS khi: `provider=gemini` (không phải MOCK), `FULL_AI banner generated` cho PREMIUM (nếu có OpenAI key + quota), metric `ai.fullai.call{tier=PREMIUM,outcome=success}` tăng.

---

## 7. Checklist hoàn thiện GAP-1117

- [ ] §2.2 / §2.3 — set `GEMINI_API_KEY` (local export hoặc AWS secret) → TEMPLATE copy thật
- [ ] §2.3 — set `OPENAI_API_KEY` (PREMIUM/ENTERPRISE FULL_AI banner)
- [ ] §3 — wire `BannerRenderer` @Primary (Playwright sidecar/JVM) → banner image WebP thật
- [x] §4 — tier propagation (DONE PR này)
- [ ] §6 — verify logs + metric sau khi set
