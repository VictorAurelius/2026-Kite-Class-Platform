---
title: Wave beta-readiness-7 — Document performance cluster (cache + benchmark + alerts + fonts)
status: draft
created: 2026-05-25
updated: 2026-05-25
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
**Estimated wall-clock:** ~6-7h (4 agents — mixed Ops + Java + runbook); ~25h serial → ~3-4x speedup.

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
| A | GAP-215 BrandingService @Cacheable | bg-agent Opus | ~1.5-2h | ✅ kitehub-branding/.../BrandingService + cache config |
| B | GAP-216 JMH micro-benchmark + p95 + Prometheus histogram | bg-agent Opus | ~3-4h | ✅ kitehub-branding/.../benchmark/ + Prometheus instrumentation |
| C | GAP-217 alert rules | bg-agent Sonnet | ~1.5-2h | ✅ infrastructure/prometheus/ + AlertManager config |
| D | GAP-218 font runbook + Dockerfile assertion | bg-agent Sonnet | ~1-1.5h | ✅ Dockerfile + runbook md |
| **E** | **GAP-742 Outbox DLQ alert wiring (audit-1 OPS-BR4-001)** | **bg-agent Sonnet** | **~2h** | **✅ infrastructure/prometheus + CloudWatch alarm config (paired GAP-144)** |
| Closure | 5-target sync + 4 P0 DONE flip + GAP-742 DONE | coordinator inline | ~30-45 min | After 5 buckets |

Disjoint check:
- Bucket A: `kitehub/kitehub-branding/.../BrandingService.java` + cache config in `application*.yml`
- Bucket B: `kitehub/kitehub-branding/src/test/java/.../benchmark/*.java` (new) + `application*.yml` Micrometer config + Prometheus histogram
- Bucket C: `infrastructure/prometheus/*.yml` + AlertManager config + Grafana panel JSON
- Bucket D: `kitehub/kitehub-branding/Dockerfile` + `documents/05-guides/operations/pdf-font-missing-runbook.md`
- 4 disjoint scope; minor read overlap (Bucket A + B đều đọc BrandingService); coordinator merge sequential nếu cần

---

## 3. Scope

**Stake tier:** HIGH → Opus 4.7 cho Bucket A + B (caching + benchmarking critical-path), Sonnet OK cho Bucket C + D (config + runbook).
**Cross-layer?:** NO — pure BE service + infrastructure config scope.

| # | Bucket | Gap(s) | Priority | Files (glob) | Spawn order |
|:-:|--------|--------|:--------:|--------------|:-----------:|
| 1 | **A** | GAP-215 BrandingService @Cacheable | 🟠 P1 | `kitehub/kitehub-branding/.../BrandingService.java` + `application*.yml` cache config + cache invalidation hook | parallel batch 1 |
| 2 | **B** | GAP-216 JMH p95 micro-benchmark + Prometheus | 🟠 P1 | `kitehub/kitehub-branding/src/test/java/.../benchmark/DocumentRenderBenchmark.java` (new) + Micrometer instrumentation | parallel batch 1 |
| 3 | **C** | GAP-217 alert rules + escalation | 🟠 P1 | `infrastructure/prometheus/document-alerts.yml` + AlertManager routes + Grafana panel | parallel batch 1 |
| 4 | **D** | GAP-218 font runbook + Dockerfile assertion | 🟠 P1 | `kitehub/kitehub-branding/Dockerfile` + `documents/05-guides/operations/pdf-font-missing-runbook.md` | parallel batch 1 |
| 5 | **Closure** | 5-target sync + 4 P0 DONE flip | 🟠 P1 | After 4 buckets verify | sequential |

### Bucket A — BrandingService @Cacheable

- Files: `kitehub/kitehub-branding/.../BrandingService.java` `getBranding(tenantId)` → `@Cacheable(value="branding", key="#tenantId")`
- Cache config: `application*.yml` `spring.cache.*` + TTL (e.g., 1h) + tenant-scoped key
- Acceptance: Cache hit rate >90% measured via Micrometer; invalidation hook on `BrandingService.updateBranding(...)`

### Bucket B — JMH micro-benchmark + Prometheus histogram

- Files: `kitehub/kitehub-branding/src/test/java/.../benchmark/DocumentRenderBenchmark.java` (new JMH test)
- Prometheus instrumentation: Micrometer `Timer` cho document render endpoint; histogram buckets p50/p95/p99
- Acceptance: JMH run output p95 baseline; Prometheus scrape verifies histogram metric exposed

