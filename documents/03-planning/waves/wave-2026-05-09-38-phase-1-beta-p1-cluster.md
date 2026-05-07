---
title: Wave 38 — Phase 1 BETA P1 STRONGLY cluster (CDN + status page + tag-CI + staging activation)
status: complete
created: 2026-05-07
updated: 2026-05-07
waves: [38]
gaps: [GAP-371, GAP-373, GAP-374, GAP-380]
---

# Wave 38 — Phase 1 BETA P1 STRONGLY cluster

**Goal:** Đóng 4 P1 STRONGLY recommend Phase 1 BETA still OPEN sau Wave 37 (per ROADMAP §🚀 Next Action #4 + Phase 1 BETA P1 STRONGLY row).

**Trigger:** Wave 37 SHIPPED (#941 closed) + Architecture B locked (ADR-025) + Phase 1 BETA P0 BLOCKING infrastructure foundation đã ship (Wave 33 + 37).

**Estimated wall-clock:** ~4-5h dev parallel; ~15-25 min với 4 background agents (D heaviest).

**Prerequisite:** Wave 37 closed.

---

## 1. Brainstorm

**Q1 (alignment):** 4 P1 STRONGLY-recommend gaps from `release-1-deploy-plan.md` Phase 1 BETA P1 row. Tất cả là deploy-readiness artifacts, NOT FE/BE production code → pure infra/devops/runbook scope.

**Q2 (trade-offs):**
- **Reject** ship 4 gaps serial — disjoint file scope, parallel agents save 70% wall-clock
- **Reject** mix với GAP-272o orchestrator wiring — unrelated domain (FE wizard)
- **Accept** Sonnet cho LOW-stakes docs/runbook buckets (B/C); Opus medium cho Bucket A (CI logic, conventional-commit changelog parsing) + Bucket D (Architecture B re-scope from EKS-based to EC2-based staging)
- **Accept** GAP-380 needs scope adjustment per ADR-025 (no EKS Phase 1 → 1 staging EC2 host with docker-compose OR small dedicated EC2 + Helm-skip path)

**Q3 (risks):**
- **R1:** Bucket B (CDN) + Bucket C (status page) chủ yếu là USER-EXECUTED ops actions (Cloudflare account, Statuspage vendor signup). Agent ship docs + scripts + DNS templates; user executes account setup post-merge. Status flip = 🟡 PARTIAL per `gap-done-discipline.md` §3 (cùng pattern Wave 33 + Wave 37 deploy gaps).
- **R2:** Bucket D scope-rewrite — gap mentions EKS staging cluster nhưng Architecture B = EC2-only Phase 1. Agent phải re-scope thành EC2-based staging (single t3.micro hoặc shared with prod) với docker-compose deploy path, hoặc defer EKS-staging variant đến Phase 2 EKS migration (per Wave 37 GAP-415).
- **R3:** Bucket A (release-tag CI) interacts với Wave 37 Bucket B docker-build-push.yml (just landed) — scope check: extend existing workflow OR add NEW `release-tag.yml`? Agent prefers NEW workflow (clear separation; trigger-by-tag vs trigger-by-PR).
- **R4:** Cross-bucket dependency: NONE. All 4 disjoint. Coordinator merges A→B→C→D arbitrary order.

---

## 2. Task Breakdown

| Bucket | Gap | Owner | Effort | Disjoint? |
|--------|-----|-------|--------|-----------|
| A | GAP-374 Tag-based release CI | bg-agent (Opus medium) | 1.5h | ✅ `.github/workflows/release-tag.yml` NEW + `scripts/generate-changelog.sh` NEW |
| B | GAP-371 Cloudflare CDN | bg-agent (Sonnet) | 1h | ✅ `documents/05-guides/deploy/cloudflare-setup.md` NEW + `scripts/verify-cdn-headers.sh` NEW |
| C | GAP-373 Status page + incident comms | bg-agent (Sonnet) | 1h | ✅ `documents/05-guides/operations/incident-comms-runbook.md` NEW + `documents/05-guides/operations/post-mortem-template.md` NEW + `documents/02-architecture/adr/ADR-027-statuspage-vendor.md` NEW |
| D | GAP-380 Staging activation (Architecture B revision) | bg-agent (Opus medium) | 2h | ✅ `infrastructure/terraform-aws/staging.tf` NEW + `.github/workflows/deploy-staging.yml` rewrite + `scripts/seed-staging-fixtures.sh` NEW + `documents/05-guides/deploy/staging-activation-runbook.md` NEW |

Disjoint check:
- A=`.github/workflows/release-tag.yml` + `scripts/generate-changelog.sh` + `documents/03-planning/roadmap/versioning-policy.md` (1 small append)
- B=`documents/05-guides/deploy/cloudflare-setup.md` + `scripts/verify-cdn-headers.sh`
- C=`documents/05-guides/operations/{incident-comms-runbook,post-mortem-template}.md` + `documents/02-architecture/adr/ADR-027*.md`
- D=`infrastructure/terraform-aws/staging.tf` + `.github/workflows/deploy-staging.yml` (rewrite) + `scripts/seed-staging-fixtures.sh` + `documents/05-guides/deploy/staging-activation-runbook.md`

No file collision between buckets (A's versioning-policy.md append + B's cloudflare-setup.md + C's incident-comms.md + D's staging-activation-runbook.md all NEW or distinct sections).

**Cross-layer wave?** NO — pure infra/devops/runbook, không touch FE+BE production code. Skip Bucket 0 Foundation per `contract-first-for-cross-layer.md` §2.

---

## 3. Scope

**Stake tier:** MEDIUM (P1 STRONGLY = không block Phase 1 BETA launch nhưng giảm risk + professionalism). Model: **Opus medium** A+D (CI logic + Terraform infra), **Sonnet** B+C (docs-heavy runbooks).

| # | Bucket | Gap | Priority | Files | Spawn order |
|:-:|--------|-----|:--------:|-------|:-----------:|
| 1 | A | GAP-374 | 🟠 P1 | `.github/workflows/release-tag.yml` (NEW) + `scripts/generate-changelog.sh` (NEW) + `documents/03-planning/roadmap/versioning-policy.md` (append §6 release process) | parallel |
| 2 | B | GAP-371 | 🟠 P1 | `documents/05-guides/deploy/cloudflare-setup.md` (NEW) + `scripts/verify-cdn-headers.sh` (NEW) | parallel |
| 3 | C | GAP-373 | 🟠 P1 | `documents/05-guides/operations/incident-comms-runbook.md` (NEW) + `documents/05-guides/operations/post-mortem-template.md` (NEW) + `documents/02-architecture/adr/ADR-027-statuspage-vendor.md` (NEW) | parallel |
| 4 | D | GAP-380 | 🟠 P1 | `infrastructure/terraform-aws/staging.tf` (NEW) + `.github/workflows/deploy-staging.yml` (REWRITE Architecture B) + `scripts/seed-staging-fixtures.sh` (NEW) + `documents/05-guides/deploy/staging-activation-runbook.md` (NEW) | parallel |

### Bucket A — GAP-374 Tag-based release CI

Files (RELATIVE):
- `.github/workflows/release-tag.yml` (NEW): trigger `tags: 'v[0-9]+.[0-9]+.[0-9]+*'`; jobs: `validate-tag` (parse version + is_prerelease) → `build-images` (delegate to existing docker-build-push.yml via workflow_call) → `generate-changelog` → `create-github-release`
- `scripts/generate-changelog.sh` (NEW): conventional-commit parser; output `## [vX.Y.Z] - YYYY-MM-DD\n### Added\n### Changed\n### Fixed`
- `documents/03-planning/roadmap/versioning-policy.md` (append §6.x release process automated steps cross-link)

Acceptance per gap §AC.

### Bucket B — GAP-371 Cloudflare CDN

Files (RELATIVE):
- `documents/05-guides/deploy/cloudflare-setup.md` (NEW): step-by-step account creation + DNS migration + page rules + WAF + smoke check
- `scripts/verify-cdn-headers.sh` (NEW): smoke `curl -I` checks for `CF-Ray`, `CF-Cache-Status`, `Server: cloudflare`

Acceptance per gap §AC. Status flip = 🟡 PARTIAL (account creation user-action).

### Bucket C — GAP-373 Status page + incident comms

Files (RELATIVE):
- `documents/05-guides/operations/incident-comms-runbook.md` (NEW): 6-step procedure (Detect → Triage → Post → Update → Resolve → Post-mortem)
- `documents/05-guides/operations/post-mortem-template.md` (NEW): RCA format, action items, timeline
- `documents/02-architecture/adr/ADR-027-statuspage-vendor.md` (NEW): vendor evaluation Statuspage.io vs Instatus vs Cachet vs Statping; recommend hosted (Instatus) Phase 1 cost-bound

Acceptance per gap §AC. Status flip = 🟡 PARTIAL (vendor signup + DNS configure user-action).

### Bucket D — GAP-380 Staging activation (Architecture B revision)

**Scope re-write:** Per ADR-025 + Wave 37 Bucket A Terraform Architecture B (no EKS Phase 1), staging environment Phase 1 = single EC2 t3.micro host với docker-compose (mirroring prod). Phase 2 EKS migration (per Wave 37 GAP-415) sẽ swap path.

Files (RELATIVE):
- `infrastructure/terraform-aws/staging.tf` (NEW): single t3.micro EC2 staging host + RDS db.t3.micro staging + S3 staging bucket + Route53 staging.kitehub.vn / staging.kiteclass.vn (Cloudflare proxied per Bucket B)
- `.github/workflows/deploy-staging.yml` (REWRITE): drop EKS_CLUSTER + helm; replace với SSH-into-staging-EC2 + docker-compose pull + restart
- `scripts/seed-staging-fixtures.sh` (NEW): synthetic 5-10 tenants + sample students/classes/lessons; sandbox payment + mailhog catchall
- `documents/05-guides/deploy/staging-activation-runbook.md` (NEW): activation steps + Helm-skip rationale (Phase 2 EKS migration trigger gate references Wave 37 GAP-415)

Acceptance per gap §AC (revised). Status flip = 🟡 PARTIAL (terraform apply + first deploy = user-action).

---

## 4. State-Check Evidence

Per `audit-to-gap-pipeline.md` §2.6 — verify symbols referenced trong §3 Scope:

| Symbol | Type | Verification command | Verdict |
|--------|------|----------------------|---------|
| `.github/workflows/release-tag.yml` | NEW workflow | — | 🆕 to-be-created (Bucket A) |
| `.github/workflows/docker-build-push.yml` | Existing workflow (Bucket B reuses via workflow_call) | `ls .github/workflows/docker-build-push.yml` | ✅ exists (post-Wave-37) |
| `.github/workflows/deploy-staging.yml` | Existing workflow (Bucket D rewrites) | `ls .github/workflows/deploy-staging.yml` | ✅ exists |
| `documents/03-planning/roadmap/versioning-policy.md` | Existing doc (Bucket A appends §6) | `ls documents/03-planning/roadmap/versioning-policy.md` | ✅ exists |
| `documents/05-guides/deploy/` | Folder | `ls documents/05-guides/deploy/` | ✅ exists |
| `documents/05-guides/operations/` | Folder | `ls documents/05-guides/operations/` | ✅ exists (audit-chain-break-runbook + disaster-recovery-plan + dns-setup-runbook + runbooks/) |
| `documents/02-architecture/adr/` | Folder | `ls documents/02-architecture/adr/` | ✅ exists |
| `ADR-025` | Reference (Architecture B) | `ls documents/02-architecture/adr/ADR-025*.md` | ✅ exists (AWS Singapore) |
| `ADR-026` | Reference (Ollama defer) | `ls documents/02-architecture/adr/ADR-026*.md` | ✅ exists (Wave 37 Bucket E) |
| `ADR-027-statuspage-vendor.md` | NEW ADR | — | 🆕 to-be-created (Bucket C) |
| `infrastructure/terraform-aws/staging.tf` | NEW | — | 🆕 to-be-created (Bucket D) |
| `infrastructure/terraform-aws/main.tf` | Existing reference (Bucket D builds on) | `ls infrastructure/terraform-aws/main.tf` | ✅ exists (Wave 37) |
| `scripts/generate-changelog.sh` | NEW | — | 🆕 to-be-created (Bucket A) |
| `scripts/verify-cdn-headers.sh` | NEW | — | 🆕 to-be-created (Bucket B) |
| `scripts/seed-staging-fixtures.sh` | NEW | — | 🆕 to-be-created (Bucket D) |
| `release-deploy-standard.md` | Rule | `ls .claude/rules/release-deploy-standard.md` | ✅ exists v1.0.0 |
| `gap-done-discipline.md` | Rule | `ls .claude/rules/gap-done-discipline.md` | ✅ exists |

All forward-looking 🆕 symbols owned by named bucket. ✅

---

## 5. Verification Gates

| Bucket | Local verify | CI gate |
|--------|---------------|---------|
| A | `python3 -c "import yaml; yaml.safe_load(open('.github/workflows/release-tag.yml'))"` + `shellcheck scripts/generate-changelog.sh` + `bash scripts/generate-changelog.sh --self-test` (synthetic conventional commits → expected output) | YAML parse |
| B | Markdown lint OK + `shellcheck scripts/verify-cdn-headers.sh` | (no CI; manual user-execute post-Cloudflare-setup) |
| C | Markdown lint OK on 3 NEW docs + ADR-027 follows MADR template (Status / Context / Decision / Consequences) + cross-link check | (no CI; docs-only) |
| D | `terraform fmt -check infrastructure/terraform-aws/staging.tf` + `terraform init -backend=false && terraform validate` + `python3 yaml.safe_load(deploy-staging.yml)` + `shellcheck scripts/seed-staging-fixtures.sh` | terraform-plan workflow runs on PR (Wave 37 Bucket A shipped) |

---

## 6. Agent Spawn Pattern

Per `feedback_parallel_agent_strategy.md` + `agent-background-spawn-default.md`:
- 4 buckets parallel, no Bucket 0 (cross-layer wave = NO)
- Opus medium 2 buckets (A/D — CI logic + Terraform infra); Sonnet 2 buckets (B/C — docs-heavy LOW-stakes)
- `run_in_background: true` + `isolation: worktree`
- RELATIVE paths trong prompts per `feedback_worktree_absolute_path_contamination.md`
- Coordinator merge order arbitrary (all disjoint), recommend A→B→C→D

---

## 7. Closure Protocol

- Each bucket PR: GAP files Status flip per `gap-done-discipline.md` §2 (verify AC checked, no banned phrases, follow-up gap filed cho deferred user-action items per §3 PARTIAL exit ramp)
- Wave 38 closure PR: ROADMAP §🚀 Next Action update + Release Plan Progress section (Phase 1 BETA P1 STRONGLY row → 4 of 7 STILL OPEN → 0 OPEN if all 4 ship) + `wave-history.jsonl` append + worktree prune (per `post-wave-cleanup.md`)
- **Audit strategy:** `AUDIT_DEFER_DOMAIN_MILESTONE: release-deploy-artifacts` — same domain registry entry as Wave 37 (per `post-wave-audit-mandate.md` §2.4); milestone = Phase 1 BETA launch wave (post-deploy AWS apply + smoke + signoff). Pure infra/devops/runbook scope.

**Phase 1 BETA promotion gate (post Wave 38):**
- 22 P0 + 4 P1 STRONGLY shipped artifacts (Wave 33 + 37 + 38)
- Remaining BLOCKING = user-executed (domain registration, SES production approval, AWS Secrets Manager provisioning, terraform apply, ECR push, Cloudflare account, Statuspage vendor signup, AWS Activate Founders Pack submission)
- Post user-action: smoke test + invite 5-10 beta tenants → Phase 1 BETA launch ready

---

## 8. Log

- **2026-05-07** (draft): Plan created post-Wave-37 closure (PR #941). 4 P1 STRONGLY remaining gaps clustered: GAP-371 CDN + GAP-373 status page + GAP-374 tag-CI + GAP-380 staging (Architecture B re-scope from EKS to EC2 + docker-compose Phase 1; Phase 2 EKS migration deferred per Wave 37 GAP-415). Cross-layer scope NO (pure infra/devops/runbook). Estimated wall-clock 15-25 min với 4 background agents (D heaviest ~25 min Terraform + workflow rewrite + fixtures script + runbook).
- **2026-05-07** (SHIPPED): All 4 buckets merged. PRs: #943 (A) + #945 (B) + #944 (C) + #946 (D). Status flips: 0 DONE / 4 PARTIAL — all deferred user-action (Cloudflare account, Statuspage signup, Notification channel, terraform apply + first deploy). Side-PR #947 release-1-deploy-runbook DRAFT (Phase 0-9 ordered sequence post user re-trace request). 3 coordinator-applied iterations: (1) Bucket B salvaged Sonnet thrash (615 LOC files intact pre-crash), (2) Bucket C wrote 3 docs directly after Sonnet thrash 2x, (3) Bucket D salvaged Opus 529 overloaded + post-fix terraform heredoc-ternary syntax error. Audit strategy: AUDIT_DEFER_DOMAIN_MILESTONE: release-deploy-artifacts — same domain as Wave 37 (per `post-wave-audit-mandate.md` §2.4); milestone = Phase 1 BETA launch wave. 74th consecutive 0-clarif streak. Wall-clock parallel ~12 min longest agent + ~15 min coordinator iterations + ~10 min closure.
