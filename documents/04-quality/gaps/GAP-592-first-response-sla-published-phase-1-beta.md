# GAP-592: First-response SLA published doc cho Phase 1 BETA

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Ops / Content
**Phase:** phase-1-beta
**Found:** 2026-05-15 (Wave 86 Bucket A benchmark-vn-saas-edu Q9)
**Affects:** Beta cohort trust + support expectations + ambiguous SLA risk

## Problem

Industry benchmark Q9:
- B2B SaaS email target: 4-6 hours typical, top-tier <1h
- Strategic accounts: 2-4 hours
- Zalo OA VN: 87% smartphone users active → expect <1h

Wave 86 5-tenant cohort manual SLA = no automated tooling. Hiện chưa publish SLA → ambiguous expectation → tenants có thể expect Intercom-style 1-min response → trust damage khi reality là 4-24h.

## Root Cause

Wave 86 plan không cite explicit SLA. `user-manual-content-standard.md` §2 row 5 mention Zalo OA defer Phase 1.5+ (aligned), nhưng SLA doc chưa exist.

## Proposed Fix

1. **SLA doc** `documents/05-guides/operations/support-sla-phase-1-beta.md`:
   - Channels: Email `support@kitehub.me` (primary), Zalo OA (defer Phase 1.5+)
   - **First-response SLA**: <4 giờ business hours (8h-18h Mon-Fri VN time)
   - **Resolution SLA**: <24 giờ cho P1 incidents (P0 = ASAP)
   - Out-of-hours: best-effort + auto-reply explaining business hours
   - Beta-specific caveat: "Phiên bản Beta — phản hồi có thể chậm hơn"
   - Escalation path: tenant không satisfied → email founder direct
2. **Welcome email cite SLA**: Wave 86 Bucket G email template references this doc + SLA explicit
3. **Tracking spreadsheet** `documents/01-business/kitehub/beta-cohort/support-response-tracker.csv`:
   - Columns: ticket_id, received_at, first_response_at, resolved_at, sla_met (boolean), notes
4. **Pattern alert**: nếu >4h response → file follow-up gap với reason

## Acceptance Criteria

- [ ] SLA doc shipped
- [ ] Welcome email cite SLA explicit
- [ ] Tracking spreadsheet shipped với header rows
- [ ] Auto-reply out-of-hours configured Resend (defer if Resend không support → manual)
- [ ] Bucket H runbook reference này cho incident response

## Related

- Audit: `documents/04-quality/audits/persona-review/2026-05-15-pre-wave-86-benchmark-vn-saas-edu.md` §3 Q9 + §6 GAP-NEW-4
- Wave 86 plan §3 Bucket H AC H-AC14 (paired)
- `user-manual-content-standard.md` §2 row 5 (Zalo OA defer)
