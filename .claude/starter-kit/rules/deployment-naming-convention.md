# Deployment Naming Convention — folder taxonomy + filename patterns

**Priority:** 🟠 MANDATORY — deployment artifact placement governance
**Version:** 1.0.1
**Created:** 2026-05-08
**Last-Reviewed:** 2026-05-14
**Reviewer-Approver:** @nguyenvankiet (starter-kit upstream maintainer)
**Applies to:** Every artifact under `documents/05-guides/{deploy,operations,account-prep,setup}/**`, `infrastructure/**`, `documents/03-planning/roadmap/*deploy*`, `.github/workflows/{*deploy*,*terraform*,docker-build-push}.yml`, deployment scripts under `scripts/`, runbook files anywhere

---

## 1. The Rule

> **Every deployment artifact PHẢI nằm đúng folder theo §2 taxonomy và follow §3 filename pattern.** Drift giữa "intended folder per intent" vs "actual location" = rule violation; cleanup PR sẽ relocate.

This rule emerged từ Wave 45 Bucket C drift incident 2026-05-08 — `email-ses-setup-runbook.md` actual path `documents/05-guides/operations/` while plan referenced `deploy/`. Coexistence of `deploy/` + `operations/` folders without clear delineation = systemic drift cause cho mọi runbook subsequent.

---

## 2. Folder taxonomy

Decision rule for each artifact: **WHEN does it run vs HOW often?**

| Folder | Lifecycle phase | Frequency | Examples |
|--------|----------------|-----------|----------|
| `documents/05-guides/account-prep/` | Pre-environment | One-time per cloud account / domain | AWS account creation, IAM bootstrap, domain procurement |
| `documents/05-guides/deploy/` | Pre-deploy + during-deploy | One-time per release / environment | Terraform apply runbooks, infra bootstrap, DNS setup, SES production approval, SSL cert install, secrets seeding |
| `documents/05-guides/operations/` | Post-deploy | Recurring (per-alert / per-incident / scheduled) | Incident response, monitoring runbooks, alert triage, log analysis, on-call procedures, key rotation cadence |
| `documents/05-guides/operations/runbooks/` | Post-deploy alerts | Per-alert specific | Alert-name-specific runbooks (one runbook per Prometheus/CloudWatch alert) |
| `infrastructure/terraform-aws/` | Code (HCL) | N/A | Terraform modules + state config |
| `infrastructure/helm/` | Code (YAML) | N/A | Helm charts |
| `infrastructure/k8s/` | Code (YAML) | N/A | Raw K8s manifests |
| `documents/03-planning/roadmap/` | Plans (versioned) | Per release | `release-N-deploy-plan.md`, deploy strategy docs |
| `.github/workflows/` | CI/CD (YAML) | N/A | Deploy + terraform workflows |
| `documents/05-guides/dev/` + `local-dev/` | Developer ergonomics | N/A | Dev-stack bootstrap, local-only setup |

### Decision rule for borderline cases

Ask 2 questions in order:

1. **Khi nào artifact này chạy?** — pre-deploy (one-time setup) → `deploy/` | post-deploy (ongoing) → `operations/` | pre-environment (per-cloud-account) → `account-prep/`
2. **Recurring?** — yes (cron / per-alert / per-incident) → `operations/` | no (one-time per release) → `deploy/`

Example applications:
- `email-ses-setup-runbook.md` — one-time pre-deploy SES domain verification + sandbox→production approval → **`deploy/`** (currently in `operations/` — drift)
- `dns-setup-runbook.md` — one-time domain procurement + DNS records → **`deploy/`** (or `account-prep/` if procurement-only)
- `secrets-management-runbook.md` — depends on content: initial seeding = `deploy/`, rotation cadence = `operations/`. Split if covers both.
- `terraform-apply-bootstrap-runbook.md` — one-time chicken-and-egg admin apply → **`deploy/`** ✅ (renamed from `terraform-apply-bootstrap.md` per §3 suffix requirement, cleanup PR 2026-05-08)
- `prometheus-alert-X-runbook.md` — per-alert response → **`operations/runbooks/`** ✅
- `restore-procedure.md` — recurring DR drill → **`operations/`** (currently in `deploy/` — verify)

