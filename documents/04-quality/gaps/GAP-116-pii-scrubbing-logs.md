# GAP-116: PII Scrubbing trong Logs

**Status:** 🔵 OPEN
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

- [ ] PIIScrubber implement + tested
- [ ] logback config applies scrubbing cho tất cả appenders
- [ ] `@Redact` annotation cho User, Student, Parent DTOs
- [ ] Existing code audit → fix 100% PII log leaks
- [ ] Unit tests: log email → verify scrubbed
- [ ] Compliance check: FERPA/PDPA rule alignment documented trong `documents/05-guides/infrastructure/SECRET-MANAGEMENT.md`

## Related

- Audit: `documents/04-quality/audits/ops/ops-readiness-audit-2026-04-19.md` §5
- Depends: GAP-114 (structured logging infrastructure)
- Related: GAP-048 (output review standards) — logs listed as VIOLATION

## Log

- 2026-04-19 — Discovered in ops-readiness baseline audit
