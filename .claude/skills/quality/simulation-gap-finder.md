---
name: simulation-gap-finder
description: "Dùng khi user nói 'simulate gaps', 'persona simulation', 'tìm gaps bằng simulation', 'stress test design'. Tìm gaps bằng cách mô phỏng nhiều persona × stage × category."
user-invocable: true
---

# Skill: Simulation Gap Finder

**Version:** 1.0
**Created:** 2026-04-14
**Purpose:** Systematic method để tìm gaps trong design bằng cách mô phỏng nhiều persona × stage × category. Tránh miss gaps do shallow simulation.

---

## When to Use

- Design một feature lớn xong, muốn verify coverage toàn diện
- Đã phát hiện "vẫn còn gaps sau khi tưởng đã xong"
- Review plan trước khi implement
- Chuẩn bị wave lớn
- User critique "vẫn thiếu" sau redesign

## Usage

```
/simulate-gaps [feature-name]
```

---

## Framework: 3-Axis Coverage Matrix

**Chạy simulation với matrix 3 trục. Mỗi ô = 1 scenario cần check.**

### Axis 1: **Personas (5 roles)**

| Persona | Viewpoint | Touch feature khi |
|---------|-----------|-------------------|
| **Owner/Admin** | Primary user, tạo + manage | Signup, wizard, rebrand, billing |
| **End User** (student/teacher) | Consumer, không touch config | Daily use, see branded UI |
| **Platform Admin** | Oversight, compliance | Review, moderate, take-down |
| **Developer** | Integration, maintenance | API consumer, webhook, SDK |
| **Support Staff** | Troubleshooting tenant issues | Impersonation, diagnose |

### Axis 2: **Journey Stages (8 stages)**

| Stage | Key events |
|-------|-----------|
| 1. **Discovery** | Marketing, landing page, competitor compare |
| 2. **Signup/Onboarding** | Register, verify, first login |
| 3. **Configuration** | Wizard, form, settings |
| 4. **Provisioning** | Backend generation, deployment |
| 5. **Daily Usage** | Normal operations |
| 6. **Edge/Error** | Failures, edge cases, recovery |
| 7. **Evolution** | Update, rebrand, scale |
| 8. **Termination** | Churn, offboarding, deletion |

### Axis 3: **Concern Categories (10 categories)**

| # | Category | What to check |
|---|----------|---------------|
| C1 | **Functional** | Core feature works end-to-end |
| C2 | **UX** | Usability, accessibility, mobile, i18n |
| C3 | **Data** | Input validation, storage, migration, lifecycle |
| C4 | **Performance** | Latency, throughput, scaling, caching |
| C5 | **Security** | AuthZ, input validation, injection, DoS |
| C6 | **Compliance** | GDPR, legal, audit, TOS |
| C7 | **Operations** | Monitoring, alerts, runbooks, DR |
| C8 | **Integration** | APIs, events, downstream services |
| C9 | **Commercial** | Billing, upsell, analytics, conversion |
| C10 | **Evolution** | Versioning, migration, backward compat |

---

## Methodology (8 steps — Step 0 mandatory)

### Step 0 — Canonical-status lookup TRƯỚC khi emit candidates (BẮT BUỘC)

Skill này emit candidate list cho gap filing → MUST chạy state-check qua canonical CSV + ROADMAP §Dropped TRƯỚC khi output, KHÔNG phải sau khi user thấy report. Khác biệt:

- ❌ State-check AT filing time (legacy Step 7 / Pre-Flight line 124) = quá muộn — candidate đã emit ra report, user/Claude phải triage thủ công
- ✅ State-check AT emission time = filter trước khi emit, chỉ output genuinely new items

**Mandatory commands (read full output, KHÔNG `| head` truncate per `audit-to-gap-pipeline.md` §2.5 hardened protocol):**

```bash
# 1. List items đã shipped (skip nếu candidate match)
bash scripts/query-gaps.sh "" DONE ""

# 2. List items active scope (cross-ref candidate)
bash scripts/query-gaps.sh "" PARTIAL ""
bash scripts/query-gaps.sh "" OPEN ""
bash scripts/query-gaps.sh "" IN_PROGRESS ""

# 3. List items user-rejected
grep -E "Dropped:.*GAP-" documents/04-quality/gaps/ROADMAP.md
```

