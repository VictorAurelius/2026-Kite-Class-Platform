# 10 — Acceptance Criteria

The exact checklist Round 2 deliverables get tested against before user accepts. If any row fails, Claude Design self-reports and asks "fix or escalate?"

**Use this when:** validating a deliverable before showing to user. Run through every section. Be honest — false positives are cheap, false acceptance is expensive.

---

## Per-screen acceptance (each HTML file in `screens/`)

### Section 1 — Visual fidelity (10 points)

- [ ] Renders correctly at 320px width (no horizontal scroll, no overlap)
- [ ] Renders correctly at 768px width (tablet layout)
- [ ] Renders correctly at 1440px width (desktop layout)
- [ ] Light mode: visual hierarchy clear (titles → sections → content)
- [ ] Dark mode: same fidelity as light mode
- [ ] Typography matches `colors_and_type.css` scale (no arbitrary `font-size: 17px`)
- [ ] Colors match HSL vars (no hardcoded hex outside the `colors_and_type.css` source)
- [ ] Icons all from lucide library (no mixed sources)
- [ ] Spacing follows 4px Tailwind scale
- [ ] No Lorem ipsum, no placeholder text

### Section 2 — Vietnamese UX (10 points)

Per `02-vietnamese-ux-musts.md`:

- [ ] All copy in Vietnamese (no English fallback shown to user)
- [ ] Address user as `bạn` (informal you)
- [ ] Currency format: `199.000đ` (lowercase đ, dot separator)
- [ ] Date format: `dd/MM/yyyy` or relative ("2 ngày trước")
- [ ] Time: 24-hour `HH:mm`
- [ ] Phone: `0901 234 567` (4-3-3 grouping)
- [ ] Names: Vietnamese (Nguyễn Văn An / Trần Thị Hương / etc.)
- [ ] Class names: `Lớp 10A2` style (NOT "Class 1")
- [ ] Sentence case for headings (NOT Title Case)
- [ ] Empty/error/success copy is empathetic, not robotic

### Section 3 — Accessibility (10 points)

Per `06-quality-bar.md` §2:

- [ ] Body text contrast ≥ 4.5:1 (measured + documented in HTML comment)
- [ ] Large text contrast ≥ 3:1
- [ ] Non-text contrast ≥ 3:1 (UI components, focus rings)
- [ ] All interactive elements keyboard-reachable (Tab order makes sense)
- [ ] Focus indicator visible (≥2px outline, 3:1 contrast)
- [ ] Form inputs have `<label>` or `aria-label`
- [ ] Heading hierarchy correct (h1 → h2 → h3, no skips)
- [ ] Touch targets ≥ 44×44px on mobile
- [ ] Status not conveyed by color alone (icon + text)
- [ ] `prefers-reduced-motion` respected for animations

### Section 4 — States (10 points)

Each screen MUST have these state variants in `screens/`:

- [ ] `default.html` — normal loaded state
- [ ] `loading.html` — skeleton or spinner
- [ ] `empty.html` — no data + CTA to populate
- [ ] `error.html` — error message + retry CTA
- [ ] `success.html` — confirmation state (after CRUD action)
- [ ] Each state passes Section 1 (visual fidelity)
- [ ] Each state passes Section 2 (VN UX)
- [ ] Each state passes Section 3 (accessibility)
- [ ] Empty states have icon + helpful copy + primary CTA
- [ ] Error states distinguish recoverable vs unrecoverable

### Section 5 — Persona alignment (10 points)

Per `01-personas.md` × `06-quality-bar.md` §7:

- [ ] Screen designed for ONE primary persona (named in HTML comment)
- [ ] Density matches persona (sparse for parent, dense for admin)
- [ ] Vocabulary matches persona's tech literacy (no jargon for parent)
- [ ] Time-of-day context considered (parent evening = relaxed, admin business-hours = task-focused)
- [ ] Device target matches persona (mobile for parent/student, desktop for admin)
- [ ] Touch UX vs mouse UX appropriate for device
- [ ] Information depth matches persona's needs (parent sees overview, admin sees detail)
- [ ] CTA hierarchy reflects persona priorities
- [ ] Error tolerance matches (parent forgives slow loads, admin needs instant)
- [ ] Examples in mock data plausible for persona

### Section 6 — Data realism (10 points)

Per `05-business-flows.md`:

- [ ] Mock data uses VN names (no John Doe)
- [ ] Phone numbers VN format
- [ ] Currency in đ (no $)
- [ ] Dates dd/MM/yyyy
- [ ] Class/course names Vietnamese
- [ ] Tenant names plausible (Trung tâm X / Trường Y)
- [ ] Email addresses plausible (`@gmail.com` / `@trungtam-eduplus.vn`)
- [ ] Numbers realistic (e.g., 25 students/class — not 7 or 500)
- [ ] Statuses match business flows in dossier
- [ ] No "test test test" or "abc123"

### Section 7 — Component reuse (10 points)

Per `09-tech-constraints.md`:

- [ ] Buttons use shadcn pattern (variants: default/secondary/destructive/outline/ghost/link)
- [ ] Inputs use shadcn pattern (proper label, error states, helper text)
- [ ] Cards use shadcn pattern (`rounded-2xl border bg-card shadow-soft`)
- [ ] Dialogs use Radix-based shadcn dialog
- [ ] No custom CSS for components shadcn already provides
- [ ] Icons use lucide-react syntax (`<Icon name="..." />` or `<IconName />`)
- [ ] Class merging via clsx/tailwind-merge pattern
- [ ] Form structure uses react-hook-form pattern (FormField + FormItem + FormControl)
- [ ] Tables use TanStack Table pattern (columns + data + filters)
- [ ] No Bootstrap, MUI, or other framework markup leaked in

