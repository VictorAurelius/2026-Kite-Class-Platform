---
id: GAP-827
title: Landing input safety — sanitize-on-write + heroImageUrl allowlist + JSONB sanitize + length cap
status: OPEN
priority: P0
phase: phase-1-beta
domain: Backend
created: 2026-06-01
---

# GAP-827 — Landing input safety (sanitize + validate write-path)

> Surfaced bởi outside-in failure-mode audit (2026-06-01, 1/3 agent landing-input state-check). Write-path nhận tenant input (text + banner) hiện **0 sanitization** + heroImageUrl **0 scheme/host allowlist** → P0 stored-XSS + injection risk. Phải đóng TRƯỚC khi mở editor self-service (GAP-815) cho tenant nhập.

## Problem

`UpdateLandingPageRequest` → `LandingPageMapper.updateEntity` (MapStruct copy thẳng, không sanitize) → entity. Findings empirical (failure-mode agent):

- **P0 sanitize-on-write absent:** text fields lưu raw. React JSX auto-escape che XSS ở render `.tsx`, NHƯNG text được reuse ở surface KHÔNG auto-escape: `JsonLd.tsx` (`dangerouslySetInnerHTML`!), email template, PDF cert, `<meta>` tag → stored-XSS rò. Có `SvgSanitizer` trong codebase nhưng KHÔNG wire vào landing write-path.
- **P0 heroImageUrl scheme/host:** chấp nhận URL bất kỳ → `javascript:` / off-origin `.svg` chứa script / `.html` đổi đuôi `.png`.
- **P0 JSONB raw:** `teachers/testimonials/faqs/programs/pricingTiers/stats` = `JsonNode` thô, 0 validation nội dung — lỗ hổng lớn nhất; phải audit mọi FE render JSONB có `dangerouslySetInnerHTML`.
- **P1 unbounded length:** `teacherBio` / `aboutText` / `address` KHÔNG có `@Size` → DB/cache bloat (endpoint public-cached!).
- **P1 CSS resilience:** token đơn dài (200 ký tự không space) vỡ hero dù có `text-wrap:balance`; banner sai tỉ lệ méo.

## Acceptance Criteria

- [ ] **Sanitize-on-write** mọi text field: Jsoup `Safelist.none()` trong service/mapper (defense-in-depth, không tin render-side)
- [ ] **heroImageUrl `@Pattern`** chỉ `^https://<storage-host>/...` + host allowlist (MinIO/CDN nội bộ); chặn off-origin + non-https
- [ ] **JSONB string values** sanitize + audit mọi FE `dangerouslySetInnerHTML` (bắt đầu `JsonLd.tsx`)
- [ ] **`@Size` cap** cho `teacherBio` / `aboutText` / `address` + heroTitle/subtitle (budget hợp lý — xem GAP-828)
- [ ] **CSS:** `overflow-wrap:anywhere` cho h1 + `aspect-ratio` cố định + scrim cho banner
- [ ] **NFC normalize** dấu tiếng Việt on-write
- [ ] Cross-flow sweep per `cross-flow-bug-class-sweep.md`: mọi entry-point nhận tenant text (không chỉ landing) — sanitize đồng nhất
- [ ] IT verify: XSS payload ở mỗi field → escaped/stripped ở MỌI render surface (tsx + JsonLd + email + PDF)

## Related

- Outside-in failure-mode audit 2026-06-01 (3-agent landing-input state-check)
- `cross-flow-bug-class-sweep.md` — sanitize = sister bug class; sweep all tenant-text entry points
- GAP-815 (editor UI) — safety-gate phải đóng TRƯỚC khi mở self-service input
- GAP-826 (multi-banner) + GAP-828 (conversion scope) — sister landing-input gaps
- ADR-037 (AI branding stack) — AI-gen text cũng phải qua sanitize + prompt constraint (no fabricated stats)
