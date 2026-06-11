---
wave: wave-wizard-step7-1
tag_primary: wizard-step7
tags_secondary: [ai-branding, pr-2289]
created: 2026-06-10
status: in-progress
base_branch: wave/branding-fix-2026-06-10 (PR #2289, HEAD acf8c648)
gap_block: GAP-1140..1144
session: session-20260610-034209
---

# Wave Wizard Step 7 — mode selector + live banner preview + asset reuse + landing-100 preview

## Mục tiêu (user direction 2026-06-10)

Enhancement Bước 7 (Step6Preview) của AI Branding wizard (PR #2289):
1. **Cho user chọn loại AI** — mode selector tier-gated (SUB-22 / ADR-037): TEMPLATE (◉ Mẫu, free, regen ∞) vs FULL_AI (○ AI cao cấp). FREE/BASIC khoá FULL_AI + CTA upgrade; PREMIUM còn N/quota; ENTERPRISE ∞. `GenerationMode.forTier` hiện auto-chọn → thay bằng explicit choice bounded by eligibility.
2. **Live banner preview** — render banner WebP thật (sidecar compose HTML → Playwright → WebP). Preview dùng TEMPLATE để không tốn quota; FULL_AI chỉ commit khi Deploy.
3. **Chọn banner từ asset cũ** — asset library reuse (`useAssets`).
4. **Preview = landing chuẩn wave-landing-100** — thay `buildPreviewHtml` ad-hoc bằng landing-100 standard structure, nhúng live banner làm hero.

## Brainstorm / risk

- **Coupling point = `Step6Preview.tsx`** (orchestrator). 4 buckets KHÔNG được cùng sửa file này → mỗi bucket build module/file MỚI standalone; **integration vào Step6Preview = coordinator** (sau khi 4 bucket land).
- BE preview endpoint là contract chung A↔C — chốt shape trước (xem §Contract).
- Base = my branch HEAD `acf8c648` (BannerHtmlComposer/GenerationMode chưa lên main; chỉ ở PR #2289).
- Multi-session: session 031148 (PR #2279 branding G2) chạy song song — không đụng wizard files (verified). Gap block disjoint 1140..1149 vs 1106..1112.

## Buckets (disjoint files, worktree off acf8c648, Opus agents)

| Bucket | Gap | Worktree/branch | Scope (NEW/own files only) |
|---|---|---|---|
| **A — BE** | 1141 | wave/wizard-step7-a-be | `kitehub-branding`: `POST /api/v1/branding/jobs/preview-banner` + `PreviewBannerRequest` DTO + deploy honors `mode` (CreateWizardJobRequest+11 field, thread to AIBrandingProcessor, no Flyway). |
| **B — FE** | 1142 | wave/wizard-step7-b-mode | NEW `GenerationModeSelector.tsx` (tier-gated) + test. KHÔNG sửa Step6Preview. |
| **C — FE** | 1143 | wave/wizard-step7-c-prev | NEW `hooks/useBannerPreview.ts` + `BannerLivePreview.tsx` + `AssetReusePicker.tsx` + tests. KHÔNG sửa Step6Preview. |
| **D — FE** | 1144 | wave/wizard-step7-d-land | NEW `buildLandingPreviewHtml.ts` (landing-100 standard srcDoc, nhúng bannerUrl hero) + test. KHÔNG sửa Step6Preview. |
| **Integration** | 1140 | coordinator (main tree) | Wire B/C/D + endpoint A vào Step6Preview.tsx, swap buildPreviewHtml→buildLandingPreviewHtml, `pnpm --filter kitehub-frontend build`, walk. |

## Contract — preview-banner endpoint (A ↔ C)

```
POST /api/v1/branding/jobs/preview-banner       @PreAuthorize(WRITE_AUTHZ owner-tier)
Request: { organizationName, copy?, logoUrl?, portraitUrls?: string[], themeIcon?,
           colours: { primary, secondary, accent, neutral, background } }   // 5 hex #RRGGBB
Response: { bannerUrl: string | null, mode: "TEMPLATE" }                     // null when stub renderer
No Gemini · No DB write · No quota consumption (TEMPLATE preview only).
```

## Verify (post-integration, coordinator)
- `pnpm --filter kitehub-frontend build` PASS (per `fe-build-local-verify.md`).
- Walk PREMIUM persona Step 7 → banner WebP thật + mode selector → pick TEMPLATE/FULL_AI → Deploy uses chosen mode (per `feature-ship-runtime-walk-mandate.md`).
- Walk FREE persona → FULL_AI khoá + CTA. Preview = landing-100 structure.
