# GAP-365: File Tier-1 `S-student.md` persona AC doc (currently absent)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2 Business-Logic (per `meta-gap-priority.md` §3 — persona AC infrastructure)
**Domain:** Business / BRD / Persona criteria
**Found:** 2026-05-05 (Wave 20 Bucket A external review surfaced absence)
**Affects:** `documents/00-brd/persona-criteria/S-student.md` (NEW file) + downstream all kits/audits referencing S. Student persona

## Problem

`documents/00-brd/persona-criteria/` has Tier-1 AC docs for P1/P2/P3/P5 personas but **NO top-level `S-student.md`**. Student persona is covered indirectly via `secondary/student-in-P2.md` + `secondary/student-in-P3.md` + `secondary/student-in-P5.md` (student-within-tenant secondary docs) — but no Tier-1 doc.

Wave 20 Bucket A (kiteclass-student external review) flagged this absence: complete persona-alignment scoring blocked because there's no canonical S. Student AC document. Bucket A used `secondary/student-in-P2.md` as proxy + flagged for follow-up.

## Current State (verified 2026-05-05)

```
documents/00-brd/persona-criteria/
├── README.md
├── P1-solo-teacher.md          ✅ Tier-1
├── P2-small-center.md          ✅ Tier-1
├── P3-medium-center.md         ✅ Tier-1
├── P5-k12-school.md            ✅ Tier-1
├── _TEMPLATE.md
└── secondary/
    ├── student-in-P2.md        ⚠️ secondary, used as proxy
    ├── student-in-P3.md        ⚠️ secondary
    └── student-in-P5.md        ⚠️ secondary
```

Tier-1 `S-student.md` is **❌ ABSENT**.

## Why this matters

S. Student is NOT just a sub-persona of P2/P3/P5 — they are a Tier-1 persona in their own right:
- Mobile-primary (~85% sessions per agent self-report 116 ⭐⭐ kit context)
- Age range spans 6-22 (K-12 + vocational + university tutoring)
- Has unique journeys NOT inherited from owner persona: assignment submission, grade self-tracking, attendance check-in, parent-trigger payment workflow (AC-FIN-001)
- Has unique constraints: child-protection (under 18), payment-locked (cannot execute commitments), notification-throttled

Without Tier-1 doc:
- Kit reviews use proxy ACs → calibration drift
- Track 2 port specs cite scattered evidence pointers
- Persona-based business reviews cannot role-play S. Student authoritatively
- New gaps for student features (e.g., GAP-269 student port) cannot ground AC properly

## Proposed Fix

Create `documents/00-brd/persona-criteria/S-student.md` following `_TEMPLATE.md` pattern:

- **Persona basics**: name, age range, primary device, session pattern, communication preferences
- **Journeys** (Tier-1, mobile-PWA primary):
  - Today (home, next-class context)
  - My Classes (enrolled list + filters)
  - Assignment workflow (view, submit, saved-draft)
  - Grades (self-tracking + GPA + Học lực + parent visibility)
  - Attendance (self-view + teacher mark)
  - Notifications (throttled, parent-kép visualization)
  - Profile (basic info, no payment access for K-12)
  - Payment fees (READ-ONLY for K-12 — payment via parent-trigger workflow)
- **AC-* identifiers** (use AC-* prefix per existing pattern):
  - AC-ONBOARD-001..N (registration, parent-paired account creation)
  - AC-FIN-001..N (READ-ONLY fees access; child-protection lock)
  - AC-EDGE-001..N (forgotten password parent-reset workflow)
  - AC-NOTIF-001..N (parent-kép visualization, throttling)
  - AC-CONTENT-001..N (assignment, grade, attendance access patterns)
- **Cross-references** to:
  - `secondary/student-in-P2.md` / `student-in-P3.md` / `student-in-P5.md` (preserved as tenant-context-specific extensions)
  - `personas-catalog.md` (top-level persona registry)
  - Kit `ui_kits/kiteclass-student/README.md` (UI realization)
  - `documents/01-business/kiteclass/parent-portal/rules.md` (parent-kép + consent BR)

## Acceptance Criteria

- [ ] `documents/00-brd/persona-criteria/S-student.md` created following `_TEMPLATE.md`
- [ ] All 8 journey areas documented with AC-* identifiers
- [ ] Child-protection constraints (AC-FIN-001 payment lock for K-12) clearly enumerated
- [ ] Cross-references to secondary/* docs (clarify Tier-1 vs tenant-context relationship)
- [ ] `personas-catalog.md` updated with S-student row (Tier-1)
- [ ] `documents/00-brd/persona-criteria/README.md` index updated
- [ ] Cross-link from `ui_kits/kiteclass-student/README.md` to this Tier-1 doc
- [ ] Re-validate Wave 20 Bucket A review report's persona-alignment scoring (§5) using new Tier-1 doc

## Related

- Wave 20 Bucket A review (surfaced absence): `documents/04-quality/audits/ui-review/2026-05-05-round-3-kiteclass-student-review.md`
- Parent gap: GAP-348 (Round 3 review) — flips PARTIAL on this filing
- Sister polish gap: GAP-363 (kiteclass-student kit polish — payments AC-FIN-001 violation depends on this AC doc)
- Persona criteria index: `documents/00-brd/persona-criteria/README.md`
- Template: `documents/00-brd/persona-criteria/_TEMPLATE.md`

## Why Business-Logic-tier (not Meta)

Per `meta-gap-priority.md` §3 — this gap touches `documents/00-brd/**` (BRD persona-criteria). Business-Logic-tier sits between Meta and Feature. Wrong/missing persona doc = wrong product specs downstream, even if skill/rule infrastructure is perfect.

## Effort estimate

~6-8h (single agent, document writing). Can pair-wave with GAP-363 + GAP-364 as 3-bucket post-Wave-20 polish + persona-doc wave-pack.

## Log

- **2026-05-05:** Filed by Wave 20 Bucket C closure (this PR) per `audit-to-gap-pipeline.md`. Surfaced by Bucket A external review (kiteclass-student) flagging Tier-1 doc absence — used `secondary/student-in-P2.md` as proxy.
