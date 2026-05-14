<!-- wave-plan-completeness-exempt: Pre-Wave-76 legacy plan — predates current _TEMPLATE.md structure -->

# Wave 0 — Stakeholder Inputs Checklist

**Purpose:** Collect non-engineering inputs required before later waves can start. Owned by stakeholders, not engineering.

**Referenced by:** [wave-roadmap-p0.md](./wave-roadmap-p0.md)

---

## Hard Blockers (Day 1 of target wave)

### ☐ AI SLA Targets per Tier → Wave 3

**Needed:** P50/P95/P99 latency targets for each tier.

| Tier | P50 target | P95 target | P99 target | Concurrency cap per tenant |
|------|:----------:|:----------:|:----------:|:---------------------------:|
| Free | ? | ? | ? | ? (suggested: 1) |
| Pro | ? | ? | ? | ? (suggested: 3) |
| Enterprise | ? | ? | ? | ? (suggested: 10) |

**Owner:** Product / Tech Lead
**Due:** Before Wave 3 (Week 5)
**Default if no input:** suggested values above

---

### ☐ Pricing Model for AI Usage Metering → Wave 6

Choose one:
- ☐ Per-token markup (e.g., 2x cost from Ollama/OpenAI)
- ☐ Flat per-request (e.g., 0.5 VND/request for Free, 0.2 for Pro, 0 for Enterprise)
- ☐ Tiered bucket (e.g., 1000 free requests/mo, then 1 VND each)

**Overage threshold default:** when tenant hits N% of monthly quota, trigger notification.

**Owner:** Finance / Product
**Due:** Before Wave 6 (Week 12)

---

### ☐ Legal Firm Engagement → Wave 9 (and Wave 2 soft)

**Scope:**
- VN education law (MoET circulars on grade formula, attendance, academic warning)
- Personal Data Protection Law (PDPL) — parent PII consent
- Labor law — teacher payroll, VN tax/BHXH (for Wave 11)
- Review 3 critical business rules in code:
  1. Grade calculation weights
  2. Attendance threshold academic warning (25%? confirm)
  3. Parent consent capture on student PII

**Lead time:** 2–4 weeks (contract + first engagement)

**Owner:** Legal / CEO
**Due:** Start contract **today**. Outputs due before Wave 9 (Week 13).

---

## Soft Blockers (needed before target wave, not Day 1)

### ☐ Bulk Import xlsx Schema → Wave 1

Lock the column schema before Day 1 of Wave 1:

**Suggested columns (K-12 pilot):**
| Column | Required | Format | Notes |
|--------|:--------:|--------|-------|
| name | ✓ | text | Full Vietnamese name |
| email | ✓ | email | Unique per tenant |
| phone | ○ | VN format | Normalize +84/0 prefix |
| date_of_birth | ✓ | dd/mm/yyyy | VN format |
| grade_level | ✓ | text | e.g., "10A1", "11B2" |
| homeroom_class | ○ | text | Maps to existing Class entity |
| parent_email | ○ | email | For future parent invite (Wave 2) |

**Duplicate policy:** ☐ Reject entire batch ☐ Skip-and-report ☐ Fail-on-first

**Owner:** Product
**Due:** Before Wave 1 (Week 1)

---

### ☐ VN PDPL / MoET Parent PII Guidance → Wave 2

Consent wording required for:
- Parent signup screen (what data stored, how used, retention)
- Email invitation (what child data parent will see)
- Dashboard (visible data scope)

**Dual-parent-same-email policy:** ☐ Allow (share account) ☐ Reject (require distinct email per parent)

**Owner:** Legal (with legal firm engaged)
**Due:** Before Wave 2 production ship. Dev can start behind feature flag.

---

### ☐ Document Template Visuals → Wave 8, 10, 11

**Template design files needed:**
- ☐ Student enrollment certificate (Wave 8 reference) — PDF mockup
- ☐ Report card (Wave 10) — MoET-compliant, VN K-12 format — PDF mockup
- ☐ Payroll slip (Wave 11) — VN labor compliant — PDF mockup
- ☐ Logo usage guidelines — how tenant branding appears on each doc

**Owner:** Design / Product
**Due:** Before respective waves

---

## Technical Audit (Engineering-owned)

### ☐ GAP-002 Async Pipeline Status → Wave 3

**Audit scope:**
- Is `BrandingJobConsumer` fully async (RabbitMQ listener, no sync HTTP)?
- Is `AIProviderConfig` wrapping Ollama/OpenAI in an async pattern?
- Are there any sync HTTP calls to AI providers from controllers or services?
- Is the job tracking entity (AIJob, BrandingJob) complete?

**If gaps found:** Wave 3 scope expands by 1-2 weeks to complete GAP-002 first.

**Owner:** Tech Lead
**Due:** Before Wave 3 kickoff (Week 5)
**Action:** Read these files and document findings:
- `kitehub/kitehub-branding/src/main/java/com/kitehub/branding/config/AIProviderConfig.java`
- `kitehub/kitehub-branding/src/main/java/com/kitehub/branding/queue/BrandingJobConsumer.java`
- `kitehub/kitehub-branding/src/main/java/com/kitehub/branding/service/BrandingJobService.java`
- `kitehub/kitehub-branding/pom.xml` (RabbitMQ, Redis deps)

---

## Status Tracking

| Input | Owner | Status | Deadline |
|-------|-------|:------:|----------|
| AI SLA targets | Product | ⏳ OPEN | Week 5 |
| AI pricing model | Finance | ⏳ OPEN | Week 12 |
| Legal firm LOI | Legal/CEO | ⏳ OPEN | Start today |
| Bulk import schema | Product | ⏳ OPEN | Week 1 |
| VN PDPL guidance | Legal | ⏳ OPEN | Week 4 |
| Doc templates | Design | ⏳ OPEN | Week 15+ |
| GAP-002 audit | Tech Lead | ⏳ OPEN | Week 4 |

---

## How to Update This Doc

When an input is resolved:
1. Change status from ⏳ OPEN to ✅ DONE
2. Add brief note with date + reference (where documented)
3. Commit via standard PR flow

Example:
```
| AI SLA targets | Product | ✅ DONE | Week 5 | 2026-04-25: documented in product-brief.md |
```
