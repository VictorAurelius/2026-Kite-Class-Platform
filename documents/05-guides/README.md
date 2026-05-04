# 05-guides — Operational Guides & Runbooks

**Rules:** [`.claude/rules/docs-folder-structure.md`](../../.claude/rules/docs-folder-structure.md)

Operator-facing documentation — "how to run, deploy, recover, and troubleshoot the system." Khác [`02-architecture/`](../02-architecture/) (design + rationale) và [`03-planning/`](../03-planning/) (roadmap). Mọi doc trong đây viết cho người **đang vận hành** production hoặc dev environment, không phải designer.

**Audience:** SRE, DevOps, On-call engineers, New developers setting up local env, Incident responders.

---

## Directory Map

| Path | Purpose | Typical files |
|------|---------|---------------|
| `README.md` | This index | 1 |
| [`SECRET-MANAGEMENT.md`](SECRET-MANAGEMENT.md) | K8s Sealed Secrets + Vault setup | 1 |
| [`deploy-go-nogo-checklist.md`](deploy-go-nogo-checklist.md) | Pre-deploy gate checklist (GAP-087) | 1 |
| [`incident-response-runbook.md`](incident-response-runbook.md) | SEV1-SEV3 triage procedure (GAP-086) | 1 |
| [`rollback-procedure.md`](rollback-procedure.md) | Per-service rollback steps (GAP-088) | 1 |
| [`wsl2-fresh-setup.md`](wsl2-fresh-setup.md) | Clean-room reproducer for Windows + WSL2 dev env (added 2026-04-28) — start here for new machines | 1 |
| [`wsl-migration-playbook.md`](wsl-migration-playbook.md) | WSL2 migration playbook for existing Windows installs (preserves Claude Code memory; added 2026-04-18) | 1 |
| [`ssh-terminal-direct-access.md`](ssh-terminal-direct-access.md) | SSH direct from outside machine vào WSL2 dev terminal — sshd setup, Windows portproxy, tmux patterns, ops workflows; alternative to Claude Code remote-control cho ops-heavy verification loops (GAP-284 follow-up, 2026-05-04) | 1 |
| [`local-dev-setup-non-wsl.md`](local-dev-setup-non-wsl.md) | Mac/Linux native dev setup (GAP-102, 2026-04-18) | 1 |
| [`cicd-release-procedure.md`](cicd-release-procedure.md) | PR merge → prod deploy procedure (GAP-102, 2026-04-18) | 1 |
| [`tenant-onboarding-checklist.md`](tenant-onboarding-checklist.md) | End-to-end school onboarding (3-day target) (GAP-102, 2026-04-18) | 1 |
| [`api-performance-slo.md`](api-performance-slo.md) | API latency p95 SLO tiers + tagging rubric (GAP-135, 2026-04-21) | 1 |
| [`dependabot-guide.md`](dependabot-guide.md) | Dependabot config + workflow + troubleshooting (audience: Devs + Claude; 2026-04-24) | 1 |
| [`branding-integration.md`](branding-integration.md) | FE consumption of `/api/v1/branding/{public,/{id}/package}` — CSS-vars, ETag flow, BrandingProvider (audience: FE engineers; GAP-229 Phase 2.1, 2026-04-26) | 1 |
| [`ai-branding-wizard-flow.md`](ai-branding-wizard-flow.md) | 6-step wizard + saga handoff + tier behavior + support runbook (audience: onboarding + support + PMs; GAP-229 Phase 2.2, 2026-04-26) | 1 |
| [`template-contribution-guide.md`](template-contribution-guide.md) | How designers add SVG templates — 5 review criteria, file structure, commit checklist (audience: designers; GAP-229 Phase 2.3 + GAP-011, 2026-04-26) | 1 |
| [`operations/`](operations/) | Operations runbooks (deploy procedures) | `runbooks/*.md` |
| [`vietnamese/`](vietnamese/) | Vietnamese-language guides (Oracle Cloud deploy) | 1+ |

---

## File Placement Rules

- ✅ **Belongs here:**
  - Runbooks (step-by-step operational procedures)
  - Playbooks (decision trees for scenarios: dev setup, incident, rollback)
  - Checklists (go/no-go, pre-flight, post-deploy)
  - Troubleshooting guides (common errors + fixes)
  - Tenant onboarding operational steps

- ❌ **Does NOT belong here:**
  - Architecture rationale ("why RabbitMQ over batch") → [`02-architecture/adr/`](../02-architecture/adr/)
  - System design docs → [`02-architecture/`](../02-architecture/)
  - Feature planning / waves → [`03-planning/`](../03-planning/)
  - Gap reports / audits → [`04-quality/`](../04-quality/)
  - Business rules per domain → [`01-business/`](../01-business/)

- Naming: `kebab-case.md`, runbooks nên prefix context: `incident-*`, `deploy-*`, `rollback-*`

---

## Current Guides Philosophy

Đủ bộ guides cho production readiness cần cover 3 nhóm:

### 🔴 Production operations (must-have trước GA)
- ✅ Incident response runbook
- ✅ Rollback procedure
- ✅ Deploy go/no-go checklist
- ✅ Secret management
- ❌ Monitoring + alerting runbook (Wave 6 dependency — GAP-102)
- ❌ Database backup/restore SOP (GAP-093 + GAP-102)
- ❌ Security incident playbook (GAP-102)
- ❌ CI/CD release procedure (GAP-102)

### 🟠 Developer experience
- ✅ WSL migration playbook
- ✅ Local dev setup non-WSL (Mac/Linux native)

### 🟡 Tenant lifecycle
- ✅ Tenant onboarding checklist (3-day target)
- ❌ Tenant offboarding / data export (future)

### 🟢 Release management
- ✅ CI/CD release procedure

Completion tracked trong [GAP-102](../04-quality/gaps/GAP-102-guides-completion-adr-kickoff.md).

---

## Subdirectories

- **`operations/runbooks/`** — Granular runbook content (deployment procedures). Kept separate for volume-based organization.
- **`vietnamese/`** — Vietnamese-language versions cho guides mà target audience yêu cầu tiếng Việt (vd. client-facing Oracle Cloud deploy guide). Không phải mọi guide cần bản VN.

---

## Archive Policy

Move to `documents/07-archived/guides-YYYY/` khi:
- Procedure obsoleted (vd. rollback strategy changed fundamentally)
- Technology retired (vd. if we drop Oracle Cloud, archive VN Oracle guide)
- Guide replaced by auto-generated runbook

Keep version history — runbooks are "living docs", update in-place với changelog section at bottom.

---

## Related

- **Gaps:** [GAP-086](../04-quality/gaps/GAP-086-incident-response-runbook.md), [GAP-087](../04-quality/gaps/GAP-087-deploy-go-no-go.md), [GAP-088](../04-quality/gaps/GAP-088-rollback-procedure.md), [GAP-102](../04-quality/gaps/GAP-102-guides-completion-adr-kickoff.md)
- **Wave 6** — Monitoring + Observability drives new monitoring runbook
- **Wave 9** — Compliance MVP drives security incident playbook with legal sign-off
