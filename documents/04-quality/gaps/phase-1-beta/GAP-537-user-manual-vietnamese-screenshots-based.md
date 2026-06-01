# GAP-537: User manual Vietnamese — screenshots-based, per-persona

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 — required cho Phase 1 BETA invite (manual sent alongside invite email OR linked in dashboard)
**Domain:** Mixed (FE screenshots + DevOps publish + content writing)
**Found:** 2026-05-14 (Wave 78 planning — inside-out + dependency chain analysis)
**Affects:** All beta personas (P1 Solo teacher + P2 Center owner) — no manual = beta tenants stuck after first login
**Phase:** phase-1-beta

## Current State (verified 2026-05-14)

| Piece | File / Path | Status |
|-------|-------------|--------|
| User manual (any form) | `documents/05-guides/` | ❌ missing — `grep -rliE "user.?manual\|hướng dẫn người dùng" documents/05-guides/` → 0 |
| Screenshots automation | `scripts/capture-screenshots.ts` | 🟡 verify-at-spawn — may exist for design system Round 2/3 |
| UI kit stability gates | GAP-348 + GAP-364 + GAP-428 | 🟡 PARTIAL — kits còn polish |
| Vercel preview deployment | (FE deploy) | 🟡 verify-at-spawn — may be active for screenshot source |

**Grep commands run:**
```bash
grep -rliE "user.?manual|hướng dẫn người dùng" documents/05-guides/
# → 0 results
grep -E "manual|hướng dẫn" documents/04-quality/gaps/gap-status.csv | head
# → 0 results matching scope
find scripts -iname "*capture*" 2>/dev/null
# → verify-at-spawn (Wave 79)
```

## Problem

Phase 1 BETA invite-only launch (5-20 tenants Linear cohort playbook). Sau khi tenant signup + provision, không có tài liệu hướng dẫn → P1 Solo teacher + P2 Center owner phải mò mẫm UI → bounce hoặc support burden.

**Dependency chain blocking immediate execution:**
1. Screenshots cần FE stable
2. FE stable cần UI kits đóng (GAP-348 + GAP-364 + GAP-428 PARTIAL)
3. UI kits đóng = Wave 78 (Prospects/GAP-428) + Wave 79 (kit polish residue)
→ Manual draft viable only **after Wave 79 ships**

## Proposed Fix

**Phase 1 (Wave 79 — screenshot capture)** sau khi UI kit polish done:
1. Automation script `scripts/capture-screenshots-manual.ts`:
   - Headless Playwright → screenshot 30-50 screens cần thiết
   - 2 persona flows: P1 (Solo teacher) + P2 (Center owner)
   - Output: `documents/05-guides/user-manual/screenshots/{persona}/{flow}/{step-N}.png`
2. Screenshot inventory checklist (~30 screens per persona)

**Phase 2 (Wave 79 hoặc 80 — manual content draft)**:
3. Manual structure (Vietnamese narrative per `dev-readable-doc-language.md`):
   - `documents/05-guides/user-manual/README.md` — index
   - `documents/05-guides/user-manual/p1-solo-teacher.md` — 5-7 chapters
   - `documents/05-guides/user-manual/p2-center-owner.md` — 5-7 chapters
   - `documents/05-guides/user-manual/common/{topic}.md` — shared chapters
4. Chapter template:
   - "Bạn muốn làm gì?" intro
   - Step-by-step với screenshot
   - "Khi nào có lỗi?" troubleshooting
   - Link to support channel
5. PDF render (optional) `scripts/render-manual-pdf.sh` for offline use

**Phase 3 (Wave 80 — publish + integrate)**:
6. Manual link surface trong:
   - Welcome email (post-signup)
   - Dashboard footer/help menu
   - Beta invite email
7. Update + maintenance schedule documented

## Acceptance Criteria

- [ ] **Phase 1:** Screenshot automation script runnable + 30-50 screens captured per persona
- [ ] **Phase 2:** Manual chapters Vietnamese cho 2 persona Tier 1; index ships dưới `documents/05-guides/user-manual/`
- [ ] **Phase 2:** Manual cover top 10 happy-path use cases per persona + top 5 troubleshooting
- [ ] **Phase 3:** Manual link present trong welcome email + dashboard footer + beta invite email
- [ ] Optional PDF render available (defer to Wave 80 if Phase 2 đủ Markdown)

## Related

- **Blocks on:** GAP-348 + GAP-364 + GAP-428 (UI kits stability — Wave 78/79)
- **Wave queue:** Wave 79 (Phase 1+2) + Wave 80 (Phase 3 publish)
- **Sister Wave 78 outside-in:** GAP-538 (onboarding+sample data — first-touch when no manual yet), GAP-540 (support channel — fallback when manual fails)
- **Dependency:** `dev-readable-doc-language.md` — Vietnamese narrative required cho customer-facing scope

## Log

- 2026-06-01 — **Wave meta-8 Bucket B SCOPE-REVISE:** SCOPE-REVISE — Status header says OPEN but completion 75; Wave 80 Bucket D Phase 2 F2 15 sources + PDF gen + Playwright capture + F1+Admin screenshots shipped (3/5 AC if AC restructured); P2/P3 screenshots placeholder GAP-537c CSV completion_pct adjusted to 60%; gap body Status/AC reflect documented scope BEFORE Wave meta-7 audit — re-read audit artifact for current empirical reality. Source: `documents/04-quality/audits/meta/2026-06-01-wave-meta-7-bucket-d-p1-partial.md`.

- **2026-05-14:** PARTIAL 25% — Wave 79 Bucket F1 shipped anonymous-prospect 5-page MDX sample (`index/pricing/beta-access/terms/faq`) at `/help/anonymous/[slug]` route via PR #1371. F2 (P2 Owner + P3 Manager + Platform Admin) deferred Wave 80+ gated on F1 dev review. Inside-out scope refined per `user-manual-content-standard.md` v1.0.0 (sister rule shipped same wave Bucket F1).

- **2026-05-14** — Initial write-up. User-flagged Wave 78 planning convo (2026-05-14): manual depends on UI kit chốt → queued post-Wave 79. Stub created tách rời Wave 78 RETAIN scope.

- **2026-05-15:** PARTIAL 25% → 75% — Wave 80 Bucket D shipped 15 F2 source pages (5 P2 Owner + 5 P3 Manager + 5 Platform Admin) + `scripts/render-user-manual-pdf.{sh,mjs}` (Puppeteer A4 portrait) + `scripts/capture-user-manual-screenshots.{sh,mjs}` (Playwright vi-VN 1440×900) + Next.js routes `/help/{p2-owner,p3-manager,platform-admin}/[slug]` + 10/20 actual screenshot PNGs (F1 anonymous 5 + F2 Platform Admin 5). P2 Owner + P3 Manager screenshots + Tier 2 annotation overlay deferred → GAP-537c P1 Wave 81 follow-up (PR #1382).
