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

Use skill: `.claude/skills/reference/design-pattern-advisor.md` để guidance.

### Stage 2.6: Document Generation Review (Wave 5+)

Trigger: PR touches `kiteclass-core/src/main/java/com/kiteclass/core/module/document/**` OR `kiteclass-core/src/main/resources/templates/**` OR `documents/01-business/kiteclass/document-generation/**`.

**Doc-gen checks:**
- [ ] Sample golden output committed under `kiteclass-core/src/test/resources/document-samples/` for any new template/format (per Wave 5 plan §2.6 + per `output-review-mandate.md` §3 templates row)
- [ ] 3-layer business docs updated in same PR — `rules.md` BR-DOC-* IDs, `use-cases.md` UC-DOC-* IDs, `api-contract.md` HTTP/data-map schema (Living Docs rule, CLAUDE.md)
- [ ] Branding-key reads use the documented keys (`branding.primaryColor`, `branding.logoUrl`, `branding.displayName`, ...) and fall back gracefully to defaults when absent (per BR-DOC-016)
- [ ] OGNL pin (`ognl:3.3.4`) untouched if PR modifies `kiteclass-core/pom.xml` and Thymeleaf is in scope — see memory `feedback_thymeleaf_ognl_pin.md` and `PdfGeneratorTest` is the canary
- [ ] Tests cover the diacritic + branding paths — extend `XlsxGeneratorTest` / `DocxGeneratorTest` / `PdfGeneratorTest` and refresh `DocumentBrandingIntegrationTest` if a new format ships

If a new template lands without a sample golden output OR without 3-layer doc updates → BLOCK per `output-review-mandate.md` §6.

## Gotchas

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

## Severity Rubric (5-tier)

Per `documents/04-quality/gaps/GAP-254-severity-rubric-two-stage-code-review.md`,
review findings carry a 5-tier severity label. The Praise tier is the novel
addition — explicit positive recognition reduces the deficit-bias that
accumulates in long PRs (per 2026 reviewer-fatigue research). External refs:
[awesome-skills/code-review-skill](https://github.com/awesome-skills/code-review-skill)
+ [sennaBruno/claude-skills/quality-gate](https://github.com/sennaBruno/claude-skills).

| Tier | Definition | Example in this codebase |
|------|-----------|-------------------------|
| 🛑 **Blocker** | MUST fix before merge — correctness / security / data loss / contract breach | "Outbox bypassed without §3.5.1 exception A/B/C/D marker" — `design-patterns.md` violation |
| 🔴 **Critical** | SHOULD fix this PR or file follow-up gap — quality regression with downstream cost | "@Cacheable on @Service without invalidation path" — silent stale-data risk |
| 🟡 **Warning** | SHOULD fix opportunistically — anti-pattern, missing test, perf concern | "Missing unit test for new public method" |
| ℹ️ **Info** | Informational, no action required — context for future readers | "Variable name could be clearer (consider `studentRoster` over `s`)" |
| 🌟 **Praise** | Explicit recognition of good work — novel pattern, useful test, well-documented decision | "Novel use of XState machine for complex wizard flow — easier to reason about than nested conditionals" |

**When to use each tier — rule of thumb:**

- **🛑 Blocker / 🔴 Critical** map to existing Stage 2 BLOCKING / strong-recommendation buckets. Use these tiers when the finding falls in those buckets and you want a sharper label.
- **🟡 Warning / ℹ️ Info** map to existing Stage 2 MINOR + nice-to-have. Same idea, sharper label.
- **🌟 Praise** is additive — at least one Praise tier per non-trivial PR. If you can articulate a praise tier and you don't, you're being deficit-biased. Praise specific, concrete things (a clever test, a clear comment, a tasteful pattern application). Avoid generic "great job".

The rubric is **additive**, not replacement — Stage 2 GRADED 🔴/🟠/🟡 buckets above remain primary; the 5-tier rubric provides finer-grained severity labels reviewers may attach to individual findings.

For audit skills emitting /100, mapping to severity tiers is **optional this version** — a 13-skill cascade is too big for one PR. Tracked as a follow-up under GAP-255 / GAP-256 family if user wants to lift across audits.

## Log

- **2026-04-28** Added `## Severity Rubric` section (5-tier: 🛑 Blocker / 🔴 Critical / 🟡 Warning / ℹ️ Info / 🌟 Praise). Inspired by external community standard (awesome-skills/code-review-skill, sennaBruno/claude-skills/quality-gate); Praise tier is the novel addition. Closes GAP-254. Wave Meta-Gov 1, Move 2, Agent B PR. ≤80 lines added per AC.
