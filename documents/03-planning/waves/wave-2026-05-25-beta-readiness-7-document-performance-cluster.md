---
title: Wave beta-readiness-7 — Document performance cluster (cache + benchmark + alerts + fonts)
status: draft
created: 2026-05-25
updated: 2026-05-26
wave: 7
tag_primary: beta-readiness
tags_secondary: [performance, ops, gap-215, gap-216, gap-217, gap-218]
counter: 7
date_launch: 2026-05-25
waves: [beta-readiness-7]
gaps: [GAP-215, GAP-216, GAP-217, GAP-218]
---

# Wave beta-readiness-7 — Document performance cluster (4 P0 gap)

**Goal:** Fix 4 P0 gap blocking PDF/XLSX/DOCX generation reliability + ops observability. Branding cache hit rate >90%, p95 SLO met, alerts fire on document endpoint regression, fonts validated at image build.
**Trigger:** Session handoff `2026-05-24-wave-beta-readiness-4-closure.md` §"Wave 5/5" — 4 P0 gap blocking document generation pipeline; Ops + Performance scope mix.
**Estimated wall-clock:** ~4-5h (coordinator inline Bucket A ~30 min + 4 Opus 1M bg-agents B/C/D/E parallel ~3-4h); ~20h serial → ~4-5x speedup. **Revised from 6-7h** sau state-check 2026-05-26 phát hiện Bucket A đã essentially shipped trong code.

---

## 1. Brainstorm

**Q1 (alignment — inside-out 4-bucket):**

- **Inside-out từ session handoff** §"Wave 5/5": 4-bucket scope (A GAP-215 BrandingService @Cacheable + B GAP-216 micro-benchmark + C GAP-217 alert rules + D GAP-218 font runbook + Dockerfile assertion)
- **Inside-out từ queue file:** verify 4 gaps trong Phase 1 BETA scope
- **Inside-out từ audit:** Wave 85 performance audit 86/100 B+ surfaced p95 SLO baseline; this wave executes Ops + Performance gates
- **Outside-in NEW:** SKIP per `outside-in-coverage-trigger.md` §4 row 4 (wave 100% internal scope — ops/performance, không user-facing change)

Persona phục vụ: All tenants generating documents (invoices, certificates, transcripts) + Ops on-call SRE + future-Claude observability decisions. Domain: kitehub-branding + document generation pipeline + Prometheus/Grafana observability.

**Q2 (trade-offs):**

| Rejected option | Reason |
|---|---|
| Combine Bucket A + B (cache + benchmark) | Disjoint scope — cache = Java annotation; benchmark = JMH separate package. Keep parallel |
| Skip JMH micro-benchmark Bucket B | Performance audit requires p95 baseline measurement; without benchmark = no SLO assertion |
| Use Prometheus AlertManager alone (Bucket C) | Per `audit-skill-rubric-ops-readiness-audit.md` §2.4 alert rules MUST include escalation policy; coordinate với existing alerting infra |
| Defer font validation Wave audit-2 | GAP-218 BLOCKING — PDF font-missing causes runtime 500 in document render flow |

**Q3 (risks):**

| Risk | Recovery |
|---|---|
| Bucket A @Cacheable invalidation timing wrong (stale cache) | TTL config + tenant-scoped cache key + invalidation hook on branding update |
| Bucket B JMH micro-benchmark CI overhead | Separate `mvn verify -P perf` profile; not run in standard CI |
| Bucket C alert rules fire false-positive | Verify alert thresholds vs Wave 85 perf baseline; staged WARN-mode first |
| Bucket D Dockerfile assertion adds image build time | Font validation = quick FS check (<5s); acceptable |
| AWS suspended (GAP-612) blocks alert deployment | Alert rules ship as code (Prometheus config); apply blocked → follow-up gap |

---

## 2. Task Breakdown

