# Security Hardening — Business Rules

**Domain:** Security Hardening (Injection Defense)
**Source of truth:** This doc — SPI live in `kiteclass-core/src/main/java/com/kiteclass/core/common/security/`
**ADR:** `documents/02-architecture/adr/ADR-011-defense-in-depth-security.md`
**Gap:** `GAP-041`

---

## BR-SEC-001 — SVG Upload Sanitization

Any SVG byte stream accepted from a non-admin actor (logo upload, branding banner, email signature)
MUST pass through `SvgSanitizer.sanitize(...)` before being persisted, rendered, or re-distributed.

- Must strip: `<script>`, `on*` event handlers, `foreignObject`, `iframe`, `embed`, `object`,
  off-origin `xlink:href` / `href`, `javascript:` / `expression()` in `style`.
- Must preserve: path/rect/circle/polygon/g/text/defs/gradient/animate plus standard presentation
  attributes (fill, stroke, d, viewBox, transform, ...) so branding assets render intact.
- Config key: `security.svg.enabled` — when `false`, uploads are rejected outright (no fallback path).

---

## BR-SEC-002 — Outbound URL SSRF Allowlist

Before any service performs an outbound HTTP request with a **user-supplied URL** (webhook,
AI analyse-URL, callback), it MUST call `UrlAllowlistValidator.isAllowed(url, tenantId)`.

- Schemes permitted: `http`, `https`. All others denied.
- Hosts denied regardless of allowlist: `localhost`, `127.0.0.0/8`, `10.0.0.0/8`, `172.16.0.0/12`,
  `192.168.0.0/16`, `169.254.0.0/16`, `::1`, any link-local / multicast / site-local.
- URLs containing userinfo (`http://user:pw@host`) are denied.
- Unresolvable hostnames → deny (fail-closed).
- Allowlist priority per request:
  1. `security.url.allowlist.<tenantId>` (tenant-scoped list)
  2. `security.url.allowlist.default` (global shared list)
  3. `security.url.allowlist.public-api-patterns` (regex — applied when `tenantId == null`)
- Domain patterns support literal (`api.partner.com`) or wildcard subdomain (`*.trusted.org`).
- Patterns under `public-api-patterns` are full-URL regex, e.g. `^https://api\\.ollama\\.com/.*`.

---

## BR-SEC-003 — CSRF Double-Submit Token

State-changing HTTP requests (POST/PUT/PATCH/DELETE) against cookie-authenticated endpoints MUST
be verified with `CsrfTokenProvider.verify(headerToken, cookieToken)`.

- Tokens carry an embedded `issuedAt` and HMAC-SHA256 signature; both cookie and header must
  match exactly AND the signature must validate.
- TTL: configurable via `security.csrf.ttl-hours` (default `4`). Expired tokens rejected.
- Secret: `security.csrf.secret`. Service refuses to start when value is blank, `insecure-default`,
  the placeholder `PLEASE_OVERRIDE_IN_PROD_32_BYTE_MIN`, or shorter than 32 characters.

---

## Config Keys

| Key | Default | Notes |
|-----|---------|-------|
| `security.svg.enabled` | `true` | Feature flag for SVG uploads. |
| `security.url.allowlist.default` | `""` | Comma-separated domain patterns. |
| `security.url.allowlist.<tenantId>` | _(unset)_ | Per-tenant domain patterns. |
| `security.url.allowlist.public-api-patterns` | `^https://api\.(ollama\|openai\|anthropic)\.com/.*` | Regex for null-tenant outbound calls. |
| `security.csrf.secret` | _(env)_ | Min 32 chars, fail-loud on insecure defaults. |
| `security.csrf.ttl-hours` | `4` | Token lifetime. |

---

## Five-attribute review per `business-logic-review.md`

Per-rule attributes (Source / Rationale / Reviewer / Compliance check / Review cadence) backfilled at file-level placeholder per Phase 1 of GAP-433. Per-rule granularity tracked via GAP-156 Phase 2 stakeholder sign-offs.

- **Source:** Existing rules in this file derive from a mix of: feature gaps cited inline (where present), ADRs, persona reviews, and informed-gut estimates from Wave 1-30 work. Rules without inline citation default to `informed gut` per `business-logic-review.md` §2.1 and inherit quarterly re-review obligation below.
- **Rationale:** Rule values reflect product judgment + (where applicable) competitor benchmarks + VN regulatory minimums. Detailed per-rule rationale to be backfilled during GAP-156 Phase 2 stakeholder review; until then, treat values as `informed gut` subject to next quarterly review.
- **Reviewer:** @nguyenvankiet (acting Product Owner, solo-dev, 2026-05-08). Formal stakeholder + legal counsel sign-off queued via GAP-156. Solo-dev exemption per `business-logic-review.md` §2.3 — the Reviewer line documents which hat is being worn AND obligation is attached for team-growth or pre-launch trigger.
- **Compliance check:** **Compliant** — Luật An ninh mạng 2018; Decree 53/2022/NĐ-CP Art 26 (data localization); OWASP ASVS L2 baseline.
- **Review cadence:** Quarterly (default per `business-logic-review.md` §2.5). **Next review:** 2026-08-08. Event triggers: Annual security audit, OWASP ASVS revision, hardening-baseline shift.

## Log

- 2026-04-15 — Doc created alongside Sub-PR 4.2 implementation (GAP-041).
