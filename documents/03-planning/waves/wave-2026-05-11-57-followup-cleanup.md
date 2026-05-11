---
title: Wave 57 — Follow-up cleanup wave-pack (helm-lint + CVE bump + RLS perf baseline)
status: complete
created: 2026-05-11
updated: 2026-05-11
waves: [57]
gaps: [GAP-467, GAP-468, GAP-469]
---

# Wave 57 — Follow-up cleanup wave-pack

**Goal:** Close 3 disjoint follow-up gaps surfaced by Wave 55+56 in parallel: helm-lint unblock + 9 HIGH CVE bump + RLS perf baseline methodology. Drive repo level RED→GREEN + close GAP-466 perf-deferred-AC.

**Trigger:** Post-Wave-56 follow-up — `repo-status.sh` reports level RED driven by 9 high CodeQL alerts (Wave 55 side-discovery GAP-468) + helm-lint broken on main since PR #984 (Wave 55 side-discovery GAP-467) + RLS perf-measurement deferred from Wave 56 closure (GAP-469).

**Estimated wall-clock:** ~1 day total with 3 background agents parallel; longest-bucket (B BOM bump) ~3-4h sequential equivalent.

---

## 1. Brainstorm (5-10 min)

**Q1 (alignment):** which personas / domains / waves does this serve?
- **On-call / SRE** — Bucket A fixes `helm lint` so future helm work has chart-level smoke; Bucket B closes 9 HIGH CVE → repo level RED→GREEN.
- **Phase 1 BETA security gate** — Bucket B closes CVE-2025-41253 (spring-cloud-gateway) + CVE-2026-42198 (postgresql JDBC) + 6 netty CVE + bcprov CVE.
- **Phase 1 BETA critical-path step 2.5 closure** — Bucket C delivers the perf baseline methodology + harness; full measurement deferred staging deploy but unblocks GAP-466 perf AC.
- **Wave 55+56 follow-up housekeeping** — all 3 gaps filed by previous waves' side-discoveries.

**Q2 (trade-offs):**
- **3 parallel buckets vs sequential** → 3 parallel per `feedback_parallel_agent_strategy.md` rule #1. 3 disjoint zones (helm / maven / scripts+docs). Rejected: sequential — wastes ~6-8h coordinator wall-clock when parallel is ~3-4h.
- **Bucket B BOM bump scope** → bump Spring Boot 3.5.14 → latest stable 3.5.x; agent verifies which CVEs auto-resolve vs need explicit overrides. Rejected: pin individual netty/bcprov/scgw overrides upfront — premature; let BOM bump fix what it can, agent adds overrides only for unpatched.
- **Bucket C perf baseline scope** → harness + methodology + first dry-run on TestContainers; full staging measurement deferred. Rejected: full pgbench on staging — no staging cluster in solo-dev mode; per `release-deploy-standard.md` §9 staging-load measurement is post-deploy artifact. Methodology + scripts in repo = sufficient closure of GAP-469 chart-level.

**Q3 (risks):**
- **Risk A: Bucket B BOM bump breaks tests** — transitive dep changes (netty / bcprov / postgres / scgw) may shift behavior. Mitigation: agent runs `./mvnw verify -P strict-warnings` per all 7 modules; if any test breaks, file follow-up gap; do NOT merge until resolved or scope-cut to passing modules.
- **Risk B: Bucket B fixes <9 CVE** — BOM bump may not pick up latest patch line for all 4 CVE families (scgw / postgres / bcprov / netty). Mitigation: agent runs `gh api /repos/.../code-scanning/alerts?state=open&severity=error` post-build + re-scan; for any remaining open CVE, agent adds explicit `<dependencyManagement>` override in parent pom.
- **Risk C: Bucket A helm-template regression** — extracting Go-template blocks may alter rendered output. Mitigation: agent runs `helm template` before + after change; diffs MUST be no-op for the alertmanager.config section (same rendered YAML).
- **Risk D: Bucket C `pgbench` requires `pgbench` binary** — may not be installed locally. Mitigation: methodology doc explicitly handles "no local pgbench" path (e.g., `apt install postgresql-contrib` 1-liner OR run via Docker `postgres:16-alpine` image). Harness script gracefully degrades.
- **Risk E: zero overlap assumption wrong** — A touches values.yaml; B touches pom.xml; C touches scripts/perf/. Verified disjoint via file glob. Same-file overlap risk ZERO.

---

## 2. Task Breakdown

| Bucket | Gap | Owner | Effort | Disjoint? |
|--------|-----|-------|--------|-----------|
| A | GAP-467 | bg-agent | ~2h | ✅ helm chart only |
| B | GAP-468 | bg-agent | ~3-4h | ✅ pom.xml only |
| C | GAP-469 | bg-agent | ~3-4h | ✅ scripts/perf/ + docs only |

Disjoint check: A touches `infrastructure/helm/**`; B touches `*/pom.xml`; C touches `scripts/perf/` + `documents/04-quality/audits/performance/` — fully disjoint file globs. No coordinator rebase expected.

---

## 3. Scope (compact schema)

