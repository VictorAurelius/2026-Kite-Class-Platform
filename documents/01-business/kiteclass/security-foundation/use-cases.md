# Security Foundation — Use Cases

### UC-SEC-FDN-01: Record security-relevant action
- **Actor:** Any service inside a `@Transactional` method (moderation, DMCA, deletion)
- **Steps:**
  1. Service completes domain change
  2. Service calls `auditLog.record(AuditLogWriter.AuditLogEvent.builder()...)`
  3. Both writes commit atomically
- **Postcondition:** AuditLog row available for compliance review

### UC-SEC-FDN-02: Query audit trail for aggregate
- **Actor:** Admin / regulator
- **Call:** `repository.findByAggregateTypeAndAggregateIdOrderByCreatedAtDesc(type, id)`
- **Result:** chronological list of actions on that aggregate — admin UI renders as timeline (Wave 8 admin console)

### UC-SEC-FDN-03: SvgSanitizer usage (consumer pattern)
- **Actor:** Logo upload endpoint (storage module)
- **Steps:**
  1. Receive SVG bytes
  2. Call `svgSanitizer.sanitize(raw)`
  3. Persist sanitized output only
- **Notes:** Concrete impl in 4.2; consumer consumes the interface

### UC-SEC-FDN-04: UrlAllowlistValidator usage
- **Actor:** AI callback registration / webhook delivery
- **Steps:**
  1. Check `validator.isAllowed(url, tenantId)` before dispatching
  2. Reject with 400 if false + audit the rejection (actionType=`security.url.blocked`)

### UC-SEC-FDN-05: CSRF token round-trip
- **Actor:** State-changing POST / DELETE routes
- **Steps:**
  1. GET page → server issues CSRF token in cookie + embeds in form
  2. POST includes both header (X-CSRF-Token) and cookie
  3. CsrfTokenFilter (4.2) calls `provider.verify(headerToken, cookieToken)`
  4. Mismatch → 403 + audit

### UC-SEC-FDN-06: Migration reservation coordination
- **Actor:** Parallel agents on sub-PRs 4.1/4.3/4.4/4.5
- **Constraint:** Each agent consumes ONLY its reserved version number (see rules.md table)
- **Deviation:** requires lead-agent approval + updating the reservation table before push

## Log
- 2026-04-14 — Initial UCs
