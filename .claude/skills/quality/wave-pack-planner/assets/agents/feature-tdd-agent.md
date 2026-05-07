# Feature TDD Agent Template

**Use when:** Code change requires full Superpowers methodology — brainstorm + task breakdown + test-first + implement + self-review per `CLAUDE.md` §Superpowers.

**Spawn config:** `isolation=worktree`, `subagent_type=general-purpose`
**Branch naming:** `feat/wave-{theme}-gap-{id-slug}`

## Prompt template

```
You are Agent {LETTER} of wave-pack {THEME}. Your scope: {GAP_ID} — {GAP_TITLE}.

## Wave context
Wave plan: documents/03-planning/waves/wave-{DATE}-{THEME}.md
Worktree root: {WORKTREE_ROOT} (you are isolated; do NOT cd to main repo)
Branch: feat/wave-{THEME}-gap-{GAP_ID_SLUG}

## Reserved resources (from wave plan — do NOT exceed)
- Migration version slot(s): {MIGRATION_VERSIONS — e.g. "V47, V48"}
- GAP ID range: {GAP_RANGE — for follow-up gaps}
- Config key prefix: {CONFIG_PREFIX — e.g. "kitehub.alertmanager.*"}
- Allowed paths: {ALLOWED_PATHS}

## Mandatory process — Superpowers methodology (per CLAUDE.md)

### Phase 1 — Brainstorm (5-10 min)
- Read documents/04-quality/gaps/{GAP_ID}.md — Acceptance Criteria + Proposed Fix
- Read related docs: {RELATED_DOCS}
- Identify scope, edge cases, dependencies, blockers
- Output: 1-paragraph plan in PR body draft

### Phase 2 — Task breakdown (5-10 min)
- Decompose into 3-7 concrete tasks
- Use TaskCreate tool to track in agent session
- Estimate effort per task

### Phase 3 — TDD (RED → GREEN → REFACTOR per task)
- Write failing test FIRST (RED)
- Minimal impl to pass (GREEN)
- Refactor with tests still green
- One commit per RED-GREEN cycle preferred

### Phase 4 — Implementation
- Follow `.claude/rules/design-patterns.md`:
  - Outbox §3.5.1 — cross-service events MUST flow through OutboxEventWriter or per-module emitter; direct rabbitTemplate.convertAndSend banned without Exception A/B/C/D marker
  - State Machine §3.3 — no status switch/if cascades
  - Adapter §3.4 — no external API types in domain
  - Strategy for ≥2 implementations
- Follow `.claude/rules/ai-branding-guidelines.md` if touching `kitehub-branding`
- Follow `.claude/rules/logs-format-standard.md` for any new log statement (no PII, structured)

### Phase 5 — Self-review (per `.claude/skills/core/two-stage-code-review.md`)
- Stage 1: re-read your diff
- Stage 2: pretend you're a hostile reviewer

## Migration awareness (if you add Flyway migrations)
- ONLY use slots {MIGRATION_VERSIONS} reserved in wave plan
- Naming: `V{N}__{snake_case_description}.sql`
- jsonb columns: pair `@Column(columnDefinition = "jsonb")` with `@JdbcTypeCode(SqlTypes.JSON)` per `feedback_jpa_jsonb_jdbctypecode.md`
- Test on dev profile (per `feedback_dev_profile_schema_workaround.md` — set spring profile explicitly via `-Dspring-boot.run.profiles=dev`)

## Verification before commit
- `mvn test -pl {MODULE}` (Java) — green
- `pnpm test` (frontend) — green
- `mvn verify` if integration tests exist
- Helm chart change? `helm template infrastructure/helm/kitehub | head -200` to inspect rendered manifests
- IDE warnings check (per `feedback_ide_warnings_check.md`): unused imports, deprecated APIs

## Deliverable format
After commits, report back:
1. Branch name + commit SHAs (one per RED-GREEN cycle ideal)
2. Files added/modified (path list)
3. Test pyramid coverage: unit + integration (+ E2E if AC requires)
4. Design pattern compliance: 1-line per pattern applied (Outbox? State machine? Adapter?)
5. Migration slots consumed (if any)
6. PR URL (`gh pr create --base main --title "feat({SCOPE}): {GAP_ID} — ..."`)
7. CI green confirmation
8. Note: do NOT flip {GAP_ID} Status to DONE — coordinator handles per gap-done-discipline.md

## PR body — MANDATORY sections
Per Wave 32 rework brief §3.4 + §3.5 (codified in `wave-pack-planner/SKILL.md` §Step 4.6 model-agnostic gates), every PR body PHẢI có 2 sections sau (BLOCK merge nếu thiếu):

### §"Local verification (pre-push)"
Paste literal command output:
```
$ pnpm exec tsc --noEmit          # OR: mvn test -pl {MODULE}
<output showing PASS / 0 errors>

$ pnpm test --run <changed-files>  # OR: mvn verify
Test Files  N passed (N)
Tests  M passed (M)

