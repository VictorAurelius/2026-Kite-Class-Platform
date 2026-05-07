---
title: Wave 37 — Release-Hardening 5-layer (22 GAP-NEW Phase 1 BETA deploy readiness)
status: complete
created: 2026-05-07
updated: 2026-05-07
waves: [37]
gaps: [GAP-395, GAP-396, GAP-397, GAP-398, GAP-399, GAP-400, GAP-401, GAP-402, GAP-403, GAP-404, GAP-405, GAP-406, GAP-407, GAP-408, GAP-409, GAP-410, GAP-411, GAP-412, GAP-413, GAP-414, GAP-415, GAP-416]
---

# Wave 37 — Release-Hardening 5-layer

**Goal:** Close 22 GAP-NEW phân theo 5 layer cho Phase 1 BETA deploy readiness (v0.9.0-beta) per Architecture B + AWS Activate strategy chốt 2026-05-07.

**Trigger:** Wave 36 SHIPPED ✅ + Phase 1 BETA Quality trigger gate 80/100 ✅ + user-confirmed Architecture B + Ollama defer Phase 2.

**Estimated wall-clock:** ~25h dev parallel, longest-bucket Layer 1 Terraform (~6h). With 5 background agents Opus medium effort → ~60-90 min wall-clock.

**Prerequisite:** Wave 36 SHIPPED + ADR-025 AWS Singapore ACCEPTED + ADR-026 Ollama defer Phase 2 (filed này wave).

---

## 1. Brainstorm

**Q1 (alignment):** Phase 1 BETA invite-only deploy readiness. Architecture B = ~$72/mo Yr1 (AWS Activate $1k cover 13.9 tháng → effective $0). 5 layer governance: infra/release/verify/dev/cost.

**Q2 (trade-offs):**
- **Reject** ship 22 gaps serial trong nhiều wave — 5 layer disjoint, parallel agent saves 70% wall-clock
- **Reject** mix với Phase 1 P0 deploy artifacts cluster GAP-369..380 — context heavy, scope drift
- **Accept** Sonnet cho LOW-stakes (Layer 4 dev resource — config + docs); Opus medium cho 4 layer khác (cross-cutting infra)
- **Accept** ADR-025/026 = strategy fundament; gap files = execution scope

**Q3 (risks):**
- **R1:** Layer 1 Terraform requires AWS account ready — agent code-only, human apply (per GAP-381 Phase 2 BANNED). Wave 37 agent ship code; production apply post-merge by user.
- **R2:** Layer 5 cost gaps (C1..C5 + S1) document-heavy — agent generates artifacts, user submits AWS Activate manually (GAP-412 needs user action).
- **R3:** Layer 2 Docker D1 6 services Dockerfile — risk per-service quirks; mvn vs gradle wrap; Spring Boot Buildpack alternative consideration. Bucket 2 agent must audit existing Dockerfile-shape across modules first.
- **R4:** Cross-layer dependency: Layer 1 (T1 ECR repos) blocks Layer 2 (D1 push images). Coordinator merge Layer 1 → then Layer 2. Other layers independent.

---

## 2. Task Breakdown

| Bucket | Layer | Gap(s) | Owner | Effort | Disjoint? |
|--------|-------|--------|-------|--------|-----------|
| A | 1 — Terraform | GAP-395, 396, 397 | bg-agent (Opus medium) | 6h | ✅ `infrastructure/terraform-aws/` only |
| B | 2 — Docker release | GAP-398, 399, 400, 401, 402 | bg-agent (Opus medium) | 5h | ✅ `.github/workflows/docker-build-push.yml` + 6 KH Dockerfile |
| C | 3 — Deploy+verify | GAP-403, 404, 405, 406 | bg-agent (Opus medium) | 5h | ✅ `.github/workflows/e2e-pre-release.yml` + Playwright specs |
| D | 4 — Local dev resource | GAP-407, 408, 409, 410 | bg-agent (Sonnet) | 3h | ✅ `docker-compose.kitehub.yml` + `documents/05-guides/dev/*` |
| E | 5 — AWS cost + Free Tier + Strategy | GAP-411, 412, 413, 414, 415, 416 | bg-agent (Opus medium) | 5h | ✅ `documents/02-architecture/adr/` + `documents/05-guides/deploy/*` + sizing matrix |

Disjoint check:
- A=`infrastructure/terraform-aws/`
- B=`.github/workflows/docker-build-push.yml` + `kitehub/{6 modules}/Dockerfile`
- C=`.github/workflows/e2e-pre-release.yml` + `kitehub-frontend/e2e/`, `kiteclass-frontend/e2e/`
- D=`docker-compose.kitehub.yml` + `documents/05-guides/dev/`
- E=`documents/02-architecture/adr/ADR-026*.md` + `documents/05-guides/deploy/`

