# GAP-340: MOET Inter-school Transfer API (cùng tỉnh)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend + External Integration
**Detected:** 2026-05-04 (Wave 17 Bucket D — NEW from P5 §"NEW-4")
**Related:** P5-k12-school.md AC-EDGE-001, AC-EXIT-002

## Current State (verified 2026-05-04)

No MOET inter-school API integration. Transfer = paper hand-carry by PH.

## Problem

HS chuyển trường giữa năm or end-year. Without API integration → PH cầm tay paper, slow + lossy.

## Proposed Fix

1. **MOET API client** (if Phòng GD exposes one — research first)
2. **Transfer package PDF + XML** generation
3. **Receiving school confirm receipt** workflow
4. **Fallback:** signed paper for trường khác tỉnh

## Acceptance Criteria

- [ ] MOET API integration (or fallback PDF if API unavailable)
- [ ] Transfer package complete per MOET format
- [ ] Receipt confirmation tracking
- [ ] Test: transfer HS A → receiving school confirm → tenant cũ retains 5y archive
- [ ] business-logic-review.md 5-attribute

## Related

- **Depends on:** GAP-184 (5y retention), GAP-055, GAP-051
- **Wave plan:** Bucket D Stage 5

## Log

- **2026-05-04** — Filed Wave 17 Bucket D. **MOET API research required first.**
