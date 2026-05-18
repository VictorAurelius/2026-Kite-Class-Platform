---
id: GAP-537c
title: P2 Owner + P3 Manager screenshots capture + Tier 2 annotation overlay
status: PARTIAL
priority: P1
layer: Frontend
phase: phase-1-beta
percent_complete: 50
created: 2026-05-15
updated: 2026-05-16
parent: GAP-537
wave_target: 81
---

# GAP-537c — P2/P3 screenshots + Tier 2 annotation

**Type:** Sister/follow-up gap (split from GAP-537 PARTIAL 75% Wave 80 Bucket D)
**Priority:** 🟠 P1 — manual completeness for beta tenant invite UX
**Wave target:** 81 (post-DEPLOY) — bundle with deploy verification self-test
**Estimate:** ~2-3h

## Problem

Wave 80 Bucket D shipped 20 F1+F2 source pages + PDF generation script + Playwright screenshot capture script. Two remaining items:

1. **P2 Owner (chị Hằng) + P3 Manager (anh Tâm) screenshots** — pages depend on Bucket B (invite-staff UI) + Bucket C (RBAC RoleGuard) which merged in Wave 80 — now possible to capture but didn't run in same wave because Bucket D worktree was parallel-isolated from B+C
2. **Tier 2 annotation overlay** (mũi tên đỏ + viền vàng + số bước via Sharp/Jimp) — deferred per `user-manual-content-standard.md` §2 row 6 Wave 79 placeholder allowance; placeholder PNGs (1×1 transparent) shipped Wave 80

## Scope

- [ ] Run `bash scripts/capture-user-manual-screenshots.sh p2-owner` against live dev server với OWNER seeded user
- [ ] Run `bash scripts/capture-user-manual-screenshots.sh p3-manager` against live dev server với STAFF seeded user
- [ ] Tier 2 annotation: Sharp/Jimp overlay mũi tên đỏ `#dc2626` + viền vàng `#facc15` + số bước cho key UI elements (apply to F1+F2 all 20 page screenshot sets)
- [ ] Replace placeholder PNGs với actual captures + annotated overlays
- [ ] Regenerate 4 persona PDFs (anonymous / p2-owner / p3-manager / platform-admin) verify visual quality

## Acceptance Criteria

- [ ] 20 actual screenshot PNGs (not placeholder 1×1) at `documents/05-guides/user-manual/{persona}/screenshots/{topic}-step-{N}.png`
- [ ] Each screenshot has Tier 2 annotation overlay (arrow + border + number)
- [ ] 4 PDFs render correctly with embedded screenshots
- [ ] PDF size <5MB per persona (compression validation)
- [ ] vi-VN locale confirmed in all captures (browser UI text Vietnamese)

## Dependencies

- **Upstream:** GAP-537 PARTIAL 75% (Wave 80 Bucket D PR #1382 shipped F2 sources + scripts + 10/20 screenshots) — DONE-eligible after this gap closes
- **Cross-link:** Sister wave Bucket B (#1383 invite-staff) + Bucket C (#1381 RBAC) shipped → Owner + Manager UI now functional for screenshot capture

## Refs

- `.claude/rules/user-manual-content-standard.md` §2 row 6 (annotation spec)
- `scripts/capture-user-manual-screenshots.sh` (Wave 80 Bucket D)
- `scripts/render-user-manual-pdf.sh` (Wave 80 Bucket D)

## Log

- **2026-05-15:** Filed as Wave 80 Bucket D PARTIAL exit-ramp per `gap-done-discipline.md` §3. Deferred to Wave 81 to bundle with DEPLOY+SMOKE post-deploy verification self-test (logical sequence: screenshots capture live UI after deploy verify).
- **2026-05-16:** Wave 86 Bucket C — flipped OPEN → PARTIAL (50%). Shipped: 5 new user manual pages (P2: signup.md, onboarding-wizard.md, first-class.md; P3: invite-accept.md, daily-ops.md) với placeholder screenshot HTML comments per `user-manual-content-standard.md` §2 row 6 allowance + audit doc `documents/04-quality/audits/persona-review/2026-05-15-p2-onboarding-wizard-audit.md` documenting C-AC1/2/3 verdicts (1 PASS + 2 PARTIAL). Screenshots directories created (empty, .gitkeep placeholder). EC2 backend stopped during this session → live capture deferred. Real screenshot capture + Tier 2 annotation tracked GAP-537c-followup-screenshot-capture (P1 wave 87+). C-AC1 wizard step count verified 4 steps via code reading `OnboardingWizard.tsx`. C-AC2 permission matrix explicit via existing `permissions.md` + new `invite-accept.md` §4. C-AC3 ≤5 phút verified via step-time analysis in `first-class.md` §7 (median ~4 phút).
