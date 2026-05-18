---
title: Wave 64 — Cleanup cluster (Dependabot HIGH + GAP-476 + GAP-465)
status: complete
created: 2026-05-11
updated: 2026-05-11
waves: [64]
gaps: [GAP-204, GAP-476, GAP-465, GAP-475]
---

<!-- wave-plan-completeness-exempt: Pre-Wave-76 legacy plan — predates current _TEMPLATE.md structure -->

# Wave 64 — Cleanup cluster

**Goal:** Flip repo RED→GREEN (Dependabot HIGH cleanup) + close GAP-476 (unblock GAP-475 Sub-5 DONE) + ship GAP-465 helm staleness audit. Bundle 3 cleanup gaps into 1 wave.
**Trigger:** Post-Wave 63 ROADMAP §🚀 Wave 64 candidates B+C+E+D consolidated. E folded into closure PR (GAP-475 status assessment after C completes).
**Estimated wall-clock:** ~1h serial → 3 buckets parallel ~1h longest bucket.

---

## 1. Brainstorm (5-10 min)

**Q1:** All 3 buckets serve Phase 1 BETA readiness — B (security baseline), C (smoke completeness Sub-5), D (helm staleness audit for Phase 2 K8s migration prep).

**Q2:**
- Considered: defer GAP-465 helm to Phase 2 (rejected — audit cheap, blocks Phase 2 wave prep)
- Considered: combine GAP-476 endpoint + script wire-up + Sub-5 DONE into 1 bucket (accepted — atomic concern)
- Considered: 5 separate Dependabot PRs (rejected — all `next` package, single bump suffices)

**Q3:**
- Next.js bump breakage in FE tests → run `pnpm test --run` + `pnpm build` both frontends
- GAP-476 Spring Actuator Flyway endpoint security: must be admin-auth gated (Wave 60 Bucket A OWASP audit precedent)
- GAP-465 audit may surface 2nd-order gaps (helm version drift) — file follow-ups not fix-in-place

---

## 2. Task Breakdown

| Bucket | Scope | Owner | Effort | Disjoint? |
|--------|-------|-------|--------|-----------|
| B | Next.js HIGH bump cross 2 frontends | bg-agent | ~30min | ✅ `package.json` + `pnpm-lock.yaml` × 2 |
| C | GAP-476 Flyway endpoint + smoke-test wire-up | bg-agent | ~1h | ✅ backend Java + `scripts/smoke-test.sh` |
| D | GAP-465 helm staleness audit | bg-agent | ~1h | ✅ `infrastructure/helm/` + audit report |

Disjoint: B=FE deps, C=BE+script, D=helm+audit. Zero overlap.

---

## 3. Scope

**Stake tier:** MEDIUM (security baseline + production endpoint + infra audit) → model: **Opus medium**
**Cross-layer?:** NO → skip Bucket 0

| # | Bucket | Gap(s) | Priority | Files |
|:-:|--------|--------|:--------:|-------|
| 1 | **B** | GAP-204 (Dependabot HIGH) | 🟠 P1 | `kitehub/kitehub-frontend/{package.json,pnpm-lock.yaml}` + `kiteclass/kiteclass-frontend/{package.json,pnpm-lock.yaml}` + root `pnpm-lock.yaml` |
| 2 | **C** | GAP-476 + GAP-475 Sub-5 | 🟡 P2 | Backend Java (Spring Actuator config OR custom MigrationStatusController) + `scripts/smoke-test.sh` |
| 3 | **D** | GAP-465 helm staleness | 🟡 P2 | `infrastructure/helm/**` (read-only audit) + `documents/04-quality/audits/helm-k8s/2026-05-11-staleness-audit.md` (new) |

### Bucket B — Dependabot HIGH Next.js bump

- 5 alerts all `next` package; single version bump fixes all
- Determine target version: `gh api repos/.../dependabot/alerts?state=open --jq '.[] | .security_advisory.cvss.vector_string'` + check Next.js release notes for fix version
- `pnpm update next@latest --filter kitehub-frontend --filter kiteclass-frontend` OR explicit version pin
- Test: `pnpm -F kitehub-frontend test --run && pnpm -F kitehub-frontend build` + same for kiteclass-frontend
- Acceptance: 5 HIGH alerts auto-resolve; both FE builds green

### Bucket C — GAP-476 Flyway endpoint + smoke-test wire-up

- Choose Option A (Spring Actuator Flyway expose) per GAP-476 §Proposed Fix — simpler
- Add to `kiteclass-core/src/main/resources/application.yml`: `management.endpoints.web.exposure.include: health,info,flyway` (verify existing exposure list first)
- Add admin auth filter for `/actuator/flyway` — gateway-level filter OR Spring Security config (production must not be public)
- Update `scripts/smoke-test.sh` `check_migration_head` function: remove graceful SKIP path; assert real endpoint response + version match
- Acceptance: endpoint returns 401 without admin token, 200 with; smoke function flips from SKIP to actual check; GAP-476 DONE; GAP-475 Sub-5 unblocked

