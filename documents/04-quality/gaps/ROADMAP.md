# Gaps Roadmap — Epic-Based Organization

**Mục tiêu:** Biến 47 gaps thành actionable roadmap với epics + dependencies + sprints.

> **Khi nào đọc file này thay vì README.md?**
> - README: flat index, tra cứu 1 gap
> - ROADMAP: execution planning, sprint planning, dependency check

---

## 1. Epic Taxonomy

47 gaps được group thành **10 epics**:

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

---

## 2. Epics Detailed

### Epic 1: Foundation Infrastructure
**Goal:** Setup prerequisites cho AI Branding implementation.
**Why first:** Các epic khác depend vào này.

| Gap | Title | Priority | Effort |
|-----|-------|:--------:|:------:|
| GAP-011 | Template library curation plan + review standards | 🔴 P0 | L |
| GAP-014 | Wave mock plan include AI branding | 🔴 P0 | M |
| GAP-015 | Tenant provisioning auto-trigger (event-driven) | 🔴 P0 | M |
| GAP-016 | Living docs impact scope | 🔴 P0 | S |
| GAP-046 | Design patterns applied systematically | 🟠 P1 | M |

**Dependencies:** None — starts immediately.

**Blocks:** Epic 2, Epic 4.

---

### Epic 2: Core AI Branding Pipeline
**Goal:** Build the actual AI branding feature (MVP).

| Gap | Title | Priority | Effort |
|-----|-------|:--------:|:------:|
| GAP-007 | Resource classification pipeline (static/template/AI) | 🔴 P0 | L |
| GAP-008 | AI Agent workflow (analyzer/planner/executor) | 🟠 P1 | XL |
| GAP-009 | Instance provisioning lifecycle (6 states) | 🟠 P1 | L |
| GAP-013 | Guided branding wizard UX | 🟠 P1 | L |
| GAP-031 | Expand wizard inputs beyond logo | 🔴 P0 | M |
| GAP-004 | Template-based image composition (Canva-like) | 🟡 P2 | L |

**Dependencies:** Epic 1 (GAP-011 templates must exist).
**Blocks:** Epic 3, Epic 4.

---

### Epic 3: AI Infrastructure
**Goal:** Scale, reliability, model management.

| Gap | Title | Priority | Effort |
|-----|-------|:--------:|:------:|
| GAP-005 | AI queue fair scheduling + capacity plan | 🔴 P0 | L |
| GAP-002 | Async pipeline for heavy AI tasks | 🟠 P1 | M |
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
| GAP-010 | Branding package API + KiteClass integration | 🟠 P1 | M |
| GAP-021 | Branding propagation to email + services | 🟠 P1 | M |
| GAP-037 | Branded auth flows (verify, reset pwd) | 🟠 P1 | S |
| GAP-032 | Branded error pages (404/500) | 🟠 P1 | S |
| GAP-039 | Webhook reliability (retry, idempotency) | 🟠 P1 | M |

**Dependencies:** Epic 2 (branding data), Epic 1 (infrastructure).

---

### Epic 5: Security & Compliance
**Goal:** Non-negotiable legal/security requirements.

| Gap | Title | Priority | Effort |
|-----|-------|:--------:|:------:|
| GAP-018 | Content safety & compliance (moderation, audit, GDPR) | 🔴 P0 | L |
| GAP-041 | Security hardening (SVG XSS, SSRF, CSRF, injection) | 🔴 P0 | M |
| GAP-042 | Legal/IP protection (trademark, DMCA, copyright) | 🔴 P0 | M |
| GAP-012 | Automated instance quality review | 🟠 P1 | M |

**Dependencies:** Can parallelize với Epic 2.
**Critical:** MUST complete before GA launch.

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
| GAP-033 | Branding version history & rollback (user) | 🟠 P1 | M |
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

Thay vì P0/P1/P2 flat, dùng **phase-based**:

| Tier | Description | Count |
|------|-------------|-------|
| 🟥 **Block GA** (must fix before launch) | Security, core pipeline, lifecycle | 17 gaps |
| 🟨 **Block GROWTH** (needed after GA within 3 months) | UX, conversion, ops | 18 gaps |
| 🟦 **Block SCALE** (needed after 10k users) | Multi-brand, marketplace, advanced | 12 gaps |

### 🟥 Block GA (17)

GAP-005, 007, 008, 009, 010, 011, 012, 013, 014, 015, 016, 018, 031, 041, 042, 046, 047

### 🟨 Block GROWTH (18)

GAP-002, 006, 017, 019, 020, 021, 023, 026, 032, 033, 036, 037, 038, 039, 040, 043, 004, 024

### 🟦 Block SCALE (12)

GAP-001, 003, 022, 025, 027, 028, 029, 030, 034, 035, 044, 045

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

**Last Updated:** 2026-04-14
