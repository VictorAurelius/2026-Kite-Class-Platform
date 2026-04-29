# 06 — Quality Bar

The standard every Round 2 deliverable MUST meet before user accepts it. Round 1 bundle had no measurable quality gate; this dossier sets the threshold.

**Use this when:** self-reviewing a Claude Design output before showing to user. Run through the checklist; if any row fails, fix or report `Y/10 passed. Failed: [list]. Fix or escalate?`

---

## 1. UI Audit `/128` rubric (4 dimensions × 4 sub-criteria × 4 points × 2 = 128 total)

Source: `.claude/skills/quality/ui-review/SKILL.md`. Round 1 bundle screens averaged ~73/128. Round 2 target: **≥100/128 per screen**.

### Dimension 1 — Tech (32 points)

| Sub-criterion | 0 | 1 | 2 | 3 | 4 |
|---------------|---|---|---|---|---|
| Responsive 320 / 768 / 1440 | broken | 1 viewport | 2 viewports | 3 viewports + minor issue | 3 viewports flawless |
| Dark mode parity | none | broken | partial | works with minor color drift | full parity |
| Loading state | none | spinner only | skeleton in 1 area | skeleton everywhere | skeleton + smooth transition |
| Error state | crashes | generic message | branded message | branded + retry CTA | branded + retry + error context |
| Empty state | none | text only | text + icon | text + icon + CTA | full empty illustration + CTA + helpful copy |
| Performance perception | janky | OK | smooth | smooth + perceived <1s | smooth + actual <1s |
| Code splitting | none | partial | per-route | per-route + lazy components | optimized + bundle <250KB First Load JS |
| Accessibility (keyboard, ARIA, focus) | broken | partial | most works | all works | a11y audit clean |

### Dimension 2 — Heuristics (Nielsen 10 → 10 sub × 4 = 40 points → normalized to 32)

| Sub-criterion | Test |
|---------------|------|
| Visibility of system status | does user see what's happening? |
| Match real world | language matches user's vocabulary (Vietnamese here) |
| User control + freedom | undo, cancel, escape paths |
| Consistency + standards | matches platform conventions (shadcn / web) |
| Error prevention | confirm dangerous actions, validate before submit |
| Recognition over recall | visible options, not memorized |
| Flexibility | shortcuts for power users (⌘K) |
| Aesthetic + minimalist | no superfluous content |
| Help users recognize errors | plain-language, actionable |
| Help + documentation | inline hints, tooltips |

### Dimension 3 — Aesthetics (32 points)

| Sub-criterion | 0–4 |
|---------------|-----|
| Visual hierarchy clarity | titles > sections > content |
| Typography (scale, leading, kerning) | matches `colors_and_type.css` |
| Color use (primary/accent/semantic) | per `02-vietnamese-ux-musts.md` |
| Whitespace + density | breathing room, not crowded |
| Iconography (lucide consistency) | uniform style + size |
| Motion (purposeful, not decorative) | enters/exits, hover lift |
| Photography / illustrations (if any) | brand-coherent, VN context |
| Polish (micro-interactions, easter eggs) | confetti on success, etc. |

### Dimension 4 — UX (32 points)

| Sub-criterion | 0–4 |
|---------------|-----|
| Persona alignment | targets specified persona (see `01-personas.md`) |
| Task completion (golden path) | smooth, no dead-ends |
| Cognitive load | one screen = one job |
| Information density (matches persona) | dense for admin, sparse for parent |
| Forgiveness (undo, recover) | easy to fix mistakes |
| Real data shape (no Lorem ipsum) | mock from `05-business-flows.md` |
| Localization quality | natural Vietnamese, not Google-translated |
| Edge cases handled (long names, missing data) | doesn't break |

**Total: 32 + 32 + 32 + 32 = 128. Target ≥100 per screen for Round 2 acceptance.**

---

## 2. Accessibility — WCAG AA mandatory

Every screen must pass:

| Check | Threshold | Tool |
|-------|-----------|------|
| Text contrast | 4.5:1 (body), 3:1 (large 18px+) | axe DevTools or Stark |
| Non-text contrast | 3:1 (UI components) | manual |
| Keyboard navigation | All interactive elements reachable | manual `Tab` / `Shift+Tab` |
| Focus indicator | Visible (≥2px outline, contrasts) | manual |
| Form labels | Every input has `<label>` or `aria-label` | axe |
| Image alt text | Descriptive `alt`, not "image1.jpg" | manual |
| Heading hierarchy | h1 → h2 → h3 (no skips) | manual |
| Touch target size | 44×44 minimum (mobile) | manual |
| Color-not-only | Status conveyed by icon + text, not just color | manual |
| Reduced motion | `prefers-reduced-motion` respected for animations | manual |

**Round 2 deliverable note:** Each Claude Design output MUST include a comment block at the top of HTML showing measured contrast for primary text/bg combos:

```html
<!--
  Contrast ratios (measured):
  - Body on bg-card: 14.8:1 (AAA ✓)
  - Muted-fg on bg-card: 4.7:1 (AA ✓)
  - Primary on white: 4.5:1 (AA ✓)
  - Accent on white: 4.6:1 (AA ✓)
-->
```

---

## 3. Performance budget