| Bucket | Gap(s) | Owner | Effort | Disjoint? |
|--------|--------|-------|--------|-----------|
| A | GAP-215 BrandingServiceImpl @Cacheable | **coordinator inline** | **~30 min** | ✅ **SCOPE REDUCED 2026-05-26** — `@Cacheable + @CacheEvict` đã ship trong code; coordinator chỉ verify integration test + flip DONE |
| B | GAP-216 JMH micro-benchmark + p95 + Prometheus histogram | bg-agent Opus 1M | ~3-4h | ✅ kitehub-branding/.../benchmark/ (new) + Micrometer histogram instrumentation |
| C | GAP-217 alert rules extend existing Helm prometheusrule | bg-agent Opus 1M | ~1.5-2h | ✅ `infrastructure/helm/kitehub/templates/prometheusrule.yaml` (existing — extend) + alertmanager-config.yaml |
| D | GAP-218 font runbook + Dockerfile assertion | bg-agent Opus 1M | ~1-1.5h | ✅ kitehub-branding/Dockerfile + runbook md (new) |
| **E** | **GAP-742 Outbox DLQ alert wiring (audit-1 OPS-BR4-001)** | **bg-agent Opus 1M** | **~2h** | **✅ extend `infrastructure/helm/kitehub/templates/prometheusrule.yaml` + alertmanager-config.yaml (paired GAP-144)** |
| Closure | 5-target sync + 4 P0 DONE flip + GAP-742 DONE | coordinator inline | ~30-45 min | After A inline + 4 buckets B/C/D/E |

Disjoint check (verified 2026-05-26 pre-spawn state-check):
- Bucket A: `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/settings/service/BrandingServiceImpl.java` (lines 67-71 `@Cacheable("branding-by-tenant", sync=true)` + lines for updateBranding/uploadLogo/uploadFavicon `@CacheEvict`) — **all 4 annotations ALREADY shipped** in prior wave per state-check
- Bucket B: `kitehub/kitehub-branding/src/test/java/.../benchmark/DocumentRenderBenchmark.java` (new JMH test) + Micrometer `Timer` cho document render endpoint
- Bucket C: `infrastructure/helm/kitehub/templates/prometheusrule.yaml` (existing file — extend với p95 SLO breach rule) + alertmanager-config.yaml (existing — add routing) + Grafana dashboards (existing)
- Bucket D: `kitehub/kitehub-branding/Dockerfile` (existing — add font validation `RUN test -f`) + `documents/05-guides/operations/pdf-font-missing-runbook.md` (new)
- Bucket E: same prometheusrule.yaml as Bucket C (extend separate alert for Outbox DLQ depth) + alertmanager-config.yaml
- **MERGE CONFLICT RISK:** Bucket C + E đều edit `prometheusrule.yaml` + `alertmanager-config.yaml`. Coordinator merge SEQUENTIAL C → E (resolve trailing newline + alphabetical order conflicts). Bucket B + D disjoint.

---

## 3. Scope

**Stake tier:** HIGH → Opus 4.7 1M mandatory cho mọi spawned agent per `agent-model-opus-default.md` v1.0.0. Bucket A inline (coordinator) vì scope nhỏ ~30 min sau state-check.
**Cross-layer?:** NO — pure BE service + infrastructure config scope.

| # | Bucket | Gap(s) | Priority | Files (glob) | Spawn order |
|:-:|--------|--------|:--------:|--------------|:-----------:|
| 1 | **A** | GAP-215 BrandingServiceImpl @Cacheable verify+flip | 🔴 P0 | `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/settings/service/BrandingServiceImpl.java` (verify-only — code already shipped) + verify IT exists | **coordinator inline** |
| 2 | **B** | GAP-216 JMH p95 micro-benchmark + Prometheus | 🔴 P0 | `kitehub/kitehub-branding/src/test/java/.../benchmark/DocumentRenderBenchmark.java` (new) + Micrometer Timer + histogram | parallel batch 1 |
| 3 | **C** | GAP-217 alert rules + escalation | 🔴 P0 | `infrastructure/helm/kitehub/templates/prometheusrule.yaml` (extend existing) + `alertmanager-config.yaml` routes | parallel batch 1 |
| 4 | **D** | GAP-218 font runbook + Dockerfile assertion | 🔴 P0 | `kitehub/kitehub-branding/Dockerfile` + `documents/05-guides/operations/pdf-font-missing-runbook.md` (new) | parallel batch 1 |
| 5 | **E** | GAP-742 Outbox DLQ alert wiring | 🟠 P1 | `infrastructure/helm/kitehub/templates/prometheusrule.yaml` + `alertmanager-config.yaml` (shared with C — merge sequential C→E) | parallel batch 1 |
| 6 | **Closure** | 5-target sync + 4 P0 DONE flip + GAP-742 DONE | 🔴 P0 | After A inline + B/C/D/E verify | sequential |

