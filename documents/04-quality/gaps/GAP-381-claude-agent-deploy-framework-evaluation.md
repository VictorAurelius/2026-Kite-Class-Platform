# GAP-381: Claude Agent Deploy Framework — Evaluation + Decision

**Status:** 🟢 DONE 2026-05-06 (decision documented in `release-deploy-standard.md` §9 + this gap log)
**Priority:** 🟠 P1 Meta (governance — clarifies agent role in production deploy, prevents drift)
**Domain:** Architecture / DevOps / Governance
**Found:** 2026-05-06 (user feedback "có gaps đề cập đến việc dùng agent để deploy rồi chứ?")
**Affects:** Every future release deploy execution (v1.0.0+ và sau)

## Context

User flagged 2026-05-06 sau Release Lần 1 deploy plan PR (#833 with 12 deploy gaps): "có gaps đề cập đến việc dùng agent để deploy rồi chứ?".

State-check 2026-05-06:
- **GAP-103** (DONE 2026-04-18) shipped `deployment-strategy.md` + ADR-015 covering AWS Agent Plugins evaluation. Decision: DEFER Q3 2026
- **KHÔNG có gap dedicated** về "Claude Code subagents executing production deploy"
- Khác concept với ADR-015 (AWS official agents cho AWS operations) vs Claude Code subagents trong this project

## Problem

Project dùng Claude agents intensively cho coding waves (Wave 22-24: 67% PRs delivered via wave-pack agents). Production deploy hiện chưa decide:
- Có cho Claude agent execute deploy commands (terraform apply, helm upgrade, DNS cutover)?
- Hay human-in-the-loop required?
- Phạm vi prep work (gaps, plans, runbooks) vs execution work khác nhau thế nào?

Without explicit decision:
- Risk: ad-hoc agent invocations in production = audit trail gap
- Risk: agent makes unintended changes (per `feedback_worktree_absolute_path_contamination.md` precedent)
- Risk: blast radius too high for agent autonomy

## Decision (chốt 2026-05-06)

Mirror ADR-015 evaluation pattern. 4 phases of deploy lifecycle, agent role per phase:

| Phase | Agent role | Reasoning |
|---|---|---|
| **1. Deploy preparation** | ✅ ADOPT | Agent generates: gap files, deploy plans, runbooks, Helm values templates, smoke test scripts. Already proven Wave 22-24 pattern. High productivity, low risk (output reviewed pre-merge). |
| **2. Deploy execution** | ❌ SKIP | Critical commands (terraform apply, helm upgrade --install production, kubectl apply prod, DNS cutover, DB migrations on prod) MUST be human-executed. Production blast radius too high; audit trail requires human accountability per `output-review-mandate.md` Section 6. |
| **3. Post-deploy verification** | ✅ ADOPT | Agent runs smoke test scripts, parses logs, suggests fixes, updates status page. Read-only observation safe; speeds debugging significantly. |
| **4. Rollback decision** | ⚠️ HUMAN-IN-THE-LOOP | Agent can flag issues + recommend rollback BUT human decides rollback trigger. Rollback = irreversible state change; requires judgment. |

### Why this delineation

**Adopt prep + verification:**
- Wave-pack pattern already proven 5x leverage on coding tasks
- Plan generation + runbook drafting low-risk + reviewable pre-merge
- Smoke test + log parsing read-only — no state mutation
- Reduces coordinator overhead significantly

**Skip critical execution:**
- Production failures from agent action → no human accountability
- Agent bugs (per `feedback_worktree_absolute_path_contamination.md` precedent) — risk on prod = catastrophic
- Audit trail integrity per PDPL Art 11 (data processing accountability) requires identifiable human actor
- `output-review-mandate.md` Section 6 (Customer/legal-impact actions) = human-required
- Industry standard (AWS Well-Architected Operational Excellence pillar) recommends human approval gates for production changes

**Human-in-loop for rollback:**
- Rollback = data state change (DB migrations reverted, DNS cut, cache flushed)
- Wrong rollback = double damage
- Agent provides recommendation + evidence; human approves trigger

## Codification

Decision codified in `.claude/rules/release-deploy-standard.md` §9 — mandatory for all future release deploys.

Skill `.claude/skills/quality/release-deploy/SKILL.md` Stage 5 explicit guards:
> Agent CAN do: prep/observation
> Agent SHOULD NOT do: terraform apply, helm upgrade, kubectl apply (production), DNS cutover, prod DB migrations

## Acceptance Criteria

- [x] 4-phase agent role evaluation completed
- [x] Decision documented in `release-deploy-standard.md` §9 (paired same-PR)
- [x] Skill `quality/release-deploy/SKILL.md` Stage 5 guards (paired same-PR)
- [x] Anti-pattern §7 row "Agent execute production commands" added to rule
- [x] Cross-link to `output-review-mandate.md` Section 6 (human-required actions)
- [x] Cross-link to ADR-015 (AWS Agent Plugins similar evaluation)
- [x] Cross-link to memory `feedback_worktree_absolute_path_contamination.md` (precedent)
- [x] Self-test pattern: apply decision to Release 1 v1.0.0 deploy → agent does prep (this PR's content), human executes (when deploy time comes)

## Why P1 Meta (not P0)

- Meta governance — applies to every future deploy
- Not blocking current Release 1 work (prep phase already done by agents per existing wave-pack pattern)
- Important to lock now to prevent drift / inconsistent agent autonomy

## Effort estimate

~30 phút coordinator-only — decision + documentation. Already shipped trong this PR.

## Related

- **Companion:** `.claude/rules/release-deploy-standard.md` (this PR — codifies decision)
- **Companion:** `.claude/skills/quality/release-deploy/SKILL.md` (this PR — enforces decision in skill flow)
- **Sister evaluation:** `documents/02-architecture/adr/ADR-015-aws-agent-plugins-evaluation.md` (AWS Agent Plugins = DEFER Q3 2026)
- **Source-of-truth:** `documents/02-architecture/deployment-strategy.md` (GAP-103 DONE)
- **Precedent risk:** `feedback_worktree_absolute_path_contamination.md` (agent action propagation risk)
- **Audit governance:** `.claude/rules/output-review-mandate.md` Section 6 (human-required for legal/financial impact)
- **Wave-pack pattern:** `feedback_parallel_agent_strategy.md` (proven for coding; this gap delineates for deploy)

## Log

- **2026-05-06:** Filed + closed in same PR per user-flagged miss "có gaps đề cập đến việc dùng agent để deploy rồi chứ?". Decision documented + codified in rule + skill paired same-PR per `rule-change-process.md` §6.5 Enforcement Parity Mandate. Status DONE since decision is the deliverable (no further implementation needed; rule + skill enforce going forward).
