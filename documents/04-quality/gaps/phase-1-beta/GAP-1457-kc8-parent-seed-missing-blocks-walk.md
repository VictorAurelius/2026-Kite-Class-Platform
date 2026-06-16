# GAP-1457: KC-8 parent seed thiếu hẳn (0 parents/links/consent/PARENT-credential) → không walk được parent portal

**Status:** 🔵 OPEN
**Priority:** 🔴 P1
**Domain:** DB
**Found:** 2026-06-16 (Flow Verification Campaign — KC-1/2/3/8 browser re-walk)
**Affects:** DB

## Problem

KC-8 G2 walk blocker: kiteclass_shared có 143 students nhưng 0 parents, 0 parent_student_links, 0 consent_record, auth_credentials 0 PARENT. Seed wave-plan (parent1@test.com) không còn. Re-seed parent+child+link+credential+consent trong instance TRIAL sky-education-074901 để walk.

## Acceptance Criteria

- [ ] Fix/verify per Problem
- [ ] Browser re-walk confirm

## Related

- Discovered in: 2026-06-16 browser walk batch (KC-1/2/3/8)
