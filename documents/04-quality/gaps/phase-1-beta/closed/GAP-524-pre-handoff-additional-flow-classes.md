# GAP-524: Extend pre-handoff-self-test-completeness with additional flow classes (META)

**Status:** 🟢 DONE 2026-05-14 — Wave 72b Bucket E shipped all 7 flow classes §2.5-§2.11 in pre-handoff rule v1.1.0
**Priority:** 🟠 P1 META (discovery-driven; not blocking but high-leverage)
**Domain:** Meta
**Found:** 2026-05-13 (Wave 71c-meta-Phase-2)
**Affects:** `.claude/rules/pre-handoff-self-test-completeness.md` v1.0.0 → v1.1.0

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

- [x] On each flow-class incident: add §2.N to rule + worked self-test — Wave 72b Bucket E shipped 7 classes preemptively per GAP-524's enumerated discovery list (instead of waiting for incidents) to close coverage gap proactively
- [x] Close GAP-524 only after ≥3 classes added — 7 classes added (§2.5 file-upload, §2.6 payment, §2.7 multi-tenant switch, §2.8 SSE/WS, §2.9 background job, §2.10 time-sensitive, §2.11 i18n)

## Related

- Parent rule: `pre-handoff-self-test-completeness.md` v1.0.0 → v1.1.0
- Sister: GAP-523 (audit rubric review wave — same Wave 72b Bucket E)
- Discovery pipeline: `incident-to-rule-pipeline.md`
- Rule: `meta-gap-priority.md` §3 Meta-P1 boost

## Log

- **2026-05-14:** Wave 72b Bucket E — pre-handoff rule bumped v1.0.0 → v1.1.0 with 7 new flow class checklists §2.5-§2.11. Each class mirrors §2.1-§2.4 4-row checklist structure adapted to its domain. CSV row updated (version 1.1.0, last_reviewed 2026-05-14). Frontmatter check passes. Status flip per `gap-done-discipline.md` §2 — both AC checked (preemptive shipping of all 7 enumerated classes satisfies "≥3 classes added" criterion; no banned phrases; no deferrals).