| Metric | KH | KC |
|--------|----|----|
| First Load JS (per route) | < 250 KB | < 250 KB (CI gate enforced — GAP-236) |
| LCP (Largest Contentful Paint) | < 2.5s | < 2.5s |
| CLS (Cumulative Layout Shift) | < 0.1 | < 0.1 |
| INP (Interaction to Next Paint) | < 200ms | < 200ms |
| TTFB | < 800ms | < 800ms |

**Round 2 designs must:**
- Use lazy-load for below-the-fold sections
- Specify image dimensions to prevent CLS
- Avoid `motion-safe` heavy animations on mobile
- Document any 3rd-party SDK that adds >50KB (e.g., HCaptcha)

---

## 4. Internationalization

| Rule | Detail |
|------|--------|
| Default locale | `vi-VN` |
| Fallback | `en-US` (placeholder text only — not customer-facing) |
| Date / time | per `02-vietnamese-ux-musts.md` §1 |
| Currency | per `02-vietnamese-ux-musts.md` §1 |
| RTL support | not required (VN is LTR) |
| Pluralization | Vietnamese has no plural form — 1 student, 5 student |
| String externalization | All UI copy in `messages/vi.json` (not hardcoded JSX) |

**Round 2 deliverable:** every page shows the i18n key in a comment, e.g.:

```jsx
<h1>{/* i18n: dashboard.greeting.morning */}Chào buổi sáng, An 👋</h1>
```

---

## 5. Mock data quality

NO Lorem ipsum. NO `John Doe`. NO US zip codes. NO `123 Main St`.

| Field | Bad | Good |
|-------|-----|------|
| Names | John Doe / Jane Smith | Nguyễn Văn An / Trần Thị Hương / Lê Minh Tuấn |
| Addresses | 123 Main St | 123 Nguyễn Văn Cừ, P. Phước Long B, Q. 9, TP. HCM |
| Phone | (555) 123-4567 | 0901 234 567 |
| Email | john@example.com | nguyen.an@gmail.com / contact@trungtam-eduplus.vn |
| Currency | $99.00 | 199.000đ |
| Class names | Class 1, Class 2 | Lớp 10A2, Lớp 1A1, Toán nâng cao K10 |
| Course names | Course Title | Khóa luyện thi THPT Quốc gia 2026 |
| Tenant names | Acme Corp | Trung tâm Toán Master / Trường THCS-THPT EduPlus |

---

## 6. Dark mode

**Mandatory** for every screen (KH first-class, KC at parity).

Test that EVERY token has dark variant:
- Background: `bg-background` (white/gray-950)
- Card: `bg-card`
- Primary: same hue, adjusted lightness
- Borders: visible in both modes
- Shadows: shift to inset glow in dark

If dark mode looks "ported" or has color drift, score 1/4 max for that sub-criterion. Native dark mode designs win.

---

## 7. Persona density discipline

Don't ship one density to all personas:

| Persona | Density | Layout |
|---------|---------|--------|
| P1 Solo Teacher | sparse | 1-2 columns, mobile-friendly |
| P2 Center Owner | medium | 2-3 columns, desktop dashboards OK |
| P3 Medium Center Admin | dense | 3-4 columns, data tables, bulk actions |
| P5 K–12 Principal | dense + hierarchical | sidebar nav with deep nesting |
| Student | sparse + visual | 1 column mobile, big type, illustrations |
| Parent | very sparse | 1 column mobile, hero metric per screen |

If Claude Design ships P2 dashboard with same density as Parent screen, score 0/4 on UX persona alignment.

---

## 8. Design output deliverable structure

Each Round 2 deliverable should ship as:

```
ui_kits/[name]/
├── README.md           # what this kit covers, persona, screens included
├── styles.css          # imports colors_and_type.css + kit-specific overrides
├── index.html          # multi-screen demo with click-thru navigation
├── app.jsx             # main React-ish demo component (or components.jsx)
└── screens/            # per-screen HTML files for full-fidelity capture
    ├── home.html
    ├── empty.html
    ├── loading.html
    ├── error.html
    └── ...
```

Each screen file MUST embed the contrast comment block (see §2).

---

## 9. Acceptance gate

Before user reviews Round 2 output, Claude Design self-reports:

```
Deliverable: kiteclass-teacher

UI score self-estimate: 105/128 (target: ≥100)
WCAG AA: 8/10 checks pass (failed: focus indicator on toggle, reduced-motion missing)
Performance: bundle estimate ~180KB ✓
i18n: all keys externalized ✓
Mock data: VN names + phone + currency ✓
Dark mode: parity confirmed ✓
Persona: P3 Admin density ✓

Failed: focus indicator + reduced-motion. Fix or escalate?
```

User decides: ship, fix, or escalate (back to dossier discussion).

---

## 10. Anti-patterns that auto-fail

If any of these appear, score the screen **0/128** regardless of other quality:

- Lorem ipsum copy
- US-format dates `MM/dd/yyyy`
- US currency `$`
- English-only error messages
- Stock photos of Western classrooms
- "John Doe" / "Jane Smith"
- Hardcoded colors not from `colors_and_type.css`
- Missing dark mode entirely
- One screen design with no states (just default)
- Carousel as primary navigation (cognitive load)
- Modal-on-modal (UX death spiral)
