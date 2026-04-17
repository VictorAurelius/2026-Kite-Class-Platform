# GAP-048: Output Review Standards Coverage (Governance)

**Status:** 🟢 DONE
**Priority:** 🔴 P0 (governance — affects all outputs)
**Domain:** Governance / Quality / Process
**Detected:** 2026-04-14
**Related:**
- `.claude/rules/output-review-mandate.md` (master rule)

## Problem

Rule mới: "Mọi output phải có review standard + process". **9 violations hiện tại** trong dự án — outputs đang được tạo mà không có review tiêu chuẩn.

## Violations (9 outputs missing review)

| # | Output | Evidence | Risk |
|---|--------|----------|------|
| 1 | **Gap reports** | Self-written, no peer review | Irony — gap queue has no gap review |
| 2 | **Rules docs** | I've been creating rules without review | Meta-governance hole |
| 3 | **Architecture docs** | No ADR process | Unilateral decisions |
| 4 | **Database migrations** | Only code review, no DBA checklist | Data loss risk |
| 5 | **Scripts** (bash, Python) | No review standards | Security risk |
| 6 | **API contracts** | No contract tests | Silent breaking changes |
| 7 | **Email templates** | No brand/legal review | Customer-facing risk |
| 8 | **Marketing/legal docs** | No formal review | Compliance risk |
| 9 | **Logs format** | No standard | Debug difficulty |

## Proposed Fix (9 sub-actions)

### 9.1 Gap Review Process
- Template: gap-review-template.md
- Peer review step trong `gap-to-pr-converter`
- Gap cannot reach `🟡 PLANNED` without review
- Criteria: Problem clear, AC measurable, dependencies identified

### 9.2 Rules/Skills ADR-style Review
- Rules doc requires:
  - Front-matter: version, last-reviewed date, reviewers
  - Changelog section
- Lead + 1 dev approve before merge
- Review quarterly

### 9.3 Architecture ADR Process
```
documents/02-architecture/adr/
├── ADR-001-choose-postgres.md
├── ADR-002-multi-tenant-shared-db.md
├── ADR-003-ai-branding-v2-redesign.md
└── _TEMPLATE.md
```

Template:
```markdown
# ADR-XXX: [Title]
**Status:** PROPOSED / ACCEPTED / DEPRECATED
**Date:** YYYY-MM-DD
**Reviewers:** ...
## Context
## Decision
## Consequences
## Alternatives Considered
```

Review in architecture meeting (weekly).

### 9.4 Database Migration Review Checklist

Create `.claude/rules/migration-review.md`:
```markdown
- [ ] Backward compatible với running service?
- [ ] Rollback script provided?
- [ ] Index creation CONCURRENTLY (no lock)?
- [ ] Data migration batched (no full table lock)?
- [ ] Tested trên staging với production-like data volume?
- [ ] Migration duration measured?
- [ ] Observability: metrics/alerts during migration?
```

Required sign-off: DBA hoặc senior backend.

### 9.5 Script Review

Create `.claude/rules/script-standards.md`:
```markdown
Bash:
- [ ] shellcheck passes
- [ ] set -euo pipefail
- [ ] --help flag
- [ ] --dry-run mode for destructive ops

Python:
- [ ] ruff passes
- [ ] Type hints
- [ ] Tests if >100 lines

All:
- [ ] No hardcoded secrets
- [ ] Input validation
- [ ] Idempotent OR has guard flag
- [ ] Documentation: purpose, usage, edge cases
```

### 9.6 API Contract Testing

- OpenAPI spec co-located với controller
- Contract tests (Spring Cloud Contract hoặc Pact)
- Breaking change detector in CI
- Version bump + deprecation notice protocol

### 9.7 Email Template Review

Create `.claude/rules/email-template-standards.md`:
```markdown
Every email template:
- [ ] Brand: logo, colors, fonts applied
- [ ] Legal: unsubscribe link, address footer
- [ ] i18n: Vietnamese + English variants
- [ ] Preview: tested với sample data
- [ ] Mobile: responsive on 320px-768px
- [ ] Accessibility: alt text, contrast
- [ ] Plain-text fallback

Approval:
- Marketing lead signs off on copy
- Legal lead signs off on customer-facing
```

### 9.8 Marketing/Legal Review

- Legal counsel review cho:
  - Terms of Service
  - Privacy Policy
  - Data Processing Agreement
  - Teacher contracts
- Brand review cho marketing copy
- Version control + dated signatures
- Quarterly re-review

### 9.9 Logs Standard

Create `.claude/rules/logging-standards.md`:
```markdown
Format: JSON structured
Required fields:
  - timestamp (ISO 8601)
  - service (kitehub-branding, kiteclass-core, etc.)
  - level (DEBUG, INFO, WARN, ERROR)
  - tenantId (if tenant-scoped)
  - traceId (distributed tracing)
  - message

Retention:
  - DEBUG: 7 days
  - INFO: 30 days
  - WARN/ERROR: 90 days

PII scrubbing:
  - No passwords, tokens, full credit cards
  - Mask emails: j***@example.com
  - Mask phone: 091****567
```

## Enforcement

### Automated
- Pre-commit hook: detect output types in diff → require review evidence
- CI check: verify review checklist in PR body

### Manual
- Quarterly audit: `quality-audit` skill adds "Review Standards Coverage" category
- Target: 100% coverage by Q2 2026

## Acceptance Criteria

- [ ] Master rule `output-review-mandate.md` published ✓ Done
- [ ] 9 sub-rule files created (one per violation type)
- [ ] ADR folder + template created
- [ ] Migration review checklist documented
- [ ] Script standards documented + linter enforced
- [ ] API contract testing integrated
- [ ] Email template review process (marketing + legal)
- [ ] Logs standard with PII scrubbing
- [ ] PR template updated với output review checklist
- [ ] Pre-commit hook detects output types
- [ ] CI check enforces review evidence
- [ ] Quarterly audit category added
- [ ] Existing outputs retroactively reviewed (grandfather period 1 month)

## Execution Plan

**Sprint 0 (quick wins, 1 week):**
- Create 9 sub-rules (documentation only)
- Update PR template
- Add quarterly audit category

**Sprint 1 (automation, 1 week):**
- Pre-commit hooks
- CI checks
- Linters (shellcheck, ruff)

**Sprint 2 (retroactive, 2 weeks):**
- Review existing outputs
- Create missing ADRs for past decisions
- Migration checklists for existing V1-V27

**Sprint 3 (legal/compliance, 2 weeks):**
- Legal counsel engagement
- Brand review pass for marketing
- Email template compliance

## Dependencies

- Tech lead engagement (ADR process)
- Legal counsel (GAP-042 already flags this)
- DBA role clarified (if not assigned)

## References

- Master rule: `.claude/rules/output-review-mandate.md`
- Related: GAP-042 (legal review), GAP-016 (living docs)

## Log

- 2026-04-14 — Governance gap identified qua user question
