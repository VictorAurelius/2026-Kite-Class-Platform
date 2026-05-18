# GAP-279: Track 2 Coverage — Common modals + dialogs catalog (D1..D10)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2 (UX cross-cutting — used by all 7 kit ports)
**Domain:** Frontend / Design System
**Found:** 2026-04-29 via audit §2.6
**Affects:** Both apps — modal/dialog components (10 distinct ❌ missing)

## Problem

R2/R3 component catalog G1..G12 covers cross-cutting components but does NOT cover modals/dialogs explicitly. Audit found 10 distinct modal sites with ❌ NO kit coverage. These mirror G1..G12 pattern but for stateful overlays.

## Current State (verified 2026-04-29)

**KC modals (2 distinct ❌):**
| File | Use case |
|------|---------|
| `components/class/cancel-class.tsx` (renders as `<Card>`, anti-pattern) | Cancel class confirmation |
| `confirm-dialog.tsx` (`common/`) — exists but states not catalogued | Generic confirm with destructive variant |

**KH modals (8 distinct ❌):**
| File | Use case |
|------|---------|
| 2 `branding/` Danger Zone dialogs | Reset branding / Delete instance |
| 1 `(admin)/admin/instances/` row-action dialog | Suspend / resume tenant |
| 2 `(admin)/admin/instances/[id]/` detail dialogs | Force-rebrand / Migrate tenant |
| 3 `(admin)/admin/payments/` confirm dialogs | Confirm payment / Reject payment / Show QR |

## Proposed Fix

Create `ui_kits/dialogs/` HTML kit mirroring `components/G1..G12` pattern. Each dialog = catalog entry (D1..D10) with 4-6 state demos + spec.md.

**Catalog (D1..D10):**

| ID | Dialog | States | Pattern |
|:--:|--------|:------:|---------|
| D1 | Generic confirm dialog | default / destructive / loading / error / success | `common/confirm-dialog.tsx` |
| D2 | Cancel class confirm | default / soft-cancel / hard-cancel-with-refund / processing | `class/cancel-class.tsx` (must convert from Card to Dialog) |
| D3 | Reset branding (Danger Zone) | default / warning-list / confirming-with-cooldown / success | `branding/Danger Zone` |
| D4 | Delete instance (Danger Zone) | default / impact-summary / typed-confirmation / processing | `branding/Danger Zone` |
| D5 | Suspend / resume tenant | default / suspend-flow / resume-flow / reason-required | `(admin)/admin/instances/` |
| D6 | Force-rebrand tenant | default / pre-impact / processing / done | `(admin)/admin/instances/[id]/` |
| D7 | Migrate tenant | default / pre-check / processing / rollback-available | `(admin)/admin/instances/[id]/` |
| D8 | Confirm payment | default / VN-currency-display / partial-payment / processing | `(admin)/admin/payments/` |
| D9 | Reject payment | default / reason-required / refund-pending / done | `(admin)/admin/payments/` |
| D10 | Show QR for payment | default / QR-displayed / countdown / refresh-CTA / expired | `(admin)/admin/payments/` |

**Plus tech-debt:** convert `class/cancel-class.tsx` from `<Card>` to `<Dialog>` (anti-pattern fix).

## Acceptance Criteria

- [ ] HTML kit `ui_kits/dialogs/` ≥105/128 per dialog
- [ ] D1..D10 each have 4-6 state demos + spec.md
- [ ] D1 (generic confirm) reusable foundation for all destructive actions
- [ ] D2..D10 inherit D1 patterns + add domain-specific states
- [ ] VN currency formatting in D8
- [ ] QR countdown timer in D10
- [ ] Production ported all 10 modals + tech-debt fix on D2 anti-pattern
- [ ] WCAG AA + keyboard navigation (focus trap + Esc to close)

## Split decision

If oversized (>10 modals at wave kickoff): **split into GAP-279.A (KC 2 modals + generic D1) + GAP-279.B (KH 8 admin modals)**.

## Related

- Audit evidence: §2.6
- Sister catalog: `dossier/04-component-gaps.md` G1..G12 (mirror structure)
- Tech-debt fix included: `cancel-class.tsx` Card→Dialog conversion

## Effort estimate

~1 wave (component-style: 10 modals × 5 states avg = ~50 demo files, comparable to R2 components 5 components × 5 states = 25).

## Log

- **2026-04-29:** Filed from audit synthesis. Mirrors G1..G12 pattern for modal/dialog overlay scope.