No file collision between buckets.

**Cross-layer wave?** NO — pure infra/devops, không touch FE+BE production code. Skip Bucket 0 Foundation per `contract-first-for-cross-layer.md` §2.

---

## 3. Scope

**Stake tier:** MEDIUM-HIGH (Phase 1 BETA deploy readiness; T1+T2+D1+D2+V1+C1+C2+S1 = P0) → model: **Opus medium** cho 4 buckets, **Sonnet** cho UI/dev resource bucket.

| # | Bucket | Layer | Priority | Files | Spawn order |
|:-:|--------|-------|:--------:|-------|:-----------:|
| 1 | A | Terraform | 🔴 P0 + 🟠 P1 | `infrastructure/terraform-aws/{main,rds,ec2,ecr,s3,secrets,iam,security-groups}.tf` + `bootstrap/main.tf` + `.github/workflows/terraform-plan.yml` | parallel (Layer 1) |
| 2 | B | Docker release | 🔴 P0 + 🟠 P1 + 🟡 P2 | `kitehub/{subscription,branding,email,admin,gateway,platform}/Dockerfile` + `.github/workflows/docker-build-push.yml` + Trivy/Cosign/Syft steps | parallel (Layer 2) |
| 3 | C | Deploy+verify | 🔴 P0 + 🟠 P1 + 🟡 P2 | `.github/workflows/e2e-pre-release.yml` + `kitehub-frontend/e2e/beta-funnel/*.spec.ts` (3 NEW) + `playwright.config.ts` + `documents/05-guides/security/owasp-top-10-baseline.md` | parallel (Layer 3) |
| 4 | D | Local dev resource | 🟠 P1 + 🟡 P2 | `docker-compose.kitehub.yml` (profiles) + `documents/05-guides/dev/{ollama-stop-policy,wsl2-config}.md` + `.wslconfig.example` | parallel (Layer 4) |
| 5 | E | AWS cost + Strategy | 🔴 P0 + 🟠 P1 + 🟡 P2 | `documents/02-architecture/adr/ADR-026-ollama-defer-phase-2.md` (NEW) + `documents/05-guides/deploy/{aws-architecture-sizing-matrix,aws-cost-monitoring,aws-activate-credit-policy}.md` + `documents/03-planning/roadmap/phase-2-eks-migration.md` (NEW) + `documents/00-brd/kite-pitch-deck.md` (1-page draft) | parallel (Layer 5) |

### Bucket A — Layer 1 Terraform (GAP-395, 396, 397)

3 sub-issues:
- **395 Production stack:** VPC + 2 EC2 + RDS + ECR + S3 + ALB + Route53 + Secrets Manager + IAM + SG (Architecture B)
- **396 State backend:** S3 + DynamoDB lock + bootstrap script
- **397 Plan CI:** `.github/workflows/terraform-plan.yml` + OIDC role + PR comment plan output

Files (RELATIVE):
- `infrastructure/terraform-aws/{main,vpc,rds,ec2,ecr,s3,secrets,iam,security-groups,backend}.tf`
- `infrastructure/terraform-aws/bootstrap/main.tf` (S3 + DynamoDB lock table)
- `infrastructure/terraform-aws/terraform.tfvars.example`
- `.github/workflows/terraform-plan.yml`

KHÔNG `terraform apply` (per GAP-381 Phase 2 BANNED for agent).

### Bucket B — Layer 2 Docker release (GAP-398, 399, 400, 401, 402)

5 sub-issues:
- **398 Build 6 KH services:** Audit existing Dockerfile per module + add missing + multi-stage + non-root
- **399 ECR region pin:** `us-east-1` → `ap-southeast-1` + ECR repo names `kite/<service>` convention
- **400 Trivy scan:** Step post-build, fail HIGH/CRITICAL, SARIF upload
- **401 Multi-arch:** Buildx amd64 + arm64
- **402 SBOM + Cosign:** Syft CycloneDX + Cosign keyless signing

Files (RELATIVE):
- `kitehub/kitehub-{subscription,branding,email,admin,gateway,platform}/Dockerfile` (audit + add missing)
- `.github/workflows/docker-build-push.yml` (extend matrix + add scan/multi-arch/sign steps)

### Bucket C — Layer 3 Deploy+verify (GAP-403, 404, 405, 406)

4 sub-issues:
- **403 E2E pre-release gate:** New workflow trigger `tags: v*.*.*-rc*` + Playwright run staging + trace upload
- **404 Beta funnel E2E coverage:** 3 specs (request-flow / admin-approve / signup-with-claim-code) + `kitehub-frontend/e2e/` setup
- **405 Visual regression baseline:** `expect.toHaveScreenshot()` cho 8-12 screens
- **406 Pen-test light:** OWASP ZAP baseline workflow + manual checklist + headers verify

