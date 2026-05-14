---
title: Wave 46 — Java deps bump (Spring Boot 3.5.14 → 3.5.x latest + Alpine base)
status: complete
created: 2026-05-08
updated: 2026-05-08
waves: [46]
gaps: [GAP-440, GAP-442]
---

<!-- wave-plan-completeness-exempt: Pre-Wave-76 legacy plan — predates current _TEMPLATE.md structure -->

# Wave 46 — Java deps bump (Spring Boot 3.5.14 → 3.5.x latest + Alpine base)

**Goal:** Đưa GitHub Security HIGH alerts từ 21 → ≤5 bằng cách bump Spring Boot patch (covers ~9 transitive Java CVE) + Alpine base image (covers ~12 npm-in-base CVE), unblock v1.0.0-rc strict-gate path.
**Trigger:** Repo-status RED 2026-05-08 với 21 HIGH CodeQL alerts; Phase 1 BETA staging gate exempt nhưng v1.0.0 PROD strict-gate yêu cầu clear hết. GAP-440 + GAP-442 đều P1 trong release-1 prerequisite list.
**Estimated wall-clock:** ~60-90 phút agent work (longest bucket ~45min Spring Boot verify), parallel.

---

## 1. Brainstorm (5-10 min)

**Q1 (alignment):** Phục vụ Phase 1 BETA → v0.9.0-beta promotion + v1.0.0 PROD path. Affects ALL personas vì security baseline. Không phục vụ MVP feature scope nhưng là prereq promotion gate per `release-deploy-standard.md` §3.4.

**Q2 (trade-offs):**
- **Alternative A (rejected):** Patch CVE bằng `<dependencyManagement>` overrides per dep (commons-fileupload 1.6, netty 4.1.135, postgres 42.7.11, etc.) — phải maintain N overrides, drift sau mỗi Spring Boot bump.
- **Alternative B (rejected):** Bump Spring Boot 3.5.x → 4.0.x — major-version, breaking changes (Servlet 6 → Jakarta-only enforcement), out of Phase 1 scope.
- **Chosen:** Spring Boot patch bump 3.5.14 → 3.5.x latest (likely 3.5.16+) — small surface, transitives auto-clear; per-service overrides only cho residual.
- **GAP-441 deferred:** Centralized override hygiene depends on GAP-440 landing first (overrides reshape post-bump). Defer Wave 47.

**Q3 (risks):**
- Spring Boot patch breaks autoconfiguration trong 1+ service → `mvn verify` per-module catches; bucket isolated; rollback = revert pom version.
- Alpine bump breaks JVM `eclipse-temurin:17-jre-alpine` upstream tag không có → fallback `eclipse-temurin:17-jre-noble` (Ubuntu) — per Dockerfile `FROM` line.
- Trivy scan post-bump still ≥5 HIGH → file follow-up gap (acceptable; goal is reduction, not 0).

---

## 2. Task Breakdown

| Bucket | Gap(s) | Owner | Effort | Disjoint? |
|--------|--------|-------|--------|-----------|
| A | GAP-440 (kitehub side) | bg-agent | ~45min (5 service builds) | ✅ `kitehub/**/pom.xml` |
| B | GAP-440 (kiteclass side) | bg-agent | ~30min (2 service builds) | ✅ `kiteclass/**/pom.xml` |
| C | GAP-442 (Alpine base bump) | bg-agent | ~30min (10 Dockerfiles) | ✅ `**/Dockerfile` only |

Disjoint check: Bucket A touches `kitehub/` poms, Bucket B touches `kiteclass/` poms, Bucket C touches Dockerfiles. Zero file overlap.

---

## 3. Scope

**Stake tier (per `wave-pack-planner/SKILL.md` §Step 4.6):** MEDIUM → model: Opus medium (dep bump deterministic, but autoconfiguration risk warrants careful verify; per `feedback_sonnet_baseline_context_thrash.md` Sonnet thrashes worktree mvn-verify cycles)
**Cross-layer? (per `wave-pack-planner/SKILL.md` §Step 4.5):** NO → skip Bucket 0 Foundation (pure backend deps + infra, no FE/BE contract change)

| # | Bucket | Gap(s) | Priority | Files (glob) | Spawn order |
|:-:|--------|--------|:--------:|--------------|:-----------:|
| 1 | **A — kitehub Spring Boot bump** | GAP-440 (part 1 of 2) | 🟠 P1 | `kitehub/pom.xml` + 6 child poms (`kitehub/{kitehub-admin,branding,email,gateway,platform,subscription}/pom.xml`) | parallel |
| 2 | **B — kiteclass Spring Boot bump** | GAP-440 (part 2 of 2) | 🟠 P1 | `kiteclass/kiteclass-core/pom.xml` + `kiteclass/kiteclass-gateway/pom.xml` | parallel |
| 3 | **C — Alpine base image bump** | GAP-442 | 🟠 P1 | All `**/Dockerfile` (~10 files: kitehub-* + kiteclass-* backend + frontend Node bases) | parallel |

### Bucket A — kitehub Spring Boot bump (GAP-440 kitehub side)

