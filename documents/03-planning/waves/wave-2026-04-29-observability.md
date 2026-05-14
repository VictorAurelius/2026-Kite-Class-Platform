---
title: Wave Observability — runbooks + Grafana + Alertmanager receivers
status: complete
created: 2026-04-28
updated: 2026-04-29
gaps: [GAP-121, GAP-143, GAP-144]
deferred_to_next_wave: [GAP-122]
deferred_separate_track: [GAP-114, GAP-115]
---

<!-- wave-plan-completeness-exempt: Pre-Wave-76 legacy plan — predates current _TEMPLATE.md structure -->

# Wave Observability — Cluster Pack 1

**Wave date:** 2026-04-29 (kicked off 2026-04-28 evening)
**Cluster theme:** Production observability — close on-call gaps that GAP-111/GAP-120 foundation work left as stubs
**Strategy reference:** First demonstration of agent-first wave-pack methodology proposed 2026-04-28 session (Option C — execute then codify framework)

## Scope

| # | Gap | Title | Priority | Agent | Disjoint files |
|:-:|-----|-------|:--------:|:-----:|----------------|
| 1 | **GAP-121** | Per-alert runbooks library | 🟠 P1 | A | `documents/05-guides/operations/runbooks/*.md` (NEW), runbook_url annotations in alert rule files |
| 2 | **GAP-143** | Grafana dashboards in Helm | 🟠 P1 | B | `infrastructure/helm/kitehub/values.yaml` Grafana section, new ConfigMap templates, dashboard JSON |
| 3 | **GAP-144** | Alertmanager production receivers | 🔴 P0 | C | `infrastructure/helm/kitehub/values.yaml` Alertmanager section, new ExternalSecret template |

## Deferred (next wave)

- **GAP-122** — Missing platform alerts (12 new alert rules). Deferred because adding `runbook_url` to new alerts would race with Agent A's work on existing alerts; cleaner to land 121 first then 122 as wave 2.
- **GAP-114** — Structured JSON logging (multi-service migration, multi-PR scope per `logs-format-standard.md` Phase 2). Tracked separately, not bundled.
- **GAP-115** — Log aggregation pipeline (depends on 114). Same.

## File overlap analysis

| File | Touched by | Conflict risk |
|------|-----------|:-------------:|
| `documents/05-guides/operations/runbooks/*.md` | A only | None |
| `kitehub/docker/prometheus/alert-rules.yml` + `kiteclass/docker/prometheus/alert-rules.yml` | A (runbook_url annotations) | None — only A touches |
| `infrastructure/helm/kitehub/templates/prometheusrule.yaml` | A (annotations) | None — only A touches |
| `infrastructure/helm/kitehub/values.yaml` Grafana section | B only | None |
| `infrastructure/helm/kitehub/values.yaml` Alertmanager section | C only | None |
| `infrastructure/helm/kitehub/values.yaml` (whole file) | B + C | **SOFT** — different sections, git usually auto-merges; if not, integrator resolves at sequential merge |
| `infrastructure/helm/kitehub/templates/grafana-dashboards/*.yaml` (NEW) | B only | None |
| `infrastructure/helm/kitehub/templates/alertmanager-external-secret.yaml` (NEW) | C only | None |

Net: only `values.yaml` is shared between B + C, and they edit non-overlapping sections.

## Agent workflow

Per `feedback_parallel_agent_strategy.md`:

1. Each agent gets `isolation: "worktree"` (separate git checkout)
2. Branches off main (after this foundation PR merges)
3. Commits + creates own PR — branch naming: `feat/wave-obs-{gap-id-slug}`
4. Reports back PR number + scope summary
5. Coordinator (me) merges sequentially: A → B → C
6. If values.yaml conflict at C merge → coordinator resolves (Alertmanager section is well-bounded)
7. Wave closure ROADMAP entry after all 3 merge

## Acceptance criteria (wave-level)

- [ ] 3 PRs merged (one per gap) with green CI
- [ ] All 3 gap files transitioned 🔵 OPEN → 🟢 DONE per `gap-done-discipline.md` §2 (every AC checkbox checked, no banned phrases in DONE-flip Log)
- [ ] ROADMAP "Current Status Snapshot" gets wave-closure entry (counts updated, queue rotated)
- [ ] No conflicts left unresolved on main
- [ ] Worktrees cleaned post-merge per `feedback_parallel_agent_strategy.md` rule #6

## Wall-clock target

- Foundation PR: ~10 min
- 3 parallel agents: ~20-30 min wall (each ~30-60 min agent-time, parallel)
- Sequential merge + conflict resolution: ~15 min
- Closure: ~5 min
- **Total wave: ~60-80 min**

## Lessons-learned target (for tomorrow's framework PR)

After this wave merges, capture:
- Did agent worktree-isolation hold? (Phase 2b had cross-contamination — verify no recurrence)
- Did values.yaml auto-merge? (sample data point for future shared-file estimates)
- Did agents need clarification rounds? (proxy for prompt quality)
- Total tokens consumed (proxy for cost-per-gap)

These feed into `quality/wave-pack-planner/SKILL.md` (Day 2 of Option C).

## Log

- 2026-04-28 — Wave plan created. Foundation PR will land this doc + ROADMAP active-wave callout. After merge, 3 agents spawn from main.