Files (RELATIVE):
- `.github/workflows/e2e-pre-release.yml`
- `.github/workflows/zap-baseline.yml`
- `kitehub-frontend/playwright.config.ts` (NEW)
- `kitehub-frontend/e2e/beta-funnel/{request-flow,admin-approve,signup-with-claim-code}.spec.ts` (3 NEW)
- `documents/05-guides/security/owasp-top-10-baseline.md`

### Bucket D — Layer 4 Local dev resource (GAP-407, 408, 409, 410)

4 sub-issues:
- **407 Compose profiles:** infra-only / branding-only-no-ai / branding-only / beta-funnel / kc-only / full
- **408 JVM heap cap dev:** `-Xmx512m` cho 8 services trong dev profile
- **409 Ollama policy:** Document start/stop policy + cloud API fallback config
- **410 WSL2 .wslconfig:** Template + 4-row trade-off matrix

Files (RELATIVE):
- `kitehub/docker-compose.kitehub.yml` (profiles + heap cap)
- `documents/05-guides/dev/{ollama-stop-policy,wsl2-config}.md`
- `.wslconfig.example`
- `kitehub/scripts/up.sh` (wrapper accept profile arg)

### Bucket E — Layer 5 AWS cost + Strategy (GAP-411, 412, 413, 414, 415, 416)

6 sub-issues (Layer 5 = highest gap density):
- **411 Sizing matrix:** `aws-architecture-sizing-matrix.md` Phase 1→2 progression + cost projection 3-year
- **412 AWS Activate application:** Pitch deck draft + submit Founders Pack + credit policy doc
- **413 AWS Budgets:** 3 alarms (monthly $80 / credit <20% / per-service tag) + ECR lifecycle
- **414 Right-sizing review:** Monthly cron + first report template
- **415 Phase 2 EKS migration plan:** Trigger gate + cutover strategy + rollback runbook
- **416 Ollama defer Phase 2 ADR:** ADR-026 + cross-impact GAP-006/225/228

Files (RELATIVE):
- `documents/02-architecture/adr/ADR-026-ollama-defer-phase-2.md` (NEW)
- `documents/05-guides/deploy/{aws-architecture-sizing-matrix,aws-cost-monitoring,aws-activate-credit-policy}.md` (3 NEW)
- `documents/03-planning/roadmap/phase-2-eks-migration.md` (NEW)
- `documents/04-quality/cost-reports/2026-06-template.md` (NEW)
- `documents/00-brd/kite-pitch-deck.md` (1-page draft NEW)

---

## 4. State-Check Evidence

Per `audit-to-gap-pipeline.md` §2.6 — verify symbols referenced trong §3 Scope:

| Symbol | Type | Verification command | Verdict |
|--------|------|----------------------|---------|
| `infrastructure/terraform-aws/` | Folder | `ls infrastructure/terraform-aws/` | ✅ exists (scaffold) |
| `.github/workflows/docker-build-push.yml` | Workflow | `ls .github/workflows/docker-build-push.yml` | ✅ exists |
| `kitehub/kitehub-subscription/Dockerfile` | Dockerfile | `find kitehub/kitehub-subscription -name Dockerfile` | ⚠️ verify-at-spawn (may exist or NEW) |
| `kitehub-frontend/e2e/` | E2E folder | `ls kitehub-frontend/e2e/ 2>&1` | 🆕 to-be-created (Bucket C) |
| `kiteclass-frontend/e2e/critical-journeys/` | Existing E2E specs | `ls kiteclass-frontend/e2e/critical-journeys/` | ✅ exists 3 specs |
| `kitehub/docker-compose.kitehub.yml` | Compose file | `ls kitehub/docker-compose.kitehub.yml` | ✅ exists |
| `documents/02-architecture/adr/` | ADR folder | `ls documents/02-architecture/adr/` | ✅ exists |
| `ADR-025` | Reference ADR | `ls documents/02-architecture/adr/ADR-025*.md` | ✅ exists (AWS Singapore) |
| `ADR-026-ollama-defer-phase-2.md` | NEW ADR | — | 🆕 to-be-created (Bucket E GAP-416) |
| `documents/05-guides/deploy/` | Folder | `ls documents/05-guides/deploy/` | ✅ exists |
| `documents/05-guides/dev/` | Folder | `ls documents/05-guides/dev/` | ⚠️ verify-at-spawn |
| `documents/00-brd/kite-pitch-deck.md` | NEW doc | — | 🆕 to-be-created (Bucket E GAP-412) |
| GAP-381 reference | Rule | `ls documents/04-quality/gaps/GAP-381*.md` | ✅ exists DONE |
| GAP-006 reference | Gap (defer impact) | `ls documents/04-quality/gaps/GAP-006*.md` | ✅ exists |
| `release-deploy-standard.md` | Rule | `ls .claude/rules/release-deploy-standard.md` | ✅ exists v1.0.0 |
| `contract-first-for-cross-layer.md` | Rule | `ls .claude/rules/contract-first-for-cross-layer.md` | ✅ exists v1.0.0 (cross-layer N/A here) |

