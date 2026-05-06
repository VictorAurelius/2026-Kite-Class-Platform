# GAP-364b: kitehub-admin kit — cross-screen polish (skeletons / empty-states / dark-mode / staff-vetting / Zalo OA extract)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2 (Track 2 port quality polish — NOT P1; school-profile rebuild already lifted lowest screen above floor in GAP-364)
**Domain:** Frontend / Design System
**Found:** 2026-05-06 (Wave 22 closure — GAP-364 PARTIAL deferral, planned at wave-22 plan §3 Bucket B)
**Affects:** `documents/02-architecture/design-system/ui_kits/kitehub-admin/` — 12 existing screens + 1 new (staff-vetting); downstream blocks **GAP-271** avg-floor compliance

## Problem

Wave 22 Bucket B shipped GAP-364 PARTIAL — `school-profile.html` rebuilt 91 → 107. Cross-screen polish items were explicitly out-of-scope per Wave 22 plan to keep wall-clock parallel-friendly (~14h Bucket vs ~37h serial). This gap tracks the deferred ~23h of cross-screen work.

## Current State (verified 2026-05-06 post-Wave-22-Bucket-B-merge)

| Screen | Score /128 | Issue |
|---|---:|---|
| `dashboard.html` | ~106 | Loading skeleton good (gold standard) |
| `school-profile.html` | **107** (post-Wave-22) | ✅ rebuilt; reference for tabbed pattern |
| Other 10 screens | 99-104 | Most lack: per-screen skeletons, in-context empty states, dark-mode parity |
| (NEW) `staff-vetting.html` | n/a | AC-ONBOARD-005 staff vetting workflow not visualized |
| `_shared/components/zalo-oa-card.html` | n/a | Zalo OA pattern hardcoded in `parent-comms.html` only; not reusable |

## Proposed Fix

**Item 1 — Per-screen loading skeletons** (~6h):
Extract dashboard's skeleton pattern → reusable `_shared/components/skeleton-row.html`. Apply to remaining 11 screens. Each screen 30-min adaptation.

**Item 2 — Per-screen empty states** (~5h):
Migrate from `empty-states.html` gallery references to in-context per-screen empty states. Each screen ~25-min sketch.

**Item 3 — Dark-mode CSS parity** (~5h):
Extend `_shared/colors_and_type.css` `[data-theme="dark"]` token block to cover all admin screens. Currently dashboard + report-cards have partial dark vars; remaining 10 screens default-light only. Manual visual check per screen.

**Item 4 — Staff vetting workflow** (~5h):
NEW screen `screens/staff-vetting.html` visualizing AC-ONBOARD-005:
- Pending vetting queue (with state machine: SUBMITTED / IN_REVIEW / APPROVED / REJECTED)
- LLTP document upload preview
- Approval flow (single-approver + dual-approver paths)
- Cross-link to `documents/01-business/kiteclass/child-protection/rules.md` BR-CHILD-PROTECT-* + Wave 18b3 `MinIOVettingDocumentStorageImpl` (real backend)

**Item 5 — Zalo OA reusable component** (~2h):
Extract `parent-comms.html` Zalo OA card → `_shared/components/zalo-oa-card.html`. Document props (caption, state pill, action button). Update `parent-comms.html` to import shared.

## Acceptance Criteria

- [ ] All 12 existing screens have per-screen loading skeletons
- [ ] All 12 existing screens have in-context empty states
- [ ] Dark-mode CSS parity verified on all 12 screens (manual visual check screenshot)
- [ ] `screens/staff-vetting.html` created visualizing AC-ONBOARD-005
- [ ] `_shared/components/zalo-oa-card.html` extracted; `parent-comms.html` re-imports
- [ ] Re-score via `quality/ui-review-prototype` skill — kit avg ≥105 (was 101.1 + school-profile lift)
- [ ] **GAP-271** (Track 2 admin port) unblocked
- [ ] Cross-link in `kitehub-admin/README.md` updated removing GAP-364b deferral note

## Related

- Parent gap: GAP-364 (Wave 22 Bucket B PARTIAL — school-profile.html rebuild only)
- Wave 20 Bucket B external review: `documents/04-quality/audits/ui-review/2026-05-05-round-3-kitehub-admin-review.md`
- Track 2 port (BLOCKED on avg-floor): GAP-271
- Wave 22 plan §3 Bucket B "OUT OF SCOPE (defer to GAP-364b)" list

## Effort estimate

~23h total (6+5+5+5+2). Single agent bucket OR pair-wave with GAP-363b (kiteclass-student delta-to-105) as 2-bucket UI kits Round 4 polish wave-pack.

## Log

- **2026-05-06:** Filed at Wave 22 closure per Wave 22 plan §3 Bucket B "OUT OF SCOPE" list + `gap-done-discipline.md` §3 PARTIAL exit ramp for GAP-364.