### Bucket A — BrandingServiceImpl @Cacheable (coordinator inline, scope REDUCED 2026-05-26)

- **State-check 2026-05-26 surface:** `@Cacheable("branding-by-tenant", sync=true, key=tenant)` ALREADY on `BrandingServiceImpl.getBranding()` line 67. `@CacheEvict` ALREADY on `updateBranding/uploadLogo/uploadFavicon`. CacheConfig (Redis) exists. ~80-100% done in code; CSV says OPEN P0 0% — discipline miss per `gap-done-discipline.md`.
- Coordinator inline scope (~30 min):
  1. Verify integration test exists verifying cache hit/evict behavior (per gap AC: `@SpyBean` repository, assert `findByInstanceIdAndDeletedFalse` invoked once per cache window; `updateBranding` evicts)
  2. Nếu IT missing → file follow-up gap để add OR add inline same closure PR
  3. Run `cd kiteclass/kiteclass-core && ./mvnw verify -Dtest=BrandingResourceTest,DocumentBrandingIntegrationTest` PASS
  4. Flip GAP-215 DONE per `gap-done-discipline.md` §2 + git mv → phase-1-beta/closed/

### Bucket B — JMH micro-benchmark + Prometheus histogram

- Files: `kitehub/kitehub-branding/src/test/java/.../benchmark/DocumentRenderBenchmark.java` (new JMH test)
- Prometheus instrumentation: Micrometer `Timer` cho document render endpoint; histogram buckets p50/p95/p99
- Acceptance: JMH run output p95 baseline; Prometheus scrape verifies histogram metric exposed

### Bucket C — Alert rules + escalation (path CORRECTED 2026-05-26)

