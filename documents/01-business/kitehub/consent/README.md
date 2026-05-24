# Domain — Consent (PDPL Decree 13/2023)

**Wave:** beta-readiness-4 Bucket B — GAP-353b
**Status:** ⚠️ PARTIAL — v1 dev-implementation; counsel formal review queued Phase 2

Server-side immutable consent record + hash chain audit cho PDPL Decree 13/2023
Article 11-14 (informed consent, accessible withdrawal, retention discipline).

## Files

| File | Purpose |
|------|---------|
| `rules.md` | Business rules — coexists với `marketing/rules.md` for shared `BR-PDPL-CONSENT-001..004`; nội dung domain-specific deferred to next wave |
| `api-contract.md` | 3 endpoints — POST `/api/v1/consent/v2/record` + GET `/api/v1/consent/v2/{userId}` + POST `/api/v1/consent/v2/withdraw` |

## Related

- `documents/04-quality/compliance/pdpl-pre-launch-checklist.md`
- `documents/02-architecture/adr/ADR-034-cookie-consent-vendor.md`
- `documents/01-business/kitehub/marketing/rules.md` (BR-PDPL-CONSENT-001..004)
- `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/consent/immutable/` — implementation
