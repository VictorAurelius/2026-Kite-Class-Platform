---
title: Wave 99B — Architecture docs sweep + expansion (7 buckets)
status: draft
created: 2026-05-19
updated: 2026-05-19
waves: [99b]
gaps: [GAP-668, GAP-669, GAP-670, GAP-671, GAP-672, GAP-673, GAP-674]
---

# Wave 99B — Architecture Docs Sweep + Expansion

**Goal:** Sweep stale `documents/02-architecture/` (56 files → cap-compliant 50) + ship 5 new high-blast-radius architecture reports per 3-agent outside-in consensus, to unblock dev onboarding + incident MTTR + compliance review.

**Trigger:** User 2026-05-19 flagged 4-bucket arch sweep scope (Mermaid render error fix shipped PR #1562; this wave handles remaining 3 buckets). Per `outside-in-coverage-trigger.md` v1.1.0 §2 — 3-agent outside-in audit ran (persona / external benchmark / failure-mode); convergent 5-report consensus + 4-report defer + anti-pattern guard (over-doc + volume cap).

**Estimated wall-clock:** ~5-7h agent work parallel, longest-bucket ~90min (B2 Compliance×Code map — manual table). Coordinator merge sequential.

---

## 1. Brainstorm (5-10 min)

**Q1 (alignment):** which personas / domains / waves does this serve?
- Persona 1 (new backend dev debug 403): B1 Service Catalog + Auth Flow + B3 Database Map
- Persona 2 (new frontend dev): B5 Golden-path Onboarding + B1 Service Catalog
- Persona 3 (SRE on-call): B1 Service Dependency + B2 SLO Registry
- Persona 4 (tech lead review): B2 Compliance × Code Map + B3 Database Map
- Wave 100+ unlocks: B1 + B2 + B3 = foundation for v1.0.0-rc readiness reports
- Cross-wave reference: Wave 96 PR2 shipped 3 base arch reports (kitehub/kiteclass/multi-tenant); this wave EXTENDS not duplicates

**Q2 (trade-offs):** what alternatives were considered and rejected, and why?
- 10-report inside-out proposal (Claude) — REJECTED per outside-in convergent "defer 4 reports" (cost / user-tier / AI orchestration / sequence catalog) per `outside-in-coverage-trigger.md` v1.1.0 §3
- Single mega-report (single 100-page arch doc) — REJECTED per arc42 + GitLab anti-pattern (stale-by-design); use multi-file with index
- C4 model L1-L4 full coverage — REJECTED per Benchmark "L1+L2 only, defer L3+L4 to docstrings"
- Skip volume cap compliance (`docs-folder-volume-budget.md`) — REJECTED per all 3 agents' anti-pattern warning; sweep B6 FIRST
- Per-tenant unit cost report — DEFERRED Phase 2 (AWS Free Tier now; cost model premature per Failure-Mode + Persona convergence)
- User tier FREE/PAID/ENTERPRISE matrix — DEFERRED (no ENTERPRISE in Phase 1 BETA per Persona warning)

**Q3 (risks):** what could go wrong; how does each bucket recover?
- **R1: Volume cap regression** — B0-B5 add 5 new files net (after B6 archive). Mitigation: B6 archives ≥6 files first (52 baseline before adds) per `docs-folder-volume-budget.md` cap 50.
- **R2: bg-agent context-thrashing** per Wave 97 lesson (Java code scope >6 file ops). Mitigation: B1+B3 are HEAVIEST (need code scan for service catalog + DB schema); spawn FOREGROUND (not bg-agent) per `feedback_parallel_agent_strategy.md`.
- **R3: Auto-gen vs hand-maintain decision drift** — Benchmark warned auto-gen feasible for B1+B3 (Backstage / Flyway pattern). Mitigation: ship v1 hand-written + flag auto-gen as Wave 100+ follow-up gap.
- **R4: New reports without `Last-Reviewed` field** — Persona Risk D + Benchmark anti-pattern. Mitigation: B0 sweep MANDATES Last-Reviewed backfill on all 24 non-ADR files (including new ones).
- **R5: PR review fatigue** — 7 buckets = 7 PRs minimum. Mitigation: coordinator sequential merge; B6 first → B0 second → B1-B5 parallel (5 PRs concurrent post-B0).

---

## 2. Task Breakdown

| Bucket | Gap | Owner | Effort | Disjoint? |
|--------|-----|-------|--------|-----------|
| B6 | GAP-668 | foreground agent | ~30min | ✅ archive script + folder moves only |
| B0 | GAP-669 | bg-agent | ~45min | ✅ frontmatter backfill 24 files (no content edit) |
| B1 | GAP-670 | foreground agent | ~90min | ✅ NEW file `02-architecture/service-catalog-and-auth-flow.md` |
| B2 | GAP-671 | foreground agent | ~90min | ✅ NEW file `02-architecture/compliance-control-map.md` |
| B3 | GAP-672 | foreground agent | ~75min | ✅ NEW file `02-architecture/database-architecture-map.md` |
| B4 | GAP-673 | bg-agent | ~45min | ✅ NEW file `02-architecture/c4-context-container.md` |
| B5 | GAP-674 | bg-agent | ~30min | ✅ REWRITE `02-architecture/README.md` |

**Disjoint check:** ✅ all 7 buckets touch distinct files; no overlap on edited paths. B6 archive moves are independent of B0-B5 new-doc adds.

---

## 3. Scope (compact schema)

**Stake tier (per `wave-pack-planner/SKILL.md` §Step 4.6):** MEDIUM → model: Opus medium (default; HIGH-stake reserved for B1+B2 cross-cutting governance — Opus full for those)
**Cross-layer? (per `wave-pack-planner/SKILL.md` §Step 4.5):** NO — docs-only scope; no code touched, no FE+BE bucket split needed. Skip Foundation Bucket 0.

> **Gap referencing convention** (per `.claude/rules/gap-architecture-v2.md`): IDs GAP-668..674 will be filed by coordinator in B6 PR (foundation). Use `bash scripts/query-gaps.sh <prefix>` post-filing to verify status sync.

| # | Bucket | Gap | Priority | Files (glob) | Spawn order |
|:-:|--------|-----|:--------:|--------------|:-----------:|
| 1 | **B6** Archive sweep (FOUNDATION — unblocks volume cap) | GAP-668 | 🟠 P1 META | `documents/02-architecture/*.md` (move >90d stale → `documents/02-architecture/closed/` or `documents/07-archived/architecture-2026-Q2/`) | Wave 1 FIRST |
| 2 | **B0** Last-Reviewed backfill + Mermaid migration audit | GAP-669 | 🟠 P1 META | `documents/02-architecture/*.md` (24 non-ADR files) frontmatter only | Wave 2 (after B6 merge) |
| 3 | **B1** Service Catalog + Dependency Graph + Auth Flow | GAP-670 | 🔴 P0 | `documents/02-architecture/service-catalog-and-auth-flow.md` (NEW); Mermaid component diagram + per-service table | Wave 2 parallel |
| 4 | **B2** Compliance × Code Map + SLO Registry | GAP-671 | 🔴 P0 | `documents/02-architecture/compliance-control-map.md` (NEW); table PDPL/PCI Art × code path × test evidence | Wave 2 parallel |
| 5 | **B3** Database Architecture Map (consolidated) | GAP-672 | 🔴 P0 | `documents/02-architecture/database-architecture-map.md` (NEW); entity catalog + FK graph + RLS map + Flyway history index | Wave 2 parallel |
| 6 | **B4** C4 Context + Container Diagram (L1+L2 only) | GAP-673 | 🟠 P1 | `documents/02-architecture/c4-context-container.md` (NEW); Mermaid 2 diagrams + narrative | Wave 2 parallel |
| 7 | **B5** Golden-path Onboarding Tour | GAP-674 | 🟠 P1 | `documents/02-architecture/README.md` (REWRITE — 89 lines stub → orchestrator 7-step reading order) | Wave 3 (after B0-B4 land — references new files) |

### Bucket B6 — Archive sweep (FOUNDATION)

- Files: scan `documents/02-architecture/*.md` (24 non-ADR files); identify files >90d old per `docs-archival-cadence.md` cadence OR superseded by Wave 96 PR2 reports; move to `documents/07-archived/architecture-2026-Q2/` OR consolidate
- Tests: post-archive count ≤50 per `docs-folder-volume-budget.md` cap (currently 56 = -6 minimum sweep)
- Acceptance: GAP-668 AC §all checked; CI `check-docs-folder-volume-budget.sh` PASS (when script lands)
- Output: archived files list + audit artifact `documents/04-quality/audits/meta/2026-05-19-arch-sweep-baseline.md`

### Bucket B0 — Last-Reviewed backfill + Mermaid migration audit

- Files: 24 non-ADR arch files (post-B6 ≤18 surviving + 5 NEW from B1-B5)
- Tests: every file has `last-reviewed:` frontmatter field per `rule-change-process.md` §3.5 pattern adapted for arch docs; Mermaid block syntax verified (no `<br/>` in stateDiagram — already fixed PR #1562, audit confirms)
- Acceptance: GAP-669 AC checked; 100% Last-Reviewed coverage on non-archived files
- Output: audit artifact noting any non-Mermaid diagrams found (PlantUML legacy → migration follow-up gap)

### Bucket B1 — Service Catalog + Dependency Graph + Auth Flow

- Files: `documents/02-architecture/service-catalog-and-auth-flow.md` (NEW)
- Contents (per outside-in consensus):
  - **Service catalog table** (Backstage pattern): 6 KiteHub services + 1 KiteClass core + 1 gateway + 2 frontends. Per row: name / repo path / port / owner / runbook link / health endpoint / on-call
  - **Dependency graph** (Mermaid `flowchart`): inter-service HTTP calls + RabbitMQ exchanges + DB connections
  - **Auth flow diagram** (Mermaid `sequenceDiagram`): browser → gateway → JWT validate → service @PreAuthorize → tenant context propagation per GAP-604 chain
  - **Role-guard matrix** (table): per-role permitted endpoints (anchors to GAP-518 + GAP-637 fixes)
- Tests: file renders correctly on GitHub Mermaid; cross-references existing ADR-011 + multi-tenant-architecture.md
- Acceptance: GAP-670 AC checked; 3-of-4 personas (Persona 1+2+3 from outside-in) cite this report as Top 3 need
- Closes incident classes: GAP-518 (role-guard discoverability) + 2026-05-16 admin-login (auth flow visibility)

### Bucket B2 — Compliance × Code Map + SLO Registry

- Files: `documents/02-architecture/compliance-control-map.md` (NEW)
- Contents:
  - **Compliance control table**: PDPL Art (8/9/11/14/15/20) + ISO27001 baseline + payment compliance → enforcement code path + test evidence
  - **SLO Registry**: per-service uptime / P99 latency / error-rate target (consolidate from scattered audit reports + ADR-028)
  - **NFR + Quality Attribute Registry** per arc42 §10
  - **Risk Register**: known tech debt + threat-model summary (links to existing `02-architecture/threat-models/*.md`)
- Tests: Tech Lead Persona 4 self-test — "review billing PR" → find applicable compliance rule in ≤5 min
- Acceptance: GAP-671 AC checked; cross-references all 3 existing threat-models + ADR-013 retention + GAP-156 quarterly audit

### Bucket B3 — Database Architecture Map

- Files: `documents/02-architecture/database-architecture-map.md` (NEW)
- Contents:
  - **Entity catalog**: all tables across kitehub + kiteclass — owner service + tenant_id presence + RLS policy active
  - **FK graph** (Mermaid `erDiagram` partial): top 30 entities + relationships
  - **Migration history index**: per-service Flyway V-files chronological + breaking-change flags
  - **Tenant_id propagation map**: which tables have it + RLS policy snippet per table cluster (extends multi-tenant-architecture.md §3)
  - **DB sizing baseline** (Phase 1 BETA): per-table row count + projected Phase 2 trajectory
- Tests: backend dev self-test — "where does table X live + has RLS?" → answer in ≤30 sec
- Acceptance: GAP-672 AC checked; cross-references ADR-001 k12 data model + multi-tenant-architecture.md + GAP-466 RLS implementation
- Auto-gen flag: post-merge file follow-up gap to auto-gen FK graph from Flyway parser (Backstage pattern per Benchmark agent recommendation)

### Bucket B4 — C4 Context + Container Diagram (L1+L2 only)

- Files: `documents/02-architecture/c4-context-container.md` (NEW)
- Contents:
  - **C4 Level 1 (System Context)**: KiteHub Platform + actors (P1/P2/P3 + Anonymous + Admin + Student + Parent) + external systems (Resend / AWS SES / VietQR / Zalo OA / Cloudflare DNS / Statuspage)
  - **C4 Level 2 (Container)**: 6 backend services + 2 frontends + shared infra (Postgres / RabbitMQ / Redis / MinIO / gateway)
  - **Mermaid syntax**: per `diagram-format-selection.md` v1.0.0 — Mermaid `flowchart TB` (PlantUML C4 deferred per "Mermaid default + GitHub native render")
  - Narrative explaining each container's responsibility
- Tests: render correctly on GitHub; junior dev onboarding "system mental model" check
- Acceptance: GAP-673 AC checked; L3+L4 explicitly DEFERRED per Benchmark "L1+L2 only" recommendation

### Bucket B5 — Golden-path Onboarding Tour (rewrite README)

- Files: `documents/02-architecture/README.md` (REWRITE — current 89 lines stub → orchestrator)
- Contents:
  - **Reading order tour** (7 steps): C4 (B4) → Service Catalog (B1) → Database Map (B3) → Multi-Tenant (existing) → Compliance Map (B2) → ADR index (existing) → Threat Models (existing)
  - **"Trace one request" tutorial**: walk through a specific user action end-to-end with links to relevant files
  - **Index by persona**: P1 backend / P2 frontend / P3 SRE / P4 tech lead → recommended reading list
  - **Last-Reviewed badge** + `audience: dev` frontmatter
- Tests: new-dev self-test — "I read this README + linked files, can I trace a feature end-to-end?"
- Acceptance: GAP-674 AC checked; replaces 89-line stub; references all B1-B4 new files
- Spawn order: AFTER B0-B4 land (depends on new file paths existing)

---

## 4. State-Check Evidence (per `audit-to-gap-pipeline.md` §2.6)

| Symbol referenced | Type | Verification | Verdict |
|---|---|---|---|
| `documents/02-architecture/multi-tenant-architecture.md` | Existing file | `ls` confirmed | ✅ exists (PR #1562 just fixed Mermaid in §2) |
| `documents/02-architecture/kitehub-architecture.md` | Existing file | `ls` confirmed | ✅ exists (Wave 96 PR2) |
| `documents/02-architecture/kiteclass-architecture.md` | Existing file | `ls` confirmed | ✅ exists (Wave 96 PR2) |
| `documents/02-architecture/adr/` 32 ADRs | Existing folder | `find` count = 32 | ✅ confirmed |
| `documents/02-architecture/threat-models/` 4 files | Existing folder | `find` count = 4 | ✅ confirmed |
| `documents/02-architecture/README.md` (89 lines) | Existing file | `wc -l` confirmed | ✅ exists (B5 rewrites) |
| `documents/02-architecture/service-catalog-and-auth-flow.md` | 🆕 to-be-created | grep returns 0 hits | 🆕 B1 creates |
| `documents/02-architecture/compliance-control-map.md` | 🆕 to-be-created | grep returns 0 hits | 🆕 B2 creates |
| `documents/02-architecture/database-architecture-map.md` | 🆕 to-be-created | grep returns 0 hits | 🆕 B3 creates |
| `documents/02-architecture/c4-context-container.md` | 🆕 to-be-created | grep returns 0 hits | 🆕 B4 creates |
| `documents/07-archived/architecture-2026-Q2/` | 🆕 to-be-created | dir not exist | 🆕 B6 creates (archive destination) |
| GAP-668..674 | 🆕 to-be-filed | gap-status.csv max 667 | 🆕 B6 PR files (foundation) |
| `docs-folder-volume-budget.md` cap 50 | Existing rule | rule body §2 confirmed | ✅ Wave 95 v1.0.0 |
| `docs-archival-cadence.md` cadence | Existing rule | rule body §2 confirmed | ✅ Wave 95 v1.0.0 |
| `diagram-format-selection.md` Mermaid default | Existing rule | rule body §2.4 confirmed | ✅ Wave 96 v1.0.0 |
| `outside-in-coverage-trigger.md` v1.1.0 | Existing rule | rule body §3 confirmed | ✅ Wave 93 |

**Verdict:** all referenced symbols verified ✅ or marked 🆕 to-be-created with explicit creating bucket. No phantom references.

---

## 5. Coordinator merge order

1. **B6** ship FIRST → archive >6 stale arch docs → volume cap compliant 50 → unblocks B0-B5 file adds
2. **B0** ship SECOND → Last-Reviewed backfill + Mermaid audit → baseline for new docs
3. **B1, B2, B3, B4** parallel after B0 merge → 4 concurrent agent spawns (NOT 5 — per `feedback_parallel_agent_strategy.md` 3-concurrent safe / 4+ rate-limit risk; stagger if Anthropic throttle hit)
4. **B5** ship LAST → references new file paths from B1-B4

Per `concurrent-production-mutation-ops.md` — N/A (docs-only, no production mutation).

---

## 6. Outside-in audit findings (informing scope)

3-agent outside-in run 2026-05-19 (per `outside-in-coverage-trigger.md` v1.1.0 Bước 1-5):

| Agent | Top 3 reports | Key finding |
|---|---|---|
| **Persona simulation** | Service Catalog + Auth / Runbook Index / Data Architecture | 4 personas converge on Service Catalog + Compliance map. Blind spots: Auth Flow Diagram + Bounded-Context map |
| **External benchmark** | C4 Context + Container / Backstage Service Catalog / Quality Attribute Registry | KiteHub MISSING C4 + Service Catalog (industry pillars); STRONG on ADRs + Deployment |
| **Failure-mode matrix** | Service Dependency Graph / Compliance Control Map / Per-tenant Unit Cost | 3am incident MTTR + compliance violation = highest blast radius. Volume cap warning. |

**Convergent (3-of-3):** Service catalog/dependency + Compliance/SLO registry + Anti-pattern warning (volume cap)
**2-of-3:** Database Map + Onboarding tour + C4 diagram
**Defer per consensus:** User tier matrix / Per-tenant cost / AI orchestration / Full sequence catalog

Audit artifacts (defer to actual file save in Wave 99B execution; outputs preserved in agent transcripts at `/tmp/.../tasks/{agentId}.output`).

---

## 7. Closure Protocol

Per `gap-done-discipline.md` + `wave-closure-scope-completeness.md` v1.0.0 + `post-merge-sync-completeness.md` + `post-wave-cleanup.md`:

- Each bucket PR updates affected GAP file Log + CSV row status + completion_pct
- ROADMAP §🚀 Next Action updated trong closure PR
- Wave plan frontmatter `status: complete` flip trong closure PR
- `wave-history.jsonl` append trong closure PR (Rule 15)
- **Scope-Completeness Reconciliation table per `wave-closure-scope-completeness.md` §3** — every §3 bucket categorized ✅/🟡/❌ + follow-up gap link
- Sub-gaps filed cho any deferral; PARTIAL exit-ramp per `gap-done-discipline.md` §3
- Run `bash scripts/prune-merged-worktrees.sh --yes` after all bucket PRs merged

**Post-wave audit suite cadence per `post-wave-audit-mandate.md` §2.2:** within 3 days post-Wave-99B-closure — UI /128 N/A (docs-only) + Quality /100 refresh + Business Logic /100 (compliance map B2 directly impacts).

---

## 8. Log

- **2026-05-19** (draft): Plan created. Triggered by user 2026-05-19 4-bucket arch sweep request (Item 1 Mermaid fix shipped PR #1562; this wave handles Items 2+3+4). Per `outside-in-coverage-trigger.md` v1.1.0 §2 — 3-agent outside-in audit ran first (Persona + External Benchmark + Failure-Mode); consensus 5-report scope + 4-report defer. Per `feedback_wave_plan_through_pr.md` — wave plan PR FIRST before agent spawn. Reviewer: @nguyenvankiet (solo-dev).