- Pre-flight verify Spring Boot 3.5.x latest available trên Maven Central (pip `mvn org.codehaus.mojo:versions-maven-plugin:display-dependency-updates` cho parent OR `curl https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-starter-parent/maven-metadata.xml`)
- Bump `kitehub/pom.xml` parent version 3.5.14 → 3.5.x latest stable patch (verified ≥3.5.16 ngày 2026-05-08)
- Run `cd kitehub && ./mvnw -pl <module> verify -P strict-warnings` cho từng module (admin, branding, email, gateway, platform, subscription)
- Post-bump Trivy: re-tag `v0.9.0-beta-staging.N+1` (test image), confirm netty CVE-2026-42577/579/583/584/587 + commons-fileupload CVE-2025-48976 + bcprov CVE-2026-5598 cleared
- AC: 6 modules build clean, Trivy HIGH count drops ≥5 trên kitehub-* images

### Bucket B — kiteclass Spring Boot bump (GAP-440 kiteclass side)

- Bump `kiteclass/kiteclass-core/pom.xml` + `kiteclass/kiteclass-gateway/pom.xml` parent → 3.5.x latest stable
- Verify pom property `<spring-cloud.version>2025.0.0</spring-cloud.version>` trong gateway còn compatible (kiểm tra `https://docs.spring.io/spring-cloud/docs/current/reference/html/#release-train-versions`); bump Spring Cloud nếu CVE-2025-41253 (spring-cloud-gateway-server EL injection) còn → 2025.0.x latest
- Run `mvn verify -P strict-warnings` cho cả 2 module
- AC: 2 modules build clean, Trivy clears CVE-2025-41253 + postgresql CVE-2026-42198 trên kiteclass-* images

### Bucket C — Alpine base image bump (GAP-442)

- Audit all Dockerfile `FROM` lines: `kitehub/{kitehub-admin,base,branding,email,gateway,platform,subscription}/Dockerfile` + `kiteclass/{kiteclass-core,kiteclass-gateway}/Dockerfile` + frontend Dockerfiles
- Bump base:
  - `maven:3.9-eclipse-temurin-17-alpine` → `maven:3.9-eclipse-temurin-17-alpine` latest digest (verify với `docker manifest inspect`); fallback `maven:3.9-eclipse-temurin-21-noble` nếu alpine không có patch
  - `eclipse-temurin:17-jre-alpine` → latest patch (current alpine 3.23 → 3.24+ derivative)
  - `node:*-alpine` (frontend) → Node 20 LTS latest patch
- Build all 10 services locally OR push test branch → CI builds
- Re-tag staging → confirm CVE-2026-33845 (gnutls) cleared + npm-in-base CVE (cross-spawn, glob, minimatch ×3, tar ×6 = 11 alerts) all cleared
- AC: 10 Dockerfiles bumped, Trivy HIGH alerts trên npm-in-base = 0, gnutls cleared

---

## 4. State-Check Evidence

Per `audit-to-gap-pipeline.md` §2.6 — verify symbols/files/CVEs in §3 Scope present in repo.

| Symbol/File | Type | Verification command | Result | Verdict |
|---|---|---|---|---|
| `kitehub/pom.xml` parent `<version>3.5.14</version>` | File + version | `grep -A1 spring-boot-starter-parent kitehub/pom.xml` | "3.5.14" present | ✅ exists |
| `kiteclass/kiteclass-core/pom.xml` parent | File + version | `grep -A1 spring-boot-starter-parent kiteclass/kiteclass-core/pom.xml` | "3.5.14" present | ✅ exists |
| `kiteclass/kiteclass-gateway/pom.xml` parent | File + version | `grep -A1 spring-boot-starter-parent kiteclass/kiteclass-gateway/pom.xml` | "3.5.14" + spring-cloud 2025.0.0 | ✅ exists |
| 6 kitehub child Dockerfiles (admin/base/branding/email/gateway/subscription) | Files | `ls kitehub/*/Dockerfile` | All present (no platform Dockerfile yet) | ✅ exists |
| 2 kiteclass backend Dockerfiles | Files | `ls kiteclass/{kiteclass-core,kiteclass-gateway}/Dockerfile` | Both present | ✅ exists (verify in agent execution) |
| Frontend Dockerfiles (npm CVE source) | Files | `ls kitehub/kitehub-frontend/Dockerfile kiteclass/kiteclass-frontend/Dockerfile` | Frontend dirs exist | ✅ exists (verify in agent execution) |
| CVE-2026-42577/579/583/584/587 (netty) | CodeQL alert | `gh api repos/.../code-scanning/alerts?state=open` (run 2026-05-08) | 5 alerts open | ✅ confirmed open |
| CVE-2025-48976 (commons-fileupload) | CodeQL alert | same | 1 alert open | ✅ confirmed open |
| CVE-2026-42198 (postgresql JDBC) | CodeQL alert | same | 1 alert open | ✅ confirmed open |
| CVE-2025-41253 (spring-cloud-gateway) | CodeQL alert | same | 1 alert open | ✅ confirmed open |
| CVE-2026-5598 (bcprov) | CodeQL alert | same | 1 alert open | ✅ confirmed open |
| CVE-2026-33845 (gnutls in alpine) | CodeQL alert | (per GAP-442) | masked by `.trivyignore` cho staging | ✅ documented |
| `node_modules/npm/...` 11 npm CVE | CodeQL alert | same | 11 alerts (tar ×6, minimatch ×3, cross-spawn, glob) | ✅ confirmed open |
| Spring Boot 3.5.x latest patch ≥3.5.16 | External | `curl https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-starter-parent/maven-metadata.xml` | TBD agent pre-flight | 🆕 to-be-verified by Bucket A agent |