All forward-looking 🆕 symbols owned by named bucket. ✅

---

## 5. Verification Gates

| Bucket | Local verify | CI gate |
|--------|---------------|---------|
| A | `terraform fmt -check` + `terraform validate` (no apply) + `python3 -c "import yaml; yaml.safe_load(open('.github/workflows/terraform-plan.yml'))"` | terraform-plan workflow lint |
| B | `docker build` 6 KH services dry-run + `python3 -c yaml.safe_load(...)` | docker-build-push workflow run on PR |
| C | `pnpm -F kitehub-frontend exec playwright test --list` (verify spec syntax) + workflow YAML validate | E2E workflow dry-run |
| D | `docker compose --profile infra-only config` (validate) + shellcheck `kitehub/scripts/up.sh` | (no CI; manual) |
| E | Markdown lint + cross-link check | (docs-only; markdown CI) |

---

## 6. Agent Spawn Pattern

Per `feedback_parallel_agent_strategy.md` + `agent-background-spawn-default.md`:
- 5 buckets parallel, no Bucket 0
- Opus medium 4 buckets (A/B/C/E); Sonnet 1 bucket (D — config + docs LOW-stakes)
- `run_in_background: true` + `isolation: worktree`
- RELATIVE paths trong prompts per `feedback_worktree_absolute_path_contamination.md`
- Coordinator merges sequential A→B→C→D→E (T1 must merge BEFORE B reads ECR repo names)

---

## 7. Closure Protocol

- Each bucket PR: GAP files Status flip per `gap-done-discipline.md` §2 (verify AC checked, no banned phrases, verification artifacts)
- Wave 37 closure PR: ROADMAP §🚀 Next Action update + Release Plan Progress section + `wave-history.jsonl` append + worktree prune (per `post-wave-cleanup.md`)
- Wave 37 closure: re-run audit suite **OR** trigger domain-milestone deferral per `post-wave-audit-mandate.md` §2.4 (domain `release-deploy-artifacts` registered)

**Phase 1 BETA promotion gate (post Wave 37):**
- 22 gaps DONE → ready cho v0.9.0-beta tag preparation
- AWS Activate credit approved → effective $0 cost
- Architecture B Terraform applied (human-executed) → infra ready
- Docker images pushed ECR ap-southeast-1 → deploy ready
- Beta tenant invite mechanism (GAP-372) ready → invite-only soft launch

---

## 8. Log

- **2026-05-07** (draft): Plan created post-Wave-36 closure (PR #934). 22 GAP-NEW filed (GAP-395..416) chia 5 layer per Architecture B + AWS Activate strategy chốt 2026-05-07. Cross-layer scope NO (pure infra/devops). Pairs trong cùng plan PR với 22 gap files. Estimated wall-clock 60-90 min với 5 background agents.
- **2026-05-07** (SHIPPED): All 5 buckets merged sequential A→B→C→D→E. PRs: #938 (A) + #936 (B) + #937 (C) + #940 (D) + #939 (E). Status flips: 8 GAP → 🟢 DONE (395/396/397/403/404/407/409/410/411/416), 14 GAP → 🟡 PARTIAL (production execution + staging baselines + human-action items per gap-done-discipline.md §3 PARTIAL exit ramp). Coordinator-applied 3 mechanical fixes: (1) Bucket B 5 KH Dockerfile parent `<modules>` resolution via COPY all sibling pom stubs, (2) Bucket B 2 frontend Dockerfile pnpm@9 pin (Node 20 incompat với pnpm@latest=11 require node:sqlite Node 22.5+), (3) Bucket D direct ship sau 2× Sonnet agent autocompact-thrash (LOW-stakes scope nhẹ). Audit strategy: AUDIT_DEFER_DOMAIN_MILESTONE: release-deploy-artifacts — milestone Phase 1 BETA launch wave (post-deploy AWS apply + smoke + signoff) per post-wave-audit-mandate.md §2.4. 73rd consecutive 0-clarif streak. Wall-clock parallel ~12 min longest agent + ~10 min coordinator CI fix iterations + ~10 min closure.