---

## 3. Filename patterns

| Artifact type | Pattern | Examples |
|---------------|---------|----------|
| Pre-deploy runbook | `<topic>-<action>-runbook.md` | `email-ses-setup-runbook.md`, `dns-cloudflare-setup-runbook.md`, `terraform-apply-bootstrap-runbook.md` |
| Post-deploy runbook | `<topic>-<action>-runbook.md` (same pattern, folder differs) | `incident-response-runbook.md`, `db-backup-rotation-runbook.md` |
| Per-alert runbook | `<alert-name>-runbook.md` | `kh-backend-memory-high-runbook.md` |
| Procedure (executable steps) | `<topic>-procedure.md` | `rollback-procedure.md`, `restore-procedure.md` |
| Plan (versioned) | `release-<N>-<artifact>-plan.md` | `release-1-deploy-plan.md` |
| Plan session log | `release-<N>-deploy-session-<YYYY-MM-DD>.md` | `release-1-deploy-session-2026-05-07.md` |
| GitHub workflow | `<verb>-<target>.yml` | `terraform-apply.yml`, `terraform-plan.yml`, `docker-build-push.yml` |
| Setup script | `<phase>-<target>.sh` | `setup-jwt-keys.sh`, `prune-merged-worktrees.sh` |
| ADR-class architecture decision | `ADR-<NNN>-<slug>.md` | placed under `documents/02-architecture/adr/`, NOT under deployment scope |

### Required suffixes

- **`-runbook.md`** for any artifact describing step-by-step operational procedure (pre or post-deploy)
- **`-procedure.md`** for executable repeatable processes (preferred over `-runbook.md` when scope = single linear procedure, không phải decision tree)
- **NO suffix** required for plans (`release-N-*-plan.md` already self-describing)

### Banned patterns

- Generic names: `setup.md`, `notes.md`, `temp.md` — fail to indicate scope
- Date in filename without versioning: `setup-2026-05.md` — use git history instead
- Mixing concerns: `deploy-and-monitoring.md` — split into 2 files

---

## 4. Anti-patterns

| ❌ Don't | ✅ Do |
|---------|------|
| Place new pre-deploy runbook in `operations/` "because it's about ops" | Apply §2 decision rule: WHEN runs + WHEN often → folder follows |
| Skip `-runbook.md` suffix because filename "already long" | Suffix là contract — readers grep `find . -name "*-runbook.md"` cho operational artifacts |
| Place release plan trong `05-guides/deploy/` | Plans (versioned, evolving) → `documents/03-planning/roadmap/`; runbooks (stable procedures) → `05-guides/` |
| Duplicate runbook content across `deploy/` + `operations/` | Pick canonical location per §2; cross-link from sister folder if needed |
| Mix Helm chart YAML in `documents/05-guides/` | Code → `infrastructure/`; docs ABOUT code → `documents/05-guides/` |
| Name workflow `deploy-things-v2.yml` | `<verb>-<target>.yml` — `terraform-apply.yml` not `apply-tf.yml` |
| Use `account-prep/` for re-runnable per-release work | `account-prep/` = one-time per cloud account; per-release work → `deploy/` |

---

## 5. Decision flow (3-question flowchart)

```
1. Is artifact deployment-related?
   ├─ NO → out of scope (use docs-folder-structure.md generic rule)
   └─ YES → continue

2. WHEN does it run?
   ├─ Before environment exists (per cloud account)     → account-prep/
   ├─ Before/during deploy (per release)                → deploy/
   ├─ After deploy (post-launch ongoing)                → operations/
   ├─ Per-alert specific                                → operations/runbooks/
   └─ Code (HCL/YAML) → infrastructure/{terraform-aws,helm,k8s}/

3. Recurring frequency?
   ├─ One-time per release                              → deploy/
   ├─ Recurring (cron/alert/incident)                   → operations/
   ├─ One-time per cloud account                        → account-prep/
   └─ Versioned plan (per-release evolving)             → documents/03-planning/roadmap/
```

---

## 6. Self-test (worked example — Wave 45 Bucket C drift incident)

