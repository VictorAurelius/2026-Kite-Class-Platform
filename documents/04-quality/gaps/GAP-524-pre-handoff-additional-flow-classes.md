# GAP-524: Extend pre-handoff-self-test-completeness with additional flow classes (META)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 META (discovery-driven; not blocking but high-leverage)
**Domain:** Meta
**Found:** 2026-05-13 (Wave 71c-meta-Phase-2)
**Affects:** `.claude/rules/pre-handoff-self-test-completeness.md` v1.0.0

## Problem

PR #1278 shipped `pre-handoff-self-test-completeness.md` with 4 class checklists:
- §2.1 Auth-gated user flow
- §2.2 Anonymous/public flow
- §2.3 Email-driven flow
- §2.4 Admin/privileged action

Missing classes likely to cause future "verify-claimed-but-flow-broken" incidents:

1. **File-upload flow** — image / document / CSV upload → MIME validation + size limit + virus scan + storage location + retrieval URL
2. **Payment flow** — Stripe/payment-gateway redirect + webhook signature verify + idempotency key + reconciliation
3. **Multi-tenant tenant-switch flow** — login as user-with-N-tenants → tenant picker → JWT swap → data isolation verify
4. **SSE / WebSocket / long-polling** — connection establish + heartbeat + reconnect + auth-on-reconnect
5. **Background job / async** — enqueue → worker pick → retry → DLQ → notification
6. **Time-sensitive flow** — token expiry / refresh / clock skew tolerance
7. **i18n flow** — locale detection + fallback + content variant rendering

Discovery: extend lazily per incident — when an incident exposes missing class, add §2.N.

## Proposed Fix

Defer 6 incidents — each class added when first incident demonstrates need. Per `incident-to-rule-pipeline.md` premature-rule guard (don't add until 2nd recurrence).

For now: file this gap as a tracking artifact; refer to it when future incident requires class extension.

## Acceptance Criteria

- [ ] On each future flow-class incident: add §2.N to rule + worked self-test
- [ ] Close GAP-524 only after ≥3 classes added (proving rule's coverage extending pattern works)

## Related

- Parent rule: `pre-handoff-self-test-completeness.md` v1.0.0
- Discovery pipeline: `incident-to-rule-pipeline.md`
- Rule: `meta-gap-priority.md` §3 Meta-P1 boost
