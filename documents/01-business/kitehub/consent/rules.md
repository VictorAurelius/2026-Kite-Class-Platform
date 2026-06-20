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

## Five-attribute review per `business-logic-review.md` §2

> Pointer stub — the canonical 5-attribute review for `BR-PDPL-CONSENT-001..004` lives in [`../marketing/rules.md`](../marketing/rules.md). This folder hosts only the immutable hash-chain implementation; the block below covers that integrity extension.

- **Source:** Delegated — consent business rules sourced + reviewed in `marketing/rules.md` (Wave 23, PDPL consent design). Immutability extension (Wave br-4 Bucket B, GAP-353b): engineering decision (SHA-256 hash chain + RLS UPDATE/DELETE block) implementing tamper-evidence for the same rules.
- **Rationale:** Authenticated post-login consent capture needs non-repudiation (withdraw = INSERT new row, not flip) so a tenant/auditor can prove consent state over time — PDPL accountability principle. Hash chain + RLS block silent tampering.
- **Reviewer:** @nguyenvankiet (acting Legal scout + Tech Lead, solo-dev, 2026-06-21). Formal DPO/legal counsel review queued — GAP-156 AC-D.
- **Compliance check:** **Considered (self-assessed, counsel pending GAP-156 AC-D)** — per `documents/00-brd/compliance-checklist.md` L1/L7: **Nghị định 13/2023/NĐ-CP (PDPL) Điều 11** (consent legal basis + time/version stamping, BR-PDPL-CONSENT-001/002/003); **Luật Giao dịch điện tử 2023** (electronic consent record = valid e-transaction); withdrawal accessible as grant (BR-PDPL-CONSENT-004) supports the PDPL right-to-withdraw. No counsel verification of version-stamping sufficiency yet.
- **Review cadence:** **Annual** + event-driven on PDPL implementing-decree publication. **Next review:** 2026-09-21 (next audit checkpoint), then Annual.
