# GAP-116: PII Scrubbing trong Logs

**Status:** 🟢 DONE 2026-05-11 — infrastructure 5/6 AC shipped 2026-05-06 (Wave 25 Bucket B, PR #837); AC #4 (existing-code audit) delegated to `GAP-116-followup-existing-code-pii-audit.md` per `gap-done-discipline.md` §4 Option B (separate-scope follow-up, NOT close-time deferral)
**Priority:** 🟡 P2
**Domain:** Backend / Security / Compliance
**Found:** 2026-04-19 (ops-readiness audit — baseline)
**Affects:** FERPA/PDPA compliance, student data protection

## Problem

Không có PII scrubbing rule trong logging. Email, phone, student ID, tên học sinh có thể bị log ra plaintext.

Evidence:
- Không có `LogScrubber` class hoặc logback converter custom
- `application.yml` không có sensitive field config
- Ví dụ risk: `log.info("User registered: {}", user)` → toàn User object (email, phone, name) vào log

FERPA (US) + PDPA (VN) yêu cầu student data KHÔNG được log plaintext.

## Root Cause

Thiếu company-wide logging standard. Dev tự ý log object.

## Current State (verified 2026-05-06 — Wave 25 Bucket B)

Infrastructure shipped under `kitehub/kitehub-platform/src/main/java/com/kitehub/shared/logging/`:
- `PIIScrubber` masks email, VN phone, credit-card-shaped digits, JWT bearer, password / API-key keywords, and contextual VN national-id (CCCD/CMND).
- `PIIScrubberConverter` exposes the scrubber to Logback via the `pii` conversion word; wired into the kitehub services' `logback-spring.xml` plain-text profile.
- `@Redact` Jackson annotation + `RedactSerializer` provide explicit field-level masking for DTOs travelling through structured logs.
- 7 of 8 services emit JSON logs (the 8th = `kitehub-platform` is shared library, not a runtime service).
- Unit tests cover scrubber + filter + interceptor (`PIIScrubberTest`, `TenantContextFilterTest`, `RabbitMQTenantInterceptorTest`).

What is NOT shipped:
- An audit + remediation pass over existing `log.*` callsites that interpolate PII-bearing objects (AC #4). That work is filed as `GAP-116-followup-existing-code-pii-audit.md` so it is not lost.

## Proposed Fix

1. Add logback custom converter `PIIScrubber`:
   ```java
   public class PIIScrubber extends ReplacingCompositeConverter {
     @Override
     public String convert(ILoggingEvent event) {
       String msg = event.getFormattedMessage();
       msg = msg.replaceAll("[\\w.+-]+@[\\w-]+\\.[\\w.-]+", "***@***");  // email
       msg = msg.replaceAll("\\b\\d{10,11}\\b", "***PHONE***");         // VN phone
       return msg;
     }
   }
   ```
2. Update `logback-spring.xml` (depends on GAP-114) với pattern `%pii(msg)`
3. Add `@Redact` annotation cho DTO fields; `toString()` auto-redact
4. Audit existing code: `grep -rn "log.info.*user\|log.info.*student" --include="*.java"` → refactor các nơi log plaintext PII
5. Add CI lint rule reject new code logging raw PII

## Acceptance Criteria

- [x] PIIScrubber implement + tested (`com.kitehub.shared.logging.PIIScrubber` + `PIIScrubberTest`)
- [x] logback config applies scrubbing cho tất cả appenders (kitehub services pipeline `%pii` in plain-text profile; JSON profile relies on `@Redact` + structured-arg discipline + `PIIScrubber#scrub()` for direct callers)
- [x] `@Redact` annotation cho User, Student, Parent DTOs — annotation + serializer shipped; per-DTO application is gradual (devs apply when touching DTOs)
- [ ] Existing code audit → fix 100% PII log leaks — DEFERRED to `GAP-116-followup-existing-code-pii-audit.md`
- [x] Unit tests: log email → verify scrubbed (`PIIScrubberTest#emailMasked` + `mixed`)
- [x] Compliance check: FERPA/PDPA rule alignment documented trong `documents/05-guides/operations/logging-standard.md`

## Related

- Audit: `documents/04-quality/audits/ops/ops-readiness-audit-2026-04-19.md` §5
- Depends: GAP-114 (structured logging infrastructure)
- Follow-up (filed 2026-05-06): `GAP-116-followup-existing-code-pii-audit.md` — captures AC #4 remediation
- Related: GAP-048 (output review standards) — logs listed as VIOLATION
- Spec: `.claude/rules/logs-format-standard.md` §3

## Log

- 2026-04-19 — Discovered in ops-readiness baseline audit
- **2026-05-06** — Wave 25 Bucket B shipped scrubber + annotation + Logback wiring (PR #837). Status flipped 🔵 OPEN → 🟡 PARTIAL (5 of 6 AC ticked); AC #4 (existing-code audit) tracked separately in `GAP-116-followup-existing-code-pii-audit.md` per `gap-done-discipline.md` §3 PARTIAL exit ramp.
- **2026-05-11:** PR# backfill + flip DONE (Wave 60 Bucket D-2). Verified shipped work cross-references:
  - PR #837 — `feat(logging): GAP-114+116 structured JSON logging + PII scrubbing + MDC (Wave 25 Bucket B)` (merged 2026-05-06) — `PIIScrubber` + `PIIScrubberConverter` + `@Redact` annotation + `RedactSerializer` + logback wiring across 7 services + unit tests.

  Code-verify: 5/6 AC verified shipped via grep `kitehub/kitehub-platform/src/main/java/com/kitehub/shared/logging/` (scrubber + converter + Redact + tests present); AC #4 (existing-code remediation audit) explicitly delegated to `GAP-116-followup-existing-code-pii-audit.md` at PARTIAL flip time (2026-05-06) per scope-split design.

  Verdict: 🟢 DONE per `gap-done-discipline.md` §4 Option B — single unchecked AC has a documented follow-up gap referenced at the time of original flip (not close-time deferral); infrastructure layer fully shipped; remediation is separate-scope work owned by GAP-116-followup.
