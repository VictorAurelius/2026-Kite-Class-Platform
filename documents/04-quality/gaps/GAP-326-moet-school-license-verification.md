# GAP-326: MOET School License Verification at Tenant Signup

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend
**Detected:** 2026-05-04 (Wave 17 Bucket D)
**Related:** P5-k12-school.md AC-ONBOARD-003

## Current State (verified 2026-05-04)

```bash
grep -rl "school.license\|MOET.verify\|tenant.verification" kitehub/ kiteclass/ --include="*.java"
```
Result: zero matches.

## Problem

K12_ENTERPRISE tier doesn't exist + no signup verification. Anyone can claim "trường" without giấy phép. Risk: bad-actor school accepts children data without authority.

## Proposed Fix

1. **K12_ENTERPRISE tier enum** added to subscription tiers
2. **Signup form fields:** mã số trường MOET (numeric), giấy phép thành lập upload (PDF), HT info (CCCD scan + chứng chỉ quản lý)
3. **Admin-Kite verify queue:** TRIAL until verified → ACTIVE
4. **Audit log:** evidence preserved 7y

## Acceptance Criteria

- [ ] K12_ENTERPRISE tier enum + tier metadata
- [ ] Signup form rejects K-12 without 3 evidence fields
- [ ] Admin verification UI with status workflow
- [ ] Test: signup K-12 → status TRIAL → admin approve → ACTIVE
- [ ] business-logic-review.md 5-attribute

## Related

- **Depends on:** existing tenant signup infra
- **Wave plan:** Bucket D Stage 3

## Log

- **2026-05-04** — Filed Wave 17 Bucket D.
