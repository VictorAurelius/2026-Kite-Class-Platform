---
title: Wave 55 — Production Observability Validation (Loki + Tracing + Alert Receivers)
status: complete
created: 2026-05-11
updated: 2026-05-11
waves: [55]
gaps: [GAP-434, GAP-112, GAP-144]
---

# Wave 55 — Production Observability Validation

**Goal:** Close 3 disjoint observability gaps in parallel so Phase 1 BETA critical-path step 2 ("production observability validation") flips from ⏳ to ✅ within ~3 weeks wall-clock.

**Trigger:** Wave 54 Bucket B observability state-check (PR #1109) verdict 🟡 PARTIAL-VERIFIABLE on Phase 1 BETA §3.6 — surfaced 4 follow-up gaps; this wave closes 3 of them in parallel (GAP-257 S3-backup deferred to Wave 56-57 per ROADMAP).

**Estimated wall-clock:** ~32-42h agent work distributed across 3 background agents; longest-bucket (B tracing) ~16-20h sequential equivalent → ~5-7 calendar days with parallelism + reviewer cycles.

---

## 1. Brainstorm (5-10 min)

**Q1 (alignment):** which personas / domains / waves does this serve?
- **On-call / SRE persona** — primary beneficiary; today must `kubectl logs` per service + grep across 5 logs to correlate one slow request. Loki gives unified search; tracing gives per-request timeline; alert receivers route incidents to the right channel.
- **Phase 1 BETA critical-path step 2** — this wave IS step 2 closure trigger.
- **Phase 4 audit (Wave 53)** carry-forward: 60/100 ops-readiness baseline cannot reach the 80 production-gate without log aggregation + tracing per `2026-05-08-wave-40-ops-readiness.md`.

**Q2 (trade-offs):**
- **Loki single-binary vs microservices mode** → Single-binary (Phase 2a) per GAP-434 §Proposed Fix step 3. Cost-driven; microservices mode (Phase 2b) deferred until log volume warrants. Rejected: shipping microservices upfront — premature complexity for Phase 1 BETA invite-only volume.
- **Tracing backend Tempo vs Jaeger** → Tempo (already in `kube-prometheus-stack` ecosystem; OTLP-native; no separate Cassandra). Rejected: Jaeger — extra storage backend lift.
- **Alert receivers external-secrets-operator (ESO) vs sealed-secrets** → ESO already used in `alertmanager-external-secret.yaml` per existing template. Continue with ESO; reject sealed-secrets to avoid two secret strategies.
- **Wave-pack vs serial** → Wave-pack 3 parallel agents per `feedback_parallel_agent_strategy.md` rule #1; ~5x speedup proven Wave Observability 1 (~75min for 3 gaps vs ~6h serial).

**Q3 (risks):**
- **Risk A: `values.yaml` merge conflict** between Bucket A (adds `loki:` section) and Bucket C (extends `alertmanager.config.receivers:`) — same file, different YAML sub-trees. Mitigation: worktree isolation + sequential rebase; reviewer scans for cross-section conflicts in PR diff. If conflict materializes, coordinator rebases B onto A's HEAD.
- **Risk B: Tracing dep across 7 modules (gateway + 6 services)** — Bucket B touches many `pom.xml` + `application.yml` files; could collide with future BE PRs. Mitigation: Bucket B coordinator merges fast (no waiting on A or C); other in-flight BE work paused until B lands.
- **Risk C: `helm test loki` smoke requires actual cluster** — no local k8s cluster in solo-dev mode. Mitigation: Bucket A acceptance = `helm template` + `helm lint` clean + smoke-test script committed but execution deferred to first deploy (matches GAP-144 mock-fire precedent — chart-level wiring DONE without live cluster).
- **Risk D: Bucket C mock-fire still infra-blocked** if no cluster — same precedent as GAP-144 PARTIAL flip 2026-04-28. Mitigation: scope Bucket C to `amtool` offline test fixture + receiver YAML completion (not live mock-fire); if blocker remains, flip GAP-144 mock-fire ACs to PARTIAL with infra-block citation per `gap-done-discipline.md` §3.

---

## 2. Task Breakdown

| Bucket | Gap(s) | Owner | Effort | Disjoint? |
|--------|--------|-------|--------|-----------|
| A | GAP-434 | bg-agent | ~12-16h | ✅ helm chart + scripts only |
| B | GAP-112 | bg-agent | ~16-20h | ✅ Java code (7 modules) + pom.xml |
| C | GAP-144 mock-fire | bg-agent | ~4-6h | ⚠️ values.yaml overlap risk with A — see §1 Q3 risk A |

Disjoint check: A and B fully disjoint (helm vs Java). A and C overlap on `infrastructure/helm/kitehub/values.yaml` only (different YAML sub-trees). Coordinator merges A first, then rebases C.

---

## 3. Scope (compact schema — Strategy B+C proven Wave 33)

**Stake tier (per `wave-pack-planner/SKILL.md` §Step 4.6):** HIGH → model: Opus 4.7 full (production observability = critical-path step 2; mistakes cost incident-MTTR for entire Phase 1 BETA).
**Cross-layer? (per `wave-pack-planner/SKILL.md` §Step 4.5):** NO → skip Bucket 0 Foundation. All 3 buckets are pure DevOps/BE infrastructure with no FE consumer.

| # | Bucket | Gap(s) | Priority | Files (glob) | Spawn order |
|:-:|--------|--------|:--------:|--------------|:-----------:|
| 1 | **A** | GAP-434 | 🟠 P1 | `infrastructure/helm/kitehub/Chart.yaml` + `infrastructure/helm/kitehub/values.yaml` (loki section) + `infrastructure/helm/kitehub/templates/loki-*.yaml` (if needed) + `scripts/smoke-test.sh` (LOGS_OVERVIEW_E2E extension) | parallel |
| 2 | **B** | GAP-112 | 🟠 P1 | `kitehub/*/pom.xml` (6 modules) + `kiteclass/kiteclass-core/pom.xml` + `kiteclass/kiteclass-gateway/pom.xml` + `*/src/main/resources/application.yml` (tracing config) + `*/src/main/java/.../config/TracingConfig.java` (if needed) | parallel |
| 3 | **C** | GAP-144 mock-fire | 🔴 P0 | `infrastructure/helm/kitehub/values.yaml` (alertmanager.config.receivers section) + `infrastructure/helm/kitehub/templates/alertmanager-external-secret.yaml` + `documents/05-guides/operations/runbooks/alertmanager-mock-fire-runbook.md` | parallel (coordinator rebases on A) |

### Bucket A — Loki/Promtail Phase 2 stack

- Files (RELATIVE paths only per `feedback_worktree_absolute_path_contamination.md`):
  - `infrastructure/helm/kitehub/Chart.yaml` — add `grafana/loki-stack` to `dependencies:` block
  - `infrastructure/helm/kitehub/values.yaml` — add `loki:` section (single-binary mode, S3 schema config, Promtail DaemonSet enabled)
  - `infrastructure/helm/kitehub/templates/` — Loki-specific templates only if subchart values insufficient (prefer subchart values)
  - `scripts/smoke-test.sh` — append `LOGS_OVERVIEW_E2E` test (`helm test` + `count_over_time` LogQL query)
- Tests:
  - `helm lint infrastructure/helm/kitehub` clean
  - `helm template infrastructure/helm/kitehub > /tmp/rendered.yaml` clean
  - `helm dependency update infrastructure/helm/kitehub` succeeds
- Acceptance (subset of GAP-434 AC):
  - [ ] `Chart.yaml` lists `loki-stack` dependency with pinned version
  - [ ] `values.yaml` `loki:` section configures single-binary mode + S3 chunks + BoltDB index
  - [ ] Promtail scrapes `/var/log/containers/*.log` with JSON parsing
  - [ ] Grafana datasource auto-provisioned with uid `loki`
  - [ ] `helm lint` + `helm template` clean
  - [ ] `scripts/smoke-test.sh` extended with LOGS_OVERVIEW_E2E (execution deferred to live cluster per Risk C)

### Bucket B — Distributed tracing across 7 modules

- Files (RELATIVE paths):
  - `kitehub/kitehub-{gateway,subscription,branding,email,platform,admin}/pom.xml` — add `micrometer-tracing-bridge-otel` + `opentelemetry-exporter-otlp`
  - `kiteclass/kiteclass-core/pom.xml` + `kiteclass/kiteclass-gateway/pom.xml` — same deps
  - `*/src/main/resources/application.yml` — `management.tracing.sampling.probability` + `management.otlp.tracing.endpoint` (env-var driven)
  - `*/src/main/java/com/kite/.../config/TracingConfig.java` (only if W3C trace context propagation across RabbitMQ needs custom interceptor)
- Tests:
  - `./mvnw -pl <module> verify -P strict-warnings` clean for each of 7 modules
  - Add 1 unit test per service verifying `Tracer` bean is wired
- Acceptance (subset of GAP-112 AC):
  - [ ] All 7 modules have tracing deps in pom.xml
  - [ ] `application.yml` tracing config in all 7 (env-var `OTEL_EXPORTER_OTLP_ENDPOINT`)
  - [ ] traceId propagates: HTTP request → gateway → service (visible in MDC `traceId` field per `logs-format-standard.md` §2.2)
  - [ ] RabbitMQ traceparent propagation either works OOTB OR custom interceptor added with test
  - [ ] All 7 modules pass strict-warnings build

### Bucket C — Alertmanager mock-fire backfill (GAP-144 PARTIAL → DONE)

- Files (RELATIVE paths):
  - `infrastructure/helm/kitehub/values.yaml` — extend `alertmanager.config.receivers:` with real Slack/PagerDuty/SMTP routes (driven by ExternalSecret refs)
  - `infrastructure/helm/kitehub/templates/alertmanager-external-secret.yaml` — extend ExternalSecret keys for `slack-webhook-url`, `pagerduty-routing-key`, `smtp-password`
  - `documents/05-guides/operations/runbooks/alertmanager-mock-fire-runbook.md` — `amtool` mock-fire recipe (offline + online variants)
- Tests:
  - `helm template` clean (alertmanager config valid YAML)
  - `amtool check-config` against rendered alertmanager.yaml
- Acceptance (closes GAP-144 mock-fire 2 deferred ACs):
  - [ ] Slack receiver wired via ExternalSecret ref (no placeholder)
  - [ ] PagerDuty receiver wired via ExternalSecret ref
  - [ ] SMTP receiver wired via ExternalSecret ref
  - [ ] amtool mock-fire runbook published
  - [ ] If live-cluster verification still infra-blocked: GAP-144 stays 🟡 PARTIAL with citation; chart-level + runbook done = sufficient closure for solo-dev mode per `gap-done-discipline.md` §3 PARTIAL exit-ramp

---

## 4. State-Check Evidence (BẮT BUỘC per `audit-to-gap-pipeline.md` §2.6)

| Symbol | Type | Verification command | Evidence | Verdict |
|--------|------|----------------------|----------|---------|
| `infrastructure/helm/kitehub/Chart.yaml` | Helm chart manifest | `ls infrastructure/helm/kitehub/Chart.yaml` | 1 file, has `kube-prometheus-stack` dep already | ✅ exists |
| `loki-stack` Helm dependency | Chart dep | `grep -n "loki-stack" infrastructure/helm/kitehub/Chart.yaml` | 0 matches | 🆕 to-be-created (Bucket A) |
| `infrastructure/helm/kitehub/values.yaml` `loki:` section | Helm values | `grep -n "^loki:" infrastructure/helm/kitehub/values.yaml` | 0 matches | 🆕 to-be-created (Bucket A) |
| `templates/dashboard-logs-overview.yaml` | Grafana dashboard | `ls infrastructure/helm/kitehub/templates/dashboard-logs-overview.yaml` | 1 file (Phase 1 of GAP-115 shipped Wave 41) | ✅ exists — Bucket A wires datasource so panels resolve |
| `micrometer-tracing-bridge-otel` Maven dep | Java dep | `grep -rn "micrometer-tracing-bridge-otel" --include=pom.xml .` | 0 matches | 🆕 to-be-created (Bucket B) |
| `opentelemetry-exporter-otlp` Maven dep | Java dep | `grep -rn "opentelemetry-exporter-otlp" --include=pom.xml .` | 0 matches | 🆕 to-be-created (Bucket B) |
| `management.tracing.*` Spring config | YAML config key | `grep -rn "management.tracing\|management:.*tracing" --include=application.yml` | 0 matches | 🆕 to-be-created (Bucket B) |
| `templates/alertmanager-external-secret.yaml` | ExternalSecret | `ls infrastructure/helm/kitehub/templates/alertmanager-external-secret.yaml` | 1 file | ✅ exists — Bucket C extends |
| `alertmanager.config.receivers` placeholder | Receiver stubs | grep done in GAP-144 file (3 stubs documented) | 3 placeholder receivers (`default-webhook`, `critical-webhook`, `warning-email`) | ✅ exists (placeholders) — Bucket C replaces |
| `scripts/smoke-test.sh` | Smoke test | `ls scripts/smoke-test.sh` | 1 file | ✅ exists — Bucket A extends |
| `documents/05-guides/operations/runbooks/` | Runbook dir | `ls documents/05-guides/operations/runbooks/` | dir exists | ✅ exists — Bucket C adds new runbook |

Banned shortcuts (mirror §2.5):
- `| head` truncation on grep/find ✅ none used in evidence above
- Skipping verification "because agents will check at execution" ✅ all symbols verified pre-merge
- Aspirational references without 🆕 flag ✅ all 🆕 symbols owned by named bucket

---

## 5. Verification Gates (per bucket)

| Bucket | Local verify command | CI gate |
|--------|---------------------|---------|
| A | `helm dependency update infrastructure/helm/kitehub && helm lint infrastructure/helm/kitehub && helm template infrastructure/helm/kitehub > /dev/null` | (no helm CI yet — reviewer manual; consider adding in follow-up) |
| B | `cd kitehub && ./mvnw verify -P strict-warnings` for changed modules + `cd kiteclass/kiteclass-core && ./mvnw verify -P strict-warnings` | core-ci + kitehub-ci |
| C | `helm template infrastructure/helm/kitehub \| grep -A20 alertmanager` clean + `amtool check-config` against rendered config | (reviewer manual) |

---

## 6. Agent Spawn Pattern

Per `feedback_parallel_agent_strategy.md` + `agent-background-spawn-default.md`:
- All 3 buckets spawned with `run_in_background: true`
- Worktree isolation (`isolation: worktree`) for parallel safety
- RELATIVE paths in agent prompts per `feedback_worktree_absolute_path_contamination.md`
- Coordinator merges sequentially after all background completions: B first (most isolated; Java code) → A second (helm chart + values) → C last (rebases on A's values.yaml HEAD)
- Per `agent-aws-access.md`: agents do NOT touch AWS; chart changes only — deploy execution by user via workflow_dispatch (per `release-deploy-standard.md` §9 v1.0.1)

---

## 7. Closure Protocol

Per `gap-done-discipline.md` + `feedback_post_merge_doc_sync.md` + `feedback_wave_history_append_required.md` + `post-wave-cleanup.md` + `feedback_wave_closure_release_progress_report.md`:
- Each bucket PR updates affected GAP file Log + status (GAP-434 → DONE; GAP-112 → DONE; GAP-144 → DONE if mock-fire infra unblocked, else stays 🟡 PARTIAL with citation)
- ROADMAP §🚀 Next Action updated in closure PR — flip step 2 from ⏳ to ✅ if all 3 close cleanly
- Wave plan frontmatter `status: complete` flip in closure PR
- `wave-history.jsonl` append in closure PR (Rule 15 enforcement)
- Sub-gaps filed for any deferral; PARTIAL exit-ramp per `gap-done-discipline.md` §3
- Run `bash scripts/prune-merged-worktrees.sh --yes` after all bucket PRs merged, before drafting closure PR
- **`## Release Plan Progress` section in closure PR body** — Phase 1 BETA critical-path step 2 status update + Waves Remaining table (3 rows: strict-min v0.9.0-beta / practical v0.9.0-beta / v1.0.0 PROD)

---

## 8. Log

- **2026-05-11** (draft): Plan created. State-check evidence in §4 confirms 3 disjoint scopes; Risk A (`values.yaml` cross-bucket overlap) noted with mitigation. Awaiting plan PR review/merge before agent spawn.
- **2026-05-11** (in-progress): 3 background agents spawned post plan-PR #1118 merge; all worktree-isolated, RELATIVE paths.
- **2026-05-11** (complete): Wave SHIPPED. **Outcomes:**
  - **Bucket A GAP-434** → PR #1119 → 🟡 PARTIAL (6/12 AC; chart-level wired, live-cluster `helm test` deferred per Risk C — no local k8s in solo-dev mode; matches GAP-144 mock-fire precedent)
  - **Bucket B GAP-112** → PR #1125 → 🟡 PARTIAL (3/5 AC; application-side foundation 100% complete across 7 modules + RabbitMQ auto-instrumented confirmed; 2 AC deferred to GAP-111 Phase 2 = live Tempo backend + Grafana dashboard)
  - **Bucket C GAP-144 mock-fire** → PR #1120 → 🟡 PARTIAL (chart-level + amtool runbook DONE; live-cluster mock-fire still gated platform deploy)
  - **Side-discoveries** → PR #1121 → 2 follow-up gaps filed (GAP-467 helm values.yaml Go-templates pre-existing PR #984 issue; GAP-468 Spring Boot BOM bump for 9 HIGH CVE in built jars — sequenced AFTER Bucket B)
  - **Risk A (`values.yaml` overlap A+C)** materialized as expected; mitigation worked — sequential rebase A→C produced clean merge (different YAML sub-trees: A added top-level `loki:`, C extended `monitoring.alertmanager.smtp` block)
  - **Wall-clock:** ~95 min from plan-merge to closure-PR-draft (3 agents parallel + sequential coordinator merge B→A→C). Estimate was ~5-7 calendar days; Wave-pack methodology delivered same scope in ~1.5h coordinator wall-clock.
  - **0-clarification streak:** 90 (Wave 55 = 0 clarification rounds across all 3 agents)
- **Phase 1 BETA critical-path step 2 status:** flipped ⏳ → 🟡 PARTIAL (chart-level + foundation shipped; live-cluster validation gated first deploy = step 4 Tier 3 cutover via GAP-449 → step 3 AWS funding decision unblock).
