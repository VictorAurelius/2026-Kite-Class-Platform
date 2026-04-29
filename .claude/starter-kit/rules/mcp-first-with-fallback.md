# MCP-First with CLI Fallback — Tool Selection Rule

**Priority:** 🟠 MANDATORY — applies to all workflow skills
**Version:** 1.0.0
**Created:** 2026-04-18
**Last-Reviewed:** 2026-04-29
**Reviewer-Approver:** @nguyenvankiet (starter-kit upstream maintainer)
**Applies to:** GitHub ops, filesystem ops, database introspection, any repeatable external system interaction

---

## 1. The Rule

> **When an MCP server is available, USE IT.** Fall back to CLI only when MCP unavailable, disconnected, or MCP tool lacks specific capability needed.

Rationale:
- MCP tools return **structured JSON** — no `jq` parsing, no regex on CLI output
- MCP tools are **version-stable** — CLI flags change across versions
- MCP reduces **context pollution** — parsed output vs raw CLI text
- MCP works across **transports** (local stdio, remote HTTP) without command changes

---

## 2. Tool Selection Matrix

| Operation | Primary (MCP if available) | Fallback (CLI) |
|-----------|---------------------------|----------------|
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

**Setup:**
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

### 4.2 PostgreSQL MCP

**Setup:**
```bash
claude mcp add postgres -s project \
  -- npx -y @modelcontextprotocol/server-postgres \
  "postgresql://USER:$POSTGRES_PASSWORD@localhost:5433/DBNAME"
```

**Use for:**
- Quick schema lookup without reading migration chain
- Data inspection in dev DB (never prod)
- Verifying migration applied correctly

**Fallback:** Read migrations + entity classes; use `docker exec <container> psql` for ad-hoc queries.

### 4.3 Playwright MCP

**When to prefer:** UI regression testing, screenshot capture after FE change.
**Fallback:** Existing `scripts/capture-screenshots.ts` or equivalent.

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

- Pre-merge PR review checks: any new skill touching GitHub/DB/browser ops should reference this rule
- Agent prompts for workflow tasks should include: "Use GitHub MCP if available, `gh` CLI otherwise"
- `audit-gate.py` hook logs which method was used (future enhancement)

---

## 7. Anti-Patterns

| ❌ Don't | ✅ Do |
|---------|-------|
| "Run `gh pr create --title X`" as the only documented way | "Create PR (MCP or `gh pr create --title X` fallback)" |
| Skip MCP because CLI is habit | Check `claude mcp list` first, prefer connected MCP |
| Hardcode MCP tool names that may break on update | Reference operation category (create_pr, list_runs) |
| Parse CLI JSON with 3-layer jq | Use MCP structured output if available |
| Require MCP for production scripts (CI, hooks) | Keep hooks/scripts CLI-only (no MCP assumption in automation) |

---

## 8. Log

- **2026-04-29** (v1.0.0 upstream import): Imported into starter-kit v2.3.0 from project source. Local project remains source of truth; upstream version may diverge as starter-kit evolves separately.
- **2026-04-28** (v1.0.0 backfill): Frontmatter backfill — added Version + Last-Reviewed + Reviewer-Approver fields. No content change.
- **2026-04-18:** Rule created after observing GitHub MCP was connected but unused. Session defaulted to `gh` CLI habit, wasting ~5 min on polling loops and ~100 tokens per CI check. All workflow skills updated to MCP-first pattern.
