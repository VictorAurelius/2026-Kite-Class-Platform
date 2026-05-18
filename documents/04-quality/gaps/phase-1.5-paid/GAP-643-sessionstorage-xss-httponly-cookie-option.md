# GAP-643: sessionStorage XSS Phase 1.5+ httpOnly cookie option (same-document XSS hardening)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Frontend / Security
**Detected:** 2026-05-18 (Wave 92 post-wave Security audit v2 P2-1 NEW finding per GAP-619)
**Related Audits:** `documents/04-quality/audits/security/2026-05-18-wave-92-security-audit-v2.md`

## Current State (verified 2026-05-18)

| Piece | File / Path | Status |
|-------|-------------|--------|
| sessionStorage facade | `kitehub/kitehub-frontend/src/lib/jwt-storage.ts` (Wave 85 Bucket B) | ✅ shipped |
| Persistent XSS hardening | sessionStorage tab-scoped vs localStorage persistent | ✅ reduces persistent XSS attack surface |
| Same-document XSS hardening | None | ❌ same-document XSS still possible (sessionStorage accessible to any script in same tab) |
| httpOnly cookie alternative | None | ❌ not implemented |

## Problem

Wave 85 Bucket B migrated JWT storage từ localStorage → sessionStorage giảm persistent XSS surface (tab-scoped lifetime). Tuy nhiên same-document XSS vẫn possible — bất kỳ script nào trong same tab có thể access `sessionStorage` reads JWT.

Per Wave 92 Security audit v2 P2-1 NEW finding — Phase 1.5+ option migrate sang httpOnly cookie với `SameSite=Strict` cho full XSS hardening. Cookie không accessible từ JavaScript → eliminates JWT exfiltration via any XSS vector.

## Context

Wave 92 post-wave audit suite (GAP-619) shipped 2026-05-18; this gap surfaced từ Security audit v2 P2-1 finding. Paired với GAP-625 KYC (Phase 1.5a P0) + GAP-578 P2 Owner 2FA Phase 1.5a sequential — full auth hardening cluster.

## Proposed Fix

1. **Design doc** — httpOnly cookie migration strategy:
   - Cookie attributes: `httpOnly` + `Secure` + `SameSite=Strict` + path-scoped
   - Refresh token rotation flow via cookie
   - CSRF protection (SameSite=Strict primary; CSRF token secondary for legacy browsers)
2. **Backward compat plan** — fallback sessionStorage cho older browsers KHÔNG support SameSite=Strict (rare 2026+)
3. **Cookie-based JWT verification middleware** — extract JWT từ `Cookie` header thay vì `Authorization: Bearer` for browser flows
4. **Cost-benefit analysis** — engineering effort vs XSS risk reduction (low fraud rate Phase 1 BETA; higher when paid market opens)
5. **Migration path** — feature flag rollout per tenant tier

## Acceptance Criteria

- [ ] Design doc shipped tại `documents/02-architecture/adr/ADR-NNN-httponly-cookie-jwt-storage.md`
- [ ] Cost-benefit analysis với fraud rate projection Phase 1.5+ vs current sessionStorage
- [ ] Backward compat strategy documented
- [ ] CSRF protection design (SameSite=Strict + token fallback if needed)
- [ ] Migration path Phase 1.5b+ với feature flag rollout
- [ ] Pre-handoff self-test per `pre-handoff-self-test-completeness.md` §2.6 Payment flow + §2.10 Time-sensitive (cookie expiry + clock skew)

## Related

- **Audit origin:** `documents/04-quality/audits/security/2026-05-18-wave-92-security-audit-v2.md` P2-1 NEW finding
- **Wave plan:** `documents/03-planning/waves/wave-2026-05-18-94c-gap-619-wave-92-audit-suite.md`
- **Parent gap:** GAP-619 (this gap surfaces from Wave 92 post-wave audit suite)
- **Related auth gaps:** GAP-625 (KYC), GAP-578 (P2 Owner 2FA), GAP-577 (platform admin hardening)
- **Rules:** `pre-launch-auth-hardening-checklist.md`

## Log

- **2026-05-18** — Initial write-up. Filed from Wave 92 post-wave audit suite (GAP-619) Security audit v2 P2-1 NEW finding. State-check confirms sessionStorage facade shipped Wave 85 Bucket B reduces persistent XSS but same-document XSS still possible. Priority P2 — phase-1.5-paid scope (full hardening when paid market opens).