**Filter rule:** Emit candidate CHỈ khi:
- Title/scope KHÔNG match existing CSV row (any status)
- ID KHÔNG appear trong ROADMAP §Dropped section
- Attach evidence inline trong report: "Verified against gap-status.csv YYYY-MM-DD: GAP-XXX absent → genuinely new"

**Why mandatory:** 2026-04-20 audit `simulation-gap-finder` emit 12 candidates với 66% noise (7 shipped same week + 1 user-rejected vẫn list). Memory `feedback_audit_candidate_pre_filing_state_check.md` documents incident. Reference: `audit-to-gap-pipeline.md` §2.5 + `gap-architecture-v2.md` §1 + `pre-mutation-state-check.md` §1.

### Step 1: Define feature boundary
- What's in scope, what's not
- Related features it touches
- Users/systems affected

### Step 2: Build 3-axis matrix
- Fill table: Persona × Stage → all 10 categories
- 5 × 8 × 10 = 400 cells (don't need to fill all — skip irrelevant)

### Step 3: For each CELL, ask 3 questions
1. **Có scenario không?** (is there interaction at this intersection?)
2. **Design cover scenario đầy đủ?** (does current design handle it?)
3. **Gap gì nếu thiếu?** (what's missing?)

### Step 4: Walk through stages chronologically
- Stage 1 → 8 per persona
- Note gaps discovered
- Check for interactions between personas

### Step 5: Stress-test với edge cases
- What if X fails?
- What if Y scaled 100x?
- What if Z happens concurrently?
- What if malicious actor does W?

### Step 6: Cross-check against personas
- Each persona re-reads design — "Am I covered?"
- Opposing views (user vs admin conflict?)

### Step 7: Output structured gap list
- Classify each gap by category + priority
- Reference which cell in matrix discovered it
- Check duplicates against existing gaps

---

## Pre-Flight Checklist (trước khi declare "done")

- [ ] All 5 personas walked through all 8 stages
- [ ] All 10 concern categories checked
- [ ] Edge cases considered (failure, scale, concurrency, malice)
- [ ] Cross-persona interactions reviewed
- [ ] Evolution stage considered (not just initial build)
- [ ] Termination stage considered (not just happy path)
- [ ] Compliance/legal review (not just technical)
- [ ] Operations/monitoring (not just features)
- [ ] Each new gap cross-checked against existing gaps (no duplicates) — `audit-to-gap-pipeline.md` Step 2
- [ ] **Each new gap cross-checked against current codebase** — `audit-to-gap-pipeline.md` Step 2.5: grep actual code/infra/docs paths before filing. If partial implementation exists → file as 🟡 PARTIAL with `## Current State` table; if full → do NOT file.

---

## Common Gap Types (Checklist)

### UX Gaps (C2)
- [ ] Mobile-first vs desktop-only
- [ ] Keyboard navigation / accessibility
- [ ] Loading states, empty states, error states
- [ ] i18n / localization (RTL, diacritics, text expansion)
- [ ] Offline / network resilience
- [ ] State persistence (refresh, browser close)

### Data Gaps (C3)
- [ ] Input validation (client + server)
- [ ] Storage quota / lifecycle / cleanup
- [ ] Backup / restore / migration
- [ ] Versioning / history / rollback
- [ ] Orphan data cleanup

### Performance Gaps (C4)
- [ ] Caching strategy (multi-tier)
- [ ] Cache stampede / thundering herd
- [ ] CDN invalidation
- [ ] N+1 queries
- [ ] Async/queue for heavy ops

### Security Gaps (C5)
- [ ] AuthZ (tenant isolation)
- [ ] Input validation (XSS, SQL injection)
- [ ] File upload security (SVG XSS, virus, size)
- [ ] SSRF, CSRF
- [ ] Rate limit bypass
- [ ] Audit log

### Compliance Gaps (C6)
- [ ] GDPR (deletion, portability, consent)
- [ ] Terms of Service, AUP
- [ ] DMCA / copyright
- [ ] Audit trail retention
- [ ] Regional compliance (Vietnam data residency)

### Operations Gaps (C7)
- [ ] Monitoring dashboards
- [ ] Alerting rules
- [ ] Runbooks for incidents
- [ ] RTO/RPO definitions
- [ ] Backup strategy
- [ ] Deploy/rollback automation

### Integration Gaps (C8)
- [ ] API docs / SDK
- [ ] Webhook retry / idempotency
- [ ] Event schema evolution
- [ ] Downstream cache invalidation
- [ ] Third-party dependencies

### Commercial Gaps (C9)
- [ ] Billing integration
- [ ] Upsell / teaser flows
- [ ] Trial mechanics
- [ ] Analytics / metrics
- [ ] A/B testing

### Evolution Gaps (C10)
- [ ] Feature flags
- [ ] Versioning strategy
- [ ] Backward compatibility
- [ ] Deprecation timeline
- [ ] Migration playbook

---

## Example Application: AI Branding

Run skill qua feature AI Branding:

**Persona: End User (student/teacher)** × **Stage: Daily Usage**:
- C2 UX: Student thấy branded landing? ✓ covered (GAP-010)
- C2 UX: Student nhận welcome email có branding? ❓ → **GAP-021**
- C2 UX: Student thấy 404/500 pages có branding? ❌ → NEW GAP
- C5 Security: Student có thể thấy tenant khác không? ✓ tenant isolation

**Persona: Platform Admin** × **Stage: Evolution**:
- C7 Ops: Admin có thể rollback tenant branding khi bug? ❌ → NEW GAP (version history)

**Persona: Developer** × **Stage: Integration**:
- C8 Integration: Docs for consuming branding package API? ❌ → NEW GAP
- C8 Integration: SDK/client library? ❌ → NEW GAP

... and so on.

---

## Output Format

```markdown
# Gap Finder Report: [feature]

**Ngày:** YYYY-MM-DD
**Feature:** ...
**Method:** 3-axis simulation

## Matrix Coverage

| Persona × Stage | Covered | Gap | Category |
|-----------------|---------|-----|----------|
| Owner × Signup | ✓ | - | - |
| Owner × Wizard | ⚠️ | Missing state persist | C3 Data |
| End User × Daily | ❌ | Error pages no branding | C2 UX |
| ... | | | |

## New Gaps Found (not in existing list)

| ID | Title | Category | Priority |
|----|-------|----------|----------|
| GAP-XXX | ... | ... | ... |

## Recommended Actions

1. Create gap files for NEW gaps
2. Update existing gaps if scope changed
3. Verify no duplicates
```

---

## Rules

- ✅ Systematic — walk through matrix, don't skip
- ✅ Multi-persona — don't assume only 1 user type
- ✅ Evolution + Termination stages (beyond happy path)
- ✅ Stress test: failure, scale, concurrency, malice
- ❌ Không stop ở "enough" — complete matrix
- ❌ Không assume category (C1-C10) không apply — verify each

## Skill Contents

- This SKILL.md — methodology
- `reference/persona-profiles.md` — detailed persona descriptions (TODO)
- `reference/category-checklists.md` — deep-dive checklists per C1-C10 (TODO)
- `data/last-run.md` — previous simulation outputs (auto-generated)

---

## Gotchas

- **3-axis matrix has 5×8×10 = 400 cells** — running every cell exhaustively is impractical; sample diagonally (Persona A × Stage 2 × Category 3, then Persona B × Stage 5 × Category 7, ...) for first pass, then deep-dive only on cells that produced findings
- **Edge/Error stage (#6) catches more than Daily Usage (#5)** — most teams over-simulate happy path; force at least 2 cells per persona on stage 6 (broker down, payment timeout, rate-limit hit, partial state)
- **Termination stage (#8) is ALWAYS under-simulated** — tenant offboarding, GDPR deletion, account closure, billing failure-then-cancel. If the simulation finds zero gaps in stage 8, it didn't actually simulate stage 8
- **Concern category overlaps cause double-counting** — "Security" + "Compliance" + "Data" often flag the same gap from 3 angles; dedupe via `audit-to-gap-pipeline.md` Step 2 before filing
- **Don't simulate features not yet in BRD** — speculative features generate noise gaps that distract from real ones; cross-check `documents/00-brd/` first
