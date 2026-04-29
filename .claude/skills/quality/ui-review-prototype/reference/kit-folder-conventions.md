# Kit Folder Conventions

Conventions every kit under `documents/02-architecture/design-system/ui_kits/` follows. The 3 scripts in this skill rely on these conventions.

---

## 1. Top-level layout

```
documents/02-architecture/design-system/ui_kits/
├── README.md                  # Overview of all kits + rules link
├── index.html                 # Landing page — one card per kit folder
│
├── _shared/                   # Cross-kit assets (NOT a kit)
│   ├── colors_and_type.css    # Single source of HSL color vars + typography scale
│   ├── server-runbook.md      # How to start static HTTP server
│   ├── assets/                # Logos, icons used across kits
│   └── scripts/
│       └── check-ui-kits-landing.sh   # Tier 1 parity script
│
├── _v1-baseline/              # (rare) Round 1 reference (READ-ONLY)
│
├── kiteclass-pro-v2/          # Each kit is a folder named like a slug
├── kiteclass-parent/
├── kiteclass-teacher/
├── kitehub-pro-v2/
├── ai-branding-wizard-v2/
└── components/                # Cross-cutting components kit (different sub-layout)
```

**Critical:**
- `_shared/` and `_v1-baseline/` are NOT kits. The 3 scripts in this skill exclude them via `grep -vE '^_(shared|v1-baseline)$'`.
- Adding a new kit = adding a new top-level folder + a card in `index.html`. Both must land in same PR (or landing-parity.sh fails).

---

## 2. Per-kit layout (standard kit)

```
ui_kits/<kit-slug>/
├── README.md           # Kit purpose, persona, screens, score self-report
├── index.html          # Kit-level click-thru (lists screens within kit)
├── app.jsx             # Production-port skeleton (informational; not built)
├── styles.css          # Kit-specific styles (uses _shared/colors_and_type.css)
├── _v1-baseline/       # Round 1 reference for THIS kit (READ-ONLY)
└── screens/
    ├── <screen>-default.html
    ├── <screen>-loading.html
    ├── <screen>-empty.html
    ├── <screen>-error.html
    ├── <screen>-success.html
    ├── <screen>-dark.html
    └── ...
```

**Naming:** `<screen>-<state>.html` where:
- `<screen>` = primary screen name (e.g., `dashboard`, `attendance-day`, `billing-list`)
- `<state>` = one of: `default`, `loading`, `empty`, `error`, `success`, `dark`, plus variants like `marking`, `saved`, `confetti`

Bare `default.html` (no screen prefix) is allowed when the kit has only one screen.

---

## 3. `components/` kit — special sub-layout

The `components/` kit is cross-cutting (G2 Roster, G5 Payment, G6 Invoice, G7 Parent invite, G12 Bulk actions). Each component is its own subfolder:

```
ui_kits/components/
├── README.md
├── G2-attendance-roster/
│   ├── default.html
│   ├── loading.html
│   ├── empty.html
│   ├── error.html
│   ├── success.html
│   └── spec.md
├── G5-payment-method-selector/
│   └── (same pattern)
├── G6-invoice-detail/
├── G7-parent-invite/
└── G12-bulk-actions-bar/
```

State files use bare names (`default.html`, `loading.html`, etc.) inside each component subfolder because each subfolder IS one screen.

`state-coverage.sh` detects this layout via `find -maxdepth 3` fallback when `screens/` doesn't exist directly.

---

## 4. Special files

| File | Purpose | Lifecycle |
|------|---------|-----------|
| `_v1-baseline/` | Round 1 reference snapshot | Frozen — agents MUST NOT touch |
| `_shared/colors_and_type.css` | HSL color vars + typography scale | Modified by foundation PRs only |
| `_shared/scripts/check-ui-kits-landing.sh` | Tier 1 landing parity | Modified rarely; Tier 2 calls into it |
| `_shared/assets/` | Logos, kite-mark.svg | Add new assets via foundation PR |
| `_partials.html` | Reusable HTML fragments per kit | Optional; ai-branding-wizard-v2 uses this |
| `manifest.json` + `sw.js` | PWA infra (kiteclass-parent only) | Mobile-first kits only |

---

## 5. Per-screen HTML conventions

Every screen file starts with an HTML comment block:

```html
<!--
  Persona: P2 Center Owner (KC) — primary
  Flow ref: dossier/05-business-flows.md Flow #3 daily attendance
  Score self-estimate: 108/128 (avg target ≥105)
  Contrast: body 14.8:1 (slate-900 on slate-50) AAA · muted 4.7:1 AA
-->
```

Required fields:
- **Persona** — names ONE primary persona per `dossier/01-personas.md`
- **Flow ref** — pointer to `dossier/05-business-flows.md` flow ID
- **Score self-estimate** — author's /128 self-score
- **Contrast** — measured contrast ratios (computed manually, documented inline)

`scoring-guide.md` reference uses these self-reports as evidence; reviewer cross-checks against actual visual.

---

## 6. Per-kit README structure

Standard sections:

1. **Overview** — purpose, persona, deliverable target
2. **Screens / Components covered** — list with score self-report
3. **Quality gate self-report** — `/100` (per dossier §"Per-screen acceptance" §1-10) + `/128` aggregate
4. **Cross-references** — flow IDs covered, component-gap IDs reused
5. **Status** — Wave shipped, PR number, merge date

The landing card score (`108.4/128`, `114/128 ⭐`) MUST match the README's "Avg" line — `landing-parity.sh` checks the score is present in the card; reviewer cross-checks consistency manually.

---

## 7. Static HTTP server

Kits are served as static HTML over local HTTP (NOT `file://` — breaks Tailwind CDN cors). Default port 9999. See `_shared/server-runbook.md`.

```bash
cd documents/02-architecture/design-system/ui_kits/
python3 -m http.server 9999 &
# Browse: http://127.0.0.1:9999/
```

No build step. No bundler. Tailwind via CDN. Vanilla JS only. This is intentional — kits are throw-away prototypes for design iteration, NOT production code.

Production port lives in `kiteclass-frontend/` and `kitehub-frontend/` and is tracked separately (post-acceptance gaps GAP-264..267 from foundation PR).

---

## 8. Anti-patterns (will fail review)

| ❌ Don't | ✅ Do |
|---------|------|
| Add new kit folder without landing card | Same PR adds folder + card in `index.html` |
| Modify `_v1-baseline/` | Read-only — copy to new path if iterating |
| Inline hex colors (`color: #2563eb`) | Use HSL var (`color: hsl(var(--primary))`) |
| Mix kit content into `_shared/` | `_shared/` is generic only; kit-specific = inside kit folder |
| Skip persona declaration in HTML comment | Every screen MUST declare persona |
| Use `file://` to view kits | Always `http://127.0.0.1:9999/` (CDN cors) |
| Leave TODO/FIXME in shipped HTML | Clean commits only |
| Use English copy or `John Doe` mock data | Vietnamese UX per dossier §2 |
