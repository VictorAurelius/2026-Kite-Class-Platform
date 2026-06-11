# GAP-364: kitehub-admin kit polish — `school-profile.html` rebuild + 5 medium-priority items

**Status:** 🟢 DONE — 2026-06-11 (Wave ui-kits-100 Bucket B) closed via **GAP-364b**: cross-screen polish 5 items shipped + re-audit avg **106.2/128** (≥105 ✓, floor 103 ≥95 ✓). school-profile rebuild (Wave 22) + cross-screen polish (GAP-364b) together clear the kit.
**Priority:** 🟡 P2 (Track 2 port quality polish — NOT P1 because no persona/legal violation; school-profile below floor but not blocking)
**Domain:** Frontend / Design System
**Found:** 2026-05-05 (Wave 20 Bucket B external review)
**Affects:** `documents/02-architecture/design-system/ui_kits/kitehub-admin/screens/school-profile.html` (rebuild) + 5 polish items; downstream blocks **GAP-271** (Track 2 port) avg-floor compliance

## Problem

External /128 review (Wave 20 Bucket B — `audits/ui-review/2026-05-05-round-3-kitehub-admin-review.md`) scored kit avg **101.1/128** (delta -6.1 vs self-report 107.2 — unusually small per agent's analysis: kit had explicit WCAG ratios commented per screen + 4 MoET regulations cited at article level + realistic VN K-12 mock data). Verdict: **APPROVE WITH POLISH**.

### Below-floor finding

- `school-profile.html` **91/128** (below 95 floor) — settings-form-only screen, low aesthetics ceiling, no progressive disclosure

### Borderline

- `teacher-management.html` 99/128 — list UI competent but doesn't stand out vs MoET-bearing peers (NOT requiring polish gap, just floor-passing)

### 5 medium-priority polish items (Bucket B §11.2)

1. Per-screen loading skeletons (currently dashboard-only)
2. Per-screen empty states (relies on shared gallery, not in-context)
3. Dark-mode parity (deferred at kit level)
4. Staff vetting workflow visualization (AC-ONBOARD-005 not visualized)
5. Cross-screen Zalo OA reusable pattern (parent-comms has it, others don't)

## Current State (verified 2026-05-05 via Bucket B report)

| Screen | Score /128 | Status |
|---|---:|---|
| `dashboard.html` | ~106 | ⭐⭐⭐ good (top scorer) |
| `report-cards.html` | ~106 | ⭐⭐⭐ good |
| `academic-calendar.html` | ~104 | ⭐⭐⭐ good |
| `bulk-import.html` | ~103 | ⭐⭐⭐ good |
| `conduct.html` | ~102 | ⭐⭐⭐ good |
| `multi-class-roster.html` | ~102 | ⭐⭐⭐ good |
| `parent-comms.html` | ~101 | ⭐⭐⭐ good (Zalo OA pattern reusable) |
| `fees.html` | ~101 | ⭐⭐⭐ good |
| `login.html` | ~100 | ⭐⭐⭐ good |
| `empty-states.html` | ~99 | ⭐⭐ borderline |
| `teacher-management.html` | 99 | ⭐⭐ borderline (NOT polish gap) |
| `school-profile.html` | **91** | ⭐ rebuild |

11/12 screens cluster 99-106 (good but not great); only 1 above 105 target.

## Proposed Fix

### Priority 1 — `school-profile.html` rebuild (~12-16h)

- Convert form-only layout to dashboard-style: hero KPI block (school stats: 1.247 HS / 62 GV / 25 lớp / NK 2025-2026) + tabbed sections (Thông tin cơ bản / Cơ sở vật chất / Đội ngũ / Pháp lý)
- Progressive disclosure: collapse legal/license fields under "Pháp lý & MoET" tab (default collapsed)
- Add visual cues: school logo upload preview, organizational chart sparkline, accreditation badge pills
- Target: 91 → ≥105

### Priority 2 — Cross-screen polish (~12-16h)

- Per-screen loading skeletons (12 screens × ~30 min each)
- Per-screen empty states (in-context, not gallery-only)
- Dark-mode CSS parity (extend `_shared/colors_and_type.css` dark vars to all screens)
- Staff vetting workflow visualization (AC-ONBOARD-005) — likely new screen `staff-vetting.html` OR extend `teacher-management.html`
- Zalo OA reusable component extraction → `_shared/components/zalo-oa-card.html`

### Priority 3 — Defer (NOT this gap)

- `teacher-management.html` 99 polish — scope creep; current floor-passing acceptable

## Acceptance Criteria

- [x] `school-profile.html` rebuilt to ≥105 score (self-score 107; Round 4 external re-audit 109/128)
- [x] Loading skeletons added per-screen → GAP-364b DONE (shared `admin-states.js`)
- [x] Empty states in-context per-screen → GAP-364b DONE
- [x] Dark-mode parity verified on all 12 screens → GAP-364b DONE
- [x] Staff vetting workflow visualized (AC-ONBOARD-005) → GAP-364b DONE (teacher-management board)
- [x] Zalo OA pattern extracted to `_shared/components/` → GAP-364b DONE
- [x] Re-score — kit avg **106.2/128** ≥105 (was 101.1) → GAP-364b DONE
- [x] GAP-271 avg-floor precondition unblocked (avg 106.2 ≥105, floor 103 ≥95) → GAP-364b
- [x] Cross-link added in `ui_kits/kitehub-admin/README.md` to this polish gap (Wave 22 section + Round 4 sync)

## Related

- Review report: `documents/04-quality/audits/ui-review/2026-05-05-round-3-kitehub-admin-review.md`
- Parent gap: GAP-348 (Wave 20 Round 3 review) — flips PARTIAL on this filing
- Persona AC: `documents/00-brd/persona-criteria/P5-k12-school.md` (P5 K-12 Principal Tier-1) + `documents/00-brd/persona-reviews/P2-small-center-round-1-2026-05-04.md` (P2 cross-ref)
- Track 2 port (BLOCKED on avg-floor): GAP-271

## Effort estimate

~32-42h total (~4-6 wave-days). Single agent bucket OR pair-wave with GAP-363 (kiteclass-student polish) as 2-bucket polish wave-pack.

## Log

- **2026-06-11 (Wave ui-kits-100 Bucket B):** Closed via **GAP-364b** — all 5 cross-screen polish items shipped (loading skeletons + in-context empty states via shared `admin-states.js`, dark-mode parity 12/12, staff vetting AC-ONBOARD-005 in teacher-management, Zalo OA `_shared/components/zalo-oa-card.html` reused at conduct + report-cards). Round 4 re-audit `2026-06-11-round-4-kitehub-admin-reaudit.md` = **106.2/128 avg** (+5.1 vs baseline 101.1), floor 103, 11/12 ≥105. GAP-271 avg-floor precondition unblocked. **Status → DONE**, git mv → `phase-2/closed/`.
- **2026-05-06 (Wave 22 Bucket B):** `school-profile.html` rebuilt — form-only layout (91/128) → dashboard-style with hero KPI block (1.247 HS / 62 GV / 25 lớp / NK 2026-2027) + 4-tab progressive disclosure (Thông tin cơ bản / Cơ sở vật chất / Đội ngũ / Pháp lý) + collapsible MoET licensing + organizational chart sparkline (vanilla SVG) + accreditation badge pills (chuẩn quốc gia mức 2 + kiểm định CL TT-17/2018 + PCCC + ATTP) + school logo placeholder with edit affordance. WCAG AA self-measured: hero contrast 7.2:1 AAA / body 14.8:1 AAA / muted-fg 4.7:1 AA / tab focus 2px / arrow-key tab navigation. Self-score 107/128 (T28/H30/A26/U23). README cross-link section added with explicit GAP-364b deferral list. **Status PARTIAL per `gap-done-discipline.md` §3 PARTIAL exit ramp** — cross-screen polish items (skeletons / empty states / dark-mode / staff-vetting / Zalo OA extract / kit avg ≥105) genuinely deferred to GAP-364b (filed in Wave 22 closure PR by coordinator).
- **2026-05-05:** Filed by Wave 20 Bucket C closure (this PR) per `audit-to-gap-pipeline.md` + Bucket B external review findings. Lower priority than GAP-363 (no child-protection violation; school-profile is aesthetics-only deficiency). Recommended wave-pack pairing with GAP-363.
