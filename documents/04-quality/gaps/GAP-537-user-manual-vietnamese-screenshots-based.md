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

- **2026-05-14** — Initial write-up. User-flagged Wave 78 planning convo (2026-05-14): manual depends on UI kit chốt → queued post-Wave 79. Stub created tách rời Wave 78 RETAIN scope.
