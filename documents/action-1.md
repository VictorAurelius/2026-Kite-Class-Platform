---
title: Action Log — User Instructions Backlog
purpose: Consolidated, thematic index of all user instructions accumulated across sessions. Used to audit coverage (what's been addressed vs what remains) and feed next-session prompts.
source: Raw user messages from prior sessions (chronological dump)
maintained_by: Claude (updated each major session)
last_reorganized: 2026-04-20
legend:
  - ✅ RESOLVED — gap closed or PR merged covering the ask
  - 🟡 IN-PROGRESS — gap filed, work underway
  - 🟠 OPEN — gap filed, no work yet
  - 🔵 NEW — candidate gap, not yet filed
  - 💬 QUESTION — diagnostic/exploratory question (no gap needed)
  - 📋 DECIDED — user made a decision captured below
---

# Action Log — Thematic Index

**Scope:** Re-organized from 702-line chronological dump into 14 scientific themes + decision log + appendix. Each item annotated with status + gap/PR reference where applicable.

**How to read:** Start with §0 Decisions Log for fixed user choices, then scan §1–§14 for theme-specific asks. Open items without a gap = candidates for §15 New Gap Candidates.

---

## Table of Contents

- [§0. Decisions Log (fixed user choices)](#0-decisions-log)
- [§1. PR / CI / Git Workflow Hygiene](#1-pr--ci--git-workflow-hygiene)
- [§2. Audit Coverage & Cadence](#2-audit-coverage--cadence)
- [§3. Business Logic — Structure, Review, Persona, BRD](#3-business-logic--structure-review-persona-brd)
- [§4. AI Branding — Architecture & Key-Feature Design](#4-ai-branding--architecture--key-feature-design)
- [§5. SaaS Lifecycle — Trial / Payment / Backup / Email](#5-saas-lifecycle--trial--payment--backup--email)
- [§6. SEO, Marketing Site, Domain Strategy](#6-seo-marketing-site-domain-strategy)
- [§7. Multi-Instance Stack Composition (FullStack test)](#7-multi-instance-stack-composition)
- [§8. Documentation Architecture & Living Docs](#8-documentation-architecture--living-docs)
- [§9. Skills / Rules / Starter-Kit Meta-Governance](#9-skills--rules--starter-kit-meta-governance)
- [§10. Dev Environment — Mock Data, Docker, AI Local](#10-dev-environment--mock-data-docker-ai-local)
- [§11. UI / UX — Template-Driven Frontend](#11-ui--ux--template-driven-frontend)
- [§12. Session Management & Parallel Agents](#12-session-management--parallel-agents)
- [§13. Deploy & Infrastructure — helm / k8s / terraform](#13-deploy--infrastructure)
- [§14. KiteClass & KiteHub Feature Gaps (non-AI)](#14-kiteclass--kitehub-feature-gaps)
- [§15. New Gap Candidates (from this reorganization)](#15-new-gap-candidates)
- [Appendix A. Raw → Reorganized Line Mapping](#appendix-a-raw--reorganized-line-mapping)

---

## §0. Decisions Log

User-confirmed choices — treat as final unless user reverses.

| # | Decision | Choice | Context |
|:-:|----------|--------|---------|
| D1 | Trial limit per owner | 1 lần (chuẩn SaaS) | §5 trial mechanics |
| D2 | Data retention after trial | Mua theo gói payment thay vì cố định | §5 config-driven |
| D3 | Backup retention for trial | **7 ngày** (giữ khách + 2 email warnings) | §5 trial → conversion |
| D4 | AI generation approach | **Template-first** (instant UX) | §4 AI branding |
| D5 | Blog platform for KiteHub marketing | **MDX** (in-repo, simple, free) | §6 SEO |
| D6 | Primary domain | **kitehub.vn** | §6 domain strategy |
| D7 | Free-form user prompt for AI branding | ❌ Banned (except Enterprise opt-in) | §4 AI branding |
| D8 | Docs layout for modules | Service `docs/` = quick-access only; deep docs → `documents/` | §8 docs architecture |
| D9 | Folder structure meta | Helm/k8s/terraform moved to `infrastructure/` | §13 done |
| D10 | Priority tier for gaps | Meta → Business-Logic → Feature (at each P-level) | `meta-gap-priority.md` §3 |
| D11 | KiteHub positioning | Not dashboard — real software-selling site with SEO | §6 marketing |
| D12 | Business-logic granularity | 3-layer (rules/use-cases/api-contract) per domain | §3 mandated |

---

## §1. PR / CI / Git Workflow Hygiene

| # | Ask (abridged) | Status | Reference |
|:-:|---------------|:------:|-----------|
| 1.1 | Monitor CI bằng script, không gh pr checks raw | ✅ | `scripts/check-ci.sh`, memory `reference_ci_script.md` |
| 1.2 | CI history clean-up (≤2 failed runs) | ✅ | `scripts/cleanup-ci-runs.sh`, memory `feedback_ci_history_hygiene.md` |
| 1.3 | Không Co-Authored-By trong commit | ✅ | CLAUDE.md §Commit, memory `feedback_no_coauthored_by.md` |
| 1.4 | `git pull --ff-only` bắt buộc | ✅ | memory `feedback_git_pull_ff_only.md` |
| 1.5 | Stacked PR `--delete-branch` trap (child auto-close) | ✅ | memory `feedback_stacked_pr_delete_branch.md` |
| 1.6 | Mọi thay đổi qua PR (kể cả docs-only) | ✅ | CLAUDE.md §Git Workflow, memory `feedback_always_pr_even_docs.md` |
| 1.7 | Commit PR-{N}.json log sau merge | ✅ | memory `feedback_pr_log_commit.md` |
| 1.8 | PR staged all vs từng file (khuyến khích explicit) | ✅ | CLAUDE.md §Git Safety Protocol |
| 1.9 | Squash-merge bắt buộc cho wave/feature branches | ✅ | CLAUDE.md §Wave Branch Strategy |
| 1.10 | MCP-first with CLI fallback (GitHub MCP, PG MCP) | ✅ | `.claude/rules/mcp-first-with-fallback.md` |

---

## §2. Audit Coverage & Cadence

| # | Ask | Status | Reference |
|:-:|-----|:------:|-----------|
| 2.1 | UI audit /128 per-screen | ✅ | `skills/quality/ui-review/SKILL.md` |
| 2.2 | Targeted audit (không full re-audit sau fix) | ✅ | memory `feedback_targeted_audit.md` |
| 2.3 | Quality audit /100 (10 categories) | ✅ | `skills/quality-audit/` |
| 2.4 | Business-logic ↔ code audit (mapping check) | ✅ | `skills/quality/business-logic-audit/` |
| 2.5 | Security audit /100 | ✅ | `skills/quality/security-audit/` |
| 2.6 | Performance audit /100 | ✅ | `skills/quality/performance-audit/` baseline 58/100 → 64/100 |
| 2.7 | Ops readiness audit /100 | ✅ | `skills/quality/ops-readiness-audit/` baseline 49/100 |
| 2.8 | API contract audit /100 | ✅ | `skills/quality/api-contract-audit/` |
| 2.9 | Audit skill grep scope (multi-module safety) | ✅ | GAP-149 closed, 5 skills hardened |
| 2.10 | Audit cadence enforcement (hook block non-compliant) | ✅ | `.claude/rules/post-wave-audit-mandate.md` + `audit-gate.py` |
| 2.11 | Audit compact avoidance (token optimization) | ✅ | per-skill token budgets tuned |
| 2.12 | Self-audit vs specialist calibration (delta 15-20pt) | ✅ | memory `feedback_audit_calibration.md` |
| 2.13 | Screenshots cần mock data (không errors states) | ✅ | memory `feedback_screenshot_mock_data.md`, MSW wired |
| 2.14 | Subagent audit parallel (4 agents) | ✅ | memory `feedback_subagent_audit.md` |
| 2.15 | UI audit pre-flight (Node 20, ports, playwright) | ✅ | memory `feedback_ui_audit_setup.md` |

---

## §3. Business Logic — Structure, Review, Persona, BRD

### 3.1 3-Layer structure (rules / use-cases / api-contract)

| # | Ask | Status | Reference |
|:-:|-----|:------:|-----------|
| 3.1.1 | Where to store business logic (single source of truth) | ✅ DECIDED D12 | `documents/01-business/{project}/{domain}/` |
| 3.1.2 | Detail level for FE/BE design (edit course → swap teacher UX) | ✅ | Layer 2 (use-cases.md) pattern |
| 3.1.3 | Create/update timing | ✅ | CLAUDE.md §Business Logic 3-Layer — "same PR as code change" |
| 3.1.4 | Each layer = separate file (not one giant file) | ✅ | Rule enforced by 3-file structure |
| 3.1.5 | Verification chain: BR-xxx → UC-xxx → endpoint → @Mapping → @Test | ✅ | Pre-flight check skill |
| 3.1.6 | Scattered docs (kiteclass-core/docs, kiteclass-frontend/docs, …) | ✅ DECIDED D8 | GAP-101 / docs-folder-structure rule |

### 3.2 Business-logic correctness (end-user perspective review)

| # | Ask | Status | Reference |
|:-:|-----|:------:|-----------|
| 3.2.1 | Review "thing right vs right thing" | 🟡 | GAP-049 (correctness), GAP-050 (persona) |
| 3.2.2 | Persona-based role-play review | 🟡 | `skills/quality/persona-based-business-review.md` |
| 3.2.3 | Example case: xlsx import missing for 500-student school | ✅ | GAP-051 (bulk import xlsx), GAP-137 (FE UI), GAP-109 (rules undocumented) |
| 3.2.4 | Per-persona acceptance criteria template | 🟡 | GAP-151 (template + 4 Tier-1 AC docs skeleton) |
| 3.2.5 | Execute persona review round 1 | 🟠 | GAP-152 (blocked by 151 + 153) |
| 3.2.6 | Secondary personas (Student/Parent/Teacher/Admin) | 🟡 | GAP-153 (matrix 9 personas × 4 tenant contexts) |
| 3.2.7 | BRD for student subject | 🟠 | GAP-153 includes Student persona AC doc |

### 3.3 BRD scope expansion

| # | Ask | Status | Reference |
|:-:|-----|:------:|-----------|
| 3.3.1 | BRD folder completeness (objectives, compliance, pricing, NFR, GTM) | 🟡 | GAP-150 (Phase 1: 5 strategic skeletons) |
| 3.3.2 | Simulation-based BRD gap finder | ✅ | `audits/business/brd-simulation-gap-finder-2026-04-20.md` (100+ cells) |
| 3.3.3 | Umbrella for 22 additional missing BRD docs | 🟡 | GAP-154 (umbrella) |
| 3.3.4 | Phase 1 P0 legal skeletons (TOS/AUP/Privacy/Refund/Retention/Billing/Child) | 🟠 | GAP-180..186 filed, Wave 8 assigned |

---

## §4. AI Branding — Architecture & Key-Feature Design

### 4.1 Core architecture

| # | Ask | Status | Reference |
|:-:|-----|:------:|-----------|
| 4.1.1 | Resource classification STATIC/TEMPLATE/FULL_AI | ✅ | GAP-007 done Wave 2+3 |
| 4.1.2 | Analyzer → Planner → Executor pipeline | ✅ | GAP-008 done Wave 3 |
| 4.1.3 | Instance lifecycle state machine (6 states) | ✅ | GAP-009 done Wave 2 |
| 4.1.4 | Template-first (Canva-style) before AI | ✅ DECIDED D4 | GAP-004 (P2 future) |
| 4.1.5 | Queue for AI (heavy async via RabbitMQ) | 🟡 | GAP-005 (fair scheduling Phase 2), GAP-104 (rules undocumented) |
| 4.1.6 | Multi-model (image gen fallback) | 🟠 | GAP-003 |
| 4.1.7 | Design pattern systematic application | 🟡 | GAP-046 (P1 meta) |

### 4.2 User-facing rules

| # | Ask | Status | Reference |
|:-:|-----|:------:|-----------|
| 4.2.1 | Free-form prompt (❌ except Enterprise) | ✅ DECIDED D7 | `.claude/rules/ai-branding-guidelines.md` §2 |
| 4.2.2 | 6+ template previews mandatory | ✅ | ai-branding-guidelines §2.2 |
| 4.2.3 | Wizard + per-resource approve | ✅ | GAP-013 done Wave 3 |
| 4.2.4 | Regenerate counter per tier | ✅ | ai-branding-guidelines §4.3 |
| 4.2.5 | Quality gate /100 before DEPLOY | 🟠 | GAP-012 planned |

### 4.3 Still-open concerns

| # | Ask | Status | Reference |
|:-:|-----|:------:|-----------|
| 4.3.1 | Capacity/throughput sizing (how many users before overload) | 🟠 | GAP-005 Phase 2 addresses |
| 4.3.2 | Template library curation (30 templates) | 🟠 | GAP-011 (P0 feature) |
| 4.3.3 | Wave mock include AI branding | 🟠 | GAP-014 (P0 feature) |
| 4.3.4 | Admin console for branding | 🟠 | GAP-068 |
| 4.3.5 | Scheduled rebrand (academic year refresh) | 🟠 | GAP-072 |

---

## §5. SaaS Lifecycle — Trial / Payment / Backup / Email

### 5.1 Trial mechanics

| # | Ask | Status | Reference |
|:-:|-----|:------:|-----------|
| 5.1.1 | 1 trial per owner (no re-trial) | ✅ DECIDED D1 | GAP-092 done |
| 5.1.2 | 7-day backup after trial ends (2 email warnings) | ✅ DECIDED D3 | GAP-184 (retention policy skeleton) |
| 5.1.3 | Config-driven (not hardcoded trial days / backup days) | 🟡 | GAP-108 (payment/invoice config hardcoded), GAP-184 retention config keys |
| 5.1.4 | Trial → paid handoff (no downtime) | 🟠 | needs audit — see §15.C |

### 5.2 Backup / Restore

| # | Ask | Status | Reference |
|:-:|-----|:------:|-----------|
| 5.2.1 | Backup not functional | ✅ | GAP-093 done |
| 5.2.2 | Hard delete not implemented | ✅ | GAP-094 done |
| 5.2.3 | Restore drill test | 🟠 | GAP-117 |
| 5.2.4 | MinIO backup strategy | 🟠 | GAP-118 |

### 5.3 Email notifications

| # | Ask | Status | Reference |
|:-:|-----|:------:|-----------|
| 5.3.1 | Trigger points (register / near-trial-end / upgrade / near-expiry / ...) | ✅ | GAP-095/096/097 done |
| 5.3.2 | Batch vs real-time tech choice | ✅ | GAP-097 (RabbitMQ queue) |
| 5.3.3 | Template existence + brand propagation | ✅ | GAP-021 done Wave 4 |
| 5.3.4 | Idempotency guard | ✅ | GAP-091 done |
| 5.3.5 | Admin controls + monitoring dashboard | ✅ | GAP-096 done |
| 5.3.6 | Notification settings API | 🟠 | GAP-098 (P2) |
| 5.3.7 | Email template review standard | 🟠 | GAP-173 |
| 5.3.8 | SMS/Zalo channels | 🟠 | GAP-063 |

---

## §6. SEO, Marketing Site, Domain Strategy

| # | Ask | Status | Reference |
|:-:|-----|:------:|-----------|
| 6.1 | KiteHub SEO optimization (positioning as product, not dashboard) | 🔵 NEW | §15.A candidate |
| 6.2 | Blog platform MDX in-repo | ✅ DECIDED D5 | §15.A candidate (implementation gap) |
| 6.3 | Domain kitehub.vn (primary) | ✅ DECIDED D6 | §15.B candidate |
| 6.4 | Instance kiteclass domain config (subdomain vs custom) | 🔵 NEW | §15.B candidate |
| 6.5 | Marketing copy + legal review | 🟠 | GAP-174 |

---

## §7. Multi-Instance Stack Composition

| # | Ask | Status | Reference |
|:-:|-----|:------:|-----------|
| 7.1 | Why only kiteclass-core + kiteclass-frontend run in local IT test (not gateway, redis, db, minio)? | ✅ | Shared infrastructure (`kite-` prefix) reused — documented in CLAUDE.md §Docker Naming |
| 7.2 | Fullstack naming unclear (group + service) | ✅ | Naming convention enforced (`kite-` / `kitehub-` / `kiteclass-`) |
| 7.3 | Canonical compose file | ✅ | `kitehub/docker-compose.kitehub.yml` |

---

## §8. Documentation Architecture & Living Docs

| # | Ask | Status | Reference |
|:-:|-----|:------:|-----------|
| 8.1 | README.md outdated (e.g. "KiteHub Setup Guide _(future)_") | ✅ | README refreshed 2026-04-19 |
| 8.2 | Scattered docs at kiteclass/docs, kiteclass-core/docs, … | ✅ DECIDED D8 | GAP-101 restructure |
| 8.3 | Living docs rule (README/CLAUDE.md/business/skills-index) | ✅ | CLAUDE.md §Living Documents |
| 8.4 | documents/ folder README (generic rule) | ✅ | GAP-101 + docs-folder-structure.md |
| 8.5 | documents/05-guides is empty — purpose + roadmap | 🟠 | GAP-102 (guides kickoff) |
| 8.6 | documents/00-brd low priority — has gap coverage? | 🟡 | GAP-150/154 + GAP-180..186 |
| 8.7 | Duplicate numbering (e.g. 2 folders with same prefix) | ✅ | Restructure PR merged |
| 8.8 | Output-review-mandate coverage (every output has review standard) | 🟡 | GAP-048 umbrella, 170-175 sub-gaps |
| 8.9 | ADR for architecture / rules | 🟠 | GAP-171 (rules ADR), GAP-172 (architecture ADR) |
| 8.10 | Logs format standard | 🟠 | GAP-175 |

---

## §9. Skills / Rules / Starter-Kit Meta-Governance

| # | Ask | Status | Reference |
|:-:|-----|:------:|-----------|
| 9.1 | Skills phình to, không có index rõ ràng | ✅ | `.claude/skills/_README-skills-index.md` |
| 9.2 | Classify skills by category | ✅ | Index organized by 9 categories |
| 9.3 | Check skill conflicts | ✅ | Skill conventions rule |
| 9.4 | MiniMax skills review (document generation Excel/Word/PDF/PPT) | 🟠 | GAP-047 (P0 meta) |
| 9.5 | UI UX Pro Max skill integration | 🟠 | GAP-176 |
| 9.6 | Starter-kit remote sync strategy | ✅ | `.claude/rules/skill-conventions.md` §Remote Repo Sync |
| 9.7 | Starter-kit bulk update after many PRs | 🔵 NEW | §15.F candidate |
| 9.8 | Terraform cloud deploy skill adoption | ✅ | `skills/devops/terraform-cloud-deploy/` |
| 9.9 | Gap triage meta-rule (meta > business > feature) | ✅ | `.claude/rules/meta-gap-priority.md` (2026-04-20 tiers) |
| 9.10 | Output-review-mandate master rule | ✅ | `.claude/rules/output-review-mandate.md` |
| 9.11 | Audit-to-gap pipeline rule | ✅ | `.claude/rules/audit-to-gap-pipeline.md` |
| 9.12 | Session-start skill (new conversation prep) | 🔵 NEW | §15.D candidate |
| 9.13 | Script compliance check (shellcheck/ruff for bash/python) | 🔵 NEW | §15.E candidate |
| 9.14 | IDE warnings check before commit | ✅ | memory `feedback_ide_warnings_check.md` |
| 9.15 | PR lifecycle log self-test + IDE-warnings fields | ✅ | `audit-gate.py` hook updated |

---

## §10. Dev Environment — Mock Data, Docker, AI Local

| # | Ask | Status | Reference |
|:-:|-----|:------:|-----------|
| 10.1 | FE mock API (MSW) for decoupled dev | ✅ | Mock data wave shipped |
| 10.2 | BE DataSeeder (Spring Boot, toggle via env) | ✅ | GAP-014 (include AI branding mock) |
| 10.3 | Local Docker image mock data toggle | ✅ | Data seeder env flag |
| 10.4 | AI local (Ollama) vs OPENAI_API_KEY in E2E | ✅ | E2E fixed to use local-first |
| 10.5 | Docker resource limits | 🟠 | GAP-130 |
| 10.6 | E2E Docker test resources | ✅ | Docker compose documented |

---

## §11. UI / UX — Template-Driven Frontend

| # | Ask | Status | Reference |
|:-:|-----|:------:|-----------|
| 11.1 | Figma-template-based FE dev (vs free-form) | 🟠 | GAP-176 (ui-ux-pro-max-skill integration) |
| 11.2 | FE quality standards kit (sharable across projects) | ✅ | Starter-kit maintained |
| 11.3 | Register flow: center registration → kitehub link (local vs prod) | ✅ | fixed per-env config |
| 11.4 | Dark mode KiteHub not switching | ✅ | GAP-078 done |
| 11.5 | i18n gaps KiteClass | 🟡 | GAP-079 / 140 / 141 / 142 |
| 11.6 | Dashboard loading UX | 🟠 | GAP-080 |
| 11.7 | Landing hero duplicated text | ✅ | GAP-138 done |
| 11.8 | Parent dashboard MVP | 🟠 | GAP-139 |

---

## §12. Session Management & Parallel Agents

| # | Ask | Status | Reference |
|:-:|-----|:------:|-----------|
| 12.1 | Parallel-agent worktree isolation | ✅ | memory `feedback_parallel_agent_strategy.md` |
| 12.2 | Agent quality vs sequential (no regression?) | ✅ | Wave 4 retro verified |
| 12.3 | Multi-session concurrent — conflict prevention | 🔵 NEW | §15.D candidate (session-lock mechanism) |
| 12.4 | Session-start skill to load context | 🔵 NEW | §15.D candidate |
| 12.5 | Wave QA mandatory (no skip under time pressure) | ✅ | memory `feedback_wave_qa_mandatory.md` |
| 12.6 | Context-full regression (output quality degrades) | ✅ | memory + compact protocol |

---

## §13. Deploy & Infrastructure

| # | Ask | Status | Reference |
|:-:|-----|:------:|-----------|
| 13.1 | Helm / k8s / terraform folder layout | ✅ DECIDED D9 | moved to `infrastructure/` |
| 13.2 | Deploy philosophy ADR | 🟠 | GAP-103 |
| 13.3 | Monitoring stack in prod (Grafana / Loki / Alerts) | 🟠 | GAP-111..125, 143..145 |
| 13.4 | HPA / PDB / NetworkPolicy hardening | 🟠 | GAP-123 / 124 |
| 13.5 | Canary deployment | 🟠 | GAP-125 |
| 13.6 | Distributed tracing | 🟠 | GAP-112 |

---

## §14. KiteClass & KiteHub Feature Gaps

### 14.1 KiteClass (education platform)

| # | Ask | Status | Reference |
|:-:|-----|:------:|-----------|
| 14.1.1 | Bulk xlsx import users | 🟡 | GAP-051 / 109 / 137 |
| 14.1.2 | Parent portal | 🟠 | GAP-052 / 105 / 139 |
| 14.1.3 | Academic year / semester structure | 🟠 | GAP-053 |
| 14.1.4 | Multi-subject per student | 🟠 | GAP-054 |
| 14.1.5 | Official VN report card | 🟠 | GAP-055 |
| 14.1.6 | Homeroom teacher (GVCN) | 🟠 | GAP-056 |
| 14.1.7 | Payroll + commission | 🟠 | GAP-057 / 062 |
| 14.1.8 | Role hierarchy + org chart | 🟠 | GAP-058 |
| 14.1.9 | Student conduct tracking | 🟠 | GAP-059 |
| 14.1.10 | Period-based attendance | 🟠 | GAP-060 (calendar-mode variant not explicit — see §15.H) |
| 14.1.11 | Promotion/retention logic | 🟠 | GAP-061 |
| 14.1.12 | SCORM/xAPI compliance | 🟠 | GAP-064 |
| 14.1.13 | Structured class schedule | 🟠 | GAP-099 |
| 14.1.14 | Migration fresh-deploy safety | 🟠 | GAP-065 |

### 14.2 KiteHub (SaaS control plane)

| # | Ask | Status | Reference |
|:-:|-----|:------:|-----------|
| 14.2.1 | Unified reports dashboard | 🟠 | GAP-066 |
| 14.2.2 | Instance control plane (AWS-console-like) | 🟠 | GAP-067 |
| 14.2.3 | Admin branding console | 🟠 | GAP-068 |
| 14.2.4 | Industry-specific branding presets | 🟠 | GAP-069 |
| 14.2.5 | Concurrent rebrand race / approval | 🟠 | GAP-070 |
| 14.2.6 | Branding migration on tier change | 🟠 | GAP-071 |
| 14.2.7 | GDPR deletion of AI assets | 🟠 | GAP-073 |
| 14.2.8 | Developer sandbox tenant | 🟠 | GAP-075 |
| 14.2.9 | Admin dashboard unbounded findAll | ✅ | GAP-126 done |

---

## §15. New Gap Candidates

These are themes from action-1 with no existing gap coverage. Propose filing in next session.

### §15.A — KiteHub SEO + Marketing Site (NEW)
- **Scope:** SEO meta tags, blog MDX setup, sitemap, landing CTAs, structured data
- **Why:** User decision D11 — "KiteHub là trang bán sản phẩm thật sự, không phải dashboard"
- **Priority:** 🟠 P1 Business-Logic (positioning-critical but not GA-blocker)
- **Proposed ID:** GAP-190
- **Blocks:** GTM doc in GAP-150 (Phase 1 BRD skeleton)

### §15.B — Domain Registration & Instance DNS Strategy (NEW)
- **Scope:** kitehub.vn registration, DNS config, per-instance subdomain policy (e.g. `schoolA.kiteclass.com`), custom-domain support
- **Why:** User ask (line 61): "domain của kitehub và các instance kiteclass sẽ được đăng ký và cấu hình như thế nào"
- **Priority:** 🟠 P1 Business-Logic (tenant-onboarding blocker)
- **Proposed ID:** GAP-191

### §15.C — Trial → Paid Migration Zero-Downtime Design (NEW)
- **Scope:** Data handoff procedure, state machine, downtime SLA, rollback
- **Why:** User ask (line 33): "Quy trình để chuyển giao data từ trial lên payment đã rõ ràng chưa? có down time hay không?"
- **Priority:** 🔴 P0 Business-Logic (conversion-critical, SaaS standard)
- **Proposed ID:** GAP-192
- **Related:** GAP-092 (re-trial prevention done), GAP-093 (backup done), GAP-108 (config hardcoded)

### §15.D — Session Orchestration & Start-Session Skill (NEW META)
- **Scope:** (1) Skill to prepare fresh session (load CLAUDE.md, ROADMAP, active wave, open PRs), (2) multi-session conflict detection / lock file
- **Why:** User ask (line 607–627): "bắt đầu session mới như thế nào", "nhiều session cùng lúc được không", "cần kiểm soát session tránh conflict"
- **Priority:** 🟠 P1 Meta (quality-of-work enabler)
- **Proposed ID:** GAP-193

### §15.E — Bash / Python Script Compliance (shellcheck / ruff) (NEW META)
- **Scope:** CI integration of shellcheck for `scripts/*.sh`, ruff for `scripts/*.py`, + skill update in `script-review-checklist.md`
- **Why:** Explicitly flagged by user as "known limitation, improvement riêng" (line 544–546). `output-review-mandate.md` §5.5 lists it.
- **Priority:** 🟠 P1 Meta
- **Proposed ID:** GAP-194

### §15.F — Starter-Kit Bulk Retro-Sync (NEW META)
- **Scope:** After 100+ PRs in this project, distill learnings back into remote starter-kit (`github.com/VictorAurelius/claude-starter-kit`): new rules, skills, gotchas
- **Why:** User ask (line 653): "kế hoạch để update cho starter-kit sau rất nhiều PR/WAVE của dự án là gì?"
- **Priority:** 🟡 P2 Meta (has sync rule, missing retro-sync routine)
- **Proposed ID:** GAP-195

### §15.G — 9router Tool Evaluation (NEW)
- **Scope:** Evaluate `9router` tool (user mentioned line 427). Determine applicability to kitehub API gateway, document decision as ADR.
- **Why:** User asked for investigation; unclear if already closed or still open.
- **Priority:** 🟡 P2 Meta (investigation → ADR)
- **Proposed ID:** GAP-196

### §15.H — Attendance Calendar-Mode UI Variant (NEW)
- **Scope:** Calendar-view (month/week grid) for attendance, on top of period-based backend (GAP-060)
- **Why:** User ask (line 421): "điểm danh đang design thế nào, có cần làm mode kiểu calender không?"
- **Priority:** 🟡 P2 Feature (UX enhancement — backend logic exists via GAP-060)
- **Proposed ID:** GAP-197

### §15.I — FE ↔ BE Decoupled Mock Contract (NEW)
- **Scope:** Strengthen MSW mocks as formal contract tests; verify FE mock responses match BE actual responses via consumer-driven contract tests (Pact or similar). Mentioned at `output-review-mandate.md` as partial.
- **Why:** User ask (line 302): "FE phải có đủ bộ mock API cho BE", + api-contract partial status
- **Priority:** 🟡 P2 Meta (hardens contract audit)
- **Proposed ID:** GAP-198

### §15.J — Rework Audit Against Context-Degraded PRs (NEW)
- **Scope:** Systematically re-audit PRs merged during high-context-pressure sessions (detected by turn count + token usage) to catch quality regressions. PRs from Wave 6-8 era suspected.
- **Why:** User ask (line 611): "chất lượng output giảm do context quá đầy, làm thế nào để đánh giá PR/wave gần đây và xem có phải rework hay không?"
- **Priority:** 🟠 P1 Meta (quality assurance retroactive)
- **Proposed ID:** GAP-199

---

## Appendix A. Raw → Reorganized Line Mapping

**Deduplicated:** §3.1–§3.4 trial-email block repeated at raw lines 30–36, 321–327, 501–510 → consolidated into §5.1–§5.3.

**Dropped (pure session noise, preserved in git history):** lines 1–20 (CI monitoring micro-prompts), lines 230–250 (starter-kit interactive Q&A already captured in rule files).

**Preserved original chronological raw dump:** moved to `documents/07-archived/action-log-raw-2026-04-20.md` (if needed for forensic review; otherwise `git log documents/action-1.md` shows prior revisions).

---

## Appendix B. Update Protocol

1. **When user issues new instructions in a session:** append to relevant theme section.
2. **End of each wave:** re-classify items (RESOLVED → move to "closed" subsection).
3. **Pre-compact checkpoint:** ensure all open items have gap reference or §15 candidate row.
4. **Before GA:** all 🔵 NEW items must be either filed (→ 🟠 OPEN) or declined with rationale in decisions log.

---

## Log

- **2026-04-20:** Reorganized from 702-line chronological dump into 14 thematic sections + decision log + 10 new gap candidates (§15.A–§15.J). Dedupe removed 3 repeat blocks. No original user instruction lost — preserved via `git log` + this structured index.
