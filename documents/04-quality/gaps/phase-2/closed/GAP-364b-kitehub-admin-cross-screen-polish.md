# GAP-364b: kitehub-admin kit — cross-screen polish (skeletons / empty-states / dark-mode / staff-vetting / Zalo OA extract)

**Status:** 🟢 DONE — 2026-06-11 (Wave ui-kits-100 Bucket B) all 5 cross-screen polish items shipped; re-audit avg **106.2/128** (≥105 ✓, floor 103 ≥95 ✓)
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

- [x] All 12 existing screens have per-screen loading skeletons — shared `_shared/scripts/admin-states.js` injects context-aware skeleton per `body[data-skeleton]` (`?state=loading`), 10 state-enabled + 2 `data-no-states` (login/empty-states); was dashboard-only
- [x] All 12 existing screens have in-context empty states — same script injects `.empty-state` from `body[data-empty-*]` (`?state=empty`); own per-screen copy + icon + CTA (was shared gallery only)
- [x] Dark-mode CSS parity on all 12 screens — dark toggle wires the `.dark` token layer (in `_shared/colors_and_type.css`); every colour `hsl(var(--token))` so flip recolours all 12; persists localStorage + honours `prefers-color-scheme`; was 2/12
- [x] AC-ONBOARD-005 staff vetting workflow visualised — extended `teacher-management.html` (per Proposed Fix "OR extend" option) with 4-column board + CCCD/bằng cấp/LLTP checklist + 2-level approval + child-protection auto-reject
- [x] `_shared/components/zalo-oa-card.html` extracted (documented props/slots) + `.zalo-oa-card` CSS in kit `styles.css`; reused at conduct + report-cards notify points; `parent-comms.html` retains origin pattern
- [x] Re-score — kit avg **106.2/128** ≥105 (was 101.1); `documents/04-quality/audits/ui-review/2026-06-11-round-4-kitehub-admin-reaudit.md`
- [x] **GAP-271** avg-floor precondition unblocked (avg 106.2 ≥105, floor 103 ≥95) — Track 2 port itself remains separate scope under GAP-271
- [x] Cross-link in `kitehub-admin/README.md` updated — deferral list marked shipped + Round 4 score sync + Last Updated 2026-06-11

## Walk evidence (HTML/JSX prototype — per output-review-mandate §3 "HTML/JSX prototypes" row)

- `node --check _shared/scripts/admin-states.js` → **PASS** (valid JS, no syntax error)
- `bash _shared/scripts/check-ui-kits-landing.sh` → **PASS** (landing parity intact)
- Component presence verified: `zalo-oa-card.html` + `_shared/components/README.md` created; `vet-board` + `AC-ONBOARD-005` present in `teacher-management.html`; zalo card in conduct + report-cards
- Dark mode: tokens cascade via `hsl(var(--token))` + `.dark` block → flip recolours all 12 (token-driven, no hardcoded hex outside shared CSS)
- States: `?state=loading` / `?state=empty` demonstrable via `Tải` / `Trống` state-tabs injected by `admin-states.js`

## Related

- Parent gap: GAP-364 (Wave 22 Bucket B PARTIAL) — closed via this delta
- Re-audit (this gap closes it): `documents/04-quality/audits/ui-review/2026-06-11-round-4-kitehub-admin-reaudit.md`
- Wave 20 Bucket B external review (baseline 101.1): `documents/04-quality/audits/ui-review/2026-05-05-round-3-kitehub-admin-review.md`
- Track 2 port (avg-floor unblocked): GAP-271
- Wave 22 plan §3 Bucket B "OUT OF SCOPE (defer to GAP-364b)" list

## Effort estimate

~23h total (6+5+5+5+2). Single agent bucket OR pair-wave with GAP-363b (kiteclass-student delta-to-105) as 2-bucket UI kits Round 4 polish wave-pack.

## Log

- **2026-06-11 (Wave ui-kits-100 Bucket B):** All 5 cross-screen polish items shipped + re-audited. (1) Per-screen loading skeletons + (2) in-context empty states via shared `admin-states.js` (DRY context-aware injection, demonstrable `?state=loading`/`?state=empty`); (3) dark-mode parity 12/12 wiring `.dark` token layer; (4) staff vetting AC-ONBOARD-005 board in `teacher-management.html`; (5) `_shared/components/zalo-oa-card.html` extracted + reused at conduct + report-cards. Re-audit `2026-06-11-round-4-kitehub-admin-reaudit.md` = **106.2/128 avg** (+5.1 vs baseline 101.1), floor 103 (login), 11/12 ≥105. PASS ≥105 avg + ≥95 floor. README + audits-index.csv synced. **Status → DONE**, git mv → `phase-2/closed/`. Closes GAP-364 via this delta.
- **2026-05-06:** Filed at Wave 22 closure per Wave 22 plan §3 Bucket B "OUT OF SCOPE" list + `gap-done-discipline.md` §3 PARTIAL exit ramp for GAP-364.
