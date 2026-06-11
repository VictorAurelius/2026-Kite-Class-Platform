# Pull Request

## Summary
<!-- Brief summary of what this PR does (1-2 sentences) -->

## Related Issues/PRs
<!-- Link to related issues or implementation plan section -->
- Implements PR [X.X] from [kiteclass-implementation-plan.md](../documents/03-planning/implementation/kiteclass-implementation-plan.md)
- Closes #[issue number] (if applicable)

## Type of Change
<!-- Check all that apply -->
- [ ] 🚀 New feature (non-breaking change which adds functionality)
- [ ] 🐛 Bug fix (non-breaking change which fixes an issue)
- [ ] ♻️ Refactoring (code improvement without functional changes)
- [ ] 📝 Documentation update
- [ ] ⚙️ Configuration change (docker, CI/CD, dependencies)
- [ ] 🧪 Test improvements
- [ ] 💥 Breaking change (fix or feature that would cause existing functionality to not work as expected)

---

# Two-Stage Code Review Checklist

> **Review Process**: Stage 1 (Spec Compliance) MUST PASS before proceeding to Stage 2 (Code Quality)
> **Reference**: `.claude/skills/two-stage-code-review.md`

## Stage 1: Specification Compliance (15-20 min) 🔴 BLOCKING

**Reviewer must verify ALL checkboxes before proceeding to Stage 2:**

### Requirements Match
- [ ] Matches PR description exactly
- [ ] Implements all acceptance criteria from plan
- [ ] No missing features (incomplete implementation)
- [ ] No extra features (scope creep)

### Edge Cases Coverage
- [ ] Handles null/empty inputs (validation present)
- [ ] Handles invalid data (error responses correct)
- [ ] Handles errors gracefully (try-catch, proper exceptions)
- [ ] Multi-tenant isolation verified (if applicable)

### KiteClass-Specific Security Checks (Stage 1)
- [ ] All repository queries include `instance_id` filter
- [ ] No hardcoded instance IDs (`UUID.fromString(...)`)
- [ ] Services use `TenantContext.getCurrentInstanceId()`
- [ ] Cross-tenant access tests added and passing
- [ ] API responses filtered by current tenant
- [ ] Input validation (`@Valid`, `@NotNull`, `@Size`)
- [ ] Authentication required for non-public endpoints
- [ ] Authorization enforced (role-based access)

### File Paths Match Plan
- [ ] Code files in correct locations (per implementation-plan.md)
- [ ] Test files in corresponding test directories
- [ ] No unexpected file changes (only planned files modified)

### API Contracts Match Design
- [ ] Request/Response DTOs match api-design.md
- [ ] HTTP status codes correct (200, 201, 400, 404, 500, etc.)
- [ ] Endpoint paths follow conventions (`/api/v1/...`)

### Tests Prove Requirements Met
- [ ] Every acceptance criterion has corresponding test
- [ ] Tests actually verify the requirement (not just code coverage)
- [ ] All tests pass (green) ✅
- [ ] Test coverage ≥ 80% on new code

### Stage 1 Outcome
- [ ] ✅ **PASS** - All requirements met, proceed to Stage 2
- [ ] ❌ **FAIL** - Requirements issues found, return to developer

**If FAIL, list issues here:**
<!--
- Missing requirement: [description]
- Edge case not handled: [description]
-->

---

## Stage 2: Code Quality (20-30 min) 🟠🟡 GRADED

**Only review if Stage 1 PASSED**

### 🔴 Critical Issues (BLOCKING - Must Fix)
- [ ] No security vulnerabilities (SQL injection, XSS, auth bypass)
- [ ] No data loss risks (missing transactions, incorrect deletes)
- [ ] No breaking changes (API contract changes without versioning)
- [ ] No secrets in code/logs
- [ ] Financial data properly secured (if applicable)

**Critical issues found:**
<!--
- [File:Line] Security: [description]
-->

---

### 🟠 Major Issues (Recommended - Should Fix)
- [ ] No N+1 queries or performance problems
- [ ] Error handling present (proper exception types with error codes)
- [ ] Class/method size reasonable (<300 lines class, <50 lines method)
- [ ] No code duplication (DRY principle followed)
- [ ] Proper logging (no sensitive data, appropriate levels)

**Major issues found:**
<!--
- [File:Line] Performance: [description]
- [File:Line] Error Handling: [description]
-->

---

### 🟡 Minor Issues (Optional - Nice to Have)
- [ ] Naming is clear and descriptive
- [ ] JavaDoc present on public methods
- [ ] Code style consistent (Checkstyle passing)
- [ ] No commented-out code
- [ ] No TODO comments left for implemented features

