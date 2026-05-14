---
parent_rule: output-review-mandate.md
purpose: deferred-load §Remediation Plan (historical artifact) for context budget compliance
---

# output-review-mandate — Examples / Remediation Plan

Companion to `.claude/rules/output-review-mandate.md`. Body moved here per Wave 76 Bucket E streamline.

## Remediation Plan (historical — Wave 8b closed)

Note: this remediation plan was the original 2026-04-14 scoping artifact that drove GAP-170..175 + GAP-048. All 6 CRITICAL violations closed Wave 8b (2026-04-20). Preserved here for historical reference; current state lives in `output-review-mandate.md` §3 matrix.

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
