---
title: Next-session handoff — Option B single-gap focus after Cluster 4 oversized verdict
status: active
created: 2026-04-28
updated: 2026-04-28
gaps: [GAP-122, GAP-067, GAP-066, GAP-068]
recommended_pick: GAP-122
fallback_picks: [GAP-067-phase-1-stub]
---

# Next-session handoff — Single-gap focus

**Decision context (2026-04-28 evening):** Cluster 4 KH admin (GAP-066/067/068) flagged as oversized cluster per `cluster-pattern.md` §"Anti-cluster patterns". Each gap = multi-week feature (066 ~2-3w, 067 ~11w phased, 068 ~3w), not fit wave-pack 60-75 min target. User chose **Option B = single-gap focus** ship full PR. This file pre-decides next session's pick + plan so `/start-session` outputs concrete next action.

---

## TL;DR for next session

1. Run `/start-session` — collector should surface this plan via ROADMAP "Next recommended" line
2. Decide: **GAP-122 (recommended)** OR **GAP-067 Phase 1 stub (fallback)** OR override
3. Branch: `feat/single-{gap-id}-{slug}`
4. Ship 1 PR, target wall-clock 2-3h
5. Update ROADMAP wave-closure entry post-merge

---

## Recommended pick: GAP-122 — 12 platform-critical alerts

**Why this beats GAP-067 Phase 1:**
- ✅ No infra blocker — Prometheus ready, Alertmanager landed Wave Obs PR #627, runbook template landed PR #626
- ✅ Estimable scope: ~3-4h (12 alerts × ~10 min + CI check + alerting-standards doc + tests)
- ✅ High value — closes ops-readiness audit P1 finding directly
- ✅ Clean dependency chain — only depends on already-DONE GAP-120 (routing) + GAP-121 (runbooks)
- ✅ Single PR scope — no Phase 1/2/3 carve-out needed
- ✅ Doc-only side artifacts (alerting-standards.md) → low review surface

**Scope (single PR):**

| Sub-task | Effort |
|----------|--------|
| 12 alerts (`alert-rules.yml` updates split between docker + helm — same content) — categorize critical (4) vs warning (8) per gap §Proposed Fix | ~2h |
| Per-alert runbook stubs in `documents/05-guides/operations/runbooks/` (12 files using GAP-121 template) | ~30min |
| `documents/05-guides/alerting-standards.md` — categorization rules + `runbook_url` annotation requirement | ~30min |
| CI check: pre-commit script asserting new alert has `runbook_url` annotation | ~30min |
| Tests: smoke `helm template` + Prometheus `promtool check rules` if available | ~15min |

**Files to touch (estimate):**
- `kitehub/docker/prometheus/alert-rules.yml` (+ kiteclass equivalent)
- `infrastructure/helm/kitehub/templates/prometheusrule.yaml`
- `documents/05-guides/operations/runbooks/{12-new}.md` (NEW)
- `documents/05-guides/alerting-standards.md` (NEW)
- `scripts/check-alert-runbook-url.sh` (NEW, optional)
- `.github/workflows/script-quality.yml` (job add, optional)

**Acceptance criteria (per gap file):**
- [ ] 12 new alerts implemented with severity classification (4 critical → page; 8 warning → Slack)
- [ ] Runbook per alert (using GAP-121 template)
- [ ] Test: trigger each alert OR document amtool fire-recipe (per Wave Obs GAP-144 pattern)
- [ ] `alerting-standards.md` document created
- [ ] CI check: new alert requires `runbook_url` annotation

**Risks:**
- 12 runbooks × even 5-min stub = noise risk if stubs stay too thin → mitigate by using GAP-121 template strictly (sections must be filled, not "TBD")
- `alert-rules.yml` lives in 2 places (docker + helm) — risk drift; integrate via shared values OR document maintenance cadence

**Branch suggestion:** `feat/single-gap-122-platform-alerts`
**PR title suggestion:** `feat(ops): GAP-122 — 12 platform-critical alerts + runbook stubs + alerting-standards`

---

## Fallback pick: GAP-067 Phase 1 (stub-only scope)

⚠️ **Infra-blocked for full Phase 1** — needs Loki (GAP-114/115 deferred to Wave 7+). Only viable as **stub-only PR** (UI scaffold + mock backend endpoint), real Loki integration deferred to follow-up.

**Stub scope (~2-3h):**
- New route `/admin/instances/[id]/logs` with mock log-tail UI (SSE-ready stub)
- New backend endpoint `/api/v1/admin/instances/{id}/logs?since=&level=` returning mock log lines
- Frontend `LogTail.tsx` component (auto-refresh, level filter, search input — all client-side on mock data)
- Document Loki integration path in code comment + Phase 1 follow-up note

**Why fallback not primary:**
- Stub work feels speculative when real Loki not even queued for current quarter
- Returns less value per hour than GAP-122's actually-deployable alerts
- Sets up future scope-creep risk (mock vs real divergence)

**Branch suggestion if chosen:** `feat/single-gap-067-phase-1-stub`

---

## Override option

If next session prefers entirely different work:

- **K-12 features Wave (GAP-055/056/057)** — IN_PROGRESS per ROADMAP queue #7. GAP-055 Tasks 0-2 DONE, Tasks 3-10 remain. Single-gap = pick GAP-055 Task 3 OR GAP-056 / GAP-057. BL-P0 priority.
- **Business correctness cluster (GAP-049/050/150)** — Cluster 5 in queue, all P0+P1. If next session has appetite for re-evaluating cluster vs single-gap, could be wave-pack candidate (TBD eligibility check via skill Step 1-2).
- **AI Branding cluster (GAP-006/223 Sub-PR 223.2)** — Still ⏸ DEFERRED on Ollama+stack. Don't pick unless infra ready.

---

## Cluster 4 deferred work — sliced into sub-gaps (NOT FILED, recommendation only)

If user later wants to revive Cluster 4 as wave-packs, decompose into Phase 1 sub-gaps:

| Original | Phase 1 sub-gap (proposed) | Effort |
|----------|---------------------------|--------|
| GAP-066 (analytics dashboard) | `platform_metrics` table + 1 chart section (Revenue Growth MRR) + CSV export | ~2-3h |
| GAP-067 (ops console) | Phase 1 stub (above) OR wait for Loki + ship full Phase 1 | ~2-3h stub / ~4w real |
| GAP-068 (admin branding) | `/admin/branding` route + Instances tab only (no Templates/Analytics/Moderation/Actions) | ~3h |

These 3 sub-gaps **would be** wave-pack-eligible (~9h serial → ~3h parallel via 3 agents). File as GAP-258/259/260 if user approves the slicing approach.

---

## Session-end state (2026-04-28 evening, before /clear)

**Repo:** clean, on branch `chore/session-handoff-cluster-4-sliced` (this PR's branch)
**Counts:** 98 OPEN (post Wave DR/Backup closure)
**Recent merges:** PR #635 closure (709cb840), PR #634 GAP-118, PR #633 GAP-119, PR #632 GAP-117, PR #631 foundation, PR #630 wave-pack-planner skill
**Local branches:** main only (after this PR merges)
**Worktrees:** 0
**Session locks:** orphan from earlier this session (next `/start-session` purges automatically)

## Log

- 2026-04-28 evening — Created during session-handoff after Cluster 4 KH admin flagged as oversized per cluster-pattern.md. User chose Option B (single-gap focus) as alternative. This plan pre-decides next session's pick to minimize re-evaluation cost.
