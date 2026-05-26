# GAP-742: Outbox DLQ alert missing — production outbox enabled without DLQ monitoring

**Status:** 🟢 DONE 100%
**Priority:** 🟠 P1
**Domain:** Ops (Alerting)
**Found:** 2026-05-25 (Wave audit-1 Bucket D Ops Readiness audit)
**Affects:** Tenant-facing message dispatch (email confirmations, payment receipts)

## Problem

Per `documents/04-quality/audits/ops-readiness/2026-05-25-wave-br-4-ops-readiness-audit.md` §OPS-BR4-001:

PR #1781 Wave br-4 enable `kitehub.outbox.enabled=true` production. Outbox consumer dispatch events; failures route → RabbitMQ DLQ.

**Gap:** DLQ monitoring/alert chưa wired. Outbox dispatch failures có thể silently accumulate trong DLQ mà không có on-call notification → tenant-facing message dispatch (email, payment receipt) drop silently.

Per `pre-handoff-self-test-completeness.md` §2.9 (C4-4): "DLQ non-empty alert fires" mandate — KHÔNG được satisfy.

## Root Cause

Bucket D Wave br-4 (PR #1781) focus on outbox consumer code + feature flag. DLQ monitoring path treated as ops follow-up. Session handoff không cite DLQ alert wiring.

## Proposed Fix

1. RabbitMQ DLQ queue depth metric → CloudWatch hoặc Prometheus (depending on stack)
2. Alarm threshold: DLQ depth > 0 → SNS alert (paired GAP-144 AlertManager wiring)
3. Test: send synthetic failed message → expect alert fires within ≤5 phút
4. Document trong `documents/02-architecture/` outbox section

## Acceptance Criteria

- [ ] DLQ queue depth metric configured
- [ ] CloudWatch alarm threshold > 0 → SNS topic
- [ ] Alert verified via test message
- [ ] Documentation updated
- [ ] Ops audit re-run: OPS-BR4-001 closed → Cat4 +1

## Related

- Audit: `documents/04-quality/audits/ops-readiness/2026-05-25-wave-br-4-ops-readiness-audit.md` §OPS-BR4-001
- Paired GAP-144 AlertManager + SNS wiring (P0 carry)
- Rule: `pre-handoff-self-test-completeness.md` §2.9
- Wave: planned `wave-beta-readiness-7` (Bucket E addition)

## Log

- **2026-05-25 (created):** Filed per Wave audit-1 Ops Readiness audit OPS-BR4-001. Wave beta-readiness-7 Bucket E scope.

- **2026-05-26 (Wave br-7 Bucket E PR #1843 — new outbox-dlq-alerts Prometheus group + outbox-dlq-investigation.md runbook closure):** Flipped DONE 100% — . CSV row updated + file moved to phase-1-beta/closed/ per `gap-done-discipline.md` §2.
