# GAP-666: Wave 98 business META cleanup — BR-ID javadoc refs + business README index sync

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Meta (business docs + code traceability hygiene)
**Found:** 2026-05-19 (Wave 98 post-closure audit suite — GAP-661 Business Logic audit)
**Affects:** `documents/01-business/README.md` + Java services trong kitehub-email + kitehub-platform + kitehub-subscription / business rule traceability

## Problem

GAP-661 Business Logic audit surfaced 2 P2 META hygiene issues khi audit Wave 98 3 new domains (preferences + email + seed):

1. **BR-ID javadoc refs absent in Java code** — `grep -E "BR-(EMAIL|SEED|PREFERENCES)-\d+"` across `kitehub/` returns **0 hits**. Business rules defined trong `documents/01-business/kitehub/{email,seed}/rules.md` (with IDs BR-EMAIL-001..007 + BR-SEED-001..010) nhưng matching Java service classes (`EmailTemplateRenderer`, `VietnamSampleDataGenerator`, `PreferencesController`) KHÔNG javadoc reference back. Future grep "find code implementing BR-X" returns empty.

2. **`documents/01-business/README.md` index not updated for 3 new domains** — index missing entries cho `preferences/`, `email/`, `seed/` (3 folders shipped Wave 98). Browse via README cannot discover new domains.

Impact: traceability chain BROKEN. Per `audit-to-gap-pipeline.md` Living Docs verification chain: `BR-xxx → UC-xxx → endpoint → @Mapping → @Test`. Without javadoc BR-IDs, BR-xxx → @Mapping link manual-only.

## Root Cause

Wave 98 agent ship convention: prioritize working code + minimal docs to declare contract. Code-↔-doc reverse linking (javadoc cite BR-ID) historically advisory not mandatory → silently drops in agent context budget. README index sync nằm ngoài scope của bucket agent (no agent owns `documents/01-business/README.md` per bucket scope).

## Proposed Fix

### Step 1: Annotate Java service classes với BR-ID javadoc

Pattern:

```java
/**
 * Renders Thymeleaf email templates.
 *
 * <p>Implements:
 * <ul>
 *   <li>{@code BR-EMAIL-001} — Default tone FORMAL_SAFE_DEFAULT for unknown role
 *   <li>{@code BR-EMAIL-002} — Plain-text sibling rendering for HTML-strip mail clients
 *   <li>{@code BR-EMAIL-005} — Per-tone variant resolution (Wave 99 scope)
 * </ul>
 *
 * @see documents/01-business/kitehub/email/rules.md
 */
@Component
public class EmailTemplateRenderer { ... }
```

Target classes:
- `EmailTemplateRenderer` → BR-EMAIL-001/002/005
- `ResendEmailService` → BR-EMAIL-003 (Resend channel)
- `SESEmailService` → BR-EMAIL-003 (SES channel)
- `VietnamSampleDataGenerator` → BR-SEED-001/002/003/004 (locale + 6 CSV mapping)
- `SeedWorkerService` → BR-SEED-005 (consumer wiring — defer Wave 99 per GAP-658)
- `PreferencesController` → BR-PREFERENCES-001 (banner dismiss TTL — once GAP-664 backfills rules.md)

### Step 2: Update `documents/01-business/README.md` Directory Map

Add 3 new entries:

```markdown
| `kitehub/preferences/` | User preferences (banner state, opt-outs) | rules.md + use-cases.md + api-contract.md |
| `kitehub/email/` | Email service (5 templates + 2 channels SES+Resend) | rules.md + use-cases.md + api-contract.md |
| `kitehub/seed/` | Tenant seed data (VN-friendly samples) | rules.md + use-cases.md + api-contract.md |
```

Note: entries depend on GAP-664 backfilling missing layers; coordinator merge order GAP-664 BEFORE this gap.

## Acceptance Criteria

- [ ] 6+ Java service classes annotated với BR-ID javadoc references
- [ ] `documents/01-business/README.md` Directory Map includes 3 new domain entries
- [ ] Grep `BR-(EMAIL|SEED|PREFERENCES)-\d+` returns ≥10 hits across `kitehub/` Java code
- [ ] Living Docs verification chain links: BR-EMAIL-001 → grep code → `EmailTemplateRenderer` line found
- [ ] Business Logic audit refresh next wave reflects fix → Cat 5 Traceability +2 pts

## Related

- **Parent audit:** `documents/04-quality/audits/business-logic/2026-05-19-wave-98-new-domains.md`
- **Blocking dependency:** GAP-664 (must backfill rules.md/use-cases.md trước khi annotate)
- **Rule:** CLAUDE.md §"Business Logic Documents 3-Layer Structure" verification chain
- **Rule:** `business-logic-review.md` §2 5-attribute coverage