### Bucket D — GAP-465 helm staleness audit

- Read-only audit: enumerate every chart in `infrastructure/helm/` → check (a) appVersion vs latest upstream Docker image tag, (b) Helm chart version vs latest minor, (c) values.yaml fields referencing deprecated APIs
- Audit report `documents/04-quality/audits/helm-k8s/2026-05-11-staleness-audit.md` with:
  - Per-chart table (chart name, current ver, latest ver, drift, severity)
  - Recommended bump priority (P0/P1/P2 per chart)
  - 2nd-order gaps to file (1 per chart if material drift)
- Acceptance: audit report shipped; if any HIGH severity drift → follow-up gap filed; GAP-465 DONE

---

## 4. State-Check Evidence

| Symbol | Type | Verification | Verdict |
|--------|------|--------------|---------|
| `next` package in FE | dep | `grep -l next kitehub/kitehub-frontend/package.json kiteclass/kiteclass-frontend/package.json` | ✅ exists |
| 5 HIGH alerts confirmed | Dependabot | `gh api repos/.../dependabot/alerts?state=open --jq '[.[] \| select(.security_vulnerability.severity == "high")] \| length'` | ✅ = 5 (verified) |
| `management.endpoints.web.exposure` | Config | `grep -rn "management.endpoints.web.exposure" kiteclass-core/src/main/resources/` | TBD verify in Bucket C |
| `application.yml` admin profile | Config | `ls kiteclass-core/src/main/resources/application*.yml` | TBD verify in Bucket C |
| `infrastructure/helm/` | Dir | `ls infrastructure/helm/` | ✅ exists |
| `scripts/smoke-test.sh` `check_migration_head` | Function | `grep -n "check_migration_head" scripts/smoke-test.sh` | ✅ exists (Wave 62) |

---

## 5. Verification Gates

| Bucket | Local verify |
|--------|--------------|
| B | `pnpm -F kitehub-frontend test --run && pnpm -F kitehub-frontend build && pnpm -F kiteclass-frontend test --run && pnpm -F kiteclass-frontend build` |
| C | `cd kiteclass/kiteclass-core && ./mvnw verify -Dtest=*FlywayConfig*` + `bash -n scripts/smoke-test.sh && shellcheck scripts/smoke-test.sh` |
| D | Markdown lint on audit report; no code change |

---

## 6. Agent Spawn

3 Opus-medium bg agents, worktree-isolated, RELATIVE paths. Model justification: MEDIUM stake (security baseline routine + endpoint expose pattern well-known + read-only audit).

---

## 7. Closure Protocol

- GAP-204 DONE flip (5 Dependabot alerts resolved); GAP-476 DONE; GAP-465 DONE; **GAP-475 Sub-5 DONE + completion bump to 90%** (5/6 sub functional; Sub-6 still gated user-action)
- ROADMAP §🚀 Next Action updated
- Wave plan `status: complete`
- `wave-history.jsonl` append
- Worktree prune
- `## Release Plan Progress` — Wave 64 closes Phase 1 BETA cleanup; only user-action remaining

---

## 8. Log

- **2026-05-11** (draft): Plan created. 3 cleanup gaps bundled. E folded into closure (GAP-475 Sub-5 DONE flip post-Bucket-C). Stake MEDIUM → Opus medium.
- **2026-05-11** (in-progress, session-interrupt + recovery): Bucket B agent died with uncommitted local; Dependabot AUTO PR #1193 (next 15.5.15→15.5.18) covers same scope — MERGED instead. Bucket C agent died early (empty worktree) — re-spawned, PR #1195 shipped. Bucket D agent wrote 179 LOC audit but died before commit — coordinator salvaged → PR #1194 + filed GAP-478 + GAP-479.
- **2026-05-11** (complete): Wave SHIPPED. 3 bucket PRs merged (#1193 Dependabot Bucket B / #1194 Bucket D salvage / #1195 Bucket C re-spawn). Closure: GAP-475 PARTIAL 75%→90%, GAP-465 DONE, GAP-476 DONE, GAP-204 PARTIAL 50%→75%. New gaps GAP-478 P1 + GAP-479 P2 (Phase 2 helm prep). Post-closure handles 26 new Dependabot alerts (per-FE pnpm-lock dupes stale at 15.5.15 despite root 15.5.18) — delete per-FE locks + gitignore (root lock canonical per workspace mode). Both FE builds PASS post-cleanup. Streak: 97→98 consecutive 0-clarification waves (Bucket B Dependabot does not count as agent clarification).