**Stake tier:** HIGH → model: Opus 4.7 full (Bucket B = security; Bucket A = unblocks all future helm work; Bucket C = closes GAP-466 final AC).
**Cross-layer?:** NO → skip Bucket 0 Foundation. All 3 buckets pure infra/BE.

| # | Bucket | Gap | Priority | Files (glob) | Spawn order |
|:-:|--------|-----|:--------:|--------------|:-----------:|
| 1 | **A** | GAP-467 | 🟠 P1 | `infrastructure/helm/kitehub/values.yaml` + `infrastructure/helm/kitehub/templates/alertmanager-config.yaml` (NEW) | parallel |
| 2 | **B** | GAP-468 | 🟠 P1 | `kitehub/pom.xml` + `kiteclass/pom.xml` + per-module `pom.xml` overrides if needed (all 7 modules) | parallel |
| 3 | **C** | GAP-469 | 🟡 P2 | `scripts/perf/rls-baseline.sh` (NEW) + `documents/04-quality/audits/performance/2026-05-11-rls-baseline-methodology.md` (NEW) + `documents/05-guides/operations/runbooks/rls-perf-baseline-runbook.md` (NEW) | parallel |

### Bucket A — Helm Go-templates extract

- Files (RELATIVE paths):
  - **UPDATE** `infrastructure/helm/kitehub/values.yaml` — remove `{{- if ... }}` blocks (lines 286-396 currently); replace with plain YAML data structure that the new template file consumes via `.Values.monitoring.alertmanager.receivers.production` and `.Values.monitoring.alertmanager.config`
  - **NEW** `infrastructure/helm/kitehub/templates/alertmanager-config.yaml` — contains the templated logic; renders to a `ConfigMap` or wires into the alertmanager subchart's config slot via Go template engine (where Helm DOES process templates)
- Tests:
  - `helm template infrastructure/helm/kitehub > /tmp/after.yaml` clean
  - `git stash && helm template infrastructure/helm/kitehub > /tmp/before.yaml; git stash pop` BEFORE fix would fail — confirms regression; AFTER fix renders clean
  - `diff` between before+fix rendered output should be NO-OP for the alertmanager.config section (semantic equivalence preserved)
- Acceptance (subset of GAP-467 AC):
  - [ ] `values.yaml` parses cleanly as plain YAML (no `{{-` / `}}-` tokens)
  - [ ] Templated logic moved to `templates/alertmanager-config.yaml`
  - [ ] `helm lint infrastructure/helm/kitehub` exits 0
  - [ ] `helm template` exits 0
  - [ ] CI guard (extend `scripts/check-docs.sh` or new `scripts/check-helm-lint.sh`) to prevent recurrence — optional this PR, can defer to follow-up if scope tight

### Bucket B — Spring Boot BOM bump + CVE closure

- Files (RELATIVE paths):
  - **UPDATE** `kitehub/pom.xml` — bump `<version>3.5.14</version>` → latest stable 3.5.x (agent checks Spring release notes)
  - **UPDATE** `kiteclass/pom.xml` — same
  - **UPDATE** if BOM bump insufficient, add explicit `<dependencyManagement>` overrides for: `io.netty:netty-bom`, `org.bouncycastle:bcprov-jdk18on`, `org.postgresql:postgresql`, `org.springframework.cloud:spring-cloud-gateway-server`
- Tests:
  - `./mvnw verify -P strict-warnings` clean across all 7 modules (gateway + 6 KH services + kc-core/gateway)
  - Post-merge: rebuild Docker images via `docker-build-push.yml` workflow + re-run `gh api /repos/.../code-scanning/alerts?state=open&severity=error` — expect 0 errors OR document remaining
- Acceptance (subset of GAP-468 AC):
  - [ ] Root parent BOM bumped to latest Spring Boot 3.5.x stable patch
  - [ ] Explicit overrides added for any package not auto-resolved
  - [ ] All 7 modules pass `mvn verify -P strict-warnings`
  - [ ] PR body documents which CVE auto-resolved by BOM vs explicit override
  - [ ] Post-merge code-scanning alerts targeted: 9 HIGH → 0 HIGH (or follow-up gap filed for residual)

### Bucket C — RLS perf baseline methodology + harness

- Files (RELATIVE paths):
  - **NEW** `scripts/perf/rls-baseline.sh` — `pgbench`-based harness measuring 3 endpoints (per GAP-469 §Proposed Fix step 1): student-list / student-insert / grades-filtered
  - **NEW** `documents/04-quality/audits/performance/2026-05-11-rls-baseline-methodology.md` — methodology doc: dataset shape (10 tenants × 10k students × 5 courses ≈ 500k rows) + pgbench script invocation + percentile measurement + reporting template
  - **NEW** `documents/05-guides/operations/runbooks/rls-perf-baseline-runbook.md` — runbook for running baseline on staging when available (when GAP-449 Tier 3 cutover ships)
- Tests:
  - `bash -n scripts/perf/rls-baseline.sh` clean syntax
  - `shellcheck scripts/perf/rls-baseline.sh` clean
  - Dry-run script on local TestContainers (10 tenants × 100 students subset for syntax verify; full 500k dataset deferred staging)
