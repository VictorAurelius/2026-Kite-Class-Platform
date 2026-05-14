# Authentication — Domain Index

**Rules:** [`.claude/rules/docs-folder-structure.md`](../../../../.claude/rules/docs-folder-structure.md) + CLAUDE.md §"Business Logic Documents — 3-Layer Structure"

Domain governing user authentication: login flow, account lockout, 2FA TOTP enrollment + verify, recovery codes, login audit + admin alerts, JWT TTL + rotation, password complexity. Created Wave 72b Bucket 0 Foundation (paired with Wave 72a backend DB schema + Wave 72b Buckets A/B/C/D implementation).

---

## Files

| File | Purpose |
|------|---------|
| `README.md` | This index |
| `rules.md` | Layer 1 — business rules (BR-AUTH-001..010) with 5-attribute compliance per `business-logic-review.md` |
| `use-cases.md` | Layer 2 — actor-driven flows (UC-AUTH-001 login; UC-AUTH-002 enrollment; UC-AUTH-003 verify; UC-AUTH-004 recovery; UC-AUTH-005 login alert) |
| `api-contract.md` | Layer 3 — 5 new 2FA endpoint schemas + login response extension + outbox event payloads + error code reference |

---

## Cross-layer scope (Wave 72b)

This domain is the cross-layer source-of-truth (per `.claude/rules/contract-first-for-cross-layer.md`) for Wave 72b auth hardening. Bucket A BE (`TwoFactorController`, lockout extension, `LoginAuditService`) + Bucket B FE (2FA wizard + recovery codes UI) + Bucket C BE (login audit event) + Bucket D (email templates) all read these three documents to align on the auth-hardening contract.

## Related

- Source-of-truth controller: `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/auth/twofactor/TwoFactorController.java` (NEW Wave 72a Bucket A)
- MSW handler: `kitehub/kitehub-frontend/src/test/msw/handlers/auth.ts` (NEW Wave 72b Bucket 0)
- Wave plan: `documents/03-planning/waves/wave-2026-05-14-72b-2fa-audit-rubric-review.md`
- Pre-launch checklist: `.claude/rules/pre-launch-auth-hardening-checklist.md` (OWASP A07 8-check gate)
- DB schema: Wave 72a V35 (lockout columns) + V36 (totp + admin_audit_log)
