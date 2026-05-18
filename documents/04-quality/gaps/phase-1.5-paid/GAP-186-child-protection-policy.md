# GAP-186: Child Protection Policy (K-12 Minors)

**Status:** 🟡 PARTIAL — Phase 1 skeleton SHIPPED 2026-04-29 (Wave Legal-BRD Phase 1.5, PR #696). Phase 2 (legal counsel với VN child protection law expertise + MOLISA consultation if platform registration required + parental consent UI implementation + age verification workflow + safeguarding reporting channel + teacher vetting requirement onboarding + first quarterly safeguarding review) blocked-on stakeholder engagement → tracked GAP-154 umbrella per `gap-done-discipline.md` §3 PARTIAL exit-ramp. **Strategic blocker: P5 K-12 School persona deployment requires Phase 2 legal sign-off before any school onboarding.**
**Priority:** 🔴 P0 (business-logic tier — **Child Protection Law mandate**)
**Domain:** Legal / BRD / Child Safety / Compliance
**Found:** 2026-04-20 (BRD simulation — GAP-154 Phase 1; user K-12 focus)
**Wave:** Wave 8 Business Governance
**Affects:** K-12 tenants (P5 persona), parental consent flows, minor data protection, platform reputation, legal exposure

## Problem

No child protection policy. P5 K-12 School persona is strategic target (user's priority) but:
- No parental consent mechanism for minors (<16 VN) — VN PDPL Art 16 requires parental consent
- No minor-specific data handling (stricter retention, no marketing, no profiling)
- No child safety features defined (predator detection, adult user guards)
- No COPPA-equivalent compliance (for international/future)
- No teacher-student communication safeguards (required for safeguarding)

**Legal exposure:** without this policy, deploying to any school = VN law violation.

## Scope

Create `documents/00-brd/child-protection-policy.md`:

1. **Scope**
   - Who is a minor (VN PDPL: <16; internally stricter may apply)
   - Which persona combinations trigger (Student-in-P5, Student-in-P3 if < 16, Parent-in-P5)
2. **Parental Consent**
   - When required (account creation, data processing, communication)
   - Consent format (written, digital signature, verifiable)
   - Consent scope (what parent authorizes)
   - Withdrawal mechanism
   - Age verification process
3. **Minor Data Protection**
   - Stricter retention (per GAP-184 `sensitive-minor` category — 6 months max post-termination)
   - No marketing to minors
   - No profiling / behavioral advertising
   - No third-party sharing beyond educational purpose
   - Enhanced encryption / access control
4. **Safeguarding Rules**
   - Teacher-student communication MUST be platform-mediated (no direct DMs off-platform)
   - 1-to-1 calls require recording option + parent visibility
   - Report suspicious behavior flow
   - Mandatory reporting to authorities (grooming, abuse indicators)
5. **Platform Safety Features**
   - Content filtering stricter for minor accounts
   - Time-of-day restrictions option
   - Screen time reporting to parents
   - Emergency contact quick access
6. **Staff Safeguarding**
   - Teacher vetting requirement (at tenant level)
   - Background check documentation (at tenant level)
   - Code of conduct acceptance
7. **Incident Response** (child safety specific)
   - 24/7 hotline / priority support for safeguarding
   - Escalation to authorities (police, MOLISA — Ministry of Labor, Invalids and Social Affairs)
   - Evidence preservation
   - Notification to parents
8. **Training + Awareness**
   - Annual safeguarding training requirement for teachers
   - Materials for students (age-appropriate)
   - Parent awareness resources

## Acceptance Criteria

### Phase 1 (skeleton)

- [ ] `documents/00-brd/child-protection-policy.md` with 8 sections
- [ ] Age verification + consent flow diagram description
- [ ] Minor data handling matrix (data category × standard rules × minor-specific rules)
- [ ] Safeguarding incident classification (severity → response)
- [ ] Cross-references to GAP-154 umbrella, GAP-182 Privacy (minor section), GAP-184 Retention (minor category), GAP-181 AUP (education-specific prohibitions)
- [ ] Mandatory reporting list (to which VN authorities, under what circumstances)
- [ ] README link updated with K-12 priority flag

### Phase 2 (content + implementation)

- [ ] Legal counsel review (VN child protection law expertise)
- [ ] MOLISA consultation (if required for platform registration)
- [ ] Consent UI implemented (parental consent at signup, separate feature gap)
- [ ] Age verification workflow live
- [ ] Safeguarding reporting channel established
- [ ] Teacher vetting requirement documented (tenant onboarding)
- [ ] First quarterly safeguarding review scheduled

## Out of Scope

- **Parental consent UI** — separate frontend feature gap
- **Age verification implementation** — separate feature (may use ID verification service)
- **Tenant vetting process** — operational onboarding
- **Teacher background check integration** — tenant responsibility, not platform

## Dependencies

- GAP-154 umbrella
- GAP-182 Privacy Policy (minor section aligned)
- GAP-184 Retention (minor category stricter)
- GAP-181 AUP (minor-specific prohibitions)
- GAP-180 TOS (minor user section)
- GAP-052 parent portal (enables parent visibility — safeguarding dependency)
- Legal counsel with child protection law expertise

## Related

- Report: `brd-simulation-gap-finder-2026-04-20.md` §1.1 item Z
- VN Law: **Law on Children 2016** (Luật Trẻ em), **Decree 56/2017/NĐ-CP** (implementation), PDPL 2023 Art 16, **Decree 13/2023/NĐ-CP**, MOLISA circulars on child online safety
- International: COPPA (US, if expanding), UK Children's Code, Singapore PDPA child provisions
- Rule: `.claude/rules/meta-gap-priority.md` §3 — business-logic P0 + persona coverage impact (blocks P5 K-12 strategic segment)

## Log

- **2026-04-29** — Phase 1 skeleton SHIPPED (Wave Legal-BRD Phase 1.5, PR #696 squash-merged commit `cff9af72`). 475-line markdown file `documents/00-brd/child-protection-policy.md` với 11 sections (8 mandated + Mục lục + Cross-references + Update Log). **44 legal citations** inline: Luật Trẻ em 2016 (Law No. 102/2016/QH13), Decree 56/2017/NĐ-CP, PDPL Art 16, Decree 13/2023/NĐ-CP, Penal Code Art 142-147, MOLISA circulars on child online safety. International references: COPPA (US), UK Children's Code, Singapore PDPA child provisions. 5 tables/matrices: Persona × tenant type trigger matrix (§1), Minor data handling matrix 9 categories (§3.6), Safeguarding incident severity P0-P3 classification (§7.4), Mandatory reporting list 10 incident types × authority × VN law (§4.4), Age verification + parental consent ASCII flow diagram (§2.5). Frontmatter 9 fields including Strategic priority = P5 K-12 blocker. 11 Phase 2 TODO markers. Cross-links: 4 sibling skeletons (TOS/AUP/Privacy/Retention) + GAP-052 parent portal planned (×4 references). Status flipped 🔵 OPEN → 🟡 PARTIAL by coordinator (Phase 1 AC items 1-7 fully met; Phase 2 AC items 8-14 tracked under GAP-154 umbrella; **strategic gating** for P5 K-12 deployment).
- 2026-04-20 — Created as GAP-154 Phase 1 sub-gap. K-12 strategic segment blocker.
