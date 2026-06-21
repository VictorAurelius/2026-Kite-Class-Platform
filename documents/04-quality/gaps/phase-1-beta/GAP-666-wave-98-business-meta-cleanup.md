# GAP-666: Wave 98 business META cleanup — BR-ID javadoc refs + business README index sync

**Status:** 🟡 PARTIAL (50% — README index sync DONE; BR-ID javadoc refs → GAP-1522)
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

- [x] `documents/01-business/README.md` Directory Map includes 3 new domain entries — **DONE 2026-06-21, scope expanded**: index was stale far beyond "3 new domains" (chỉ liệt kê 9 KiteHub + 16 KiteClass = 25 trong khi thực tế 27 KiteHub + 48 KiteClass = 75 domains, tất cả đủ 3 layers). README §5 index now lists toàn bộ 75 domains; §1 tree comment + counts cập nhật; marketing/consent/email/preferences flip từ PARTIAL/missing → ✅ (GAP-1516 + GAP-664 backfilled). `check-3-layer-completeness.sh` = 75/75 complete.
- [ ] 6+ Java service classes annotated với BR-ID javadoc references → **DEFERRED → GAP-1522** (code-heavy, spans 3 services)
- [ ] Grep `BR-(EMAIL|SEED|PREFERENCES)-\d+` returns ≥10 hits across `kitehub/` Java code → **DEFERRED → GAP-1522**
- [ ] Living Docs verification chain links: BR-EMAIL-001 → grep code → `EmailTemplateRenderer` line found → **DEFERRED → GAP-1522**
- [ ] Business Logic audit refresh next wave reflects fix → Cat 5 Traceability +2 pts → **DEFERRED → GAP-1522** (downstream of javadoc annotation)

## Out-of-scope / Deferred (per gap-done-discipline.md §3 PARTIAL exit ramp)

| Item | Where |
|------|-------|
| BR-ID javadoc annotation cho 6 Java classes (EmailTemplateRenderer / ResendEmailService / SESEmailService / VietnamSampleDataGenerator / SeedWorkerService / PreferencesController) | **GAP-1522** — code-heavy, converts to code PR (3 services), separable từ docs-only README sync |
| BR-(EMAIL\|SEED\|PREFERENCES) grep ≥10 hits | GAP-1522 (consequence of javadoc annotation) |
| Business Logic audit Cat 5 Traceability +2 refresh | GAP-1522 (downstream of javadoc) |

Verified 2026-06-21: `grep -rnE "BR-(EMAIL|SEED|PREFERENCES)-[0-9]+" kitehub/ --include="*.java"` = 0 hits (javadoc deliverable confirmed not-yet-done). Target classes all present (4 main + 1 test + SeedWorkerService).

## Log

- **2026-06-21:** README index sync DONE (GAP-666 unblocked by GAP-664). Index was stale far beyond original "3 new domains" scope — synced `documents/01-business/README.md` §5 to list all 75 domains (27 KiteHub + 48 KiteClass), all verified 3-layer-complete via `check-3-layer-completeness.sh` (75/75). §1 tree + counts + `**Last Updated:**` header updated; marketing/consent/email/preferences flipped to ✅ (GAP-1516 + GAP-664 backfills). BR-ID javadoc annotation (AC #1/#3/#4/#5) deferred to **GAP-1522** per `gap-done-discipline.md` §3 — code-heavy, spans 3 services, would convert this docs-only PR into a code PR. Status OPEN → 🟡 PARTIAL (50% — docs half done, code half tracked). Gap stays in `phase-1-beta/` (PARTIAL, not closed/).

## Related

- **Parent audit:** `documents/04-quality/audits/business-logic/2026-05-19-wave-98-new-domains.md`
- **Blocking dependency:** GAP-664 (must backfill rules.md/use-cases.md trước khi annotate)
- **Rule:** CLAUDE.md §"Business Logic Documents 3-Layer Structure" verification chain
- **Rule:** `business-logic-review.md` §2 5-attribute coverage
