# `_shared/components/` — cross-kit reusable UI fragments

Reusable HTML+token fragments shared across UI kits. Each component is
self-contained (relies only on `_shared/colors_and_type.css` tokens +
the consuming kit's `styles.css` primitives) and documents its props
inline.

| Component | Purpose | Origin |
|-----------|---------|--------|
| `zalo-oa-card.html` | Zalo OA parent-notify card (caption + state pill + action). Reused at every "notify parent" touch-point — conduct escalation, report-card release, fee reminder. | Extracted from `kitehub-admin/parent-comms.html` (GAP-364b item 5) |

**Usage:** copy the example instance into a screen, adjust the documented
`data-*` props + slot text. The card is token-driven so it recolours
automatically under `.dark`.

**Last Updated:** 2026-06-11
