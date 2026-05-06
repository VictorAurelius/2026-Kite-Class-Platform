# G9 Instance Lifecycle Status — Component Spec (production port)

**Source spec:** [`documents/02-architecture/design-system/ui_kits/components/G9-instance-lifecycle/README.md`](../../../../../documents/02-architecture/design-system/ui_kits/components/G9-instance-lifecycle/README.md) + 6 state HTML files under `states/`.
**State machine source of truth:** [`.claude/rules/ai-branding-guidelines.md`](../../../../../.claude/rules/ai-branding-guidelines.md) §6 Lifecycle State Machine.
**Component gap:** G9 per [`dossier/04-component-gaps.md`](../../../../../documents/02-architecture/design-system/dossier/04-component-gaps.md) §G9.
**Tracking gap:** [`GAP-273`](../../../../../documents/04-quality/gaps/GAP-273-track-2-port-12-components-shared-lib.md) — final 4-of-12 G* components ported in Wave 29.
**Wave:** 29 Bucket B (G9 — paired with G1 Bucket A, G11 Bucket C, G12 Bucket D).
**Replaces:** Empty `/instances/[id]` skeleton (33/128 R1) + matching `/admin/instances/[id]` admin view.

---

## What this PR ships

- `<InstanceLifecycleStatus>` React component covering all 6 spec'd states (`NOT_STARTED`, `INITIALIZING`, `GENERATING`, `DEPLOYED`, `REGENERATING`, `FAILED`).
- `InstanceLifecycleState` union (uppercase snake) — wire-compatible with the backend `InstanceLifecycleService` enum.
- `LifecycleEvent` shape: `from` + `to` + `timestamp` (ISO-8601 string) + optional `reason` / `actor` — one event per `<li>` in the timeline `<ol>`.
- `validTransition(from, to)` pure helper — returns `true` only for transitions allowed by `ai-branding-guidelines.md` §6.  Encoded as a frozen adjacency map (NOT a status switch — per `design-patterns.md` §3.3).
- TypeScript types exported on the public `@kite/shared-ui` API: `InstanceLifecycleStatus`, `validTransition`, `InstanceLifecycleStatusProps`, `InstanceLifecycleState`, `LifecycleEvent`.
- Vitest coverage: 12 RTL component tests + 11 utils tests covering legal + illegal transitions + retry CTA visibility + timeline rendering + Vietnamese datetime format.

## State machine (verbatim from `ai-branding-guidelines.md` §6)

```
NOT_STARTED -> INITIALIZING -> GENERATING -> DEPLOYED <-> REGENERATING
                  |               |             ^
                FAILED <----- FAILED ---------+ (retry)
```

Encoded in `utils.ts` as `TRANSITION_GRAPH`:

| From | Allowed `to` |
|------|--------------|
| `NOT_STARTED` | `INITIALIZING`, `FAILED` |
| `INITIALIZING` | `GENERATING`, `FAILED` |
| `GENERATING` | `DEPLOYED`, `FAILED` |
| `DEPLOYED` | `REGENERATING` |
| `REGENERATING` | `DEPLOYED`, `FAILED` |
| `FAILED` | `GENERATING` (retry path — bypasses INITIALIZING since analyser result is reused) |

Self-transitions and unknown-state inputs return `false` fail-safe.

## State / pill mapping

| State | Pill copy | Pill colour | Banner | Other UI |
|-------|-----------|-------------|--------|----------|
| `NOT_STARTED` | `Chưa khởi tạo` | muted | none | empty timeline copy |
| `INITIALIZING` | `Đang khởi tạo` | info (pulse) | none | — |
| `GENERATING` | `Đang tạo` | warning (pulse) | none | — |
| `DEPLOYED` | `Đã triển khai` | success | none | live URL + "Truy cập" link |
| `REGENERATING` | `Đang tạo lại` | info (pulse) | none | — |
| `FAILED` | `Lỗi` | destructive | `role="alert"` banner | optional retry CTA when `onRetry` provided |

## Vietnamese formatting

- State labels per kit README §States table; copy verbatim:
  - `Chưa khởi tạo` / `Đang khởi tạo` / `Đang tạo` / `Đã triển khai` / `Đang tạo lại` / `Lỗi`.
