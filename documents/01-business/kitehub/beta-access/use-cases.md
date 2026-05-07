# Beta Access — Use Cases

**Domain:** Beta tenant invite mechanism
**Last verified:** 2026-05-08 (Wave 35 Bucket 0 Foundation)

---

## UC-BETA-001 — Submit beta access request (with PDPL consent)

**Actor:** Pre-tenant prospect (P1 solo teacher, or P2 center owner) — unauthenticated public visitor.
**Trigger:** Visitor lands on the public beta-signup landing page and decides to apply.
**Endpoint:** `POST /api/v1/auth/request-beta-access`

### Happy path

1. Prospect fills the form: `email`, `name`, `orgName`, `persona`, optional `referralSource`.
2. Prospect ticks the **PDPL consent checkbox** (`consentGiven=true`) acknowledging the privacy notice and terms (FE submit button stays disabled until ticked).
3. FE submits the JSON payload with `honeypot=""` (auto-populated empty by hidden input).
4. BE validates payload (`@Valid` on DTO) — `consentGiven=true`, persona in allowed enum, email/name/orgName non-blank, honeypot empty.
5. BE checks duplicate-email (BR-BETA-002): no active `PENDING`/`APPROVED` row for this email.
6. BE persists `BetaAccessRequest` with `status=PENDING`, `consent_given=true`, `consent_at=now()`.
7. BE emits `beta.consent.given` audit event via the per-module outbox emitter.
8. BE returns `201 Created` with `BetaRequestResponse` body (the requester learns: id + persisted snapshot, no invite token yet).
9. FE displays a success acknowledgement screen ("We received your request — coordinator will review and email you within X business days").

### Error branches

| Step | Failure | HTTP | Error code | FE behavior |
|------|---------|:----:|------------|-------------|
| 4 | `consentGiven=false`/`null`/missing | 400 | `BETA_CONSENT_REQUIRED` | Show inline error on consent checkbox; keep submit disabled |
| 4 | `email` fails `@Email` | 400 | `BETA_INVALID_EMAIL` | Inline field error |
| 4 | `persona` not in enum | 400 | `BETA_INVALID_PERSONA` | Persona radio shows error |
| 4 | `honeypot` non-empty | 400 | `BETA_HONEYPOT_FILLED` | (silent — bot trap; never shown to legitimate users) |
| 5 | duplicate active email | 409 | `BETA_DUPLICATE_EMAIL` | Banner: "An active request already exists for this email — check your inbox" |
| (gw) | rate-limit exceeded | 429 | `RATE_LIMITED` | Banner: "Too many requests — try again later" |

### FE behavior notes

- Submit button MUST be `disabled` whenever `consentGiven=false` (BR-BETA-001 enforcement at the FE layer; BE is the authoritative gate).
- Consent checkbox label MUST link to `/legal/privacy` and `/legal/terms` (open in new tab) — placeholder routes acceptable for Phase 1 BETA, real legal documents land Phase 3.
- The label text MUST be in Vietnamese as primary language (CLAUDE.md communication-language rule); English subtitle optional.
- Honeypot field is a hidden `<input>` — not announced to screen readers, not visible to legitimate users.

### Acceptance criteria (Bucket B — GAP-385)

- BE rejects missing/false `consentGiven` with `400 BETA_CONSENT_REQUIRED`.
- BE persists `consent_given` + `consent_at` columns on success.
- FE checkbox is unticked by default (PDPL Art 11 — Decree 13/2023/NĐ-CP).
- FE submit button is disabled until checkbox is ticked.
- Audit log entry `beta.consent.given` emitted via outbox.

---

## Other use cases (existing — Wave 33)

The remaining use-cases — UC-BETA-002 (token validate), UC-BETA-003 (signup completion), UC-BETA-004 (admin list), UC-BETA-005 (admin approve), UC-BETA-006 (admin reject) — were shipped as part of Wave 33 (GAP-372) and are documented in `api-contract.md` §Endpoints. They are stable and out-of-scope for Wave 35 except where Bucket A (GAP-384) hardens UC-BETA-004/005/006 with `@PreAuthorize`.

When Wave 36+ deepens the admin coordinator surface, this file should be expanded with full step-by-step happy-path narratives matching UC-BETA-001's level of detail.

---

## Related

- API contract: `documents/01-business/kitehub/beta-access/api-contract.md`
- Business rules: `documents/01-business/kitehub/beta-access/rules.md`
- Wave 35 plan: `documents/03-planning/waves/wave-2026-05-08-35-audit-p0-blockers-sprint.md`
