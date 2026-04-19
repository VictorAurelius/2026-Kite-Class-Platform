# Output Review Mandate

**Version:** 1.0
**Created:** 2026-04-14
**Priority:** 🔴 CRITICAL — project-wide governance rule

---

## 1. The Mandate

> **Mọi output (artifact) sinh ra trong dự án PHẢI có:**
> 1. **Review standard** documented (criteria to evaluate)
> 2. **Review process** executed (who, when, how)
> 3. **Review evidence** preserved (logs, reports, sign-offs)

Không có review = không được merge/deploy/publish.

---

## 2. What Counts as "Output"?

Mọi artifact tenant, user, dev, hay downstream system consume:

**Code:**
- Source code (Java, TypeScript)
- Scripts (bash, Python, SQL)
- Configuration (YAML, properties)

**Documentation:**
- Business docs (rules, use-cases, api-contract)
- Architecture docs
- Plans (wave, implementation, roadmap)
- Gap reports
- Quality audit reports
- Skills + rules (meta — self-governance)
- User-facing guides

**Generated Artifacts:**
- Database migrations
- Generated documents (invoices, certificates, transcripts, reports)
- AI-generated assets (banners, hero, images)
- Templates (UI, image, contract, email)
- Email content sent to users
- API responses (contracts)
- Screenshots
- Logs (format + retention)

**External-facing:**
- Website content
- Marketing copy
- Legal documents
- Customer communications

---

## 3. Review Standards Matrix

