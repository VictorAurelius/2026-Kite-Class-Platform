# Handoff — AI Branding wizard walk (2026-06-10)

**Branch:** `wave/branding-fix-2026-06-10` → PR #2289
**Context at handoff:** 81% (session getting long → fresh session for the enhancement)

## Done this session (all pushed to PR #2289)

| Commit | Fix |
|---|---|
| `f13eb49b` | Gemini model `gemini-1.5-flash` (retired, 404) → `gemini-flash-latest` (free-tier quota, never deprecates). Key `AQ.*` is valid; `gemini-2.0-flash` returns 429 on free tier. |
| `6e6d2221` | Wizard step labels: Portrait inserted as step 3 shifted Audience/Tone/Template → fixed Logo 2/7, Audience 4/7, Tone 5/7, Template 6/7. |
| `1277f730` + `34bebf23` | **Banner WebP sidecar** `kitehub-banner-renderer` (Node+Playwright+sharp) + `PlaywrightBannerRenderer @Primary`. POST /render {html}→WebP (verified 14.7KB). Blank `BANNER_RENDERER_URL` → StubBannerRenderer fallback. `@Autowired` on Spring constructor (two-ctor ambiguity caused crash-loop). |
| `d4d59f5e` | Walk bugs: (1) presigned URL host — `S3_PUBLIC_ENDPOINT=http://localhost:9100` (kite-minio:9000 unreachable from browser); (2) multipart 10MB + gateway codec 12MB (portrait 413); (3) tier pricing BASIC 500k / PREMIUM 1.5M + regen "/ngày" per SUB-22. |

**Stack state:** branding/gateway/FE/banner-renderer all rebuilt + healthy. Gemini real (`mock=false`, gemini-flash-latest). Re-walk to verify #1/#2/#5.

**Local run note:** branding rebuild MUST re-export keys (shell env doesn't persist):
```bash
export AI_PROVIDER=gemini
export GEMINI_API_KEY=$(aws secretsmanager get-secret-value --secret-id kitehub/production/gemini-api-key --query SecretString --output text)
export OPENAI_API_KEY=$(aws secretsmanager get-secret-value --secret-id kitehub/production/openai-api-key --query SecretString --output text)
bash kitehub/scripts/rebuild.sh branding
```

## Next: enhancement — Step 7 mode selector + live banner preview

User asks (2026-06-10): "phải xem được hết ở preview chứ, mà tự động chọn loại AI à, cho user chọn chứ nhỉ".

### Constraint (SUB-22 / ADR-037) — FULL_AI is PAID, tier-gated
- FREE/BASIC: **TEMPLATE only** (FULL_AI locked + upgrade CTA — upsell, not a bug).
- PREMIUM: TEMPLATE (free, unlimited regen) OR FULL_AI (5/month quota).
- ENTERPRISE: TEMPLATE or FULL_AI (unlimited).
- Current auto-pick: `GenerationMode.forTier(tier)` — replace with explicit user choice bounded by eligibility.

### Backend
1. **Preview endpoint** `POST /api/v1/branding/jobs/preview-banner` — body: `{organizationName, copy, logoUrl, portraitUrls, themeIcon, colours}` (FE passes its already-computed deterministic palette + copy) → `BannerHtmlComposer.compose(...)` → `BannerRenderer.render(...)` (sidecar) → `{bannerUrl}`. **No Gemini, no DB, no quota** (TEMPLATE preview). Reuses existing beans. Add `@PreAuthorize` like the other wizard endpoints.
   - `BannerHtmlComposer.compose(orgName, copy, logoUrl, portraitUrls, themeIcon, colours)` — see `service/banner/BannerHtmlComposer.java`. Need `BrandColours` shape.
2. **Deploy respects chosen mode** — `CreateWizardJobRequest` + `BrandingJobV1Controller` accept optional `mode` (TEMPLATE/FULL_AI) → `BrandingJobService.createJob(...)` → `BrandingJobMessage.mode` → `AIBrandingProcessor` uses explicit mode when eligible (FullAiQuotaService already gates FULL_AI), else forTier. Additive.

### Frontend (kitehub-frontend `:3001`)
3. **Mode selector** in `Step6Preview.tsx` (Step 7) — TEMPLATE vs FULL_AI radio/segmented. Tier from `useBrandingTier`. FREE/BASIC → FULL_AI disabled + upgrade CTA. Show FULL_AI remaining quota (N/5) for PREMIUM.
4. **Live banner preview** — on Step 7 mount + on selection change, call preview-banner endpoint → display returned WebP. Use TEMPLATE for preview (don't burn FULL_AI quota); FULL_AI commits only on Deploy (or an explicit "Preview với FULL_AI (tốn 1 lượt)" button).

### Verify
- Re-walk wizard PREMIUM persona → Step 7 shows real banner WebP + mode selector → pick TEMPLATE/FULL_AI → Deploy uses chosen mode.
- FREE persona → FULL_AI locked + CTA.

### Files to read first (next session)
- `kitehub-branding/.../service/banner/BannerHtmlComposer.java` (compose signature + BrandColours)
- `kitehub-branding/.../service/AIBrandingProcessor.java` `generateBanner` (palette + copy derivation to mirror in preview)
- `kitehub-frontend/.../wizard/Step6Preview.tsx` (Step 7 structure + deploy call)
- `kitehub-frontend/.../wizard/RegenerateCounter.tsx` (tier display, already fixed)