### Bucket C — Alert rules + escalation

- Files: `infrastructure/prometheus/document-alerts.yml` (new) + AlertManager routes
- Pattern: p95 SLO breach (e.g., >5s for /api/v1/documents/*) → fire alert → escalate to on-call
- Acceptance: Prometheus rule syntax valid; AlertManager routing valid; staged WARN-mode initially

### Bucket D — Font runbook + Dockerfile assertion

- Files: `kitehub/kitehub-branding/Dockerfile` (add font validation step) + `documents/05-guides/operations/pdf-font-missing-runbook.md`
- Dockerfile assertion: `RUN test -f /usr/share/fonts/.../NotoSans-Regular.ttf` (or equivalent font check)
- Runbook: troubleshooting guide cho font-missing 500 errors
- Acceptance: Docker image build fails if font missing; runbook documents resolution

---

## 4. State-Check Evidence

| Symbol | Type | Verification command | Evidence | Verdict |
|--------|------|----------------------|----------|---------|
| GAP-215 | Gap file | `bash scripts/query-gaps.sh 215` | OPEN P1 | ✅ exists |
| GAP-216 | Gap file | `bash scripts/query-gaps.sh 216` | OPEN P1 | ✅ exists |
| GAP-217 | Gap file | `bash scripts/query-gaps.sh 217` | OPEN P1 PARTIAL | ✅ exists |
| GAP-218 | Gap file | `bash scripts/query-gaps.sh 218` | OPEN P1 | ✅ exists |
| `BrandingService.getBranding` | Java method | `grep -rn "getBranding" kitehub/kitehub-branding/src/main/java` | (verify pre-spawn) | ✅ expected to exist |
| `kitehub-branding/Dockerfile` | Dockerfile | `ls kitehub/kitehub-branding/Dockerfile` | (verify pre-spawn) | ✅ expected to exist |
| `infrastructure/prometheus/` | Prometheus config dir | `ls -d infrastructure/prometheus/` | (verify pre-spawn) | ✅ expected to exist |
| `DocumentRenderBenchmark.java` | JMH benchmark | (post-spawn) | 🆕 to-be-created (Bucket B) | 🆕 |
| `document-alerts.yml` | Prometheus alerts | (post-spawn) | 🆕 to-be-created (Bucket C) | 🆕 |
| `pdf-font-missing-runbook.md` | Runbook | (post-spawn) | 🆕 to-be-created (Bucket D) | 🆕 |

---

## 5. Verification Gates

| Bucket | Local verify command | CI gate |
|--------|---------------------|---------|
| A | `cd kitehub && ./mvnw -pl kitehub-branding verify -P strict-warnings` + smoke cache hit/miss | kitehub-ci |
| B | `cd kitehub && ./mvnw -pl kitehub-branding test -P perf -Dtest=DocumentRenderBenchmark` + Micrometer scrape verify | kitehub-ci + perf-ci (nếu exist) |
| C | `promtool check rules infrastructure/prometheus/document-alerts.yml` + AlertManager config syntax | None (config-only, no apply) |
| D | `docker build -t kitehub-branding:test kitehub/kitehub-branding/` + runbook markdown render check | kitehub-ci (Docker build) |
| Closure | All 4 verify PASS + smoke document generation E2E | None |

---

## 6. Agent Spawn Pattern

4 agents parallel batch 1 (mixed model):

```
Bucket A: subagent_type=general-purpose, model=opus, isolation=worktree, run_in_background=true
Bucket B: subagent_type=general-purpose, model=opus, isolation=worktree, run_in_background=true
Bucket C: subagent_type=general-purpose, model=sonnet, isolation=worktree, run_in_background=true
Bucket D: subagent_type=general-purpose, model=sonnet, isolation=worktree, run_in_background=true

After 4 verify:
  - Coordinator E2E smoke document render
  - Flip 4 P0 gaps DONE
  - 5-target sync
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

- **2026-05-25 (status: draft):** Wave plan drafted per session handoff §"Wave 5/5". Counter `beta-readiness-7` = next monotonic. Mixed Ops + Performance scope. Outside-in audit SKIP per §4 row 4 (internal scope). 4 buckets parallel; Bucket A+B Opus (perf critical), C+D Sonnet (config + runbook). Author: @nguyenvankiet (solo-dev).