- Files: `infrastructure/helm/kitehub/templates/prometheusrule.yaml` (EXISTING — extend with new alert) + `infrastructure/helm/kitehub/templates/alertmanager-config.yaml` (EXISTING — add routing)
- **NOTE state-check 2026-05-26:** plan §3 previously cited `infrastructure/prometheus/document-alerts.yml` — that folder doesn't exist. Actual Prometheus + AlertManager config lives under Helm templates. Grafana dashboards also in `infrastructure/helm/kitehub/dashboards/`.
- Pattern: p95 SLO breach (e.g., >5s for /api/v1/documents/*) → fire alert → escalate to on-call routing
- Acceptance: `helm lint infrastructure/helm/kitehub` PASS; AlertManager routing valid; staged WARN-mode initially

### Bucket D — Font runbook + Dockerfile assertion

- Files: `kitehub/kitehub-branding/Dockerfile` (add font validation step) + `documents/05-guides/operations/pdf-font-missing-runbook.md`
- Dockerfile assertion: `RUN test -f /usr/share/fonts/.../NotoSans-Regular.ttf` (or equivalent font check)
- Runbook: troubleshooting guide cho font-missing 500 errors
- Acceptance: Docker image build fails if font missing; runbook documents resolution

---

## 4. State-Check Evidence

| Symbol | Type | Verification command | Evidence | Verdict |
|--------|------|----------------------|----------|---------|
| GAP-215 | Gap file | `grep "^GAP-215," documents/04-quality/gaps/gap-status.csv` | OPEN **P0** Backend (verified 2026-05-26 — CSV P0 not P1) | ✅ exists |
| GAP-216 | Gap file | `grep "^GAP-216," documents/04-quality/gaps/gap-status.csv` | OPEN **P0** Mixed (verified 2026-05-26) | ✅ exists |
| GAP-217 | Gap file | `grep "^GAP-217," documents/04-quality/gaps/gap-status.csv` | OPEN **P0** DevOps (verified 2026-05-26) | ✅ exists |
| GAP-218 | Gap file | `grep "^GAP-218," documents/04-quality/gaps/gap-status.csv` | OPEN **P0** DevOps (verified 2026-05-26) | ✅ exists |
| GAP-742 | Gap file | `grep "^GAP-742," documents/04-quality/gaps/gap-status.csv` | OPEN P1 DevOps (verified 2026-05-26) | ✅ exists |
| **GAP-215 implementation already shipped** | Code state | `grep -A4 "@Cacheable\|@CacheEvict" kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/settings/service/BrandingServiceImpl.java` | 4 annotations match GAP-215 Proposed Fix verbatim (`@Cacheable("branding-by-tenant", sync=true, key=tenant)` + 3 `@CacheEvict` on update/uploadLogo/uploadFavicon) | ✅ **CODE DONE; CSV needs flip** |
| `BrandingServiceImpl.getBranding` | Java method (Bucket A target) | `find kiteclass -name "BrandingServiceImpl.java"` | `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/settings/service/BrandingServiceImpl.java` line 67 | ✅ verified 2026-05-26 (path CORRECTED — was kitehub-branding in old plan) |
| `kitehub/kitehub-branding/Dockerfile` | Dockerfile (Bucket D) | `ls kitehub/kitehub-branding/Dockerfile` | exists | ✅ verified 2026-05-26 |
| `infrastructure/helm/kitehub/templates/prometheusrule.yaml` | Prometheus config (Bucket C+E target — CORRECTED) | `ls infrastructure/helm/kitehub/templates/prometheusrule.yaml` | exists | ✅ verified 2026-05-26 (path CORRECTED — was `infrastructure/prometheus/` which doesn't exist) |
| `infrastructure/helm/kitehub/templates/alertmanager-config.yaml` | AlertManager config (Bucket C+E) | `ls infrastructure/helm/kitehub/templates/alertmanager-config.yaml` | exists | ✅ verified 2026-05-26 |
| `infrastructure/helm/kitehub/dashboards/` | Grafana dashboards | `ls -d infrastructure/helm/kitehub/dashboards/` | exists | ✅ verified 2026-05-26 |
| `CacheConfig` (Redis manager) | Spring cache config | `find kiteclass/kiteclass-core -name "CacheConfig.java"` | `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/common/config/CacheConfig.java` | ✅ verified 2026-05-26 |
| `DocumentBrandingIntegrationTest.java` | Existing IT (Bucket A verify) | `find kiteclass/kiteclass-core/src/test -name "DocumentBrandingIntegrationTest.java"` | exists | ✅ verified 2026-05-26 (coordinator inline checks if cache-hit assertion present) |
| `DocumentRenderBenchmark.java` | JMH benchmark | (post-spawn) | 🆕 to-be-created (Bucket B) | 🆕 |
| `pdf-font-missing-runbook.md` | Runbook | (post-spawn) | 🆕 to-be-created (Bucket D) | 🆕 |

---

## 5. Verification Gates

| Bucket | Local verify command | CI gate |
|--------|---------------------|---------|
| A (inline) | `cd kiteclass/kiteclass-core && ./mvnw verify -Dtest='BrandingResourceTest,DocumentBrandingIntegrationTest'` + verify cache annotation count = 4 (1 @Cacheable + 3 @CacheEvict) | core-ci |
| B | `cd kitehub && ./mvnw -pl kitehub-branding test -Dtest=DocumentRenderBenchmark` + Micrometer scrape verify | kitehub-ci |
| C | `helm lint infrastructure/helm/kitehub` + `helm template infrastructure/helm/kitehub \| yq eval-all .` clean (verify prometheusrule.yaml syntax via helm render) | quality-infra workflow |
| D | `docker build -t kitehub-branding:test kitehub/kitehub-branding/` (fail-fast nếu font check absent) + runbook markdown render check | kitehub-ci |
| E | Same as C — `helm lint` + `helm template` + verify Outbox DLQ alert + AlertManager routing for DLQ class | quality-infra workflow |
| Closure | All 4 buckets PASS + Bucket A inline verify + smoke document generation E2E | None |

---

## 6. Agent Spawn Pattern

4 bg-agents parallel batch 1 (all Opus 1M per `agent-model-opus-default.md` v1.0.0) + 1 coordinator inline (Bucket A scope reduced after state-check):

```
Coordinator inline: Bucket A verify GAP-215 already-shipped code + IT check + flip DONE (~30 min)

Parallel batch 1 (Opus 1M, isolation=worktree, run_in_background=true):
  Bucket B: subagent_type=general-purpose, model=opus, isolation=worktree, run_in_background=true
  Bucket C: subagent_type=general-purpose, model=opus, isolation=worktree, run_in_background=true
  Bucket D: subagent_type=general-purpose, model=opus, isolation=worktree, run_in_background=true
  Bucket E: subagent_type=general-purpose, model=opus, isolation=worktree, run_in_background=true

After 4 verify:
  - Coordinator merge PRs SEQUENTIAL C → E first (shared prometheusrule.yaml + alertmanager-config.yaml — conflict risk HIGH), then B + D parallel-safe
  - Each agent prompt MUST include investigation-first mandate per `release-fix-retry-budget.md` §3.5:
    "Read GAP-XXX full file + empirically verify code state before designing fix.
     Don't trust gap problem statement filed 2026-04-25 verbatim — module structure may have drifted."
  - E2E smoke document render (PDF + XLSX + DOCX)
  - Flip 4 P0 gaps DONE (CSV first per gap-architecture-v2.md, then markdown checkbox)
  - GAP-742 DONE (or PARTIAL if alert routing depends GAP-144 AWS-restore)
  - 5-target sync per post-merge-sync-completeness.md
```

---

## 7. Closure Protocol

1. All 4 buckets SHIPPED + local verify PASS
2. E2E smoke document generation (PDF + XLSX + DOCX render OK)
3. 4 P0 gaps flipped per `gap-done-discipline.md`
4. Cache hit rate baseline measured; p95 SLO documented; alerts fire on test trigger; font validation enforces at image build
5. 5-target sync + handoff
6. Worktree cleanup

---

## 8. Log

- **2026-05-26 (state-check patch):** Coordinator next-session pre-spawn state-check per `audit-to-gap-pipeline.md` §2.8 + `release-fix-retry-budget.md` §3.5 surfaced 5 scope issues:
  - **🟢 Bucket A SCOPE WIN — already implemented:** `BrandingServiceImpl.getBranding()` line 67 already has `@Cacheable("branding-by-tenant", sync=true, key=tenant)` exactly per GAP-215 Proposed Fix. `@CacheEvict` already wired on `updateBranding/uploadLogo/uploadFavicon`. CacheConfig (Redis) exists. ~80-100% complete in code; CSV says OPEN P0 0% — discipline miss per `gap-done-discipline.md`. Bucket A demoted bg-agent → coordinator inline ~30 min.
  - **🚨 Bucket A module path wrong:** Plan said `kitehub/kitehub-branding/.../BrandingService.java`. Actual = `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/settings/service/BrandingServiceImpl.java`. Gap body cited correct path; plan §3 incorrectly inferred from gap title.
  - **🚨 Bucket C+E path wrong:** Plan said `infrastructure/prometheus/document-alerts.yml` — that folder doesn't exist. Actual Prometheus rules + AlertManager config live under `infrastructure/helm/kitehub/templates/prometheusrule.yaml` + `alertmanager-config.yaml` + Grafana dashboards. Scope = extend existing Helm templates, not create new folder.
  - **⚠️ Priority drift P0 → P1:** CSV rows GAP-215/216/217/218 all P0; plan §3 incorrectly showed P1.
  - **⚠️ Model drift Sonnet vs Opus:** Plan §3 + §6 mixed Opus(A+B) + Sonnet(C+D). Per `agent-model-opus-default.md` v1.0.0 recurrence ≥2 waves Sonnet thrash, ALL spawned agents MUST Opus 1M. Synced.
  - **⚠️ §2/§3 inconsistency:** Bucket E (GAP-742 Outbox DLQ) added §2 (5 buckets) but §3 only showed 4. Synced.
  - Counterfactual: spawning without patch → wrong module paths Bucket A + missing Prometheus folder Bucket C+E + 5 agents instead of 4 (Bucket A wasted) → ~1-2h preventable round-trips eliminated.
  - Estimated wall-clock revised 6-7h → 4-5h.
- **2026-05-25 (status: draft):** Wave plan drafted per session handoff §"Wave 5/5". Counter `beta-readiness-7` = next monotonic. Mixed Ops + Performance scope. Outside-in audit SKIP per §4 row 4 (internal scope). 4 buckets parallel; Bucket A+B Opus (perf critical), C+D Sonnet (config + runbook). Author: @nguyenvankiet (solo-dev). **⚠️ Vacated 2026-05-26 state-check** — see top entry for corrections.
