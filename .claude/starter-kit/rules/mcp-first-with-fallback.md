# MCP-First, Dedicated-Tools-Second, Bash-Last — Tool Selection Rule

**Priority:** 🟠 MANDATORY — applies to all workflow skills + agent tool-calls
**Version:** 1.1.0
**Created:** 2026-04-18
**Last-Reviewed:** 2026-05-14
**Reviewer-Approver:** @nguyenvankiet (starter-kit upstream maintainer)
**Applies to:** GitHub ops, in-repo file ops (search/read/list), database introspection, any repeatable external system interaction, plus skill authors writing tool-call examples

---

## 1. The Rule

> **3-tier hierarchy:** (1) MCP server if connected → (2) Dedicated tools (Glob/Grep/Read/Edit/Write) → (3) Bash CLI as last resort.

**Use Bash only when neither tier 1 nor tier 2 covers the operation** — git, gh CLI, mvn/pnpm/docker builds, project scripts (`./scripts/*.sh`), interactive auth flows.

Rationale:
- **Tier 1 (MCP):** structured JSON output, version-stable, less context pollution, transport-agnostic
- **Tier 2 (Dedicated tools):** no subprocess overhead, pre-approved (no permission prompts), structured output modes (`output_mode=files_with_matches`/`count`/`content`, `head_limit`), safer than shell parsing
- **Tier 3 (Bash):** fallback when shell-only operation (git, build tools, scripts) — RTK auto-wraps for token savings

CLAUDE.md system prompt §"Using your tools" already mandates this; project rule formalizes for skill authors + reviewer enforcement.

---

## 2. Tool Selection Matrix

### 2.1 GitHub / DB / Browser (MCP tier 1 → CLI tier 3)

| Operation | Primary (MCP if available) | Fallback (Bash CLI) |
|-----------|---------------------------|---------------------|
| **List PRs** | GitHub MCP: `list_pull_requests` | `gh pr list --json ...` |
| **Create PR** | GitHub MCP: `create_pull_request` | `gh pr create --title ... --body ...` |
| **Get PR details** | GitHub MCP: `get_pull_request` | `gh pr view <n> --json ...` |
| **Merge PR** | GitHub MCP: `merge_pull_request` | `gh pr merge <n> --squash` |
| **Check CI runs** | GitHub MCP: `list_workflow_runs` | `gh run list --branch ... --json ...` |
| **Get CI logs** | GitHub MCP: `get_workflow_run_logs` | `gh run view <id> --log-failed` |
| **Create issue** | GitHub MCP: `create_issue` | `gh issue create --title ... --body ...` |
| **DB schema introspection** | Postgres MCP: query `information_schema` | Read Flyway migrations manually |
| **DB data inspection (dev)** | Postgres MCP: `query` | `docker exec postgres psql -c "..."` |
| **Browser automation** | Playwright MCP | `npx tsx scripts/capture-screenshots.ts` |

### 2.2 In-repo file ops (Dedicated tools tier 2 → Bash tier 3)

| Operation | Primary (Dedicated tool) | ❌ AVOID (Bash) |
|-----------|--------------------------|----------------|
| **List files by pattern** | `Glob pattern="docs/**/*.md"` | `ls`, `find` |
| **Search content in files** | `Grep pattern="..." path="..." output_mode="content"` (supports `-A`/`-B`/`-C`/`-n`/`-i`/`head_limit`/`type`/`glob` filters) | `grep`, `rg`, `ripgrep` |
| **Search and count matches** | `Grep ... output_mode="count"` | `grep -c \| wc -l` |
| **Find files matching content** | `Grep ... output_mode="files_with_matches"` | `grep -rl` |
| **Read specific file (full)** | `Read file_path="..."` | `cat`, `less` |
| **Read file slice** | `Read file_path="..." offset=N limit=M` | `head`, `tail`, `sed` |
| **Edit file (in place)** | `Edit file_path="..." old_string="..." new_string="..."` | `sed -i`, `awk -i` |
| **Create new file** | `Write file_path="..." content="..."` | `echo > file`, `cat <<EOF` |

**Why not Bash for these:**
- Subprocess overhead per invocation
- Plain-text output → harder to filter than tool's structured modes
- Permission prompts trigger for `ls`/`grep`/`find` outside allowlist
- RTK wrapping helps but doesn't beat zero-subprocess tool path
- No structured limits (`head_limit`, `output_mode`) — must `| head` manually

### 2.3 Bash legitimately needed (tier 3 — no tier-1/2 alternative)

