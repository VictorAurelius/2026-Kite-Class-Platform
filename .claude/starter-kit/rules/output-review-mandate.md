# Output Review Mandate

**Priority:** 🔴 CRITICAL — project-wide governance rule
**Version:** 1.2.0
**Created:** 2026-04-14
**Last-Reviewed:** 2026-04-29
**Reviewer-Approver:** @nguyenvankiet (starter-kit upstream maintainer)
**Applies to:** Every artifact (code, docs, gaps, audits, AI assets, contracts, generated reports, scripts, templates, logs) the project produces, plus every review process listed in §3 matrix

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
| **Ops readiness** | ops-readiness-audit skill /100 | Post-wave + quarterly | Auditor | ✅ |
| **Performance baseline** | performance-audit skill /100 | Post-wave + quarterly | Auditor | ✅ |
| **Business docs implementation match** (code ↔ rules.md sync) | Living Docs rule (3-layer) | Same PR as code change | PR reviewer | ✅ DONE |
| **Business logic CORRECTNESS** (giá trị rule đúng thị trường + law) | BRD + stakeholder sign-off + compliance | Before launch + quarterly | Product + Business + Legal | ⚠️ PARTIAL — rule shipped (`business-logic-review.md`); audit + stakeholder sign-offs → tracking gap (<example: GAP-XXX>) |
| **PRs** | check-pr skill | Pre-merge | Reviewer | ✅ DONE |
| **Wave plans** | Wave review checklist | Before launch | Team lead + architect | ⚠️ PARTIAL (skill exists, no formal review) |
| **Gap reports** | Gap review template | After creation | Peer | ✅ DONE (closed by <example: GAP-XXX>) |
| **Gap closure (Status flip → DONE)** | `gap-done-discipline.md` (AC checked, no banned phrases, follow-up filed for any deferral) | Pre-merge of closing PR | Author + reviewer + skill detection | ✅ DONE — `.claude/rules/gap-done-discipline.md` + `session-docs-check` Rule 13 detector + 3-fixture self-test; closes silent-deferral incident |
| **Coverage gaps in rules/skills (incidents)** | `incident-to-rule-pipeline.md` (5-stage: Detect → Classify → Rule+Enforce → Self-Test → Retro Log) | When user/reviewer flags a miss | Author + reviewer | ✅ DONE — `.claude/rules/incident-to-rule-pipeline.md` paired with `rule-change-process.md` §6.5 Enforcement Parity Mandate |
| **Architecture docs** | ADR process | When written | Tech lead + team | ✅ DONE (closed by <example: GAP-XXX>) |
| **Skills (meta)** | skill-conventions.md rules + `scripts/check-skill-conventions.sh` | Pre-merge (CI) | Lead + CI | ✅ DONE (closed by <example: GAP-XXX>) |
| **Rules docs (meta)** | ADR-like | Pre-merge | Lead + team | ✅ DONE (closed by <example: GAP-XXX> — `rule-change-process.md` + `quality/rule-review/`) |
| **Templates (UI/image)** | 5-criteria template review | Before publish | Designer + lead | ⚠️ PLANNED (tracking gap) |
| **Email templates** | Brand + legal check | Before send | Marketing + legal | ✅ DONE (closed by <example: GAP-XXX>) |
| **AI-generated assets** | Quality gate /100 + content safety + audit skill | Auto + manual | Automated + admin | ⚠️ PARTIAL — governance scaffold DONE; real WCAG/visual-regression/ML classifier tracked in follow-up gaps |
| **Contracts (Word)** | Legal review | Before use | Lawyer | ❌ **VIOLATION** |
| **Generated PDFs/Excel** | QA checklist + visual regression | Before delivery | QA | ⚠️ PLANNED (tracking gap) |
| **Database migrations** | migration-review-checklist skill | Pre-merge | DBA + peer | ✅ DONE |
| **Scripts (bash/Python)** | script-review-checklist skill | Pre-merge | Peer | ✅ DONE |
| **API contracts** | api-contract-audit skill + schema validation | Pre-merge + runtime | Consumer/producer | ⚠️ PARTIAL (audit skill exists, no consumer-driven contract tests yet) |
| **Screenshots** | Manual + automated audit | Capture time | Auditor | ⚠️ PARTIAL (ui-review skill) |
| **Logs format** | Log standard doc | Audit period | SRE | ✅ DONE (closed by <example: GAP-XXX>) |
| **README freshness** | `scripts/check-readme-freshness.sh` (`**Last Updated:**` date check, 30d WARN / 90d FAIL, exempt via `<!-- readme-freshness-exempt: <reason> -->`) | Pre-merge (CI) | CI + reviewer | ✅ DONE |
| **Marketing copy** | Brand + legal | Before publish | Marketing + legal | ✅ DONE (closed by <example: GAP-XXX>) |
| **Legal docs sent to tenants** | Full legal review | Before issue | Lawyer | ✅ DONE (closed by <example: GAP-XXX> — shared `marketing-legal-review` skill covers TOS/Privacy/DPA) |

**Legend:**
- ✅ DONE — standard exists, process runs
- ⚠️ PARTIAL — standard partial or process informal
- ❌ VIOLATION — no standard/process, remediation needed
- ⚠️ PLANNED — remediation tracked in gap

---

## 4. Current Violations Summary

### 🔴 CRITICAL violations

Track project-specific violations in your project's `documents/04-quality/gaps/`. The matrix above shows which output types have current standards.

### ⚠️ PARTIAL (exists but informal)

- Wave plans
- Skills self-review
- Screenshots scoring
- Templates
- API contracts (audit skill exists, no consumer-driven contract tests)

### ⚠️ PLANNED (tracked in gaps)

- AI asset quality gate
- Generated document QA

---

## 5. Remediation Plan

Create a tracking gap (e.g. `GAP-output-review-coverage`) to close all violations.

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
  - [ ] i18n
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
Target: 100% by end of quarter
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
- `.claude/rules/design-patterns.md` (if adopted) → applies to code output
- Extends all trên với universal mandate

---

## 10. Related

- Skill: `two-stage-code-review.md`
- Skill: `ui-review/SKILL.md`
- Skill: `quality-audit/SKILL.md`
- Skill: `quality/business-gap-check.md`
- Rules: `skill-conventions.md`, `design-patterns.md`

---

## 11. Log

- **2026-04-29** (v1.2.0 upstream import): Imported into starter-kit v2.3.0 from project source. Local project remains source of truth; upstream version may diverge as starter-kit evolves separately. Specific GAP IDs in matrix replaced with `<example: GAP-XXX>` placeholders.
- **2026-04-29** (v1.1.0 → v1.2.0): MINOR additions across the year — matrix rows added for HTML/JSX prototypes, README freshness, gap closure, incident pipeline; PARTIAL flips for business correctness; status sync after Wave milestones.
- **2026-04-27** (v1.1.0): MINOR — added §3 matrix rows: "Gap closure (Status flip → DONE)" and "Coverage gaps in rules/skills (incidents)". No constraint loosening — only adds coverage rows for previously-uncovered output types.
- **2026-04-20:** 6 CRITICAL violations closed: Gap reports + Rules docs + Architecture ADRs + Email templates + Marketing + Legal + Logs format.
- 2026-04-16 — Resolved 2 violations: Scripts (script-review-checklist skill), DB migrations (migration-review-checklist skill).
- 2026-04-14 — Rule established; ~9 critical violations identified; remediation via tracking gap.