- Datetime: `dd/MM/yyyy HH:mm:ss` (e.g. `29/04/2026 07:01:00`) — UTC accessors so test fixtures + production match across timezones; matches kit README §Vietnamese UX.
- Reason copy is caller-supplied (typically the backend's quality-gate or error message).
- Vietnamese-only labels per CLAUDE.md §Communication Language.

## Retry CTA visibility

Three independent conditions all required:

1. `state === 'FAILED'`
2. `onRetry` prop is a function (not undefined)
3. The retry button is wrapped inside the `role="alert"` banner so screen readers announce "Có lỗi xảy ra" + "Thử lại" together.

When state ≠ FAILED, the retry CTA never renders (regardless of `onRetry`).  When `onRetry` is omitted in FAILED state, the alert banner still renders but without the CTA — the caller is presenting a read-only failure view (admin observer mode).

## Accessibility (WCAG AA)

- Status pill carries `role="status"` + `aria-label` with the pill copy so screen readers announce state.
- FAILED-state alert banner carries `role="alert"` + `aria-live="polite"`.
- Timeline rendered as semantic `<ol aria-label="Timeline trạng thái instance">` with one `<li>` per event.
- Last event in the timeline carries `aria-current="step"` on its icon span (the icon glyph itself is `aria-hidden`; meaning is conveyed through the title text).
- Step icon glyphs (`◯` / `⏳` / `✓` / `🔁` / `✗`) are `aria-hidden` decorative.
- Live URL "Truy cập" link uses `target="_blank" rel="noopener noreferrer"`.
- Retry button is a `<button type="button">` with focus-visible ring.
- Pulse animations are gated by `motion-safe:` so users with `prefers-reduced-motion: reduce` see no movement.

## Design-pattern compliance

Per `.claude/rules/design-patterns.md`:

- **State Pattern (lookup map, not switch)** — the per-state visual treatment lives in `STATE_VISUAL` (frozen `Record<InstanceLifecycleState, ...>`).  Adding a 7th state means adding a row.  No `if (state === 'X') ... else if ...` cascades.
- **No primitive obsession** — `LifecycleEvent.from` and `.to` are `InstanceLifecycleState` typed unions, never bare strings.
- **No god component** — the file is ~250 lines; renders only.  The state-machine validator lives in a separate `utils.ts` module so it can be imported server-side without React.
- **Outbox / event-publishing concerns** are backend's responsibility (per AI Branding §6); this component only renders the events the backend reports.

## What this PR does NOT ship (deferred)

- Live progress bars (per-asset progress in GENERATING — `logo done, banner 62%, hero pending` from `states/generating.html`) — caller renders these alongside the status block; this component is a presentational shell + timeline.
- SSE consumer wiring — handled by host app `kitehub-frontend/src/lib/instance-events.ts` (existing) per kit README §Reuse.
- Admin-only collapsible error log + admin-forced state override — admin view scope, separate component / follow-up.
- Health metrics 4-card panel for DEPLOYED state — separate component / follow-up.
- Side-by-side current-vs-preview for REGENERATING — separate component / follow-up.
- Wiring into production routes (`kitehub-frontend/.../instances/[id]/page.tsx`, `.../admin/instances/[id]/page.tsx`) — host-app concern in a follow-up PR.
- Other Wave 29 components (G1, G11, G12) — separate buckets in this wave under [GAP-273](../../../../../documents/04-quality/gaps/GAP-273-track-2-port-12-components-shared-lib.md).

## Acceptance criteria status (mapping to GAP-273 AC)

- [x] State machine matches `ai-branding-guidelines.md` §6 verbatim — `validTransition` validates the graph; tests assert each row.
- [x] Status badge with VN labels per kit README §States table (6 labels).
- [x] Retry CTA only when `state === 'FAILED'` (and `onRetry` provided) — covered by 3 component tests.
- [x] Event timeline scroll showing transitions — `<ol>` with one `<li>` per event; chronological order.
- [x] Component ported with TypeScript types
- [x] `spec.md` mirror committed
- [x] Unit tests per state + transition + retry visibility (12 component + 11 utils = 23 tests; AC requested ≥9)
- [x] Vietnamese-only labels
- [ ] All 12 components ported — 9/12 after Wave 29 ships (Wave 27 shipped 4, Wave 28 shipped 5; this Wave 29 Bucket B ships G9; G1/G11/G12 ship in this wave's other buckets).
- [ ] Storybook / `/dev/components/` route — out of scope for this PR.
- [ ] Production usage ≥105/128 verification — needs host-app wiring + UI review run.

GAP-273 stays 🟡 PARTIAL until all 12 G* + D1 are ported AND wired into production routes.
