# Gaps Roadmap — Epic-Based Organization

**Mục tiêu:** Biến 103 gaps thành actionable roadmap với epics + dependencies + sprints.

> **Khi nào đọc file này thay vì README.md?**
> - README: flat index, tra cứu 1 gap
> - ROADMAP: execution planning, sprint planning, dependency check

---

## 🎯 Current Status Snapshot (2026-04-20)

**Progress:** 59/155 gaps CLOSED (38%). Waves 1-4 shipped. **Audit catch-up Part A — 5/5 COMPLETE** 2026-04-19. **Part B top-5 priorities — 5/5 SHIPPED** 2026-04-20 (PRs #371–#375) closing 9 gaps. **Re-audit validated 2026-04-20:** business-logic 65→**72** (+7), performance 58→**64** (+6). **Master plan merged PR #382** covers 92 open gaps across 12 waves (~2-3 months). **6 meta gaps tracked** (GAP-170–175) from output-review-mandate §4 VIOLATIONS → Wave 8b. **Part C Sprint 0 CLOSED** 2026-04-20 — GAP-149 (audit grep scope fix) closed, 5 audit skills hardened against multi-module false positives. Quality audit baseline 77/100 pending next refresh.

**GA Blockers remaining: 6 — ordered per `meta-gap-priority.md` (meta before feature within P0)**

| # | Gap | Title | Type | Status | Effort |
|:-:|-----|-------|:----:|:------:|:------:|
| 1 | **GAP-047** | Document generation skills (Excel/Word/PDF/PPT) — adopt from MiniMax | 🔴 Meta (skills) | 🔵 OPEN | XL |
| 2 | **GAP-046** | Design patterns applied systematically | 🔴 Meta (rules) | 🟡 PLANNED | M |
| 3 | **GAP-016** | Living docs impact scope (3-layer sweep) | 🔴 Meta (docs contract) | 🟡 PLANNED | S |
| 4 | GAP-011 | Template library curation (30 templates) | Feature | 🟡 PLANNED | L |
| 5 | GAP-014 | Wave mock plan include AI branding | Feature | 🟡 PLANNED | M |
| 6 | GAP-005 | AI queue fair scheduling (Phase 2) | Feature | 🟡 IN_PROGRESS | M |

> **Priority rule:** Meta-gaps (skills/rules/workflow) go first at each P-level — 1 broken skill/rule affects every future PR, so force multiplier first. Ref `.claude/rules/meta-gap-priority.md`.

**Epics fully closed:** Epic 5 (Security/Compliance), Epic 11 (SaaS Lifecycle Hardening), Epic 12 (Process/DevOps Maturity), Epic 13 (Frontend Quality — 4/5).

**Next recommended wave:** Wave 5 **GAP-047** document generation (highest priority meta gap) — splits into Sub-PR 5.1 PDF+Excel (P0), 5.2 Word (P1), 5.3 PPT (P2). Alternative if unblocked by dependencies: GAP-046 design-pattern audit + rules enforcement.

---

## 1. Epic Taxonomy

100 gaps được group thành **14 epics**:

| Epic | Theme | Gaps | Priority |
|------|-------|------|:--------:|
| [E1](#epic-1-foundation-infrastructure) | Foundation Infrastructure | 5 | 🔴 MUST FIRST |
| [E2](#epic-2-core-ai-branding-pipeline) | Core AI Branding Pipeline | 6 | 🔴 CORE |
| [E3](#epic-3-ai-infrastructure) | AI Infrastructure (model + queue) | 5 | 🟠 SCALE |
| [E4](#epic-4-integration--delivery) | Integration & Delivery | 5 | 🟠 DEPLOY |
| [E5](#epic-5-security--compliance) | Security & Compliance | 4 | 🔴 NON-NEG |
| [E6](#epic-6-operations--scale) | Operations & Scale | 5 | 🟠 PRODUCTION |
| [E7](#epic-7-ux--conversion) | UX & Conversion | 7 | 🟠 GROWTH |
| [E8](#epic-8-admin--support) | Admin & Support | 4 | 🟡 INTERNAL |
| [E9](#epic-9-developer-experience) | Developer Experience | 2 | 🟡 FUTURE |
| [E10](#epic-10-cross-cutting--architecture) | Cross-cutting & Architecture | 4 | 🟡 CLEANUP |
| [E11](#epic-11-saas-lifecycle-hardening) | SaaS Lifecycle Hardening | 7 | 🔴 BLOCK GA |
| [E12](#epic-12-process--devops-maturity) | Process & DevOps Maturity | 10 | 🟠 PRODUCTION |
| [E13](#epic-13-frontend-quality) | Frontend Quality | 5 | 🟠 GROWTH |
| [E14](#epic-14-quality-governance) | Quality Governance | 6 | 🟡 INTERNAL |

---

## 2. Epics Detailed

### Epic 1: Foundation Infrastructure
**Goal:** Setup prerequisites cho AI Branding implementation.
**Why first:** Các epic khác depend vào này.

| Gap | Title | Priority | Effort |
|-----|-------|:--------:|:------:|
| GAP-011 | Template library curation plan + review standards | 🔴 P0 | L |
| GAP-014 | Wave mock plan include AI branding | 🔴 P0 | M |
| GAP-015 ✅ | Tenant provisioning auto-trigger (event-driven) — DONE Wave 3 | 🟢 DONE | M |
| GAP-016 | Living docs impact scope | 🔴 P0 | S |
| GAP-046 | Design patterns applied systematically | 🟠 P1 | M |

**Dependencies:** None — starts immediately.

**Blocks:** Epic 2, Epic 4.

---

### Epic 2: Core AI Branding Pipeline
**Goal:** Build the actual AI branding feature (MVP).

| Gap | Title | Priority | Effort |
|-----|-------|:--------:|:------:|
| GAP-007 ✅ | Resource classification pipeline — DONE Wave 2+3 | 🟢 DONE | L |
| GAP-008 ✅ | AI Agent workflow (analyzer/planner/executor) — DONE Wave 3 | 🟢 DONE | XL |
| GAP-009 ✅ | Instance provisioning lifecycle (6 states) — DONE Wave 2 | 🟢 DONE | L |
| GAP-013 ✅ | Guided branding wizard UX — DONE Wave 3 | 🟢 DONE | L |
| GAP-031 ✅ | Expand wizard inputs beyond logo — DONE Wave 3 | 🟢 DONE | M |
| GAP-004 | Template-based image composition (Canva-like) | 🟡 P2 | L |

**Dependencies:** Epic 1 (GAP-011 templates must exist).
**Blocks:** Epic 3, Epic 4.

---

### Epic 3: AI Infrastructure
**Goal:** Scale, reliability, model management.

| Gap | Title | Priority | Effort |
|-----|-------|:--------:|:------:|
| GAP-005 🟡 | AI queue fair scheduling — Phase 1 DONE 2026-04-18, Phase 2 open | 🟡 IN_PROGRESS | L |
| GAP-002 ✅ | Async pipeline for heavy AI tasks — DONE Wave 3 (2026-04-18) | 🟢 DONE | M |
| GAP-006 | Upgrade AI models to Gemma 4 | 🟠 P1 | S |
| GAP-003 | Multi-tier image generation | 🟡 P2 | M |
| GAP-028 | AI model versioning & migration | 🟡 P2 | M |

**Dependencies:** Epic 2 (core pipeline).
**Blocks:** Epic 6 (ops).

---

### Epic 4: Integration & Delivery
**Goal:** Branding reaches users via multiple channels.

| Gap | Title | Priority | Effort |
|-----|-------|:--------:|:------:|
| GAP-010 ✅ | Branding package API + KiteClass integration — DONE Wave 3 | 🟢 DONE | M |
| GAP-021 ✅ | Branding propagation to email + services — DONE Wave 4 | 🟢 DONE | M |
| GAP-037 ✅ | Branded auth flows (verify, reset pwd) — DONE Wave 4 | 🟢 DONE | S |
| GAP-032 ✅ | Branded error pages (404/500) — DONE Wave 4 | 🟢 DONE | S |
| GAP-039 | Webhook reliability (retry, idempotency) | 🟠 P1 | M |

**Dependencies:** Epic 2 (branding data), Epic 1 (infrastructure).

---

### Epic 5: Security & Compliance
**Goal:** Non-negotiable legal/security requirements.

| Gap | Title | Priority | Effort |
|-----|-------|:--------:|:------:|
| GAP-018 ✅ | Content safety & compliance — DONE Wave 4 (MVP) | 🟢 DONE | L |
| GAP-041 ✅ | Security hardening (SVG XSS, SSRF, CSRF) — DONE Wave 4 | 🟢 DONE | M |
| GAP-042 ✅ | Legal/IP protection (DMCA workflow) — DONE Wave 4 | 🟢 DONE | M |
| GAP-012 ✅ | Automated instance quality review — DONE Wave 4 | 🟢 DONE | M |

**Dependencies:** Can parallelize với Epic 2.
**Status:** 🟢 All 4 gaps closed in Wave 4 (2026-04-14).

---

### Epic 6: Operations & Scale
**Goal:** Production readiness.

| Gap | Title | Priority | Effort |
|-----|-------|:--------:|:------:|
| GAP-019 | AI observability & cost monitoring | 🟠 P1 | M |
| GAP-043 | Performance protection (cache stampede) | 🟠 P1 | M |
| GAP-030 | Disaster recovery for AI branding | 🟡 P2 | M |
| GAP-044 | Synthetic monitoring + feature flags | 🟡 P2 | M |
| GAP-024 | Asset lifecycle & storage cleanup | 🟡 P2 | S |

**Dependencies:** Epic 3 (need real traffic to monitor).

---

### Epic 7: UX & Conversion
**Goal:** User experience + revenue optimization.

| Gap | Title | Priority | Effort |
|-----|-------|:--------:|:------:|
| GAP-020 | Wizard state persistence | 🟠 P1 | S |
| GAP-017 | AI usage → billing integration | 🟠 P1 | M |
| GAP-026 | Trial/freemium AI mechanics | 🟠 P1 | M |
| GAP-036 | Tier upgrade UX (reveal, teaser) | 🟠 P1 | M |
| GAP-033 | Branding version history & rollback (user) | 🟡 IN_PROGRESS (Wave 4 partial — manual rollback done; auto + A/B deferred) | M |
| GAP-034 | Branding export pack (ZIP + PDF) | 🟡 P2 | M |
| GAP-025 | Mobile-first wizard UX | 🟡 P2 | M |

**Dependencies:** Epic 2, Epic 4.

---

### Epic 8: Admin & Support
**Goal:** Internal tools for operations team.

| Gap | Title | Priority | Effort |
|-----|-------|:--------:|:------:|
| GAP-023 | Admin moderation tools | 🟠 P1 | L |
| GAP-040 | Support impersonation & diagnostics | 🟠 P1 | M |
| GAP-022 | Template analytics & A/B | 🟡 P2 | M |
| GAP-029 | Quality gate calibration | 🟡 P2 | S |

**Dependencies:** Epic 5 (audit logs), Epic 6 (monitoring infra).

---

### Epic 9: Developer Experience
**Goal:** Open ecosystem for integrations.

| Gap | Title | Priority | Effort |
|-----|-------|:--------:|:------:|
| GAP-038 | Developer API docs + SDK libraries | 🟠 P1 | L |
| GAP-045 | Template marketplace (community) | 🟡 P2 | XL |

**Dependencies:** Epic 4 (stable APIs).
**Note:** Can defer until post-GA.

---

### Epic 10: Cross-cutting & Architecture
**Goal:** Platform-wide concerns, cleanup.

| Gap | Title | Priority | Effort |
|-----|-------|:--------:|:------:|
| GAP-047 | Document generation skills (Excel/Word/PDF/PPT) | 🔴 P0 | XL |
| GAP-001 | kiteclass-gateway decision | 🟡 P2 | S |
| GAP-027 | Multi-brand per tenant (franchise) | 🟡 P2 | XL |
| GAP-035 | Wizard team collaboration | 🟡 P2 | L |

**Dependencies:** Mixed — document gen crosses all, multi-brand ties to all.

---

## 3. Dependency Graph

```
                ┌──────────────────┐
                │ Epic 1 Foundation │ ←── MUST START FIRST
                └─────────┬────────┘
                          │
              ┌───────────┴───────────┐
              ▼                       ▼
    ┌─────────────────┐    ┌──────────────────┐
    │  Epic 2 Core    │    │ Epic 5 Security  │ ←── PARALLEL
    │  Pipeline       │    │ & Compliance     │
    └────────┬────────┘    └─────────┬────────┘
             │                       │
   ┌─────────┼─────────┐             │
   ▼         ▼         ▼             │
 ┌────┐   ┌────┐    ┌────┐          │
 │ E3 │   │ E4 │    │ E7 │          │
 │ AI │   │Int.│    │ UX │          │
 │Inf.│   │    │    │    │          │
 └─┬──┘   └──┬─┘    └──┬─┘          │
   │         │          │            │
   └────┬────┴──────────┴────────────┘
        ▼
   ┌──────────────┐
   │ Epic 6 Ops   │ ←── Needs Epic 3, 4
   │ & Scale      │
   └──────┬───────┘
          │
          ▼
   ┌──────────────┐     ┌──────────────┐
   │ Epic 8 Admin │     │ Epic 9 DX    │
   │ & Support    │     │ (defer)      │
   └──────────────┘     └──────────────┘

   ┌──────────────┐
   │ Epic 10 X-cut│ ←── Can parallelize with most
   └──────────────┘
```

---

## 4. Sprint Roadmap

### 🚀 Sprint 0: Foundation (2 weeks) — MUST DO FIRST

**Goal:** Unblock all future work.
**Gaps:** GAP-011, 014, 016, 046
**Deliverables:**
- 30 initial templates curated
- Wave mock plan finalized
- Business docs updated
- Design pattern rules enforced

### 🚀 Sprint 1: MVP Pipeline (3 weeks)

**Goal:** End-to-end branding generation works.
**Gaps:** GAP-007, 008 (partial), 013, 031, 015
**Deliverables:**
- Resource router working
- Wizard with rich inputs
- Tenant created → auto-provision triggered
- First template-first branding generated

### 🚀 Sprint 2: Core Delivery (2 weeks)

**Goal:** Branding reaches users.
**Gaps:** GAP-009, 010, 032, 037
**Deliverables:**
- Lifecycle state machine
- Package API with ETag caching
- Branded error pages, auth flows
- Integration tests pass

### 🚀 Sprint 3: Security + Quality Gate (2 weeks) — PARALLEL with S1/S2

**Goal:** Non-negotiable compliance.
**Gaps:** GAP-018, 041, 012
**Deliverables:**
- Content moderation integrated
- Security hardening (SVG sanitize, SSRF protection, CSRF)
- Automated quality review in pipeline

### 🚀 Sprint 4: AI Scale (3 weeks)

**Goal:** Handle 100+ concurrent users.
**Gaps:** GAP-005, 002, 006 (Gemma 4 upgrade), 008 (finish)
**Deliverables:**
- RabbitMQ fair queue per tier
- Async image generation
- Gemma 4 in production

### 🚀 Sprint 5: UX Polish (2 weeks)

**Goal:** Conversion optimization.
**Gaps:** GAP-020, 021, 017, 026, 036
**Deliverables:**
- Wizard autosave/resume
- Email branding propagation
- Billing integration
- Trial mechanics + upgrade UX

### 🚀 Sprint 6: Ops Readiness (2 weeks)

**Goal:** Production launch ready.
**Gaps:** GAP-019, 043, 023, 042
**Deliverables:**
- Grafana dashboards
- Cache stampede protection
- Admin moderation UI
- Legal/IP framework

### 🚀 Sprint 7: Extended Features (flexible)

**Goal:** Enhancements based on feedback.
**Gaps:** Remaining P2 items (GAP-024, 025, 030, etc.)

### 🚀 Sprint 8+: Future / Nice-to-have

**Gaps:** GAP-027 (multi-brand), GAP-035 (collab), GAP-045 (marketplace), GAP-038 (SDK)

**Document Generation (GAP-047) — cross-cutting:**
Inject into Sprint 4-5 (invoice for billing, certificate for completion).

---

## 5. Critical Path

```
GAP-011 (templates) →
  GAP-007 (classification) →
    GAP-008 (agent) →
      GAP-009 (lifecycle) →
        GAP-010 (package API) →
          GAP-012 (quality gate) →
            [GA LAUNCH]
```

**Bottleneck:** GAP-011 (external dependency — designer) và GAP-008 (XL effort).

---

## 6. Effort Summary

| Size | Days | Gaps |
|------|------|------|
| S (Small, 1-3 days) | 3 | 5 gaps |
| M (Medium, 4-7 days) | 6 | 24 gaps |
| L (Large, 8-14 days) | 12 | 13 gaps |
| XL (Extra Large, 15+ days) | 20 | 5 gaps |

**Total estimated effort:** ~300 person-days (~6 months with 1 dev, ~2 months với 3 devs parallel).

---

## 7. Consolidation Opportunities

Some gaps có overlap, có thể merge:

| Candidates | Rationale |
|-----------|-----------|
| GAP-012 + GAP-029 | Both about quality review. Keep separate but implement together. |
| GAP-019 + GAP-044 | Both observability. Parts of same dashboard project. |
| GAP-032 + GAP-037 | Both branded pages (404/auth). Implement in 1 sprint together. |
| GAP-003 + GAP-028 | Both model versioning concerns. Unify when tackling. |
| GAP-018 + GAP-042 | Content safety + legal IP. Shared admin UI (GAP-023). |

**Don't merge** — track separately for clarity but implement in combined sprints.

---

## 8. Priority Tier Simplification

> **Superseded by refreshed tier table lower in file ("Updated Priority Tiers (103 gaps, refreshed 2026-04-18)").**
> Original Sprint 0-6 planning preserved here for historical context.

Original mapping (Wave 1 planning, pre-execution):

| Tier | Count (original plan) |
|------|-----------------------|
| 🟥 Block GA | 17 gaps |
| 🟨 Block GROWTH | 18 gaps |
| 🟦 Block SCALE | 12 gaps |

See refreshed counts + remaining-open list in §"Updated Priority Tiers" below.

---

## 9. Recommended Execution Model

**Team size scenarios:**

### Solo (1 dev, 6 months to GA)
- Strict sequential: Sprint 0 → 1 → 2 → 3 → 4 → 5 → 6
- Can't parallelize Epic 5 security
- Launch with 17 GA-blocker gaps closed

### Small team (3 devs, 2-3 months to GA)
- Parallel streams:
  - **Stream A (backend):** E1 → E2 → E3 → E6
  - **Stream B (frontend):** E1 → E2 wizard → E4 integration → E7 UX
  - **Stream C (security/ops):** E5 → E6 operations
- Launch with 25 gaps closed (GA + early growth)

### Full team (5+ devs, 1-2 months)
- All streams parallel
- Dedicated security team for Epic 5
- Launch with 30+ gaps closed

---

## 10. What To Do Right Now (Action Items)

1. **Approve roadmap** — user review this doc
2. **Assign Sprint 0 tasks** — GAP-011 (hire designer), GAP-014/016 (docs), GAP-046 (architecture)
3. **Set launch target date** — based on team size scenario
4. **Create tracking** — Linear/Jira/GitHub project với epics as milestones
5. **Cadence** — weekly sprint review, biweekly retro
6. **Dependency watchers** — alert when blocker resolved

---

## 11. Related Files

- `README.md` — flat index of all 47 gaps
- `_TEMPLATE.md` — template for new gaps
- Per-gap details: `GAP-XXX-*.md`
- AI Branding master design: `documents/02-architecture/ai-branding-v2-redesign.md`
- Design patterns: `documents/02-architecture/ai-branding-design-patterns.md`
- MiniMax skills analysis: `documents/04-quality/skills-gap-analysis-vs-minimax.md`

---

## 12. Progress Log

### Wave 2 — Data Model Foundation — 🟢 COMPLETE (2026-04-14)

7 sub-PRs merged sequentially:

| Sub-PR | PR | Gap | Status |
|--------|----|-----|--------|
| 2.1 ADRs (5 architectural decisions) | #271 | — | 🟢 |
| 2.2 Academic Year + Semester + Holiday | #273 | GAP-053 | 🟢 |
| 2.3 K-12 Multi-Subject Model | #275 | GAP-054 | 🟢 |
| 2.4 Role Hierarchy + Permissions | #276 | GAP-058 | 🟢 |
| 2.5 Instance Provisioning Lifecycle | #277 | GAP-009 | 🟢 |
| 2.6 Resource Classification Pipeline | #278 | GAP-007 | 🟢 |
| 2.7 Integration + Wave Completion | (this PR) | — | 🟢 |

**Wave 2 Gaps closed:** GAP-053, GAP-054, GAP-058, GAP-009, GAP-007

Deferred items from Wave 2 all landed in Wave 3: REST controllers (3.4), outbox foundation (3.1), concrete resource handlers (3.3), MinIO layout (3.3), internal webhooks (3.4).

### Wave 3 — AI Branding Core Pipeline — 🟢 COMPLETE (2026-04-14)

8 sub-PRs merged sequentially:

| Sub-PR | PR | Gaps addressed |
|--------|----|----|
| 3.1 ADRs (006-009) + Transactional Outbox foundation | #284 | — |
| 3.2 AI Provider adapter + Resilience4j | #285 | — |
| 3.3 Resource Handlers + MinIO storage layout | #286 | GAP-007 (completed) |
| 3.4 REST + Package API + webhook | #287 | GAP-010 ✅ |
| 3.5 AI Agent workflow + GAP-070 rebrand approval | #288 | GAP-008 ✅ GAP-070 ✅ |
| 3.6 Tenant Provisioning Saga | #289 | GAP-015 ✅ |
| 3.7 Guided Wizard UX | #290 | GAP-013 ✅ GAP-031 ✅ GAP-069 ✅ |
| 3.8 Integration + Wave Completion | (this PR) | 🟢 all closed |

**Wave 3 Gaps closed:** GAP-007 (full), GAP-008, GAP-010, GAP-013, GAP-015, GAP-031, GAP-069, GAP-070

Patterns landed: Outbox, Adapter, Strategy, Decorator, Command, Composite, Saga, State Pattern (×2), Builder, Proxy, Optimistic Lock, XState-style FSM (FE reducer).

Deferred to follow-up PRs / later waves (see `03-planning/wave-03-ai-branding-core.md` §Deferred): RabbitMQ consumer wiring, async generate Steps, real Ollama HTTP, REST for rebrand-approvals, Playwright E2E, SSE live progress.

### Wave 4 — Security & Compliance — 🟢 COMPLETE (2026-04-14, parallel-agent)

**First wave at this repo using parallel-agent execution** (worktree-isolated). 6 sub-PRs:

| Sub-PR | PR | Mode | Gaps addressed |
|--------|----|------|----------------|
| 4.0 Foundation + ADRs 010-013 | #294 | serialized (lead) | — |
| 4.1 Content Moderation | #297 | parallel agent #1 | GAP-018 ✅ |
| 4.2 Security Hardening (SVG/SSRF/CSRF) | #296 | parallel agent #2 | GAP-041 ✅ |
| 4.3 Legal/IP (DMCA + trademark) | #295 | parallel agent #3 | GAP-042 ✅ |
| 4.4 GDPR Deletion + retention | #298 | parallel agent #4 | GAP-073 ✅ |
| 4.5 Quality Gate | #299 | serialized (depends on 4.1) | GAP-012 ✅ |
| 4.6 Integration + Wave Completion | (this PR) | serialized | 🟢 all closed |

**Wave 4 Gaps closed:** GAP-012, GAP-018, GAP-041, GAP-042, GAP-073

Wall-clock vs serial: 4 middle sub-PRs took ~20min agent work + ~90min human sequencing vs estimated ~5 days serial. 3 application.yml conflicts during sequencing (resolved each time). 1 CI failure (CSRF test-profile secret) — trivially fixed.

Patterns landed: AuditLog, State Pattern (×3 new — Moderation, DMCA, Deletion), Strategy (Quality checks ×5), Adapter (CSRF), Saga (DMCA workflow), Decorator/Sanitizer (SVG XSS), Validator (URL allowlist).

Deferred (see `03-planning/wave-04-security-compliance.md` §Deferred): real ML NSFW classifier, USPTO API, MinIO streaming export, scheduled expiry job, real contrast/screenshot/URL-ping checks, KiteHub admin UI hookups (slated for Wave 8).

**Next Wave:** Wave 5 K-12 Critical Features (unblocked from Wave 2) OR Wave 6 Ops Readiness OR quality-audit refresh.

---

## NEW EPICS (added 2026-04-16)

### Epic 11: SaaS Lifecycle Hardening
**Goal:** Business logic cho subscription/trial/retention THẬT SỰ hoạt động đúng.
**Why:** Deep audit phát hiện rules có nhưng code thiếu enforcement.

| Gap | Title | Priority | Effort | Dependency |
|-----|-------|:--------:|:------:|:----------:|
| GAP-092 | Re-trial prevention (TR-07 not in code) | 🔴 P0 | S | — |
| GAP-093 | Database backup only logs (not functional) | 🟢 DONE | L | — |
| GAP-091 | Email idempotency guard (2/13 types) | 🟢 DONE | S | — |
| GAP-094 | Hard delete not implemented | 🟢 DONE | M | GAP-093 |
| GAP-095 | Email failure retry mechanism | 🟢 DONE | M | GAP-097 |
| GAP-096 | Email admin controls + monitoring dashboard | 🟢 DONE | L | GAP-097 |
| GAP-097 | Email queue via RabbitMQ (replace direct HTTP) | 🟢 DONE | M | — |

**Dependencies:**
- GAP-093 → GAP-094 (backup trước, hard delete sau)
- GAP-097 → GAP-095, GAP-096 (queue infrastructure trước, retry + admin sau)
**Critical:** MUST complete before GA. Without GAP-093, data loss. Without GAP-097, emails unreliable.

---

### Epic 12: Process & DevOps Maturity
**Goal:** Process gaps cho production readiness — scripts, migrations, CI, deploy, incidents.

| Gap | Title | Priority | Effort | When |
|-----|-------|:--------:|:------:|:----:|
| GAP-081 ✅ | Script review checklist — DONE | 🟢 DONE | S | — |
| GAP-082 ✅ | Migration review checklist — DONE | 🟢 DONE | S | — |
| GAP-086 ✅ | Incident response runbook — DONE | 🟢 DONE | M | — |
| GAP-087 ✅ | Deploy go/no-go checklist — DONE | 🟢 DONE | M | — |
| GAP-088 ✅ | Rollback procedure per service — DONE | 🟢 DONE | L | — |
| GAP-083 ✅ | Gap triage process — DONE | 🟢 DONE | S | — |
| GAP-084 ✅ | CI failure triage — DONE | 🟢 DONE | M | — |
| GAP-085 ✅ | Cross-app consistency check — DONE | 🟢 DONE | M | — |
| GAP-089 ✅ | Post-deploy smoke test — DONE | 🟢 DONE | M | — |
| GAP-090 ✅ | API contract tests — DONE | 🟢 DONE | L | — |

**Status:** 🟢 Epic 12 fully closed (10/10 gaps DONE). Production-readiness governance in place.

---

### Epic 13: Frontend Quality
**Goal:** Fix UI issues từ UI audit.

| Gap | Title | Priority | Effort |
|-----|-------|:--------:|:------:|
| GAP-076 ✅ | KiteHub capture mock auth — DONE | 🟢 DONE | M |
| GAP-077 ✅ | KiteClass dev error overlay — DONE | 🟢 DONE | S |
| GAP-078 ✅ | KiteHub dark mode not switching — DONE | 🟢 DONE | M |
| GAP-079 ✅ | KiteClass i18n gaps — DONE | 🟢 DONE | M |
| GAP-080 | KiteHub dashboard loading/error UX | 🟡 P2 | M |

**Status:** 4/5 DONE. Only P2 GAP-080 open.

---

### Epic 14: Quality Governance
**Goal:** Meta-process — review standards cho outputs mà chưa có review process.

| Gap | Title | Priority | Effort |
|-----|-------|:--------:|:------:|
| GAP-048 ✅ | Output review standards coverage — DONE | 🟢 DONE | M |
| GAP-049 | Business logic correctness (stakeholder review) | 🟠 P1 | M |
| GAP-050 | Persona-based business review process | 🟡 PLANNED | S |
| GAP-101 ✅ | Docs folder README standardization (4 folders) — DONE PR #349 | 🟢 P3 | S |
| GAP-102 🟡 | 05-guides completion + ADR kickoff — PARTIAL (Part 2 DONE #350, Part 1 P2 DONE #352, Part 1 P1 open) | 🟡 P2 | M |
| GAP-103 ✅ | Deploy philosophy consolidation + AWS Agent Plugins ADR — DONE PR #351 | 🟢 P3 | M |
| GAP-149 ✅ | Audit skill grep scope multi-module (prevent GAP-107 false positive) — DONE 2026-04-20 Part C Sprint 0 | 🟢 DONE | S |

**Dependencies:** GAP-101 → GAP-102 (needs 05-guides README) → GAP-103 (needs ADR template + 02-architecture README).
**Split:** GAP-101 standalone PR. GAP-102 split Part 1 (guides) + Part 2 (ADR kickoff). GAP-103 after 101+102.

**Part C Sprint 0 (meta-skills calibration):** GAP-149 closed. 5 audit skills (business-logic, performance, ops-readiness, security, api-contract) now document safe grep scope patterns. Retroactive check confirmed GAP-106/108/110 are valid (not false positives).

---

## Updated Priority Tiers (103 gaps, refreshed 2026-04-18)

| Tier | Description | Count |
|------|-------------|-------|
| 🟥 **Block GA** (remaining open) | Core pipeline foundation + doc gen | **6 gaps** |
| 🟨 **Block GROWTH** (open) | UX, conversion, ops, webhooks | ~20 gaps |
| 🟦 **Block SCALE** (open) | Multi-brand, marketplace, advanced | ~12 gaps |
| ⬜ **Process/Internal** (open) | Advanced governance, persona review | ~4 gaps |
| ✅ **CLOSED** | Completed in Waves 1-4 + post-wave cleanup | **48 gaps** |

### 🟥 Block GA — Only 6 remain open (refresh 2026-04-18)

| Gap | Title | Status | Effort |
|-----|-------|:------:|:------:|
| GAP-005 | AI queue fair scheduling | 🟡 Phase 2 open | M remaining |
| GAP-011 | Template library curation (30 templates) | 🟡 PLANNED Sprint 0 | L |
| GAP-014 | Wave mock plan include AI branding | 🟡 PLANNED Sprint 0 | M |
| GAP-016 | Living docs impact scope (3-layer sweep) | 🟡 PLANNED Sprint 0 | S |
| GAP-046 | Design patterns applied systematically | 🟡 PLANNED Sprint 0 | M |
| GAP-047 | Document generation skills (Excel/Word/PDF/PPT) | 🔵 OPEN | XL |

**Previously listed GA blockers now CLOSED:** GAP-007, 008, 009, 010, 012, 013, 015, 018, 031, 041, 042, 081, 082, 086, 087, 088, 092, 093.

---

**Last Updated:** 2026-04-20 (Part C Sprint 0: GAP-149 audit grep scope meta-fix CLOSED; 59/155 DONE; 6 GA blockers remain — unchanged)

### Session 3 refresh 2026-04-18 — ROADMAP status audit

Discrepancies fixed:
- GAP-081, 082, 083, 084, 085, 086, 087, 088, 089, 090 — were listed as P0 Block GA / P1 pending, actually all DONE → Epic 12 fully closed
- GAP-076, 077, 078, 079 — were listed P0/P1, actually DONE → Epic 13 reduced to 1 open (P2)
- GAP-048 — Epic 14 governance, actually DONE
- GAP-007, 008, 009, 010, 012, 013, 015, 018, 031, 041, 042 — core AI branding + security gaps DONE Wave 2-4, epic tables updated inline
- GAP-002 — async pipeline DONE Wave 3 (2026-04-18)
- GAP-015 — tenant provisioning auto-trigger DONE Wave 3 (was in Epic 1 as open)
- Priority Tier counts: 95 → 103 total, Block GA 24 → 6 actual open, CLOSED 15 → 48

Triggered by: status check found 6+ "Block GA" gaps already merged but ROADMAP not refreshed since 2026-04-14 wave log entries.

### New gaps 2026-04-18 (TODO audit post Wave 4)

- **GAP-098** (P2) — Notification settings API not implemented — `InstanceTab.tsx:57`
- **GAP-099** (P2) — Structured class schedule (replace free-form text) — `SubjectSection.java:24`
- **GAP-100** (P3) — Lunar calendar for VN holidays — `VnHolidayProvider.java`

### New gaps 2026-04-18 (docs folder governance audit)

- **GAP-101** (P3) — Docs folder README standardization (4 folders: 00-brd, 02-architecture, 05-guides, 07-archived)
- **GAP-102** (P2) — 05-guides completion (6 operational guides) + ADR kickoff (template + ADR-001 jobs+RabbitMQ)
- **GAP-103** (P3) — Deploy philosophy consolidation + ADR-002 AWS Agent Plugins evaluation

### Planning docs added 2026-04-18

- `documents/03-planning/plans/plan-ui-ux-design-system-integration.md` — 3-PR plan to adopt ui-ux-pro-max reasoning rules + upgrade ui-review skill to /148 scoring
- `documents/03-planning/waves/wave-05-document-generation.md` — Wave 5 plan for GAP-047 (document generation skills adoption from MiniMax). **Status: 🟡 PLANNING** — awaiting user sign-off on 6 open questions (Section 9) before Sub-PR 5.0 starts.

### Rules added 2026-04-18

- `.claude/rules/docs-folder-structure.md` — generic rule extending `planning-docs-structure.md` pattern to all `documents/` folders (GAP-101)

**Prior:** 2026-04-16 (added Epics 11-14, 48 new gaps from UI/process/SaaS audits)

### Audit Catch-up 2026-04-19 — 3 baselines shipped (Part A 3/5) — 🟢 COMPLETE

Parallel-agent execution (3 worktree-isolated agents, ~10-11 min wall-clock each, zero conflicts). Conflict-control applied per `feedback_parallel_agent_strategy.md`: pre-assigned GAP ranges, parent-owned shared files (ROADMAP + output-review-mandate + MEMORY consolidated in this PR), parent-sequenced merges (3 clean FF merges).

| Audit | PR | Score | Grade | Gaps (range) |
|-------|:--:|:-----:|:-----:|--------------|
| business-logic /100 (refresh, 27d stale) | #366 | 65/100 | D | GAP-104 → GAP-110 (7) |
| ops-readiness /100 (first-ever baseline) | #365 | 49/100 | F | GAP-111 → GAP-125 (15) |
| performance /100 (first-ever baseline) | #364 | 58/100 | F | GAP-126 → GAP-135 (10) |

**32 new gaps created (GAP-104 → GAP-135).**

Top P0 findings (meta-gaps listed first per `meta-gap-priority.md`):
- **GAP-104** (P0 meta) — Wave 3 fair-queue Phase 1 shipped 8+ config keys, 0 BR-QUEUE-* rules. Living Docs contract broken.
- **GAP-105** (P0 meta) — `parent-portal` domain missing 3-layer docs despite `ParentPortalProperties.java:16` referencing `BR-PARENT-003` (ghost rule ID).
- **GAP-111** (P0) — Monitoring stack (Prometheus/Grafana) only in dev docker-compose; production Helm/k8s deploys blind.
- **GAP-120** (P0) — Alertmanager has 7 alert rules but 0 receiver configured — alerts would fire silent.
- **GAP-117** (P0) — Backup restore never tested (GAP-093 shipped pg_dump but no restore drill/runbook).
- **GAP-126** (P0) — Admin dashboard calls `findAll() × 2` on Instance + Subscription tables no-cache, 6 stream aggregations per request.
- **GAP-127** (P0) — Frontend 0 code-splitting across 64 pages; framer-motion (~130KB) + recharts (~180KB) in initial bundle (~400-550KB First Load JS).
- **GAP-129** (P0) — `BrandingPackage` accepts `instanceId` param but ignores it, returns cross-tenant findAll — perf + multi-tenancy bug.

Status changes applied in this consolidation PR (`.claude/rules/output-review-mandate.md` §3):
- business-logic: stale (27d) → CURRENT (2026-04-19)
- ops-readiness: VIOLATION (never audited) → BASELINE_CAPTURED (2026-04-19, 49/100)
- performance: PLANNED → BASELINE_CAPTURED (2026-04-19, 58/100)

**Remaining Part A audits (per plan `documents/03-planning/plans/plan-audit-catchup-2026-04-19.md`):**
- Audit 4: ui-review /128 (8d stale)
- Audit 5: quality-audit /100 refresh (depends on Audits 1-4 findings)

### Audit Catch-up Part A — 5/5 COMPLETE (2026-04-19) — 🟢 COMPLETE

Continuation of 3/5 entry above. Audits 4+5 shipped in same session:

| Audit | PR | Score | Gaps |
|-------|:--:|:-----:|------|
| ui-review /128 (refresh, 8d stale) | #368 | KC 81/128, KH 59/128 (+1 each) | GAP-136 → GAP-142 (7) |
| quality-audit /100 (refresh, final) | #369 | **77/100 C+** (Δ −18 vs 95/100) | — (no new gaps per plan §3.5) |

**Total Part A gaps: 39** (GAP-104 → GAP-142). Running total 48/142 closed (34%).

**Calibration insight (Audit 5 report):** −18 delta is NOT a regression in 5 days. The 95/100 on 2026-04-14 was optimistic self-audit without specialist data (ops, perf were never audited). The 77/100 today is the FIRST HONEST BASELINE with ground-truth evidence from 4 specialist audits. Future deltas measure genuine improvement against 77, not inflated 95.

**Top 5 next-wave priorities (meta-boost per `meta-gap-priority.md`):**
1. **GAP-104** Wave 3 BR-QUEUE rules (Meta P0, 4-6h) — Living Docs contract broken
2. **GAP-105** parent-portal 3-layer docs (Meta P0, 4-6h) — ghost rule reference
3. **GAP-136** KiteHub custom error pages (Feature P0, 2-3h) — 5+ routes return English 404
4. **GAP-111 + GAP-120** monitoring + alertmanager prod Helm (Feature P0, 1-2d) — ops visibility
5. **GAP-128/129/133/131 batch** perf quick wins (Feature P0/P1, 1d)

Expected recovery per Audit 5: 77 → 85 (B+) end Week 2, → 90 (A) end Week 4.

**Governance turnaround COMPLETE:** hook (PR #362) enforces freshness; 5 audits now FRESH; baselines captured for 2 never-audited categories (ops, perf). Part B (fix waves) tracked via top-5 priorities above.

### Audit Catch-up Part B — 5/5 top priorities SHIPPED (2026-04-20) — 🟢 COMPLETE

Parallel-agent execution continued from Part A. 5 worktree-isolated agents fixed the Audit 5 top-5 priorities simultaneously. Wall-clock: Agent A 6 min, C 7 min, B 8 min, D 15 min, E 69 min (Maven + testcontainers). Zero merge conflicts — disjoint file sets.

| PR | Gap(s) closed | Agent | Highlights |
|:--:|---------------|:-----:|------------|
| #371 | GAP-104 (Meta P0) | A | 18 BR-QUEUE rules + 4 UC-AGENT-08..11 + metrics catalogue |
| #373 | GAP-105 (Meta P0) | B | parent-portal 3-layer: 30 BR-PARENT + 6 UC-PARENT + 5 endpoints; BR-PARENT-003 verified |
| #372 | GAP-136 (P0) | C | 3 error pages (not-found/error/global-error) + 13/13 tests green, dark-mode + Vietnamese |
| #374 | GAP-111 + GAP-120 (P0, foundation) | D | Prometheus + Alertmanager Helm deps + ServiceMonitors; 3 follow-up gaps (GAP-143/144/145) |
| #375 | GAP-128 + GAP-129 + GAP-131 + GAP-133 (P0/P1) | E | Installment scan fix, BrandingPackage tenant isolation, 6/9 HTTP timeouts, Hibernate batch=50; 5 new test files, ~1430 tests green |

**Gaps closed in Part B: 9** (GAP-104, 105, 111, 120, 128, 129, 131, 133, 136) → progress 48/142 → 57/147 (39%).

**New follow-up gaps created: 5**
- GAP-143 Grafana Dashboards Helm (P1, from D)
- GAP-144 Alertmanager Production Receivers (P0, from D)
- GAP-145 Loki Tracing Stack (P2, from D)
- GAP-146 HTTP timeouts remainder — payment/email/captcha (P2, from E)
- GAP-147 KiteHub Admin OpenAPI bean conflict — pre-existing (P2, discovered by E)

**Top-3 residual GA risks** (to review next wave):
- GAP-144 Alertmanager receivers (needed before prod deploy — alerts still silent)
- GAP-127 FE code-splitting (64 pages, ~400-550KB First Load JS) — not in Part B scope
- GAP-126 Admin dashboard findAll cache — not in Part B scope

**Superpowers adherence:** All 5 agents followed brainstorm + task-breakdown + (TDD where code) + implementation + self-review. Agent C and E delivered tests alongside code (TDD). Agents D and E self-caught writing to main worktree by mistake (hard rule 3 from `feedback_parallel_agent_strategy.md`) — no contamination landed on main.

**Conflict-control effectiveness:** 4/5 agents zero-collision auto-FF merge. Agent E merged with local leftover from worktree-root confusion (cosmetic, discarded before pull). No PR-level conflicts.

### Re-audit 2026-04-20 — Part B impact validation — 🟢 COMPLETE

Ran 2 parallel re-audit agents after Part B merge to measure delta. First attempt crashed silently (both agents stopped ~21 min post-spawn, coincident with `mcp__ide__*` disconnect — unrelated infra issue). Respawn succeeded cleanly.

| Category | Baseline 2026-04-19 | Refresh 2026-04-20 | Δ | PR |
|----------|:-------------------:|:------------------:|:-:|:--:|
| business-logic /100 | 65 D | **72 C** | +7 | #379 |
| performance /100 | 58 F | **64 D** | +6 | #378 |

**Business-logic findings (PR #379):**
- 2 CLOSED: GAP-104 (Wave 3 BR-QUEUE verified), GAP-105 (parent-portal 3-layer verified)
- 1 FALSE POSITIVE retracted: **GAP-107** — baseline grep scope missed `kiteclass/kiteclass-core/`; `ResilientAIClient` + `MockAIClient` + `OllamaAIClient` all exist with correct `@Profile("ai-live")` wiring
- 1 NEW: **GAP-148** (P2) — `BR-QUEUE-015..018` circuit breaker config exists in kitehub-branding but 0 `@CircuitBreaker` annotation (dead config)
- 7 unchanged (GAP-106/108/109/110 + 3 minor)

**Performance findings (PR #378):**
- 3 CLOSED: GAP-128 (installment PK lookup), GAP-129 (BrandingPackage tenant + V45 index + regression test), GAP-133 (Hibernate batch=50 × 5 services)
- 1 PARTIAL: GAP-131 (6/9 sites; remainder → GAP-146)
- 6 UNCHANGED: GAP-126, 127, 130, 132, 134, 135 (not in Part B scope)
- 0 new gaps, 0 regressions
- Category deltas: DB +3, API +2, Cache 0, FE 0, Resource +1

**Lessons learned added to skill roadmap (future work):**
- Business-logic-audit skill needs explicit broader grep scope (not just `kitehub/` + `kiteclass/` top-level) — risked false-positive like GAP-107
- Re-audit pattern works: shows calibrated delta + flags regressions; took ~5-8 min per agent

**Cumulative progress after re-audit:**
- Progress 57/147 → 58/148 (GAP-107 closed, GAP-148 added)
- Quality-audit 77/100 unchanged (not refreshed this round)
- Next recovery milestone: 77 → ~80 B- after next sprint closing GAP-148 + GAP-146 + GAP-132 (1-2 days)
