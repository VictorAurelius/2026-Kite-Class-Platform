# Marketing + Legal Review Workflow

End-to-end workflow from draft to publish to archive. Different paths for marketing copy vs legal docs.

---

## Path A: Marketing Copy (landing, pricing, blog, brochure)

### A1. Draft
- Author creates draft in feature branch (`feat/marketing-copy-{topic}`)
- Store copy in `documents/marketing/drafts/{slug}.md` OR directly in FE component file
- Include source citations for any claim (pricing, stats, testimonials)

### A2. Internal review (Marketing + Brand)
Reviewer checks (1-2 business days):
- Brand voice consistent (see `documents/brand/voice-guide.md` — file separately if missing)
- Claims substantiated (no "#1 in Vietnam" without source)
- Competitor references generic (never name competitor negatively)
- CTA clear + tracked (UTM params, analytics event)
- SEO metadata set (title ≤60 chars, description ≤160 chars, OG image)
- i18n — VN primary, EN for international pages

### A3. Legal spot-check (optional for blog, MANDATORY for pricing)
Pricing pages, warranty claims, "free forever" promises, guarantees → counsel spot-check (1 business day).

### A4. Publish
- Merge PR → auto-deploy via CI
- Capture production screenshot to `documents/marketing/published/{slug}-YYYY-MM-DD.png`

### A5. Archive
Old marketing copy moves to `documents/07-archived/marketing-YYYY/` on page retirement or significant rewrite.

---

## Path B: Legal Documents (TOS, Privacy, DPA, SLA, Cookie Policy)

### B1. Draft
- Create new version file: `documents/legal/{type}-v{X.Y}-YYYY-MM-DD.md`
- Semver bump rules:
  - **MAJOR (v2.0):** material change that alters tenant obligations (requires 30-day notice + opt-out)
  - **MINOR (v1.1):** clarifying change, new optional clauses, additional jurisdictions
  - **PATCH (v1.0.1):** typo, grammar, link fix — no notice required
- Frontmatter REQUIRED:
  ```yaml
  ---
  title: Terms of Service
  version: 2.0
  effective_date: 2026-05-20
  supersedes: legal/tos-v1.3-2026-01-15.md
  material_change: true
  notice_period_days: 30
  jurisdictions: [VN, EU]
  ---
  ```
- Author writes in `documents/legal/drafts/` until reviewed.

### B2. Internal review (Engineering + Product)
- Product owner: does this match actual product behavior? (e.g., TOS says "no data export" but we have export button = mismatch)
- Engineering: technical feasibility — if privacy says "deletion within 30 days", is there a real job?
- 2 business days

### B3. Legal counsel review (MANDATORY)
- Send draft to retained legal counsel (external firm OR in-house counsel once hired)
- Counsel reviews for:
  - Jurisdiction-specific compliance (see `compliance-checklist.md`)
  - Enforceability of clauses under Vietnamese civil code
  - Risk exposure (arbitration, liability cap, indemnification)
  - Conflict with consumer protection laws
- SLA: 5-10 business days depending on scope
- Counsel returns redlined doc + opinion letter

### B4. Reconciliation
- Author incorporates counsel changes
- Second review round if material redline — abbreviated (2 business days)

### B5. Executive sign-off
- CEO / legal authorized signatory signs final version
- Signed PDF stored in `documents/legal/signed/{type}-v{X.Y}-signed-YYYY-MM-DD.pdf`
- Digital signature via DocuSign / VNPT-CA / Viettel-CA for contracts >VND 50M
- Retain counsel opinion letter + redlines in `documents/legal/opinions/{type}-v{X.Y}-opinion-YYYY-MM-DD.pdf`

### B6. Tenant notification (for material changes)

**30-day advance notice via ALL channels:**

| Channel | Who | Timing |
|---------|-----|--------|
| Email to admin | All active tenants | Day -30 + reminder Day -7 |
| In-app banner | All users (dismissible after acknowledging) | Day -30 → Day 0 |
| Blog post | Public | Day -30 |
| Footer link | All visitors | Day -30 |
| Direct outreach | Enterprise tenants | Day -30 phone/account-manager call |

