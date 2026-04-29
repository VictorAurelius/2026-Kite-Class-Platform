# GAP-184: Data Retention + Deletion Policy

**Status:** 🟡 PARTIAL — Phase 1 skeleton SHIPPED 2026-04-29 (Wave Legal-BRD Phase 1, PR #690). Phase 2 (legal counsel + tax advisor sign-off on retention values + config externalization replacing GAP-108 hardcoding + deletion engineering SOP in 05-guides/ + tenant offboarding runbook + first quarterly audit) blocked-on stakeholder engagement → tracked GAP-154 umbrella + GAP-108 (config externalization) per `gap-done-discipline.md` §3 PARTIAL exit-ramp.
**Priority:** 🔴 P0 (business-logic tier — **VN PDPL Article 6 mandate**)
**Domain:** Legal / BRD / Data Protection / Ops
**Found:** 2026-04-20 (BRD simulation — GAP-154 Phase 1)
**Wave:** Wave 8 Business Governance
**Affects:** VN PDPL compliance, churn handling, storage cost, audit readiness, engineering SOP

## Problem

No data retention or deletion policy. Engineering improvises on churn/deletion. VN PDPL 2023 **Article 6** mandates:
- Data retained only as long as necessary for processing purpose
- Data subjects have right to request erasure (VN PDPL Art 11)
- Breach → uncoordinated handling

Also surfaces GAP-108 related issue: `StorageCleanupScheduler.SOFT_DELETE_GRACE_PERIOD_DAYS = 30` hardcoded without policy basis.

## Scope

Create `documents/00-brd/data-retention-deletion-policy.md`:

1. **Retention Categories + Periods**
   | Data Category | Active retention | Post-termination | Legal basis | Config key |
   |---------------|:----------------:|:----------------:|-------------|------------|
   | User accounts (active) | While tenant active | 30 days soft delete | Contract | `retention.user-account.days` |
   | Educational records (grades, attendance) | While tenant active | 5 years (MOET) | Education Law | `retention.edu-records.years` |
   | Financial records (invoices, payments) | While tenant active | 10 years (VN Tax Law) | Tax Law | `retention.financial.years` |
   | Audit logs | 1 year | 1 year post-termination | Cybersecurity Law | `retention.audit-log.days` |
   | Marketing consent records | While consent active | 3 years | PDPL | `retention.marketing-consent.years` |
   | AI generation outputs | While instance active | 30 days | Service contract | `retention.ai-output.days` |
   | Support tickets + chats | 2 years | 2 years | Consumer Law | `retention.support.years` |
   | Parent communication (SMS/Zalo logs) | 2 years | 1 year | PDPL | `retention.comm-logs.years` |
   | Student sensitive (health absences, conduct) | While enrolled | **6 months max** post-termination | PDPL minor | `retention.sensitive-minor.months` |
2. **Deletion Triggers**
   - Subject request (PDPL Art 11)
   - Retention period expiry
   - Tenant termination
   - Legal hold release
3. **Deletion Process**
   - Soft delete → hard delete timeline
   - Anonymization vs deletion (when to choose)
   - Backup purge alignment
   - Search index invalidation
   - Cache invalidation
4. **Legal Hold**
   - Disputes, investigations, regulatory inquiry
   - Override retention clock
   - Documentation + approval chain
5. **Exceptions**
   - Aggregated/anonymized analytics (no PII, retained indefinitely)
   - Legal archives (tax records, MOET required)
6. **Tenant Offboarding Runbook** — step-by-step (ops)
7. **Subject Erasure Request Runbook** — step-by-step (support + engineering)
8. **Audit Trail of Deletions** — what, when, why, by whom

## Acceptance Criteria

### Phase 1 (skeleton)

- [ ] `documents/00-brd/data-retention-deletion-policy.md` with 8 sections
- [ ] Retention matrix table populated with placeholders + legal basis
- [ ] Config keys documented (feeds per-domain `rules.md` and `application.yml`)
- [ ] Cross-references to GAP-108 (StorageCleanupScheduler), GAP-182 (Privacy Policy), GAP-117 (restore drill)
- [ ] Deletion process flow diagram description
- [ ] Legal hold documentation template

### Phase 2 (content + implementation)

- [ ] Legal counsel review
- [ ] Retention config externalized per category (replaces GAP-108 hardcoding for storage)
- [ ] Deletion engineering SOP in `05-guides/`
- [ ] Backup policy aligned
- [ ] Subject erasure request implementation (separate feature gap)
- [ ] Tenant offboarding runbook live
- [ ] First quarterly audit of actual vs documented retention

## Out of Scope

- **Erasure request UI** — separate frontend gap
- **Backup system redesign** — infrastructure scope
- **MOET reporting format** — GAP-192 (MOET Regulatory Matrix)

## Dependencies

- GAP-154 umbrella
- GAP-182 Privacy Policy (retention disclosures)
- GAP-108 payment/invoice config (storage retention hardcoded)
- GAP-117 restore drill (tests deletion completeness)
- GAP-186 Child Protection (stricter retention for minors)

## Related

- Report: `brd-simulation-gap-finder-2026-04-20.md` §1.1 item L
- VN Law: **Decree 13/2023/NĐ-CP** Art 6, Education Law (record retention), Tax Law (10 years), Cybersecurity Law (audit logs)
- Rule: `.claude/rules/meta-gap-priority.md` §3

## Log

- **2026-04-29** — Phase 1 skeleton SHIPPED (Wave Legal-BRD Phase 1, PR #690 squash-merged commit `80762ce3`). 195-line markdown file `documents/00-brd/data-retention-deletion-policy.md` với 9 sections (8 mandated + Phase 2 mở rộng). **Retention matrix với 9 data categories** + Phase 2 review column marking "informed gut Q3 2026" placeholders per `business-logic-review.md` §2 pattern (User accounts / Educational records / Financial records / Audit logs / Marketing consent / AI generation outputs / Support tickets / Parent comm logs / Student sensitive). 14 TODO/informed-gut markers (9 matrix rows + 5 inline placeholders). Frontmatter cites Decree 13/2023 Art 6 + Art 11 + MOET Education Law 2019 + VN Tax Law 2019 + ND 123/2020 + Cybersecurity Law 2018 + ND 53/2022 + Consumer Protection 2023 + GDPR Art 5(1)(e). Cross-links: GAP-182 sibling (privacy-policy.md), GAP-186/117/108/093 planned. Status flipped 🔵 OPEN → 🟡 PARTIAL by coordinator per `gap-done-discipline.md` §3 (Phase 1 AC items 1-6 fully met; Phase 2 AC items 7-12 tracked under GAP-154 umbrella + GAP-108 config externalization).
- 2026-04-20 — Created as GAP-154 Phase 1 sub-gap. VN PDPL Art 6 mandate.