**Minor issues found:**
<!--
- [File:Line] Naming: [suggestion]
-->

---

### Stage 2 Outcome
- [ ] ✅ **APPROVE** - No critical/major issues
- [ ] 🟠 **APPROVE with recommendations** - Major issues noted, not blocking
- [ ] 🔴 **BLOCK** - Critical issues must be fixed

---

## Stage 2.5: Design Patterns Review (per `.claude/rules/design-patterns.md`)

### 🔴 Anti-patterns BANNED
- [ ] No God Service (>500 lines / >15 methods)
- [ ] No Primitive Obsession (value objects for structured data)
- [ ] No scattered status switch/if — use State Pattern
- [ ] No direct external API types in domain — use Adapter
- [ ] No direct event publish — use Outbox pattern
- [ ] No external HTTP without Circuit Breaker + fallback

### 🟠 Required patterns applied (if applicable)
- [ ] Multiple implementations → Strategy Pattern
- [ ] Finite state entity → State Machine
- [ ] Pipeline of steps → Command + Composite
- [ ] Service orchestrating many deps → Facade
- [ ] Heavy async task → RabbitMQ queue + worker
- [ ] Pattern choice documented in javadoc

**Use skill:** `.claude/skills/reference/design-pattern-advisor.md`

---

## Living Docs Compliance (per `.claude/rules/output-review-mandate.md` + GAP-016)

If PR changes business logic, verify docs updated:

- [ ] `01-business/{product}/{domain}/rules.md` — business rules
- [ ] `01-business/{product}/{domain}/use-cases.md` — UCs
- [ ] `01-business/{product}/{domain}/api-contract.md` — endpoints
- [ ] `03-planning/database/database-design.md` — new entities
- [ ] `03-planning/database/database-migration-plan.md` — migrations
- [ ] `06-diagrams/plantuml/03-erd.puml` — relationships
- [ ] `06-diagrams/plantuml/04-architecture-full.puml` — components
- [ ] `.claude/skills/api-design.md` — API spec
- [ ] New user-facing feature → guide in `05-guides/`

**Verification chain:** BR-xxx → UC-xxx → endpoint → @Mapping → @Test

---

## Gap Tracking (if PR closes gap from queue)

- [ ] Gap ID referenced: GAP-XXX
- [ ] Gap file status updated to 🟢 DONE
- [ ] PR link added to gap file
- [ ] ROADMAP.md sprint progress updated (if wave complete)

---

## Output Review (per `.claude/rules/output-review-mandate.md`)

Applies if PR produces new output types:

