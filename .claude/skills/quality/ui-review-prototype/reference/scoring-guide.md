# Scoring Guide — `/128` rubric for HTML Prototype Path

Extends `quality/ui-review/SKILL.md` (which scores running Next.js dev server captures) for the static HTML prototype path under `documents/02-architecture/design-system/ui_kits/**`.

The rubric is the same — **4 dimensions × 4 sub-dimensions × 4 pts × 2 = 128** — but the evidence is the static HTML kit and reviewer self-report (per-screen HTML comments + per-kit README), not Playwright captures.

---

## 1. Rubric (same as `ui-review`)

Each screen scored across 4 dimensions, each with 4 sub-dimensions:

| Dimension | Sub-dim 1 | Sub-dim 2 | Sub-dim 3 | Sub-dim 4 |
|-----------|-----------|-----------|-----------|-----------|
| **Visual** | Layout fidelity | Typography | Color & theme | Responsive |
| **Interaction** | Affordance clarity | State feedback | Keyboard reach | Touch target |
| **Content** | Vietnamese UX | Mock data realism | Empty/error empathy | i18n readiness |
| **Tech** | Component reuse | A11y compliance | Performance signals | Documentation |

Each sub-dimension scored 0–4. Subtotal × 2 = max 128. Target ≥105. Floor 95.

---

## 2. HTML-prototype-specific scoring evidence

Unlike `ui-review` which captures the running app, here the evidence is the kit itself. Look for:

### Visual fidelity
- HTML comments document contrast measurements: `<!-- Contrast: body 14.8:1 (slate-900 on slate-50) AAA -->`
- Color literals come from `colors_and_type.css` HSL vars — NOT inline hex
- Typography uses defined scale — no arbitrary `font-size: 17px`
- Responsive breakpoints documented via Tailwind classes (sm:, md:, lg:)
- Per `dossier/10-acceptance-criteria.md` §1: tested at 320 / 768 / 1440 widths (reviewer verifies in browser walk-through)

### Vietnamese UX
- All copy in Vietnamese (no English fallback shown)
- VN names: `Nguyễn Văn An` (NOT John Doe)
- Currency: `199.000đ` (lowercase đ, dot separator) — never `$`
- Dates: `dd/MM/yyyy` or `2 ngày trước`
- Phone: `0901 234 567` (4-3-3 grouping)
- Class names: `Lớp 10A2` (NOT "Class 1")

### Mock data realism
- Tenant names plausible (`Trung tâm Anh ngữ ABC` / `Trường Quốc tế XYZ`)
- Numbers realistic (25 students/class, not 7 or 500)
- No `test test test` / `abc123` / Lorem ipsum

### Documentation per screen
- HTML comment block at top with: persona, flow ref, score self-estimate, contrast measurements
- No TODO/FIXME left in shipped HTML
- No commented-out code
- State files clearly named (`*-default.html`, `*-loading.html`, etc.)

---

## 3. Skip these checks (vs `ui-review`)

These are NOT relevant to static HTML kits:

- **Next.js build success** — kits don't build, they're hand-coded HTML
- **Hydration warnings** — no React hydration
- **Bundle size measurement** — kits aren't bundled (HTML+CDN-Tailwind+vanilla JS)
- **Server component rendering** — N/A
- **Playwright auto-capture** — the kits ARE the captures (browse them directly via static HTTP server)

---

## 4. Add these checks (vs `ui-review`)

HTML-prototype-specific:

| Check | What to verify |
|-------|----------------|
| **`_v1-baseline/` untouched** | Baseline kits MUST stay frozen as Round 1 reference. Any diff → automatic FAIL. |
| **`_shared/` untouched per agent PR** | Shared CSS / scripts only modified by foundation PRs, never per-kit agents. |
| **Kit README self-report** | README ends with quality-gate self-report per `dossier/prompts.md` §7 |
| **State file naming** | Per dossier §4 `<screen>-<state>.html` (or bare `<state>.html` in component sub-folders) |
| **Persona pill in landing card** | Every kit's landing card displays a persona chip (sanity check via `landing-parity.sh`) |
| **No broken `<a href>`** | Run `link-checker.sh` |

---

## 5. Aggregating to wave-level

Use `documents/04-quality/audits/ui-review/_REVIEW-TEMPLATE.md`:

- §2 deliverable acceptance gate (per-kit avg + min)
- §5 integration smoke test (Tier 1 + Tier 2 + Tier 3 script outputs + browser walk-through)
- §7 quality-gate self-report aggregate (per §1–10 sections × all kits)

Wave verdict:
- **APPROVE** — all kits ≥105 avg, ≥95 min, all 3 scripts green, browser walk-through clean
- **REQUEST CHANGES** — at least one kit <105 avg or script red, but fixable in <1 day
- **ESCALATE** — systemic issues (multiple kits <95, framework drift, persona miss)

---

## 6. Self-report calibration

Per `feedback_audit_calibration.md` — author self-scores typically over by 15-20 pts vs external auditor. When reading kit README self-scores, mentally subtract ~10 pts for honest projection. The /128 rubric naturally tolerates this because target is ≥105 (not ≥120).
