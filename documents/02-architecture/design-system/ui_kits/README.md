# UI Kits — Round 2 + Round 3

Design prototypes for human review. Generated using `documents/02-architecture/design-system/dossier/` as context (PR #667).

**Last Updated:** 2026-05-04

**Review standard:** `.claude/rules/output-review-mandate.md` §3 row "HTML/JSX prototypes" (added 2026-04-29 v1.2.0 via GAP-263 Phase 1).

**Deploy / Review (GitHub Pages):** auto-deploy via `.github/workflows/deploy-design-system.yml` on `push: main` touching `ui_kits/**` (or `workflow_dispatch`). Live gallery: **https://victoraurelius.github.io/2026-Kite-Class-Platform/** . Each kit at `<gallery>/<kit-folder>/` (e.g. `/marketing-site/`).

## Status

| Kit | Wave | Status | Avg score /128 (target ≥105) | Lift vs Round 1 baseline |
|-----|:----:|:------:|:----------------------------:|:------------------------:|
| Foundation | 1 | ✅ DONE — PR #668 | — | — |
| `kiteclass-pro-v2/` | 1 | ✅ DONE — PR #669 | **108.4** | +35 vs ~73 |
| `kiteclass-parent/` | 1 | ✅ DONE — PR #670 | **114** ⭐ | +41 vs ~73 |
| `components/` (G2/G5/G6/G7/G12) | 1 | ✅ DONE — PR #671 | **106.7** | +34 vs ~73 |
| `kitehub-pro-v2/` (Wave 1.5 add-on) | 1.5 | ✅ DONE — PR #673 | **107.8** | +35 vs ~73 |
| `kiteclass-teacher/` (Wave 1.6 add-on) | 1.6 | ✅ DONE — PR #674 | **108.0** | +35 vs ~73 |
| `ai-branding-wizard-v2/` (Wave 1.7 add-on) | 1.7 | ✅ DONE — PR #675 | **115.6** ⭐⭐ | +43 vs ~73 |
| `kiteclass-student/` (Round 3) | 3 | ✅ DONE — PR #700; **external review SHIPPED 2026-05-05 ([Wave 20 Bucket A](../../../04-quality/audits/ui-review/2026-05-05-round-3-kiteclass-student-review.md))** — APPROVE WITH POLISH; polish gap [GAP-363](../../../04-quality/gaps/GAP-363-kiteclass-student-polish-payments-persona-violation.md) (P1 BLOCKING) | self-report **116** → external **100.4** (delta -15.6 ✓ band) | +43 vs ~73 (self) / +27 (external) |
| `kitehub-admin/` (Round 3) | 3 | ✅ DONE — PR #703; **external review SHIPPED 2026-05-05 ([Wave 20 Bucket B](../../../04-quality/audits/ui-review/2026-05-05-round-3-kitehub-admin-review.md))** — APPROVE WITH POLISH; polish gap [GAP-364](../../../04-quality/gaps/GAP-364-kitehub-admin-polish-school-profile-rebuild.md) (P2) | self-report **107.2** → external **101.1** (delta -6.1 — kit had explicit WCAG ratios + MoET citations + realistic VN K-12 mock data, smaller than typical band) | +34 vs ~73 (self) / +28 (external) |
| `kitehub-story-v2/` (Direction A polish) | 21 | ✅ DONE — PR #807 (Wave 21, agent self-report); 6 scroll-driven sections (hero 113 / parallax-features 110 / before-after 108 / một-ngày-chu-trung-tam 112 / mock-dashboard 109 / pricing-cta 107); polished from Round 1 baseline `_v1-baseline/`; closes [GAP-350](../../../04-quality/gaps/GAP-350-round-3-polish-kitehub-story-v2.md) | **109.8** self-report | +37 vs ~73 |

> **Round 3 external review (Wave 20) shipped 2026-05-05:** GAP-348 closure flipped 🟡 PARTIAL — both kits APPROVE WITH POLISH; 3 follow-up gaps filed: [GAP-363](../../../04-quality/gaps/GAP-363-kiteclass-student-polish-payments-persona-violation.md) (P1 BLOCKING — payments.html persona violation), [GAP-364](../../../04-quality/gaps/GAP-364-kitehub-admin-polish-school-profile-rebuild.md) (P2 — school-profile rebuild + 5 polish items), [GAP-365](../../../04-quality/gaps/GAP-365-s-student-tier-1-ac-doc.md) (P2 BL — file Tier-1 `S-student.md` AC doc). **Track 2 Phase 4 ports BLOCKED** until polish gaps close: GAP-269 (student) on GAP-363 + GAP-365; GAP-271 (admin) on GAP-364. Calibration confirmed per `feedback_audit_calibration.md` (delta -15.6 student / -6.1 admin). Track 2 Phase 2 wave-pack plan: [GAP-349](../../../04-quality/gaps/GAP-349-track-2-phase-2-wave-pack-plan.md).

**Wave aggregate:** 7 deliverables × 76 screens × avg **110.5/128** vs Round 1 baseline ~73/128 = **+51% lift**. Review evidence: `documents/04-quality/audits/ui-review/2026-04-29-wave-1-add-ons-review.md`.

## Folder structure

```
ui_kits/
├── README.md                         # this file
├── index.html                        # landing — links to all kits
├── _shared/
│   ├── colors_and_type.css           # tokens (HSL + RGB theme overlay) — Round 1 source
│   ├── assets/                       # kite-mark, kiteclass-logo, kitehub-logo SVG
│   ├── scripts/
│   │   └── capture-screenshots.mjs   # Playwright auto-capture (deferred Phase 2)
│   └── server-runbook.md             # how to start/stop preview server
├── kiteclass-pro-v2/
│   ├── README.md
│   ├── styles.css
│   ├── index.html                    # click-thru navigation between kit screens
│   ├── app.jsx
│   ├── _v1-baseline/                 # Round 1 starting point (preserved for Agent A reference)
│   └── screens/
│       └── (filled by Agent A)
├── kiteclass-parent/
│   ├── README.md
│   ├── styles.css
│   ├── index.html
│   ├── app.jsx
│   ├── manifest.json                 # PWA
│   ├── sw.js                         # Service Worker
│   └── screens/
│       └── (filled by Agent B)
└── components/
    ├── G2-attendance-roster/
    ├── G5-payment-method-selector/
    ├── G6-invoice-detail/
    ├── G7-parent-invite/
    └── G12-bulk-actions-bar/
        (each filled by Agent C)
```

## Preview server

Run from repo root:
```bash
python3 -m http.server 9999 --bind 127.0.0.1 &
# Open: http://127.0.0.1:9999/documents/02-architecture/design-system/ui_kits/
# Stop: pkill -f "http.server 9999"
```

See `_shared/server-runbook.md` for details + alternatives (Vite hot reload, npx serve).

## Acceptance gate (Wave 1 deliverables)

Per `dossier/10-acceptance-criteria.md` (100-item checklist):
- Each screen ≥ 95/128 (floor); each kit avg ≥ 105/128 (target)
- WCAG AA contrast 4.5:1 measured + documented in HTML comment
- Dark mode parity
- Mobile 320 / Tablet 768 / Desktop 1440 — all 3 viewports
- VN mock data only (no Lorem ipsum, no John Doe, no $)
- All states: default / loading / empty / error / success / dark
- No hardcoded hex outside `_shared/colors_and_type.css`
- Persona declared in HTML comment per screen

Self-report per `dossier/prompts.md` §7 format.

## Round 1 reference

Round 1 bundle (from `claude.ai/design` 2026-04-29) preserved at:
- `kiteclass-pro-v2/_v1-baseline/` — Agent A extends this for Round 2 v2
- `documents/07-archived/design-round-1-2026-04-29/` — `kitehub-story` (Direction A future), `ai-branding` (Direction C reference), `mobile-app` (Direction D superseded by web responsive)

## Track 2 — production port

After Round 2 + user acceptance, file gaps GAP-264..267 to port HTML prototypes → real Next.js components in `kitehub-frontend/src/` and `kiteclass-frontend/src/`. Production port is **OUT of scope** for this folder.