- [ ] Database migration — DBA checklist (if applicable)
- [ ] Script added — linted (shellcheck / ruff)
- [ ] Email template — brand + legal check
- [ ] API contract — OpenAPI updated + contract test
- [ ] ADR doc — tech lead reviewed
- [ ] Logs format — structured JSON, no PII
- [ ] **Design layer coverage (4-layer V-model)** — if PR touches UI/design/kit/feature scope, all 4 Japanese layers (要件定義 / 基本設計 / 詳細設計 / コンポーネント設計) verified per `.claude/rules/design-layer-coverage.md` §2 matrix. Reference: `documents/02-architecture/design-system/dossier/16-design-layer-mapping.md`. Missing layer = file follow-up gap inline OR commit trailer `DESIGN_LAYER_OVERRIDE: <layer> N/A — <reason>`
- [ ] **FE kit-parity** — if PR touches `kiteclass-frontend/src/app/**` or `kitehub-frontend/src/app/**` (production FE port from kit), production matches kit spec per `.claude/skills/frontend/frontend-standards.md` §3.1 "Kit as Source of Truth" (visual diff vs kit screenshot + AC traceability) OR PR is kit-first (file kit gap before production). Parity audit: `.claude/skills/quality/kit-production-parity/SKILL.md` (4-layer V-model). Bidirectional — kit→prod port OR prod→kit back-port.
- [ ] **Decision-doc code-sync** — if PR introduces or changes a config-shaped value (domain / brand / support email / env var / cloud region / vendor / account ID) in a gap file flipped DONE / ADR / runbook / brand guide, grep evidence shows zero stale refs across `kitehub/`, `kiteclass/`, `infrastructure/`, `scripts/`, `documents/05-guides/`, helm values, terraform vars, CI workflows OR a follow-up sync gap is filed and linked per `.claude/rules/audit-to-gap-pipeline.md` §2.7. Override trailer: `DECISION_DOC_SYNC_DEFERRED: <reason + follow-up gap link>`
- [ ] **Pre-mutation state-check** — if PR triggers production mutation (terraform apply via workflow_dispatch / AWS CLI `create-*`/`put-*`/`update-*` / Cloudflare API PATCH/DELETE on production zones / k8s prod apply / GitHub Variables on production env), audit artifact under `documents/04-quality/audits/<category>/YYYY-MM-DD-<topic>.md` exists with Scope + Commands run + Findings (real-vs-phantom) + Prior actions verified + Recommendation per `.claude/rules/pre-mutation-state-check.md` §3. Override trailer: `PRE_MUTATION_OVERRIDE: <reason + PRE_MUTATION_FOLLOWUP: gap link>`
- [ ] **Post-merge sync (4 targets)** — per `.claude/rules/post-merge-sync-completeness.md` §2, if PR changes gap Status / closes wave-scoped work / adds new memory entry: (1) `documents/04-quality/gaps/gap-status.csv` row updated in same diff; (2) `documents/04-quality/gaps/ROADMAP.md` §🚀 reflects current state; (3) `.claude/skills/quality/wave-pack-planner/data/wave-history.jsonl` appended if wave-scoped; (4) `MEMORY.md` index updated if new memory entry (PR description embeds memory text under `## Memory entry (copy to user-memory)` heading per rule §7.5). Override trailer: `POST_MERGE_SYNC_OVERRIDE: <target>(s) — <reason> + POST_MERGE_SYNC_FOLLOWUP: <follow-up gap link>`
- [ ] **Meta CSV index sync** — if PR adds / renames / deletes a rule (`.claude/rules/*.md`), ADR (`documents/02-architecture/adr/ADR-*.md`), or other tracked enumeration, the matching `*-index.csv` row was added / updated / removed in the same commit per `.claude/rules/meta-csv-index-pattern.md`. CI `check-<scope>-index-csv.sh` validates. Override trailer: `META_CSV_INDEX_DEFER: <scope> <follow-up PR link>`
- [ ] **Closes GAP-NNN(s)** — list each gap closed by this PR using `Closes: GAP-NNN` (or `Resolves: GAP-NNN` / `Refs: GAP-NNN`) syntax in PR body. On merge, `audit-gate.py` `auto_close_referenced_gaps()` parses markers and: `Closes`/`Resolves` → flip CSV row to `DONE 100%` + `git mv` file to `phase-X/closed/` + append closure Log entry; `Refs` → bump `last_verified` only, status unchanged. Per `.claude/rules/gap-folder-organization.md` v2.0.0 §3.3 + `.claude/rules/gap-done-discipline.md` §2 + GAP-751 Option A. Soft-fails on missing CSV row or file — manual sync still needed in those cases.

**Summary:**
<!-- Brief summary of code quality review -->

---

# Superpowers Skills Applied

**Check all skills actively used in this PR:**
<!-- Reference: `.claude/skills/` directory -->

### Week 1 Skills (Superpowers-Inspired)
- [ ] **Systematic Debugging** - Used 4-phase process (Reproduce → Trace → Root Cause → Defensive Fix)
  - Regression test added: `[TestClass#testMethod]`
  - Updated troubleshooting.md: [Yes/No]
  - Root cause documented in: [commit message / MEMORY.md / troubleshooting.md]

- [ ] **Socratic Brainstorming** - Documented design decision
  - Decision doc: `[file path or section in implementation-plan.md]`
  - Alternatives considered: [Number]
  - Trade-offs documented: [Yes/No]

- [ ] **Test-Driven Development** - Followed RED-GREEN-REFACTOR
  - Test written first: [Yes/No]
  - Test file modified before code: [Yes/No]
  - All phases completed: RED → GREEN → REFACTOR

- [ ] **Two-Stage Review** - Self-reviewed before requesting review
  - Stage 1 self-check: [Pass/Fail]
  - Stage 2 self-check: [No Critical / X Major / Y Minor issues found]
  - Self-review time: [X min]

- [ ] **Task Breakdown** - Broke work into 2-5 min tasks
  - Task count: [Number]
  - Average time per task: [X min]
  - Tasks documented in: [commit messages / plan comments]

### Existing Skills
- [ ] **Multi-tenant Testing** - Applied tenant isolation patterns
  - Tenant filter tests added: [Yes/No]
  - Cross-tenant access tests: [Yes/No]

- [ ] **Spring Boot Testing Quality** - Used proper test slices
  - Test type: [@SpringBootTest / @WebMvcTest / @DataJpaTest]
  - TestContainers used: [Yes/No]

- [ ] **Code Style** - Followed naming conventions and structure
  - Checkstyle passed: [Yes/No]
  - Import ordering correct: [Yes/No]

