# G4 Class Schedule Manager — Component Spec (production port)

**Source spec:** [`documents/02-architecture/design-system/ui_kits/components/G4-class-schedule-manager/README.md`](../../../../../documents/02-architecture/design-system/ui_kits/components/G4-class-schedule-manager/README.md)
**Component gap:** G4 per `dossier/04-component-gaps.md` §G4
**Tracking gap:** [`GAP-273`](../../../../../documents/04-quality/gaps/GAP-273-track-2-port-12-components-shared-lib.md) — stays 🟡 PARTIAL after this port.
**Wave:** 28 Bucket B (paired with G3/G8/G10/D1 in same wave per `wave-track-2-ui-kits-port-umbrella.md`).
**Used by:** KC `/classes/[id]/schedule` (Principal / Owner / Teacher).

---

## What this PR ships

- `<ClassScheduleManager>` React component covering 5 spec'd states: `empty`, `single-class`, `recurring-edit`, `conflict-warning`, `saved`.
- `detectConflicts(slots)` pure helper — pairwise overlap detection (back-to-back NOT a conflict; partial / full / 1-min overlap = conflict).
- TypeScript types exported on the public `@kite/shared-ui` API: `ClassScheduleManager`, `ClassScheduleManagerProps`, `RecurrenceRule`, `WeekDay`, `ScheduleSlot`, `ConflictWarning`, `detectConflicts`.
- Vitest coverage: 11 utility tests (overlap edge cases) + 9 component tests (state-render + interaction wiring) = **20 tests** for G4. Wave 27's 108-test baseline stays green.

## State / view mapping (mirrors HTML protos)

| `state` prop | Render |
|---|---|
| `empty`            | Border-dashed CTA card + 3 quick-preset cards (3-buổi/tuần / Hằng ngày / Cuối tuần) |
| `single-class`     | Week grid (T2..CN headers) with one slot row |
| `recurring-edit`   | Form: subject + day toggles (T2..CN, MON-first) + time-from/to + recurrence-end (date OR count radio) + holiday note |
| `conflict-warning` | `role="alert"` panel listing conflict summary + 3 resolution options per conflict (change-teacher / change-time / skip-day) |
| `saved`            | Saved banner + stat strip (sessions/week, hours/week, subjects, teachers) + week grid + class list aside |

## Vietnamese UX (kit README §VN UX)

- Week starts MONDAY (T2 = first column, NEVER Sunday).
- Day labels: T2/T3/T4/T5/T6/T7/CN — short pills + `Thứ Hai/Ba/Tư/.../Chủ nhật` long form in week grid.
- Weekend cols (T7, CN) styled in info-blue (`text-info` Tailwind class).
- Time 24h `HH:mm` — `14:00 – 15:30` with en-dash + spaces.
- Class names: `Toán nâng cao`, `Văn 6`, `Anh văn` (NEVER "Math 1").
- Recurrence-end mode: mutually exclusive radio between date end (`endsOn`) and count end (`endsAfterOccurrences`).

## Conflict detection contract

`detectConflicts(slots)` is **pairwise** over `i < j`. For each emitted `ConflictWarning`:

- `slotAId` always references the earlier slot in the input array; `slotBId` the later.
- `summary` is pre-formatted VN copy (`dd/MM/yyyy · HH:mm – HH:mm`); the UI renders verbatim.
- `reason` cites the OTHER slot's class name + teacher (when present).

The component does NOT compute conflicts itself — it consumes a caller-supplied `conflicts` prop. Callers are free to run `detectConflicts` client-side OR feed server-computed conflicts in. This mirrors the controlled shape of the other Wave 27 ports.

## Accessibility (WCAG AA)

- Day toggles: `role="group"` with `aria-pressed` per button.
- Conflict alert: `role="alert"`, color is never the only signal — icon + dashed border + label always co-occur.
- Touch targets ≥44×44 (`min-w-[44px] min-h-[44px]` on every button + input).
- `prefers-reduced-motion` honoured — no animations on conflict highlight (opacity / border only).
- Saved banner uses `role="status"` + `aria-live="polite"` so screen readers announce save completion.

## What this PR does NOT ship (deferred)

- **Recurrence projection engine** — `daysOfWeek` × `endsOn` / `endsAfterOccurrences` → per-day expansion is a caller concern in v1. (Most schools have ≤ 50 slots / class / school year — server-side projection is fine.)
- **Holiday auto-overlay engine** — caller supplies `daysOfWeek`; the spec's holiday list (Tết, Giỗ tổ Hùng Vương, 30/4, 2/9) is informational only in this PR.
- **Drag-to-reschedule** in the saved week grid — kept read-only in v1.
- **Excel/CSV export** — only PDF wired (`onExportPdf`); CSV deferred.
- **Wiring into production routes** (`kiteclass-frontend/src/app/classes/[id]/schedule/page.tsx`) — host-app concern in a follow-up PR.

## Acceptance criteria status (mapping to GAP-273 AC)

- [x] Component ported with TypeScript types
- [x] `spec.md` mirror committed
- [x] Unit tests per state + props edge cases (20 tests for G4)
- [x] Vietnamese-only labels (verbatim from kit README)
- [x] Week starts Monday (T2 first column)
- [x] Recurrence-end mode mutually exclusive (date OR count radio)
- [x] Conflict-warning displays affected slot details (date + time range + class name)
- [x] WCAG AA: 44×44 targets, role="alert", `aria-pressed` on toggles
- [x] No new deps
- [ ] All 12 components ported — 5/12 in this PR (G2 + G5 + G6 + G7 + G4); 7 remaining
- [ ] Storybook / `/dev/components/` route — out of scope for this PR
- [ ] Production usage ≥105/128 verification — needs host-app wiring + UI review run
- [ ] Visual regression baseline — captured separately under post-wave audit

GAP-273 stays 🟡 PARTIAL.
