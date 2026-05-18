---
id: GAP-537c-followup-screenshot-capture
title: P2 Owner + P3 Manager live screenshot capture + Tier 2 annotation (Wave 87+ post-EC2-restart)
status: OPEN
priority: P1
layer: Frontend
phase: phase-1-beta
percent_complete: 0
created: 2026-05-16
updated: 2026-05-16
parent: GAP-537c
wave_target: 87
---

# GAP-537c-followup — Live screenshot capture cho P2 + P3 manuals

**Type:** Sister follow-up gap (split from GAP-537c PARTIAL 50% Wave 86 Bucket C)
**Priority:** 🟠 P1 — manual completeness for beta tenant invite UX
**Wave target:** 87+ (post-EC2 backend restart)
**Estimate:** ~2-3h (FE dev server + Playwright capture + Sharp/Jimp annotation + verify PDF render)

## Problem

Wave 86 Bucket C shipped 5 new P2/P3 user manual pages + audit doc, nhưng tất cả screenshot vẫn là **placeholder HTML comments** (per `user-manual-content-standard.md` §2 row 6 allowance) vì:

1. **EC2 backend stopped** during Wave 86 Bucket C session per /start-session AWS snapshot (Free Tier hour save)
2. Live screen capture requires:
   - BE EC2 instances running (kh_backend + kc_app)
   - RDS available
   - FE dev server on port 3001 (or staging Vercel reachable)
3. Without live UI render → cannot run `bash scripts/capture-user-manual-screenshots.sh {persona}`

## Scope (14 screens total)

### P2 Owner (8 screens)

| File path | Step | Content |
|---|---|---|
| `screenshots/signup-step-1.png` | 1 | Landing page với CTA "Yêu cầu truy cập Beta" |
| `screenshots/signup-step-2.png` | 2 | Form đăng ký 4 fields |
| `screenshots/signup-step-3.png` | 3 | Email confirm inbox + link |
| `screenshots/signup-step-4.png` | 4 | Admin approval email |
| `screenshots/onboarding-wizard-step-1.png` | W1 | Wizard step 1 - chào mừng |
| `screenshots/onboarding-wizard-step-2.png` | W2 | Wizard step 2 - sidebar tour |
| `screenshots/onboarding-wizard-step-3.png` | W3 | Wizard step 3 - website preview |
| `screenshots/onboarding-wizard-step-4.png` | W4 | Wizard step 4 - checklist |
| `screenshots/first-class-step-{1..6}.png` | 1-6 | Create class flow 6 steps |

(Total = 4 + 4 + 6 = **14 P2 screens**; task spec says 8 — refine to top priority subset)

### P3 Manager (6 screens)

| File path | Step | Content |
|---|---|---|
| `screenshots/invite-accept-step-1.png` | 1 | Email mời từ chị Hằng |
| `screenshots/invite-accept-step-2.png` | 2 | Accept page với thông tin trung tâm |
| `screenshots/invite-accept-step-3.png` | 3 | Form đặt password |
| `screenshots/invite-accept-step-4.png` | 4 | First-login overlay tour |
| `screenshots/invite-accept-step-5.png` | 5 | Dashboard Manager |
| `screenshots/daily-ops-step-{1..6}.png` | 1-6 | Daily workflow 6 steps |

(Top priority 6 selected from invite-accept + daily-ops)

## Acceptance Criteria

- [ ] EC2 backend started + RDS available + FE accessible
- [ ] `bash scripts/aws/start-stack.sh` confirmed `IsLogging=true`
- [ ] 14+ raw screenshots PNGs at `documents/05-guides/user-manual/{p2-owner,p3-manager}/screenshots/`
- [ ] Tier 2 annotation overlay applied (mũi tên đỏ `#dc2626` + viền vàng `#facc15` + số bước) via Sharp/Jimp programmatic OR Figma export
- [ ] Browser locale vi-VN confirmed (UI text Vietnamese trong captures)
- [ ] Resolution: 1440×900 desktop OR 375×812 mobile (mix per use case)
- [ ] Replace all `<!-- Screenshot placeholder: ... -->` comments với actual `![alt](screenshots/...)` markdown
- [ ] Regenerate PDFs: `bash scripts/render-user-manual-pdf.sh p2-owner` + `p3-manager` → verify PDFs <5MB each + screenshots embedded
- [ ] C-AC1/2 verification re-run live (UI overlay tour exists? wizard skip-resume across browser close?)
- [ ] If FE persistence missing → file `GAP-537c-followup-wizard-persistence` (P2)
- [ ] If UI overlay tour missing → file `GAP-537c-followup-permission-tour-ui` (P2)

## Dependencies

- **Upstream:** GAP-537c PARTIAL 50% (Wave 86 Bucket C — docs + placeholders shipped this PR)
- **Blocker:** EC2 backend must be running (currently stopped per cost-save)
- **Cross-link:** `scripts/capture-user-manual-screenshots.sh` + `scripts/render-user-manual-pdf.sh` (Wave 80 Bucket D)

## Refs

- Parent: `documents/04-quality/gaps/GAP-537c-user-manual-p2-p3-screenshots-tier2-annotation.md`
- Audit: `documents/04-quality/audits/persona-review/2026-05-15-p2-onboarding-wizard-audit.md`
- Rule: `.claude/rules/user-manual-content-standard.md` §2 row 6 (annotation spec)
- Wave: `documents/03-planning/waves/wave-2026-05-15-86-rc1-tag-preflight.md` §3 Bucket C

## Log

- **2026-05-16:** Filed as Wave 86 Bucket C PARTIAL exit-ramp per `gap-done-discipline.md` §3. EC2 backend stopped during this session prevented live capture; placeholder HTML comments shipped per `user-manual-content-standard.md` §2 row 6 allowance. Defer to Wave 87+ when AWS stack restarted (`bash scripts/aws/start-stack.sh`).
