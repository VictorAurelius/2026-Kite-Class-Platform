# Legal / IP Protection — Business Rules

**Domain:** legal-ip-protection
**Source:** Wave 4 Sub-PR 4.3, ADR-012 (DMCA / trademark workflow), GAP-042

## Rules

### DMCA workflow (reactive — Track 2)

| ID | Rule |
|----|------|
| BR-DMCA-001 | DMCA takedown status transitions enforced by `DmcaStatus` state machine: `PENDING → REVIEWING → VALID / INVALID → EXECUTED / CONTESTED`. Invalid transitions throw `IllegalStateException`. |
| BR-DMCA-002 | `REVIEWING → VALID` means the notice is legitimate; affected branding asset SHOULD be flagged for revert. (Stubbed — logging only in Sub-PR 4.3; actual asset flagging deferred.) |
| BR-DMCA-003 | `VALID → EXECUTED` means the asset has been reverted to the TEMPLATE category (DMCA §512 takedown complete). |
| BR-DMCA-004 | `VALID → CONTESTED` means a counter-notice was received; asset stays live until court order or grace window (10–14 business days per §512(g)). |
| BR-DMCA-005 | Terminal states: `INVALID`, `EXECUTED`, `CONTESTED`. No further mutation permitted. |
| BR-DMCA-006 | Every state transition (including intake) MUST write one `AuditLog` row in the same transaction (composes with BR-AUDIT-001). |
| BR-DMCA-007 | Counter-notice email dispatch to the original reporter is deferred — Ops team notifies reporter manually for now. |
| BR-DMCA-008 | Public intake (`POST /public/dmca`) MUST be rate-limited by the gateway `RateLimitingFilter` (reused, no new filter). |

### Trademark workflow (proactive — Track 1)

| ID | Rule |
|----|------|
| BR-TM-001 | `TrademarkCheckService.checkTextKeywords` MUST run against any tenant-supplied branding text (name, tagline, prompt) BEFORE publishing an AI-generated asset. (Wire-up target: Wave 3 `GenerateLogoStep` — deferred.) |
| BR-TM-002 | Match is case-insensitive substring; multi-word keywords supported. |
| BR-TM-003 | On non-clear result, the pipeline MUST route the resource to `ResourceCategory.TEMPLATE` fallback (no AI generation with flagged inputs). |
| BR-TM-004 | Seed list curated in `legal.trademark.banned-keywords`. Tenant-level override list NOT implemented in this sub-PR. |
| BR-TM-005 | Fuzzy-matching and USPTO-API integration are deferred. |

## Config keys

| Key | Type | Default | Rule |
|-----|------|---------|------|
| `legal.trademark.banned-keywords` | `List<String>` | `["Nike","Adidas","Apple Inc"]` | BR-TM-004 — seed list only |

## Migration version reserved

- V37 → `dmca_takedown_requests` (this sub-PR)

## Log
- 2026-04-15 — Rules established (Wave 4 Sub-PR 4.3, GAP-042)
