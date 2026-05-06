# GAP-353c: DSAR Self-Service Intake Form (PDPL Art 14 Phase 2)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2 (PDPL Art 14 doesn't mandate self-service; manual email-based DSAR acceptable for MVP)
**Domain:** Frontend / Compliance / Backend
**Found:** 2026-05-06 (Wave 23 closure follow-up)
**Affects:** `(public)/legal/data-rights/page.tsx` × KH+KC + DSAR backend ticketing

## Problem

PDPL 2023 Art 14 grants 6 data subject rights: access / rectification / erasure / portability / restrict-processing / object-to-processing. Wave 23 GAP-368 shipped Privacy Policy + Cookie Policy + Terms pages; rights are documented but no SELF-SERVICE intake form. Currently:

- Users wanting to exercise rights must email manually (likely `legal@kite.com` or similar)
- Response SLA per PDPL: 20-30 days
- No structured intake → response tracking is manual + error-prone

Phase 2 enhancement: ship `(public)/legal/data-rights/page.tsx` with structured form + backend DSAR ticket queue.

## Current State (verified 2026-05-06)

| Surface | Status |
|---|---|
| Privacy Policy production page | ✅ shipped (Wave 23 F) |
| §"Exercising Rights" in Privacy Policy | ✅ documented (cites email contact) |
| Self-service intake form | ❌ missing |
| DSAR ticket queue / SLA tracker | ❌ missing |

## Proposed Fix

**Frontend** (`(public)/legal/data-rights/page.tsx` × KH+KC):
- Form fields:
  - Right being exercised (radio: access / rectification / erasure / portability / restrict / object)
  - Identity: email + full name + national ID last 4 (PDPL identity verification)
  - Affected data scope (free text + dropdown for known data categories from Privacy Policy §4)
  - Reason (optional free text)
  - Contact preference (email / phone)
- Vietnamese-first; future-i18n EN scaffold
- Honeypot + reCAPTCHA (if available) — anti-spam since public surface

**Backend** (likely `kitehub-subscription` or new `kitehub-dsar` module):
- `POST /api/v1/dsar/request` — create ticket
- `GET /api/v1/dsar/{ticketId}` — status check (returns redacted state to non-staff)
- DSAR ticket entity + repository + service
- Email notification to DPO + acknowledgement to requester
- SLA timer (20-day default per PDPL)

**DSAR ticket DB schema**:
```sql
CREATE TABLE dsar_ticket (
  id BIGSERIAL PRIMARY KEY,
  ticket_uuid UUID UNIQUE NOT NULL,
  requester_email VARCHAR(320) NOT NULL,
  requester_name VARCHAR(200) NOT NULL,
  national_id_last4 VARCHAR(4) NOT NULL,
  right_type VARCHAR(50) NOT NULL,  -- ACCESS, RECTIFICATION, ERASURE, etc.
  scope TEXT NULL,
  reason TEXT NULL,
  status VARCHAR(50) NOT NULL DEFAULT 'PENDING',  -- PENDING, IN_REVIEW, COMPLETED, REJECTED
  sla_deadline TIMESTAMP WITH TIME ZONE NOT NULL,
  resolution TEXT NULL,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
  resolved_at TIMESTAMP WITH TIME ZONE NULL
);
```

## Acceptance Criteria

- [ ] `(public)/legal/data-rights/page.tsx` × KH+KC with form
- [ ] Form validation (client + server)
- [ ] reCAPTCHA / honeypot anti-spam
- [ ] Backend `POST /api/v1/dsar/request` endpoint
- [ ] `dsar_ticket` Flyway migration
- [ ] Email notification flow (DPO + requester acknowledgement)
- [ ] SLA timer (20-day deadline per PDPL Art 14)
- [ ] Privacy Policy §"Exercising Rights" updated to link to new form
- [ ] OpenAPI spec
- [ ] Tests: unit + IT
- [ ] BR-PDPL-DSAR-* business rules in `documents/01-business/kitehub/marketing/rules.md` extension OR new `dsar/rules.md` domain

## Related

- Parent gap: GAP-353 (PDPL master)
- Sister Phase 2: GAP-353b (server consent API), GAP-353d (DPIA docs)
- Privacy Policy: GAP-368 (production legal pages — Wave 23 F)
- BRD reference: `documents/00-brd/privacy-policy.md` §10-11 (rights + how to exercise)

## Effort estimate

~6-8h (~1 day). Single agent bucket. Pair-eligible with GAP-353b as 2-bucket Phase 2 wave-pack.

## Log

- **2026-05-06:** Filed at Wave 23 closure per wave plan §7 Closure Protocol. PDPL Art 14 doesn't mandate self-service per WG13/2023 implementing decree analysis — manual email DSAR acceptable for MVP. Self-service form is Phase 2 quality-of-life + audit-trail improvement.
