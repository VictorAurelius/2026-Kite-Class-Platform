# Rules — Consent (PDPL)

**Wave:** beta-readiness-4 Bucket B — GAP-353b
**Status:** ⚠️ PARTIAL — pointer only; canonical rules live in `marketing/rules.md`

## Canonical rules

Business rules `BR-PDPL-CONSENT-001..004` defined trong
[`documents/01-business/kitehub/marketing/rules.md`](../marketing/rules.md) Wave 23.
This domain folder hosts the immutable hash-chain implementation (Wave br-4 Bucket B)
which serves the same rules với additional integrity guarantees:

| Rule ID | Summary | Implementation |
|---------|---------|----------------|
| BR-PDPL-CONSENT-001 | Essential cookies locked-on | Server coerces `granted.essential=true` |
| BR-PDPL-CONSENT-002 | 12-month re-prompt cadence | Frontend `expiresAt` calc; backend respects |
| BR-PDPL-CONSENT-003 | Consent schema versioning | `granted` JSONB allows category evolution |
| BR-PDPL-CONSENT-004 | Withdrawal accessible as grant | POST `/v2/withdraw` — single-call API |

## Immutability extension (Wave br-4 Bucket B)

Wave 25 Bucket A path (visitor_id-based, upsert-in-place) chấp nhận flip flags
trên existing row. Wave br-4 Bucket B introduces **immutable + hash chain** path
cho post-login authenticated consent capture:

- Withdraw = INSERT new row với `granted={essential:true,analytics:false,marketing:false}` (NOT flip)
- `prev_hash` + `current_hash` SHA-256 chain prevents silent tampering
- RLS policies block UPDATE + DELETE at DB level

Detailed implementation: `kitehub-subscription/src/main/java/com/kitehub/subscription/consent/immutable/`.

## Related

- `api-contract.md` — 3 endpoints
- `../marketing/rules.md` — BR-PDPL-CONSENT-001..004
- `../../../04-quality/compliance/pdpl-pre-launch-checklist.md`
