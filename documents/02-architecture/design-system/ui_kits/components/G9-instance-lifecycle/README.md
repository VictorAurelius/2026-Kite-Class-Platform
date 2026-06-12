# G9 — Instance Lifecycle Status

**Component gap:** G9 per `documents/02-architecture/design-system/dossier/04-component-gaps.md` §G9
**Flow ref:** `.claude/rules/ai-branding-guidelines.md` §6 Lifecycle State Machine
**Used by screens:** KH `/instances/[id]` (was 33/128 🔴 R1), KH `/admin/instances/[id]`
**Persona:** P2 Center Owner (waiting for provisioning), Internal Admin (debugging)

---

## Purpose

Visualize the 6 lifecycle states an AI Branding instance moves through, from owner-clicked-Bắt-đầu to deployed (and any retry/regenerate cycles). Replaces the current empty `/instances/[id]` skeleton (33/128) with a state-aware page that owners can leave open or close (email notifies on completion).

State machine (per `ai-branding-guidelines.md` §6):

```
NOT_STARTED -> INITIALIZING -> GENERATING -> DEPLOYED <-> REGENERATING
                  |               |             ^
                FAILED <----- FAILED ---------+ (retry)
```

Transitions go ONLY through `InstanceLifecycleService`; each transition publishes a RabbitMQ event. SSE streams live progress to the page.

---

## States

| File | State | UI summary |
|------|-------|-----------|
| `states/not-started.html` | `NOT_STARTED` | Hero with wizard CTA + 4-stage primer |
| `states/initializing.html` | `INITIALIZING` | Progress 22%, sub-steps (subdomain, DB, storage, TLS) |
| `states/generating.html` | `GENERATING` | Progress 68%, per-asset bars (logo done, banner 62%, hero pending), cancel CTA |
| `states/deployed.html` | `DEPLOYED` | Live URL + copy/visit, health metrics 4-card, asset inventory |
| `states/failed.html` | `FAILED` | Quality-gate fail (62/100), retry counter (2/3), collapsible admin log |
| `states/regenerating.html` | `REGENERATING` | Side-by-side current-vs-preview, current stays live until new ≥70 |

---

## Vietnamese UX

- Friendly copy per `02-vietnamese-ux-musts.md` §9 (informal "bạn"):
  - INITIALIZING: `Đang khởi tạo trang web cho bạn...`
  - GENERATING: `AI đang tạo bộ nhận diện thương hiệu...`
  - FAILED: `Có lỗi xảy ra. Đội ngũ kỹ thuật đã được thông báo.`
- Time format: `Còn khoảng 25 giây` / `1 phút 20 giây` (NOT `~30s`)
- Date: `29/04/2026 14:18:42` (dd/MM/yyyy HH:mm:ss)
- Domain: `edison.kitehub.me` shown verbatim with copy CTA
- Quality-gate copy: `Quality gate điểm 62/100 — chưa đạt ngưỡng tối thiểu 70/100`

---

## Accessibility

- All status pills use `role="status"` + icon + text (color is NOT only signal)
- FAILED state uses `role="alert"` + `aria-live="polite"`
- Progress bars: `role="progressbar" aria-valuenow/min/max` + label
- Timeline `<ol>` with `aria-current="step"` on active state
- Spinner SVGs respect `@media (prefers-reduced-motion: reduce)` — pulse/spin disabled
- Keyboard: Tab cycles back-arrow, primary CTA, secondary CTA, then content links
- Focus indicator: `focus-visible:ring-2 focus-visible:ring-ring` (2px + 3:1 contrast)

---

## Reuse (per `dossier/09-tech-constraints.md`)

- shadcn `Button` for CTAs (default + outline destructive variants)
- shadcn `Badge`/pill component for status chips
- lucide icons: `arrow-left`, `loader-2`, `check-circle`, `x-circle`, `alert-triangle`, `refresh-cw`, `copy`, `external-link`
- SSE consumer: `kitehub-frontend/src/lib/instance-events.ts` (existing) — emit `instance.progress` events
- Outbox pattern: every transition logged to `instance_lifecycle_events` table per `design-patterns.md` §3.5

---

## Self-score (each state file)

| State | Score |
|-------|------:|
| `not-started.html` | 105/128 |
| `initializing.html` | 106/128 |
| `generating.html` | 110/128 |
| `deployed.html` | 112/128 |
| `failed.html` | 108/128 |
| `regenerating.html` | 107/128 |
| `index.html` (showcase) | 109/128 |
| **Average** | **~108/128** |

All states ≥105/128, comfortably above R2 floor 95.

---

## Acceptance criteria

- [x] All 6 states match `ai-branding-guidelines.md` §6 state machine
- [x] FAILED state shows reason + retry CTA + admin log details
- [x] DEPLOYED state shows live URL + copy + visit CTA + health metrics
- [x] REGENERATING preserves current as Live until new passes quality gate
- [x] WCAG AA contrast measured + commented in every file
- [x] `prefers-reduced-motion` respected (pulse/spin disabled)
- [x] Vietnamese-only content; informal "bạn" tone
- [x] Self-score commented in HTML head
