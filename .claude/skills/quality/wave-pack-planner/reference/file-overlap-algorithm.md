# File Overlap Algorithm — `scripts/analyze-overlap.sh`

Detail cho Step 2 trong [SKILL.md](../SKILL.md). Algorithm dùng bởi script + manual fallback khi script không cover edge case.

## Input / Output

**Input:** N gap file paths (e.g. `documents/04-quality/gaps/GAP-121.md GAP-143.md GAP-144.md`)

**Output:** Markdown matrix table — `File | Touched by | Conflict risk` — 3-level risk: `None` / `SOFT` / `HARD`

## Parse strategy

Algorithm chính: **regex extract** từ 3 sections của mỗi gap file:

1. `## Affects` (or `**Affects:**` field trong frontmatter-ish header)
2. `## Proposed Fix`
3. `## Acceptance Criteria` (often references file paths in checkboxes)

### Path-detection patterns

| Pattern | Example | How extracted |
|---------|---------|---------------|
| Backtick paths | `` `infrastructure/helm/kitehub/values.yaml` `` | regex `` `([\w./_-]+\.[a-z]+)` `` |
| Bold file markers | `**File:** path/to/x.java` | regex `\*\*File:\*\*\s*([^\s]+)` |
| Inline `path/file.ext` | `migration `V47__init.sql`` | combined backtick + path regex |
| List-item paths | `- src/main/java/Foo.java` | regex `^[-*]\s+([\w./_-]+\.[a-z]+)` per line |
| Glob patterns | `documents/05-guides/operations/runbooks/*.md` | preserve as-is, expand at compare time |
| NEW file markers | `(NEW)` or "create new file" wording | tag as NEW → defaults None risk |

### Section boundary detection

