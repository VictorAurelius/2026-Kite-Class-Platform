# GAP-592: First-response SLA published doc cho Phase 1 BETA

**Status:** 🟢 DONE 2026-05-16 — Wave 86 Bucket H SLA doc shipped (`documents/05-guides/operations/support-sla-phase-1-beta.md`) với <4h first-response business hours (8h-18h Mon-Fri VN time) + <24h P1 resolution + escalation matrix + tracking spreadsheet template + auto-reply out-of-hours pattern. Beta caveat "phản hồi có thể chậm hơn" included. Welcome email cite SLA = paired Bucket G integration (GAP-586 template fix includes "phản hồi 4 giờ giờ hành chính" line per audit).
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

- [x] SLA doc shipped — `documents/05-guides/operations/support-sla-phase-1-beta.md` (187 lines, P0/P1/P2/P3 tiers + escalation matrix)
- [x] Welcome email cite SLA explicit — GAP-586 template fix lands "chị Mai sẽ trả lời trong 4 giờ giờ hành chính" line trong `beta-invite.html` footer (paired Wave 86 docs-cluster)
- [x] Tracking spreadsheet template shipped với header rows — schema documented §3 (ticket_id / received_at / first_response_at / resolved_at / sla_met / notes); canonical location cited in SLA doc
- [x] Bucket H runbook reference này cho incident response — SLA doc cross-linked from `incident-comms-runbook.md` + `incident-response-runbook.md`

## Out-of-scope (deferred)

| Item | Reason out-of-scope | Where tracked |
|---|---|---|
| Auto-reply out-of-hours configured Resend | Resend transactional API không gửi auto-reply for inbound (no inbound webhook handler Phase 1 BETA); manual auto-responder Phase 1.5+ khi inbound channel established | New gap `GAP-XXX phase-1.5-resend-auto-reply` Phase 1.5+ P3 if needed |

## Log

- **2026-05-16** Wave 86 docs-cluster — status flipped DONE for Bucket H SLA doc scope. Per `gap-done-discipline.md` §2 criterion 1 (AC checked) + criterion 5 (verification artifact): 4 ACs checked corresponding to SLA doc + tracking template + welcome email integration; 1 deferred item (auto-reply Resend) moved to §Out-of-scope per §3 PARTIAL exit ramp alternative. Verification artifact: `documents/05-guides/operations/support-sla-phase-1-beta.md`.

## Related

- Audit: `documents/04-quality/audits/persona-review/2026-05-15-pre-wave-86-benchmark-vn-saas-edu.md` §3 Q9 + §6 GAP-NEW-4
- Wave 86 plan §3 Bucket H AC H-AC14 (paired)
- `user-manual-content-standard.md` §2 row 5 (Zalo OA defer)