- [ ] **Error Logging** - Proper exception handling with error codes
  - Error codes used: `[ERROR_CODE_1, ERROR_CODE_2]`
  - Messages i18n-ready: [Yes/No]

- [ ] **API Design** - RESTful conventions, proper status codes
  - Endpoint follows `/api/v1/{resource}` pattern: [Yes/No]
  - Status codes correct: [List: 200, 201, 404, etc.]

---

## Changes Made

### Files Changed

**Backend:**
- `path/to/file1.java` - (mô tả thay đổi)
- `path/to/file2.java` - (mô tả thay đổi)

**Frontend:**
- `path/to/component1.tsx` - (mô tả thay đổi)
- `path/to/component2.tsx` - (mô tả thay đổi)

**Tests:**
- `path/to/test1.java` - (X tests added)
- `path/to/test2.spec.ts` - (Y tests added)

### Test Coverage

**Before:**
- Backend: XX%
- Frontend: YY%

**After:**
- Backend: XX% (+Z%)
- Frontend: YY% (+Z%)

**New Tests Added:**
- Unit Tests: X tests
- Integration Tests: Y tests
- E2E Tests: Z tests

---

## Testing Instructions

### How to Test This PR

1. **Setup:**
   ```bash
   # Commands to setup test environment
   git checkout <branch-name>
   ./mvnw clean install
   ```

2. **Run Tests:**
   ```bash
   # Backend tests
   ./mvnw test

   # Frontend tests
   npm run test

   # E2E tests
   npm run test:e2e
   ```

3. **Manual Testing:**
   - Step 1: ...
   - Step 2: ...
   - Expected result: ...

### Test Evidence

**Screenshots/Videos:**
(Attach screenshots or videos demonstrating the changes)

**Test Results:**
```
# Paste test output here
✅ All tests passed
Coverage: 85.2%
```

---

## Security Checklist (for Security PRs)

- [ ] Security vulnerability identified and documented
- [ ] Fix implemented and tested
- [ ] Security tests added (≥5 tests per vulnerability)
- [ ] No new security vulnerabilities introduced
- [ ] Penetration testing completed (if applicable)
- [ ] Security team review requested

---

## Performance Impact

- [ ] No performance regression
- [ ] Performance benchmarks run (if applicable)
- [ ] Response time within targets (P95 < 500ms)
- [ ] Database queries optimized (< 100ms)
- [ ] Frontend bundle size acceptable (< 500KB)

**Performance Test Results:**
```
# Paste k6 or Lighthouse results here
```

---

## Deployment Notes

### Database Changes

- [ ] Database migration required
- [ ] Migration script tested
- [ ] Migration is idempotent
- [ ] Rollback script provided

**Migration Files:**
- `V{version}__{description}.sql`

### Environment Variables

- [ ] New environment variables required
- [ ] `.env.example` updated
- [ ] Documentation updated

**New Variables:**
```
NEW_VAR_NAME=default_value
```

### Breaking Changes

- [ ] No breaking changes
- [ ] Breaking changes documented
- [ ] Migration guide provided

**Breaking Changes:**
(List any breaking changes and migration steps)

---

## Merge Criteria

### Must Pass Before Merge

- [ ] All CI/CD checks passing
- [ ] Code review approved (≥1 reviewer)
- [ ] Security review approved (for security PRs)
- [ ] Test coverage ≥80%
- [ ] No HIGH/CRITICAL security vulnerabilities
- [ ] No merge conflicts
- [ ] All conversations resolved

### Dependent PRs

- [ ] All dependent PRs merged (if applicable)

---

## Post-Merge Actions

- [ ] Delete feature branch
- [ ] Update project board
- [ ] Notify stakeholders
- [ ] Monitor production (if deployed)

---

## Additional Context

(Any additional context, screenshots, or information that would help reviewers)

---

## Reviewer Notes

**For Reviewers:**
- **MUST** follow Two-Stage Review process (see `.claude/skills/two-stage-code-review.md`)
- **Stage 1 BLOCKS Stage 2**: If spec compliance fails, return to developer immediately
- Check security implications (especially multi-tenant isolation)
- Verify test coverage ≥ 80% on new code
- Run tests locally before approving

**Review Time Estimates:**
- Stage 1 (Spec Compliance): 15-20 min
- Stage 2 (Code Quality): 20-30 min
- **Total**: ~40-50 min per PR

---

**Resources:**
- Implementation Plan: `documents/03-planning/implementation/kiteclass-implementation-plan.md`
- Skills Directory: `.claude/skills/`
- Architecture Docs: `documents/07-archived/research/architecture/`

---

**Co-Authored-By:** Claude Sonnet 4.5 <noreply@anthropic.com>
