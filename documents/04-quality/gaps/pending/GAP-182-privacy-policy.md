# GAP-182: Privacy Policy — VN PDPL 2023 + GDPR Compliance

**Status:** 🟡 PARTIAL — Phase 1 skeleton SHIPPED 2026-04-29 (Wave Legal-BRD Phase 1, PR #691). Phase 2 (legal counsel với VN PDPL expertise + DPO designation + MPS A05 consultation if sensitive-data registration required + EN translation + cookie consent banner UI) blocked-on stakeholder engagement → tracked GAP-154 umbrella per `gap-done-discipline.md` §3 PARTIAL exit-ramp. **Critical path: Decree 13/2023/NĐ-CP effective 2026-07-01** — Phase 2 must complete before live tenant signup of any commercial customer.
**Priority:** 🔴 P0 (business-logic tier — **legal mandate**)
**Domain:** Legal / BRD / Data Protection
**Found:** 2026-04-20 (BRD simulation — GAP-154 Phase 1)
**Wave:** Wave 8 Business Governance
**Affects:** VN PDPL compliance (mandatory), payment onboarding, EU traffic handling, data subject rights

## Problem

No Privacy Policy. **Direct legal violation** of VN Personal Data Protection Law 2023 (Decree 13/2023/NĐ-CP) — Article 11 requires data subject notice before processing. Also blocks:
- Payment processor onboarding (all require published privacy policy)
- Enterprise procurement (due diligence checkpoint)
- EU user handling (GDPR extraterritorial application)
- App store listing (Play/App Store require privacy policy URL)

## Scope

Create `documents/00-brd/privacy-policy.md` with mandatory sections per VN PDPL Decree 13/2023:

1. **Data Controller Identity** — company registration, contact
2. **Data Protection Officer** (DPO) — contact for data subject requests
3. **Data Subject Categories** — tenant users (admin, teacher, student, parent, accountant, etc.)
4. **Data Categories Processed**
   - Identification (name, DOB, gender, ID number for billing)
   - Contact (email, phone, address)
   - Educational (grades, attendance, homework, conduct for K-12)
   - Financial (payment info, invoices)
   - Technical (IP, device, session)
   - Sensitive: health (absence reasons), minors' data (K-12)
5. **Processing Purposes** — education delivery, billing, support, analytics, AI features
6. **Legal Basis** — contract (TOS), consent (marketing), legal obligation (MOET reporting), legitimate interest
7. **Data Sharing** — third parties (VNPay, MoMo, Zalo, Google Workspace, hosting), no selling
8. **Cross-Border Transfer** — if any (AI Ollama local vs OpenAI international)
9. **Retention Period** — per data category (ties to GAP-184)
10. **Data Subject Rights** (VN PDPL Art 11)
    - Right to know
    - Right to access
    - Right to rectification
    - Right to erasure
    - Right to restrict processing
    - Right to object
    - Right to data portability (ties to GAP-188 future)
    - Right to lodge complaint with MPS A05
11. **Exercising Rights** — contact, response SLA (20-30 days per VN PDPL)
12. **Minor Data** (< 16 years in VN) — parental consent (ties to GAP-186)
13. **Security Measures** — encryption, access control, audit
14. **Breach Notification** — 72 hours per VN PDPL (ties to GAP-190 Incident Response)
15. **Cookie Policy** — session cookies, analytics, consent mechanism
16. **Changes** — notice period, re-consent

## Acceptance Criteria

### Phase 1 (skeleton)

- [ ] `documents/00-brd/privacy-policy.md` skeleton with 16 sections
- [ ] Data category matrix (category × purpose × legal basis × retention)
- [ ] Data subject rights table (right × how to exercise × response SLA)
- [ ] Cross-references to GAP-184 (retention), GAP-186 (minor), GAP-190 (breach notification)
- [ ] DPO designation placeholder
- [ ] Cookie consent banner implementation note (separate feature gap)
- [ ] README link updated

### Phase 2 (content — legal counsel + MPS consultation)

- [ ] Legal counsel review (VN PDPL expert)
- [ ] Data Protection Officer designated
- [ ] Data processing register maintained (internal doc)
- [ ] Vietnamese + English versions
- [ ] A05 (Cybersecurity and High-Tech Crime Police) registration if required for sensitive data
- [ ] Cross-border transfer impact assessment (if applicable)
- [ ] Cookie consent banner live
- [ ] Status: `skeleton` → `approved` → `published`

## Out of Scope

- **Cookie banner UI** — separate frontend feature gap
- **Data subject request handling workflow** — ops runbook, separate gap
- **A05 registration** — operational task, not BRD doc

## Dependencies

- GAP-154 umbrella
- GAP-180 (TOS — references Privacy Policy)
- GAP-184 (Data Retention — aligned retention periods)
- GAP-186 (Child Protection — parental consent)
- GAP-190 (Incident Response — breach notification)
- Legal counsel with VN PDPL expertise

## Related

- Report: `brd-simulation-gap-finder-2026-04-20.md` §1.1 item D
- VN Law: **Decree 13/2023/NĐ-CP** (Personal Data Protection), Cybersecurity Law 2018, Law on Electronic Transactions 2023
- International: GDPR Articles 13-14 (notice), Articles 15-22 (rights)
- Rule: `.claude/rules/meta-gap-priority.md` §3

## Log

- **2026-04-29** — Phase 1 skeleton SHIPPED (Wave Legal-BRD Phase 1, PR #691 squash-merged commit `4a271cf3`). 319-line markdown file `documents/00-brd/privacy-policy.md` với 22 sections (16 mandated PDPL sections + Tóm tắt + Data Category Matrix + Data Subject Rights Table + Cross-References + Phase 2 TODO Tracker + Log). **61 PDPL article citations** inline (Decree 13/2023 Art 3/9/10/11/12/13/14/15/16/17/20/23/25/27/28). 4 tables: Section 6 (Purpose × Legal Basis), Section 9 (Retention placeholder), Section 10 (Rights inline), Bottom (Data Category Matrix + Data Subject Rights table). Frontmatter 8 fields including Languages (Vietnamese canonical, EN Phase 2). Cross-links: 7 markdown links — GAP-184 (×2 sibling), GAP-186/190 (planned), compliance-scope.md (×2), personas-catalog.md. Phase 2 TODO markers at company entity, DPO email, retention values cross-link GAP-184, breach contact, MPS registration status. **Worktree-contamination incident** during Agent C execution: Write tool initially landed file at main worktree path (not agent worktree); caught immediately on first verification grep, copied to correct path, removed stray, committed cleanly. No upstream contamination. Per `feedback_worktree_absolute_path_contamination.md` documented hazard. Status flipped 🔵 OPEN → 🟡 PARTIAL by coordinator per `gap-done-discipline.md` §3 (Phase 1 AC items 1-7 fully met; Phase 2 AC items 8-15 tracked under GAP-154 umbrella; PDPL effective date 2026-07-01 = Phase 2 hard deadline).
- 2026-04-20 — Created as GAP-154 Phase 1 sub-gap. VN PDPL mandatory.
