# Security Hardening — Use Cases

---

## UC-SEC-001 — Tenant uploads malicious SVG logo

**Actor:** Tenant admin (branding wizard / upload form)
**Pre:** User authenticated, branding SPI configured (`security.svg.enabled=true`).

**Steps:**
1. User POSTs SVG file to `/api/v1/branding/logo`.
2. Backend reads byte stream → UTF-8 string `raw`.
3. Calls `svgSanitizer.sanitize(raw)` → `safe`.
4. `safe` is persisted to MinIO; `raw` is discarded.
5. Downstream render paths serve only `safe`.

**Errors / FE behavior:**
- If sanitizer returns empty string → FE shows "SVG could not be processed (contained unsafe markup)".
- FE preview renders `safe` in a sandboxed `<iframe sandbox>`.

---

## UC-SEC-002 — Service dispatches outbound webhook

**Actor:** System (webhook dispatcher)
**Pre:** Tenant has configured a callback URL in a previous UC.

**Steps:**
1. Worker pulls pending webhook → `targetUrl`, `tenantId`.
2. Calls `urlAllowlistValidator.isAllowed(targetUrl, tenantId)`.
3. If `false` → record delivery as `BLOCKED`, alert SRE, do NOT attempt HTTP.
4. If `true` → proceed with HTTP POST, record response.

**Errors / FE behavior:**
- Admin dashboard shows webhook `BLOCKED` reason with the denied URL (redacted hostname for internal).
- Tenant admin prompted to add the domain to the allowlist config.

---

## UC-SEC-003 — Browser submits state-changing form

**Actor:** Authenticated browser session
**Pre:** Session cookie present; CSRF cookie issued on first page load.

**Steps:**
1. FE reads CSRF cookie via `document.cookie`.
2. FE sends mutating request with `X-CSRF-Token` header set to the cookie value.
3. Backend filter resolves both via `CsrfTokenProvider.verify(header, cookie)`.
4. On `true` → request proceeds; on `false` → return `403 Forbidden`.

**Errors / FE behavior:**
- `403` due to CSRF fail → FE shows "Session expired, please refresh" and forces reload to obtain a
  fresh cookie.
- Expired token (past `ttl-hours`) behaves identically to mismatched token.

---

## UC-SEC-004 — Service boot-time secret validation

**Actor:** Platform (startup)
**Pre:** Environment provides `SECURITY_CSRF_SECRET`.

**Steps:**
1. Spring resolves `security.csrf.secret`.
2. `DoubleSubmitCsrfTokenProvider.@PostConstruct` checks length / known-bad values.
3. If invalid → Spring context startup fails with `IllegalStateException`; container exits.
4. Deployment pipeline fails loudly; operator sets a valid secret and redeploys.

**Errors / FE behavior:** N/A — pre-runtime guard.

---

## Log

- 2026-04-15 — Use-cases authored with Sub-PR 4.2.
