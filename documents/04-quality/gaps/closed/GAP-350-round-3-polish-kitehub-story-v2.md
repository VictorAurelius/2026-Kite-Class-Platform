# GAP-350: Round 3 Polish — `kitehub-story-v2/` (Direction A Marketing Storytelling)

**Status:** 🟢 DONE 2026-05-05 — Wave 21 SHIPPED via PR #807 (kit build) + closure (this PR); 6 scroll-driven sections + Round 1 baseline preserved; agent self-report avg 109.8/128 (target ≥105 ✓)
**Priority:** 🟡 P2 — promoted from P2 LOWER 2026-05-05 per user request (Wave 21 marketing storytelling kit pickup); see Decision 3 still applies (marketing-only, not MVP-critical) but added to active wave queue ahead of Track 2 Phase 5
**Domain:** Frontend / Design System
**Found:** 2026-05-04 (session audit — Round 3 listed `kitehub-story-v2/` as 🔵 future, no gap filed)
**Affects:** `documents/02-architecture/design-system/ui_kits/kitehub-story-v2/` (NEW folder to be created)

## Problem

`kitehub-story-v2/` listed in `ui_kits/README.md` Status table as 🔵 **future** but no gap exists to track it. The Round 1 baseline (`kitehub-story` 546 LOC JSX) is preserved in `documents/07-archived/design-round-1-2026-04-29/kitehub-story/` (3 files: `app.jsx` 24.5K + `styles.css` 22.2K + `index.html` 1.2K).

Per `dossier/08-direction-decisions.md` Decision 3 — Direction A is **kept in scope but LOWER priority**, meant to be polished in Round 2 batch but deferred when Round 2 capacity prioritized B/D/C directions. Without a tracked gap, Track 2 production port (GAP-275 KH public marketing kit) has ambiguous source: port from Round 1 archive raw, or wait for Round 2/3 polish?

## Current State (verified 2026-05-04)

| Artifact | Status | Path |
|---|---|---|
| Round 1 baseline | ✅ Preserved | `documents/07-archived/design-round-1-2026-04-29/kitehub-story/` (`app.jsx` + `styles.css` + `index.html`) |
| Round 2/3 polish kit folder | ❌ Not created | `ui_kits/kitehub-story-v2/` does NOT exist |
| Direction A scope decision | ✅ Documented | `dossier/08-direction-decisions.md` Decision 3 |
| Track 2 port gap | ✅ Filed | GAP-275 (depends on this gap deciding source: polish-first vs raw archive port) |

## Proposed Fix

Create `ui_kits/kitehub-story-v2/` Round 3 polish kit, extending the Round 1 baseline with the deferred Round 2 enhancements per Decision 3.

**Scope (per `dossier/08-direction-decisions.md` Decision 3):**

- **Polish Round 1 baseline** — port 546 LOC from archive, modernize tokens to `_shared/colors_and_type.css` Round 1 source
- **Add scroll-driven storytelling** — parallax sections, sticky headers, before/after slider (refined)
- **Add "Một ngày của chủ trung tâm" section** — narrative storytelling for P2 Center Owner persona (Sao Demo demo flow)
- **Mock dashboard animation** — chart-rising animation + notification pop-in (CSS/JS, no real backend)
- **Marketing-only** — does NOT touch product UI (`/dashboard`, `/billing`, etc.)

**Tech direction:**
- Static HTML kit (consistent with other prototypes — Round 2/3 are HTML, NOT React)
- Vietnamese-only copy + VN mock data per `dossier/10-acceptance-criteria.md`
- WCAG AA contrast measured + commented per kit standard
- 3 viewports: mobile 320 / tablet 768 / desktop 1440

**Out of scope** (per Decision 3):
- Product UI redesign (Direction B/D handles that)
- A/B test infrastructure
- Investor pitch deck variant

## Acceptance Criteria

