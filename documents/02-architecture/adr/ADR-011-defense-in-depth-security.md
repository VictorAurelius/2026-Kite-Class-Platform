# ADR-011: Defense-in-Depth Security (Validators + Output-Encoders + CSP)

**Status:** ACCEPTED
**Date:** 2026-04-14
**Deciders:** Tech Lead + Security + Architect
**Related Gap:** GAP-041 (Wave 4 Sub-PR 4.2)

## Context

Branding pipeline handles user uploads (SVG logos) + machine-generated content (AI output) + callbacks to external URLs (AI provider webhooks). Three vectors worry us:

1. **SVG XSS** — user uploads `<svg><script>alert(1)</script></svg>`; rendered in admin preview → stored XSS
2. **SSRF** — AI provider callback URL `http://169.254.169.254/latest/meta-data/` → cloud-credentials leak
3. **CSRF** — state-changing endpoints (rebrand, delete) hit via cross-origin form submission

Relying on a single layer (e.g., CSP only) fails when that layer has a bug. Defense-in-depth stacks orthogonal protections.

## Decision

**Three layers, each independently complete:**

### Layer 1: Input sanitization at ingress
- `SvgSanitizer` — JSoup-based `SafeList` allowing only safe SVG elements + attributes; strips `<script>`, `on*` handlers, `xlink:href` pointing off-origin
- `UrlAllowlistValidator` — all outbound callback URLs must match a tenant-configured allowlist; private-IP ranges blocked by default (169.254/16, 10/8, 172.16/12, 192.168/16, loopback)
- Validation happens in `@ValidateInput`-tagged controllers BEFORE reaching service layer

### Layer 2: Output encoding at egress
- Templating layer HTML-escapes by default (Next.js default); explicit opt-in (`dangerouslySetInnerHTML`) flagged in code review
- Admin preview renders user-uploaded SVG inside a `srcdoc`-sandboxed iframe

### Layer 3: Request-state protection
- `CsrfTokenFilter` — double-submit cookie pattern for state-changing endpoints (POST/PUT/DELETE)
- SameSite=Lax cookies by default; strict for sensitive routes
- CORS allowlist enforced at gateway

Each layer has its own test suite and fails closed.

## Consequences

### Positive
- ✅ Single-layer bugs don't lead to full compromise
- ✅ Testable per-layer (unit tests on sanitizer, integration on filter, e2e on CSP)
- ✅ Tenant-configurable URL allowlist gives enterprise flexibility without loosening defaults

### Negative
- ❌ Multiple points to maintain when threat model evolves
- ❌ Performance overhead (sanitizer per upload, validator per callback) — measured acceptable at current scale

## Alternatives

- **A. Rely on CSP alone** — rejected: CSP is output-side; can't stop SSRF
- **B. Use a commercial WAF** — deferred: adds infra complexity; app-layer validators more explicit
- **C. Block all SVG uploads** — rejected: breaks legitimate brand assets; sanitization + sandbox is the industry norm

## Implementation Notes

Base interfaces in `common/security/` (Sub-PR 4.0 foundation):
```java
interface SvgSanitizer { String sanitize(String rawSvg); }
interface UrlAllowlistValidator { boolean isAllowed(String url, String tenantId); }
interface CsrfTokenProvider { String issue(); boolean verify(String token, String cookie); }
```

Concrete impls land in Sub-PR 4.2.

## References
- GAP-041
- OWASP Top 10 (A01 Broken Access Control, A03 Injection, A04 Insecure Design, A10 SSRF)
- design-patterns.md §3.10 (Leaky Abstraction) — validator interfaces keep JSoup out of domain layer

## Log
- 2026-04-14 — Accepted
