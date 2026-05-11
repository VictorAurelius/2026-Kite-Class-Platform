---
name: GAP-175 — Logs format standard
description: Log format inconsistent across services; structured JSON + required fields + retention + PII scrub policy
type: gap
---

# GAP-175: Logs Format Standard

**Status:** 🟢 DONE (standard published; implementation deferred to Wave 7 — GAP-114/115/116)
**Priority:** 🟡 P2 (meta — observability quality)
**Domain:** Logging / SRE / Observability
**Found:** 2026-04-14 (output-review-mandate §4 Violation #6)
**Affects:** All log output; debugging velocity; log aggregation effectiveness

## Problem

Log format varies across services. Some services use default Spring Boot logback (text), others structured. Field naming inconsistent (tenantId vs tenant_id vs tenant). No retention policy. PII in logs not scrubbed consistently.

Overlaps with GAP-114 (structured JSON logging + MDC propagation — P0 ops) + GAP-116 (PII scrubbing — P2 ops) but those are implementation gaps. This gap is the STANDARD document that drives them.

## Root Cause

No single source of truth for log format. Skill/rule for structured logging missing. Each service independently decided.

## Proposed Fix

1. Create `.claude/rules/logging-standard.md`:
   - Structured JSON (logstash-encoder or similar)
   - Required fields: timestamp (ISO-8601), level, service, tenantId, traceId, spanId, message, userId (nullable)
   - Optional fields: httpStatus, durationMs, errorCode, stack
   - PII scrubbing rules: mask email `a***@b.com`, phone `09**12***89`, no plaintext passwords
   - Retention: hot 7 days, warm 30 days, cold 180 days (per legal/audit)
2. Logback config template per service — propagate MDC
3. ArchUnit rule: enforce `@SLF4JLogger` pattern; forbid `System.out.println` in main code
4. PII scrubber filter registered in all services

## Acceptance Criteria

- [ ] `.claude/rules/logging-standard.md` exists
- [ ] Logback config template committed in shared module
- [ ] All 10+ services use standard config
- [ ] PII scrubber audit: grep for email/phone patterns in logs → 0 hits
- [ ] Retention policy configured in log aggregation infra (GAP-115 / Wave 7)

## Related

- Parent violation: output-review-mandate §4 #6
- Implementation sibling: GAP-114 (structured JSON logging), GAP-115 (aggregation pipeline), GAP-116 (PII scrubbing)
- This gap = STANDARD; 114/115/116 = IMPLEMENTATION

## Log

- **2026-04-20 (Wave 8b-D):** Standard published at `.claude/rules/logs-format-standard.md`. Defines required/contextual/optional fields, PII scrubbing masks (email/phone/CCCD/JWT/secrets), retention tiers (hot 7d / warm 30d / cold 180d, security logs 7y per ND-13/2023), level usage, logback-spring.xml reference snippet, and Python/Node/Shell notes. Implementation (logback encoder, PII scrubber bean, aggregator pipeline) remains Wave 7 scope via GAP-114/115/116. This gap is DONE for the standard; migration checklist for Wave 7 embedded in §8–9 of the rule.