- [x] `ui_kits/kitehub-story-v2/` folder created with `README.md` + `index.html` landing + `styles.css` + section files (PR #807, 13 files)
- [x] Round 1 archive content migrated and refined (not lost — preserved in `_v1-baseline/` per AC)
- [x] Scroll-driven sections: hero kite character + parallax + sticky headers + before/after slider (6 sections shipped)
- [x] "Một ngày của chủ trung tâm" narrative section present (avg 112/128)
- [x] Mock dashboard animation (chart + notification) — CSS/JS only, no real data wiring
- [x] Avg score ≥105/128 per `dossier/10-acceptance-criteria.md` floor (kit-level target) — **109.8 self-report**
- [x] WCAG AA self-measurement in HTML comments per section, 3 viewports addressed (320/768/1440), `prefers-reduced-motion` honoured
- [x] Vietnamese-only copy with realistic VN mock tenant names ("Trung tâm Toán Master") + numbers (MoMo/VNPay/ZaloPay, NĐ 123/2020); honest framing "Mục tiêu Q4 2026" per `business-logic-review.md` §2.1 informed-gut discipline
- [x] `ui_kits/index.html` landing card added (closure PR — Wave 21 Bucket B coordinator)
- [x] `ui_kits/README.md` Status row updated 🔵 future → ✅ DONE with PR #807 + score 109.8 (closure PR)
- [x] Cross-link added to GAP-275 (Track 2 port source decision: port FROM this kit, NOT from raw archive) (closure PR)

## Decisions deferred to execution time

| Open question | Trigger to decide |
|---|---|
| HTML-only vs Round 1 React JSX migration approach | Wave plan Phase 0 — agent can convert 546 LOC JSX → HTML or keep as JSX kit (precedent: kiteclass-parent has `app.jsx`) |
| Investor pitch deck variant inclusion | Per Decision 3 "things NOT decided" — likely NO for Round 3 |
| External review timing (vs self-report only) | Cover via existing GAP-348 pattern OR file follow-up "GAP-348-style review for kitehub-story-v2" |

## Why P2 (not P1)

Per `meta-gap-priority.md` §3 — Marketing storytelling kit is Feature-tier, not Business-Logic-tier (no compliance/legal mandate, no persona blocker). LOWER priority confirmed by Decision 3. P2 = pickable when MVP-critical waves quiet, before Track 2 Phase 5 (GAP-275 KH public marketing port).

## Related

- Round 1 archive: `documents/07-archived/design-round-1-2026-04-29/kitehub-story/`
- Direction decision: `dossier/08-direction-decisions.md` Decision 3 + per-direction priority table
- Sister GAP-275 (Track 2 port KH public marketing + blog) — depends on this gap's output as source-of-truth
- `ui_kits/README.md` Status row currently 🔵 future
- Persona target: P2 Center Owner (KH SaaS marketing landing → trial signup conversion)
- **ConsentBanner mockup added Wave 23 Bucket E** (`screens/consent-banner.html`) — informational add-on demonstrating PDPL 2023 cookie consent banner placement on the marketing landing. Production component lives at `packages/shared-ui/src/components/ConsentBanner/` (Wave 23 Bucket BC). Kit Status remains 🟢 DONE; the Wave 23 add-on is supplementary (no AC change to this gap).

## Effort estimate

~1-2 days. Single agent bucket (HTML kit polish, no backend, no parallel needed). Single PR. Wave-pack candidate IF executed alongside GAP-274 (KC public marketing kit polish, also LOWER priority) — could pair as 2-bucket marketing wave-pack.

## Log

- **2026-05-05 (Wave 21 SHIPPED, closure PR):** Status 🔵 OPEN → 🟢 DONE per `gap-done-discipline.md` §2 — all 11 ACs verified + checked. PR #807 (kit build, 13 files, 2872 LOC) + closure (this PR adds index.html landing card + README Status row + GAP-275 cross-link). Agent self-report avg **109.8/128** (target ≥105 ✓ floor; sits between Round 2 baselines 107.8 and 110.5 — honest scoring per `feedback_audit_calibration.md`). Per-section: hero 113 / parallax-features 110 / before-after 108 / một-ngày-chu-trung-tam 112 / mock-dashboard 109 / pricing-cta 107. Static HTML + vanilla JS (NO React/Babel) — token-themed via `_shared/colors_and_type.css` HSL vars, KH sky+orange brand locked. ARIA slider on Before/After + aria-live on day-timeline scene panel + dashboard notif. `prefers-reduced-motion` honoured globally. Realistic VN mock data (Trung tâm Toán Master, MoMo/VNPay/ZaloPay, NĐ 123/2020). Trust signal honest framing: "Mục tiêu Q4 2026" (NOT unverified claim) per `business-logic-review.md` §2.1 informed-gut discipline. Round 1 baseline preserved intact in `_v1-baseline/` per AC. **Unblocks GAP-275** (Track 2 KH public marketing port) — port FROM this kit's output, not raw Round 1 archive.
- **2026-05-05 (priority promotion):** Priority promoted P2 LOWER → P2 per user request. Added to ROADMAP §Active wave queue as Wave 21 (Marketing Storytelling). Solo agent bucket spawned to build `ui_kits/kitehub-story-v2/` from Round 1 baseline (archived JSX 546 LOC → HTML kit per Round 2/3 standard). Decision 3 LOWER framing still applies (marketing-only) but execution unblocks GAP-275 (Track 2 KH marketing port) source-of-truth. Closure protocol: agent builds kit folder only; coordinator closure batches `ui_kits/README.md` Status row flip + `ui_kits/index.html` landing card add + GAP status flip + ROADMAP entry, sequentially after Wave 20 Part C (avoid file overlap on `ui_kits/README.md` + `index.html`).
- **2026-05-04:** Filed after user "A" choice on session audit option triplet (file polish gap / port direct from archive / defer). Gap closes ambiguity for GAP-275 (Track 2 KH public marketing port) about source-of-truth — port FROM this kit's output, not raw Round 1 archive.