| Operation | Bash command |
|-----------|--------------|
| Git operations | `git status`, `git log`, `git diff`, `git branch`, `git push`, etc. |
| GitHub CLI when MCP unavailable | `gh pr ...`, `gh run ...`, `gh api ...` |
| Build / test runners | `mvn`, `pnpm`, `npm`, `docker`, `docker-compose` |
| Project scripts | `./scripts/*.sh`, `bash .claude/skills/.../scripts/*.sh` |
| Interactive auth | `gcloud auth login`, `aws sso login` |
| Process management | `kill`, `lsof`, background job control |

These have no MCP equivalent and dedicated tools don't cover shell semantics — Bash is correct here. RTK wraps automatically.

---

## 3. How to Detect MCP Availability

```bash
claude mcp list
```

Look for `✓ Connected`. If status is `Needs authentication` or missing, treat as unavailable → use fallback.

Explicit tool availability check (runtime):
- If you call an MCP tool and get `InputValidationError` or "tool not found" → server disconnected, fall back to CLI.

---

## 4. Specific Instructions for Known MCP Servers

### 4.1 GitHub MCP (`ghcr.io/github/github-mcp-server`)

**Setup (playbook Phase 5.1):**
```bash
GITHUB_MCP_TOKEN=$(gh auth token)
claude mcp add github -s user \
  --env "GITHUB_PERSONAL_ACCESS_TOKEN=$GITHUB_MCP_TOKEN" \
  -- docker run -i --rm -e GITHUB_PERSONAL_ACCESS_TOKEN ghcr.io/github/github-mcp-server
```

**When to prefer MCP over `gh` CLI:**
- Polling CI status in loops (`until` loops) — MCP returns structured state cleaner
- Reading PR body/diff programmatically — no need for `--json ... --jq ...`
- Batch operations (create 5 issues, list 20 PRs) — MCP natively handles pagination

**Fallback conditions:**
- MCP tool schema not loaded in current session → use `gh` CLI
- Specific capability missing (e.g. some auth scopes not exposed) → use `gh api`
- CI/script context where MCP client not available → use `gh` CLI

### 4.2 PostgreSQL MCP (defer until Wave 3 stack up)

**Setup (playbook Phase 5.2):**
```bash
claude mcp add postgres -s project \
  -- npx -y @modelcontextprotocol/server-postgres \
  "postgresql://your-product-a:$POSTGRES_PASSWORD@localhost:5433/your-product-a"
```

**Use for:**
- Quick schema lookup without reading migration chain
- Data inspection in dev DB (never prod)
- Verifying migration applied correctly

**Fallback:** Read V##__*.sql migrations + entity classes; use `docker exec shared-postgres psql` for ad-hoc queries.

### 4.3 Playwright MCP (defer to Wave 4)

**When to prefer:** UI regression testing, screenshot capture after FE change.
**Fallback:** Existing `scripts/capture-screenshots.ts`.

---

## 5. Skill Authors: Integration Pattern

When writing or updating a workflow skill, structure external system calls like this:

```markdown
## Step N: Check CI status

**Primary (MCP if connected):**
Use GitHub MCP `list_workflow_runs` to poll CI. Filter by branch, status.

**Fallback (if MCP unavailable):**
```bash
until gh run list --branch <branch> --limit 1 --json conclusion \
  --jq '.[0].conclusion' | grep -qE 'success|failure'; do sleep 30; done
```
```

**Do NOT:**
- Hardcode specific MCP tool names in examples (they drift across server versions)
- Require MCP for core workflow — every skill must work with CLI alone
- Duplicate full examples — reference this rule doc instead

**DO:**
- Describe the OPERATION (what you want), not the TOOL (how to call it)
- Keep CLI fallback inline (1-2 lines) so skill is self-sufficient
- Link to this rule for MCP details

---

## 6. Enforcement

- **Pre-merge PR review** (manual): reviewer scans diff for `Bash(...)` tool calls invoking `ls`/`grep`/`find`/`head`/`tail`/`cat`/`sed`/`awk` on in-repo paths → flag and ask "could this be Glob/Grep/Read/Edit?" Skill files + agent-prompt templates are highest-priority review surface.
- **Memory auto-load (tier 2 enforcement):** `feedback_dedicated_tools_first.md` reinforces §2.2 per-session; lists banned patterns + fix mappings. Loads at session start before any tool call.
- **Skill author checklist:** any new/edited skill in `.claude/skills/**` referencing tool calls should follow §2 matrix; `simplify` skill self-review can flag anti-patterns.
- **Agent prompts for workflow tasks** should include: "MCP-first if connected; dedicated tools (Glob/Grep/Read) for in-repo file ops; Bash only for git/gh/build tools/scripts."
- **`audit-gate.py` hook** logs which method was used (future enhancement — would parse session transcripts; deferred per cost-benefit).