| Output Type | Review Standard | Process | Reviewer | Current Status |
|-------------|----------------|---------|----------|:--------------:|
| **Code** | two-stage-code-review (Stage 1+2+2.5) | Pre-merge | Peer + CI + pattern check | ✅ DONE |
| **UI screens** | ui-review /128 per-screen | After FE PR | Auditor | ✅ DONE |
| **Quality audit reports** | quality-audit 10 categories /100 | Periodic | Auditor | ✅ DONE |
| **Ops readiness** | ops-readiness-audit skill /100 | Post-wave + quarterly | Auditor | ⚠️ BASELINE (2026-04-19, 49/100 — PR #365) |
| **Performance baseline** | performance-audit skill /100 | Post-wave + quarterly | Auditor | ⚠️ BASELINE (2026-04-19, 58/100 — PR #364) |
| **Business docs implementation match** (code ↔ rules.md sync) | Living Docs rule (3-layer) | Same PR as code change | PR reviewer | ✅ DONE |
| **Business logic CORRECTNESS** (giá trị rule đúng thị trường + law) | BRD + stakeholder sign-off + compliance | Before launch + quarterly | Product + Business + Legal | ❌ **VIOLATION** (GAP-049) |
| **PRs** | check-pr skill | Pre-merge | Reviewer | ✅ DONE |
| **Wave plans** | Wave review checklist | Before launch | Team lead + architect | ⚠️ PARTIAL (skill exists, no formal review) |
| **Gap reports** | Gap review template | After creation | Peer | ❌ **VIOLATION** |
| **Architecture docs** | ADR process | When written | Tech lead + team | ❌ **VIOLATION** |
| **Skills (meta)** | skill-conventions.md rules | Pre-merge | Lead | ⚠️ PARTIAL (rules exist, enforcement ad-hoc) |
| **Rules docs (meta)** | ADR-like | Pre-merge | Lead + team | ❌ **VIOLATION** |
| **Templates (UI/image)** | GAP-011 5 criteria | Before publish | Designer + lead | ⚠️ PLANNED (GAP-011) |
| **Email templates** | Brand + legal check | Before send | Marketing + legal | ❌ **VIOLATION** |
| **AI-generated assets** | Quality gate /100 + content safety | Auto + manual | Automated + admin | ⚠️ PLANNED (GAP-012, 018) |
| **Contracts (Word)** | Legal review | Before use | Lawyer | ❌ **VIOLATION** |
| **Generated PDFs/Excel** | QA checklist + visual regression | Before delivery | QA | ⚠️ PLANNED (GAP-047) |
| **Database migrations** | migration-review-checklist skill | Pre-merge | DBA + peer | ✅ DONE |
| **Scripts (bash/Python)** | script-review-checklist skill | Pre-merge | Peer | ✅ DONE |
| **API contracts** | api-contract-audit skill + schema validation | Pre-merge + runtime | Consumer/producer | ⚠️ PARTIAL (audit skill exists, no consumer-driven contract tests yet) |
| **Screenshots** | Manual + automated audit | Capture time | Auditor | ⚠️ PARTIAL (ui-review skill) |
| **Logs format** | Log standard doc | Audit period | SRE | ❌ **VIOLATION** |
| **Marketing copy** | Brand + legal | Before publish | Marketing + legal | ❌ **VIOLATION** |
| **Legal docs sent to tenants** | Full legal review | Before issue | Lawyer | ❌ **VIOLATION** |

**Legend:**
- ✅ DONE — standard exists, process runs
- ⚠️ PARTIAL — standard partial or process informal
- ❌ VIOLATION — no standard/process, remediation needed
- ⚠️ PLANNED — remediation tracked in gap

---

## 4. Current Violations Summary

### 🔴 CRITICAL violations (need immediate rule)

| # | Output | Why critical |
|---|--------|-------------|
| 1 | **Gap reports** | Ironic — gap queue has no gap review process! |
| 2 | **Rules docs** | Meta governance without meta review |
| 3 | **Architecture docs** | No ADR process → decisions made unilaterally |
| 4 | **Email templates** | Customer-facing, no brand/legal check |
| 5 | **Marketing copy / legal docs** | Compliance risk |
| 6 | **Logs format** | Debug difficulty nếu format drift |

### ⚠️ PARTIAL (exists but informal)

- Wave plans
- Skills self-review
- Screenshots scoring
- Templates (planned in GAP-011)
- API contracts (audit skill exists, no consumer-driven contract tests)

### ⚠️ PLANNED (tracked in gaps)

- AI asset quality gate (GAP-012, GAP-018)
- Generated document QA (GAP-047)

---

## 5. Remediation Plan

Create **GAP-048: Output Review Standards Coverage** to track closing all violations.

Each violation → dedicated action:

### 5.1 Gap Reports (meta-level)
- Add peer review step trong `gap-to-pr-converter.md`
- Template cho gap review: validates Problem clear, AC measurable, dependencies identified
- Gap không được status 🟡 PLANNED cho đến khi peer-reviewed

### 5.2 Rules + Skills (meta-governance)
- ADR template cho rules changes
- Lead + 1 dev review trước merge
- Changelog per rule file
- Version + last-reviewed date trong front-matter

### 5.3 Architecture Docs (ADR)
- `documents/02-architecture/adr/` folder
- ADR template (context, decision, consequences)
- Link ADR từ docs referencing decisions
- Reviewed in architecture meeting

### 5.4 Database Migrations
- Migration review checklist:
  - [ ] Backward compatible?
  - [ ] Rollback script provided?
  - [ ] Index impact assessed?
  - [ ] Data migration safe (no lock holds)?
  - [ ] Tested on staging with production-like data?
- DBA approval required for V-migrations

### 5.5 Scripts
- Script linting (shellcheck for bash, ruff for Python)
- Security review (no `eval`, no hardcoded secrets)
- Test coverage or at least `--dry-run` mode
- Documentation: purpose, usage, edge cases

### 5.6 API Contracts
- OpenAPI spec updates in same PR as controller changes
- Contract tests (Pact or similar)
- Backward compat check automated
- Breaking change requires version bump + deprecation notice

### 5.7 Email Templates
- Review checklist:
  - [ ] Brand colors + logo applied
  - [ ] Legal footer included (unsubscribe, address)
  - [ ] i18n (Vietnamese + English if needed)
  - [ ] Variables work (preview with sample data)
  - [ ] Mobile-responsive
- Marketing + legal sign-off for customer-facing

### 5.8 Marketing & Legal Docs
- Legal counsel review
- Compliance checklist
- Version control + dated signatures
- Archive previous versions

### 5.9 Logs Standard
- Structured logging (JSON)
- Required fields: timestamp, service, level, tenantId, traceId
- Retention policy documented
- PII scrubbing rules

---

## 6. Enforcement

### 6.1 Pre-commit hooks
```bash
# .husky/pre-commit
- Check PR touches any output type → verify review doc exists
- Example: migration added → require DBA checklist in PR
```

### 6.2 PR template additions

```markdown
## Output Review Checklist

Check all output types modified trong PR:
- [ ] Code — two-stage-code-review completed
- [ ] Business docs — updated if logic changed
- [ ] API contract — OpenAPI spec updated
- [ ] DB migration — DBA checklist
- [ ] Scripts — linted + tested
- [ ] Email templates — brand + legal check
- [ ] Architecture — ADR created if significant decision
- [ ] Gap files — peer reviewed
- [ ] Skills/Rules — lead approved
- [ ] Templates — designer reviewed (if UI/image)
```

### 6.3 Automated detection

```bash
# CI check
- git diff detect output types
- fail if review evidence missing
```

### 6.4 Quarterly audit

```
/quality-audit — add category "Review Standards Coverage"
Measure % of outputs with documented standard + process
Target: 100% ✅ by end of Q2 2026
```

---

## 7. Responsibility Matrix (RACI)

| Output Type | Responsible | Accountable | Consulted | Informed |
|-------------|------------|-------------|-----------|----------|
| Code | Dev | Tech lead | Peer | Team |
| Business docs | Dev | PM | Stakeholders | Team |
| Architecture | Architect | Tech lead | Team | Stakeholders |
| Migrations | DBA | Tech lead | Dev | SRE |
| Scripts | Dev | Security lead | Peer | Ops |
| API contracts | Dev | API owner | Consumers | Clients |
| Email templates | Marketing | Brand lead | Legal | Customers |
| AI assets | System | AI lead | Admin | Tenant |
| Templates | Designer | Creative lead | PM | Tenants |
| Generated docs | System | QA lead | Legal (contracts) | Tenant |
| Legal docs | Legal | CEO | Tech lead | All tenants |
| Logs | SRE | SRE lead | Security | Ops |

---

## 8. Exceptions

Cases khi review có thể lighter:

| Case | Lighter process |
|------|-----------------|
| Typo fix (single char) | Commit with message, CI passes |
| Comment-only change | Same as typo |
| Dev-only config | Reduced review (sanity check) |
| Emergency hotfix | Fast-track review + post-merge audit |

**Never skipped:** security, migrations, legal, customer-facing.

---

## 9. Integration với Existing

- `CLAUDE.md` Living Docs rule → subset of this mandate
- `.claude/rules/skill-conventions.md` → applies to skills output
- `.claude/rules/design-patterns.md` → applies to code output
- `.claude/rules/ai-branding-guidelines.md` → applies to AI asset output
- Extends all trên với universal mandate

---

## 10. Related

- Gap: GAP-048 (new — tracks remediation)
- Skill: `two-stage-code-review.md`
- Skill: `ui-review/SKILL.md`
- Skill: `quality-audit/SKILL.md`
- Skill: `quality/business-gap-check.md`
- Rules: `skill-conventions.md`, `design-patterns.md`, `ai-branding-guidelines.md`

---

## 11. Log

- 2026-04-19 — Audit catch-up Part A (3/5) shipped: resolved 2 first-ever VIOLATIONS via baseline capture — ops-readiness 49/100 (PR #365, 15 gaps, GAP-111 → GAP-125) and performance 58/100 (PR #364, 10 gaps, GAP-126 → GAP-135). Business-logic refresh after 27-day drift caught 7 gaps (PR #366, 65/100, GAP-104 → GAP-110). Status in §3 matrix: ops-readiness + performance now BASELINE — subsequent audits measure delta against this. Remaining Part A: ui-review /128 refresh (8d stale) + quality-audit /100 refresh.
- 2026-04-16 — Resolved 2 violations: Scripts (script-review-checklist skill), DB migrations (migration-review-checklist skill). API contracts moved to PARTIAL (audit skill exists). Remaining: 6 critical violations.
- 2026-04-14 — Rule established; 9 critical violations identified; remediation via GAP-048