### Section 8 — Performance signals (10 points)

Per `06-quality-bar.md` §3:

- [ ] Above-fold content prioritized (no 3-screen-tall hero before content)
- [ ] Images have explicit dimensions (no CLS)
- [ ] Lazy-load below-fold sections
- [ ] No 3rd-party SDK > 50KB without justification
- [ ] No autoplay video, no auto-rotating carousel
- [ ] Animation respects `prefers-reduced-motion`
- [ ] Avoid >3 web fonts (Inter + JetBrains Mono = 2, OK)
- [ ] No icon font (lucide is SVG — good)
- [ ] Estimate bundle size in HTML comment (e.g., `<!-- est. 180 KB First Load JS -->`)
- [ ] Mobile-friendly (no hover-only interactions critical to flow)

### Section 9 — i18n readiness (10 points)

Per `06-quality-bar.md` §4:

- [ ] All UI copy externalizable (no inline hardcoded English)
- [ ] i18n key shown in HTML comment for each visible string
- [ ] Date/time/number formatters use locale-aware utilities
- [ ] Currency formatter uses locale-aware utility
- [ ] Plural forms documented (Vietnamese has none, but EN fallback might)
- [ ] Long-string overflow handled (Vietnamese is ~30% longer than English)
- [ ] RTL not needed (note in comment if implementing for future)
- [ ] No string concatenation in JSX (use templates)
- [ ] Dynamic content interpolated (`{name} đã đăng nhập`, not `${name} signed in`)
- [ ] Error messages localized in mock

### Section 10 — Documentation (10 points)

Each deliverable kit MUST ship with:

- [ ] `README.md` explaining: purpose, persona, screens included, links to dossier files used
- [ ] HTML comment block at top of each screen with: persona, flow ref, score self-estimate, contrast measurements
- [ ] Inline comments explaining non-obvious choices
- [ ] State files clearly named (`default.html`, `loading.html`, etc.)
- [ ] Reference to component-gap IDs (e.g., `<!-- uses G2 Attendance Roster -->`)
- [ ] Reference to flow IDs (e.g., `<!-- Flow #3 daily attendance -->`)
- [ ] Quality gate self-report at end of README:
  - "X/10 passed in §N. Failed: [list]. Fix or escalate?"
- [ ] No TODO/FIXME left in shipped HTML
- [ ] No commented-out code (clean output)
- [ ] Sample data document accompanies kit if tabular data used

---

## Per-deliverable acceptance (kit-level)

For `kiteclass-pro` / `kiteclass-teacher` / `kiteclass-parent` / `ai-branding-wizard-v2` / `kitehub-story` (Direction A) — each kit must:

- [ ] Cover all priority screens listed for that direction in `08-direction-decisions.md`
- [ ] Average score ≥ 105/128 across all screens (per `06-quality-bar.md`)
- [ ] No screen below 95/128 (target floor)
- [ ] All §1–10 sections above pass per screen
- [ ] Cross-screen navigation works (click-thru in `index.html`)
- [ ] Component reuse documented (not 5 different button styles per kit)
- [ ] Consistent typography scale across screens
- [ ] Consistent spacing rhythm across screens
- [ ] Dark mode variant of every screen
- [ ] Mobile responsive on every screen (320 / 768 / 1440)

---

## Round 2 deliverable acceptance gate (paste this back to Claude Design)

Round 2 is accepted when:

```
Deliverable matrix (must all return ≥95/128):

| Kit | Avg score | Min screen | States ✓ | Persona ✓ | Mock VN ✓ | Quality gate |
|-----|:---------:|:----------:|:--------:|:---------:|:---------:|:------------:|
| kiteclass-pro       |  XXX/128  |   YYY/128  |    Y/N   |    Y/N    |    Y/N    |    Y/N      |
| kiteclass-teacher   |  XXX/128  |   YYY/128  |    Y/N   |    Y/N    |    Y/N    |    Y/N      |
| kiteclass-parent    |  XXX/128  |   YYY/128  |    Y/N   |    Y/N    |    Y/N    |    Y/N      |
| ai-branding-wizard  |  XXX/128  |   YYY/128  |    Y/N   |    Y/N    |    Y/N    |    Y/N      |
| kitehub-story       |  XXX/128  |   YYY/128  |    Y/N   |    Y/N    |    Y/N    |    Y/N      |

Component coverage:
- 12 component gaps (G1-G12) addressed: X/12

Flows covered:
- 10 business flows (Flow 1-10) demoed: X/10

Pain points addressed:
- 14 lowest-scoring screens (from §07): X/14 redesigned with score lift > 25 points

Mobile-tech-decision file:
- Provided: Y/N
- Recommendation: [PWA / RN / Flutter / undecided]

Open questions for user:
- [list]
```

If any row is **N** or score below threshold, Claude Design must propose targeted fixes OR escalate to user.

---

## Anti-patterns that auto-fail (any one of these = reject deliverable)

- Lorem ipsum
- US dates
- $ currency
- John Doe / Jane Smith
- English-only error messages
- Missing dark mode entirely on a screen
- One screen design with no states (just default)
- Score self-estimate misses contrast measurement (lazy quality gate)
- Component duplication (e.g., 3 different "card" styles in same kit)
- Persona unspecified in screen comment
- Mock data hardcoded inline JSX (not separated to data file or const)