---

## 7. Anti-Patterns

| ❌ Don't | ✅ Do |
|---------|-------|
| `Bash(ls documents/04-quality/gaps/ \| grep -iE "ui-?kit")` | `Glob pattern="documents/04-quality/gaps/*ui*kit*.md"` |
| `Bash(grep -rilE "ui_kits" documents/)` | `Grep pattern="ui_kits" path="documents/" output_mode="files_with_matches"` |
| `Bash(head -80 GAP-348-*.md)` | `Read file_path="..." limit=80` |
| `Bash(grep -lE "Status" *.md \| while read f; do head ...; done)` | `Grep pattern="^\*\*(Status\|Priority):" output_mode="content" -A=0` |
| "Run `gh pr create --title X`" as the only documented way | "Create PR (MCP `create_pull_request` OR `gh pr create` fallback)" |
| Skip MCP because CLI is habit | Check `claude mcp list` first, prefer connected MCP |
| Hardcode MCP tool names that may break on update | Reference operation category (create_pr, list_runs) |
| Parse CLI JSON with 3-layer jq | Use MCP structured output if available |
| Require MCP for production scripts (CI, hooks) | Keep hooks/scripts CLI-only (no MCP assumption in automation) |
| Use `head -N` after `find`/`grep` for in-repo search | Use tool's `head_limit` parameter (Glob/Grep both support) |

---

## 8. Log

- **2026-05-05** (v1.1.0): MINOR — extended scope from MCP-vs-CLI binary to 3-tier hierarchy (MCP → dedicated tools → Bash). Added §2.2 In-repo file ops matrix (Glob/Grep/Read/Edit/Write vs ls/grep/find/head/cat/sed) + §2.3 legitimate Bash use cases. §6 Enforcement extended with reviewer-checklist + memory-auto-load tier-2 enforcement. §7 Anti-Patterns prepended with 4 in-repo-file-op rows. Triggered by 2026-05-05 user-flagged miss: pick-UI-kit-gaps session used `ls | grep` + `grep -rilE` + `head -80` for in-repo search instead of Glob/Grep/Read tools (CLAUDE.md system prompt says it but project rule didn't enforce). Per `incident-to-rule-pipeline.md` 5-stage applied: Detect ✓ (user "vừa rồi pick gaps đã dùng lệnh gì?") → Classify ✓ (rule existed in CLAUDE.md, no project-rule enforcement; `mcp-first-with-fallback.md` covered MCP-vs-CLI but NOT dedicated-tools-vs-Bash) → Rule+Enforce ✓ (this v1.1.0 + memory `feedback_dedicated_tools_first.md` paired same-PR per `rule-change-process.md` §6.5 Enforcement Parity Mandate) → Self-Test ✓ (worked example below) → Retro Log ✓ (this entry). Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per §5 — adds tier-2 coverage, no constraint loosening). Detector deferred (session-transcript scanning expensive; cost-benefit per `incident-to-rule-pipeline.md` §3 advisory-rule guard); enforcement = memory auto-load + reviewer checklist sufficient for solo-dev mode.

  **Self-test (worked example) — apply v1.1.0 to 2026-05-05 pick-UI-kit-gaps session:**
  - 4 Bash invocations triggered: `ls | grep -iE "ui-?kit"`, `grep -rilE "ui_kits" documents/04-quality/gaps/`, `grep -lE "Status" GAP-{...}*.md | while read f; do head ...`, `head -80 GAP-348-*.md`.
  - Apply §2.2 matrix: each maps to a banned-bash row; each has dedicated-tool replacement.
  - Apply §7 anti-pattern table: 4 of 4 rows match the anti-pattern column verbatim.
  - Verdict: rule fires correctly on the original incident. ✅

- **2026-04-28** (v1.0.0 backfill): Frontmatter backfill per GAP-249 — added Version + Last-Reviewed + Reviewer-Approver fields. No content change. Solo-dev PATCH self-approve per `rule-change-process.md` §5.
- **2026-04-18:** Rule created after Wave 3 observation — GitHub MCP was connected but unused. Session defaulted to `gh` CLI habit, wasting ~5 min on polling loops and ~100 tokens per CI check. All workflow skills updated to MCP-first pattern.
