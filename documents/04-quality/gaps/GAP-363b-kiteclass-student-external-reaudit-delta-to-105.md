# GAP-363b: kiteclass-student kit — external re-audit + delta-to-≥105 polish

**Status:** 🔵 OPEN
**Priority:** 🟡 P2 (kit avg target — not BLOCKING; floor ≥95 restored, P0 persona violation cleared in GAP-363)
**Domain:** Frontend / Design System
**Found:** 2026-05-06 (Wave 22 closure correction — GAP-363 §2 AC threshold genuinely unmet)
**Affects:** `documents/02-architecture/design-system/ui_kits/kiteclass-student/` — all 14 screens; downstream **GAP-269** (Track 2 port) port-with-polish-parallel acceptable

## Problem

GAP-363 Wave 22 Bucket A shipped the P0 child-protection fix (`payments.html` Option C parent-trigger) + 4 polish items (my-classes / assignments / grade-detail / profile). Kit floor ≥95 restored (lowest screen 100). However, AC #6 "kit avg ≥105 (was 100.4)" remains genuinely unmet — agent self-rescore ~102.5 (+2.1 delta). Per `feedback_audit_calibration.md` self-rescore overstates 15-20 pts vs specialist, so external estimate ~85-95.

GAP-363 was downgraded 🟢 DONE → 🟡 PARTIAL at closure to honor `gap-done-discipline.md` §2 (no soft-deferral). This gap tracks the remaining delta-to-target.

## Current State (verified 2026-05-06 post-Wave-22-Bucket-A-merge)

| Screen | Round 3 score (external) | Wave 22 self-rescore | Notes |
|---|---:|---:|---|
| `payments.html` | 92 | ~108 (Option C rebuild) | P0 fix; biggest lift |
| `today.html` | 101 | unchanged | floor-passing, no edit |
| `my-classes.html` | 99 | ~101 | minor polish (chip parens) |
| `class-detail.html` | 100 | unchanged | floor-passing |
| `assignments.html` | 100 | ~101 | minor polish (tab counts) |
| `assignment-detail.html` | 102 | unchanged | floor-passing; saved-draft scope deferred |
| `grades.html` | 103 | unchanged | floor-passing |
| `grade-detail.html` | 100 | ~102 | info-icon tooltip added |
| `attendance.html` | 102 | unchanged | floor-passing |
| `notifications.html` | 102 | unchanged | parent-kép visualization deferred |
| `profile.html` | 100 | ~101 | linkable Học lực pill |
| `login.html` | 100 | unchanged | parent-reset workflow deferred |
| `empty-states.html` | 104 | unchanged | floor-passing |
| **Kit avg** | **100.4** | **~102.5** (self) | **target ≥105** |

11/14 screens cluster 99-104 ("good but not great"); only payments lifted significantly. Hitting ≥105 avg requires lifting 4-6 screens by ~3-5 pts each.

## Proposed Fix

**Step 1 — External re-audit** (~3-4h):
Run `quality/ui-review-prototype` skill against all 14 screens with explicit external auditor mindset (not self-score). Capture per-screen /128 with breakdown. Document calibration delta vs Bucket A self-rescore.

**Step 2 — Identify 5-6 highest-leverage polish targets** based on external scores:
- Probably `today.html` (home, high traffic) → richer data/animation
- Probably `class-detail.html` → more visual hierarchy
- `assignment-detail.html` → saved-draft scope clarification + visual polish
- `notifications.html` → parent-kép visualization sketch (was Track 2 spec deferral; revisit)
- `login.html` → AC-EDGE-001 parent-reset workflow sketch (was Track 2 spec deferral; revisit)

**Step 3 — Polish to lift avg ≥105** (~6-10h):
Apply per-screen targeted polish per Step 2 findings.

**Step 4 — Final external re-audit** confirms ≥105.

If external re-audit (Step 1) reveals 102-104 avg (already close), Step 3 may be lighter. If reveals 90-95 avg, more work needed and may warrant scope re-think (port-with-iteration approach via GAP-269 Track 2 port).

## Acceptance Criteria

- [ ] External re-audit completed via `quality/ui-review-prototype` skill — per-screen /128 documented
- [ ] Calibration delta (self vs external) recorded for `feedback_audit_calibration.md` learning
- [ ] 5-6 polish targets identified + scoped
- [ ] Polish PR shipped lifting avg to ≥105
- [ ] Final external re-audit confirms ≥105
- [ ] GAP-363 retroactively flipped 🟡 PARTIAL → 🟢 DONE on completion of this gap

## Related

- Parent gap: GAP-363 (Wave 22 Bucket A — Status PARTIAL pending this delta)
- Wave 20 Bucket A external review: `documents/04-quality/audits/ui-review/2026-05-05-round-3-kiteclass-student-review.md`
- Track 2 port: GAP-269 — technically unblocked on P0 (child-protection) but kit avg <105 means port screens may need parallel polish
- Persona AC: GAP-365 (S-student.md Tier-1 doc) — DONE Wave 22 Bucket C

## Effort estimate

~10-15h total (3-4h re-audit + 6-10h polish + 1h final verify). Single agent bucket. Not BLOCKING any wave; pickable opportunistically.

## Log

- **2026-05-06:** Filed at Wave 22 closure per `gap-done-discipline.md` §3 PARTIAL exit ramp. GAP-363 §AC threshold ≥105 unmet by self-rescore ~102.5; coordinator downgraded GAP-363 DONE → PARTIAL and filed this follow-up to honor `gap-done-discipline.md` §2. Tracked under Wave 22 closure ROADMAP entry.
