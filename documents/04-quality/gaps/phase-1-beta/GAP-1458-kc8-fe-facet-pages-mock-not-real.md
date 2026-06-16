# GAP-1458: KC-8 FE parent facet pages (attendance/billing/grades) render MOCK, chỉ transcript+children thật

**Status:** 🔵 OPEN
**Priority:** 🔴 P2
**Domain:** Frontend
**Found:** 2026-06-16 (Flow Verification Campaign — KC-1/2/3/8 browser re-walk)
**Affects:** Frontend

## Problem

KC-8 pre-walk FM#2: lib/api/parent.ts chỉ wire getMe/getMyChildren/getChildTranscript. attendance/billing/grades + hero 92%/GPA 8.4 dùng mock data present-as-real. Curl-G1 không thấy (test BE trực tiếp). Wire FE facet → BE real endpoints. Phase 1.5 scope.

## Acceptance Criteria

- [ ] Fix/verify per Problem
- [ ] Browser re-walk confirm

## Related

- Discovered in: 2026-06-16 browser walk batch (KC-1/2/3/8)
