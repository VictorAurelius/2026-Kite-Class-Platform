---
description: "Dùng trước khi merge PR, user nói 'review', 'self-review', 'ready to merge', 'kiểm tra code', 'check PR'. Bắt buộc cho MỌI PR. Stage 1 (Spec Compliance, BLOCKING) → Stage 2 (Code Quality, GRADED). Skip: documentation-only PRs."
---

# Two-Stage Code Review

## Khi nào dùng (Mandatory)

- All PRs before merging to main
- Feature implementations, bug fixes, refactoring PRs

## Khi nào skip

- Documentation-only PRs (no code review needed)
- Configuration-only changes (quick sanity check)

## 2-Stage Process

### Stage 1: Specification Compliance (15-20 min) 🔴 BLOCKING

Does this PR do what was asked?

- Requirements match PR description (no missing, no scope creep)
- Edge cases covered (null, invalid input, error handling, multi-tenant isolation)
- Files in correct locations (per implementation plan)
- API contracts match design (DTOs, HTTP status codes, endpoint paths)
- Tests prove requirements met (every acceptance criterion has a test)

**Outcome:** PASS → go to Stage 2. FAIL → BLOCK, return to developer.

### Stage 2: Code Quality (20-30 min) 🟠🟡 GRADED

Is this code production-ready?

- 🔴 **CRITICAL** (BLOCKING): Security vulnerabilities, data loss risks, breaking API changes, auth bypasses, banned anti-patterns (God Service, leaky abstraction)
- 🟠 **MAJOR** (strong recommendation): N+1 queries, test coverage <80%, missing error handling, class >300 lines, missing required patterns (State Machine for status, Adapter for external API, Outbox for events)
- 🟡 **MINOR** (optional): Vague naming, code duplication, missing JavaDoc, style inconsistencies, primitive obsession

### Stage 2.5: Design Pattern Review (NEW)

Reference: `.claude/rules/design-patterns.md`

**Pattern checks:**
- [ ] Service >15 methods? → Facade refactor needed
- [ ] Status logic in if/switch? → State Pattern required
- [ ] External API types in domain? → Adapter required
- [ ] Direct event publish? → Outbox required
- [ ] External HTTP call? → Circuit Breaker + fallback required
- [ ] Pattern choice documented (javadoc)?
- [ ] No banned anti-patterns (check rules doc §3)

Use skill: `.claude/skills/design-pattern-advisor.md` để guidance.

## KiteClass Gotchas

- **KHÔNG kết luận CI pass khi `in_progress`** — chạy `scripts/check-ci.sh --status` trước, đợi completed
- **Multi-tenant CRITICAL** — mọi service query thiếu tenant filter = CRITICAL issue, BLOCK ngay
- **Spring Boot version** — pom.xml phải match `APPROVED_SB_VERSION` trong `.claude/scripts/pre-commit-check.sh`
- **Stage 1 trước Stage 2** — đừng review code quality nếu spec chưa pass; lãng phí thời gian

## Skill Contents

- `quick-reference/review-checklists.md` — Stage 1 full checklist + Stage 2 severity examples
- `quick-reference/review-template.md` — Copy-paste review template (fill-in-the-blank)
- `quick-reference/review-stage-decision-tree.md` — Decision flowchart PASS/FAIL/BLOCK

## Trigger Phrases

"review", "self-review", "ready to merge", "kiểm tra code", "check PR", "PR ready", "approve"

## Quick Checklist

**Stage 1 (Must PASS):**
- [ ] All requirements implemented
- [ ] Edge cases covered (null, invalid, multi-tenant)
- [ ] Files in correct locations
- [ ] API contracts match design
- [ ] Tests prove requirements met

**Stage 2 (GRADED):**
- [ ] 🔴 No CRITICAL issues (security, data loss, breaking changes, auth bypass)
- [ ] 🟠 Minimal MAJOR issues (N+1, test coverage, error handling, class size)
- [ ] 🟡 MINOR issues noted (naming, duplication, JavaDoc, style)

**Decision:** APPROVE / APPROVE with recommendations / BLOCK