- Acceptance (closes GAP-469 chart-level):
  - [ ] Harness script created + shellcheck clean
  - [ ] Methodology doc with 3 endpoints + measurement protocol
  - [ ] Runbook for staging execution
  - [ ] Dry-run confirms script syntactic correctness (full execution deferred staging per `gap-done-discipline.md` §3 PARTIAL exit-ramp)
  - [ ] GAP-469 → 🟡 PARTIAL with citation (full pgbench measurement deferred until staging cluster ready post-GAP-449)

---

## 4. State-Check Evidence (BẮT BUỘC per `audit-to-gap-pipeline.md` §2.6)

| Symbol | Type | Verification command | Evidence | Verdict |
|--------|------|----------------------|----------|---------|
| `infrastructure/helm/kitehub/values.yaml` `{{- if ... }}` blocks | Helm Go templates | `grep -n "{{-" infrastructure/helm/kitehub/values.yaml` | 8 matches (lines 286, 295, 319, 332, 335, 354, 379, 396) | ✅ exists (Bucket A removes these) |
| `infrastructure/helm/kitehub/templates/alertmanager-config.yaml` | NEW template | `ls infrastructure/helm/kitehub/templates/alertmanager-config.yaml` | 0 files | 🆕 to-be-created (Bucket A) |
| `spring-boot-starter-parent` version 3.5.14 | Maven parent BOM | `grep -A2 spring-boot-starter-parent kitehub/pom.xml kiteclass/pom.xml` | both pom.xml `<version>3.5.14</version>` | ✅ exists (Bucket B bumps) |
| 9 HIGH CodeQL alerts | code-scanning evidence | `gh api /repos/.../code-scanning/alerts?state=open&severity=error \| jq length` | 9 HIGH errors at session start (collected Wave 55 triage; see GAP-468 §Problem) | ✅ exists (Bucket B closes) |
| `scripts/perf/rls-baseline.sh` | NEW script | `ls scripts/perf/rls-baseline.sh` | 0 files (no `scripts/perf/` directory either) | 🆕 to-be-created (Bucket C) |
| `documents/04-quality/audits/performance/` | Audit dir | `ls documents/04-quality/audits/performance/` | dir exists; multiple audits already filed (Wave 40 + Wave 54) | ✅ exists (Bucket C adds new file) |
| GAP-466 perf-baseline AC | open AC | `grep "deferred to GAP-469" documents/04-quality/gaps/GAP-466-multi-tenant-postgres-rls-defense-in-depth.md` | 1 match line 83 | ✅ exists (Bucket C closure flips GAP-466 perf AC) |

Banned shortcuts (mirror §2.5): no `| head` truncation; no skipping verification; no aspirational references.

---

## 5. Verification Gates (per bucket)

| Bucket | Local verify | CI gate |
|--------|--------------|---------|
| A | `helm lint infrastructure/helm/kitehub` + `helm template infrastructure/helm/kitehub > /dev/null` both exit 0 | (no helm CI yet; reviewer manual) |
| B | `cd kitehub && ./mvnw verify -P strict-warnings` + `cd kiteclass/kiteclass-core && ./mvnw verify -P strict-warnings` | core-ci + kitehub-ci |
| C | `bash -n scripts/perf/rls-baseline.sh` + `shellcheck scripts/perf/rls-baseline.sh` | script-quality (shellcheck job) |

---

## 6. Agent Spawn Pattern

Per `feedback_parallel_agent_strategy.md` + `agent-background-spawn-default.md`:
- All 3 buckets spawned with `run_in_background: true`
- Worktree isolation (`isolation: worktree`)
- RELATIVE paths in agent prompts per `feedback_worktree_absolute_path_contamination.md`
- Coordinator merges sequentially after completions: A first (helm — smallest blast radius) → B (pom.xml — broad Java impact) → C (docs/scripts — last)
- All disjoint file globs → no rebase expected; sequential merge is just safety order, not technical requirement

---

## 7. Closure Protocol

Per `gap-done-discipline.md` + `feedback_post_merge_doc_sync.md` + `feedback_wave_history_append_required.md` + `post-wave-cleanup.md` + `feedback_wave_closure_release_progress_report.md`:
- Each bucket PR updates affected GAP file Log + status
- ROADMAP §🚀 Next Action update — flip step 2.5 from 🟡 PARTIAL to ✅ DONE if GAP-466 + GAP-469 both close
- Wave plan frontmatter `status: complete` flip in closure PR
- `wave-history.jsonl` Rule 15 append in closure PR
- Sub-gaps filed for any deferral per `gap-done-discipline.md` §3 PARTIAL exit-ramp
- Run `bash scripts/prune-merged-worktrees.sh --yes` after all 3 bucket PRs merged
- **`## Release Plan Progress` section in closure PR body** — repo-level RED→GREEN celebration + step 2.5 status update + Waves Remaining table refresh

---

## 8. Log

- **2026-05-11** (draft): Plan created. State-check §4 verifies 3 🆕 to-be-created + 4 ✅ existing symbols. 5 risks documented with mitigations. All 3 buckets disjoint file globs. Awaiting plan PR review/merge before 3-agent spawn.