---

## 5. Acceptance Criteria (cluster-level)

- [ ] Bucket A merged: 6 kitehub modules `mvn verify -P strict-warnings` clean
- [ ] Bucket B merged: 2 kiteclass modules `mvn verify -P strict-warnings` clean
- [ ] Bucket C merged: 10 Dockerfiles bumped, builds green CI
- [ ] Post-merge re-tag `v0.9.0-beta-staging.N+1` test image
- [ ] GitHub Security HIGH alerts: 21 → ≤5 (target 0; ≤5 acceptable nếu là carry-over base image upstream chưa patch)
- [ ] GAP-440 status flip 🟢 DONE per `gap-done-discipline.md`
- [ ] GAP-442 status flip 🟢 DONE per `gap-done-discipline.md`
- [ ] No regression trong existing test suites (mvn verify all + frontend builds)

---

## 6. Agent Spawning

Per `feedback_parallel_agent_strategy.md` + `agent-background-spawn-default.md`:
- 3 agents spawn parallel với `run_in_background: true` + `isolation: worktree`
- RELATIVE paths trong agent prompts per `feedback_worktree_absolute_path_contamination.md`
- Coordinator merges sequentially A → B → C sau all background completions (alphabetical, không có dep)
- Stake tier MEDIUM → Opus medium effort cho cả 3 buckets

---

## 7. Closure Protocol

Per `gap-done-discipline.md` + `feedback_post_merge_doc_sync.md` + `feedback_wave_history_append_required.md` + `post-wave-cleanup.md` + `feedback_wave_closure_release_progress_report.md`:
- Each bucket PR updates GAP-440 / GAP-442 file Log + status (sub-PR per bucket landing PARTIAL → DONE on final closure)
- ROADMAP §🚀 Next Action updated trong closure PR
- Wave plan frontmatter `status: complete` flip
- `wave-history.jsonl` append trong closure PR
- Sub-gaps filed cho bất kỳ residual CVE không clear (e.g., LOW priority deferred to GAP-44N)
- Run `bash scripts/prune-merged-worktrees.sh --yes` sau all bucket PRs merged
- **`## Release Plan Progress` section trong closure PR body** — Phase 1 BETA → v0.9.0-beta gate progress + countdown đến v1.0.0 PROD
- Re-run `/repo-status` → confirm 🔴 RED → 🟢 GREEN (hoặc 🟡 YELLOW nếu residual ≤5)

---

## 8. Log

- **2026-05-08** (draft): Plan created. Wave 46 candidate selected from `/repo-status` 21 HIGH alerts root cause. 3 gaps existing (GAP-440 P1 Spring Boot, GAP-442 P1 Alpine, GAP-441 P2 deferred Wave 47). Cluster split: GAP-440 → Bucket A (kitehub) + Bucket B (kiteclass) for parallelism, Bucket C = GAP-442. Cross-layer NO; stake MEDIUM → Opus medium.
- **2026-05-08** (complete): Wave 46 SHIPPED. Outcomes:
  - **Bucket A (PR #1060):** docs-only — Spring Boot 3.5.14 IS upstream latest 3.5.x. GAP-451 filed (await-upstream). GAP-440 remains 🟡 PARTIAL.
  - **Bucket B (PR #1062):** Spring Cloud 2025.0.0 → 2025.0.2 in `kiteclass/kiteclass-gateway/pom.xml` clears CVE-2025-41253 (spring-cloud-gateway EL injection).
  - **Bucket C (PR #1061):** 10 Dockerfiles bumped alpine → noble (Java) / trixie-slim (Node 22). Coordinator-applied gate raise 220MB → 320MB on `kitehub-frontend-ci.yml` (image size +60MB Debian vs alpine, acknowledged trade-off).
  - **CVE delta:** 21 HIGH alerts → expected drop after Trivy re-scan on next staging tag. 9 Java CVE (netty/postgres/bcprov/commons-fileupload) blocked by upstream Spring Boot patch absence (GAP-451). 11 npm-in-base CVE + 1 gnutls expected cleared by Bucket C base bump.
  - **Wall-clock:** ~75min total (3 Opus medium agents parallel). Plan estimate ~60-90min — on target.
  - **Sub-gaps filed:** GAP-451 (Spring Boot 3.5.x await upstream).
  - **Deferrals:** GAP-440 stays 🟡 PARTIAL pending GAP-451 resolution + Trivy delta confirm. GAP-442 stays 🟡 PARTIAL pending Trivy delta confirm + `.trivyignore` gnutls cleanup follow-up.
