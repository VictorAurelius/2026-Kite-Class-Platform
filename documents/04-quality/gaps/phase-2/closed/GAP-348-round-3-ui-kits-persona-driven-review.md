# GAP-348: Round 3 UI Kits Persona-Driven Review (kiteclass-student + kitehub-admin)

**Status:** 🟢 DONE 2026-06-11 — Wave ui-kits-100: GAP-363b/363 (#2333) + GAP-364b/364 (#2334) DONE, GAP-365 DONE Wave 22 → umbrella đóng. Track 2 ports unblocked.
**Priority:** 🟠 P1 (pre-port quality gate — blocks Track 2 Phase 2 trustworthiness)
**Domain:** Quality / Design System
**Found:** 2026-05-04 (session audit — UI kits review coverage gap)
**Affects:** `documents/02-architecture/design-system/ui_kits/kiteclass-student/`, `documents/02-architecture/design-system/ui_kits/kitehub-admin/`

## Problem

Round 3 kits merged 2026-04-29 (PR #700 student avg **116/128** ⭐⭐, PR #703 admin avg **107.2/128**) but **only have agent self-report scores** — no external review through `quality/ui-review/SKILL.md` /128 rubric, no persona-AC mapping, no calibration vs persona-criteria docs.

Per `feedback_audit_calibration.md`: self-audit consistently overstates 15-20 pts vs specialist external review. Trusting 116 / 107.2 self-scores while planning Track 2 Phase 2 production port (GAP-269 student + GAP-271 admin) means we may port unvetted designs into production code.

## Current State (verified 2026-05-04)

| Artifact | Exists? | Persona-driven? |
|---|---|---|
| `audits/ui-review/2026-04-29-wave-1-add-ons-review.md` | ✅ | ✅ Wave 1.5/1.6/1.7 only (kitehub-pro v2 / kiteclass-teacher / ai-branding-wizard-v2) — NO Round 3 |
| `audits/ui-review/2026-04-29-frontend-ui-coverage-audit.md` | ✅ | 4-layer V-model coverage — not /128 scoring |
| Round 3 kit external review | ❌ | Self-report only inside PR #700 / #703 bodies |
| Persona AC mapping for student / admin journeys | ⚠️ | P2 Center Owner review (`P2-small-center-round-1-2026-05-04.md`) covers admin BUT scopes production code; Student persona has NO Tier-1 AC doc |

## Proposed Fix

Run `quality/ui-review/SKILL.md` /128 rubric externally on both Round 3 kits, plus map per-screen to persona ACs.

**Scope:**

### Part A — kiteclass-student review (~1 day)
- Score all 14 screens × /128 (rubric: visual / VN UX / mobile-PWA / states / a11y / consistency / dark-mode / persona-fit)
- Map each screen to S. Student persona journeys (Today / Classes / Assignments / Grades / Notif / Profile)
- Calibrate vs self-report 116; expect external 95-105 range (per audit-calibration delta heuristic)
- Flag kit-level gaps: WCAG measurements, dark-mode parity, mobile 320 viewport, VN data realism
- Output: `audits/ui-review/2026-05-XX-round-3-kiteclass-student-review.md`

### Part B — kitehub-admin review (~1 day)
- Score all 10 screens × /128
- Map to P2 Center Owner persona journeys (academic-calendar, multi-class-roster, fees, conduct, parent-comms, report-cards, bulk-import)
- Cross-reference `00-brd/persona-reviews/P2-small-center-round-1-2026-05-04.md` to identify P2 ACs the kit attempts
- Output: `audits/ui-review/2026-05-XX-round-3-kitehub-admin-review.md`

### Part C — Persona gap surfacing (~0.5 day)
- For Student kit: file new gap if no Tier-1 Student persona AC doc exists (`00-brd/persona-criteria/`)
- For Admin kit: cross-link review report into `P2-small-center-round-2-*.md` evidence pointers
- Update `ui_kits/README.md` Round 3 row with external review score + delta vs self-report

## Acceptance Criteria

- [ ] `audits/ui-review/2026-05-XX-round-3-kiteclass-student-review.md` written, all 14 screens scored, persona-mapped, with screenshot evidence pointers
- [ ] `audits/ui-review/2026-05-XX-round-3-kitehub-admin-review.md` written, all 10 screens scored, P2 persona-mapped
- [ ] Each kit external avg score documented with delta vs self-report (e.g., "self-report 116, external 102, delta -14")
- [ ] Kit-level gaps filed for any screen scoring <95 (per `dossier/10-acceptance-criteria.md` floor)
- [ ] Persona-AC coverage gaps surfaced (e.g., "S. Student persona has no Tier-1 AC doc → file follow-up")
- [ ] `ui_kits/README.md` Round 3 row updated with external review citations + delta
- [ ] If external avg < 105 (kit-level target floor), follow-up "kit polish" gap filed BEFORE Track 2 port for that kit can start

## Why P1 (not P2)

Per `meta-gap-priority.md` §3 priority matrix — this is **Business-Logic / Quality-gate tier**, sits between Meta and Feature. Without this gate, Track 2 Phase 2 (GAP-269 + GAP-271 production port) ships unvetted designs into production code where iteration cost is 3-5× higher than fixing in HTML prototype. P1 = fix before downstream Track 2 work touches affected kits.

## Related

- HTML prototypes: `ui_kits/kiteclass-student/` (PR #700) + `ui_kits/kitehub-admin/` (PR #703) merged 2026-04-29
- Wave plan: `wave-2026-04-29-ui-kits-round-3.md`
- Track 2 downstream: GAP-269 (port student), GAP-271 (port admin) — BLOCKED on this review per AC last bullet
- Sister review reports: `audits/ui-review/2026-04-29-wave-1-add-ons-review.md` (Wave 1.5-1.7 — same template)
- Calibration: `feedback_audit_calibration.md` (self-audit overstate 15-20 pts)
- Standard: `output-review-mandate.md` §3 row "HTML/JSX prototypes" (per-kit `/128` rubric mandate)

## Effort estimate

~2-3 days total (Parts A + B + C). Parallelizable: Part A and Part B can run as 2 background agents same wave-pack; Part C synthesizes after both ship.

## Log

- **2026-06-11 (Wave ui-kits-100 Bucket B):** kitehub-admin polish branch closed — **GAP-364 + GAP-364b → DONE** (Round 4 re-audit 106.2/128 avg ≥105, floor 103; GAP-271 avg-floor unblocked). **GAP-348 GIỮ PARTIAL — đợi Bucket A** (kiteclass-student polish): GAP-363 + GAP-363b chưa merge (vẫn ở `phase-2/`, GAP-363b OPEN P0). GAP-348 flips DONE khi cả kiteclass-student (GAP-363/363b) lẫn kitehub-admin (GAP-364/364b) branches đều closed.
- **2026-05-05 (Wave 20 Bucket C closure — this PR):** Status 🔵 OPEN → 🟡 PARTIAL per `gap-done-discipline.md` §3 PARTIAL exit ramp. Bucket A + B external reviews SHIPPED. Bucket C synthesis: 3 follow-up gaps filed.
  - **Wave 20 Bucket A (PR #803, merged 2026-05-05):** kiteclass-student external avg **100.4/128** (delta -15.6 vs self-report 116, calibration band ✓). Verdict: **APPROVE WITH POLISH** — `payments.html` 92/128 child-protection AC-FIN-001 violation (Pay button for K-12 S. Student). Tier-1 `S-student.md` AC doc absent. Report: `documents/04-quality/audits/ui-review/2026-05-05-round-3-kiteclass-student-review.md` (264 lines, 13 sections).
  - **Wave 20 Bucket B (PR #805, merged 2026-05-05):** kitehub-admin external avg **101.1/128** (delta -6.1 vs self-report 107.2 — unusually small per Bucket B analysis: kit had explicit WCAG ratios + 4 MoET regulations cited + realistic VN K-12 mock data). Verdict: **APPROVE WITH POLISH** — `school-profile.html` 91/128 below kit floor. Report: `documents/04-quality/audits/ui-review/2026-05-05-round-3-kitehub-admin-review.md` (346 lines, 15 sections).
  - **Follow-up gaps filed:**
    - **GAP-363** (P1 BLOCKING) — kiteclass-student polish (payments persona violation + 4 partials)
    - **GAP-364** (P2) — kitehub-admin polish (school-profile rebuild + 5 medium-priority polish items)
    - **GAP-365** (P2 Business-Logic) — file Tier-1 `S-student.md` AC doc (currently absent)
  - **Track 2 ports BLOCKED** per AC last bullet: GAP-269 (student) blocks on GAP-363 + GAP-365; GAP-271 (admin) blocks on GAP-364
  - **Status remains PARTIAL** because: (a) AC item "follow-up kit polish gap filed BEFORE Track 2 port for that kit can start" → 3 gaps filed (✅), but kit polish not yet executed; (b) AC item "If external avg < 105, follow-up gap filed" → both kits below 105, gaps filed (✅). Bucket A + B + C all shipped per plan.
- **2026-05-04:** Filed after session audit found Round 3 kits merged with self-report only — no external review through `ui-review` skill, no persona AC mapping. Track 2 Phase 2 plan (GAP-349) cannot trust agent self-scores when porting prototypes to production code.

- **2026-06-11 (DONE — Wave ui-kits-100 closure):** Umbrella đóng: kiteclass-student external avg 105.2/128 (GAP-363b/363 DONE #2333) + kitehub-admin external avg 106.2/128 (GAP-364b/364 DONE #2334) — cả 2 kit ≥105 floor ≥95; GAP-365 S-student.md đã DONE Wave 22. AC cuối "kit polish gap filed BEFORE Track 2 port" hoàn thành trọn: polish EXECUTED. GAP-269 (student port) + GAP-271 (admin port) hết blocker.