Content required per notice:
- Summary of changes (plain language)
- Link to full new version + redline diff vs old
- Opt-out right: "If you disagree, cancel before YYYY-MM-DD for full refund/data export"
- Effective date + transition period
- Contact for questions

### B7. Publish
- Merge PR on effective date (scheduled merge or manual trigger)
- Update `documents/legal/index.md` — bump current pointer
- Deploy: `/terms`, `/privacy`, `/dpa`, `/sla` pages reflect new version
- Per-tenant table: keep old version pointer for tenants who agreed pre-effective-date

### B8. Per-tenant version tracking (CRITICAL)

**Tenants see the version they agreed to, NOT the current version.**

Requires subscription/account schema:
```sql
-- Example fields (file gap if missing)
ALTER TABLE tenant_agreement
  ADD COLUMN tos_version VARCHAR(16) NOT NULL,
  ADD COLUMN tos_agreed_at TIMESTAMP NOT NULL,
  ADD COLUMN privacy_version VARCHAR(16) NOT NULL,
  ADD COLUMN privacy_agreed_at TIMESTAMP NOT NULL;
```

On renewal / material update: force re-agreement — user re-ticks consent box, row updates.

### B9. Archive previous version
- Move `documents/legal/{type}-v{X.Y-1}-*.md` → `documents/07-archived/legal-YYYY/`
- Keep signed PDF + opinion letter in original location (never archive signed originals)
- Tenants viewing old version get served from archive path

---

## Path C: Consent UI surfaces (cookie banner, signup checkbox, data export)

### C1. Copy + UX draft
- Engineer or designer drafts consent text + flow
- Must be: specific (not "I agree to all"), granular (separate checkboxes for marketing vs necessary vs analytics), revocable (link to settings page)

### C2. Legal counsel review (MANDATORY)
Counsel checks:
- PDPL (Nghị định 13/2023) explicit-consent requirements
- GDPR Art. 7 freely-given + informed consent
- Cookie Policy disclosure matches actual cookies set
- No pre-checked boxes (PDPL violation)
- No "consent by continuing to browse" (insufficient)

### C3. Implementation + preview
- Implement in FE with i18n keys
- Render preview in staging — counsel does final visual check
- Screenshot stored in `documents/legal/consent-screens/{surface}-YYYY-MM-DD.png`

### C4. Publish
- A/B test allowed for UX ONLY (button placement, copy tone) — never for consent granularity
- Monitor opt-in rate; sudden drop may indicate UX issue not consent issue

---

## Log entry format (per legal PR)

Add to PR body:

```
## Legal Review
- Doc: <type-version>
- Counsel: <firm, partner name>
- Counsel email/letter: <link to signed PDF in documents/legal/opinions/>
- Executive signature: <CEO name, date>
- Jurisdictions: <list>
- Material change: <yes|no>
- Tenant notice: <sent YYYY-MM-DD via {channels}> | <N/A — non-material>
- Previous version: <path, archived YYYY-MM-DD>
- Per-tenant migration: <required if material; track in follow-up ticket>
```

---

## Emergency procedure — legal threat / breach

If a tenant or regulator raises urgent claim:
1. Freeze affected doc publication (revert PR if staging, hotfix if prod)
2. Notify legal counsel within 24 hours
3. Preserve evidence — screenshot current state, `git log --all` for version trail
4. Do NOT edit archived version — create new annotated version only
5. Follow counsel's litigation hold if instructed (no deletes, no edits)

---

## Responsibility matrix (RACI for this workflow)

| Activity | R | A | C | I |
|----------|---|---|---|---|
| Marketing copy draft | Marketing | Brand Lead | Legal (if claims) | Team |
| Marketing copy review | Brand Lead | CMO | Legal | Team |
| Legal doc draft | Legal | CEO | Product + Eng | Team |
| Counsel review | External/In-house Counsel | CEO | — | Team |
| Executive signature | CEO | CEO | Legal | All tenants |
| Tenant notification | CS + Marketing | COO | Legal | Tenants |
| Per-tenant migration | Engineering | Tech Lead | Legal | Tenants |
| Version archive | Engineering | Tech Lead | — | Team |

Until external counsel retained: mark every counsel-required step as "deferred pre-GA" + file blocker gap.