- Start: line matches `^##\s+(Affects|Proposed\s+Fix|Acceptance\s+Criteria)`
- End: next `^##\s+` heading OR EOF
- Skip code fences (` ``` ` blocks) — they often contain unrelated example paths

## Risk classification rules

After extracting per-gap file lists, build matrix `file × agent`. Classify each cell:

| Rule | Risk | Why |
|------|:----:|-----|
| Only 1 agent touches file | None | No collision possible |
| File marked NEW + no other agent NEW-creates same path | None | Empty file, no merge |
| Multi-agent, file ext = `.md` shared (e.g. README, ROADMAP) | SOFT | Different sections, git auto-merges 80% time |
| Multi-agent, file ext = `.yaml`/`.yml` Helm/k8s values | SOFT | Different keys, rare nested-key collision |
| Multi-agent, file = `pom.xml` | SOFT | Different `<dependency>` groups merge cleanly; conflict only if both bump same artifact |
| Multi-agent, file = `application.yml` / `application-*.yml` | **HARD** | Spring config flat properties race; per `feedback_parallel_agent_strategy.md` rule #2 lead-owns |
| Multi-agent, file ext = `.sql` migration AND same `V##__` prefix | **HARD** | Version collide; pre-assign V_n / V_n+1 / V_n+2 |
| Multi-agent, file ext = `.sql` migration, different version slots | None | Pre-assigned slots disjoint |
| Multi-agent, single Java/TS source file | **HARD** | Line-level edits collide; refactor cluster |
| Multi-agent, FE component file (`.tsx`/`.vue`) | **HARD** | Render tree edits collide |
| Multi-agent, JSON config (`package.json`, `tsconfig.json`) | SOFT | Usually disjoint sections; verify no version bump race |
| Multi-agent, `.gitignore` / `.dockerignore` | SOFT | Append-only typical |

## Edge cases

### File rename
Source path A → renamed to B by agent X. Other agent Y still references A.
- **Detection:** `git log --diff-filter=R` after agents land, OR explicit `(rename to ...)` in gap text
- **Action:** SOFT if Y only reads A; HARD if Y modifies A (Y's edits lost on rename)
- **Mitigation:** wave plan pre-declares renames; affected agents informed in prompt

### File deletion
Agent X deletes file F. Agent Y modifies F.
- **Detection:** "delete" keyword in gap Proposed Fix
- **Action:** **HARD** — Y's PR fails to merge (file gone)
- **Mitigation:** Y delete first OR sequence X→Y with Y's edits informed

### Glob patterns
Gap text says "all files in `runbooks/*.md`". Other gap touches `runbooks/specific.md`.
- **Detection:** `*` / `**` in extracted path
- **Action:** Expand glob via `find` at analyze time; intersect against other agent's concrete paths
- **Tooling:** script should `find <glob>` and re-classify per-file

### NEW files (low risk, easy to miss)
Gap creates `assets/new-thing.png`. None other touches → None.
- **Detection:** "(NEW)" tag, "create" verb, file doesn't exist in current `git ls-files`
- **Action:** None risk — but verify path doesn't collide with another agent's NEW file

### False positives from prose
Gap mentions "see `infrastructure/foo.yaml` for context" but doesn't modify it.
- **Detection:** Path appears in `## Background` / prose paragraph, NOT in `## Affects` / `## Proposed Fix` / `## Acceptance Criteria`
- **Action:** algorithm restricts to 3 named sections (above) → reduces false-positive
- **Manual override:** wave planner reviews matrix, removes lines that are prose-mention-only

### Cross-service shared file
Gap A touches `kiteclass/.../File.java`, Gap B touches `kitehub/.../File.java` — same name, different module.
- **Detection:** full path key includes module prefix
- **Action:** treat as 2 different files → None risk
- **Pitfall:** shortening to `File.java` misclassifies → script must use full path

## Worked example (Wave Observability 2026-04-28)

Input: `GAP-121 GAP-143 GAP-144` → 3 agents A/B/C respectively.

Expected output (from `documents/03-planning/waves/wave-2026-04-29-observability.md` §"File overlap analysis"):

| File | Touched by | Conflict risk |
|------|-----------|:-------------:|
| `documents/05-guides/operations/runbooks/*.md` | A only | None |
| `kitehub/docker/prometheus/alert-rules.yml` + `kiteclass/docker/prometheus/alert-rules.yml` | A only (runbook_url annotations) | None |
| `infrastructure/helm/kitehub/templates/prometheusrule.yaml` | A only (annotations) | None |
| `infrastructure/helm/kitehub/values.yaml` Grafana section | B only | None |
| `infrastructure/helm/kitehub/values.yaml` Alertmanager section | C only | None |
| `infrastructure/helm/kitehub/values.yaml` (whole file) | B + C | **SOFT** — different sections, git auto-merges; integrator resolves at sequential merge if not |
| `infrastructure/helm/kitehub/templates/grafana-dashboards/*.yaml` (NEW) | B only | None |
| `infrastructure/helm/kitehub/templates/alertmanager-external-secret.yaml` (NEW) | C only | None |

Net: only `values.yaml` shared B+C, non-overlapping sections → SOFT, OK to ship.

**Self-test:** `./scripts/analyze-overlap.sh GAP-121 GAP-143 GAP-144` should reproduce this table. Divergence = script bug.

## When to skip the script

- Cluster is 2 gaps only — eyeball the gap files
- All gaps explicitly say "NEW directory `xyz/`" — None risk by construction
- Script bug (output mismatches manual analysis) — fall back to manual matrix, file gap to fix script

## Limitations (known unknowns)

- Doesn't catch cross-language imports (e.g. Java A references TS file B that B agent modifies — unlikely but possible)
- Doesn't analyze ArchUnit / test fixtures that may break when surface area changes
- Markdown renderers (e.g. anchors) can break silently when 2 docs link cross-doc anchors that one agent renames
- Risk levels are heuristics from 1 wave data point — recalibrate per `data/wave-history.jsonl`

## Related

- [SKILL.md](../SKILL.md) — entry point Step 2
- [cluster-pattern.md](cluster-pattern.md) — eligibility check before running this
- `scripts/analyze-overlap.sh` — implementation
- Memory `feedback_parallel_agent_strategy.md` rule #1 (pre-assign migration slots) + rule #2 (lead owns shared files)