**Scenario:** 2026-05-08, Wave 45 Bucket C agent (PR #1050) flagged `email-ses-setup-runbook.md` actual path = `documents/05-guides/operations/` while plan §3 referenced `documents/05-guides/deploy/`.

**Apply §2 + §5 decision flow retroactively:**

1. Is `email-ses-setup-runbook.md` deployment-related? ✅ YES (covers SES domain verify + DKIM/SPF/DMARC + sandbox→production approval — all pre-deploy infrastructure setup)
2. WHEN does it run? Before deploy / one-time per release (SES domain verification + production approval = one-time per AWS account + per release domain)
3. Recurring? NO — once approved, doesn't re-run

**Verdict:** correct folder = `documents/05-guides/deploy/`. Current `operations/` placement is **drift**.

**Cleanup scope** (✅ DONE 2026-05-08 cleanup PR):
- Moved `documents/05-guides/operations/email-ses-setup-runbook.md` → `documents/05-guides/deploy/email-ses-setup-runbook.md`
- Updated internal links across GAP-370/372/394/423/449, Wave 45 plan §3 Bucket C, release-1-deploy-plan.md, ROADMAP, account-prep/01/02/04, scripts/ssl-cert-setup.sh, scripts/check-dns-propagation.sh, your-service-email Java sources/tests, sister runbook cross-links — 14 files swept clean.

**Other drift candidates** (post-cleanup state):
- ✅ `operations/dns-setup-runbook.md` → `deploy/dns-setup-runbook.md` (cleanup PR 2026-05-08)
- ✅ `operations/secrets-management-runbook.md` split — GAP-452 closure 2026-05-11. §3 Provisioning + §9 first-time AC extracted to `deploy/secrets-seeding-runbook.md`; remainder renamed to `operations/secrets-rotation-runbook.md` với rotation-focused §9 AC. Both files cross-link in §1.
- ✅ `deploy/terraform-apply-bootstrap.md` → `deploy/terraform-apply-bootstrap-runbook.md` (cleanup PR 2026-05-08)

**Self-test verdict:** rule fires correctly + identifies all 4 drift candidates surfaced by parallel agents. ✅

---

## 7. Enforcement

Per `rule-change-process.md` §6.5 Enforcement Parity Mandate, this rule ships với:

### 7.1 Reviewer-checklist (active now)

PR review for any diff touching:
- New file under `documents/05-guides/{deploy,operations,account-prep}/**`
- File rename across these folders
- New `infrastructure/` README or runbook
- New `.github/workflows/*deploy*` or `*terraform*`

Reviewer asks:
- [ ] Does artifact match §2 folder per §5 decision flow?
- [ ] Filename follows §3 pattern (suffix included)?
- [ ] If borderline, has §2 decision rule applied + rationale documented in PR description?

### 7.2 Memory cross-link (paired follow-up)

`feedback_deployment_naming_consistency.md` (filed as follow-up gap if not paired same-PR) — auto-loads per session, reminds Claude to apply §5 decision flow before placing new deployment artifact.

### 7.3 Quarterly audit (deferred per `incident-to-rule-pipeline.md` premature-rule guard)

`quality-audit` skill samples 5 random recently-added deployment artifacts. Verify §2 placement + §3 naming. Pattern of misses → meta-review of this rule.

### 7.4 Future automation (deferred ≥7 days)

Pre-commit hook detecting:
- New file in `operations/` with content matching pre-deploy patterns ("first time", "one-time setup", "production approval")
- New file outside `documents/05-guides/{deploy,operations,account-prep}/` with `-runbook.md` suffix
- Workflow files violating `<verb>-<target>.yml` pattern

Tracked separately; reviewer manual sufficient for solo-dev mode pending recurrence.

### 7.5 Override mechanism

Genuine exception (e.g., legacy artifact predating rule, archive folder, multi-phase split):

```
git commit -m "...
DEPLOYMENT_NAMING_OVERRIDE: <reason — explain why §2/§3 don't apply>"
```

Trailer logged in quarterly retro. Pattern frequency >5% triggers meta-review of taxonomy.

---

## 8. Edge cases

| Case | Resolution |
|------|-----------|
| Runbook covers BOTH initial setup + recurring rotation | Split into 2 files; `deploy/<topic>-setup-runbook.md` + `operations/<topic>-rotation-runbook.md` với cross-link |
| Archived obsolete runbook | Move to `documents/07-archived/<topic>-YYYY/` per `docs-folder-structure.md`; out of scope of this rule |
| Vendor-specific (Cloudflare, AWS, Stripe) sub-runbooks | Group under `<vendor>/` subfolder if ≥3 runbooks: `deploy/cloudflare/cdn-setup-runbook.md` + `deploy/cloudflare/dns-setup-runbook.md` + `deploy/cloudflare/firewall-setup-runbook.md` |
| Dev-only runbook (local stack bootstrap) | `documents/05-guides/dev/` or `local-dev/` — out of scope of this rule (deployment vs dev-ergonomics distinction) |
| Multi-phase deploy (Phase 1 BETA → Phase 1.5 → Phase 2) | Each phase's deploy plan = separate file: `release-1-phase-1-deploy-plan.md`, `release-1-phase-2-deploy-plan.md`. Single runbook covering all phases OK if phases share procedure |

---

## 9. Relationship to other rules

- **`docs-folder-structure.md`** — generic rule cho `documents/` toàn cục; this rule = specialization for deployment scope. Where overlap, this rule takes precedence per `planning-docs-structure.md` precedent pattern.
- **`release-deploy-standard.md`** §3 per-bump-type artifact checklist — this rule defines WHERE checklist artifacts live; standard defines WHAT artifacts required.
- **`incident-to-rule-pipeline.md`** — direct origin: Wave 45 Bucket C drift incident → 5-stage pipeline → this rule.
- **`rule-change-process.md`** §6.5 Enforcement Parity Mandate — rule + reviewer-checklist + worked self-test all ship same PR.
- **`audit-to-gap-pipeline.md`** Step 2.5 state-check — cleanup PR following this rule MUST run state-check on each move (verify content scope before relocation, không blind-move).
- **`gap-done-discipline.md`** §2 — cleanup PRs that flip drift gaps to DONE follow §2 criteria; verify no broken links post-relocation.

---

## 10. Log

- **2026-05-08 (v1.0.1):** PATCH — sync §6 Cleanup scope from "to do" → "✅ DONE" reflecting cleanup PR shipped 2026-05-08. 3 file relocations applied: (a) `operations/email-ses-setup-runbook.md` → `deploy/`, (b) `operations/dns-setup-runbook.md` → `deploy/`, (c) `deploy/terraform-apply-bootstrap.md` → `deploy/terraform-apply-bootstrap-runbook.md` (suffix). 14 files swept for link updates (4 GAP files, 2 wave plans, ROADMAP, 3 account-prep guides, 1 sister runbook, 2 scripts, 1 Java test). Secrets-management split deferred to follow-up gap (substantive editorial scope). Reviewer: @nguyenvankiet (solo-dev PATCH self-approve per `rule-change-process.md` §5 — status sync, no constraint change). Validates rule §7.5 grandfathering clause: "existing misplaced runbooks grandfathered until cleanup PR ships" — cleanup shipped, ungrandfathering complete.
- **2026-05-08 (v1.0.0):** Rule created. Triggered by Wave 45 Bucket C drift incident — `email-ses-setup-runbook.md` actual `operations/` vs plan-referenced `deploy/`. Per `incident-to-rule-pipeline.md` 5-stage applied: Detect ✓ (Bucket C agent flagged) → Classify ✓ (no existing rule covers deployment artifact placement; `docs-folder-structure.md` generic doesn't specialize) → Rule+Enforce ✓ (this file + reviewer-checklist §7.1 paired same PR per `rule-change-process.md` §6.5) → Self-Test ✓ (§6 worked example identifies 4 drift candidates surfaced by parallel agents) → Retro Log ✓ (this entry). Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per §5 — new constraint, no loosening; existing misplaced runbooks grandfathered until cleanup PR ships separately). Cleanup scope identified §6 — sister cleanup PR(s) to follow per `audit-to-gap-pipeline.md` Step 2.5 state-check on each relocation. Phase 2 detection automation deferred per `incident-to-rule-pipeline.md` premature-rule guard ≥7 days.