$ pnpm build                       # FE only
✓ Compiled successfully
```

### §"AC Coverage"
Table mapping mỗi AC line trong gap → file/test/verification evidence:

| AC | Status | Evidence |
|----|:-:|---|
| <AC line từ gap> | ✅ | `path/to/file.ts:42` + test name |
| <AC line khác> | ✅ | `path/to/test.spec.ts` |

Status values: ✅ DONE, 🟡 PARTIAL (with `// TODO(GAP-XXX)` + follow-up gap link), ❌ NOT DONE (block merge).

**Anti-pattern signals (auto-reject by reviewer):**
- §"Local verification" missing OR shows skipped tests without plan-level deferral
- §"AC Coverage" table absent OR has "TBD" entries
- Mock-as-implementation without `// TODO(GAP-XXX)` + filed follow-up gap (per `gap-done-discipline.md` §2)
- CWD verification missed (worktree-isolated agents): paste `pwd | grep -F "/agent-"` confirming you're inside the assigned worktree, NOT main repo (per `feedback_worktree_absolute_path_contamination.md`)
```

## Required placeholders

| Placeholder | Example | Notes |
|---|---|---|
| {GAP_ID} | GAP-144 | Single gap |
| {GAP_TITLE} | Alertmanager production receivers | |
| {MIGRATION_VERSIONS} | V47, V48 | Pre-assigned, NEVER auto-pick |
| {GAP_RANGE} | GAP-260..GAP-265 | For follow-up gaps if needed |
| {CONFIG_PREFIX} | `kitehub.alertmanager.*` | Avoid key collision with peer agents |
| {ALLOWED_PATHS} | `infrastructure/helm/kitehub/templates/`, `infrastructure/helm/kitehub/values.yaml` (Alertmanager section ONLY) | Be explicit |
| {RELATED_DOCS} | `documents/02-architecture/adr/ADR-022-alertmanager.md` | Context |
| {MODULE} | Maven module path | For test commands |
| {SCOPE} | helm | Conventional-commit scope |

## Gotchas

- **Worktree drift:** if you `cd` to main repo by mistake, your changes will collide with peer agents. Use `pwd` checks. Per `feedback_parallel_agent_strategy.md` rule #3.
- **Worktree absolute-path bug** (per `feedback_worktree_absolute_path_contamination.md`, Wave DR/Backup 2026-04-28): if coordinator's prompt cites absolute paths (`/home/.../scripts/foo.sh`), agent may bypass worktree cwd → Write lands in MAIN repo, commits land on WRONG branch. Concrete case: Wave DR/Backup Agent B's GAP-118 commit landed on Agent C's branch → coordinator forced to rebase + force-push. **Mitigation:** verify cwd before every Write/Edit: `pwd | grep -q "\.claude/worktrees/agent-" || { echo "NOT IN WORKTREE — abort"; exit 1; }`. Use RELATIVE paths in your own commands (`scripts/foo.sh` not `/home/.../scripts/foo.sh`). Verify branch before commit: `git branch --show-current | grep -E "^(worktree-agent-|feat/wave-)"`.
- **Shared config files:** if {ALLOWED_PATHS} mentions `application.yml` or `values.yaml`, edit ONLY your assigned section — do NOT reformat whole file (causes SOFT merge conflicts; coordinator pays cost).
- **Migration version race:** auto-picking next free V_n ALWAYS collides with peer agents. Use only reserved slot.
- **Outbox bypass:** if you add `rabbitTemplate.convertAndSend(...)`, you MUST cite Exception A/B/C/D from `design-patterns.md` §3.5.1 in code comment. Otherwise audit blocks PR.
- **Test profile escape hatch** (per `feedback_parallel_agent_strategy.md` rule #4): security/CSRF/auth components need test-profile bypass or every `@SpringBootTest` breaks.
- **Helm chart tests:** run `helm template` and `helm lint` before commit; PR CI also runs but local catches faster.
- **YAML validation** (per `feedback_yaml_validate_before_push.md`): if PR touches `.github/workflows/*.yml`, run `python3 -c "import yaml; yaml.safe_load(open('...'))"` first — colon-space in unquoted strings parses to mapping silently.

## When NOT to use this template

- Pure docs work → `docs-only-agent.md`
- Pure test backfill → `test-only-agent.md`
- Cleanup/dead-code → `p3-cleanup-agent.md`
- Foundation PR (wave plan itself) → coordinator writes directly, not via agent
- Migration-only PR with no app code → still use this template; migration IS code

## Reference

- Methodology: [`../../SKILL.md`](../../SKILL.md) Step 3
- Spawn pattern: [`../../reference/agent-spawning-template.md`](../../reference/agent-spawning-template.md)
- Superpowers: `.claude/skills/core/{brainstorming-methodology,task-breakdown-guide,tdd-enforcement,two-stage-code-review}.md`
- Worked example: Agent C of Wave Observability 2026-04-28 (GAP-144 — Alertmanager receivers in helm + ExternalSecret + ADR-022, test asserts on rendered manifest via `helm template`)
