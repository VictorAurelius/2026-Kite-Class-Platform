# GAP-181: Acceptable Use Policy (AUP)

**Status:** 🔵 OPEN
**Priority:** 🔴 P0 (business-logic tier — GA blocker)
**Domain:** Legal / BRD / Compliance / Moderation
**Found:** 2026-04-20 (BRD simulation — GAP-154 Phase 1)
**Wave:** Wave 8 Business Governance
**Affects:** Content moderation, user ban/suspension, DMCA response, tenant suspension

## Problem

No Acceptable Use Policy. Platform Admin has no documented basis to:
- Suspend tenant for content violations
- Ban end user for misuse
- Respond to DMCA/copyright complaints
- Define prohibited content categories

GAP-018 (content safety) tracks technical moderation (AI detection, reporting UI). AUP is the **legal/policy layer** on top — feeds moderation rules.

## Scope

Create `documents/00-brd/acceptable-use-policy.md` with sections:

1. **Scope + Acceptance** — applies to all users (tenant + end user)
2. **Prohibited Content**
   - Illegal content (VN Cybersecurity Law, Criminal Code)
   - CSAM (child sexual abuse material) — zero tolerance
   - Hate speech, harassment, threats
   - Adult/pornographic content
   - Copyright infringement
   - Misinformation, especially health/political per VN law
3. **Prohibited Conduct**
   - Account sharing, credential abuse
   - Bot traffic, scraping, rate limit bypass
   - Reverse engineering
   - Competitive intelligence gathering
   - Spam, phishing
4. **Education-Specific Prohibitions**
   - Academic fraud (plagiarism, proxy test-taking)
   - Selling answers, leaked exams
   - Impersonating teachers
   - Predatory behavior toward minors (ties to Child Protection GAP-186)
5. **Enforcement**
   - Warning system (strikes)
   - Suspension tiers (temporary vs permanent)
   - Content removal process
   - Appeal process
6. **Reporting** — how users report violations, SLA for review
7. **Platform Response Time** — SLA per severity
8. **Cooperation with Authorities** — when/how we disclose to MOET, police, courts

## Acceptance Criteria

### Phase 1 (skeleton)

- [ ] `documents/00-brd/acceptable-use-policy.md` skeleton created with 8 sections
- [ ] Frontmatter: `status: skeleton`, owner: Legal + Trust & Safety
- [ ] Prohibited content matrix (content type × platform response)
- [ ] Strike/suspension tier table
- [ ] Appeal flow skeleton
- [ ] Cross-references to GAP-018 (moderation tech), GAP-186 (child protection), GAP-042 (legal/IP)
- [ ] README link updated

### Phase 2 (content — legal counsel)

- [ ] Legal counsel review
- [ ] T&S team review  
- [ ] MOET alignment check (content guidelines for education platforms)
- [ ] Moderation playbook derived (separate doc in `05-guides/`)

## Out of Scope

- **Moderation technical implementation** (GAP-018 tracks)
- **Moderation queue UI** — separate feature gap
- **Automated detection models** — ML workstream

## Dependencies

- GAP-154 umbrella
- GAP-018 (content safety — feeds moderation rules)
- GAP-186 (child protection — stricter AUP for minors)

## Related

- Report: `brd-simulation-gap-finder-2026-04-20.md` §1.1 item C
- VN Law: Cybersecurity Law 2018, Criminal Code, MOET content circulars
- Rule: `.claude/rules/meta-gap-priority.md` §3

## Log

- 2026-04-20 — Created as GAP-154 Phase 1 sub-gap.
