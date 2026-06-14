# GAP-353b: Server-side Consent API + Audit-log link (PDPL Phase 2)

**Status:** 🟡 PARTIAL — Wave 25 Bucket A 2026-05-06 (8/11 AC fully verified; 3 deepening items routed to follow-up [`GAP-353b-followup-multi-device-and-audit-chain.md`](./GAP-353b-followup-multi-device-and-audit-chain.md))
**Priority:** 🟠 P1 (PDPL Phase 2 — LocalStorage MVP from Wave 23 covers Art 11+13 read; server-side audit trail enriches but doesn't block legal compliance)
**Domain:** Backend / Compliance / Frontend integration
**Found:** 2026-05-06 (Wave 23 closure follow-up)
**Affects:** `kitehub-subscription` or new `kitehub-consent` module + DR-03 audit-log integration; downstream GAP-274/275/350 Track 2 marketing port

## Problem

Wave 23 ConsentBanner shipped LocalStorage-only state persistence. PDPL 2023 Art 11+13 (consent collection + revocation) satisfied for MVP, BUT:

- No server-side audit trail of consent records (BR-PDPL-CONSENT-003 retention 36mo via DR-03 — currently no row to retain)
- Multi-device/multi-browser users can't sync consent (each device has separate LocalStorage)
- Cross-tenant consent visibility for compliance audits = none
- DR-03 retention pipeline expects DB-resident records; LocalStorage doesn't flow to retention engine

Per Wave 23 plan §1 trade-off Q2 — Phase 2 enhancement deferred to keep Wave 23 wall-clock parallel-friendly.

## Current State (verified 2026-05-06 post-Wave-23-merge)

| Surface | Status |
|---|---|
| `packages/shared-ui/src/components/ConsentBanner/storage.ts` LocalStorage | ✅ shipped (Wave 23 BC) |
| `useConsent` hook with give/reject/revoke | ✅ shipped (Wave 23 BC) |
| Server consent API endpoint | ❌ missing |
| `consent_record` DB table | ❌ missing |
| DR-03 retention link (36mo) | ⚠️ rule exists in `data-retention/rules.md` but no records to retain |
| BR-PDPL-CONSENT-003 implementation | ⚠️ rule shipped Wave 23 A; backing data store missing |

## Proposed Fix

**Layer 1 — DB schema** (new Flyway migration):
```sql
CREATE TABLE consent_record (
  id BIGSERIAL PRIMARY KEY,
  visitor_id UUID NOT NULL,  -- pseudonymous; not linked to user account until login
  user_id BIGINT NULL,       -- nullable; populated after login if applicable
  tenant_id UUID NULL,       -- nullable for marketing-surface visitors pre-tenant
  essential_consented BOOLEAN NOT NULL DEFAULT TRUE,
  analytics_consented BOOLEAN NOT NULL DEFAULT FALSE,
  marketing_consented BOOLEAN NOT NULL DEFAULT FALSE,
  consent_version INTEGER NOT NULL DEFAULT 1,
  ip_address INET NULL,      -- for audit; PDPL-compliant retention
  user_agent TEXT NULL,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
  expires_at TIMESTAMP WITH TIME ZONE NOT NULL,  -- created_at + 12mo
  revoked_at TIMESTAMP WITH TIME ZONE NULL
);
CREATE INDEX idx_consent_record_visitor ON consent_record(visitor_id);
CREATE INDEX idx_consent_record_user ON consent_record(user_id) WHERE user_id IS NOT NULL;
CREATE INDEX idx_consent_record_expires ON consent_record(expires_at);
```

**Layer 2 — REST API** (likely in new `kitehub-consent` service OR extension to `kitehub-subscription`):
- `POST /api/v1/consent/record` — record/upsert consent (idempotent by visitor_id)
- `GET /api/v1/consent/{visitorId}` — query current state
- `POST /api/v1/consent/{visitorId}/revoke` — revoke flow

**Layer 3 — Frontend integration**:
- Extend `useConsent` hook in `packages/shared-ui/` to call API on consent change
- Generate/persist visitor_id (UUID v4 in LocalStorage) before first consent
- Sync flow: LocalStorage primary (offline-resilient) + server-side authoritative for cross-device

**Layer 4 — DR-03 retention link**:
- Cron job (or scheduled task) deletes consent_record rows where `expires_at < now() - 36 months`
- Audit-log entry on every record write (existing audit-log infrastructure per Wave 18 / Wave 19 hash-chain)

## Acceptance Criteria

- [x] Flyway migration shipped — `kitehub/kitehub-subscription/src/main/resources/db/migration/V25__create_consent_record.sql`
- [x] `consent_record` entity + repository + service — `com.kitehub.subscription.consent.{entity,repository,service}.*`
- [x] 3 REST endpoints implemented + Bean Validation — `ConsentController` (`POST /record`, `GET /{visitorId}`, `POST /{visitorId}/revoke`)
- [x] OpenAPI spec updated — `@Tag` + `@Operation` annotations on `ConsentController` (springdoc auto-publishes)
- [x] `useConsent` hook extended with API sync — `packages/shared-ui/src/components/ConsentBanner/useConsent.ts` (best-effort, LocalStorage primary)
- [x] visitor_id generation + LocalStorage persistence — `getOrCreateVisitorId()` in `storage.ts` + `kite_visitor_id` LocalStorage key
- [ ] Multi-device sync verified (same visitor_id + cross-browser test) — see follow-up [`GAP-353b-followup-multi-device-and-audit-chain.md`](./GAP-353b-followup-multi-device-and-audit-chain.md). Contract verified via unit/IT; live cross-browser Playwright run requires infra not available solo-dev mode.
- [x] DR-03 retention cron job — `ConsentRetentionCron` (daily 03:00, 36-month cutoff)
- [ ] Audit-log entries on consent write/revoke — currently SLF4J INFO only; hash-chain audit table routed to follow-up gap (pattern reuse from `ChildProtectionAuditServiceImpl` requires adaptation for pseudonymous visitor scope)
- [ ] Unit tests + IT (TestContainers Postgres) — Unit tests + IT shipped (`ConsentServiceImplTest`, `ConsentControllerTest`, `ConsentControllerIT`) using existing project H2 convention; TestContainers adoption decision routed to follow-up gap
- [x] BR-PDPL-CONSENT-003 implementation footer cross-link — added to `documents/01-business/kitehub/marketing/rules.md` BR-PDPL-CONSENT-003 entry; api-contract.md created

## Related

- Parent gap: GAP-353 (Wave 23 Phase 1)
- DB module: TBD — likely `kitehub-subscription` or new `kitehub-consent`
- Migration tracking: per project Flyway conventions
- `BR-PDPL-CONSENT-003` (consent retention 36mo)
- DR-03 retention rule in `documents/01-business/{kh,kc}/data-retention/rules.md`
- Audit-log infrastructure: `kiteclass-core/childprotection/service/ChildProtectionAuditServiceImpl` (hash-chain pattern, reusable)

## Effort estimate

~12-16h (~1.5-2 days). Single agent bucket OR pair-wave with GAP-353c (DSAR) as 2-bucket Phase 2 wave-pack.

## Log


- 2026-06-14: phase re-triage — phase-1-beta→phase-2 (title 'PDPL Phase 2' + counsel review Phase 2 trigger).
- **2026-05-06 (Wave 25 Bucket A):** Status flipped from OPEN → PARTIAL. Shipped: V25 Flyway
  migration + `consent` package (entity / repository / service / controller / DTOs / cron)
  + FE `api.ts` wrapper + `getOrCreateVisitorId` + `useConsent` API sync (best-effort
  LocalStorage-primary) + `api-contract.md` + BR-PDPL-CONSENT-003 implementation footer.
  Verification: `cd kitehub && ./mvnw -pl kitehub-subscription -am clean verify` →
  384/384 tests green; `pnpm -F @kite/shared-ui test` → 47/47; `pnpm -F @kite/shared-ui type-check` clean.
  Three AC items kept open and routed to follow-up
  [`GAP-353b-followup-multi-device-and-audit-chain.md`](./GAP-353b-followup-multi-device-and-audit-chain.md):
  cross-browser Playwright sync verification, hash-chain audit table, TestContainers Postgres
  IT migration. Per `gap-done-discipline.md` §3, parent stays PARTIAL until follow-up lands.
- **2026-05-06:** Filed at Wave 23 closure per wave plan §7 Closure Protocol. Server-side
  audit trail Phase 2 enhancement; LocalStorage MVP from Wave 23 BC suffices for PDPL
  Art 11+13 read.
